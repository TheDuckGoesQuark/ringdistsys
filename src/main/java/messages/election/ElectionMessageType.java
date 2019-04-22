package messages.election;

public enum ElectionMessageType {

    ELECTION, // Election
    COORDINATOR, // New coordinator
    OK, // Node has been 'bullied' into accepting the node previously sent as its coordinator.

}
