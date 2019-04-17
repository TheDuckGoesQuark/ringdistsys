package node.coordinator;

public class VirtualNode {
    private int nodeId;
    private VirtualNode prev;
    private VirtualNode next;

    VirtualNode(int nodeId) {
        this.nodeId = nodeId;
    }

    int getNodeId() {
        return nodeId;
    }

    VirtualNode getPrev() {
        return prev;
    }

    void setPrev(VirtualNode prev) {
        this.prev = prev;
    }

    VirtualNode getNext() {
        return next;
    }

    void setNext(VirtualNode next) {
        this.next = next;
    }
}
