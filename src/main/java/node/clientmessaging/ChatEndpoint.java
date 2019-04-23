package node.clientmessaging;

import logging.LoggerFactory;
import node.clientmessaging.messagequeue.UserMessage;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;

@ServerEndpoint(value = "/chat/{username}")
public class ChatEndpoint implements ClientHandler {

    private final Logger logger = LoggerFactory.getLogger();

    /**
     * HttpServer instance for handling clientmessaging requests
     */

    public ChatEndpoint(String hostAddress, int clientPort) throws Exception {
    }


    private Session session;
    private static Set<ChatEndpoint> chatEndpoints
            = new CopyOnWriteArraySet<>();
    private static HashMap<String, String> users = new HashMap<>();

    @OnOpen
    public void onOpen(
            Session session,
            @PathParam("username") String username) throws IOException, EncodeException {

        this.session = session;
        chatEndpoints.add(this);
        users.put(session.getId(), username);

        UserMessage message = new UserMessage();
        message.setFrom(username);
        message.setContent("Connected!");
        broadcast(message);
    }

    @OnMessage
    public void onMessage(Session session, UserMessage message)
            throws IOException, EncodeException {

        message.setFrom(users.get(session.getId()));
        broadcast(message);
    }

    @OnClose
    public void onClose(Session session) throws IOException, EncodeException {

        chatEndpoints.remove(this);
        UserMessage message = new UserMessage();
        message.setFrom(users.get(session.getId()));
        message.setContent("Disconnected!");
        broadcast(message);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        // Do error handling here
    }

    private static void broadcast(UserMessage message)
            throws IOException, EncodeException {

        chatEndpoints.forEach(endpoint -> {
            synchronized (endpoint) {
                try {
                    endpoint.session.getBasicRemote().
                            sendObject(message);
                } catch (IOException | EncodeException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public boolean receiveMessage() {
        return false;
    }

    @Override
    public boolean sendMessage() {
        return false;
    }

    @Override
    public int getNumberOfClients() {
        return 0;
    }

    @Override
    public void cleanup() {
    }
}
