package node.clientmessaging.repositories;

import node.clientmessaging.messages.ChatMessage;

import java.util.Set;

/**
 * Message Q which is accessible by all nodes in the ring
 */
public interface MessageRepository {

    /**
     * Sends a message to either a group or a user
     *
     * @param userMessage message to be sent
     */
    void sendMessage(ChatMessage userMessage);

    /**
     * @param usernames list of users
     * @return true if there is a message available for one of the users in the list.
     */
    boolean checkForMessageForUsers(Set<String> usernames);
}
