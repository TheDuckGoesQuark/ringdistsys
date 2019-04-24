package chat.client;

import chat.messages.ClientMessage;
import chat.messages.ClientMessageJsonEncoder;
import chat.messages.Encoder;
import chat.messages.LoginMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Optional;
import java.util.Set;

public class RingChatClient implements ChatClient {

    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;
    private final Encoder<ClientMessage, String> encoder = new ClientMessageJsonEncoder();

    private String username;
    private Set<String> groups;

    public RingChatClient(String serverAddress, int serverPort) throws IOException {
        this.socket = new Socket();
        this.socket.connect(new InetSocketAddress(serverAddress, serverPort));
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    private void sendToServer(ClientMessage message) {
        final String jsonstring = encoder.encode(message);
        System.out.println("Sending to server: " + jsonstring);
        out.println(jsonstring);
    }


    @Override
    public void logInAs(String username) throws IOException {
        final LoginMessage loginMessage = new LoginMessage(username);
        sendToServer(loginMessage);
    }

    @Override
    public void logout() throws IOException {

    }

    @Override
    public boolean loggedIn() {
        return false;
    }

    @Override
    public Optional<String> getUsername() {
        return Optional.empty();
    }

    @Override
    public void joinGroup(String groupname) throws IOException {

    }

    @Override
    public void leaveGroup(String groupname) throws IOException {

    }

    @Override
    public void sendMessageToGroup(String message, String groupname) throws IOException {

    }

    @Override
    public void sendMessageToUser(String message, String username) {

    }

    @Override
    public ClientMessage receiveMessage() throws IOException {
        return null;
    }

}
