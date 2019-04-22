package messages;

public enum MessageType {

    COORDINATOR_ELECTION, // Election related message
    SUCCESSOR, // Reply to a successor request containing that nodes new successor
    SUCCESSOR_REQUEST, // When a node experiences a disconnect from its successor, it can request a new one
    TOKEN, // Token message
    TOKEN_ACK, // Acknowledgement of received token
    JOIN, // A successor request but for nodes wishing to join the network

}
