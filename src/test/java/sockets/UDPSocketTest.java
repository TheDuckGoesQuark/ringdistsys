package sockets;

import globalpersistence.NodeRow;
import messages.Message;
import messages.MessageType;
import node.AddressTranslator;
import node.ElectionMethod;
import org.junit.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class UDPSocketTest {

    private AddressTranslator addressTranslator;

    @Before
    public void initAddressTranslator() {
        List<NodeRow> nodes = new ArrayList<>();
        nodes.add(new NodeRow("localhost", 5001, 1, null, false));
        nodes.add(new NodeRow("localhost", 5000, 0, null, false));
        addressTranslator = new AddressTranslator(nodes);
    }

    @Test
    public void sendAndReceiveMessage() throws IOException, ClassNotFoundException {
        UDPSocket srcSocket = null;
        UDPSocket destSocket = null;

        try {
            srcSocket = new UDPSocket(addressTranslator, 1);
            destSocket = new UDPSocket(addressTranslator, 0);

            Message message = new Message(MessageType.OK, ElectionMethod.RING_BASED, 1);
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