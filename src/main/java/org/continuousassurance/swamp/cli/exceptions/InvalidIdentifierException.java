package org.continuousassurance.swamp.cli.exceptions;

public class InvalidIdentifierException extends SwampApiWrapperException {
	public InvalidIdentifierException(String msg){
		super(msg);
		setExitCode(SwampApiWrapperExitCodes.INVALID_UUID);
	}
}
