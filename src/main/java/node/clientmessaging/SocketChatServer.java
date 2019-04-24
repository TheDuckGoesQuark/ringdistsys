package node.clientmessaging;

import logging.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class SocketChatServer implements ChatServer {

    private final Logger logger = LoggerFactory.getLogger();
    private final ServerSocket serverSocket;
    private final ExecutorService threadPool = Executors.newFixedThreadPool(2);

    private boolean stopped = true;

    public SocketChatServer(String hostAddress, int clientPort) throws Exception {
        serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress(hostAddress, clientPort));
    }

    @Override
    public void run() {
        stopped = false;
        while (!stopped) {
            try {
                final ClientHandler handler = new ClientHandler(serverSocket.accept());
                this.threadPool.execute(handler);
            } catch (IOException e) {
                logger.warning("Error occurred when accepting client connection: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean receiveMessage() {
        return false;
    }

    @Override
    public boolean sendMessage() {
        return false;
    }

    @Override
    public int getNumberOfClients() {
        return 0;
    }

    @Override
    public void cleanup() {
        try {
            stopped = true;
            serverSocket.close();
            threadPool.shutdown();
        } catch (IOException e) {
            logger.warning("Error occurred while closing server socket: " + e.getMessage());
        }
    }

}
