package node.sockets;

import node.ringstore.VirtualNode;
import node.nodemessaging.Message;
import node.nodemessaging.MessageType;
import node.AddressTranslator;
import org.junit.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class UDPSocketTest {

    private AddressTranslator addressTranslator;

    @Before
    public void initAddressTranslator() {
        List<VirtualNode> nodes = new ArrayList<>();
        nodes.add(new VirtualNode("localhost", 5001, 8001, 1, null, false));
        nodes.add(new VirtualNode("localhost", 5000, 8002, 0, null, false));
        addressTranslator = new AddressTranslator(nodes);
    }

    @Test
    public void sendAndReceiveMessage() throws IOException {
        UDPSocket srcSocket = null;
        UDPSocket destSocket = null;

        try {
            srcSocket = new UDPSocket(addressTranslator, 1);
            destSocket = new UDPSocket(addressTranslator, 0);

            Message message = new Message(MessageType.JOIN, 1);
            srcSocket.sendMessage(message, 0);

            Message receivedMessage = destSocket.receiveMessage(1);
            assertEquals(message.getType(), receivedMessage.getType());
        } finally {
            if (srcSocket != null)
                srcSocket.close();
            if (destSocket != null)
                destSocket.close();
        }
    }

    @Test
    public void closeSocket() throws IOException {
        UDPSocket socket = new UDPSocket(addressTranslator, 1);
        assertFalse(socket.isClosed());

        socket.close();
        assertTrue(socket.isClosed());
    }
}