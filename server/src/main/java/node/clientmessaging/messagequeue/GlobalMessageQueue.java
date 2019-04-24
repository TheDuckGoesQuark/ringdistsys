package node.clientmessaging.messagequeue;

import node.clientmessaging.messages.ChatMessage;

/**
 * Message Q which is accessible by all nodes in the ring
 */
public interface GlobalMessageQueue {

    void sendMessage(ChatMessage userMessage);

}
