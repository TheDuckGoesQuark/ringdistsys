import config.ArgumentParser;
import config.Configuration;
import node.Node;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        Configuration configuration = ArgumentParser.parseArgs(args);

        Node node = new Node(configuration);
        try {
            node.start();
        } catch (Exception e) {
            node.end();
        }
    }
}
