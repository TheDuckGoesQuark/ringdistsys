package node.coordinator;

import java.util.LinkedHashMap;
import java.util.Map;

class NodeRing {

    private final Map<Integer, VirtualNode> nodes = new LinkedHashMap<>();
    private VirtualNode lastAdded;

    /**
     * Initialize the node ring with a single node which is its own predecessor and successor
     *
     * @param myId id of node to add
     */
    NodeRing(int myId) {
        this.lastAdded = new VirtualNode(myId);
        this.nodes.put(myId, lastAdded);
        lastAdded.setNext(lastAdded);
        lastAdded.setPrev(lastAdded);
    }

    /**
     * Returns true if this node is already part of the ring
     *
     * @param nodeId id of node to check for
     * @return true if in ring
     */
    boolean containsNode(int nodeId) {
        return nodes.containsKey(nodeId);
    }

    /**
     * Adds the node with the given id to the ring
     *
     * @param nodeId id of node to add
     * @return VirtualNode newly added node with predecessor and successor
     */
    void addNode(int nodeId) {
        final VirtualNode node = new VirtualNode(nodeId);
        nodes.put(nodeId, node);

        insertIntoRing(node);
    }

    /**
     * Gets the node with the given id
     *
     * @param nodeId id of node to get
     * @return the node with the given id
     */
    VirtualNode getNode(int nodeId) {
        return nodes.get(nodeId);
    }

    /**
     * Inserts the given node after the last added node
     *
     * @param node node to insert
     */
    private void insertIntoRing(VirtualNode node) {
        node.setNext(lastAdded.getNext());
        node.setPrev(lastAdded);

        lastAdded.getNext().setPrev(node);
        lastAdded.setNext(node);

        lastAdded = node;
    }

    /**
     * Removes the given node from the ring and reconnects its neighbours to each other
     *
     * @param nodeId id of node to remove
     */
    void removeNode(int nodeId) {
        final VirtualNode toBeRemoved = getNode(nodeId);

        final VirtualNode before = toBeRemoved.getPrev();
        final VirtualNode after = toBeRemoved.getNext();

        before.setNext(after);
        after.setPrev(before);

        nodes.remove(toBeRemoved.getNodeId());

        if (lastAdded == toBeRemoved){
            lastAdded = after;
        }
    }
}
