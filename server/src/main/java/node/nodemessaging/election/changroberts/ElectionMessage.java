package node.nodemessaging.election.changroberts;

import java.io.Serializable;

public class ElectionMessage implements Serializable {

    // TODO handle message being forwarded repeatedly if originator has failed during election

    /**
     * ID of node currently being considered as coordinator
     */
    private int currentCandidate;

    public ElectionMessage(int firstCandidate) {
        this.currentCandidate = firstCandidate;
    }

    public int getCurrentCandidate() {
        return currentCandidate;
    }

    public void setCurrentCandidate(int currentCandidate) {
        this.currentCandidate = currentCandidate;
    }

    @Override
    public String toString() {
        return "ElectionMessage{" +
                "currentCandidate=" + currentCandidate +
                '}';
    }
}
