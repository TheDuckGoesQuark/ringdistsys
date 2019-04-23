package node.ringstore;

import java.util.Optional;

/**
 * Instance of database representation of Node
 */
public class VirtualNode {

    private String address;
    private int port;
    private int nodeId;
    private Integer successorId;
    private boolean isCoordinator;

    public VirtualNode(String address, int port, int nodeId, Integer successorId, boolean isCoordinator) {
        this.address = address;
        this.port = port;
        this.nodeId = nodeId;
        this.successorId = successorId;
        this.isCoordinator = isCoordinator;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public int getNodeId() {
        return nodeId;
    }

    public Optional<Integer> getSuccessorId() {
        return Optional.ofNullable(successorId);
    }

    public boolean isCoordinator() {
        return isCoordinator;
    }

    @Override
    public String toString() {
        return "VirtualNode{" +
                "address='" + address + '\'' +
                ", port=" + port +
                ", nodeId=" + nodeId +
                ", successorId=" + successorId +
                ", isCoordinator=" + isCoordinator +
                '}';
    }
}
