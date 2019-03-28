package messages;

import java.io.*;

public class Message implements Serializable {

    private final MessageType type;
    private final Object payload;

    public Message(MessageType type) {
        this.type = type;
        this.payload = null;
    }

    public Message(MessageType type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    public MessageType getType() {
        return type;
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
}
