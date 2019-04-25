package chat.client.messages;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static chat.client.messages.ClientMessageType.CHAT_MESSAGE;


/**
 * Message sent by user to other user or group
 */
public class ChatMessage extends ClientMessage {
    /**
     * Time message was sent
     */
    private Instant sentAt;
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

    /**
     * Creates an instance of a group message
     */
    public static ChatMessage buildGroupMessage(String fromUsername, Instant sentAt, String toGroupName, String messageContent) {
        final ChatMessage chatMessage = new ChatMessage(sentAt, fromUsername, messageContent);
        chatMessage.setToGroup(toGroupName);
        return chatMessage;
    }

    /**
     * Creates an instance of a user message
     */
    public static ChatMessage buildUserMessage(String fromUsername, Instant sentAt, String toUsername, String messageContent) {
        final ChatMessage chatMessage = new ChatMessage(sentAt, fromUsername, messageContent);
        chatMessage.setToUsername(toUsername);
        return chatMessage;
    }

    public ChatMessage(Instant sentAt, String fromName, String contents) {
        super(CHAT_MESSAGE);
        this.sentAt = sentAt;
        this.messageContent = contents;
        this.fromName = fromName;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public void setToUsername(String toUsername) {
        this.toUsername = toUsername;
    }

    public void setToGroup(String toGroup) {
        this.toGroup = toGroup;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public Instant getSentAt() {
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
        return Optional.of(toGroup);
    }

    public boolean forGroup() {
        return getToGroup().isPresent();
    }

    public String toPrettyString() {
        if (forGroup()) {
            return String.format("Sent from user '%s' to group '%s' at %s: \n%s\n", fromName, toGroup, sentAt.atZone(ZoneId.systemDefault()).toLocalDateTime().toString(), messageContent);
        } else {
            return String.format("Sent from user '%s' at %s: \n%s\n", fromName, sentAt.atZone(ZoneId.systemDefault()).toLocalDateTime().toString(), messageContent);
        }
    }
}
