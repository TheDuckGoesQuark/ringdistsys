package node;

import config.Configuration;
import config.NodeListFileParser;
import logging.LoggerFactory;
import messages.Message;
import messages.MessageType;
import messages.SuccessorMessage;
import node.coordinator.Coordinator;
import sockets.RingSocket;
import sockets.Switch;
import util.ComUtil;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;


public class Node {

    private static final int NUM_TRIES = 3;
    private static final int DEFAULT_JITTER = 3;
    private static final int COORDINATOR_ID = 6;

    private final Configuration config;
    private final Logger logger;
    private boolean hasToken = false;

    private ExecutorService executorService;
    private RingSocket ringSocket;
    private Switch messageSwitch;

    private Coordinator coordinator = null;
    private AddressTranslator addressTranslator;

    public Node(Configuration config) throws IOException {
        this.logger = LoggerFactory.buildLogger(config.getNodeId());
        this.addressTranslator = NodeListFileParser.parseNodeFile(config.getListFilePath(), logger);
        this.config = config;
        this.executorService = Executors.newCachedThreadPool();
    }

    private void initializeCoordinatorThread() {
        coordinator = new Coordinator(messageSwitch, logger, config.getNodeId());
        executorService.submit(coordinator);
    }

    private void initializeCommunication() throws IOException {
        try {
            logger.info("Initializing sockets");
            ringSocket = new RingSocket(addressTranslator.getSocketAddress(config.getNodeId()), logger);
            messageSwitch = new Switch(config.getNodeId(), logger, addressTranslator);
            executorService.submit(messageSwitch);
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

    /**
     * Begins node execution
     * @throws IOException
     * @throws InterruptedException
     * @throws TimeoutException
     */
    public void start() throws IOException, InterruptedException, TimeoutException {
        logger.info(String.format("Initializing node with configuration: %s", config.toString()));

        initializeCommunication();

        if (config.getNodeId() == COORDINATOR_ID) {
            this.initializeCoordinatorThread();
            hasToken = true;
        }

        updateSuccessor(this.requestSuccessor(true), true);

        // Begin token passing
        executorService.submit((Callable<Void>) () -> {
            while (!killswitch())
                passToken();

            return null;
        });

        // Handle any coordination updates
        while (!killswitch()) {
            logger.info("Listening for coordination updates");
            final Message message = messageSwitch.getNodeMessage(3);

            if (message == null) continue;

            switch (message.getType()) {
                case SUCCESSOR:
                    logger.info("Received new successor assignment");
                    updateSuccessor(
                            message.getPayload(SuccessorMessage.class).getSuccessorId(),
                            false
                    );
                    break;
            }
        }

        end();
    }

    private void updateSuccessor(int successor, boolean initial) throws IOException {
        // Await self connection in background if I'm connecting to myself
        if (initial || successor == config.getNodeId()) {
            executorService.submit((Callable<Void>) () -> {
                ringSocket.updatePredecessor();
                return null;
            });
        }

        // Connect to successor
        ringSocket.updateSuccessor(addressTranslator.getSocketAddress(successor));
    }

    private boolean killswitch() {
        final String killswitch = System.getProperty("user.home") + "/killswitch";
        logger.info(String.format("Checking for %s", killswitch));
        return new File(killswitch).exists();
    }

    private Message readToken() throws IOException {
        try {
            return ringSocket.receiveFromPredecessor();
        } catch (IOException | ClassNotFoundException e) {
            logger.warning("Disconnected from predecessor");
            // TODO fix token being lost, or hanging here when predecessor = successor
            ringSocket.updatePredecessor();
            logger.info("Connected to new predecessor");
            return readToken();
        }
    }

    private void forwardToken() throws InterruptedException, TimeoutException, IOException {
        logger.info("Forwarding token");
        final Message message = new Message(MessageType.TOKEN, config.getElectionMethod(), config.getNodeId());

        try {
            ringSocket.sendToSuccessor(message);
        } catch (IOException e) {
            logger.warning("Disconnected from successor");
            int succ = requestSuccessor(false);
            logger.info(String.format("Updating successor to %d", succ));
            ringSocket.updateSuccessor(addressTranslator.getSocketAddress(succ));
            logger.info("Retrying token send");
            forwardToken();
        }
    }

    private void passToken() throws InterruptedException, IOException, TimeoutException {
        if (!hasToken) {
            final Message token = readToken();
            logger.info(String.format("Received token from %d", token.getSrcId()));
            hasToken = true;
        }
        sleep(300);
        forwardToken();
        hasToken = false;
    }

    public void end() throws IOException {
        logger.warning("Shutting down");

        if (coordinator != null)
            this.coordinator.stop();

        if (messageSwitch != null)
            this.messageSwitch.stop();

        if (ringSocket != null)
            this.ringSocket.close();

        executorService.shutdown();

        logger.warning("Finished shutting down node.");
    }
}
