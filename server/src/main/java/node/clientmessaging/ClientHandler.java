package node.clientmessaging;

import logging.LoggerFactory;
import node.clientmessaging.messages.ClientMessage;
import node.clientmessaging.messages.ClientMessageJsonEncoder;
import node.clientmessaging.messages.Encoder;

import javax.swing.text.html.Option;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

class ClientHandler implements Runnable {

    private final Logger logger = LoggerFactory.getLogger();
    private static final Encoder<ClientMessage, String> ENCODER = new ClientMessageJsonEncoder();

    private final Socket clientSocket;
    private final PrintWriter out;
    private final BufferedReader in;

    ClientHandler(Socket clientSocket) throws IOException {
        this.clientSocket = clientSocket;
        this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        this.out = new PrintWriter(clientSocket.getOutputStream(), true);
    }

    /**
     * Polls for messages from the client
     */
    @Override
    public void run() {
        logger.info(String.format("Client connected from %s", clientSocket.getRemoteSocketAddress()));

        while (clientSocket.isConnected()) {
            final Optional<ClientMessage> message = receiveMessage();
            if (message.isPresent()) {
                handleMessage(message.get());
            } else {
                logger.info("Disconnected from client.");
                break;
            }
        }
    }

    /**
     * Handles message from client
     *
     * @param clientMessage message from client
     */
    private void handleMessage(ClientMessage clientMessage) {
        logger.info(String.format("Received message: %s", clientMessage.toString()));

        switch (clientMessage.getMessageType()) {
            case LOGIN:
                break;
            case JOIN_GROUP:
                break;
            case LEAVE_GROUP:
                break;
            case CHAT_MESSAGE:
                break;
        }

        sendMessage(clientMessage);
    }

    /**
     * Sends a message to the client
     */
    private void sendMessage(ClientMessage clientMessage) {
        logger.info(String.format("Sending message: %s", clientMessage.toString()));
        out.println(ENCODER.encode(clientMessage));
    }

    /**
     * Attempts to parse JSON from string sent over socket
     *
     * @return maybe parsed message.
     */
    private Optional<ClientMessage> receiveMessage() {
        String input = null;

        try {
            input = in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (input != null) return ENCODER.decode(input);
        return Optional.empty();
    }
}
