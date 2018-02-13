package org.continuousassurance.swamp.cli.exceptions;

public class NoDefaultPlatformException extends SwampApiWrapperException {
	public NoDefaultPlatformException(Exception exception){
		super(exception);
		setExitCode(SwampApiWrapperExitCodes.NO_DEFAULT_PLATFORM);
	}
	
	public NoDefaultPlatformException(String msg){
        super(msg);
        setExitCode(SwampApiWrapperExitCodes.NO_DEFAULT_PLATFORM);
    }
}
