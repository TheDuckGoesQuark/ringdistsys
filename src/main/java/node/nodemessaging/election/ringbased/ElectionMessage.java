package node.nodemessaging.election.ringbased;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ElectionMessage implements Serializable {

    // TODO handle message being forwarded repeatedly if originator has failed during election

    /**
     * List of IDs of ring members, appended at each hop
     */
    private List<Integer> candidates = new ArrayList<>();

    public ElectionMessage(int firstCandidate) {
        this.candidates.add(firstCandidate);
    }

    /**
     * Retrieve the list of candidates
     *
     * @return the current list of candidates
     */
    public List<Integer> getCandidates() {
        return candidates;
    }

    /**
     * Returns the ID of the node that started the election
     *
     * @return the ID of the node that started the election
     */
    public int getOriginatorId() {
        return candidates.get(0);
    }

    /**
     * Add the given ID to the list of candidates
     *
     * @param candidateId
     */
    public void addCandidate(int candidateId) {
        candidates.add(candidateId);
    }

    @Override
    public String toString() {
        return "ElectionMessage{" +
                "candidates=" + candidates.toString() +
                '}';
    }
}
