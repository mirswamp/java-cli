package edu.illinois.ncsa.swamp.cli.exceptions;

public abstract class SwampApiWrapperException  extends RuntimeException {
	SwampApiWrapperExitCodes exit_code;
	
	SwampApiWrapperException(String msg){
		super(msg);
	}
	public void setExitCode(SwampApiWrapperExitCodes ec){
		exit_code = ec;
	}

	public int getExitCode(){
		return exit_code.getExitCode();
	}
}
