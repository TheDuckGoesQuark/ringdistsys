package chat.messages;

import static chat.messages.ClientMessageType.LOGIN;

public class LoginMessage extends ClientMessage {

    private String username;

    public LoginMessage(String username) {
        super(LOGIN);
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
