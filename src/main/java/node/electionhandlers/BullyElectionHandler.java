package node.electionhandlers;

import messages.Message;
import node.RingCommunicationHandler;
import sockets.UDPSocket;

import java.io.IOException;
import java.util.Optional;

public class BullyElectionHandler implements ElectionHandler {
    public BullyElectionHandler(RingCommunicationHandler ringComms, UDPSocket udpSocket, int nodeId) {
    }

    @Override
    public ElectionMethod getMethodName() {
        return null;
    }

    @Override
    public void startElection() throws IOException {

    }

    @Override
    public void handleMessage(Message message) throws IOException {

    }

    @Override
    public int getResult() {
        return 0;
    }

    @Override
    public boolean electionConcluded() {
        return false;
    }
}

