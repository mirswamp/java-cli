package org.continuousassurance.swamp.cli.util;

import java.util.Arrays;
import java.util.List;

public enum AssessmentStatus {
	INPROGRESS,
	FAILED,
	SUCCESS,
	UNKNOWN;
	
	public static String getStatusString(AssessmentStatus status) {
		String str;
		
		switch (status) {
		case INPROGRESS:
			str = "Inprogess";
			break;
		case FAILED:
			str = "Failed";
			break;
		case SUCCESS:
			str = "Success";
			break;
		default:
			str = "Unknown";
			break;
		}
		
		return str;
	}
	
	private static boolean inList(List<String> status_list, String status_str) {
		for (String str : status_list) {
			if (str.equalsIgnoreCase(status_str)) {
				return true;
			}
		}
		return false;
	}
	
	public static AssessmentStatus translateAssessmentStatus(String status_str) {
		
		List<String> inprogess_status_list = Arrays.asList( "WAITING TO START",
				"SUBMITTING TO HTCONDOR",
				"Demand Queued",
				"Swamp Off Queued", 
				"Drain ReLaunch",
				"Drain ReQueued",
				"Creating HTCondor Job",
				"Waiting in HTCondor Queue",
				"Failed to submit to HTCondor",
				"Starting Virtual Machine",
				"Unable to Start VM",
				"vm failed - [start time] [current time] ([seconds between current and start]",
				"vm started and failed - [start time] [current time] ([seconds between current and start]",
				"vm [raw virsh command]",
				"vm [raw virsh command] failed",
				"Obtaining VM IP Address",
				"Obtaining Viewer Machine IP Address",
				"Obtained VM IP",
				"Failed to Obtain VM IP Address",
				"Starting Assessment Run Script", 
				"Executing cloc on package",
				"Performing Assessment",
				"Shutting Down the VM",
				"Shutting down the assessment machine",
				"Extracting Assessment Results",
				"Failed to extract assessment results",
				"Post-Processing",
				"Assessment failed (logged but not set as status)",
				"Assessment Passed (logged but not set as status)",
				"Assessment retry  (logged but not set as status)",
				"Assessment result not found",
				"Failed to parse assessment results",
				"Failed to preserve assessment results",
				"Failed to compute assessment result metrics",
				"Saving Results",
				"Failed to save assessment results in database",
				"Terminating"
				);
		
		List<String> failed_status_list = Arrays.asList("FAILED TO VALIDATE ASSESSMENT DATA",
				"FAILED TO START",
				"Finished with Errors",
				"Finished with Errors - Retry",
				"Terminated");
		
		List<String> success_status_list = Arrays.asList("Finished",
				"Finished with Warnings");
		
		if (inList(inprogess_status_list, status_str)) {
			// System.out.println("inprogress status: '" + status_str + "'");
			return AssessmentStatus.INPROGRESS;
		}
		
		if (inList(failed_status_list, status_str)) {
			return AssessmentStatus.FAILED;
		}
		
		if (inList(success_status_list, status_str)) {
			return AssessmentStatus.SUCCESS;
		}
		
		if (status_str.startsWith("Finish with Errors")) {
			return AssessmentStatus.FAILED;
		}
		
		if (status_str.startsWith("Finished")) {
			return AssessmentStatus.SUCCESS;
		}

		// System.out.println("unknown status: '" + status_str + "'");
		
		return AssessmentStatus.UNKNOWN;
	}
}
