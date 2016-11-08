package edu.wisc.cs.swamp.exceptions;

public class SessionSaveException extends SwampApiWrapperException {
	public SessionSaveException(String msg){
		super(msg);
		setExitCode(SwampApiWrapperExitCodes.SESSION_SAVE_ERROR);
	}
}
