package org.continuousassurance.swamp.cli.exceptions;

public class SessionExpiredException extends SwampApiWrapperException {
	public SessionExpiredException(String msg){
		super(msg);
		setExitCode(SwampApiWrapperExitCodes.SESSION_EXPIRED);
	}
}
