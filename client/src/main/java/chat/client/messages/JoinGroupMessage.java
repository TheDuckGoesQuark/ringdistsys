package chat.client.messages;


import static chat.client.messages.ClientMessageType.JOIN_GROUP;

public class JoinGroupMessage extends ClientMessage {

    private String group;

    public JoinGroupMessage(String group) {
        super(JOIN_GROUP);
        this.group = group;
    }

    public String getGroup() {
        return group;
    }

    @Override
    public String toString() {
        return "JoinGroupMessage{" +
                "group='" + group + '\'' +
                '}';
    }
}
