import config.Configuration;

import java.util.logging.Logger;

public class Node {

    private Configuration config;
    private Logger logger;

    public Node(Configuration config) {
        this.config = config;
        this.logger = LoggerFactory.buildLogger(config.getNodeId());
    }

    public void start() {
        logger.info(String.format("Initializing node with configuration: %s", config.toString()));

    }
}
