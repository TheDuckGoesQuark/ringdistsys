package messages;

public enum MessageType {

    ELECTION,
    COORDINATOR,
    OK, // Confi
    SUCCESSOR, // Reply to a successor request containing that nodes new successor
    SUCCESSOR_REQUEST, // When a node experiences a disconnect from its successor, it can request a new one
    TOKEN, // Token message
    TOKEN_ACK, // Acknowledgement of received token
    JOIN, // A successor request but for nodes wishing to join the network
    KEEPALIVE; // Keepalive message for maintaining that a connection is alive

}
