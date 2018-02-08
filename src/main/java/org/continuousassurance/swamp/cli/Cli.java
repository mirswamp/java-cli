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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;


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
			"login", 
			"logout", 
			"packages",
			"assessments", 
			"results",
			"runs",
			"projects",
			"tools",
			"platforms",
			"user"));

	public static void printHelp() {
		System.out.println("------------------------------------------------------------------------");
		System.out.println("Usage: <program> <command> <options>");
		System.out.println("------------------------------------------------------------------------");
		System.out.println("<command> must be one of the following:");
		for (String cmd :  COMMANDS) {
			System.out.println("\t\t" + cmd);
		}
		System.out.println("------------------------------------------------------------------------");
		System.out.println("For information on the <options> for a <command> execute:");
		System.out.println("\t<program> <command> --help or <program> <command> -H");
	}

	public static boolean isUuid(String str) {

		return (str.length() == 36 && 
				Pattern.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}", str));
	}

	protected HashMap<String, Object> getUserCredentials(String filename) {
		HashMap<String, Object> cred_map = new HashMap<String, Object>();
		Properties prop = new Properties();

		try {
			prop.load(new FileInputStream(filename));
			cred_map.put("username", prop.getProperty("username"));
			cred_map.put("password", prop.getProperty("password"));
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		return cred_map;
	}

	public static String optionMissingStr(Option option) {

		if (option.hasArg()) {

			return String.format("Missing options/arguments: [(-%s|--%s) <%s>]\n",
					option.getOpt(),
					option.getLongOpt(),
					option.getArgName());
		}else {
			return String.format("Missing options/arguments: [(-%s|--%s) ]\n",
					option.getOpt(),
					option.getLongOpt());
		}
	}

	public String getSwampDate(Date date) {
		return String.format("%d/%d/%d %d:%d", 
				date.getDate(), date.getMonth(), date.getYear(), 
				date.getHours(), date.getMinutes());
	}

	public HashMap<String, Object> loginOptionsHandler(ArrayList<String> args) throws ParseException, CommandLineOptionException, FileNotFoundException, IOException {

		Options options = new Options();
		OptionGroup opt_grp = new OptionGroup();
		opt_grp.setRequired(true);

		opt_grp.addOption(Option.builder("H").required(false).longOpt("help").desc("Shows Help").build());
		opt_grp.addOption(Option.builder("F").required(false).hasArg().longOpt("filepath").argName("CREDENTIALS_FILEPATH")
				.desc("Properties file containing username and password").build());
		opt_grp.addOption(Option.builder("C").required(false).hasArg(false).longOpt("console")
				.desc("Accepts username and password from the terminal").build());
		options.addOptionGroup(opt_grp);

		options.addOption(Option.builder("S").required(false).hasArg().longOpt("swamp-host").argName("SWAMP_HOST")
				.desc("URL for SWAMP host: default is " + SwampApiWrapper.SWAMP_HOST_NAME).build());
		options.addOption(Option.builder("Q").required(false).hasArg(false).longOpt("quiet")
				.desc("Less verbose output").build());

		String[] cmd_args = (String[]) args.toArray(new String[0]);
		CommandLine parsed_options = new DefaultParser().parse(options, cmd_args);
		if (args.size() == 0 || parsed_options.hasOption("H")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("Command Line Parameters", options);
			return null;
		}else if (parsed_options.hasOption("F") || parsed_options.hasOption("C") ) {
			HashMap<String, Object> cred_map = new HashMap<String, Object>();
			if (parsed_options.hasOption("Q")){
				cred_map.put("quiet", true);
			}else {
				cred_map.put("quiet", false);
			}
			cred_map.put("swamp-host", parsed_options.getOptionValue("S", SwampApiWrapper.SWAMP_HOST_NAME));

			if (parsed_options.hasOption("F")) {
				Properties prop = new Properties();
				//TODO: check for filepath
				//TODO: lowercase and check for properties
				prop.load(new FileInputStream(parsed_options.getOptionValue("F")));
				cred_map.put("username", prop.getProperty("username"));
				cred_map.put("password", prop.getProperty("password"));
			}else {
				System.out.print("swamp-username: ");
				String username = System.console().readLine();
				System.out.print("swamp-password: ");
				String password = new String(System.console().readPassword());

				cred_map.put("username", username);
				cred_map.put("password", password);

			}
			if ((cred_map.get("username") != null ) && (cred_map.get("password") != null)){
				return cred_map;
			}else {
				throw new CommandLineOptionException(String.format("No username or password in the file: %s\n",
						parsed_options.getOptionValue("F")));
			}
		}
		else {
			throw new CommandLineOptionException("Unknown / Incompatible options");
		}

	}

	public HashMap<String, Object> projectOptionsHandler(ArrayList<String> args) throws ParseException {

		Options options = new Options();
		OptionGroup opt_grp = new OptionGroup();
		opt_grp.setRequired(true);

		opt_grp.addOption(Option.builder("H").required(false).longOpt("help").desc("Shows Help").build());
		opt_grp.addOption(Option.builder("L").required(false).hasArg(false).longOpt("list")
				.desc("List projects").build());
		opt_grp.addOption(Option.builder("N").required(false).hasArg(true).longOpt("name").argName("PROJECT_NAME")
				.desc("Specify a project name and get the uuid from it").build());
		options.addOptionGroup(opt_grp);

		options.addOption(Option.builder("U").required(false).hasArg(false).longOpt("uuid")
				.desc("Get project UUID").build());
		options.addOption(Option.builder("Q").required(false).hasArg(false).longOpt("quiet")
				.desc("Less verbose output").build());

		String[] cmd_args = (String[]) args.toArray(new String[0]);
		CommandLine parsed_options = new DefaultParser().parse(options, cmd_args);
		if (args.size() == 0 || parsed_options.hasOption("H")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("Command Line Parameters", options);
			return null;
		}else {
			HashMap<String, Object> cred_map = new HashMap<String, Object>();
			if (parsed_options.hasOption("Q")){
				cred_map.put("quiet", true);
			}else {
				cred_map.put("quiet", false);
			}

			/* sanity check, -U bogus, optional extra only with -N */
			if (parsed_options.hasOption("U")) {
				if (!parsed_options.hasOption("N")) {
					throw new CommandLineOptionException(optionMissingStr(options.getOption("N")));
				}
			}

			if (parsed_options.hasOption("N")) {
				cred_map.put("project-name", parsed_options.getOptionValue("N"));
			}
			return cred_map;
		}
	}

	public HashMap<String, Object> resultsOptionsHandler(ArrayList<String> args) throws ParseException, CommandLineOptionException {

		Options options = new Options();
		options.addOption(Option.builder("H").required(false).longOpt("help").desc("Shows Help").build());
		options.addOption(Option.builder("R").required(false).hasArg(true).longOpt("results-uuid").argName("RESULTS_UUID")
				.desc("Assessment Results UUID of a project").build());
		options.addOption(Option.builder("P").required(false).hasArg(true).longOpt("project-uuid").argName("PROJECT_UUID")
				.desc("Project UUID of a project").build());
		options.addOption(Option.builder("F").required(false).hasArg(true).longOpt("file-path").argName("SCARF_FILEPATH")
				.desc("Filepath to write SCARF Results into").build());

		String[] cmd_args = (String[]) args.toArray(new String[0]);
		CommandLine parsed_options = new DefaultParser().parse(options, cmd_args);
		if (args.size() == 0 || parsed_options.hasOption("H")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("Command Line Parameters", options);
			return null;
		}else {

			if(!parsed_options.hasOption("results-uuid")){
				throw new CommandLineOptionException(optionMissingStr(options.getOption("R")));
			}
			if(!parsed_options.hasOption("project-uuid")){
				throw new CommandLineOptionException(optionMissingStr(options.getOption("P")));
			}
			if(!parsed_options.hasOption("file-path")){
				throw new CommandLineOptionException(optionMissingStr(options.getOption("F")));
			}

			HashMap<String, Object> cred_map = new HashMap<String, Object>();
			cred_map.put("project-uuid", parsed_options.getOptionValue("project-uuid"));
			cred_map.put("results-uuid", parsed_options.getOptionValue("results-uuid"));
			cred_map.put("file-path", parsed_options.getOptionValue("file-path"));
			return cred_map;
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
		}else {
			if(!parsed_options.hasOption("project-uuid")){
				throw new CommandLineOptionException(optionMissingStr(options.getOption("P")));
			}
			if(!parsed_options.hasOption("assess-uuid")){
				throw new CommandLineOptionException(optionMissingStr(options.getOption("A")));
			}
			HashMap<String, Object> cred_map = new HashMap<String, Object>();
			cred_map.put("project-uuid", parsed_options.getOptionValue("project-uuid"));
			cred_map.put("assess-uuid", parsed_options.getOptionValue("assess-uuid"));
			return cred_map;
		}
	}

	public HashMap<String, Object> packageOptionsHandler(String cmd_name, ArrayList<String> args) throws ParseException, CommandLineOptionException {

		Options options = new Options();
		OptionGroup opt_grp = new OptionGroup();
		opt_grp.setRequired(true);

		opt_grp.addOption(Option.builder("H").required(false).longOpt("help").desc("Shows Help").build());
		opt_grp.addOption(Option.builder("L").required(false).hasArg(false).longOpt("list")
				.desc("list all packages").build());
		opt_grp.addOption(Option.builder("T").required(false).hasArg(false).longOpt("types")
				.desc("list all supported package types").build());
		opt_grp.addOption(Option.builder("D").required(false).hasArg(false).longOpt("delete")
				.desc("Delete packages").build());
		opt_grp.addOption(Option.builder("U").required(false).hasArg(false).longOpt("upload")
				.desc("Upload a new package/package-version").build());
		options.addOptionGroup(opt_grp);

		Options upload_options = new Options();
		{

			upload_options.addOption(Option.builder("Q").required(false).hasArg(false).longOpt("quiet")
					.desc("Print only the Package UUID with no formatting").build());
			upload_options.addOption(Option.builder("A").required().hasArg().argName("PACKAGE_ARCHIVE_FILEPATH").longOpt("pkg-archive")
					.desc("File path to the package archive file").build());
			upload_options.addOption(Option.builder("C").required().hasArg().argName("PACKAGE_CONF_FILEPATH").longOpt("pkg-conf")
					.desc("File path to the package.conf file").build());
			upload_options.addOption(Option.builder("P").required(false).hasArg().argName("PROJECT").longOpt("project")
					.desc("Name or UUID of the project that this package must be added to.  Default: MyProject").build());
			upload_options.addOption(Option.builder("N").required(false).hasArg(false).longOpt("new")
					.desc("Flag if this package must be added as a new package, and not as a new version of an existing package").build());
			upload_options.addOption(Option.builder("O").argName("property=value").numberOfArgs(2).valueSeparator('=').longOpt("os-deps")
					.desc("use value for given property" ).build());
		}

		Options delete_options = new Options();
		{
			delete_options.addOption(Option.builder("Q").required(false).hasArg(false).longOpt("quiet")
					.desc("Do not print anything").build());
			delete_options.addOption(Option.builder("P").required(false).hasArg().argName("PROJECT").longOpt("project")
					.desc("Delete packages in this project. if --packages option is not specified, delete all").build());
			delete_options.addOption(Option.builder("K").required(false).hasArgs().argName("PACKAGES").longOpt("packages")
					.desc("Delete packages with these names or UUIDs. Accepts multiple names or UUIDs").build());
		}

		Options list_options = new Options();
		{
			OptionGroup list_opt_grps = new OptionGroup();
			list_opt_grps.addOption(Option.builder("Q").required(false).hasArg(false).longOpt("quiet")
					.desc("Do not print Headers, Description, Type ").build());
			list_opt_grps.addOption(Option.builder("V").required(false).hasArg(false).longOpt("verbose")
					.desc("Print UUIDs also").build());
			list_options.addOption(Option.builder("P").required(false).hasArg().argName("PROJECT").longOpt("project")
					.desc("Only show packages in this Project (Name or UUID)").build());
			list_options.addOption(Option.builder("KT").required(false).hasArgs().argName("PACKAGE_TYPE").longOpt("pkg-type")
					.desc("Only show packages of this Type").build());
			list_options.addOptionGroup(list_opt_grps);
		}

		if (args.size() == 0 ) {
			args.add("-H");
		}

		CommandLine main_options = new DefaultParser().parse(options, args.toArray(new String[0]), true);
		if (main_options.hasOption("help") || args.contains("-H") || args.contains("--help")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("sub-commands", options, true);
			formatter.printHelp(new PrintWriter(System.out, true), 120, 
					cmd_name + " --" + options.getOption("-D").getLongOpt(), 
					"", delete_options, 4, 4, "", true);
			formatter.printHelp(new PrintWriter(System.out, true), 120, 
					cmd_name + " --" + options.getOption("-L").getLongOpt(), 
					"", list_options, 4, 4, "", true);
			formatter.printHelp(new PrintWriter(System.out, true), 120, 
					cmd_name + " --" + options.getOption("-T").getLongOpt(), 
					"", new Options(), 4, 4, "", true);
			formatter.printHelp(new PrintWriter(System.out, true), 120,
					cmd_name + " --" + options.getOption("-U").getLongOpt(), 
					"", upload_options, 4, 4, "", true);
			return null;
		}

		args.remove(0);

		HashMap<String, Object> cred_map = new HashMap<String, Object>();

		if (main_options.hasOption("T")){
			cred_map.put("sub-command", "types");
		}else if (main_options.hasOption("L")){
			CommandLine parsed_options = new DefaultParser().parse(list_options, args.toArray(new String[0]), true);
			cred_map.put("sub-command", "list");
			cred_map.put("quiet", parsed_options.hasOption("Q"));
			cred_map.put("verbose", parsed_options.hasOption("V"));
			cred_map.put("project", parsed_options.getOptionValue("P", null));
			cred_map.put("pkg-type", parsed_options.getOptionValue("KT", null));
		}else if (main_options.hasOption("U")) {
			CommandLine parsed_options = new DefaultParser().parse(upload_options, args.toArray(new String[0]), true);
			cred_map.put("sub-command", "upload");
			cred_map.put("quiet", parsed_options.hasOption("Q"));
			cred_map.put("pkg-archive", parsed_options.getOptionValue("A"));
			cred_map.put("pkg-conf", parsed_options.getOptionValue("C"));
			cred_map.put("project", parsed_options.getOptionValue("P", null));
			cred_map.put("new-pkg", parsed_options.hasOption("N"));	

			if(parsed_options.hasOption("O")){
				Properties prop = parsed_options.getOptionProperties("O");
				cred_map.put("os-deps-map", prop);
			}
		}else if (main_options.hasOption("D")) {
			CommandLine parsed_options = new DefaultParser().parse(delete_options, args.toArray(new String[0]), true);			
			cred_map.put("sub-command", "delete");
			cred_map.put("quiet", parsed_options.hasOption("Q"));
			cred_map.put("project", parsed_options.getOptionValue("P", null));

			if (parsed_options.getOptionValues("K") != null) {
				cred_map.put("packages", Arrays.asList(parsed_options.getOptionValues("K")));
			}else {
				cred_map.put("packages", null);
			}

			if (cred_map.get("project") == null && cred_map.get("packages") == null) {
				throw new CommandLineOptionException(optionMissingStr(delete_options.getOption("P")) + 
						" or|and " + optionMissingStr(delete_options.getOption("K")));
			}
		}
		return cred_map;
	}

	public HashMap<String, Object> userOptionsHandler(ArrayList<String> args) throws ParseException{

		Options options = new Options();
		OptionGroup opt_grp = new OptionGroup();
		opt_grp.setRequired(true);

		opt_grp.addOption(Option.builder("H").required(false).longOpt("help").desc("Shows Help").build());
		opt_grp.addOption(Option.builder("I").required(false).hasArg(false).longOpt("info")
				.desc("Displays info about the currently logged in user").build());
		options.addOptionGroup(opt_grp);

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
		opt_grp.addOption(Option.builder("N").required(false).hasArg(true).argName("TOOL_NAME").longOpt("name")
				.desc("Specify the tool name and get the uuid from it").build());
		options.addOptionGroup(opt_grp);

		options.addOption(Option.builder("U").required(false).hasArg(false).longOpt("uuid")
				.desc("Get UUID from tool name").build());
		options.addOption(Option.builder("P").required(false).hasArg(true).argName("PROJECT_UUID").longOpt("project-uuid")
				.desc("Project UUID for extra project specific tools").build());
		options.addOption(Option.builder("Q").required(false).hasArg(false).longOpt("quiet")
				.desc("Less verbose output").build());

		String[] cmd_args = (String[]) args.toArray(new String[0]);
		CommandLine parsed_options = new DefaultParser().parse(options, cmd_args);
		if (args.size() == 0 || parsed_options.hasOption("help")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("Command Line Parameters", options);
			return null;
		}else {
			HashMap<String, Object> cred_map = new HashMap<String, Object>();
			if (parsed_options.hasOption("Q")){
				cred_map.put("quiet", true);
			}else {
				cred_map.put("quiet", false);
			}

			/* sanity check, -U bogus, optional extra only with -N */
			if (parsed_options.hasOption("U")) {
				if (!parsed_options.hasOption("N")) {
					throw new CommandLineOptionException(optionMissingStr(options.getOption("N")));
				}
			}

			if (parsed_options.hasOption("L")){
				cred_map.put("list", "list");
				cred_map.put("project-uuid", parsed_options.getOptionValue("P", null));
			}
			else if (parsed_options.hasOption("N")) {
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
				.desc("Show all platform").build());
		opt_grp.addOption(Option.builder("N").required(false).hasArg(true).argName("PLATFORM_NAME").longOpt("name")
				.desc("Specify the platform name and get the uuid from it").build());
		options.addOptionGroup(opt_grp);

		options.addOption(Option.builder("U").required(false).hasArg(false).longOpt("uuid")
				.desc("Get UUID from platform name").build());

		options.addOption(Option.builder("Q").required(false).hasArg(false).longOpt("quiet")
				.desc("Less verbose output").build());

		String[] cmd_args = (String[]) args.toArray(new String[0]);
		CommandLine parsed_options = new DefaultParser().parse(options, cmd_args);
		if (args.size() == 0 || parsed_options.hasOption("help")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("Command Line Parameters", options);
			return null;
		}else {
			HashMap<String, Object> cred_map = new HashMap<String, Object>();
			if (parsed_options.hasOption("Q")){
				cred_map.put("quiet", true);
			}else {
				cred_map.put("quiet", false);
			}

			/* sanity check, -U bogus, optional extra only with -N */
			if (parsed_options.hasOption("U")) {
				if (!parsed_options.hasOption("N")) {
					throw new CommandLineOptionException(optionMissingStr(options.getOption("N")));
				}
			}

			if (parsed_options.hasOption("L")){
				cred_map.put("list", "list");
			}
			else if (parsed_options.hasOption("N")){
				cred_map.put("platform-name", parsed_options.getOptionValue("N", null));
			}
			return cred_map;
		}
	}

	public HashMap<String, Object> logoutOptionsHandler(ArrayList<String> args) throws ParseException{

		Options options = new Options();
		options.addOption(Option.builder("H").required(false).longOpt("help").desc("Shows Help").build());
		options.addOption(Option.builder("Q").required(false).hasArg(false).longOpt("quiet")
				.desc("Less verbose output").build());

		String[] cmd_args = (String[]) args.toArray(new String[0]);
		CommandLine parsed_options = new DefaultParser().parse(options, cmd_args);
		if (parsed_options.hasOption("help")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("Command Line Parameters", options);
			return null;
		}else {
			HashMap<String, Object> cred_map = new HashMap<String, Object>();
			cred_map.put("logout", "logout");
			if (parsed_options.hasOption("Q")){
				cred_map.put("quiet", true);
			}else {
				cred_map.put("quiet", false);
			}
			return cred_map;
		}

	}

	public HashMap<String, Object> assessmentOptionsHandler(String cmd_name, ArrayList<String> args) throws ParseException, CommandLineOptionException{

		Options options = new Options();
		OptionGroup opt_grp = new OptionGroup();
		opt_grp.setRequired(true);

		opt_grp.addOption(Option.builder("H").required(false).longOpt("help").desc("Shows Help").build());
		opt_grp.addOption(Option.builder("R").required(false).longOpt("run").hasArg(false).desc("Run new assessment").build());
		opt_grp.addOption(Option.builder("L").required(false).longOpt("list").hasArg(false).desc("List assessments").build());
		options.addOptionGroup(opt_grp);

		Options run_options = new Options();
		{

			run_options.addOption(Option.builder("Q").required(false).hasArg(false).longOpt("quiet")
					.desc("Print only the Package UUID with no formatting").build());
			run_options.addOption(Option.builder("K").required(true).hasArg().argName("PACKAGE").longOpt("package")
					.desc("Package name or UUID").build());
			run_options.addOption(Option.builder("KV").required().hasArg().argName("PACKAGE_VERSION").longOpt("pkg-version")
					.desc("Package version").build());
			run_options.addOption(Option.builder("T").required(true).hasArg().argName("TOOL").longOpt("tool")
					.desc("Package version").build());
			run_options.addOption(Option.builder("TV").required().hasArg().argName("TOOL_VERSION").longOpt("tool-version")
					.desc("Package version").build());
			run_options.addOption(Option.builder("F").required().hasArg().argName("PLATFORM").longOpt("platform")
					.desc("Platform name").build());
		}

		Options list_options = new Options();
		{
			OptionGroup list_opt_grps = new OptionGroup();
			list_opt_grps.addOption(Option.builder("Q").required(false).hasArg(false).longOpt("quiet")
					.desc("Do not print Headers, Description, Type ").build());
			list_opt_grps.addOption(Option.builder("V").required(false).hasArg(false).longOpt("verbose")
					.desc("Print UUIDs also").build());
			list_options.addOption(Option.builder("K").required().hasArg().argName("PACKAGE").longOpt("package")
					.desc("Package name or UUID").build());
			list_options.addOption(Option.builder("T").required().hasArg().argName("TOOL").longOpt("tool")
					.desc("Package version").build());
			list_options.addOption(Option.builder("P").required(false).hasArg().argName("PROJECT").longOpt("project")
					.desc("Only show packages in this Project (Name or UUID)").build());
			list_options.addOption(Option.builder("F").required().hasArg().argName("PLATFORM").longOpt("platform")
					.desc("Platform name").build());
			list_options.addOptionGroup(list_opt_grps);
		}

		if (args.size() == 0 ) {
			args.add("-H");
		}

		CommandLine main_options = new DefaultParser().parse(options, args.toArray(new String[0]), true);
		if (main_options.hasOption("help") || args.contains("-H") || args.contains("--help")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("sub-commands", options, true);
			formatter.printHelp(new PrintWriter(System.out, true), 120, 
					cmd_name + " --" + options.getOption("-L").getLongOpt(), 
					"", list_options, 4, 4, "", true);
			formatter.printHelp(new PrintWriter(System.out, true), 120, 
					cmd_name + " --" + options.getOption("-R").getLongOpt(), 
					"", new Options(), 4, 4, "", true);
			return null;
		}

		args.remove(0);

		HashMap<String, Object> cred_map = new HashMap<String, Object>();

		
		String[] cmd_args = (String[]) args.toArray(new String[0]);
		CommandLine parsed_options = new DefaultParser().parse(options, cmd_args);
		HelpFormatter formatter = new HelpFormatter();
		if (args.size() == 0 || parsed_options.hasOption('H')) {
			formatter.printHelp("Command Line Parameters", options);
			return null;
		}


		if (parsed_options.hasOption("R")) {

			cred_map.put("project-uuid", parsed_options.getOptionValue("K"));
			cred_map.put("project-uuid", parsed_options.getOptionValue("K"));
			
			cred_map.put("pkg-uuid", parsed_options.getOptionValue("K"));
			cred_map.put("tool-uuid", Arrays.asList(parsed_options.getOptionValues('T')));
			if (parsed_options.hasOption("F")){
				cred_map.put("platform-uuid", Arrays.asList(parsed_options.getOptionValues('F')));
			}
			cred_map.put("sub-command", "run");
			return cred_map;
		}else if (parsed_options.hasOption("L")){
			if (!parsed_options.hasOption("P")){
				throw new CommandLineOptionException(optionMissingStr(options.getOption("P")));
			}
			cred_map.put("project-uuid", parsed_options.getOptionValue("P"));
			cred_map.put("list-assess", "list-assess");
			return cred_map;
		}else {
			if (!parsed_options.hasOption("P")){
				throw new CommandLineOptionException(optionMissingStr(options.getOption("P")));
			}
			if (!parsed_options.hasOption("A")){
				throw new CommandLineOptionException(optionMissingStr(options.getOption("A")));
			}
			cred_map.put("project-uuid", parsed_options.getOptionValue("P"));
			cred_map.put("assess-uuid", parsed_options.getOptionValue("A"));
			return cred_map;
		}

	}

	public HashMap<String, Object> processCliArgs(String command, ArrayList<String> cli_args) throws CommandLineOptionException, ParseException, FileNotFoundException, IOException{

		HashMap<String, Object> opt_map = null;

		switch (command) {
		case "login":
			opt_map = loginOptionsHandler(cli_args);
			break;
		case "packages":
			opt_map = packageOptionsHandler(command, cli_args);
			break;
		case "projects":
			opt_map = projectOptionsHandler(cli_args);
			break;	
		case "tools":
			opt_map = toolsOptionsHandler(cli_args);
			break;
		case "assessments":
			opt_map = assessmentOptionsHandler(command, cli_args);
			break;
		case "platforms":
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

			if ((boolean)opt_map.get("quiet") == false) {
				System.out.println("Login successful");
			}
			//System.out.printf("User UUID: %s", user_uuid + "\n");
			api_wrapper.saveSession();
		}
	}

	public void printAllProjects(boolean quiet) {
		if (!quiet) {
			System.out.printf("%-37s %-25s %-40s %-15s\n", 
					"UUID",
					"Project",
					"Description",
					"Date Added");

			for(Project proj : api_wrapper.getProjectsList()) {
				System.out.printf("%-37s %-25s %-40s %-15s\n", 
						proj.getUUIDString(), 
						proj.getFullName(),
						proj.getDescription(),
						getSwampDate(proj.getCreateDate()));
			}
		}else {
			for(Project proj : api_wrapper.getProjectsList()) {
				System.out.printf("%-37s %-25s\n", 
						proj.getUUIDString(),  
						proj.getFullName());
			}
		}
	}

	public void projectHandler(HashMap<String, Object> opt_map) {
		if (opt_map.containsKey("project-name")) {
			Project my_proj = api_wrapper.getProjectFromName((String)opt_map.get("project-name"));
			if (my_proj == null){
				System.out.printf("Project %s does not exist.\n", opt_map.get("project-name"));
			}else{
				System.out.println(my_proj.getUUIDString());
			}
		}else{
			printAllProjects((boolean)opt_map.get("quiet"));
		}
	}

	public void platformHandler(HashMap<String, Object> opt_map) {
		if (opt_map.containsKey("platform-name")) {
			System.out.println(api_wrapper.getPlatformVersionFromName((String)opt_map.get("platform-name")).getUUIDString());
		}else {
			if ((boolean)opt_map.get("quiet") == false) {
				System.out.printf("%-37s %-30s\n", "UUID", "Platform");

				for (PlatformVersion platform_version : api_wrapper.getAllPlatformVersionsList()){
					System.out.printf("%-37s %-30s\n",
							platform_version.getIdentifierString(),
							platform_version.getDisplayString());
				}
			}else {
				for (PlatformVersion platform_version : api_wrapper.getAllPlatformVersionsList()){
					System.out.printf("%-30s\n", platform_version.getDisplayString());
				}
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
			printAllTools((String)opt_map.get("project-uuid"), (boolean)opt_map.get("quiet"));
		}
	}

	protected void deletePackages(String project, List<String> packages, boolean quiet) {

		if (project != null && !Cli.isUuid(project)) {
			project = api_wrapper.getProjectFromName(project).getUUIDString();
		}

		if (packages == null) {
			//delete all packages
			for (PackageVersion pkg_ver : api_wrapper.getPackageVersionsList(project)) {
				if (api_wrapper.deletePackageVersion(pkg_ver) && !quiet) {
					System.out.println(String.format("Deleted 'Name: %s, Version: %s'", 
							pkg_ver.getPackageThing().getName(), 
							pkg_ver.getVersionString()));
				}
			}
		}else {
			List<PackageVersion>all_packages = api_wrapper.getPackageVersionsList(project);

			Set<String> pkg_uuids = new HashSet<String>();

			for (String pkg : packages) {

				if (Cli.isUuid(pkg)) {
					for (PackageVersion pkg_ver : all_packages) {
						if (pkg_ver.getPackageThing().getIdentifierString().equals(pkg)) {
							pkg_uuids.add(pkg_ver.getIdentifierString());
						}
					}
					pkg_uuids.add(pkg);
				}else {

					for (PackageVersion pkg_ver : all_packages) {
						if (pkg_ver.getPackageThing().getName().equals(pkg)) {
							pkg_uuids.add(pkg_ver.getIdentifierString());
						}
					}
				}			
			}

			for (PackageVersion pkg_ver : all_packages) {
				if (pkg_uuids.contains(pkg_ver.getIdentifierString())) {
					if (api_wrapper.deletePackageVersion(pkg_ver) && !quiet) {
						System.out.println(String.format("Deleted 'Name: %s, Version: %s'", 
								pkg_ver.getPackageThing().getName(), 
								pkg_ver.getVersionString()));
					}
				}
			}
		}
	}

	protected void uploadPackage(String pkg_conf, String pkg_archive,
			String project, Map<String, String> os_deps, boolean new_pkg, boolean quiet) {
		String package_uuid = null;

		if (os_deps != null) {
			for (Object plat: os_deps.keySet()) {
				boolean plat_found = false;
				for (PlatformVersion platform_version : api_wrapper.getAllPlatformVersionsList()) {
					if (platform_version.getDisplayString().equalsIgnoreCase((String)plat)) {
						plat_found = true;
						break;
					}
				}

				if (!plat_found) {
					throw new CommandLineOptionException("Platform " + (String)plat + " does not exist");
				}
			}
		}

		String project_uuid = null;
		if (project != null) {
			if (!Cli.isUuid(project)) {
				project = api_wrapper.getProjectFromName(project).getUUIDString();
			}
		}else {
			project = (String)api_wrapper.getProjectFromName("MyProject").getUUIDString();
		}

		package_uuid = api_wrapper.uploadPackage(pkg_conf, pkg_archive, project, os_deps, new_pkg);

		if (!quiet){
			System.out.println("Package Version UUID");
		}

		System.out.println(package_uuid);
	}
	
	public void packageHandler(HashMap<String, Object> opt_map) {

		String sub_command = (String)opt_map.get("sub-command");

		if (sub_command.equalsIgnoreCase("list")) {

			printAllPackages((String)opt_map.get("project"),
					(String)opt_map.get("pkg-type"),
					(boolean)opt_map.get("quiet"),
					(boolean)opt_map.get("verbose"));

		}else if (sub_command.equalsIgnoreCase("types")) {
			for (String pkg_type : getPackageTypes()) {
				System.out.println(pkg_type);
			}
		}else if (sub_command.equalsIgnoreCase("delete")) {
			deletePackages((String)opt_map.get("project"),
					(List<String>)opt_map.get("packages"),
					(boolean)opt_map.get("quiet"));
		}else {
			uploadPackage((String)opt_map.get("pkg-conf"),
					(String)opt_map.get("pkg-archive"),
					(String)opt_map.get("project"),
					(Map<String, String>)opt_map.get("os-deps-map"),
					(boolean)opt_map.get("new-pkg"),
					(boolean)opt_map.get("quiet"));
		}
	}

	public List<String> getPackageTypes() {
		List<String> pkg_types = api_wrapper.getPackageTypesList();

		Collections.sort(pkg_types, new Comparator<String>() {
			public int compare(String i1, String i2) {
				return (i1.compareTo(i2));
			}
		});

		return pkg_types;
	}

	public void printAllPackages(String project, String pkg_type, boolean quiet, boolean verbose) {

		if (pkg_type != null) {
			if (!getPackageTypes().contains(pkg_type)) {
				throw new InvalidNameException("Package type '" + pkg_type + "' not valid");
			}
		}

		if (project != null) {
			if (!Cli.isUuid(project)) {
				project = api_wrapper.getProjectFromName(project).getUUIDString();	
			}
		}

		if(quiet){
			for(PackageThing pkg : api_wrapper.getPackagesList(project)) {
				if (pkg_type == null ||
						pkg.getType().equalsIgnoreCase(pkg_type)) {

					for(PackageVersion pkg_ver : api_wrapper.getPackageVersions(pkg)) {
						System.out.printf("%-25s %-25s\n",
								pkg_ver.getPackageThing().getName(),
								pkg_ver.getVersionString());
					}
				}
			}
		}else if(verbose) {
			System.out.printf("%-37s %-25s %-40s %-25s %-25s\n",
					"UUID", "Package", "Description","Type", "Version");

			for(PackageThing pkg : api_wrapper.getPackagesList(project)) {
				if (pkg_type == null ||
						pkg.getType().equalsIgnoreCase(pkg_type)) {

					for(PackageVersion pkg_ver : api_wrapper.getPackageVersions(pkg)) {
						System.out.printf("%-37s %-25s %-40s %-25s %-25s\n", 
								pkg_ver.getUUIDString(),
								pkg_ver.getPackageThing().getName(),
								pkg_ver.getPackageThing().getDescription(),
								pkg_ver.getPackageThing().getType(),
								pkg_ver.getVersionString());
					}
				}
			}
		}else {
			System.out.printf("%-25s %-40s %-25s %-25s\n",
					"Package", "Description","Type", "Version");

			for(PackageThing pkg : api_wrapper.getPackagesList(project)) {
				if (pkg_type == null ||
						pkg.getType().equalsIgnoreCase(pkg_type)) {

					for(PackageVersion pkg_ver : api_wrapper.getPackageVersions(pkg)) {
						System.out.printf("%-25s %-40s %-25s %-25s\n",
								pkg_ver.getPackageThing().getName(),
								pkg_ver.getPackageThing().getDescription(),
								pkg_ver.getPackageThing().getType(),
								pkg_ver.getVersionString());
					}
				}
			}
		}
	}


	protected List<String> removeDuplicates(List<String> list) {
		if (list != null) {
			return new ArrayList<String>(new HashSet<String>(list));
		}else {
			return null;
		}
	}

	public void assessmentHandler(HashMap<String, Object> opt_map) {
		if (opt_map.containsKey("run-assess")){

			//For all tools
			for (String tool_uuid: (List<String>)opt_map.get("tool-uuid")) {
				if (tool_uuid.equalsIgnoreCase("all")) {
					PackageVersion pkg_ver= api_wrapper.getPackageVersion((String)opt_map.get("pkg-uuid"), 
							(String)opt_map.get("project-uuid"));

					List<String> all_tools = new ArrayList<String>();
					for(Tool tool : api_wrapper.getTools(pkg_ver.getPackageThing().getType(), 
							(String)opt_map.get("project-uuid"))){	
						all_tools.add(tool.getIdentifierString());
					}
					opt_map.put("tool-uuid", all_tools);
				}
			}

			//For all platforms
			if(opt_map.containsKey("platform-uuid")) { 
				for (String platform: (List<String>)opt_map.get("platform-uuid")) {
					if (platform.equalsIgnoreCase("all")) {

						Set<PlatformVersion> plat_set = new HashSet<PlatformVersion>();

						for (String tool_uuid: (List<String>)opt_map.get("tool-uuid")) {
							plat_set.addAll(api_wrapper.getSupportedPlatformVersions(tool_uuid, 
									(String)opt_map.get("project-uuid")));
						}

						//plat uuids
						List<String> all_plats = new ArrayList<String>();
						for (PlatformVersion platform_version : plat_set) {
							all_plats.add(platform_version.getIdentifierString());
						}
						opt_map.put("platform-uuid", all_plats);
					}
				}
			}

			@SuppressWarnings({"unchecked"})
			List<String> assess_uuids = api_wrapper.runAssessment((String)opt_map.get("pkg-uuid"), 
					removeDuplicates((List<String>)opt_map.get("tool-uuid")),
					(String)opt_map.get("project-uuid"), 
					removeDuplicates((List<String>)opt_map.get("platform-uuid")));

			if ((boolean)opt_map.get("quiet") == false) {
				System.out.println("Assessment UUIDs");
			}
			for (String uuid: assess_uuids) {
				System.out.println(uuid);
			}
		}
		if (opt_map.containsKey("list-assess")){
			printAssessments((String)opt_map.get("project-uuid"), (boolean)opt_map.get("quiet"));
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
		/*if (user.getPhone().equals("null")){
			System.out.printf("%s\n", "Phone:\t<Not provided>");
		}else{
			System.out.printf("%s\n", "Phone:\t" + user.getPhone());
		}*/
		System.out.printf("%s\n", "UUID:\t" + user.getUUIDString());
	}

	public void logoutHandler(HashMap<String, Object> opt_map) {
		api_wrapper.logout();
		if ((boolean)opt_map.get("quiet") == false) {
			System.out.println("Logout successful");
		}
	}

	public int executeCommands(String command, HashMap<String, Object> opt_map) throws IOException, SessionExpiredException, InvalidIdentifierException, IncompatibleAssessmentTupleException {

		if (command.equals("login")) {
			loginHandler(opt_map);
		}else {
			api_wrapper.restoreSession();
			switch (command) {
			case "projects":
				projectHandler(opt_map);
				break;
			case "platforms":
				platformHandler(opt_map);
				break;
			case "tools":
				toolHandler(opt_map);
				break;
			case "packages":
				packageHandler(opt_map);
				break;
			case "assessments":
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


	public void printAllTools(String project_uuid, boolean quiet) throws InvalidIdentifierException {
		if(!quiet){
			System.out.printf("%-37s %-21s %-40s %s\n",
					"UUID",
					"Tool",
					"Supported Package Types", 
					"Supported Platforms");

			for(Tool tool : api_wrapper.getAllTools(project_uuid).values()){			
				System.out.printf("%-37s %-21s %-40s %s\n", 
						tool.getIdentifierString(),
						tool.getName(),
						tool.getSupportedPkgTypes(), 
						api_wrapper.getSupportedPlatformVersions(tool.getIdentifierString(), project_uuid));
			}
		}else {
			for(Tool tool : api_wrapper.getAllTools(project_uuid).values()){			
				System.out.printf("%-37s %-21s\n", 
						tool.getIdentifierString(),
						tool.getName());
			}
		}
	}

	public void printAssessments(String project_uuid, boolean quiet) {

		if (!quiet){
			System.out.printf("%-37s %-15s %-15s %-15s %-15s %-15s %-15s\n", 
					"UUID",
					"Package Name", "Package Version",
					"Tool Name", "Tool Version",
					"Platform Name", "Platform Version");
			for (AssessmentRun arun : api_wrapper.getAllAssessments(project_uuid)){

				System.out.printf("%-37s %-15s %-15s %-15s %-15s %-15s %-15s\n", 
						arun.getUUIDString(), 
						arun.getPackageName(), 
						arun.getPackageVersion(),
						arun.getToolName(),
						arun.getToolVersion(),
						arun.getPlatformName(),
						arun.getPlatformVersion());
			}
		}else{
			for (AssessmentRun arun : api_wrapper.getAllAssessments(project_uuid)){
				System.out.println(arun.getUUIDString());
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
			System.out.printf(", %-37s\n", assessment_record.getAssessmentResultUUID());
		}
	}

	public void printAssessmentResultsUUID(String project_uuid, String assessment_uuid) {
		System.out.println(api_wrapper.getAssessmentRecord(project_uuid, assessment_uuid).getAssessmentResultUUID());
	}

	public void printAllAssessmentStatus(String project_uuid) {

		System.out.printf("\n\n%-37s %-37s %-22s %s\n", 
				"ASSESSMENT RUN UUID", "ASSESSMENT RESULT UUID", 
				"STATUS", "WEAKNESS COUNT");
		for(AssessmentRecord assessment_record : api_wrapper.getAllAssessmentRecords(project_uuid)) {
			System.out.printf("%-37s %-37s %-22s %d\n", assessment_record.getAssessmentRunUUID(),
					assessment_record.getAssessmentResultUUID(),
					assessment_record.getStatus(),
					assessment_record.getWeaknessCount());
		}
	}


	public static void main(String[] args) throws Exception {

		org.apache.log4j.BasicConfigurator.configure(new NullAppender());

		if (args.length == 0 || args[0].equals("-H") || args[0].equals("-h") ||
				args[0].equals("--help") || ! Cli.COMMANDS.contains(args[0]) ){
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
