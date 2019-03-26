package config;

import java.net.InetAddress;

public class Configuration {

    /**
     * The host address of this node
     */
    private InetAddress hostAddress;
    /**
     * The port number used by sockets for this node
     */
    private int portNumber;
    /**
     * The ID of this node, unique to the system
     */
    private int nodeId;

    public Configuration(InetAddress hostAddress, int portNumber, int nodeId) {
        this.hostAddress = hostAddress;
        this.portNumber = portNumber;
        this.nodeId = nodeId;
    }

    public InetAddress getHostAddress() {
        return hostAddress;
    }

    public void setHostAddress(InetAddress hostAddress) {
        this.hostAddress = hostAddress;
    }

    public int getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(int portNumber) {
        this.portNumber = portNumber;
    }

    public int getNodeId() {
        return nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }
}
