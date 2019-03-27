package config;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class Configuration {

    /**
     * The socket address used by this node for communication
     */
    private final InetSocketAddress address;

    /**
     * The ID of this node, unique to the system
     */
    private final int nodeId;

    public Configuration(InetAddress hostAddress, int portNumber, int nodeId) {
        this.address = new InetSocketAddress(hostAddress, portNumber);
        this.nodeId = nodeId;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public int getNodeId() {
        return nodeId;
    }

    @Override
    public String toString() {
        return "Address: '" +
                address.toString() +
                "' | " +
                "Node ID: '" +
                nodeId +
                "'";
    }
}
