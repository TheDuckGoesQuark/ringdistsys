import chat.ui.cli.ChatCLI;
import chat.ui.ChatUI;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            throw new IllegalArgumentException("Program requires a server address and server port.");
        }

        final String serverAddress = args[0];
        final int serverPort = Integer.parseInt(args[1]);
        final ChatUI ui = new ChatCLI(serverAddress, serverPort);

        ui.start();

    }
}
