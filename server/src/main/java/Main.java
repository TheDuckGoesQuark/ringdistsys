import config.ArgumentParser;
import config.Configuration;
import logging.LoggerFactory;
import node.Node;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws Exception {
        Configuration configuration = ArgumentParser.parseArgs(args);

        LoggerFactory.buildLogger(configuration.getNodeId());

        Node node = new Node(configuration);
        try {
            node.start();
        } catch (Exception e) {
            e.printStackTrace();
            node.end();
        }
    }
}
