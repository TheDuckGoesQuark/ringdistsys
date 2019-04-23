package node.clienthandler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import logging.LoggerFactory;

import java.io.*;
import java.util.logging.Logger;

public class HTMLHandler implements HttpHandler {

    private final String path = "index.html";
    private final Logger logger = LoggerFactory.getLogger();

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {

        int size = ClassLoader.getSystemResource(path).openConnection().getContentLength();
        httpExchange.sendResponseHeaders(200, size);

        final InputStream in = ClassLoader.getSystemClassLoader().getResourceAsStream("index.html");

        final OutputStream os = httpExchange.getResponseBody();

        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }

        os.close();
    }
}
