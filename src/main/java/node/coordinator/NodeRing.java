package node.coordinator;

import java.util.LinkedHashMap;
import java.util.Map;

public class NodeRing {

    private final Map<Integer, Node> nodes = new LinkedHashMap<>();
    private Node lastAdded;

    /**
     * Initialize the node ring with a single node which is its own predecessor and successor
     *
     * @param myId id of node to add
     */
    public NodeRing(int myId) {
        this.lastAdded = new Node(myId);
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
    public boolean containsNode(int nodeId) {
        return nodes.containsKey(nodeId);
    }

    /**
     * Adds the node with the given id to the ring
     *
     * @param nodeId id of node to add
     * @return Node newly added node with predecessor and successor
     */
    public void addNode(int nodeId) {
        final Node node = new Node(nodeId);
        nodes.put(nodeId, node);

        insertIntoRing(node);
    }

    /**
     * Gets the node with the given id
     *
     * @param nodeId id of node to get
     * @return the node with the given id
     */
    public Node getNode(int nodeId) {
        return nodes.get(nodeId);
    }

    /**
     * Inserts the given node after the last added node
     *
     * @param node node to insert
     */
    private void insertIntoRing(Node node) {
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
    public void removeNode(int nodeId) {
        final Node toBeRemoved = getNode(nodeId);

        final Node before = toBeRemoved.getPrev();
        final Node after = toBeRemoved.getNext();

        before.setNext(after);
        after.setPrev(before);

        nodes.remove(toBeRemoved.getNodeId());

        if (lastAdded == toBeRemoved){
            lastAdded = after;
        }
    }
}
