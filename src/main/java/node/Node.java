package node;

import com.opencsv.CSVReader;
import config.Configuration;
import config.NodeListFileParser;
import logging.LoggerFactory;
import org.w3c.dom.NodeList;
import sockets.RingSocket;
import sockets.UDPSocket;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static config.NodeListFileParser.parseNodeFile;

public class Node {

    private final Configuration config;
    private final Logger logger;

    private RingSocket ringSocket;
    private UDPSocket udpSocket;
    private Coordinator coordinator = null;

    public Node(Configuration config) {
        this.config = config;
        this.logger = LoggerFactory.buildLogger(config.getNodeId());
    }

    private void initializeCoordinatorThread() throws IOException {
        final Map<Integer, InetSocketAddress> nodes = NodeListFileParser.parseNodeFile(config.getListFilePath(), logger);
        this.coordinator = new Coordinator(udpSocket, nodes, logger);
        this.coordinator.start();
    }

    private void initializeSockets() throws SocketException {
        try {
            logger.info("Initializing sockets");
            this.ringSocket = new RingSocket(config.getAddress());
            this.udpSocket = new UDPSocket(config.getAddress());
        } catch (IOException e) {
            logger.warning("Failed to initialize sockets.");
            logger.warning(e.getMessage());
            throw e;
        }
    }


    private boolean isCoordinator() {
        return this.coordinator != null;
    }

    private void terminateCoordinatorThread() {
        this.coordinator.stop();
    }

    public void start() throws IOException {
        logger.info(String.format("Initializing node with configuration: %s", config.toString()));

        initializeSockets();

        if (config.getNodeId() == 6) {
            this.initializeCoordinatorThread();
        }

        // TODO node stuff.
    }
}
