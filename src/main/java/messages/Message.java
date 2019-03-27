package messages;

import java.io.*;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
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

    public static Message fromBytes(final byte[] bytes) throws BufferUnderflowException, BufferOverflowException {
        if (bytes.length > MAX_LENGTH_BYTES)
            throw new BufferOverflowException();

        int ordinalVal = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getInt();
        System.out.println(ordinalVal);
        final MessageType type = MessageType.values()[ordinalVal];
        return new Message(type);
    }
}
