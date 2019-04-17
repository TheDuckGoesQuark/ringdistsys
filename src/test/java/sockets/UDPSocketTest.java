package sockets;

import messages.Message;
import messages.MessageType;
import node.AddressTranslator;
import node.ElectionMethod;
import org.junit.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class UDPSocketTest {

    private AddressTranslator addressTranslator;

    @Before
    public void initAddressTranslator() {
        Map<Integer, InetSocketAddress> map = new HashMap<>();
        map.put(0, new InetSocketAddress("localhost", 8080));
        map.put(1, new InetSocketAddress("localhost", 8081));
        addressTranslator = new AddressTranslator(map);
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