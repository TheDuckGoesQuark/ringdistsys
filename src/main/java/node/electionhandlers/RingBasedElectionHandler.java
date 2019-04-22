package node.electionhandlers;

import logging.LoggerFactory;
import messages.Message;
import messages.MessageType;
import messages.election.ElectionMessageHeader;
import messages.election.ElectionMessageType;
import messages.election.ringbased.CoordinatorMessage;
import messages.election.ringbased.ElectionMessage;
import node.RingCommunicationHandler;

import java.io.IOException;
import java.util.logging.Logger;

import static messages.MessageType.COORDINATOR_ELECTION;
import static messages.election.ElectionMessageType.*;
import static node.electionhandlers.ElectionMethod.RING_BASED;

public class RingBasedElectionHandler implements ElectionHandler {

    private final Logger logger = LoggerFactory.getLogger();
    private final RingCommunicationHandler ringCommunicationHandler;
    private final int thisNodeId;

    private boolean ongoing = false;
    private int electedCoordinator = 0;

    public RingBasedElectionHandler(RingCommunicationHandler ringCommunicationHandler, int thisNodeId) {
        this.ringCommunicationHandler = ringCommunicationHandler;
        this.thisNodeId = thisNodeId;
    }

    @Override
    public ElectionMethod getMethodName() {
        return RING_BASED;
    }

    @Override
    public void startElection() throws IOException {
        logger.info("Starting ring based election.");
        final ElectionMessage electionMessage = new ElectionMessage(thisNodeId);
        final ElectionMessageHeader electionMessageHeader = new ElectionMessageHeader(RING_BASED, ELECTION, electionMessage);
        final Message message = new Message(COORDINATOR_ELECTION, thisNodeId, electionMessageHeader);
        ringCommunicationHandler.sendToSuccessor(message);
        ongoing = true;
    }

    @Override
    public void handleMessage(Message message) throws IOException {
        final ElectionMessageHeader electionMessageHeader = message.getPayload(ElectionMessageHeader.class);

        switch (electionMessageHeader.getType()) {
            case ELECTION:
                handleElectionMessage(electionMessageHeader);
                break;
            case COORDINATOR:
                handleCoordinatorMessage(electionMessageHeader);
                break;
        }
    }

    /**
     * Handles election message
     *
     * @param header header of election message
     */
    private void handleElectionMessage(ElectionMessageHeader header) throws IOException {
        final ElectionMessage electionMessage = header.getPayload(ElectionMessage.class);

        if (electionMessage.getOriginatorId() == thisNodeId) {
            // Conclude election if it was started by me
            concludeElection(electionMessage);
        } else {
            // Append self and forward to next node in ring
            electionMessage.addCandidate(thisNodeId);
            forwardElectionMessage(header);
            ongoing = true;
        }
    }

    /**
     * Forwards election message
     *
     * @param header election message
     * @throws IOException
     */
    private void forwardElectionMessage(ElectionMessageHeader header) throws IOException {
        final Message message = new Message(COORDINATOR_ELECTION, thisNodeId, header);
        ringCommunicationHandler.sendToSuccessor(message);
    }

    /**
     * Determines the new coordinator from the list of candidates in the election message, and sends the result to
     * the rest of the ring
     *
     * @param electionMessage election message containing list of all candidates in the ring
     */
    private void concludeElection(ElectionMessage electionMessage) throws IOException {
        int highestId = electionMessage.getCandidates().get(0);
        for (int nodeId : electionMessage.getCandidates()) {
            if (nodeId > highestId) highestId = nodeId;
        }

        electedCoordinator = highestId;
        ongoing = false;

        final CoordinatorMessage coordinatorMessage = new CoordinatorMessage(electedCoordinator);
        forwardCoordinatorMessage(coordinatorMessage);
    }

    /**
     * Forwards the coordinator message onto this nodes successor
     *
     * @param coordinatorMessage coordinator message to forward
     * @throws IOException if something goes wrong while trying to send
     */
    private void forwardCoordinatorMessage(CoordinatorMessage coordinatorMessage) throws IOException {
        final ElectionMessageHeader header = new ElectionMessageHeader(RING_BASED, COORDINATOR, coordinatorMessage);
        final Message message = new Message(COORDINATOR_ELECTION, thisNodeId, header);
        ringCommunicationHandler.sendToSuccessor(message);
    }

    /**
     * Assigns the elected coordinator
     *
     * @param message message containing new coordinator
     */
    private void handleCoordinatorMessage(ElectionMessageHeader message) throws IOException {
        final CoordinatorMessage coordinatorMessage = message.getPayload(CoordinatorMessage.class);

        // Forward message if not already aware of the new coordinator
        boolean toBeForwarded = electedCoordinator != coordinatorMessage.getCoordinatorId();
        electedCoordinator = coordinatorMessage.getCoordinatorId();
        ongoing = false;

        if (toBeForwarded) {
            forwardCoordinatorMessage(coordinatorMessage);
        }
    }

    @Override
    public int getResult() {
        return electedCoordinator;
    }

    @Override
    public boolean electionConcluded() {
        return !ongoing;
    }
}
