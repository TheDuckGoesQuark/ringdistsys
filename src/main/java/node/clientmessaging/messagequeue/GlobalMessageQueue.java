package node.clientmessaging.messagequeue;

/**
 * Message Q which is accessible by all nodes in the ring
 */
public interface GlobalMessageQueue {

    void sendMessage(UserMessage userMessage);

}
