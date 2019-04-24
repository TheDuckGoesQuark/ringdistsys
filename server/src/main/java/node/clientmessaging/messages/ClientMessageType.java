package node.clientmessaging.messages;

public enum ClientMessageType {

    CHAT_MESSAGE, // Message sent to/from client
    ERROR, // Send (typically) by server when something isn't quite right.
    JOIN_GROUP,  //  Message sent by client when they want to join group
    LEAVE_GROUP, // Message sent by client when they want to leave a group
    LOGIN, // Sent on client login
    ALIVE, // Sent by server to request liveness check from client. Sent by client in reply.

}
