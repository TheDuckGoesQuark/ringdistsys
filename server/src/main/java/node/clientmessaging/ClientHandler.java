package node.clientmessaging;

import logging.LoggerFactory;
import node.clientmessaging.messages.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Optional;
import java.util.Queue;
import java.util.logging.Logger;

class ClientHandler implements Runnable {

    private static final Encoder<ClientMessage, String> ENCODER = new ClientMessageJsonEncoder();

    private final Logger logger = LoggerFactory.getLogger();
    /**
     * Socket with client connection
     */
    private final Socket clientSocket;
    /**
     * Output stream to client
     */
    private final PrintWriter out;
    /**
     * Input stream from client
     */
    private final BufferedReader in;
    /**
     * Messages to be sent to other clients/groups
     */
    private final Queue<ChatMessage> outgoingMessages;

    private User user = null;

    ClientHandler(Socket clientSocket, Queue<ChatMessage> outgoingMessages) throws IOException {
        this.clientSocket = clientSocket;
        this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        this.out = new PrintWriter(clientSocket.getOutputStream(), true);
        this.outgoingMessages = outgoingMessages;
    }

    /**
     * Polls for messages from the client
     */
    @Override
    public void run() {
        logger.info(String.format("Client connected from %s", clientSocket.getRemoteSocketAddress()));

        while (isConnected()) {
            final Optional<ClientMessage> message = receiveMessage();
            if (message.isPresent()) {
                handleMessage(message.get());
            } else {
                logger.info("Disconnected from client.");
                break;
            }
        }

        try {
            clientSocket.close();
        } catch (IOException e) {
            logger.warning("Error when closing client socket: " + e.getMessage());
        }
    }

    /**
     * @return true if the client is still connected
     */
    boolean isConnected() {
        return !clientSocket.isClosed();
    }

    /**
     * @return the user related to this client connection.
     */
    public Optional<User> getUser() {
        return Optional.ofNullable(user);
    }

    /**
     * @return true if the user has supplied a username
     */
    private boolean isLoggedIn() {
        return isConnected() && user != null;
    }

    /**
     * Handles message from client
     *
     * @param clientMessage message from client
     */
    private void handleMessage(ClientMessage clientMessage) {
        logger.info(String.format("Received message from %s: %s",
                getUser().map(User::getUsername).orElse("new user"),
                clientMessage.toString())
        );

        switch (clientMessage.getMessageType()) {
            case LOGIN:
                handleLogin((LoginMessage) clientMessage);
                break;
            case JOIN_GROUP:
                handleJoinGroup((JoinGroupMessage) clientMessage);
                break;
            case LEAVE_GROUP:
                handleLeaveGroup((LeaveGroupMessage) clientMessage);
                break;
            case CHAT_MESSAGE:
                handleChatMessage((ChatMessage) clientMessage);
                break;
            case ALIVE:
                logger.info("keepalive");
                break;
        }
    }

    private void handleChatMessage(ChatMessage clientMessage) {
        outgoingMessages.add(clientMessage);
    }

    private void handleLeaveGroup(LeaveGroupMessage clientMessage) {
        user.removeFromGroup(clientMessage.getGroup());
        sendMessage(clientMessage);
    }

    private void handleJoinGroup(JoinGroupMessage clientMessage) {
        user.addToGroup(clientMessage.getGroup());
        sendMessage(clientMessage);
    }

    private void handleLogin(LoginMessage clientMessage) {
        if (isLoggedIn()) {
            logger.warning("Already logged in.");
            sendMessage(new ErrorMessage("Already logged in."));
        } else {
            logger.info("Logging in as " + clientMessage.getUsername());
            user = new User(clientMessage.getUsername());
            // Reply with same login message for confirmation
            sendMessage(clientMessage);
        }
    }

    /**
     * Sends a message to the client
     */
    public void sendMessage(ClientMessage clientMessage) {
        logger.info(String.format("Sending message to %s: %s", user.getUsername(), clientMessage.toString()));
        out.println(ENCODER.encode(clientMessage));
    }

    /**
     * Attempts to parse JSON from string sent over socket
     *
     * @return maybe parsed message.
     */
    private Optional<ClientMessage> receiveMessage() {
        String input = null;

        try {
            input = in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (input != null) return ENCODER.decode(input);
        return Optional.empty();
    }

}
