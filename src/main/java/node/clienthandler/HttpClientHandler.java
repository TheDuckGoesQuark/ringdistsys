package node.clienthandler;

import com.sun.net.httpserver.HttpServer;
import logging.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Logger;

public class HttpClientHandler implements ClientHandler {

    private final Logger logger = LoggerFactory.getLogger();

    /**
     * HttpServer instance for handling client requests
     */
    private final HttpServer server;

    public HttpClientHandler() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new HttpHandlerImpl());
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

    }
}
