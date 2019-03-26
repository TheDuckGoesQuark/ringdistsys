import config.ArgumentParser;
import config.Configuration;

public class Main {
    public static void main(String[] args) {
        Configuration configuration = ArgumentParser.parseArgs(args);
    }
}
