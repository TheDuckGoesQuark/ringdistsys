package node.clienthandler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;

public class HttpHandlerImpl implements HttpHandler {
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        final String response = "Hello world!\n";
        httpExchange.sendResponseHeaders(200, response.length());
        final OutputStream os = httpExchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}
