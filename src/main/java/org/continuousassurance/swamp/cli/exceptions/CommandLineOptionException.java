package org.continuousassurance.swamp.cli.exceptions;

public class CommandLineOptionException extends SwampApiWrapperException {

	public CommandLineOptionException(String msg){
		super(msg);
		setExitCode(SwampApiWrapperExitCodes.INVALID_CLI_OPTIONS);
	}

}
