package chat.ui;

public enum MenuOption {

    EXIT("Terminates the program."),
    LOGIN("Join chat."),
    LOGOUT("Leave chat."),
    JOIN("Join a group."),
    LEAVE("Leave a group."),
    SENDMESSAGE("Send message to a group or user"),
    SHOW("Show recent messages");

    private String description;

    MenuOption(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
