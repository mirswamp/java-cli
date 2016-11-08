package edu.wisc.cs.swamp;

import edu.wisc.cs.swamp.SwampApiWrapper;
import edu.wisc.cs.swamp.SwampApiWrapper.HostType;

public class TestSwampApiWrapperDev {

	public static void main(String[] args) {
		try {
			SwampApiWrapper test_api = new SwampApiWrapper(HostType.DEVELOPMENT, null);
			test_api.login(args[0], args[1]);
			test_api.printUserInfo();
			test_api.printAllProjects();
			test_api.printAllPackages(null, true);
			test_api.printAllTools(null);
			test_api.printAllPlatforms(null);
			test_api.runAssessment("e4dd79ce-9a49-497f-b739-179acd2770cb", 
					"163d56a7-156e-11e3-a239-001a4a81450b", 
					"949c3fc8-e83e-11e3-a7ca-001a4a814505", 
					"fc55810b-09d7-11e3-a239-001a4a81450b");
		}catch (Exception name) {
			name.printStackTrace(System.out);
		}
		
/*		List<? extends Project> projects =handlerFactory.getProjectHandler().getAll();
		for(Project proj : projects) {
			System.out.printf("%s\n", proj.getUUIDString());
		}
		List<? extends Tool> tools = handlerFactory.getToolHandler().getAll();
		System.out.printf("%-21s %s %38s %26s\n", "Tool name" ,"Tool UUID" ,"Create Date" , "Sharing Status" );
		for(Tool tool : tools) {
			System.out.printf("%-21s %s %s %s\n", tool.getName() ,tool.getUUIDString() ,tool.getCreateDate() , tool.getToolSharingStatus() );
		}
*/
	}

}
