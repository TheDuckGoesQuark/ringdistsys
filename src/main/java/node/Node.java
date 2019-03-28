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
import java.util.logging.Logger;


public class Node {

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
            final Message message = this.udpSocket.receiveMessage();

            logger.info(String.format("Received message type %s from ", message));
            if (message.getType() == MessageType.SUCCESSOR) {
                successorAddress = message.getPayload(SuccessorMessage.class).getSuccessorAddress();
                break;
            }
        } while (true);

        logger.info(String.format("Assigned %s as successor", successorAddress.getAddress()));
        return successorAddress;
    }

    public void start() throws IOException, ClassNotFoundException {
        logger.info(String.format("Initializing node with configuration: %s", config.toString()));

        initializeAddressTranslator();
        initializeSockets();

        if (config.getNodeId() == 6) {
            this.initializeCoordinatorThread();
        }

        final InetSocketAddress succesorAddress = this.awaitSuccessor();

        ringSocket.updateSuccessor(succesorAddress);
        ringSocket.updatePredecessor();

        // TODO await successor address from coordinator
        // TODO connect to successor

        // TODO on loss of connection to successor:
        // TODO if successor ID = Coordinator ID: Begin reelection.
    }
}
