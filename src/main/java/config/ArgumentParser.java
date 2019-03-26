package config;

import org.apache.commons.cli.*;

import java.net.InetAddress;
import java.net.UnknownHostException;


public class ArgumentParser {

    private static final String HOST = "host";
    private static final String PORT = "port";
    private static final String ID = "id";
    private static final String HELP = "help";

    private static Options buildOptions() {
        Options options = new Options();

        Option help = new Option("h", HELP, false, "show usage");
        help.setRequired(false);
        options.addOption(help);

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

        boolean help = false;
        InetAddress inputHost = null;
        int inputPort = 0;
        int inputId = 0;

        try {
            CommandLine cmd;

            cmd = parser.parse(options, args);

            if (cmd.hasOption(HELP)) {
                printHelpAndDie(options);
            }

            inputHost = InetAddress.getByName(cmd.getOptionValue(HOST));
            inputPort = ((Number) cmd.getParsedOptionValue(PORT)).intValue();
            inputId = ((Number) cmd.getParsedOptionValue(ID)).intValue();

        } catch (ParseException | UnknownHostException e) {
            System.out.println(e.getMessage());
            printHelpAndDie(options);
        }

        return new Configuration(inputHost, inputPort, inputId);
    }
}
