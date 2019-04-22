package node.electionhandlers;

import messages.Message;
import node.RingCommunicationHandler;

import java.io.IOException;
import java.util.Optional;

public class ChangeRobertsElectionHandler implements ElectionHandler {

    public ChangeRobertsElectionHandler(RingCommunicationHandler ringComms, int nodeId) {
    }

    @Override
    public ElectionMethod getMethodName() {
        return ElectionMethod.CHANG_ROBERTS;
    }

    @Override
    public void startElection() throws IOException {

    }

    @Override
    public void handleMessage(Message message) {

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
