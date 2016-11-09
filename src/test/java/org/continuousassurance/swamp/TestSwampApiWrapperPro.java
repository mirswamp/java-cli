package org.continuousassurance.swamp;

import org.continuousassurance.ncsa.swamp.cli.SwampApiWrapper;
import org.continuousassurance.ncsa.swamp.cli.SwampApiWrapper.HostType;

public class TestSwampApiWrapperPro {

	public static void main(String[] args) {
		try {
			SwampApiWrapper test_api = new SwampApiWrapper(HostType.PRODUCTION, null);
			
			test_api.login(args[0], args[1]);
			test_api.printUserInfo();
			test_api.printAllProjects();
			test_api.printAllPackages(null, true);
			test_api.printAllTools(null);
			test_api.printAllPlatforms(null);

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
