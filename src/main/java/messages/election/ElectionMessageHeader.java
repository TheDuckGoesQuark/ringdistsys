package messages.election;

import node.electionhandlers.ElectionMethod;

import java.io.Serializable;

/**
 * Contains the election method and message type for processing and parsing the contents of the message
 */
public class ElectionMessageHeader implements Serializable {

    /**
     * Election method that should be used to interpret message
     */
    private final ElectionMethod electionMethod;

    /**
     * Type of election method being sent
     */
    private final ElectionMessageType type;

    /**
     * Payload of message
     */
    private final Object payload;

    public ElectionMessageHeader(ElectionMethod electionMethod, ElectionMessageType type) {
        this.electionMethod = electionMethod;
        this.type = type;
        this.payload = null;
    }

    public ElectionMessageHeader(ElectionMethod electionMethod, ElectionMessageType type, Object payload) {
        this.electionMethod = electionMethod;
        this.type = type;
        this.payload = payload;
    }

    public ElectionMethod getElectionMethod() {
        return electionMethod;
    }

    public ElectionMessageType getType() {
        return type;
    }

    public <T> T getPayload(Class<T> clazz) {
        return (T) payload;
    }

    @Override
    public String toString() {
        return "ElectionMessageHeader{" +
                "electionMethod=" + electionMethod +
                ", type=" + type +
                ", payload=" + (payload != null ? payload.toString() : null) +
                '}';
    }
}
