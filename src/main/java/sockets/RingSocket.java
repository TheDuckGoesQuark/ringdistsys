package sockets;

import messages.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.*;

public class RingSocket {

    private static final String THREAD_NAME = "RING_SOCKET";

    private final BlockingQueue<Message> outboundMessages = new LinkedBlockingQueue<>();
    private final ServerSocket serverSocket;

    private final int keepAliveSecs;
    private final int tokenHoldingTimeSecs;

    private boolean hasToken = false;

    private Socket successorSocket = null;
    private Socket predecessorSocket = null;

    private InetSocketAddress myAddress;

    public RingSocket(InetSocketAddress myAddress, int keepAliveSecs, int tokenHoldingTimeSecs) throws IOException {
        this.myAddress = myAddress;
        this.keepAliveSecs = keepAliveSecs;
        this.tokenHoldingTimeSecs = tokenHoldingTimeSecs;
        this.serverSocket = new ServerSocket(myAddress.getPort(), 1, myAddress.getAddress());
    }

    public void updateSuccessor(SocketAddress successorAddress) throws IOException {
        if (successorSocket != null && !successorSocket.isClosed()) {
            successorSocket.close();
        }

        this.successorSocket = new Socket(myAddress.getAddress(), myAddress.getPort());
    }

    public void updatePredecessor() throws IOException {
        if (predecessorSocket != null && !predecessorSocket.isClosed()) {
            predecessorSocket.close();
        }

        predecessorSocket = serverSocket.accept();
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

