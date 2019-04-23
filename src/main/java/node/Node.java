package node;

import config.Configuration;
import node.clientmessaging.ChatEndpoint;
import node.ringstore.DatabaseRingStore;
import node.ringstore.VirtualNode;
import node.ringstore.RingStore;
import logging.LoggerFactory;
import messages.Message;
import messages.MessageType;
import messages.SuccessorMessage;
import messages.election.ElectionMessageHeader;
import node.clientmessaging.ClientHandler;
import node.electionhandlers.*;
import node.sockets.UDPSocket;
import util.Token;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.*;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;
import static messages.MessageType.SUCCESSOR;


public class Node {

    private static final int TOKEN_HOLD_TIME_SECS = 3;

    private final Logger logger = LoggerFactory.getLogger();
    private final Configuration config;
    private final ExecutorService executorService;
    private final RingStore ringStore;
    private final RingCommunicationHandler ringComms;
    private final ClientHandler clientHandler;
    private final UDPSocket udpSocket;

    private final BlockingQueue<Token> usableTokenQueue = new ArrayBlockingQueue<>(1);
    private final BlockingQueue<Token> forwardableTokenQueue = new ArrayBlockingQueue<>(1);
    private final Object successorConnectedNotifier = new Object();

    private int coordinatorId = 0;
    private boolean lostCoordinator = false;
    private ElectionHandler currentElectionHandler;

    public Node(Configuration config) throws Exception {
        this.config = config;
        this.executorService = Executors.newCachedThreadPool();

        this.ringStore = new DatabaseRingStore(config.getListFilePath(), config.shouldDropEverything());
        this.ringStore.initialize();

        final List<VirtualNode> allNodes = this.ringStore.getAllNodes();
        final VirtualNode thisNode = allNodes.stream()
                .filter(node -> node.getNodeId() == config.getNodeId())
                .findFirst()
                .orElseThrow(() -> new IOException("Node missing from database."));

        this.clientHandler = new ChatEndpoint(thisNode.getAddress(), thisNode.getClientPort());

        final AddressTranslator addressTranslator = new AddressTranslator(allNodes);
        this.udpSocket = new UDPSocket(addressTranslator, config.getNodeId());
        this.ringComms = new RingCommunicationHandler(config.getNodeId(), addressTranslator, executorService);

        this.initializeCoordinator(allNodes);
    }

    /**
     * Initializes the coordinator ID to the assigned node in the database,
     * or this node if none are currently assigned.
     *
     * @param allNodes list of nodes in database
     */
    private void initializeCoordinator(List<VirtualNode> allNodes) {
        final Optional<VirtualNode> optCoordinator = allNodes.stream()
                .filter(VirtualNode::isCoordinator)
                .findFirst();

        if (optCoordinator.isPresent()) {
            coordinatorId = optCoordinator.get().getNodeId();
        }

        if (!optCoordinator.isPresent() || coordinatorId == config.getNodeId()) {
            // WARN Possible race condition : make sure first node in
            // ring is started and set as coordinator before more join
            ringStore.updateCoordinator(config.getNodeId());
            coordinatorId = config.getNodeId();
            forwardableTokenQueue.add(new Token());
        }
    }

    private boolean isCoordinator() {
        return config.getNodeId() == coordinatorId;
    }

    /**
     * Checks for the existince of a directory called 'killswitch' in the users home directory.
     *
     * @return True if the directory '~/killswitch' exists.
     */
    private boolean killswitch() {
        final String killswitch = System.getProperty("user.home") + "/killswitch";
        return new File(killswitch).exists();
    }

    /**
     * Terminates and cleans up any resources used and created by the node, such as node.sockets and threads.
     *
     * @throws IOException if something goes wrong during termination.
     */
    public void end() throws IOException {
        logger.warning("Shutting down");

        if (udpSocket != null)
            this.udpSocket.close();

        ringComms.cleanup();
        clientHandler.cleanup();

        executorService.shutdown();

        logger.warning("Finished shutting down node.");
    }

    /**
     * Begins node execution by initializing the token ring manager and listening for messages
     *
     * @throws IOException something goes wrong.
     */
    public void start() throws IOException {
        logger.info(String.format("Initializing node with configuration: %s", config.toString()));

        joinRing();

        // Begin token handler on another thread
        executorService.submit((Callable<Void>) () -> {
            while (!killswitch())
                try {
                    handleRingMessages();
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.warning(e.getMessage());
                }

            return null;
        });

        // Handle any coordination updates
        executorService.submit((Callable<Void>) () -> {
            while (!killswitch()) {
                try {
                    handleCoordinationMessages();
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.warning(e.getMessage());
                }
            }
            return null;
        });

        // Serve clients on main thread
        while (!killswitch()) {
            try {
                final Token token = usableTokenQueue.take();
                logger.info("Holding token.");
                forwardableTokenQueue.put(token);
            } catch (InterruptedException e) {
                break;
            }
        }

        end();
    }

    /**
     * Handles token passing and election messages
     */
    private void handleRingMessages() throws IOException, InterruptedException {
        if (!forwardableTokenQueue.isEmpty()) {
            logger.info("Retrying token send");
            forwardableTokenQueue.take();
            forwardToken();
        }

        final Message message = ringComms.receiveFromPredecessor();

        if (message == null) {
            logger.info("Lost connection to predecessor");

            if (!ringComms.justDisconnectedFromSelf() && ringStore.getSizeOfRing() == 2) {
                // COUNT == 2 occurs when:
                // * connected to self and new node joins,
                // * ring contains me and other node, and that other node has failed
                // In first case, we just want to listen for a new predecessor,
                // in the second we need to trigger self connection
                handleLostSuccessor();
            } else {
                ringComms.updatePredecessor();
            }
        } else {
            switch (message.getType()) {
                case COORDINATOR_ELECTION:
                    handleElectionMessage(message);
                    break;
                case TOKEN:
                    handleToken();
                    break;
            }
        }
    }

    /**
     * Handles the election method based on the election method
     *
     * @param message message containing election message
     */
    private void handleElectionMessage(Message message) throws IOException {
        final ElectionMethod electionMethod = message.getPayload(ElectionMessageHeader.class).getElectionMethod();

        // Assign election handler if not already previously done so, or reassign to handle this new election
        if (currentElectionHandler == null || currentElectionHandler.getMethodName() != electionMethod) {
            assignHandlerForMethod(electionMethod);
        }

        currentElectionHandler.handleMessage(message);

        // Check if an election result has been obtained from the previous message
        if (currentElectionHandler.electionConcluded()) {
            coordinatorId = currentElectionHandler.getResult();
            logger.info(String.format("Election concluded, new coordinator: %d", coordinatorId));

            if (isCoordinator()) {
                ringStore.updateCoordinator(coordinatorId);
            }
        }
    }

    /**
     * Assigns this nodes election handler to the handler for the given election method
     *
     * @param method election method to be used
     */
    private void assignHandlerForMethod(ElectionMethod method) {
        switch (method) {
            case RING_BASED:
                currentElectionHandler = new RingBasedElectionHandler(ringComms, config.getNodeId());
                break;
            case CHANG_ROBERTS:
                currentElectionHandler = new ChangeRobertsElectionHandler(ringComms, config.getNodeId());
                break;
            case BULLY:
                currentElectionHandler = new BullyElectionHandler(udpSocket, config.getNodeId(), executorService, ringStore);
                break;
            default:
                logger.warning("Unknown election type.");
                currentElectionHandler = null;
        }
    }

    /**
     * Begins the election based on the configured election method
     */
    private void beginElection() throws IOException {
        assignHandlerForMethod(config.getElectionMethod());
        currentElectionHandler.startElection();
    }

    /**
     * Acknowledges token message, and holds onto it until the consuming thread returns it.
     */
    private void handleToken() throws IOException, InterruptedException {
        try {
            ringComms.sendTokenAck();
        } catch (IOException e) {
            logger.warning("Failed to send ack, assuming predecessor has failed");
            ringComms.updatePredecessor();
        } finally {
            usableTokenQueue.put(new Token());
        }

        sleep(TOKEN_HOLD_TIME_SECS * 1000);

        forwardableTokenQueue.take();
        forwardToken();
    }

    /**
     * Attempts to forward token and handles successor failure if it occurs
     *
     * @throws IOException
     * @throws InterruptedException
     */
    private void forwardToken() throws IOException, InterruptedException {
        boolean successful = ringComms.forwardToken();

        if (!successful) {
            logger.warning("Lost connection to successor");
            handleLostSuccessor();
            logger.info("Returning token to forwarding queue");
            forwardableTokenQueue.put(new Token());
        } else {
            logger.info("Token forwarded");
        }
    }

    /**
     * Handles a lost successor. If successor was the coordinator, then an election will have to take place first.
     * Then or otherwise, a new successor will be requested
     *
     * @throws IOException :(
     */
    private void handleLostSuccessor() throws IOException, InterruptedException {
        if (ringComms.getSuccessorId() == coordinatorId) {
            // Act as coordinator to and update self to connect to succ(lostCoordinator)
            handleSuccessorRequest(config.getNodeId());
            lostCoordinator = true;
        } else {
            // Ask coordinator for new successor
            requestSuccessor(false);
        }

        // Wait until other thread tells us we're connected again
        synchronized (successorConnectedNotifier) {
            successorConnectedNotifier.wait();
        }

        if (lostCoordinator) {
            beginElection();
        }
    }

    /**
     * Handles coordinator messages for maintaining ring
     */
    private void handleCoordinationMessages() throws IOException {
        final Message message = udpSocket.receiveMessage(5);

        if (message != null) {
            switch (message.getType()) {
                case JOIN:
                    if (isCoordinator()) {
                        handleJoinRequest(message);
                    }
                    break;
                case SUCCESSOR_REQUEST:
                    if (isCoordinator()) {
                        handleSuccessorRequest(message.getSrcId());
                    }
                    break;
                case SUCCESSOR:
                    handleSuccessorMessage(message);
                    synchronized (successorConnectedNotifier) {
                        successorConnectedNotifier.notifyAll();
                    }
                    break;
                case COORDINATOR_ELECTION:
                    handleElectionMessage(message);
                    break;
            }
        }
    }

    /**
     * Assumes requesting node's successor has failed and attempts to fix the ring.
     *
     * @param requestingNodeId id of node requesting successor
     */
    private void handleSuccessorRequest(int requestingNodeId) throws IOException {
        final List<VirtualNode> ringNodes = ringStore.getAllNodesWithSuccessors();

        final Optional<VirtualNode> requestingNode = ringNodes.stream()
                .filter(nodeRow -> nodeRow.getNodeId() == requestingNodeId)
                .findFirst();

        if (!requestingNode.isPresent()) {
            logger.warning("Node requesting successor is not part of ring?");
            return;
        }

        // Get the id of the node that has failed
        int lostNodeId = requestingNode.get().getSuccessorId().get();

        // Get the id of the node that came after the failing node
        int successorOfLostNodeId = ringNodes.stream()
                .filter(nodeRow -> nodeRow.getNodeId() == lostNodeId)
                .findFirst().get().getSuccessorId().get();

        // Succ(requestingNode) == succ(lostNode)
        ringStore.removeFromRing(requestingNodeId, successorOfLostNodeId, lostNodeId);

        sendSuccessorMessage(requestingNodeId, successorOfLostNodeId);
    }

    /**
     * Handles successor message
     *
     * @param message message
     */
    private void handleSuccessorMessage(Message message) throws IOException {
        final SuccessorMessage successorMessage = message.getPayload(SuccessorMessage.class);
        this.ringComms.updateSuccessor(successorMessage.getSuccessorId(), false);
    }

    /**
     * Makes a request to join the ring then handles either the successor message once it arrives,
     * or the original successor request if this node is the coordinator
     *
     * @throws IOException
     */
    private void joinRing() throws IOException {

        requestSuccessor(true);

        boolean joined = false;
        while (!joined) {
            logger.info("Waiting for coordinator to tell me my successor");
            final Message message = udpSocket.receiveMessage(3);

            if (message == null) continue;

            switch (message.getType()) {
                case SUCCESSOR:
                    ringComms.updateSuccessor(message.getPayload(SuccessorMessage.class).getSuccessorId(), true);
                    joined = true;
                    break;
                case JOIN:
                    if (this.isCoordinator() && message.getSrcId() == config.getNodeId()) {
                        this.handleJoinRequest(message);
                    }
                    break;
            }
        }

        // Try get elected if my ID is higher than the current coordinator
        if (coordinatorId < config.getNodeId()) {
            beginElection();
        }
    }

    /**
     * Requests a successor from the coordinator on next node failure or joining ring
     *
     * @param joining whether or not this node is joining the ring, or just requesting a new successor
     */
    private void requestSuccessor(boolean joining) throws IOException {
        logger.info("Requesting successor from coordinator " + coordinatorId);
        final MessageType messageType = joining ? MessageType.JOIN : MessageType.SUCCESSOR_REQUEST;
        final Message request = new Message(messageType, config.getNodeId());

        udpSocket.sendMessage(request, coordinatorId);
    }

    /**
     * Handles the given successor request by either connecting that node to itself (if its the only node) or inserting
     * it into a random point in the ring.
     *
     * @param request request made
     * @throws IOException something goes wrong while sending reply or connecting to DB
     */
    private void handleJoinRequest(Message request) throws IOException {
        List<VirtualNode> allNodes = ringStore.getAllNodesWithSuccessors();

        int requestingNodeId = request.getSrcId();
        if (allNodes.isEmpty()) {
            logger.info("Assigning node to self");
            // Assign requesting node to itself
            ringStore.setNodeSuccessor(requestingNodeId, requestingNodeId);
            sendSuccessorMessage(requestingNodeId, requestingNodeId);
        } else {
            logger.info("Inserting node into ring randomly");
            // Assign succ(new node) to random node
            final Random rand = new Random();
            final VirtualNode predecessor = allNodes.get(rand.nextInt(allNodes.size()));
            final int successor = predecessor.getSuccessorId().get();

            ringStore.insertIntoRing(predecessor.getNodeId(), successor, requestingNodeId);
            sendSuccessorMessage(requestingNodeId, successor);
            sendSuccessorMessage(predecessor.getNodeId(), requestingNodeId);

        }
    }

    /**
     * Sends a successor message informing the node to connect to the given successor
     *
     * @param nodeId         node needing successor
     * @param newSuccessorId successor they have to connect to
     * @throws IOException something goes wrong when sending message
     */
    private void sendSuccessorMessage(int nodeId, int newSuccessorId) throws IOException {
        final SuccessorMessage successorMessage = new SuccessorMessage(newSuccessorId);
        final Message reply = new Message(SUCCESSOR, config.getNodeId(), successorMessage);
        udpSocket.sendMessage(reply, nodeId);
    }

}
