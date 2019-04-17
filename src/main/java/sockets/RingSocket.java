package sockets;

import logging.LoggerFactory;
import messages.Message;
import node.AddressTranslator;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

public class RingSocket {

    private final Logger logger = LoggerFactory.getLogger();
    private final AddressTranslator addressTranslator;
    private final ServerSocket serverSocket;

    private Socket successorSocket = null;
    private Socket predecessorSocket = null;

    public RingSocket(int myId, AddressTranslator addressTranslator) throws IOException {
        final InetSocketAddress myAddress = addressTranslator.getSocketAddress(myId);

        this.addressTranslator = addressTranslator;
        this.serverSocket = new ServerSocket(myAddress.getPort(), 1, myAddress.getAddress());
    }

    public void updateSuccessor(int successorId) throws IOException {
        final InetSocketAddress successorAddress = addressTranslator.getSocketAddress(successorId);

        logger.info(String.format("Updating successor to %s", successorAddress.toString()));
        if (successorSocket != null && !successorSocket.isClosed()) {
            successorSocket.close();
        }

        this.successorSocket = new Socket(successorAddress.getAddress(), successorAddress.getPort());
        this.successorSocket.setKeepAlive(true);
        this.successorSocket.setTcpNoDelay(true);
    }

    public void updatePredecessor() throws IOException {
        if (predecessorSocket != null && !predecessorSocket.isClosed()) {
            predecessorSocket.close();
        }

        logger.info("Waiting on predecessor connection");
        predecessorSocket = serverSocket.accept();
        logger.info(String.format("Predecessor connected from address to %s", predecessorSocket.getLocalSocketAddress().toString()));
    }

    private void sendToSocket(Message message, Socket socket) throws IOException {
        final OutputStream out = socket.getOutputStream();
        out.write(message.toBytes());
        out.flush();
    }

    public void sendToSuccessor(Message message) throws IOException {
        sendToSocket(message, successorSocket);
    }

    public void sendToPredeccesor(Message message) throws IOException {
        sendToSocket(message, predecessorSocket);
    }

    private Message readFromSocket(Socket socket, Integer timeoutSecs) {
        try {
            if (timeoutSecs != null) socket.setSoTimeout(timeoutSecs * 1000);

            final ObjectInputStream out = new ObjectInputStream(socket.getInputStream());
            return (Message) out.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }

    public Message receiveFromSuccessor(int timeoutSecs) {
        return this.readFromSocket(successorSocket, timeoutSecs);
    }

    public Message receiveFromPredecessor(Integer timeoutSecs) {
        return this.readFromSocket(predecessorSocket, timeoutSecs);
    }

    public void close() throws IOException {
        if (this.successorSocket != null)
            this.successorSocket.close();

        if (this.predecessorSocket != null)
            this.predecessorSocket.close();

        if (this.serverSocket != null)
            this.serverSocket.close();
    }

    /**
     * Checks if this ring network consists of just this node
     *
     * @return true if the predecessor socket == successor socket
     */
    public boolean isClosedLoop() {
        return successorSocket.getLocalSocketAddress().equals(predecessorSocket.getRemoteSocketAddress());
    }
}

