package node;

import config.Configuration;
import config.NodeListFileParser;
import logging.LoggerFactory;
import messages.Message;
import messages.SuccessorMessage;
import sockets.UDPSocket;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;


public class Node {

    private final Logger logger = LoggerFactory.getLogger();
    private final Configuration config;
    private TokenRingManager tokenRingManager;

    private ExecutorService executorService;
    private UDPSocket udpSocket;

    private AddressTranslator addressTranslator;

    public Node(Configuration config) throws IOException {
        this.addressTranslator = NodeListFileParser.parseNodeFile(config.getListFilePath(), logger);
        this.config = config;
        this.executorService = Executors.newCachedThreadPool();
    }

    private void initializeCommunication() throws IOException {
        try {
            logger.info("Initializing sockets");
            // Starts UDP socket
            udpSocket = new UDPSocket(addressTranslator, config.getNodeId());
            tokenRingManager = new TokenRingManager(config, addressTranslator, executorService, udpSocket);
            tokenRingManager.joinRing();
        } catch (IOException e) {
            logger.warning("Failed to initialize sockets.");
            logger.warning(e.getMessage());
            throw e;
        }
    }

    /**
     * Begins node execution by initializing the token ring manager and listening for messages
     *
     * @throws IOException
     */
    public void start() throws IOException {
        logger.info(String.format("Initializing node with configuration: %s", config.toString()));

        initializeCommunication();

        // Begin token passing
        executorService.submit((Callable<Void>) () -> {
            while (!killswitch())
                tokenRingManager.passToken();

            return null;
        });

        // Handle any coordination updates
        while (!killswitch()) {
            logger.info("Listening for coordination updates");
            final Message message = udpSocket.receiveMessage(3);

            if (message == null) continue;

            switch (message.getType()) {
                case SUCCESSOR:
                    logger.info("Received new successor assignment");
                    tokenRingManager.updateSuccessor(
                            message.getPayload(SuccessorMessage.class).getSuccessorId(),
                            false
                    );
                    break;
                case JOIN:
                    logger.info("Received join request");
                    tokenRingManager.handleJoinRequest(message);
                    break;
                case SUCCESSOR_REQUEST:
                    logger.info("Received successor request");
                    tokenRingManager.handleSuccessorRequest(message);
                    break;
                default:
                    logger.info("Received unknown message type: " + message.getType().name());
                    break;
            }
        }

        end();
    }

    /**
     * Checks for the existince of a directory called 'killswitch' in the users home directory.
     *
     * @return True if the directory '~/killswitch' exists.
     */
    private boolean killswitch() {
        final String killswitch = System.getProperty("user.home") + "/killswitch";
        return new File(killswitch).exists();
    }

    /**
     * Terminates and cleans up any resources used and created by the node, such as sockets and threads.
     *
     * @throws IOException if something goes wrong during termination.
     */
    public void end() throws IOException {
        logger.warning("Shutting down");

        if (udpSocket != null)
            this.udpSocket.close();

        tokenRingManager.cleanup();

        executorService.shutdown();

        logger.warning("Finished shutting down node.");
    }
}
