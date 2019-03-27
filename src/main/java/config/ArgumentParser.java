package config;

import org.apache.commons.cli.*;

import java.net.InetAddress;
import java.net.UnknownHostException;


public class ArgumentParser {

    private static final String HOST = "host";
    private static final String PORT = "port";
    private static final String ID = "id";
    private static final String LIST_FILE = "list";

    private static Options buildOptions() {
        Options options = new Options();

        Option host = new Option("a", HOST, true, "host address");
        host.setRequired(true);
        options.addOption(host);

        Option port = new Option("p", PORT, true, "port number");
        port.setRequired(true);
        port.setType(Number.class);
        options.addOption(port);

        Option nodeId = new Option("i", ID, true, "node ID");
        nodeId.setRequired(true);
        nodeId.setType(Number.class);
        options.addOption(nodeId);

        Option list = new Option("f", LIST_FILE, true, "path to file containing list of nodes (resource P)");
        list.setRequired(true);
        options.addOption(list);

        return options;
    }

    private static void printHelpAndDie(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar <program>.jar", options);
        System.exit(1);
    }

    public static Configuration parseArgs(String[] args) {
        Options options = buildOptions();

        CommandLineParser parser = new DefaultParser();

        InetAddress inputHost = null;
        int inputPort = 0;
        int inputId = 0;
        String listFile = null;

        try {
            CommandLine cmd = parser.parse(options, args);

            inputHost = InetAddress.getByName(cmd.getOptionValue(HOST));
            inputPort = ((Number) cmd.getParsedOptionValue(PORT)).intValue();
            inputId = ((Number) cmd.getParsedOptionValue(ID)).intValue();
            listFile = cmd.getOptionValue(LIST_FILE);

        } catch (ParseException | UnknownHostException e) {
            System.out.println(e.getMessage());
            printHelpAndDie(options);
        }

        return new Configuration(inputHost, inputPort, inputId, listFile);
    }
}
