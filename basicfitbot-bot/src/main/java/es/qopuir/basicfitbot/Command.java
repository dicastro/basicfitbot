package es.qopuir.basicfitbot;

public interface Command {
    CommandType getCommand();

    String getArgs();
}