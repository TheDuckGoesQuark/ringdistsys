package persistence;

/**
 * Instance of database representation of Node
 */
public class NodeRow {

    private String address;
    private int port;
    private int nodeId;
    private Integer successorId;
    private boolean isCoordinator;

    public NodeRow(String address, int port, int nodeId, Integer successorId, boolean isCoordinator) {
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

    public Integer getSuccessorId() {
        return successorId;
    }

    public boolean isCoordinator() {
        return isCoordinator;
    }
}
