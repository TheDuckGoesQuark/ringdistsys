package node.clientmessaging.messagequeue;

import node.clientmessaging.messages.UserMessage;

/**
 * Message Q which is accessible by all nodes in the ring
 */
public interface GlobalMessageQueue {

    void sendMessage(UserMessage userMessage);

}
