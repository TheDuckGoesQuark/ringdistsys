package config;

import node.ElectionMethod;

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

    /**
     * The election method to be used in this dist sys
     */
    private final ElectionMethod electionMethod;


    public Configuration(InetAddress hostAddress, int portNumber, int nodeId, String listFilePath, ElectionMethod electionMethod) {
        this.address = new InetSocketAddress(hostAddress, portNumber);
        this.nodeId = nodeId;
        this.listFilePath = listFilePath;
        this.electionMethod = electionMethod;
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

    public ElectionMethod getElectionMethod() {
        return electionMethod;
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
                "' | " +
                "Election Method: '" +
                electionMethod +
                "'";
    }
}
