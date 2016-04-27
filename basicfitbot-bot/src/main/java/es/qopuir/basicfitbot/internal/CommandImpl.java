package es.qopuir.basicfitbot.internal;

import es.qopuir.basicfitbot.Command;
import es.qopuir.basicfitbot.CommandType;

public class CommandImpl implements Command {
	private CommandType command;
	private String args;

    public CommandImpl() {
	}

	@Override
	public CommandType getCommand() {
		return command;
	}

	@Override
	public String getArgs() {
		return args;
	}

	public void setCommand(String command) {
		this.command = CommandType.fromText(command);
	}

	public void setArgs(String args) {
		this.args = args;
	}
}