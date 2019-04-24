package node.clientmessaging.messages;

public class ClientMessage {

    private ClientMessageType messageType;

    public ClientMessage() {
    }

    public ClientMessage(ClientMessageType messageType) {
        this.messageType = messageType;
    }

    public ClientMessageType getMessageType() {
        return messageType;
    }

    @Override
    public String toString() {
        return "ClientMessage{" +
                "messageType=" + messageType +
                '}';
    }
}
