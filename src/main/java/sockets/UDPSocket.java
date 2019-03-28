package sockets;

import messages.Message;

import java.io.IOException;
import java.net.*;

public class UDPSocket {

    private static final int BUFFER_SIZE = 1024;

    private DatagramSocket datagramSocket;

    public UDPSocket(SocketAddress socketAddress) throws SocketException {
        this.datagramSocket = new DatagramSocket(socketAddress);
    }

    /**
     * Send a message to the given destination
     *
     * @param message message to send
     * @param dest    destination to send message
     * @throws IOException if unable to convert message to bytes, or socket exception occurs
     */
    public void sendMessage(Message message, SocketAddress dest) throws IOException {
        final byte[] msgBytes = message.toBytes();

        final DatagramPacket packet =
                new DatagramPacket(msgBytes, 0, msgBytes.length, dest);

        datagramSocket.send(packet);
    }

    /**
     * Poll until message is received
     *
     * @return message received from this socket
     */
    public Message receiveMessage() throws IOException, ClassNotFoundException {
        final DatagramPacket packet =
                new DatagramPacket(new byte[BUFFER_SIZE], 0, BUFFER_SIZE);

        this.datagramSocket.receive(packet);

        return Message.fromBytes(packet.getData());
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
