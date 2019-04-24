package logging;

import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LoggerFactory {

    private static Logger LOGGER = null;

    public static void buildLogger(int nodeId) {
        Logger logger = Logger.getGlobal();
        logger.setUseParentHandlers(false);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new SimpleFormatter() {
            private final String format = "[%1$tF %1$tT | " + nodeId + " ] [%2$-7s] %3$s %n";

            @Override
            public synchronized String format(LogRecord lr) {
                return String.format(format,
                        new Date(lr.getMillis()),
                        lr.getLevel().getLocalizedName(),
                        lr.getMessage()
                );
            }
        });

        logger.addHandler(handler);

        LOGGER = logger;
    }

    public static Logger getLogger() {
        if (LOGGER == null) return Logger.getGlobal();
        else return LOGGER;
    }

}
