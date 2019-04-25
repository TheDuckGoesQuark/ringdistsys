package node.clientmessaging;

import logging.LoggerFactory;
import node.clientmessaging.messages.ChatMessage;
import node.clientmessaging.repositories.MessageRepository;
import node.clientmessaging.repositories.UserGroupRepository;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class SocketChatServer implements ChatServer {

    private static final int MAX_CLIENTS = 2;

    private final Logger logger = LoggerFactory.getLogger();

    private final ExecutorService threadPool = Executors.newFixedThreadPool(MAX_CLIENTS);
    private final MessageRepository messageRepository;
    private final UserGroupRepository userGroupRepository;

    private final ClientHandler[] clients = new ClientHandler[MAX_CLIENTS];
    private final Queue<ChatMessage> outgoingMessages = new ConcurrentLinkedQueue<>();
    private final ServerSocket serverSocket;

    private boolean stopped = true;

    public SocketChatServer(String hostAddress, int clientPort, MessageRepository messageRepository, UserGroupRepository userGroupRepository) throws Exception {
        this.messageRepository = messageRepository;
        this.userGroupRepository = userGroupRepository;
        this.serverSocket = new ServerSocket();
        this.serverSocket.bind(new InetSocketAddress(hostAddress, clientPort));

    }

    @Override
    public void run() {
        stopped = false;

        while (!stopped) {
            try {
                final Socket socket = serverSocket.accept();
                registerNewClient(socket);
            } catch (IOException e) {
                logger.warning("Error occurred when accepting client connection: " + e.getMessage());
            }
        }
    }

    /**
     * Checks if this server can handle another client before beginning to serve them
     *
     * @param socket connection to new potential client
     * @throws IOException
     */
    private void registerNewClient(Socket socket) throws IOException {
        final ClientHandler handler = new ClientHandler(socket, outgoingMessages, userGroupRepository);

        boolean registered = false;
        for (int i = 0; i < clients.length; i++) {
            if (clients[i] == null || !clients[i].isConnected()) {
                clients[i] = handler;
                registered = true;
                break;
            }
        }

        if (!registered) {
            socket.close();
            throw new IOException("Unable to register new client: max reached.");
        } else {
            this.threadPool.execute(handler);
        }
    }

    @Override
    public boolean receiveMessage() {
        // TODO check resource Q for incoming messages for users and their groups
        return false;
    }

    @Override
    public boolean sendMessage() {
        // TODO check outgoing queue for messages to send
        return false;
    }

    @Override
    public int getNumberOfClients() {
        int count = 0;
        for (ClientHandler client : clients) {
            if (client != null && client.isConnected()) count++;
        }
        return count;
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
