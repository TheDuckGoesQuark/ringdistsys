package chat.ui.cli;

import java.util.Arrays;
import java.util.Optional;
import java.util.Scanner;

class CLIMenu {

    private final MenuOption[] options;
    private final Scanner reader;

    CLIMenu(MenuOption[] options, Scanner reader) {
        this.options = options;
        this.reader = reader;
    }

    MenuOption getChoice() {
        Optional<MenuOption> choice;

        do {
            printOptions();

            if (reader.hasNextInt()) {
                choice = getChoiceFromIndex(reader.nextInt());
                // Consume new line
                reader.nextLine();
            } else {
                choice = getChoiceFromName(reader.nextLine());
            }
        } while (!choice.isPresent());

        return choice.get();
    }

    private void printOptions() {
        System.out.println("Options:");
        for (int i = 0; i < options.length; i++) {
            final MenuOption option = options[i];
            System.out.printf("%d: %s - %s\n", i + 1, option.name(), option.getDescription());
        }
        System.out.println("Enter option name or number and press enter.");
    }

    private Optional<MenuOption> getChoiceFromName(String nextLine) {
        return Arrays.stream(options)
                .filter(options -> options.name().equals(nextLine))
                .findFirst();
    }

    private Optional<MenuOption> getChoiceFromIndex(int nextInt) {
        int index = nextInt - 1;

        if (index >= options.length) return Optional.empty();
        else return Optional.ofNullable(options[index]);
    }
}
