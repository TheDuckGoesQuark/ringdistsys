package node;

import config.Configuration;
import globalpersistence.DatabaseRingStore;
import globalpersistence.NodeRow;
import globalpersistence.RingStore;
import logging.LoggerFactory;
import messages.CoordinatorMessage;
import messages.Message;
import messages.MessageType;
import messages.SuccessorMessage;
import sockets.UDPSocket;

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
    private final TokenRingManager tokenRingManager;
    private final UDPSocket udpSocket;

    private final BlockingQueue<Token> usableTokenQueue = new ArrayBlockingQueue<>(1);
    private final BlockingQueue<Token> forwardableTokenQueue = new ArrayBlockingQueue<>(1);

    private final Object successorConnectedNotifier = new Object();

    private int coordinatorId = 0;
    private boolean lostCoordinator = false;

    public Node(Configuration config) throws IOException {
        this.config = config;
        this.executorService = Executors.newCachedThreadPool();

        this.ringStore = new DatabaseRingStore(config.getListFilePath(), config.shouldDropEverything());
        this.ringStore.initialize();

        final List<NodeRow> allNodes = this.ringStore.getAllNodes();
        final AddressTranslator addressTranslator = new AddressTranslator(allNodes);
        this.udpSocket = new UDPSocket(addressTranslator, config.getNodeId());
        this.tokenRingManager = new TokenRingManager(config, addressTranslator, executorService);

        this.initializeCoordinator(allNodes);
    }

    /**
     * Initializes the coordinator ID to the assigned node in the database,
     * or this node if none are currently assigned.
     *
     * @param allNodes list of nodes in database
     */
    private void initializeCoordinator(List<NodeRow> allNodes) {
        final Optional<NodeRow> optCoordinator = allNodes.stream()
                .filter(NodeRow::isCoordinator)
                .findFirst();

        if (optCoordinator.isPresent()) {
            coordinatorId = optCoordinator.get().getNodeId();
        } else {
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
     * Terminates and cleans up any resources used and created by the node, such as sockets and threads.
     *
     * @throws IOException if something goes wrong during termination.
     */
    public void end() throws IOException {
        logger.warning("Shutting down");

        if (udpSocket != null)
            this.udpSocket.close();

        tokenRingManager.cleanup();

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

        // Begin election and token handler on another thread
        executorService.submit((Callable<Void>) () -> {
            while (!killswitch())
                try {
                    handleRingMessages();
                } catch (Exception ignored) {
                }

            return null;
        });

        // Handle any coordination updates
        executorService.submit((Callable<Void>) () -> {
            while (!killswitch()) {
                try {
                    handleCoordinationMessages();
                } catch (Exception ignored) {
                }
            }
            return null;
        });

        while (!killswitch()) {
            try {
                logger.info("Waiting for token...");
                final Token token = usableTokenQueue.take();
                logger.info("Used token :)");
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

        final Message message = tokenRingManager.receiveFromPredecessor();

        if (message == null) {
            logger.info("Lost connection to predecessor");
            tokenRingManager.updatePredecessor();
        } else {
            switch (message.getType()) {
                case ELECTION:
                    logger.info("Received election message");
                    break;
                case COORDINATOR:
                    logger.info("Received coordinator message");
                    handleCoordinatorMessage(message);
                    break;
                case TOKEN:
                    logger.info("Received token");
                    handleToken();
                    break;
            }
        }
    }

    /**
     * Assigns the elected coordinator
     *
     * @param message message containing new coordinator
     */
    private void handleCoordinatorMessage(Message message) {
        CoordinatorMessage coordinatorMessage = message.getPayload(CoordinatorMessage.class);
        coordinatorId = coordinatorMessage.getCoordinatorId();

        if (isCoordinator()) {
            ringStore.updateCoordinator(coordinatorId);
        }
    }

    /**
     * Holds onto the token
     */
    private void handleToken() throws IOException, InterruptedException {
        try {
            tokenRingManager.sendTokenAck();
        } catch (IOException e) {
            logger.warning("Failed to send ack, assuming predecessor has failed");
            tokenRingManager.updatePredecessor();
        } finally {
            usableTokenQueue.put(new Token());
        }

        sleep(TOKEN_HOLD_TIME_SECS * 1000);

        logger.info("Waiting on main thread to finish using token");
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
        logger.info("Attempting to forward token");
        boolean successful = tokenRingManager.forwardToken();

        if (!successful) {
            logger.warning("Lost connection to successor");
            handleLostSuccessor();
            logger.info("Returning token to forwarding queue");
            forwardableTokenQueue.put(new Token());
        }
    }

    /**
     * Handles a lost successor. If successor was the coordinator, then an election will have to take place first.
     * Then or otherwise, a new successor will be requested
     *
     * @throws IOException :(
     */
    private void handleLostSuccessor() throws IOException, InterruptedException {
        if (tokenRingManager.getSuccessorId() == coordinatorId) {
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

    private void beginElection() {
        // TODO
    }

    /**
     * Handles coordinator messages for maintaining ring
     */
    private void handleCoordinationMessages() throws IOException {
        logger.info("Waiting for coordinator message");
        final Message message = udpSocket.receiveMessage(5);

        if (message == null) {
            logger.info("Nothing received...");
        } else {
            switch (message.getType()) {
                case JOIN:
                    logger.info("Received join request");
                    if (isCoordinator()) {
                        handleJoinRequest(message);
                    }
                    break;
                case SUCCESSOR_REQUEST:
                    logger.info("Received successor request");
                    if (isCoordinator()) {
                        handleSuccessorRequest(message.getSrcId());
                    }
                    break;
                case SUCCESSOR:
                    logger.info("Received successor assignment");
                    handleSuccessorMessage(message);
                    synchronized (successorConnectedNotifier) {
                        successorConnectedNotifier.notifyAll();
                    }
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
        final List<NodeRow> ringNodes = ringStore.getAllNodesWithSuccessors();

        final Optional<NodeRow> requestingNode = ringNodes.stream()
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
        this.tokenRingManager.updateSuccessor(successorMessage.getSuccessorId(), false);
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
                    logger.info("Received new successor assignment");
                    tokenRingManager.updateSuccessor(message.getPayload(SuccessorMessage.class).getSuccessorId(), true);
                    joined = true;
                    break;
                case JOIN:
                    logger.info("Received successor request");
                    if (this.isCoordinator() && message.getSrcId() == config.getNodeId()) {
                        this.handleJoinRequest(message);
                    }
                    break;
                default:
                    logger.info("Received irrelevant message: " + message.getType().name());
            }
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
        final Message request = new Message(messageType, config.getElectionMethod(), config.getNodeId());

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
        logger.info("Handling successor request");
        List<NodeRow> allNodes = ringStore.getAllNodesWithSuccessors();

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
            final NodeRow predecessor = allNodes.get(rand.nextInt(allNodes.size()));
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
        final Message reply = new Message(SUCCESSOR, config.getElectionMethod(), config.getNodeId(), successorMessage);
        udpSocket.sendMessage(reply, nodeId);
    }

}
