package edu.wisc.cs.swamp.exceptions;

public class SessionRestoreException extends SwampApiWrapperException {
	public SessionRestoreException(String msg){
		super(msg);
		setExitCode(SwampApiWrapperExitCodes.SESSION_RESTORE_ERROR);
	}
}
