package node.coordinator;

import messages.Message;
import messages.MessageType;
import messages.SuccessorMessage;
import node.ElectionMethod;
import sockets.Switch;
import util.StoppableThread;

import java.io.IOException;
import java.util.Optional;
import java.util.logging.Logger;

public class Coordinator extends StoppableThread {

    private static final String THREAD_NAME = "COORDINATOR_THREAD";
    private final NodeRing nodeRing;
    private final int myId;

    private Switch messageSwitch;
    private boolean running = false;

    public Coordinator(Switch messageSwitch, Logger logger, int myId) {
        super(THREAD_NAME, logger);
        this.messageSwitch = messageSwitch;
        this.myId = myId;
        this.nodeRing = new NodeRing(myId);
    }

    private Optional<Message> receiveMessage() {
        try {
            return Optional.ofNullable(messageSwitch.getCoordinatorMessage(0));
        } catch (InterruptedException e) {
            getLogger().warning("Error occurred: ");
            getLogger().warning(e.getMessage());
            stop();
        }
        return Optional.empty();
    }

    private void handleMessage(Message message) {
        switch (message.getType()) {
            case SUCCESSOR_REQUEST:
                handleSuccessorRequest(message);
                break;
            case JOIN:
                handleJoinRequest(message);
        }
    }

    private void sendSuccessor(ElectionMethod electionMethod, int destId, int successorId) {
        final Message successorReply
                = new Message(MessageType.SUCCESSOR, electionMethod, myId, new SuccessorMessage(successorId));

        try {
            this.messageSwitch.sendMessage(successorReply, destId);
        } catch (IOException e) {
            getLogger().warning("Unable to send message");
            getLogger().warning(e.getMessage());
        }
    }

    private void handleJoinRequest(Message message) {
        final int srcId = message.getSrcId();

        nodeRing.addNode(srcId);
        final Node node = nodeRing.getNode(srcId);

        // Assign successor to requesting node
        sendSuccessor(message.getElectionMethod(), srcId, node.getNext().getNodeId());

        // Reassign new successor to previous node
        sendSuccessor(message.getElectionMethod(), node.getPrev().getNodeId(), node.getNodeId());
    }

    private void handleSuccessorRequest(Message message) {
        final int srcId = message.getSrcId();

        final Node node = nodeRing.getNode(srcId);
        nodeRing.removeNode(node.getNext().getNodeId());

        // Assign successor to requesting node
        sendSuccessor(message.getElectionMethod(), srcId, node.getNext().getNodeId());
    }

    @Override
    public void run() {
        getLogger().info("Running as coordinator");

        running = true;
        while (running) {
            receiveMessage()
                    .ifPresent(this::handleMessage);
        }
    }

    public void stop() {
        running = false;
    }

}
