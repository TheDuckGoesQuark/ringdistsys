package node.electionhandlers;

import messages.Message;

import java.io.IOException;
import java.util.Optional;

public interface ElectionHandler {

    /**
     * Returns the type of election algorithm that is used by the implementation of this interface.
     *
     * @return the type of election algorithm that is used by the implementation of this interface.
     */
    ElectionMethod getMethodName();

    /**
     * triggers the start of an election
     */
    void startElection() throws IOException;

    /**
     * Handles messages relevant to the election protocol
     *
     * @param message election message
     */
    void handleMessage(Message message) throws IOException;

    /**
     * Returns the ID of the newly elected coordinator if the election has concluded.
     * If {@link ElectionHandler#electionConcluded()} returns false,
     * the value returned is meaningless and should not be used.
     *
     * @return the new coordinators ID once the election has concluded
     */
    int getResult();

    /**
     * Checks if an ongoing election has been concluded and that the new coordinators ID is known.
     *
     * @return true if a coordinator ID is known.
     */
    boolean electionConcluded();

}
