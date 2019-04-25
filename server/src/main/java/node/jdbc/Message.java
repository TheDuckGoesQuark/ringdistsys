package node.jdbc;

import java.sql.Timestamp;

/**
 * Database row from messages table
 */
public class Message {

    private int messageId;
    private Timestamp sentAt;
    private String contents;
    private String fromUsername;
    private String toGroup;

    public Message(Timestamp sentAt, String contents, String fromUsername) {
        this.sentAt = sentAt;
        this.contents = contents;
        this.fromUsername = fromUsername;
    }

    public Message(Timestamp sentAt, String contents, String fromUsername, String toGroup) {
        this.sentAt = sentAt;
        this.contents = contents;
        this.fromUsername = fromUsername;
        this.toGroup = toGroup;
    }

    public Message(Timestamp sentAt, String contents, String fromUsername, String toGroup, int messageId) {
        this.messageId = messageId;
        this.sentAt = sentAt;
        this.contents = contents;
        this.fromUsername = fromUsername;
        this.toGroup = toGroup;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public Timestamp getSentAt() {
        return sentAt;
    }

    public String getContents() {
        return contents;
    }

    public String getFromUsername() {
        return fromUsername;
    }

    public String getToGroup() {
        return toGroup;
    }

    public void setToGroup(String toGroup) {
        this.toGroup = toGroup;
    }

}
