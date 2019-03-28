package node;

import sockets.UDPSocket;

import java.util.logging.Logger;

public class Coordinator implements Runnable {

    private static final String THREAD_NAME = "COORDINATOR_THREAD";

    private Thread thread;
    private UDPSocket udpSocket;
    private Logger logger;
    private boolean running = false;
    private AddressTranslator addressTranslator;

    public Coordinator(UDPSocket udpSocket, AddressTranslator addressTranslator, Logger logger) {
        this.udpSocket = udpSocket;
        this.logger = logger;
        this.addressTranslator = addressTranslator;
    }

    public boolean isRunning() {
        return running;
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
