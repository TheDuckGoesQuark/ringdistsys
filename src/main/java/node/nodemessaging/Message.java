package node.nodemessaging;

import java.io.*;

public class Message implements Serializable {

    /**
     * Type of message being sent
     */
    private final MessageType type;
    /**
     * ID of node that sent message
     */
    private final int srcId;

    /**
     * Payload of message
     */
    private final Object payload;

    public Message(MessageType type, int srcId) {
        this.type = type;
        this.srcId = srcId;
        this.payload = null;
    }

    public Message(MessageType type, int srcId, Object payload) {
        this.type = type;
        this.srcId = srcId;
        this.payload = payload;
    }

    public MessageType getType() {
        return type;
    }

    public int getSrcId() {
        return srcId;
    }

    public <T> T getPayload(Class<T> clazz) {
        return (T) payload;
    }

    public byte[] toBytes() throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            final ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(this);
            out.flush();

            return bos.toByteArray();
        }
    }

    public static Message fromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
        final ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        try (ObjectInput in = new ObjectInputStream(bis)) {
            return (Message) in.readObject();
        }
    }

    @Override
    public String toString() {
        final String payloadStr = payload == null ? "null" : payload.toString();
        return "Message{" +
                "type=" + type +
                ", srcId=" + srcId +
                ", payload=" + payload +
                '}';
    }
}
