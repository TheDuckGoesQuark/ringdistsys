package globalpersistence;

import java.util.List;

public interface RingStore {

    /**
     * Initializes database by inserting local node list if its empty for some reason
     */
    void initialize();

    /**
     * Gets the list of all nodes
     *
     * @return list of all nodes
     */
    List<NodeRow> getAllNodes();

    /**
     * Assigns a new coordinator
     *
     * @param newCoordinatorId id of new coordinator
     */
    void updateCoordinator(int newCoordinatorId);

    /**
     * Assigns the successorId to the node with the given id
     *
     * @param nodeId      id of node to be assigned successor
     * @param successorId id of successor node
     */
    void setNodeSuccessor(int nodeId, int successorId);

    /**
     * Removes the successor assignment for the given node, effectively removing it from the ring
     *
     * @param nodeId id of node to remove successor
     */
    void removeNodeSuccessor(int nodeId);


    // TODO implement with JDBC
}
