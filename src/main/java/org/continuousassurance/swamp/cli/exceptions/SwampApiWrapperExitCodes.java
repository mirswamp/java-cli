package org.continuousassurance.swamp.cli.exceptions;

public enum SwampApiWrapperExitCodes {
	NO_ERRORS(0), 
	INVALID_CLI_OPTIONS(1),
	CLI_PARSER_ERROR(2),
	INVALID_UUID(3),
	INCOMPATIBLE_TUPLE(4),
	SESSION_EXPIRED(5),
	SESSION_RESTORE_ERROR(6),
	SESSION_SAVE_ERROR(7),
	
	HTTP_EXCEPTION(11),
	HTTP_GENERAL_EXCEPTION(12),
	;
	private int exitCode;
	 
	public int getExitCode() {
		return exitCode;
	}

	protected void setExitCode(int exitCode) {
		this.exitCode = exitCode;
	}

	private SwampApiWrapperExitCodes(int exit_code) {
		setExitCode(exit_code);
	}
	
	public static int NormalizeHttpExitCode(int http_exit_code) {
		int exit_code = HTTP_EXCEPTION.getExitCode();
		
		if (http_exit_code >= 400 && http_exit_code < 500){
			exit_code = http_exit_code - 380;
		}else if (http_exit_code >= 500 && http_exit_code <= 599){
			exit_code = http_exit_code - 460;
		}
		return exit_code;
	}
}
