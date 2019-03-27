package node;

import config.Configuration;
import logging.LoggerFactory;
import sockets.UDPSocket;

import java.net.SocketException;
import java.util.logging.Logger;

public class Node {

    private Configuration config;
    private Logger logger;
    private boolean isCoordinator;

    public Node(Configuration config) {
        this.config = config;
        this.isCoordinator = config.getNodeId() == 6;
        this.logger = LoggerFactory.buildLogger(config.getNodeId());
    }

    public void start() {
        logger.info(String.format("Initializing node with configuration: %s", config.toString()));

        if (isCoordinator)
            runAsCoordinator();
        else
            runAsNode();

    }

    private void runAsNode() {

    }

    private void runAsCoordinator() {
        logger.info("");
        try {
            UDPSocket udpSocket = new UDPSocket(config.getAddress());

        } catch (SocketException e) {
            e.printStackTrace();
        }
    }


}
