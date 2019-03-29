package messages;

import node.ElectionMethod;
import org.junit.Test;

import javax.management.relation.RoleInfoNotFoundException;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import static org.junit.Assert.*;

public class MessageTest {

    @Test
    public void toAndFromBytes() throws IOException, ClassNotFoundException {
        Message message = new Message(MessageType.OK, ElectionMethod.RING_BASED, 1);
        byte[] bytes = message.toBytes();

        Message recoveredMessage = Message.fromBytes(bytes);
        assertEquals(MessageType.OK, recoveredMessage.getType());
        assertEquals(ElectionMethod.RING_BASED, recoveredMessage.getElectionMethod());
        assertEquals(1, recoveredMessage.getSrcId());
    }

    // TODO to and from when contains message
}