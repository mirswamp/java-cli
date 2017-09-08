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

package org.continuousassurance.swamp.cli;

import edu.uiuc.ncsa.security.core.exceptions.GeneralException;

import org.continuousassurance.swamp.api.AssessmentRecord;
import org.continuousassurance.swamp.api.AssessmentRun;
import org.continuousassurance.swamp.api.PackageThing;
import org.continuousassurance.swamp.api.PackageVersion;
import org.continuousassurance.swamp.api.PlatformVersion;
import org.continuousassurance.swamp.api.Project;
import org.continuousassurance.swamp.api.Tool;
import org.continuousassurance.swamp.api.User;
import org.continuousassurance.swamp.session.HTTPException;
import org.apache.commons.cli.*;
import org.apache.log4j.varia.NullAppender;
import org.continuousassurance.swamp.cli.exceptions.*;
import org.continuousassurance.swamp.cli.util.AssessmentStatus;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

public class Cli {

	SwampApiWrapper api_wrapper;

	public Cli() throws Exception {
		api_wrapper = new SwampApiWrapper();
	}

	/*
    public Cli(String host_name) throws Exception {
        api_wrapper = new SwampApiWrapper(host_name);
    }*/

	public static final ArrayList<String> COMMANDS = new ArrayList<String>(Arrays.asList(
			"login", "logout", "assess", "project",
			"package",
			"platform", "results", "status", "tools", "user"));

	public static void printHelp() {
		System.out.println("------------------------------------------------------------------------");
		System.out.println("Usage: <program> <sub-command> <options>");
		System.out.println("------------------------------------------------------------------------");
		System.out.println("<sub-command> must be one of the following:");
		for (String cmd :  COMMANDS) {
			System.out.println("\t\t" + cmd);
		}
		System.out.println("------------------------------------------------------------------------");
		System.out.println("For information on the <options> for a <sub-command> execute:");
		System.out.println("\t<program> <sub-command> --help or <program> <sub-command> -h");
	}

	protected HashMap<String, Object> getUserCredentials(String filename) {
		HashMap<String, Object> cred_map = new HashMap<String, Object>();
		Properties prop = new Properties();

		try {
			prop.load(new FileInputStream(filename));
			cred_map.put("username",prop.getProperty("username"));
			cred_map.put("password", prop.getProperty("password"));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return cred_map;
	}

	public HashMap<String, Object> loginOptionsHandler(ArrayList<String> args) throws ParseException, CommandLineOptionException {

		Options options = new Options();
		options.addOption(Option.builder("H").required(false).longOpt("help").desc("Shows Help").build());

		options.addOption(Option.builder("F").required(false).hasArg().longOpt("filepath").argName("CREDENTIALS_FILEPATH")
				.desc("Properties file containing username and password").build());
		options.addOption(Option.builder("C").required(false).hasArg(false).longOpt("console")
				.desc("Accepts username and password from the terminal").build());
		options.addOption(Option.builder("S").required(false).hasArg().longOpt("swamp-host").argName("SWAMP_HOST")
				.desc("URL for SWAMP host: default is " + SwampApiWrapper.SWAMP_HOST_NAME).build());

		String[] cmd_args = (String[]) args.toArray(new String[0]);
		CommandLine parsed_options = new DefaultParser().parse(options, cmd_args);
		if (args.size() == 0 || parsed_options.hasOption("H")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("Command Line Parameters", options);
			return null;
		}else if (parsed_options.hasOption("F")) {
			HashMap<String, Object> cred_map = getUserCredentials(parsed_options.getOptionValue("F"));
			if ((cred_map.get("username") != null ) && (cred_map.get("password") != null)){
				cred_map.put("swamp-host", parsed_options.getOptionValue("S", SwampApiWrapper.SWAMP_HOST_NAME));
				return cred_map;
			}else {
				throw new CommandLineOptionException(String.format("No username or password in the file: %s\n",
						parsed_options.getOptionValue("F")));
			}
		}else {
			System.out.print("USERNAME:");
			String username = System.console().readLine();
			System.out.print("PASSWORD:");
			String password = new String(System.console().readPassword());
			HashMap<String, Object> cred_map = new HashMap<String, Object>();
			cred_map.put("username", username);
			cred_map.put("password", password);
			cred_map.put("swamp-host", parsed_options.getOptionValue("S", SwampApiWrapper.SWAMP_HOST_NAME));
			return cred_map;
		}
	}

	public HashMap<String, Object> projectOptionsHandler(ArrayList<String> args) throws ParseException {

		Options options = new Options();
		OptionGroup opt_grp = new OptionGroup();
		opt_grp.setRequired(true);

		opt_grp.addOption(Option.builder("H").required(false).longOpt("help").desc("Shows Help").build());
		opt_grp.addOption(Option.builder("L").required(false).hasArg(false).longOpt("list")
				.desc("List projects").build());
		opt_grp.addOption(Option.builder("N").required(false).hasArg(true).longOpt("project-name")
				.desc("Specify a the project name and get the uuid from it").build());
		options.addOptionGroup(opt_grp);

		String[] cmd_args = (String[]) args.toArray(new String[0]);
		CommandLine parsed_options = new DefaultParser().parse(options, cmd_args);
		if (args.size() == 0 || parsed_options.hasOption("H")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("Command Line Parameters", options);
			return null;
		}else {
			HashMap<String, Object> cred_map = new HashMap<String, Object>();
			if (parsed_options.hasOption("project-name")){
				cred_map.put("project-name",parsed_options.getOptionValue("project-name"));
			}
			//cred_map.put("list", "list"); Not required
			return cred_map;
		}
	}

	public HashMap<String, Object> resultsOptionsHandler(ArrayList<String> args) throws ParseException, CommandLineOptionException {

		Options options = new Options();
		OptionGroup opt_grp = new OptionGroup();
		opt_grp.setRequired(true);

		opt_grp.addOption(Option.builder("H").required(false).longOpt("help").desc("Shows Help").build());
		opt_grp.addOption(Option.builder("R").required(false).hasArg(true).longOpt("results-uuid")
				.desc("Assessment Results UUID of a project").build());
		options.addOptionGroup(opt_grp);

		options.addOption(Option.builder("P").required(false).hasArg(true).longOpt("project-uuid")
				.desc("Project UUID of a project").build());
		options.addOption(Option.builder("F").required(false).hasArg(true).longOpt("file-path")
				.desc("Filepath to write SCARF Results into").build());

		String[] cmd_args = (String[]) args.toArray(new String[0]);
		CommandLine parsed_options = new DefaultParser().parse(options, cmd_args);
		if (args.size() == 0 || parsed_options.hasOption("H")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("Command Line Parameters", options);
			return null;
		}else {
			HashMap<String, Object> cred_map = new HashMap<String, Object>();
			if(parsed_options.hasOption("project-uuid") || parsed_options.hasOption("file-path")){
				cred_map.put("project-uuid", parsed_options.getOptionValue("project-uuid"));
				cred_map.put("results-uuid", parsed_options.getOptionValue("results-uuid"));
				cred_map.put("file-path", parsed_options.getOptionValue("file-path"));
				return cred_map;
			}else if(!parsed_options.hasOption("project-uuid")){
				throw new CommandLineOptionException("Required -P --project-uuid <project-uuid> option");
			}else {
				throw new CommandLineOptionException("Required -F --file-path <scarf-file-path> option");
			}
		}
	}

	public HashMap<String, Object> statusOptionsHandler(ArrayList<String> args) throws ParseException, CommandLineOptionException {

		Options options = new Options();
		options.addOption(Option.builder("H").required(false).longOpt("help").desc("Shows Help").build());
		options.addOption(Option.builder("P").required(false).hasArg(true).longOpt("project-uuid")
				.desc("Project UUID of the project").build());
		options.addOption(Option.builder("A").required(false).hasArg(true).longOpt("assess-uuid")
				.desc("assessment UUID of an assessment run").build());

		String[] cmd_args = (String[]) args.toArray(new String[0]);
		CommandLine parsed_options = new DefaultParser().parse(options, cmd_args);
		if (args.size() == 0 || parsed_options.hasOption("H")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("Command Line Parameters", options);
			return null;
		}else if(parsed_options.hasOption("project-uuid")){
			HashMap<String, Object> cred_map = new HashMap<String, Object>();
			cred_map.put("project-uuid", parsed_options.getOptionValue("project-uuid"));
			if(parsed_options.hasOption("assess-uuid")){
				cred_map.put("assess-uuid", parsed_options.getOptionValue("assess-uuid"));
			}
			return cred_map;
		}else {
			throw new CommandLineOptionException("Required -P --project-uuid <project-uuid> option");
		}
	}

	public static String optionMissingStr(Option option) {
		return String.format("Missing options/arguments: [-%s --%s <%s>]\n",
				option.getOpt(),
				option.getLongOpt(),
				option.getArgName());
	}

	public HashMap<String, Object> packageOptionsHandler(ArrayList<String> args) throws ParseException, CommandLineOptionException {

		Options options = new Options();
		OptionGroup opt_grp = new OptionGroup();
		opt_grp.setRequired(true);

		opt_grp.addOption(Option.builder("H").required(false).longOpt("help").desc("Shows Help").build());
		opt_grp.addOption(Option.builder("L").required(false).hasArg(false).longOpt("list")
				.desc("Show the list of packages that belong to the user").build());
		opt_grp.addOption(Option.builder("A").required(false).hasArg().argName("PACKAGE_ARCHIVE_FILEPATH").longOpt("pkg-archive")
				.desc("File path to the package archive file").build());
		opt_grp.addOption(Option.builder("T").required(false).hasArg(false).longOpt("pkg-types")
				.desc("list all package types").build());
		opt_grp.addOption(Option.builder("D").required(false).hasArgs().argName("PACKAGE_UUID").longOpt("pkg-uuid")
				.desc("Package UUID").build());
		
		options.addOptionGroup(opt_grp);

		options.addOption(Option.builder("C").required(false).hasArg().argName("PACKAGE_CONF_FILEPATH").longOpt("pkg-conf")
				.desc("File path to the package conf file").build());
		options.addOption(Option.builder("P").required(false).hasArg().argName("PROJECT_UUID").longOpt("project-uuid")
				.desc("Project UUID to add the package to").build());
		options.addOption(Option.builder("O").required(false).hasArg().argName("OS_DEPENDENCIES_CONF_FILEPATH").longOpt("os-deps-conf")
				.desc("Path to OS depedencies conf file").build());
		options.addOption(Option.builder("N").required(false).hasArg(false).longOpt("new-pkg")
				.desc("Flag that indicates if the package must be added as a fresh package, and not a package version").build());
		options.addOption(Option.builder("Q").required(false).hasArg(false).longOpt("quiet")
				.desc("Print only the Package UUID with no formatting").build());

		String[] cmd_args = (String[]) args.toArray(new String[0]);
		CommandLine parsed_options = new DefaultParser().parse(options, cmd_args);
		if (args.size() == 0 || parsed_options.hasOption("help")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("Command Line Parameters", options);
			return null;
		}else {
			HashMap<String, Object> cred_map = new HashMap<String, Object>();
			if (parsed_options.hasOption("Q")){
				cred_map.put("quiet", "quiet");
			}

			if (parsed_options.hasOption("L")){
				cred_map.put("list", "list");
				cred_map.put("project-uuid", parsed_options.getOptionValue("P", null));
				return cred_map;
			}else if (parsed_options.hasOption("T")){
				cred_map.put("pkg-types", "pkg-types");
				return cred_map;
			}else if (parsed_options.hasOption("A")){
				if (parsed_options.hasOption("C") && parsed_options.hasOption("P")) {
					cred_map.put("pkg-archive", parsed_options.getOptionValue("A"));
					cred_map.put("pkg-conf", parsed_options.getOptionValue("C"));
					cred_map.put("project-uuid", parsed_options.getOptionValue("P"));
					if(parsed_options.hasOption("N")){
						cred_map.put("new-pkg", "");						
					}
					if(parsed_options.hasOption("O")){
						cred_map.put("os-deps-conf", parsed_options.getOptionValue("O"));
					}
					return cred_map;
				}else if (!parsed_options.hasOption("C")){
					throw new CommandLineOptionException(optionMissingStr(options.getOption("C")));
				}else {
					throw new CommandLineOptionException(optionMissingStr(options.getOption("P")));
				}
			}else {
				// if (parsed_options.hasOption("D")) 
				if (parsed_options.hasOption("P")) {
					cred_map.put("delete", "delete");
					cred_map.put("project-uuid", parsed_options.getOptionValue("P"));
					cred_map.put("package-uuids", Arrays.asList(parsed_options.getOptionValues("D")));
					return cred_map;
				}else {
					throw new CommandLineOptionException(optionMissingStr(options.getOption("P")));
				}
			}
		}
	}

	public HashMap<String, Object> userOptionsHandler(ArrayList<String> args) throws ParseException{

		Options options = new Options();
		options.addOption(Option.builder("H").required(false).longOpt("help").desc("Shows Help").build());
		options.addOption(Option.builder("I").required(false).hasArg(false).longOpt("info")
				.desc("Displays info about the currently logged in user").build());

		String[] cmd_args = (String[]) args.toArray(new String[0]);
		CommandLine parsed_options = new DefaultParser().parse(options, cmd_args);
		if (args.size() == 0 || parsed_options.hasOption("help")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("Command Line Parameters", options);
			return null;
		}else {
			HashMap<String, Object> cred_map = new HashMap<String, Object>();
			if (parsed_options.hasOption("I")){
				cred_map.put("info", "info");
			}
			return cred_map;
		}
	}

	public HashMap<String, Object> toolsOptionsHandler(ArrayList<String> args) throws ParseException{

		Options options = new Options();
		OptionGroup opt_grp = new OptionGroup();
		opt_grp.setRequired(true);

		opt_grp.addOption(Option.builder("H").required(false).longOpt("help").desc("Shows Help").build());
		opt_grp.addOption(Option.builder("L").required(false).hasArg(false).longOpt("list")
				.desc("Displays all tools to the user").build());
		opt_grp.addOption(Option.builder("N").required(false).hasArg(true).argName("TOOL_NAME").longOpt("tool-name")
				.desc("Specify the tool name and get the uuid from it").build());
		options.addOptionGroup(opt_grp);

		options.addOption(Option.builder("P").required(false).hasArg().argName("PROJECT_UUID").longOpt("project-uuid")
				.desc("Project UUID for extra project specific tools").build());

		String[] cmd_args = (String[]) args.toArray(new String[0]);
		CommandLine parsed_options = new DefaultParser().parse(options, cmd_args);
		if (args.size() == 0 || parsed_options.hasOption("help")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("Command Line Parameters", options);
			return null;
		}else {
			HashMap<String, Object> cred_map = new HashMap<String, Object>();
			if (parsed_options.hasOption("L")){
				cred_map.put("list", "list");
			}
			cred_map.put("project-uuid", parsed_options.getOptionValue("P", null));
			if (parsed_options.hasOption("N")){
				cred_map.put("tool-name", parsed_options.getOptionValue("N", null));
			}
			return cred_map;
		}
	}

	public HashMap<String, Object> platformOptionsHandler(ArrayList<String> args) throws ParseException{

		Options options = new Options();
		OptionGroup opt_grp = new OptionGroup();
		opt_grp.setRequired(true);

		opt_grp.addOption(Option.builder("H").required(false).longOpt("help").desc("Shows Help").build());
		opt_grp.addOption(Option.builder("L").required(false).hasArg(false).longOpt("list")
				.desc("Show the list of packages that belong to the user").build());
		opt_grp.addOption(Option.builder("N").required(false).hasArg(true).argName("PLATFORM_NAME").longOpt("platform-name")
				.desc("Specify the platform name and get the uuid from it").build());
		options.addOptionGroup(opt_grp);
		options.addOption(Option.builder("T").required(false).hasArg(true).argName("PACKAGE_TYPE").longOpt("pkg-type")
				.desc("Specify the 'package type name' to get relevant platforms").build());


		String[] cmd_args = (String[]) args.toArray(new String[0]);
		CommandLine parsed_options = new DefaultParser().parse(options, cmd_args);
		if (args.size() == 0 || parsed_options.hasOption("help")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("Command Line Parameters", options);
		}else {
			HashMap<String, Object> cred_map = new HashMap<String, Object>();
			if (parsed_options.hasOption("L")){
				cred_map.put("list", "list");
			}
			if (parsed_options.hasOption("N")){
				cred_map.put("platform-name", parsed_options.getOptionValue("N", null));
			}

			if (parsed_options.hasOption("T")){
				cred_map.put("pkg-type", parsed_options.getOptionValue("T", null));
			}

			return cred_map;
		}
		return null;
	}

	public HashMap<String, Object> logoutOptionsHandler(ArrayList<String> args) throws ParseException{

		Options options = new Options();
		options.addOption(Option.builder("H").required(false).longOpt("help").desc("Shows Help").build());

		String[] cmd_args = (String[]) args.toArray(new String[0]);
		CommandLine parsed_options = new DefaultParser().parse(options, cmd_args);
		if (parsed_options.hasOption("help")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("Command Line Parameters", options);
			return null;
		}else {
			HashMap<String, Object> cred_map = new HashMap<String, Object>();
			cred_map.put("logout", "logout");
			return cred_map;
		}

	}

	public HashMap<String, Object> assessmentOptionsHandler(ArrayList<String> args) throws ParseException, CommandLineOptionException{

		Options options = new Options();
		OptionGroup opt_grp = new OptionGroup();
		opt_grp.setRequired(true);

		opt_grp.addOption(Option.builder("H").required(false).longOpt("help").desc("Shows Help").build());
		opt_grp.addOption(Option.builder("R").required(false).hasArg(false).longOpt("run-assess")
				.desc("Run an assessment").build());
		opt_grp.addOption(Option.builder("L").required(false).hasArg(false).longOpt("list-assess")
				.desc("List assessments from a package").build());
		opt_grp.addOption(Option.builder("A").required(false).hasArg(true).longOpt("assess-uuid")
				.desc("View an assessment with Assessment uuid").build());
		options.addOptionGroup(opt_grp);

		options.addOption(Option.builder("K").required(false).hasArg(true).longOpt("pkg-uuid")
				.desc("Package uuid provided").build());
		options.addOption(Option.builder("P").required(false).hasArg(true).longOpt("project-uuid")
				.desc("Project uuid provided").build());
		options.addOption(Option.builder("T").required(false).hasArgs().longOpt("tool-uuid")
				.desc("Tool uuid provided").build());
		options.addOption(Option.builder("F").required(false).hasArg(true).longOpt("platform-uuid")
				.desc("Platform uuid provided").build());
		options.addOption(Option.builder("Q").required(false).hasArg(false).longOpt("quiet")
				.desc("Print only the Assessment UUID with no formatting").build());

		String[] cmd_args = (String[]) args.toArray(new String[0]);
		CommandLine parsed_options = new DefaultParser().parse(options, cmd_args);
		HelpFormatter formatter = new HelpFormatter();
		if (args.size() == 0 || parsed_options.hasOption('H')) {
			formatter.printHelp("Command Line Parameters", options);
			return null;
		}
		HashMap<String, Object> cred_map = new HashMap<String, Object>();
		if (parsed_options.hasOption("Q")){
			cred_map.put("quiet", "quiet");
		}
		if (parsed_options.hasOption("P")){
			cred_map.put("project-uuid", parsed_options.getOptionValue("P"));
			if (parsed_options.hasOption("K")){
				cred_map.put("pkg-uuid", parsed_options.getOptionValue("K"));
				if (parsed_options.hasOption("T")){
					if (parsed_options.hasOption("F")){
						cred_map.put("platform-uuid", Arrays.asList(parsed_options.getOptionValues('F')));
					}
					//cred_map.put("tool-uuid", parsed_options.getOptionValue("T"));
					cred_map.put("tool-uuid", Arrays.asList(parsed_options.getOptionValues('T')));
					if (parsed_options.hasOption("R")){
						cred_map.put("run-assess", "run-assess");
						return cred_map;
					}
				}
			}
			if (parsed_options.hasOption("L")){
				cred_map.put("list-assess", "list-assess");
				return cred_map;
			}
			if (parsed_options.hasOption("A")){
				cred_map.put("assess-uuid", parsed_options.getOptionValue("A"));
				return cred_map;
			}
		}

		if (parsed_options.hasOption("R")){
			throw new CommandLineOptionException( optionMissingStr(options.getOption("P")) + "\t" +
					optionMissingStr(options.getOption("K")) + "\t" +
					optionMissingStr(options.getOption("T")));
		}else if (parsed_options.hasOption("L")){
			throw new CommandLineOptionException(optionMissingStr(options.getOption("P")));
		}else {
			throw new CommandLineOptionException(optionMissingStr(options.getOption("P")));
		}
	}

	public HashMap<String, Object> processCliArgs(String command, ArrayList<String> cli_args) throws CommandLineOptionException, ParseException{

		HashMap<String, Object> opt_map = null;

		switch (command) {
		case "login":
			opt_map = loginOptionsHandler(cli_args);
			break;
		case "package":
			opt_map = packageOptionsHandler(cli_args);
			break;
		case "project":
			opt_map = projectOptionsHandler(cli_args);
			break;
		case "tools":
			opt_map = toolsOptionsHandler(cli_args);
			break;
		case "assess":
			opt_map = assessmentOptionsHandler(cli_args);
			break;
		case "platform":
			opt_map = platformOptionsHandler(cli_args);
			break;
		case "results":
			opt_map = resultsOptionsHandler(cli_args);
			break;
		case "status":
			opt_map = statusOptionsHandler(cli_args);
			break;
		case "user":
			opt_map = userOptionsHandler(cli_args);
			break;
		case "logout":
			opt_map = logoutOptionsHandler(cli_args);
			break;
		default:
			break;
		}
		return opt_map;
	}

	public void loginHandler(HashMap<String, Object> opt_map) {
		String host_name = (String)opt_map.get("swamp-host");

		String user_uuid = api_wrapper.login((String)opt_map.get("username"), 
				(String)opt_map.get("password"),
				host_name);

		if (user_uuid != null){
			System.out.println("Login successful");
			System.out.printf("User UUID: %s", user_uuid + "\n");
			api_wrapper.saveSession();
		}
	}
	
	public void projectHandler(HashMap<String, Object> opt_map) {
		if (opt_map.containsKey("project-name")) {
			Project my_proj = api_wrapper.getProjectFromName((String)opt_map.get("project-name"));
			if (my_proj == null){
				System.out.printf("Project %s does not exist.\n", opt_map.get("project-name"));
			}else{
				System.out.printf(my_proj.getUUIDString());
			}
		}else{
			api_wrapper.printAllProjects();
		}
	}
	
	public void platformHandler(HashMap<String, Object> opt_map) {
		if (opt_map.containsKey("platform-name")) {
			System.out.printf(api_wrapper.getPlatformVersionFromName((String)opt_map.get("platform-name")).getUUIDString());
		}else {
			/*
			 api_wrapper.printAllPlatforms((String)opt_map.get("pkg-type"));
			
        	 for (SwampPlatform swamp_platform : api_wrapper.getSwampPlatformsList()){

        		System.out.println(swamp_platform);
        	}*/
			System.out.printf("%-30s %-38s\n", "Platform Name", "Platform UUID");
			for (PlatformVersion platform_version : api_wrapper.getAllPlatformVersionsList()){
				System.out.printf("%-30s %-38s\n", platform_version.getDisplayString(), platform_version.getIdentifierString());
			}
		}
	}
	
	public void toolHandler(HashMap<String, Object> opt_map) {
		if (opt_map.containsKey("tool-name")) {
			Tool my_tool = api_wrapper.getToolFromName((String)opt_map.get("tool-name"), 
					(String)opt_map.get("project-uuid"));
			if (my_tool == null){
				System.out.printf("Tool %s does not exist.\n", opt_map.get("tool-name"));
			}else{
				System.out.println(my_tool.getUUIDString());
			}
		}else{
			printAllTools((String)opt_map.get("project-uuid"));
		}
	}
	
	public void packageHandler(HashMap<String, Object> opt_map) {
		if (opt_map.containsKey("list")) {
			printAllPackages((String)opt_map.get("project-uuid"), true);
		}else if (opt_map.containsKey("pkg-types")) {
			for (String pkg_type : api_wrapper.getPackageTypesList()) {
				System.out.println(pkg_type);
			}
		}else if (opt_map.containsKey("delete")) {
			for (Object pkg_uuid : (List<String>)opt_map.get("package-uuids")) {
				api_wrapper.deletePackage((String) pkg_uuid, (String)opt_map.get("project-uuid"));
			}
		}else {
		
			String package_uuid = api_wrapper.uploadPackage((String)opt_map.get("pkg-conf"),
					(String)opt_map.get("pkg-archive"),
					(String)opt_map.get("project-uuid"),
					(String)opt_map.get("os-deps-conf"),
					opt_map.containsKey("new-pkg"));

			if (opt_map.containsKey("quiet")){
				System.out.printf(package_uuid);
			}else{
				System.out.printf("Package Version UUID: %s\n", package_uuid);
			}
		}
	}
	
	public void assessmentHandler(HashMap<String, Object> opt_map) {
		if (opt_map.containsKey("run-assess")){
			@SuppressWarnings({"unchecked"})
			List<String> assess_uuid = api_wrapper.runAssessment((String)opt_map.get("pkg-uuid"), 
					(List<String>)opt_map.get("tool-uuid"),
					(String)opt_map.get("project-uuid"), 
					(List<String>)opt_map.get("platform-uuid"));
			if (opt_map.containsKey("quiet")){
				System.out.println(assess_uuid);
			}else{
				System.out.printf("Assessment UUIDs: %s\n", assess_uuid);
			}
		}
		if (opt_map.containsKey("list-assess")){
			printAssessments((String)opt_map.get("project-uuid"), opt_map.containsKey("quiet"));
		}
		if (opt_map.containsKey("assess-uuid")){
			printAssessment((String)opt_map.get("assess-uuid"), (String)opt_map.get("project-uuid"));
		}
	}
	
	public void resultsHandler(HashMap<String, Object> opt_map) throws IOException{
		api_wrapper.getAssessmentResults((String)opt_map.get("project-uuid"), 
				(String)opt_map.get("results-uuid"),
				(String)opt_map.get("file-path"));
	}
	
	public void assessmentStatusHandler(HashMap<String, Object> opt_map) {
		if (opt_map.containsKey("assess-uuid")){
			printAssessmentStatus((String)opt_map.get("project-uuid"), (String)opt_map.get("assess-uuid"));
		}else{
			printAllAssessmentStatus((String)opt_map.get("project-uuid"));
		}
	}
	
	public void printUserInfo(HashMap<String, Object> opt_map) {
		User user = api_wrapper.getUserInfo();
		System.out.printf("%s\n", "User:\t" + user.getFirstName() + " " + user.getLastName());
		System.out.printf("%s\n", "Email:\t" + user.getEmail());
		if (user.getPhone().equals("null")){
			System.out.printf("%s\n", "Phone:\t<Not provided>");
		}else{
			System.out.printf("%s\n", "Phone:\t" + user.getPhone());
		}
		System.out.printf("%s\n", "UUID:\t" + user.getUUIDString());
	}
	
	public void logoutHandler(HashMap<String, Object> opt_map) {
		api_wrapper.logout();
	}
	
	public int executeCommands(String command, HashMap<String, Object> opt_map) throws IOException, SessionExpiredException, InvalidIdentifierException, IncompatibleAssessmentTupleException {

		if (command.equals("login")) {
			loginHandler(opt_map);
		}else {
			api_wrapper.restoreSession();
			switch (command) {
			case "project":
				projectHandler(opt_map);
				break;
			case "platform":
				platformHandler(opt_map);
				break;
			case "tools":
				toolHandler(opt_map);
				break;
			case "package":
				packageHandler(opt_map);
				break;
			case "assess":
				assessmentHandler(opt_map);
				break;
			case "results":
				resultsHandler(opt_map);
				break;
			case "status":
				assessmentStatusHandler(opt_map);
				break;
			case "user":
				printUserInfo(opt_map);
				break;
			case "logout":
				logoutHandler(opt_map);
				break;
			default:
				break;
			}
		}
		return 0;
	}

	public void printAllPackagesSummary(String project_uuid) {
		System.out.printf("\n\n%-21s %-38s %-20s %-30s\n", "Package Name", "Package UUID", "Package Versions", "Package Description");
		for(PackageThing pkg : api_wrapper.getPackagesList(project_uuid)){
			System.out.printf("%-21s %-38s %-20s %-30s\n", pkg.getName(), pkg.getIdentifierString(),
					pkg.getVersions(), pkg.getDescription());
		}
	}

	public void printAllPackagesVerbose(String project_uuid) {
		for(PackageThing pkg : api_wrapper.getAllPackages(project_uuid).values()){
			System.out.printf("\n\n%-21s %-38s %-30s\n", pkg.getName(), pkg.getIdentifierString(), pkg.getDescription());
			for(PackageVersion pkg_ver : api_wrapper.getPackageVersionsList(project_uuid)){
				if (pkg_ver.getPackageThing().getUUIDString().equals(pkg.getUUIDString())) {
					System.out.printf("\t%-13s %-38s\n", pkg_ver.getVersionString(), pkg_ver.getUUIDString());
				}
			}
		}
	}

	public void printAllPackages(String project_uuid, boolean verbose) {
		if(verbose){
			printAllPackagesVerbose(project_uuid);
		}else {
			printAllPackagesSummary(project_uuid);
		}
	}

	public void printAllTools(String project_uuid) throws InvalidIdentifierException {
		System.out.printf("\n\n%-21s %-38s %-40s %s\n", "Tool Name", "Tool UUID", 
				"Supported Package Types", "Supported Platforms");
		for(Tool tool : api_wrapper.getAllTools(project_uuid).values()){
			
			System.out.printf("%-21s %-38s %-40s %s\n", tool.getName(), 
					tool.getIdentifierString(),
					tool.getSupportedPkgTypes(), 
					api_wrapper.getSupportedPlatformVersions(tool.getIdentifierString(), project_uuid));
		}
	}

	public void printAllPlatformVersions(String pkg_type) {

		System.out.printf("\n%-40s %-38s\n", "Platform Name", "Platform UUID");
		if (pkg_type != null) {
			PlatformVersion platform_version = api_wrapper.getDefaultPlatformVersion(pkg_type);
			System.out.printf("%-40s %-38s\n", platform_version.getDisplayString(), 
					platform_version.getIdentifierString());
		}else {
			for(PlatformVersion platform_version : api_wrapper.getAllPlatformVersions().values()){
				System.out.printf("%-40s %-38s\n", platform_version.getName(), 
						platform_version.getIdentifierString());
			}
		}
	}


	public void printAssessments(String project_uuid, boolean quiet) {
		
		for (AssessmentRun arun : api_wrapper.getAllAssessments(project_uuid)){
			if (quiet){
				System.out.printf(arun.getUUIDString() + "\n");
			}else{
				System.out.printf("Assessment on " + arun.getFilename() +":\n\tUUID: " + arun.getUUIDString() + "\n");
			}
		}
	}

	public void printAssessment(String assessment_uuid, String project_uuid) {
		AssessmentRun arun = api_wrapper.getAssessment(assessment_uuid, project_uuid);
		if (arun == null){
			System.out.println("Assessment " + assessment_uuid + " not found. Please verify the UUID");
		}else{
			System.out.println("Assessment Results on " + arun.getIdentifierString());
			System.out.println("Package: \t" + (arun.getPkg() == null ? "N/A" : arun.getPkg().getName()));
			System.out.println("Project: \t" + (arun.getProject() == null ? "N/A" : arun.getProject().getFullName()));
			System.out.println("Tool:    \t" + (arun.getTool() == null ? "N/A" : arun.getTool().getName()));
			//TODO: this must be platform version
			System.out.println("Platform:\t" + (arun.getPlatform() == null ? "N/A" : arun.getPlatform().getName()));
		}
	}

	public void printAssessmentStatus(String project_uuid, String assessment_uuid) {
		AssessmentRecord assessment_record = api_wrapper.getAssessmentRecord(project_uuid, assessment_uuid);        
		System.out.printf("%s, %d", 
				AssessmentStatus.translateAssessmentStatus(assessment_record.getStatus()), 
				assessment_record.getWeaknessCount());

		if (assessment_record.getAssessmentResultUUID() == null){
			System.out.printf("\n");
		}else{
			System.out.printf(", %-38s\n", assessment_record.getAssessmentResultUUID());
		}
	}

	public void printAssessmentResultsUUID(String project_uuid, String assessment_uuid) {
		System.out.println(api_wrapper.getAssessmentRecord(project_uuid, assessment_uuid).getAssessmentResultUUID());
	}
	
	public void printAllAssessmentStatus(String project_uuid) {

		System.out.printf("\n\n%-38s %-38s %-22s %s\n", 
				"ASSESSMENT RUN UUID", "ASSESSMENT RESULT UUID", 
				"STATUS", "WEAKNESS COUNT");
		for(AssessmentRecord assessment_record : api_wrapper.getAllAssessmentRecords(project_uuid)) {
			System.out.printf("%-38s %-38s %-22s %d\n", assessment_record.getAssessmentRunUUID(),
					assessment_record.getAssessmentResultUUID(),
					assessment_record.getStatus(),
					assessment_record.getWeaknessCount());
		}
	}


	public static void main(String[] args) throws Exception {

		org.apache.log4j.BasicConfigurator.configure(new NullAppender());

		if (args.length == 0 || args[0].equals("-h") || args[0].equals("--help") || ! Cli.COMMANDS.contains(args[0]) ){
			printHelp();
			return;
		}

		Cli cli = new Cli();
		ArrayList<String> cli_args = new ArrayList<String>(Arrays.asList(args));
		String command = cli_args.remove(0);

		try {
			HashMap<String, Object> opt_map = cli.processCliArgs(command, cli_args);
			if (opt_map != null){
				cli.executeCommands(command, opt_map);
			}
		} catch(SwampApiWrapperException e){
			System.err.println("ERROR:: " + e.getMessage());
			System.exit(e.getExitCode());
		}catch(ParseException e){
			System.err.println("ERROR:: " + e.getMessage());
			System.exit(SwampApiWrapperExitCodes.CLI_PARSER_ERROR.getExitCode());
		}catch (HTTPException e) {
			System.err.println("ERROR:: " + e.getMessage());
			System.exit(SwampApiWrapperExitCodes.NormalizeHttpExitCode(((HTTPException)e).getStatusCode()));
		}catch(GeneralException e){
			e.printStackTrace();
			System.err.println("ERROR:: " + e.getMessage());
			System.exit(SwampApiWrapperExitCodes.HTTP_GENERAL_EXCEPTION.getExitCode());
		}catch(IllegalStateException e){
			System.err.println("ERROR:: " + e.getMessage());
			System.exit(SwampApiWrapperExitCodes.HTTP_GENERAL_EXCEPTION.getExitCode());
		}

		System.exit(SwampApiWrapperExitCodes.NO_ERRORS.getExitCode());
	}

}
