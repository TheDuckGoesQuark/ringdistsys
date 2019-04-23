package node.clienthandler;

import com.sun.net.httpserver.HttpServer;
import logging.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.logging.Logger;

public class HttpClientHandler implements ClientHandler {

    private final Logger logger = LoggerFactory.getLogger();

    /**
     * HttpServer instance for handling client requests
     */
    private final HttpServer server;

    public HttpClientHandler(String hostAddress, int clientPort) throws Exception {
        this.server = HttpServer.create(new InetSocketAddress(hostAddress, clientPort), 0);
        server.createContext("/", new HTMLHandler());
        server.setExecutor(null);
        server.start();
    }

    @Override
    public boolean receiveMessage() {
        return false;
    }

    @Override
    public boolean sendMessage() {
        return false;
    }

    @Override
    public int getNumberOfClients() {
        return 0;
    }

    @Override
    public void cleanup() {
        try {
            server.stop(0);
        } catch (Exception e) {
            e.printStackTrace();
            logger.warning(e.getMessage());
        }
    }
}
