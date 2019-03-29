package node;

import config.Configuration;
import config.NodeListFileParser;
import logging.LoggerFactory;
import messages.Message;
import messages.MessageType;
import messages.SuccessorMessage;
import sockets.RingSocket;
import sockets.UDPSocket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;


public class Node {

    private static final int NUM_TRIES = 3;
    private static final int DEFAULT_JITTER = 2;
    private static final int COORDINATOR_ID = 6;

    private final Configuration config;
    private final Logger logger;

    private RingSocket ringSocket;
    private UDPSocket udpSocket;
    private Coordinator coordinator = null;
    private AddressTranslator addressTranslator;

    public Node(Configuration config) {
        this.config = config;
        this.logger = LoggerFactory.buildLogger(config.getNodeId());
    }

    private void initializeCoordinatorThread() {
        this.coordinator = new Coordinator(udpSocket, addressTranslator, logger);
        this.coordinator.start();
    }

    private void initializeSockets() throws SocketException {
        try {
            logger.info("Initializing sockets");
            this.ringSocket = new RingSocket(config.getAddress());
            this.udpSocket = new UDPSocket(config.getAddress());
        } catch (IOException e) {
            logger.warning("Failed to initialize sockets.");
            logger.warning(e.getMessage());
            throw e;
        }
    }

    private void initializeAddressTranslator() throws IOException {
        this.addressTranslator = NodeListFileParser.parseNodeFile(config.getListFilePath(), logger);
    }

    private boolean isCoordinator() {
        return this.coordinator != null;
    }

    private void terminateCoordinatorThread() {
        this.coordinator.stop();
    }

    private InetSocketAddress awaitSuccessor() throws IOException, ClassNotFoundException {
        logger.info("Awaiting successor from coordinator");
        InetSocketAddress successorAddress = null;

        do {
            final Message message = this.udpSocket.receiveMessage(ComUtil.timeoutJitter(DEFAULT_JITTER));
            logger.info(String.format("Received message type %d from ", message.getSrcId()));

            if (message.getType() == MessageType.SUCCESSOR) {
                successorAddress = message.getPayload(SuccessorMessage.class).getSuccessorAddress();
            }
        } while (successorAddress == null);

        logger.info(String.format("Assigned %s as successor", successorAddress.getAddress()));
        return successorAddress;
    }

    private InetSocketAddress requestSuccessor() throws IOException, ClassNotFoundException {
        logger.info("Requesting successor from coordinator");

        final Message request = new Message(MessageType.SUCCESSOR_REQUEST, config.getElectionMethod(), config.getNodeId());
        for (int numTries = 0; numTries < NUM_TRIES; numTries++) {
            udpSocket.sendMessage(request, addressTranslator.getSocketAddress(COORDINATOR_ID));

            try {
                return awaitSuccessor();
            } catch (SocketTimeoutException timeoutException) {
                if (numTries == NUM_TRIES - 1) {
                    logger.info(String.format("Failed to request successor after %d tries.", numTries));
                    throw timeoutException;
                }
            }
        }

        return null;
    }

    public void start() throws IOException, ClassNotFoundException, InterruptedException {
        logger.info(String.format("Initializing node with configuration: %s", config.toString()));

        initializeAddressTranslator();
        initializeSockets();

        if (config.getNodeId() == COORDINATOR_ID) {
            this.initializeCoordinatorThread();
        }

        final InetSocketAddress successor = this.requestSuccessor();

        ringSocket.updateSuccessor(successor);
        ringSocket.updatePredecessor();

        passToken();

        // TODO on loss of connection to successor:
        // TODO if successor ID = Coordinator ID: Begin reelection.
    }

    private void passToken() throws IOException, ClassNotFoundException, InterruptedException {
        while(true) {
            Message message = ringSocket.receiveFromPredecessor();
            if (message.getType() == MessageType.TOKEN) {
                logger.info(String.format("Received token from %d", message.getSrcId()));
                sleep(3);
                ringSocket.sendToSuccessor(message);
            }
        }
    }
}
