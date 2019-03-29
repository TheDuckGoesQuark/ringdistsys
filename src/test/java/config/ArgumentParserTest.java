package config;

import node.ElectionMethod;
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
        String[] args = {"-i", "6", "-f", "~/somefile", "-e", "BULLY"};
        Configuration config = ArgumentParser.parseArgs(args);
        assertEquals("Id is correct", 6, config.getNodeId());
        assertEquals("File path is correct", "~/somefile", config.getListFilePath());
        assertEquals("Election method is correct", ElectionMethod.BULLY, config.getElectionMethod());
    }

    @Test
    public void parseArgsWhenAllNoIdValue() {
        exit.expectSystemExitWithStatus(1);
        String[] args = {"-f", "~/somefile", "-e", "BULLY", "-i"};
        ArgumentParser.parseArgs(args);
    }
}