package sockets;

import messages.Message;
import messages.MessageType;
import node.ElectionMethod;
import org.junit.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;

import static org.junit.Assert.*;

public class UDPSocketTest {

    @Test
    public void sendAndReceiveMessage() throws IOException, ClassNotFoundException {
        UDPSocket srcSocket = null;
        UDPSocket destSocket = null;

        try {
            srcSocket = new UDPSocket(new InetSocketAddress("localhost", 8081));
            destSocket = new UDPSocket(new InetSocketAddress("localhost", 8080));

            Message message = new Message(MessageType.OK, ElectionMethod.RING_BASED, 1);
            srcSocket.sendMessage(message, new InetSocketAddress("localhost", 8080));

            Message receivedMessage = destSocket.receiveMessage(0);
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
        UDPSocket socket = new UDPSocket(new InetSocketAddress("localhost", 8081));
        assertFalse(socket.isClosed());

        socket.close();
        assertTrue(socket.isClosed());
    }
}