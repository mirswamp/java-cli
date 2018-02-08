package org.continuousassurance.swamp.cli.exceptions;

public class UnkownPlatformException extends SwampApiWrapperException {
	public UnkownPlatformException(Exception exception){
		super(exception);
		setExitCode(SwampApiWrapperExitCodes.INVALID_UUID);
	}
	
	public UnkownPlatformException(String msg){
        super(msg);
        setExitCode(SwampApiWrapperExitCodes.INVALID_UUID);
    }
}
