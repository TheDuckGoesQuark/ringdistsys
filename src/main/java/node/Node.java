package node;

import config.Configuration;
import globalpersistence.DatabaseRingStore;
import globalpersistence.NodeListFileParser;
import logging.LoggerFactory;
import messages.Message;
import messages.SuccessorMessage;
import sockets.UDPSocket;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;


public class Node {

    private final Logger logger = LoggerFactory.getLogger();
    private final Configuration config;
    private final ExecutorService executorService;

    private final TokenRingManager tokenRingManager;
    private final UDPSocket udpSocket;
    private final DatabaseRingStore databaseRingStore;

    public Node(Configuration config) throws IOException {
        this.config = config;
        this.executorService = Executors.newCachedThreadPool();

        this.databaseRingStore = new DatabaseRingStore(config.getListFilePath());
        this.databaseRingStore.initialize();

        final AddressTranslator addressTranslator = new AddressTranslator(this.databaseRingStore.getAllNodes());
        this.udpSocket = new UDPSocket(addressTranslator, config.getNodeId());
        this.tokenRingManager = new TokenRingManager(config, addressTranslator, executorService, udpSocket);
    }

    /**
     * Begins node execution by initializing the token ring manager and listening for messages
     *
     * @throws IOException
     */
    public void start() throws IOException {
        logger.info(String.format("Initializing node with configuration: %s", config.toString()));

        tokenRingManager.joinRing();

        final Callable<Void> tokenPassingThread = () -> {
            while (!killswitch()) {
                tokenRingManager.passToken();
            }
            return null;
        };

        // Begin token passing
        executorService.submit(tokenPassingThread);

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
