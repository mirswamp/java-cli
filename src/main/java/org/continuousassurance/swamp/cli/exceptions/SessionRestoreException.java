package org.continuousassurance.swamp.cli.exceptions;

public class SessionRestoreException extends SwampApiWrapperException {
	public SessionRestoreException(String msg){
		super(msg);
		setExitCode(SwampApiWrapperExitCodes.SESSION_RESTORE_ERROR);
	}
}
