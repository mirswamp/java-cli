package org.continuousassurance.swamp.cli.exceptions;

public class SessionSaveException extends SwampApiWrapperException {
	public SessionSaveException(String msg){
		super(msg);
		setExitCode(SwampApiWrapperExitCodes.SESSION_SAVE_ERROR);
	}
}
