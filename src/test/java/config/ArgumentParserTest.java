package config;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;

import static org.junit.Assert.assertEquals;

public class ArgumentParserTest {

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    @Test
    public void parseArgsWhenNone() {
        exit.expectSystemExitWithStatus(1);
        ArgumentParser.parseArgs(new String[0]);
    }

    @Test
    public void parseArgsWhenUnknownArg() {
        exit.expectSystemExitWithStatus(1);
        String[] args = {"-z", "value"};
        ArgumentParser.parseArgs(args);
    }

    @Test
    public void parseArgsWhenAddressNoValue() {
        exit.expectSystemExitWithStatus(1);
        String[] args = {"-a"};
        ArgumentParser.parseArgs(args);
    }

    @Test
    public void parseArgsWhenAllFlagsHaveValue() {
        String[] args = {"-a", "127.0.0.0", "-p", "8080", "-i", "6"};
        Configuration config = ArgumentParser.parseArgs(args);
        assertEquals("Address is equals", "127.0.0.0", config.getAddress().getAddress().getHostAddress());
        assertEquals("Port number is correct", 8080, config.getAddress().getPort());
        assertEquals("Id is correct", 6, config.getNodeId());
    }

    @Test
    public void parseArgsWhenAllNoIdValue() {
        exit.expectSystemExitWithStatus(1);
        String[] args = {"-a", "127.0.0.0", "-p", "8080", "-i"};
        ArgumentParser.parseArgs(args);
    }

    @Test
    public void parseArgsWhenAllBadIpAddress() {
        exit.expectSystemExitWithStatus(1);
        String[] args = {"-a", "1270.00", "-p", "8080", "-i", "6"};
        ArgumentParser.parseArgs(args);
    }
}