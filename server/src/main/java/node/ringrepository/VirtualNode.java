package node.ringrepository;

import java.util.Optional;

/**
 * Instance of database representation of Node
 */
public class VirtualNode {

    private final String address;
    private final int coordinatorPort;
    private final int clientPort;
    private final int nodeId;
    private final Integer successorId;
    private final boolean isCoordinator;

    public VirtualNode(String address, int coordinatorPort, int clientPort, int nodeId, Integer successorId, boolean isCoordinator) {
        this.address = address;
        this.coordinatorPort = coordinatorPort;
        this.clientPort = clientPort;
        this.nodeId = nodeId;
        this.successorId = successorId;
        this.isCoordinator = isCoordinator;
    }

    public String getAddress() {
        return address;
    }

    public int getCoordinatorPort() {
        return coordinatorPort;
    }

    public int getClientPort() {
        return clientPort;
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
                ", coordinatorPort=" + coordinatorPort +
                ", clientPort=" + clientPort +
                ", nodeId=" + nodeId +
                ", successorId=" + successorId +
                ", isCoordinator=" + isCoordinator +
                '}';
    }
}
