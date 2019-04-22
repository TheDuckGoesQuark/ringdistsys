package node;

import config.Configuration;
import logging.LoggerFactory;
import messages.Message;
import messages.MessageType;
import sockets.RingSocket;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

/**
 * Provides an abstraction for using the ringsocket class
 */
public class RingCommunicationHandler {

    private static final int DEFAULT_JITTER = 3;

    private final Logger logger = LoggerFactory.getLogger();
    private final Configuration config;
    private final ExecutorService executorService;
    private final RingSocket ringSocket;

    private int successorId = 0;

    RingCommunicationHandler(Configuration config, AddressTranslator addressTranslator, ExecutorService executorService) throws IOException {
        this.ringSocket = new RingSocket(config.getNodeId(), addressTranslator);
        this.config = config;
        this.executorService = executorService;
    }

    /**
     * Waits until an acknowledgement is received for a token
     *
     * @return true if the acknowledgement is received
     */
    private boolean ackIsReceived() {
        logger.info("Waiting on token ACK");
        final Message tokenAck = ringSocket.receiveFromSuccessor(DEFAULT_JITTER);
        return tokenAck != null;
    }

    /**
     * Sends token to successor. If node is not its own successor, it will wait for confirmation that token was received
     *
     * @return true if token is forwarded successfully, false otherwise
     */
    public boolean forwardToken() {
        logger.info("Forwarding token");
        final Message message = new Message(MessageType.TOKEN, config.getNodeId());

        boolean exceptionThrown = false;
        try {
            ringSocket.sendToSuccessor(message);
        } catch (IOException e) {
            exceptionThrown = true;
        }

        return ringSocket.isClosedLoop() || (!exceptionThrown && ackIsReceived());
    }

    /**
     * Sends token acknowledgement to predecessor
     *
     * @throws IOException if unable to send to predecessor
     */
    public void sendTokenAck() throws IOException {
        logger.info("Sending token ACK");
        final Message tokenAck = new Message(MessageType.TOKEN_ACK, config.getNodeId());
        this.ringSocket.sendToPredeccesor(tokenAck);
    }

    /**
     * Updates this nodes successor to the one with the given Id. If this node is its own successor, then its predeccesor
     * will also be updated.
     *
     * @param successor id of new successor
     * @throws IOException
     */
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
        successorId = successor;
    }

    void cleanup() throws IOException {
        if (ringSocket != null)
            this.ringSocket.close();
    }

    /**
     * Receives message from predecessor
     *
     * @return message received from predecessor
     */
    public Message receiveFromPredecessor() {
        return ringSocket.receiveFromPredecessor(null);
    }

    public void sendToSuccessor(Message message) throws IOException {
        ringSocket.sendToSuccessor(message);
    }

    /**
     * blocks until a predecessor connects
     *
     * @throws IOException
     */
    public void updatePredecessor() throws IOException {
        ringSocket.updatePredecessor();
    }

    public int getSuccessorId() {
        return successorId;
    }
}
