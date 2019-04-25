package node.electionhandlers;

import node.ringrepository.VirtualNode;
import node.ringrepository.RingRepository;
import logging.LoggerFactory;
import node.nodemessaging.Message;
import node.nodemessaging.election.ElectionMessageHeader;
import node.nodemessaging.election.bully.CoordinatorMessage;
import node.sockets.UDPSocket;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;
import static node.nodemessaging.MessageType.*;
import static node.nodemessaging.election.ElectionMessageType.*;
import static node.electionhandlers.ElectionMethod.*;

public class BullyElectionHandler implements ElectionHandler {

    private static final int SECONDS_BEFORE_ASSUMING_VICTORY = 3;
    private static final int SECONDS_BEFORE_RETRYING = 5;

    private final Logger logger = LoggerFactory.getLogger();
    private final RingRepository ringRepository;
    private final UDPSocket udpSocket;
    private final int thisNodeId;
    private final ExecutorService executorService;

    private boolean coordinatorArrived = false;
    private boolean okArrived = false;

    private int electedCoordinatorId = 0;
    private boolean ongoing = false;


    public BullyElectionHandler(UDPSocket udpSocket, int nodeId, ExecutorService executorService, RingRepository ringRepository) {
        this.udpSocket = udpSocket;
        this.thisNodeId = nodeId;
        this.executorService = executorService;
        this.ringRepository = ringRepository;
    }

    @Override
    public ElectionMethod getMethodName() {
        return BULLY;
    }

    private void resetFlags() {
        coordinatorArrived = false;
        okArrived = false;
        ongoing = false;
    }

    @Override
    public void startElection() throws IOException {
        logger.info("Starting bully election");
        sendSelfAsCandidate();
        resetFlags();
        ongoing = true;

        // Wait T time for OK message before assuming victory
        executorService.submit((Callable<Void>) () -> {
            sleep(SECONDS_BEFORE_ASSUMING_VICTORY * 1000);
            if (!coordinatorArrived && !okArrived) {
                assumeSelfWon();
            }
            return null;
        });
    }

    /**
     * Sends my ID as a possible coordinator to all nodes with IDs greater than mine
     *
     * @throws IOException
     */
    private void sendSelfAsCandidate() throws IOException {
        final List<VirtualNode> allNodesAboveMe = ringRepository.getAllNodesInRingWithIdGreaterThan(thisNodeId);
        final ElectionMessageHeader header = new ElectionMessageHeader(BULLY, ELECTION);
        final Message message = new Message(COORDINATOR_ELECTION, thisNodeId, header);

        // Send election method to each node with ID greater than mine
        for (VirtualNode virtualNode : allNodesAboveMe) {
            udpSocket.sendMessage(message, virtualNode.getNodeId());
        }
    }

    /**
     * Concludes the election and informs all other nodes that I am the captain now.
     *
     * @throws IOException if unable to broadcast
     */
    private void assumeSelfWon() throws IOException {
        logger.info("Electing self as coordinator");

        electedCoordinatorId = thisNodeId;
        ongoing = false;

        broadcastCoordinator();
    }

    /**
     * Broadcasts the current elected coordinator ID to all nodes in the ring that aren't myself
     *
     * @throws IOException if unable to send for some reason...
     */
    private void broadcastCoordinator() throws IOException {
        final List<VirtualNode> allNodes = ringRepository.getAllNodesWithSuccessors();
        final CoordinatorMessage coordinatorMessage = new CoordinatorMessage(electedCoordinatorId);
        final ElectionMessageHeader header = new ElectionMessageHeader(BULLY, COORDINATOR, coordinatorMessage);
        final Message message = new Message(COORDINATOR_ELECTION, thisNodeId, header);

        for (VirtualNode node : allNodes) {
            udpSocket.sendMessage(message, node.getNodeId());
        }
    }

    @Override
    public void handleMessage(Message message) throws IOException {
        final ElectionMessageHeader electionMessageHeader = message.getPayload(ElectionMessageHeader.class);

        switch (electionMessageHeader.getType()) {
            case OK:
                handleOKMessage(message);
            case ELECTION:
                handleElectionMessage(message);
                break;
            case COORDINATOR:
                handleCoordinatorMessage(electionMessageHeader);
                break;
        }
    }

    private void handleCoordinatorMessage(ElectionMessageHeader electionMessageHeader) {
        coordinatorArrived = true;
        electedCoordinatorId = electionMessageHeader.getPayload(CoordinatorMessage.class).getCoordinatorId();
        ongoing = false;
    }

    /**
     * If my ID is greater than the ID of the node requesting the election, then reply with an OK and start my own election
     * with higher IDs nodes. Do nothing if already part of an election
     *
     * @param message
     * @throws IOException
     */
    private void handleElectionMessage(Message message) throws IOException {
        if (message.getSrcId() < thisNodeId && !ongoing) {
            sendOk(message.getSrcId());
            startElection();
        }
    }

    /**
     * Sends OK message to the node with the given Id
     *
     * @param recipientId node to receive OK message
     * @throws IOException
     */
    private void sendOk(int recipientId) throws IOException {
        final ElectionMessageHeader header = new ElectionMessageHeader(BULLY, OK);
        final Message message = new Message(COORDINATOR_ELECTION, thisNodeId, header);
        udpSocket.sendMessage(message, recipientId);
    }

    /**
     * If the OK message arrives from a node with a greater ID, wait for the result of their election. Otherwise, retry.
     *
     * @param message
     */
    private void handleOKMessage(Message message) {
        if (message.getSrcId() > thisNodeId) {
            okArrived = true;

            // Wait T' for coordinator message
            executorService.submit((Callable<Void>) () -> {
                sleep(SECONDS_BEFORE_RETRYING * 1000);
                if (!coordinatorArrived) {
                    // Retry
                    startElection();
                }
                return null;
            });
        }
    }

    @Override
    public int getResult() {
        return electedCoordinatorId;
    }

    @Override
    public boolean electionConcluded() {
        return !ongoing;
    }
}

