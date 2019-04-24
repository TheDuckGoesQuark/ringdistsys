package chat.messages;

import java.time.Instant;

import static chat.messages.ClientMessageType.CHAT_MESSAGE;

/**
 * Message sent by user to other user or group
 */
public class ChatMessage extends ClientMessage {
    /**
     * Time message was sent
     */
    private Instant sentAt;
    /**
     * Name of clientmessaging that sent message
     */
    private String fromName;
    /**
     * Name of clientmessaging/group that message is for
     */
    private String toName;
    /**
     * Contents of message
     */
    private String messageContent;
    /**
     * If true, message is for all members that belong to the group with the name given in {@link ChatMessage#toName}.
     * If false, message is for an individual clientmessaging
     */
    private boolean forGroup;

    public ChatMessage(Instant sentAt, String fromName, String toName, String messageContent, boolean forGroup) {
        super(CHAT_MESSAGE);
        this.sentAt = sentAt;
        this.fromName = fromName;
        this.toName = toName;
        this.messageContent = messageContent;
        this.forGroup = forGroup;
    }

    public Instant getSentAt() {
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

    @Override
    public String toString() {
        return "ChatMessage{" +
                "sentAt=" + sentAt +
                ", fromName='" + fromName + '\'' +
                ", toName='" + toName + '\'' +
                ", messageContent='" + messageContent + '\'' +
                ", forGroup=" + forGroup +
                '}';
    }

}
