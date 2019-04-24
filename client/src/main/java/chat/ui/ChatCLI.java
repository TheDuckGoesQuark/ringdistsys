package chat.ui;

import chat.client.ChatClient;
import chat.client.RingChatClient;
import chat.client.messages.ChatMessage;

import java.io.IOException;
import java.util.Scanner;

import static chat.ui.MenuOption.*;

public class ChatCLI implements ChatUI {

    private final Scanner reader = new Scanner(System.in);
    private final CLIMenu LOGIN = new CLIMenu(new MenuOption[]{MenuOption.LOGIN, EXIT}, reader);
    private final CLIMenu HOME = new CLIMenu(new MenuOption[]{JOIN, LEAVE, SEND, GROUPS, SHOW, LOGOUT}, reader);
    private final CLIMenu CHAT = new CLIMenu(new MenuOption[]{USER, GROUP, BACK}, reader);

    private final ChatClient client;

    public ChatCLI(String serverAddress, int serverPort) throws IOException {
        this.client = new RingChatClient(serverAddress, serverPort);
    }

    @Override
    public void start() {
        MenuOption currentOption;

        do {
            if (client.loggedIn())
                currentOption = HOME.getChoice();
            else
                currentOption = LOGIN.getChoice();

            switch (currentOption) {
                case JOIN:
                    handleJoin();
                    break;
                case LEAVE:
                    handleLeave();
                    break;
                case SEND:
                    handleSend();
                    break;
                case SHOW:
                    handleShow();
                    break;
                case GROUPS:
                    handleShowGroups();
                    break;
                case LOGIN:
                    handleLogin();
                    break;
                case LOGOUT:
                    handleLogout();
                    break;
                case EXIT:
                    break;
            }
        } while (currentOption != EXIT);
    }

    /**
     * Shows list of users current groups
     */
    private void handleShowGroups() {
        try {
            System.out.println(String.format("User %s is part of these groups:", client.getUsername()));

            for (String groupName : client.getGroups()) {
                System.out.println(groupName);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        System.out.println("----------------------------------------------------------");
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
            System.err.println("Unable to logout: " + e.getMessage());
        }
    }

    /**
     * Show messages that have arrived since last check
     */
    private void handleShow() {
        System.out.println("UNREAD MESSAGES:");
        try {
            for (ChatMessage chatMessage : client.receiveMessages()) {
                System.out.println("----------------------------------------------------------");
                System.out.println(chatMessage.toPrettyString());
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        System.out.println("----------------------------------------------------------");
    }

    /**
     * Sends message to either a group or another user
     */
    private void handleSend() {
        final MenuOption whoTo = CHAT.getChoice();
        switch (whoTo) {
            case USER:
                handleSendToUser();
            case GROUP:
                handleSendToGroup();
            case BACK:
                break;
        }
    }

    /**
     * Sends a message to a group
     */
    private void handleSendToGroup() {
        System.out.println("Type name of group to send to and press enter:");
        final String groupName = reader.nextLine();
        System.out.println(String.format("Type message for group '%s' and press enter:", groupName));
        final String message = reader.nextLine();

        try {
            client.sendMessageToUser(message, groupName);
        } catch (IOException e) {
            System.err.println(String.format("Unable to send to group %s: %s", groupName, e.getMessage()));
        }
    }

    /**
     * Sends a message to a user
     */
    private void handleSendToUser() {
        System.out.println("Type username to send to and press enter:");
        final String username = reader.nextLine();
        System.out.println(String.format("Type message for '%s' and press enter:", username));
        final String message = reader.nextLine();

        try {
            client.sendMessageToUser(message, username);
        } catch (IOException e) {
            System.err.println(String.format("Unable to send to user %s: %s", username, e.getMessage()));
        }
    }

    /**
     * Leaves a group
     */
    private void handleLeave() {
        System.out.println("Type name of group to leave and press enter:");
        final String groupName = reader.nextLine();

        try {
            client.leaveGroup(groupName);
        } catch (IOException e) {
            System.err.println(String.format("Unable to leave group %s: %s", groupName, e.getMessage()));
        }
    }

    /**
     * Joins a group
     */
    private void handleJoin() {
        System.out.println("Type name of group to join and press enter:");
        final String groupName = reader.nextLine();

        try {
            client.joinGroup(groupName);
        } catch (IOException e) {
            System.err.println(String.format("Unable to join group %s: %s", groupName, e.getMessage()));
        }
    }
}
