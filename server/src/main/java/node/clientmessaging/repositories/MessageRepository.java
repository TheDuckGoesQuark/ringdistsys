package node.clientmessaging.repositories;

import node.clientmessaging.messages.ChatMessage;

import java.util.Optional;
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
     * Gets the oldest waiting message out of any messages waiting for the given users.
     * Message is removed from database once taken.
     *
     * @param usernames users to fetch messages for
     * @return oldest message for one of the given users
     */
    Optional<ChatMessage> getNextMessageForUser(Set<String> usernames);
}
