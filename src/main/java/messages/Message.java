package messages;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Message {

    public static int MAX_LENGTH_BYTES = Integer.SIZE / 8;

    private MessageType type;

    public Message(MessageType type) {
        this.type = type;
    }

    public MessageType getType() {
        return type;
    }

    public byte[] toBytes() throws IOException {
        return ByteBuffer.allocate(MAX_LENGTH_BYTES)
                .putInt(type.ordinal())
                .order(ByteOrder.BIG_ENDIAN)
                .array();
    }

    public static Message fromBytes(final byte[] bytes) {
        final MessageType type = MessageType.values()[ByteBuffer.wrap(bytes).getInt()];
        return new Message(type);
    }
}
