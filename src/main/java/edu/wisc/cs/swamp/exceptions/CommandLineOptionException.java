package edu.wisc.cs.swamp.exceptions;

public class CommandLineOptionException extends SwampApiWrapperException {

	public CommandLineOptionException(String msg){
		super(msg);
		setExitCode(SwampApiWrapperExitCodes.INVALID_CLI_OPTIONS);
	}

}
