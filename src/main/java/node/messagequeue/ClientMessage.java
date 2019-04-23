package node.messagequeue;

import java.sql.Time;

public class ClientMessage {
    /**
     * Time message was sent
     */
    private Time sentAt;
    /**
     * Name of client that sent message
     */
    private String fromName;
    /**
     * Name of client/group that message is for
     */
    private String toName;
    /**
     * Contents of message
     */
    private String messageContent;
    /**
     * If true, message is for all members that belong to the group with the name given in {@link ClientMessage#toName}.
     * If false, message is for an individual client
     */
    private boolean forGroup;

    public ClientMessage(Time sentAt, String fromName, String toName, String messageContent, boolean forGroup) {
        this.sentAt = sentAt;
        this.fromName = fromName;
        this.toName = toName;
        this.messageContent = messageContent;
        this.forGroup = forGroup;
    }

    public Time getSentAt() {
        return sentAt;
    }

    public String getFromName() {
        return fromName;
    }

    public String getToName() {
        return toName;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public boolean isForGroup() {
        return forGroup;
    }
}
