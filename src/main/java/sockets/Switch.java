package sockets;

import messages.Message;
import node.AddressTranslator;
import util.StoppableThread;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

/**
 * Receives and sends UDP messages and sorts into those for the coordinator thread
 * and for the node.
 */
public class Switch extends StoppableThread {

    private static final String THREAD_NAME = "SWITCH_THREAD";

    // Buffers for messages for this node
    private final BlockingQueue<Message> nodeInbound = new LinkedBlockingQueue<>();
    private final BlockingQueue<Message> coordinatorInbound = new LinkedBlockingQueue<>();

    private final Thread thread = new Thread();
    private final UDPSocket udpSocket;
    private final AddressTranslator addressTranslator;

    private boolean running = false;

    public Switch(int myId, Logger logger, AddressTranslator addressTranslator) throws SocketException {
        super(THREAD_NAME, logger);
        this.addressTranslator = addressTranslator;
        this.udpSocket = new UDPSocket(this.addressTranslator.getSocketAddress(myId));
    }

    public void start() {
        getLogger().info("Starting " + THREAD_NAME);
        if (!thread.isAlive()) {
            thread.start();
        } else {
            getLogger().warning(THREAD_NAME + " already running!");
        }
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        running = true;

        while (running) {
            receiveMessage().ifPresent(this::sortMessage);
        }

        udpSocket.close();
    }

    public void sendMessage(Message message, int destId) throws IOException {
        udpSocket.sendMessage(message, addressTranslator.getSocketAddress(destId));
    }

    public Message getNodeMessage(int timeout) throws InterruptedException, TimeoutException  {
        return nodeInbound.poll(timeout, TimeUnit.SECONDS);
    }

    public Message getCoordinatorMessage(int timeout) throws InterruptedException {
        return coordinatorInbound.poll(timeout, TimeUnit.SECONDS);
    }

    private void sortMessage(Message message) {
        switch (message.getType()) {
            case SUCCESSOR_REQUEST:
                coordinatorInbound.add(message);
                break;
            default:
                nodeInbound.add(message);
        }
    }

    private Optional<Message> receiveMessage() {
        try {
            return Optional.ofNullable(udpSocket.receiveMessage(0));
        } catch (IOException | ClassNotFoundException e) {
            getLogger().warning("Bad message received");
            getLogger().warning(e.getMessage());
        }

        return Optional.empty();
    }
}