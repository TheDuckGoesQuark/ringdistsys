package sockets;

import messages.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

public class RingSocket {

    private boolean hasToken = false;

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
        successorSocket.setKeepAlive(true);
    }

    public void updatePredecessor() throws IOException {
        if (predecessorSocket != null && !predecessorSocket.isClosed()) {
            predecessorSocket.close();
        }

        ServerSocket serverSocket = new ServerSocket(myAddress.getPort(), 0, myAddress.getAddress());
        predecessorSocket = serverSocket.accept();
        predecessorSocket.setKeepAlive(true);

        serverSocket.close();
    }

    public void sendToSuccessor(Message message) throws IOException {
        final OutputStream out = successorSocket.getOutputStream();
        out.write(message.toBytes());
        out.flush();
    }

    public Message receiveFromPredecessor() throws IOException, ClassNotFoundException {
        return (Message) new ObjectInputStream(predecessorSocket.getInputStream()).readObject();
    }
}

