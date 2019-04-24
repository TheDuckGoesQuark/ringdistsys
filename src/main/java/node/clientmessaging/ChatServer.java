package node.clientmessaging;

/**
 * User handler forwards node.nodemessaging to clients for them, and adds node.nodemessaging to the queue
 */
public interface ChatServer extends Runnable {

    /**
     * Allows the clientmessaging to remove any node.nodemessaging for its clients from the message queue (Q)
     *
     * @return true if a message was received
     */
    boolean receiveMessage();

    /**
     * Allows the clientmessaging to add a message that its clientmessaging has sent to the message queue (Q)
     *
     * @return true if a message was sent
     */
    boolean sendMessage();

    /**
     * @return the number of clients currently being served by this handler
     */
    int getNumberOfClients();

    /**
     * Terminates the clientmessaging and safely cleans up any resources it is using such as node.sockets.
     */
    void cleanup();

}
