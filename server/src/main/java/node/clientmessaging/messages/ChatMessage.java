package node.clientmessaging.messages;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

import static node.clientmessaging.messages.ClientMessageType.CHAT_MESSAGE;


/**
 * Message sent by user to other user or group
 */
public class ChatMessage extends ClientMessage {
    /**
     * Time message was sent
     */
    private Timestamp sentAt;
    /**
     * Name of client that sent message
     */
    private String fromName;
    /**
     * Name of client that message is for
     */
    private String toUsername = null;
    /**
     * Name of group message was sent to
     */
    private String toGroup = null;
    /**
     * Contents of message
     */
    private String messageContent;

    public ChatMessage(Timestamp sentAt, String fromName, String toUsername, String toGroup, String messageContent) {
        super(CHAT_MESSAGE);
        this.sentAt = sentAt;
        this.fromName = fromName;
        this.toUsername = toUsername;
        this.toGroup = toGroup;
        this.messageContent = messageContent;
    }

    public Timestamp getSentAt() {
        return sentAt;
    }

    public String getFromName() {
        return fromName;
    }

    public String getToUsername() {
        return toUsername;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public Optional<String> getToGroup() {
        return Optional.ofNullable(toGroup);
    }

    public boolean forGroup() {
        return getToGroup().isPresent();
    }
}
