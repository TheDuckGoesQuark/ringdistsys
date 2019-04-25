package node.jdbc;

public class MessageDestination {

    private final int messageId;
    private final String toUsername;

    public MessageDestination(int messageId, String toUsername) {
        this.messageId = messageId;
        this.toUsername = toUsername;
    }

    public int getMessageId() {
        return messageId;
    }

    public String getToUsername() {
        return toUsername;
    }
}
