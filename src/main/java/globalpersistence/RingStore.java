package globalpersistence;

import java.util.List;
import java.util.Optional;

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

    /**
     * Gets a list of all nodes with successors assigned (i.e. those that are part of the ring.)
     *
     * @return list of nodes with successors assigned
     */
    List<NodeRow> getAllNodesWithSuccessors();

    /**
     * Performs the following:
     * Succ(newNode) = successorId
     * Succ(predecessor) = newNode
     * Which effectively inserts the new node into the ring.
     *
     * @param predecessorId node to come before the new node
     * @param successorId   node to come after the new node
     * @param newNodeId     node to insert
     */
    void insertIntoRing(int predecessorId, int successorId, int newNodeId);

    /**
     * Performs the following:
     * Succ(predecessor) = successorId
     * Succ(nodeToRemove) = NULL
     * Which effectively removes the node from the ring.
     *
     * @param predecessorId node needing new successor
     * @param successorId   node to be assigned as succ(predecessor)
     * @param nodeToRemove  node to be removed from ring
     */
    void removeFromRing(int predecessorId, int successorId, int nodeToRemove);
}
