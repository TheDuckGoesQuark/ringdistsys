package chat.ui.cli;

public enum MenuOption {

    EXIT("Terminates the program."),
    LOGIN("Join chat."),
    LOGOUT("Leave chat."),
    JOIN("Join a group."),
    LEAVE("Leave a group."),
    SEND("Send message to a group or user."),
    SHOW("Show recent messages."),
    USER("Send message to user."),
    GROUP("Send message to group."),
    BACK("Go back."),
    GROUPS("Show current groups.");

    private String description;

    MenuOption(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
