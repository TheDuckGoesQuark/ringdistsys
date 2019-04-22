package node.electionhandlers;

import logging.LoggerFactory;
import messages.Message;
import messages.election.ElectionMessageHeader;
import messages.election.changroberts.CoordinatorMessage;
import messages.election.changroberts.ElectionMessage;
import node.RingCommunicationHandler;

import java.io.IOException;
import java.util.logging.Logger;

import static messages.MessageType.COORDINATOR_ELECTION;
import static messages.election.ElectionMessageType.COORDINATOR;
import static messages.election.ElectionMessageType.ELECTION;
import static node.electionhandlers.ElectionMethod.CHANG_ROBERTS;

public class ChangeRobertsElectionHandler implements ElectionHandler {

    private final Logger logger = LoggerFactory.getLogger();
    private final RingCommunicationHandler ringCommunicationHandler;
    private final int thisNodeId;

    private boolean participant = false;
    private int electedCoordinator = 0;

    public ChangeRobertsElectionHandler(RingCommunicationHandler ringComms, int nodeId) {
        this.ringCommunicationHandler = ringComms;
        this.thisNodeId = nodeId;
    }

    @Override
    public ElectionMethod getMethodName() {
        return CHANG_ROBERTS;
    }

    @Override
    public void startElection() throws IOException {
        logger.info("Starting chang roberts election.");
        final ElectionMessage electionMessage = new ElectionMessage(thisNodeId);
        final ElectionMessageHeader electionMessageHeader = new ElectionMessageHeader(CHANG_ROBERTS, ELECTION, electionMessage);
        final Message message = new Message(COORDINATOR_ELECTION, thisNodeId, electionMessageHeader);
        ringCommunicationHandler.sendToSuccessor(message);
        participant = true;
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

    private void handleCoordinatorMessage(ElectionMessageHeader header) throws IOException {
        final CoordinatorMessage coordinatorMessage = header.getPayload(CoordinatorMessage.class);

        // Forward message if not already aware of the new coordinator
        boolean toBeForwarded = electedCoordinator != coordinatorMessage.getCoordinatorId();
        electedCoordinator = coordinatorMessage.getCoordinatorId();
        participant = false;

        if (toBeForwarded) {
            forwardCoordinatorMessage(coordinatorMessage);
        }
    }

    /**
     * Forwards the coordinator message onto this nodes successor
     *
     * @param coordinatorMessage coordinator message to forward
     * @throws IOException if something goes wrong while trying to send
     */
    private void forwardCoordinatorMessage(CoordinatorMessage coordinatorMessage) throws IOException {
        final ElectionMessageHeader header = new ElectionMessageHeader(CHANG_ROBERTS, COORDINATOR, coordinatorMessage);
        final Message message = new Message(COORDINATOR_ELECTION, thisNodeId, header);
        ringCommunicationHandler.sendToSuccessor(message);
    }

    private void handleElectionMessage(ElectionMessageHeader header) throws IOException {
        final ElectionMessage electionMessage = header.getPayload(ElectionMessage.class);

        int currentCandidate = electionMessage.getCurrentCandidate();

        if (currentCandidate == thisNodeId) {
            // Message has returned to me and can be concluded
            concludeElection(currentCandidate);
        } else if (currentCandidate > thisNodeId) {
            // Always forward when candidate has greater ID
            forwardElectionMessage(header);
            participant = true;
        } else if (!participant) {
            // Otherwise only forward if not already participating
            if (currentCandidate < thisNodeId) {
                // Set self as better candidate
                electionMessage.setCurrentCandidate(thisNodeId);
            }

            forwardElectionMessage(header);
            participant = true;
        }
    }

    private void concludeElection(int currentCandidate) throws IOException {
        logger.info(String.format("Election concluded: %d", currentCandidate));

        electedCoordinator = currentCandidate;
        participant = false;

        final CoordinatorMessage coordinatorMessage = new CoordinatorMessage(electedCoordinator);
        forwardCoordinatorMessage(coordinatorMessage);
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

    @Override
    public int getResult() {
        return electedCoordinator;
    }

    @Override
    public boolean electionConcluded() {
        return !participant;
    }
}
