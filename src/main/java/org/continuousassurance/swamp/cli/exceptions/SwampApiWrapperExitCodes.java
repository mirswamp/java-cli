/*
*  java-cli
*
*  Copyright 2016 Vamshi Basupalli <vamshi@cs.wisc.edu>, Malcolm Reid <mreid3@wisc.edu>, Jared Sweetland <jsweetland@wisc.edu>
*
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*/

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
	NO_DEFAULT_PLATFORM(8),
	INVALID_NAME(4),
	
	HTTP_EXCEPTION(20),
	HTTP_GENERAL_EXCEPTION(30),
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
