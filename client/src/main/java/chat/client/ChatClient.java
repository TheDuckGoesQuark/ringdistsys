package chat.client;

import chat.client.messages.ClientMessage;

import java.io.IOException;
import java.util.Optional;

public interface ChatClient {

    /**
     * @param username username to login as
     * @throws IOException if unable to login
     */
    void logInAs(String username) throws IOException;

    /**
     * @return true if the user is currently logged in
     */
    boolean loggedIn();

    /**
     * @return the username currently logged in as.
     */
    Optional<String> getUsername();

    /**
     * Adds the currently logged in user to the group with the given name
     *
     * @param groupname name of group
     * @throws IOException if unable to join group
     */
    void joinGroup(String groupname) throws IOException;

    /**
     * Removes the currently logged in user from the group with the given name
     *
     * @param groupname name of group to leave
     * @throws IOException if unable to leave group
     */
    void leaveGroup(String groupname) throws IOException;

    /**
     * Sends the given message to all members of the group with the given name
     *
     * @param message   message to send
     * @param groupname group to send to
     */
    void sendMessageToGroup(String message, String groupname) throws IOException;

    /**
     * Sends the given message to the user with the given name
     *
     * @param message  message to send
     * @param username user to send to
     */
    void sendMessageToUser(String message, String username);

    /**
     * Blocks until a message becomes available
     *
     * @return message for the currently logged in user
     */
    ClientMessage receiveMessage() throws IOException;

    /**
     * Logs user out
     */
    void logout() throws IOException;
}
