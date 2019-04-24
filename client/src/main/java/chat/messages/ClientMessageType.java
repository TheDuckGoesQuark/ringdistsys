package chat.messages;

public enum ClientMessageType {

    LOGIN, // Sent on client login
    CHAT_MESSAGE, // Message sent to/from client
    JOIN_GROUP,  //  Message sent by client when they want to join group
    LEAVE_GROUP // Message sent by client when they want to leave a group

}
