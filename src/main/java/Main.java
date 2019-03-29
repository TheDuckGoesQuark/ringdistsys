import config.ArgumentParser;
import config.Configuration;
import node.Node;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Main {

    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException, TimeoutException {
        Configuration configuration = ArgumentParser.parseArgs(args);

        Node node = new Node(configuration);
        try {
            node.start();
        } finally {
            node.end();
        }
    }
}
