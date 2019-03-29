package node;

import config.Configuration;
import config.NodeListFileParser;
import logging.LoggerFactory;
import messages.Message;
import messages.MessageType;
import messages.SuccessorMessage;
import node.coordinator.Coordinator;
import org.omg.CORBA.TIMEOUT;
import sockets.RingSocket;
import sockets.Switch;
import util.ComUtil;

import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;


public class Node {

    private static final int NUM_TRIES = 3;
    private static final int DEFAULT_JITTER = 3;
    private static final int COORDINATOR_ID = 6;

    private final Configuration config;
    private final Logger logger;

    private RingSocket ringSocket;
    private Switch messageSwitch;

    private Coordinator coordinator = null;
    private AddressTranslator addressTranslator;

    public Node(Configuration config) throws IOException {
        this.logger = LoggerFactory.buildLogger(config.getNodeId());
        this.addressTranslator = NodeListFileParser.parseNodeFile(config.getListFilePath(), logger);
        this.config = config;
    }

    private void initializeCoordinatorThread() {
        this.coordinator = new Coordinator(messageSwitch, logger, config.getNodeId());
        this.coordinator.start();
    }

    private void initializeCommunication() throws SocketException {
        try {
            logger.info("Initializing sockets");
            this.ringSocket = new RingSocket(addressTranslator.getSocketAddress(config.getNodeId()), 2, 2);
            this.messageSwitch = new Switch(config.getNodeId(), logger, addressTranslator);
            this.messageSwitch.start();
        } catch (IOException e) {
            logger.warning("Failed to initialize sockets.");
            logger.warning(e.getMessage());
            throw e;
        }
    }

    private int awaitSuccessor() throws InterruptedException, TimeoutException {
        logger.info("Awaiting successor from coordinator");
        do {
            final Message message = this.messageSwitch.getNodeMessage(ComUtil.timeoutJitter(DEFAULT_JITTER));
            if (message == null) throw new TimeoutException();

            if (message.getType() == MessageType.SUCCESSOR) {
                logger.info(String.format("Received message type %d from ", message.getSrcId()));
                int successorId = message.getPayload(SuccessorMessage.class).getSuccessorId();
                logger.info(String.format("Assigned %s as successor", successorId));
                return successorId;
            }
        } while (true);
    }

    /**
     * Requests a successor from the coordinator on next node failure or joining ring
     *
     * @param joining whether or not this node is rejoining the ring
     * @return the id of the nodes new successor
     * @throws IOException
     * @throws InterruptedException
     * @throws TimeoutException
     */
    private int requestSuccessor(boolean joining) throws IOException, InterruptedException, TimeoutException {
        logger.info("Requesting successor from coordinator");

        final MessageType messageType = joining ? MessageType.JOIN : MessageType.SUCCESSOR_REQUEST;
        final Message request = new Message(messageType, config.getElectionMethod(), config.getNodeId());

        for (int numTries = 0; numTries < NUM_TRIES; numTries++) {
            messageSwitch.sendMessage(request, COORDINATOR_ID);

            try {
                return awaitSuccessor();
            } catch (TimeoutException timeoutException) {
                if (numTries == NUM_TRIES - 1) {
                    logger.warning(String.format("Failed to request successor after %d tries.", numTries + 1));
                    throw timeoutException;
                }
            } catch (InterruptedException e) {
                logger.warning(String.format("Failed to request successor after %d tries.", numTries));
                throw e;
            }
        }

        return 0;
    }

    public void start() throws IOException, ClassNotFoundException, InterruptedException, TimeoutException {
        logger.info(String.format("Initializing node with configuration: %s", config.toString()));

        initializeCommunication();

        if (config.getNodeId() == COORDINATOR_ID) {
            this.initializeCoordinatorThread();
        }

        final int successor = this.requestSuccessor(true);

        // Connect to successor
        ringSocket.updateSuccessor(addressTranslator.getSocketAddress(successor));

        // Await predecessor connection
        ringSocket.updatePredecessor();

        // Begin operation
        int times = 0;
        while (times < 10) {
            passToken();
            times++;
        }

        end();
    }

    private Message readToken() throws IOException {
        try {
            return ringSocket.receiveFromPredecessor();
        } catch (IOException | ClassNotFoundException e) {
            logger.warning("Disconnected from predecessor");
            ringSocket.updatePredecessor();
            return readToken();
        }
    }

    private void forwardToken() throws InterruptedException, TimeoutException, IOException {
        logger.info("Forwarding token");
        final Message message = new Message(MessageType.TOKEN, config.getElectionMethod(), config.getNodeId());

        try {
            ringSocket.sendToSuccessor(message);
        } catch (IOException e) {
            logger.warning("Disconnected from predecessor");
            int succ = requestSuccessor(false);
            ringSocket.updateSuccessor(addressTranslator.getSocketAddress(succ));
            forwardToken();
        }
    }


    private void passToken() throws ClassNotFoundException, InterruptedException, IOException, TimeoutException {
        final Message token = readToken();
        logger.info(String.format("Received token from %d", token.getSrcId()));
        sleep(3);
        forwardToken();
    }

    public void end() {
        logger.warning("Shutting down");
        if (coordinator != null)
            this.coordinator.stop();

        if (messageSwitch != null)
            this.messageSwitch.stop();
    }
}
