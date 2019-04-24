package chat.client;

import chat.messages.ClientMessage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Optional;
import java.util.Set;

public class RingChatClient implements ChatClient {

    private final Socket socket;

    private String username;
    private Set<String> groups;

    public RingChatClient(String serverAddress, int serverPort) throws IOException {
        this.socket = new Socket();
        this.socket.connect(new InetSocketAddress(serverAddress, serverPort));
    }

    @Override
    public void logInAs(String username) throws IOException {

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
