package node;

import config.Configuration;
import logging.LoggerFactory;
import messages.Message;
import messages.MessageType;
import messages.SuccessorMessage;
import node.coordinator.Coordinator;
import sockets.RingSocket;
import sockets.UDPSocket;
import util.ComUtil;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;

class TokenRingManager {

    private static final int DEFAULT_JITTER = 3;
    private static final int NUM_TRIES = 3;
    private static final int COORDINATOR_ID = 6;

    private final Logger logger = LoggerFactory.getLogger();
    private final Configuration config;
    private final ExecutorService executorService;
    private final UDPSocket udpSocket;
    private final RingSocket ringSocket;

    private boolean hasToken = false;
    private Coordinator coordinator = null;

    TokenRingManager(Configuration config, AddressTranslator addressTranslator, ExecutorService executorService, UDPSocket udpSocket) throws IOException {
        this.ringSocket = new RingSocket(config.getNodeId(), addressTranslator);
        this.udpSocket = udpSocket;
        this.config = config;
        this.executorService = executorService;

        if (config.getNodeId() == COORDINATOR_ID) {
            this.hasToken = true;
            this.coordinator = new Coordinator(udpSocket, logger, config.getNodeId());
        }
    }

    boolean isCoordinator() {
        return coordinator != null;
    }

    private Message readToken() {
        return ringSocket.receiveFromPredecessor(null);
    }

    private boolean ackIsReceived() {
        logger.info("Waiting on token ACK");
        final Message tokenAck = ringSocket.receiveFromSuccessor(DEFAULT_JITTER);
        return tokenAck != null;
    }

    private int awaitSuccessor() throws TimeoutException {
        logger.info("Awaiting successor from coordinator");
        do {
            final Message message = this.udpSocket.receiveMessage(ComUtil.timeoutJitter(DEFAULT_JITTER));
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
     */
    private void requestSuccessor(boolean joining) throws IOException {
        logger.info("Requesting successor from coordinator");
        final MessageType messageType = joining ? MessageType.JOIN : MessageType.SUCCESSOR_REQUEST;
        final Message request = new Message(messageType, config.getElectionMethod(), config.getNodeId());

        int numTries = 0;
        boolean successful = false;
        while (numTries < NUM_TRIES && !successful) {
            if (this.isCoordinator()) {
                if (joining) this.coordinator.handleJoinRequest(request);
                else this.coordinator.handleSuccessorRequest(request);
            } else {
                udpSocket.sendMessage(request, COORDINATOR_ID);
            }

            try {
                updateSuccessor(awaitSuccessor(), joining);
                // TODO main thread is actually handling this message when it comes, so this thread thinks it doesnt get a reply lmaoooo
                successful = true;
            } catch (TimeoutException timeoutException) {
                numTries++;
            }
        }

        if (numTries == NUM_TRIES - 1) {
            throw new IOException(String.format("Failed to request successor after %d tries.", numTries + 1));
        }
    }

    void forwardToken() throws IOException {
        logger.info("Forwarding token");
        final Message message = new Message(MessageType.TOKEN, config.getElectionMethod(), config.getNodeId());

        while (hasToken) {
            try {
                ringSocket.sendToSuccessor(message);

                if (!ringSocket.isClosedLoop() && !ackIsReceived()) {
                    throw new IOException("No acknowledgement from successor");
                } else {
                    hasToken = false;
                }

            } catch (IOException e) {
                logger.warning("Disconnected from successor");
                requestSuccessor(false);
                logger.info("Retrying token send");
            }
        }
    }

    private void sendTokenAck() throws IOException {
        logger.info("Sending token ACK");
        final Message tokenAck = new Message(MessageType.TOKEN_ACK, config.getElectionMethod(), config.getNodeId());
        this.ringSocket.sendToPredeccesor(tokenAck);
    }

    /**
     * Blocks until the token is available
     *
     * @throws IOException
     */
    void waitForToken() throws IOException {
        Message token = null;

        while (token == null) {
            token = readToken();

            if (token == null) {
                logger.warning("Disconnected from predecessor");
                ringSocket.updatePredecessor();
                logger.info("Connected to new predecessor");
            }
        }

        logger.info(String.format("Received token from %d", token.getSrcId()));
        sendTokenAck();
        hasToken = true;
    }

    void passToken() throws InterruptedException, IOException {
        if (!hasToken) {
            waitForToken();
        }
        sleep(3000);
        forwardToken();
    }

    void updateSuccessor(int successor, boolean initial) throws IOException {
        // Await self connection in background if I'm connecting to myself
        if (initial || successor == config.getNodeId()) {
            executorService.submit((Callable<Void>) () -> {
                ringSocket.updatePredecessor();
                return null;
            });
        }

        // Connect to successor
        ringSocket.updateSuccessor(successor);
    }

    void joinRing() throws IOException {
        requestSuccessor(true);
    }

    void handleJoinRequest(Message joinRequest) {
        if (this.isCoordinator()) {
            this.coordinator.handleJoinRequest(joinRequest);
        }
    }

    void cleanup() throws IOException {
        if (ringSocket != null)
            this.ringSocket.close();
    }

    public void handleSuccessorRequest(Message message) {
        if (this.isCoordinator()) {
            this.coordinator.handleSuccessorRequest(message);
        }
    }
}
