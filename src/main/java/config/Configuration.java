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

    /**
     * Path to file containing information about group members
     */
    private final String listFilePath;

    public Configuration(InetAddress hostAddress, int portNumber, int nodeId, String listFilePath) {
        this.address = new InetSocketAddress(hostAddress, portNumber);
        this.nodeId = nodeId;
        this.listFilePath = listFilePath;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public int getNodeId() {
        return nodeId;
    }

    public String getListFilePath() {
        return listFilePath;
    }

    @Override
    public String toString() {
        return "Address: '" +
                address.toString() +
                "' | " +
                "Node ID: '" +
                nodeId +
                "' | " +
                "List File Path: '" +
                listFilePath +
                "'";
    }
}
