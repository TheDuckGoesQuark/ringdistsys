package node.coordinator;

import org.junit.Test;

import static org.junit.Assert.*;

public class NodeRingTest {

    @Test
    public void containsNodeWhenPresent() {
        NodeRing nodeRing = new NodeRing(1);

        assertTrue(nodeRing.containsNode(1));
    }

    @Test
    public void containsNodeWhenNotPresent() {
        NodeRing nodeRing = new NodeRing(1);

        assertFalse(nodeRing.containsNode(2));
    }


    @Test
    public void constructor() {
        NodeRing nodeRing = new NodeRing(1);

        assertTrue(nodeRing.containsNode(1));
        Node node = nodeRing.getNode(1);

        assertEquals(node, node.getNext());
        assertEquals(node, node.getPrev());
    }

    @Test
    public void addNode() {
        NodeRing nodeRing = new NodeRing(1);

        nodeRing.addNode(2);

        Node first = nodeRing.getNode(1);
        Node second = nodeRing.getNode(2);

        assertEquals(first.getNext(), second);
        assertEquals(first.getPrev(), second);

        assertEquals(second.getNext(), first);
        assertEquals(second.getPrev(), first);
    }

    @Test
    public void addNodeWhenTwo() {
        NodeRing nodeRing = new NodeRing(1);
        nodeRing.addNode(2);
        nodeRing.addNode(3);

        Node first = nodeRing.getNode(1);
        Node second = nodeRing.getNode(2);
        Node third = nodeRing.getNode(3);

        assertEquals(first.getNext(), second);
        assertEquals(first.getPrev(), third);

        assertEquals(second.getNext(), third);
        assertEquals(second.getPrev(), first);

        assertEquals(third.getNext(), first);
        assertEquals(third.getPrev(), second);
    }

    @Test
    public void addNodeWhenThree() {
        NodeRing nodeRing = new NodeRing(1);
        nodeRing.addNode(2);
        nodeRing.addNode(3);
        nodeRing.addNode(4);

        Node first = nodeRing.getNode(1);
        Node second = nodeRing.getNode(2);
        Node third = nodeRing.getNode(3);
        Node fourth = nodeRing.getNode(4);

        assertEquals(first.getPrev(), fourth);
        assertEquals(first.getNext(), second);

        assertEquals(second.getPrev(), first);
        assertEquals(second.getNext(), third);

        assertEquals(third.getPrev(), second);
        assertEquals(third.getNext(), fourth);

        assertEquals(fourth.getPrev(), third);
        assertEquals(fourth.getNext(), first);
    }

    @Test
    public void removeNodeWhenTwoLastAdded() {
        NodeRing nodeRing = new NodeRing(1);
        nodeRing.addNode(2);
        nodeRing.removeNode(2);

        Node first = nodeRing.getNode(1);

        assertEquals(first.getNext(), first);
        assertEquals(first.getPrev(), first) ;
    }

    @Test
    public void removeNodeWhenTwoNotLastAdded() {
        NodeRing nodeRing = new NodeRing(1);
        nodeRing.addNode(2);
        nodeRing.removeNode(1);

        Node first = nodeRing.getNode(2);

        assertEquals(first.getNext(), first);
        assertEquals(first.getPrev(), first) ;
    }

    @Test
    public void removeNodeWhenThreeLastAdded() {
        NodeRing nodeRing = new NodeRing(1);
        nodeRing.addNode(2);
        nodeRing.addNode(3);
        nodeRing.removeNode(3);

        Node first = nodeRing.getNode(1);
        Node second = nodeRing.getNode(2);

        assertEquals(first.getPrev(), second);
        assertEquals(first.getNext(), second);

        assertEquals(second.getPrev(), first);
        assertEquals(second.getNext(), first);
    }

    @Test
    public void removeNodeWhenThreeNotLastAdded() {
        NodeRing nodeRing = new NodeRing(1);
        nodeRing.addNode(2);
        nodeRing.addNode(3);
        nodeRing.removeNode(2);

        Node first = nodeRing.getNode(1);
        Node third = nodeRing.getNode(3);

        assertEquals(first.getPrev(), third);
        assertEquals(first.getNext(), third);

        assertEquals(third.getPrev(), first);
        assertEquals(third.getNext(), first);
    }
}