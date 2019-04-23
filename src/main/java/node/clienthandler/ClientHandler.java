package node.clienthandler;

/**
 * Client handler forwards messages to clients for them, and adds messages to the queue
 */
public interface ClientHandler {

    /**
     * Allows the client to remove any messages for its clients from the message queue (Q)
     *
     * @return true if a message was received
     */
    boolean receiveMessage();

    /**
     * Allows the client to add a message that its client has sent to the message queue (Q)
     *
     * @return true if a message was sent
     */
    boolean sendMessage();

    /**
     * @return the number of clients currently being served by this handler
     */
    int getNumberOfClients();

    /**
     * Terminates the client and safely cleans up any resources it is using such as node.sockets.
     */
    void cleanup();

}
