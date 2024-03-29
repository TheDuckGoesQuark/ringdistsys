package config;

import node.electionhandlers.ElectionMethod;
import org.apache.commons.cli.*;


public class ArgumentParser {

    private static final String ID = "id";
    private static final String LIST_FILE = "list";
    private static final String ELECTION_METHOD = "election";
    private static final String DROP_EVERYTHING = "drop";

    private static Options buildOptions() {
        Options options = new Options();

        Option nodeId = new Option("i", ID, true, "node ID");
        nodeId.setRequired(true);
        nodeId.setType(Number.class);
        options.addOption(nodeId);

        Option list = new Option("f", LIST_FILE, true, "Path to file containing list of nodes (resource P)");
        list.setRequired(true);
        options.addOption(list);

        Option elec = new Option("e", ELECTION_METHOD, true, "Election method to use (RING_BASED/CHANG_ROBERTS/BULLY)");
        elec.setRequired(true);
        options.addOption(elec);

        Option drop = new Option("d", DROP_EVERYTHING, false, "Include if this node should trigger a database refresh");
        options.addOption(drop);

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

        int inputId = 0;
        String listFile = null;
        ElectionMethod electionMethod = null;
        boolean dropEverything = false;

        try {
            CommandLine cmd = parser.parse(options, args);

            inputId = ((Number) cmd.getParsedOptionValue(ID)).intValue();
            listFile = cmd.getOptionValue(LIST_FILE);
            electionMethod = ElectionMethod.valueOf(cmd.getOptionValue(ELECTION_METHOD, String.valueOf(ElectionMethod.RING_BASED)));
            dropEverything = cmd.hasOption(DROP_EVERYTHING);

        } catch (ParseException e) {
            System.out.println(e.getMessage());
            printHelpAndDie(options);
        }

        return new Configuration(inputId, listFile, electionMethod, dropEverything);
    }
}
