package es.qopuir.basicfitbot;

import java.util.stream.Stream;

public enum CommandType {
    UNKNOWN("unknown"), START("/start"), HELP("/help"), HORARIO("/horario");

    private final String text;

    private CommandType(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public static CommandType fromText(String textCommand) {
        // @formatter:off
        return Stream.of(CommandType.values())
                .filter(commandType -> commandType.getText().equalsIgnoreCase(textCommand))
                .findFirst()
                .orElse(UNKNOWN);
        // @formatter:on
    }
}