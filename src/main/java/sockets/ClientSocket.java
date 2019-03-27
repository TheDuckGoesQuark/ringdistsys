package sockets;

import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;

public class ClientSocket {
    private Socket tcpSocket;

    public ClientSocket(SocketAddress socketAddress) throws SocketException {
        this.tcpSocket= new Socket(socketAddress);
    }
}
