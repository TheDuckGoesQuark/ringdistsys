package chat.ui;

import chat.client.ChatClient;
import chat.client.RingChatClient;

import java.io.IOException;
import java.util.Scanner;

import static chat.ui.MenuOption.*;

public class ChatCLI implements ChatUI {

    private final Scanner reader = new Scanner(System.in);
    private final CLIMenu loginMenu = new CLIMenu(new MenuOption[]{LOGIN, EXIT}, reader);
    private final CLIMenu chatMenu = new CLIMenu(new MenuOption[]{JOIN, LEAVE, SENDMESSAGE, SHOW, LOGOUT}, reader);
    private final ChatClient client;

    public ChatCLI(String serverAddress, int serverPort) throws IOException {
        this.client = new RingChatClient(serverAddress, serverPort);
    }

    @Override
    public void start() {
        MenuOption currentOption;

        do {
            if (client.loggedIn())
                currentOption = chatMenu.getChoice();
            else
                currentOption = loginMenu.getChoice();

            switch (currentOption) {
                case JOIN:
                    handleJoin();
                    break;
                case LEAVE:
                    handleLeave();
                    break;
                case SENDMESSAGE:
                    handleSend();
                    break;
                case SHOW:
                    handleShow();
                    break;
                case LOGIN:
                    handleLogin();
                    break;
                case LOGOUT:
                    handleLogout();
                    break;
            }
        } while (currentOption != EXIT);
    }

    /**
     * Attempts to log user in by requesting username
     */
    private void handleLogin() {
        System.out.println("Type username and press enter:");
        try {
            final String username = reader.nextLine();
            client.logInAs(username);
        } catch (IOException e) {
            System.err.println("Unable to login: " + e.getMessage());
        }
    }

    /**
     * Logs this user out
     */
    private void handleLogout() {
        try {
            client.logout();
        } catch (IOException e) {
            System.err.println("Unable to login: " + e.getMessage());
        }
    }

    /**
     * Show messages that have arrived since last check
     */
    private void handleShow() {
    }

    /**
     * Sends message to either a group or another user
     */
    private void handleSend() {
    }

    /**
     * Leaves a group
     */
    private void handleLeave() {
    }

    /**
     * Joins a group
     */
    private void handleJoin() {
    }
}
