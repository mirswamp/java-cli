package edu.wisc.cs.swamp.exceptions;

public class IncompatibleAssessmentTupleException extends SwampApiWrapperException {
	public IncompatibleAssessmentTupleException(String msg){
		super(msg);
		setExitCode(SwampApiWrapperExitCodes.INCOMPATIBLE_TUPLE);
	}
}
