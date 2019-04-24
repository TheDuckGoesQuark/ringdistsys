package node.clientmessaging;

import logging.LoggerFactory;
import node.clientmessaging.messages.Encoder;
import node.clientmessaging.messages.UserMessage;
import node.clientmessaging.messages.UserMessageEncoder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Optional;
import java.util.logging.Logger;

class ClientHandler implements Runnable {

    private static final Encoder<UserMessage, String> ENCODER = new UserMessageEncoder();

    private final Logger logger = LoggerFactory.getLogger();
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
            readJSON().ifPresent(this::handleMessage);
        }
    }

    private void handleMessage(UserMessage userMessage) {
        logger.info(String.format("Received message: %s", userMessage.toString()));
        out.println(ENCODER.encode(userMessage));
    }

    /**
     * Attempts to parse JSON from string sent over socket
     *
     * @return maybe parsed message.
     */
    private Optional<UserMessage> readJSON() {
        final UserMessage message;

        try {
            final String input = in.readLine();
            message = ENCODER.decode(input);
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }

        return Optional.ofNullable(message);
    }
}
