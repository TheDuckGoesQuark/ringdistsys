package sockets;

import messages.Message;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Optional;

public class RingSocket {

    private Socket successorSocket = null;

    private Socket predecessorSocket = null;

    private InetSocketAddress myAddress;

    public RingSocket(InetSocketAddress myAddress) {
        this.myAddress = myAddress;
    }

    public void updateSuccessor(SocketAddress successorAddress) throws IOException {
        if (successorSocket == null) {
            this.successorSocket = new Socket(myAddress.getAddress(), myAddress.getPort());
        }

        if (!successorSocket.isClosed()) {
            successorSocket.close();
        }

        successorSocket.connect(successorAddress);
    }

    public void updatePredecessor(SocketAddress predecessorAddress) throws IOException {
        if (predecessorSocket != null && !predecessorSocket.isClosed()) {
            predecessorSocket.close();
        }

        ServerSocket serverSocket = new ServerSocket(myAddress.getPort(), 0, myAddress.getAddress());
        predecessorSocket = serverSocket.accept();

        serverSocket.close();
    }

    public void sendToSuccessor(Message message) throws IOException {
        successorSocket.getOutputStream().write(message.toBytes());
    }

    public Optional<Message> receiveFromPredecessor() throws IOException {
        final byte[] receivedBytes = new byte[Message.MAX_LENGTH_BYTES];
        int read = predecessorSocket.getInputStream().read(receivedBytes);

        if (read == 0) return Optional.empty();
        else return Optional.of(Message.fromBytes(receivedBytes));
    }
}

