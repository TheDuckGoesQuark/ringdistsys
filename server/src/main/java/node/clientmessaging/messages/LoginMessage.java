package node.clientmessaging.messages;

import static node.clientmessaging.messages.ClientMessageType.LOGIN;

public class LoginMessage extends ClientMessage {

    private String username;

    public LoginMessage(String username) {
        super(LOGIN);
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public String toString() {
        return "LoginMessage{" +
                "username='" + username + '\'' +
                '}';
    }
}
