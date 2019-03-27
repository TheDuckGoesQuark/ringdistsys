package logging;

import logging.LoggerFactory;
import org.junit.Test;

import java.util.logging.Logger;

import static org.junit.Assert.*;

public class LoggerFactoryTest {
    @Test
    public void buildLogger() {
        Logger logger = LoggerFactory.buildLogger(1);
        assertNotNull(logger);
    }

    @Test
    public void useLogger() {
        Logger logger = LoggerFactory.buildLogger(1);
        logger.info("hello world");
    }
}