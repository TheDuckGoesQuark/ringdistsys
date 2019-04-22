package messages;

import node.electionhandlers.ElectionMethod;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class MessageTest {

    @Test
    public void toAndFromBytes() throws IOException, ClassNotFoundException {
        Message message = new Message(MessageType.JOIN, 1);
        byte[] bytes = message.toBytes();

        Message recoveredMessage = Message.fromBytes(bytes);
        assertEquals(MessageType.JOIN, recoveredMessage.getType());
        assertEquals(1, recoveredMessage.getSrcId());
    }

    // TODO to and from when contains message
}