package sockets;

import messages.Message;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

public class RingSocket {

    private final ServerSocket serverSocket;
    private final Logger logger;

    private Socket successorSocket = null;
    private Socket predecessorSocket = null;

    public RingSocket(InetSocketAddress myAddress, Logger logger) throws IOException {
        this.serverSocket = new ServerSocket(myAddress.getPort(), 1, myAddress.getAddress());
        this.logger = logger;
    }

    public void updateSuccessor(InetSocketAddress successorAddress) throws IOException {
        logger.info(String.format("Updating successor to %s", successorAddress.toString()));
        if (successorSocket != null && !successorSocket.isClosed()) {
            successorSocket.close();
        }

        this.successorSocket = new Socket(successorAddress.getAddress(), successorAddress.getPort());
    }

    public void updatePredecessor() throws IOException {
        if (predecessorSocket != null && !predecessorSocket.isClosed()) {
            predecessorSocket.close();
        }

        logger.info("Waiting on predecessor connection");
        predecessorSocket = serverSocket.accept();
        logger.info(String.format("Predecessor connected from address to %s", predecessorSocket.getInetAddress().toString()));
    }

    public void sendToSuccessor(Message message) throws IOException {
        final OutputStream out = successorSocket.getOutputStream();
        out.write(message.toBytes());
        out.flush();
    }

    public Message receiveFromPredecessor() throws IOException, ClassNotFoundException {
        final ObjectInputStream out = new ObjectInputStream(predecessorSocket.getInputStream());
        return (Message) out.readObject();
    }

    public void close() throws IOException {
        if (this.successorSocket != null)
            this.successorSocket.close();

        if (this.predecessorSocket!= null)
            this.predecessorSocket.close();

        if (this.serverSocket != null)
            this.serverSocket.close();
    }
}

