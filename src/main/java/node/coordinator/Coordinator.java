package node.coordinator;

import messages.Message;
import messages.MessageType;
import messages.SuccessorMessage;
import node.ElectionMethod;
import sockets.UDPSocket;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Provides handlers for coordinator related messages
 */
public class Coordinator {

    private final NodeRing nodeRing;
    private final int myId;
    private final Logger logger;

    private UDPSocket udpSocket;

    public Coordinator(UDPSocket udpSocket, Logger logger, int myId) {
        this.logger = logger;
        this.udpSocket = udpSocket;
        this.myId = myId;
        this.nodeRing = new NodeRing(myId);
    }

    private void sendSuccessor(ElectionMethod electionMethod, int destId, int successorId) {
        final Message successorReply
                = new Message(MessageType.SUCCESSOR, electionMethod, myId, new SuccessorMessage(successorId));

        try {
            this.udpSocket.sendMessage(successorReply, destId);
        } catch (IOException e) {
            logger.warning("Unable to send message");
            logger.warning(e.getMessage());
        }
    }

    public void handleJoinRequest(Message message) {
        final int srcId = message.getSrcId();

        nodeRing.addNode(srcId);
        final VirtualNode node = nodeRing.getNode(srcId);

        // Assign successor to requesting node
        sendSuccessor(message.getElectionMethod(), srcId, node.getNext().getNodeId());

        // Reassign new successor to previous node (unless there is only one node)
        if (srcId != node.getPrev().getNodeId()) {
            sendSuccessor(message.getElectionMethod(), node.getPrev().getNodeId(), node.getNodeId());
        }
    }

    public void handleSuccessorRequest(Message message) {
        final int srcId = message.getSrcId();

        final VirtualNode node = nodeRing.getNode(srcId);
        nodeRing.removeNode(node.getNext().getNodeId());

        // Assign successor to requesting node
        sendSuccessor(message.getElectionMethod(), srcId, node.getNext().getNodeId());
    }
}
