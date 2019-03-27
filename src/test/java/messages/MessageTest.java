package messages;

import org.junit.Test;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import static org.junit.Assert.*;

public class MessageTest {

    @Test
    public void toAndFromBytes() throws IOException {
        Message message = new Message(MessageType.OK);
        byte[] bytes = message.toBytes();

        Message recoveredMessage = Message.fromBytes(bytes);
        assertEquals(MessageType.OK, recoveredMessage.getType());
    }

    @Test
    public void fromBytesWhenBufferSizeFour() {
        byte[] small = new byte[4];
        Message recoveredMessage = Message.fromBytes(small);
        assertEquals(MessageType.values()[0], recoveredMessage.getType());
    }

    @Test(expected = BufferUnderflowException.class)
    public void fromBytesWhenBufferSizeZero() {
        byte[] small = new byte[0];
        Message recoveredMessage = Message.fromBytes(small);
    }

    @Test(expected = BufferUnderflowException.class)
    public void fromBytesWhenBufferSizeOne() {
        byte[] small = new byte[1];
        Message recoveredMessage = Message.fromBytes(small);
    }

    @Test(expected = BufferOverflowException.class)
    public void fromBytesWhenBufferSizeFive() {
        byte[] small = new byte[5];
        Message recoveredMessage = Message.fromBytes(small);
    }
}