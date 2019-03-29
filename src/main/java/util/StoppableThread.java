package util;

import java.util.logging.Logger;

public abstract class StoppableThread implements Runnable {

    private final Thread thread;
    private final Logger logger;

    private boolean running = false;

    public StoppableThread(String threadName, Logger logger) {
        this.thread = new Thread(this, threadName);
        this.logger = logger;
    }

    public void start() {
        logger.info("Starting " + thread.getName());
        if (!thread.isAlive()) {
            running = true;
            thread.start();
        } else {
            logger.warning(thread.getName() + " already running!");
        }
    }

    public void stop() {
        this.running = false;
    }

    public Thread getThread() {
        return thread;
    }

    public Logger getLogger() {
        return logger;
    }

    public boolean isRunning() {
        return running;
    }
}
