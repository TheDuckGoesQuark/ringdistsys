package node;

import sockets.UDPSocket;

import java.util.logging.Logger;

public class Coordinator implements Runnable {

    private static final String THREAD_NAME = "COORDINATOR_THREAD";

    private final AddressTranslator addressTranslator;

    private final Thread thread = new Thread(this, THREAD_NAME);
    private UDPSocket udpSocket;
    private Logger logger;
    private boolean running = false;

    public Coordinator(UDPSocket udpSocket, AddressTranslator addressTranslator, Logger logger) {
        this.udpSocket = udpSocket;
        this.addressTranslator = addressTranslator;
        this.logger = logger;
    }

    public boolean isRunning() {
        return running;
    }

    public void start() {
        logger.info("Starting " + THREAD_NAME);
        if (!thread.isAlive()) {
            thread.start();
        } else {
            logger.warning(THREAD_NAME + " already running!");
        }
    }

    @Override
    public void run() {
        logger.info("Running as coordinator");

        running = true;

        while (running) {
            // TODO check ring members are up
            // TODO inform ring members of their successor
            // TODO await nodes sending notification that their successor has failed
            // TODO when node successor dies, send new successor to that node
            running = false;
        }
    }

    public void stop() {
        running = false;
    }
}
