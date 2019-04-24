package chat.client.messages;


import static chat.client.messages.ClientMessageType.LEAVE_GROUP;

public class LeaveGroupMessage extends ClientMessage {

    private String group;

    public LeaveGroupMessage(String group) {
        super(LEAVE_GROUP);
        this.group = group;
    }

    public String getGroup() {
        return group;
    }

    @Override
    public String toString() {
        return "LeaveGroupMessage{" +
                "group='" + group + '\'' +
                '}';
    }
}
