package node;

import config.Configuration;
import globalpersistence.DatabaseRingStore;
import globalpersistence.NodeRow;
import globalpersistence.RingStore;
import logging.LoggerFactory;
import messages.Message;
import messages.MessageType;
import messages.SuccessorMessage;
import sockets.UDPSocket;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import static messages.MessageType.SUCCESSOR;


public class Node {

    private final Logger logger = LoggerFactory.getLogger();
    private final Configuration config;
    private final ExecutorService executorService;
    private final RingStore ringStore;
    private final TokenRingManager tokenRingManager;
    private final UDPSocket udpSocket;

    private int coordinatorId = 0;

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

    private boolean isCoordinator() {
        return config.getNodeId() == coordinatorId;
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
        }
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

//        // Handle any coordination updates
//        while (!killswitch()) {
//            logger.info("Listening for coordination updates");
//            final Message message = udpSocket.receiveMessage(3);
//
//            if (message == null) continue;
//
//            switch (message.getType()) {
//                case SUCCESSOR:
//                    logger.info("Received new successor assignment");
//                    tokenRingManager.updateSuccessor(
//                            message.getPayload(SuccessorMessage.class).getSuccessorId(),
//                            false
//                    );
//                    break;
//                case JOIN:
//                    logger.info("Received join request");
//                    tokenRingManager.handleJoinRequest(message);
//                    break;
//                case SUCCESSOR_REQUEST:
//                    logger.info("Received successor request");
//                    tokenRingManager.handleJoinRequest(message);
//                    break;
//                default:
//                    logger.info("Received unknown message type: " + message.getType().name());
//                    break;
//            }
//        }

        end();
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
                    tokenRingManager.updateSuccessor(message.getPayload(SuccessorMessage.class).getSuccessorId());
                    joined = true;
                    break;
                case JOIN:
                    logger.info("Received successor request");
                    if (this.isCoordinator() && message.getSrcId() == config.getNodeId()) {
                        this.handleJoinRequest(message);
                    }
                    break;
                default:
                    logger.info("Received unknown message type: " + message.getType().name());
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
