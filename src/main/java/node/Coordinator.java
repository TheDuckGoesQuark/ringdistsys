package node;

import sockets.UDPSocket;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.logging.Logger;

public class Coordinator implements Runnable {

    private static final String THREAD_NAME = "COORDINATOR_THREAD";

    private Thread thread;
    private final Map<Integer, InetSocketAddress> nodes;
    private UDPSocket udpSocket;
    private Logger logger;
    private boolean running = false;

    public Coordinator(UDPSocket udpSocket, Map<Integer, InetSocketAddress> nodes, Logger logger) {
        this.udpSocket = udpSocket;
        this.logger = logger;
        this.nodes = nodes;
    }

    public void start() {
        logger.info("Starting " + THREAD_NAME);
        if (thread == null) {
            thread = new Thread(this, THREAD_NAME);
            thread.start();
        } else if (!thread.isAlive()) {
            thread.start();
        }
    }


    @Override
    public void run() {
        logger.info("Running as coordinator");

        running = true;
        while(running) {
            // TODO
        }
    }

    public void stop() {
        running = false;
    }
}
