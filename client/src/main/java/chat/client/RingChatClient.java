package chat.client;


import chat.client.messages.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

public class RingChatClient implements ChatClient {

    private final Encoder<ClientMessage, String> encoder = new ClientMessageJsonEncoder();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final LinkedBlockingQueue<ChatMessage> inboundMessages = new LinkedBlockingQueue<>();
    private final String serverAddress;
    private final int serverPort;

    private String username;
    private Set<String> groups = new HashSet<>();

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public RingChatClient(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    /**
     * Establish connection with server.
     */
    private void connect() throws IOException {
        this.socket = new Socket();
        this.socket.connect(new InetSocketAddress(serverAddress, serverPort));
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    @Override
    public void logInAs(String username) throws IOException {
        if (loggedIn()) throw new IOException("Already logged in.");

        // Connect to server
        connect();

        // Attempt to login
        final LoginMessage loginMessage = new LoginMessage(username);
        sendToServer(loginMessage);

        // Check for login confirmation
        final Optional<ClientMessage> reply = receiveMessage();

        if (reply.isPresent()) {
            handleLoginReply(reply.get());
        } else {
            System.err.println("Unable to login.");
        }
    }

    /**
     * Checks for error in login reply, and otherwise sets this client up for receiving messages
     *
     * @param clientMessage reply after login request
     */
    private void handleLoginReply(ClientMessage clientMessage) {
        if (clientMessage.getMessageType() == ClientMessageType.ERROR) {
            handleError((ErrorMessage) clientMessage);
        } else {
            // Initialize username as confirmed by server
            this.username = ((LoginMessage) clientMessage).getUsername();
            this.groups.clear();

            // Start listening for chat messages
            this.executor.submit(() -> {
                while (loggedIn()) {
                    pollForMessage();
                }
            });
        }
    }

    @Override
    public void logout() throws IOException {
        if (!loggedIn()) throw new IOException("Not logged in.");

        this.username = null;
        this.groups.clear();
        this.socket.close();
    }

    @Override
    public boolean loggedIn() {
        return username != null;
    }

    @Override
    public String getUsername() throws IOException {
        if (!loggedIn()) throw new IOException("Not logged in.");

        return username;
    }

    @Override
    public Set<String> getGroups() throws IOException {
        if (!loggedIn()) throw new IOException("Not logged in.");

        return groups;
    }

    @Override
    public void joinGroup(String groupname) throws IOException {
        if (!loggedIn()) throw new IOException("Not logged in.");

        final JoinGroupMessage joinGroupMessage = new JoinGroupMessage(groupname);
        sendToServer(joinGroupMessage);
    }

    @Override
    public void leaveGroup(String groupname) throws IOException {
        if (!loggedIn()) throw new IOException("Not logged in.");

        final LeaveGroupMessage leaveGroupMessage = new LeaveGroupMessage(groupname);
        sendToServer(leaveGroupMessage);
    }

    @Override
    public void sendMessageToGroup(String message, String groupname) throws IOException {
        if (!loggedIn()) throw new IOException("Not logged in.");

        final ChatMessage chatMessage = ChatMessage.buildGroupMessage(this.username, Timestamp.from(Instant.now()), groupname, message);
        sendToServer(chatMessage);
    }

    @Override
    public void sendMessageToUser(String message, String username) throws IOException {
        if (!loggedIn()) throw new IOException("Not logged in.");

        final ChatMessage chatMessage = ChatMessage.buildUserMessage(this.username, Timestamp.from(Instant.now()), username, message);
        sendToServer(chatMessage);
    }

    @Override
    public List<ChatMessage> receiveMessages() throws IOException {
        if (!loggedIn()) throw new IOException("Not logged in.");

        final List<ChatMessage> receivedMessages = new ArrayList<>();
        inboundMessages.drainTo(receivedMessages);
        return receivedMessages;
    }

    private void pollForMessage() {
        final Optional<ClientMessage> message = receiveMessage();

        if (message.isPresent()) {
            System.out.println("Received message: " + message.get().toString());
            switch (message.get().getMessageType()) {
                case CHAT_MESSAGE:
                    inboundMessages.add((ChatMessage) message.get());
                    break;
                case JOIN_GROUP:
                    handleGroupJoinReply((JoinGroupMessage) message.get());
                    break;
                case LEAVE_GROUP:
                    handleLeaveGroupReply((LeaveGroupMessage) message.get());
                    break;
                case ERROR:
                    handleError((ErrorMessage) message.get());
                    break;
            }
        } else {
            if (loggedIn()) {
                try {
                    logout();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Informs user that they have joined a group
     */
    private void handleGroupJoinReply(JoinGroupMessage joinGroupMessage) {
        System.out.println("Successfully joined group " + joinGroupMessage.getGroup());
        groups.add(joinGroupMessage.getGroup());
    }

    /**
     * Informs user that they have left a group
     */
    private void handleLeaveGroupReply(LeaveGroupMessage leaveGroupMessage) {
        System.out.println("Successfully left group " + leaveGroupMessage.getGroup());
        groups.remove(leaveGroupMessage.getGroup());
    }

    private void handleError(ErrorMessage errorMessage) {
        System.err.println(errorMessage.getErrorMessage());
    }

    /**
     * reads message from socket and parses JSON to client message
     *
     * @return client message from server
     */
    private Optional<ClientMessage> receiveMessage() {
        String input = null;

        try {
            input = in.readLine();
        } catch (IOException ignored) {
        }

        if (input != null) return encoder.decode(input);
        return Optional.empty();
    }

    /**
     * @param message message to send to server
     */
    private void sendToServer(ClientMessage message) {
        final String jsonstring = encoder.encode(message);
        System.out.println("Sending to server: " + jsonstring);
        out.println(jsonstring);
    }
}
