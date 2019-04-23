package node.sockets;

import logging.LoggerFactory;
import messages.Message;
import node.AddressTranslator;

import java.io.IOException;
import java.net.*;
import java.util.logging.Logger;

public class UDPSocket {

    private static final int BUFFER_SIZE = 1024;

    private final AddressTranslator addressTranslator;
    private final Logger logger = LoggerFactory.getLogger();

    private DatagramSocket datagramSocket;

    public UDPSocket(AddressTranslator addressTranslator, int myId) throws SocketException {
        this.addressTranslator = addressTranslator;
        this.datagramSocket = new DatagramSocket(addressTranslator.getSocketAddress(myId));
    }

    /**
     * Send a message to the given destination
     *
     * @param message message to send
     * @param destId  destination to send message
     * @throws IOException if unable to convert message to bytes, or socket exception occurs
     */
    public void sendMessage(Message message, int destId) throws IOException {
        logger.info(String.format("Sending message to %d : %s", destId, message.toString()));
        InetSocketAddress dest = addressTranslator.getSocketAddress(destId);

        final byte[] msgBytes = message.toBytes();

        final DatagramPacket packet =
                new DatagramPacket(msgBytes, 0, msgBytes.length, dest);

        datagramSocket.send(packet);
    }

    /**
     * Poll until message is received unless timeout is reached
     *
     * @return message received from this socket
     */
    public Message receiveMessage(Integer timeoutSecs) {
        final DatagramPacket packet =
                new DatagramPacket(new byte[BUFFER_SIZE], 0, BUFFER_SIZE);

        try {
            if (timeoutSecs != null)
                datagramSocket.setSoTimeout(timeoutSecs * 1000);

            datagramSocket.receive(packet);
            final Message message = Message.fromBytes(packet.getData());
            logger.info(String.format("Received message: %s", message.toString()));
            return message;
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Close the underlying socket
     */
    public void close() {
        datagramSocket.close();
    }

    public boolean isClosed() {
        return datagramSocket.isClosed();
    }
}
