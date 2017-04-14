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
import org.continuousassurance.swamp.api.Project;
import org.continuousassurance.swamp.api.Tool;
import org.continuousassurance.swamp.session.HTTPException;
import org.apache.commons.cli.*;
import org.apache.log4j.varia.NullAppender;
import org.continuousassurance.swamp.cli.exceptions.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;

public class Cli {

    SwampApiWrapper api_wrapper;

    public Cli() throws Exception {
        api_wrapper = new SwampApiWrapper();
    }

    public Cli(String host_name) throws Exception {
        api_wrapper = new SwampApiWrapper(host_name);
    }

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

    protected HashMap<String, String> getUserCredentials(String filename) {
        HashMap<String, String> cred_map = new HashMap<String, String>();
        Properties prop = new Properties();

        try {
            prop.load(new FileInputStream(filename));
            cred_map.put("username",prop.getProperty("USERNAME"));
            cred_map.put("password", prop.getProperty("PASSWORD"));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return cred_map;
    }

    public HashMap<String, String> loginOptionsHandler(ArrayList<String> args) throws ParseException, CommandLineOptionException {

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
            HashMap<String, String> cred_map = getUserCredentials(parsed_options.getOptionValue("F"));
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
            HashMap<String, String> cred_map = new HashMap<String, String>();
            cred_map.put("username", username);
            cred_map.put("password", password);
            cred_map.put("swamp-host", parsed_options.getOptionValue("S", SwampApiWrapper.SWAMP_HOST_NAME));
            return cred_map;
        }
    }

    public HashMap<String, String> projectOptionsHandler(ArrayList<String> args) throws ParseException {

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
            HashMap<String, String> cred_map = new HashMap<String, String>();
            if (parsed_options.hasOption("project-name")){
                cred_map.put("project-name",parsed_options.getOptionValue("project-name"));
            }
            //cred_map.put("list", "list"); Not required
            return cred_map;
        }
    }

    public HashMap<String, String> resultsOptionsHandler(ArrayList<String> args) throws ParseException, CommandLineOptionException {

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
            HashMap<String, String> cred_map = new HashMap<String, String>();
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

    public HashMap<String, String> statusOptionsHandler(ArrayList<String> args) throws ParseException, CommandLineOptionException {

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
            HashMap<String, String> cred_map = new HashMap<String, String>();
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

    public HashMap<String, String> packageOptionsHandler(ArrayList<String> args) throws ParseException, CommandLineOptionException {

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
        opt_grp.addOption(Option.builder("K").required(false).hasArgs().argName("PACKAGE_UUID").longOpt("pkg-uuid")
                .desc("Package UUID").build());

        options.addOptionGroup(opt_grp);

        options.addOption(Option.builder("C").required(false).hasArg().argName("PACKAGE_CONF_FILEPATH").longOpt("pkg-conf")
                .desc("File path to the package conf file").build());
        options.addOption(Option.builder("P").required(false).hasArg().argName("PROJECT_UUID").longOpt("project-uuid")
                .desc("Project UUID to add the package to").build());
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
            HashMap<String, String> cred_map = new HashMap<String, String>();
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
                    return cred_map;
                }else if (!parsed_options.hasOption("C")){
                    throw new CommandLineOptionException(optionMissingStr(options.getOption("C")));
                }else {
                    throw new CommandLineOptionException(optionMissingStr(options.getOption("P")));
                }
            }else {
                if (parsed_options.hasOption("K") && parsed_options.hasOption("P")) {
                    cred_map.put("project-uuid", parsed_options.getOptionValue("P", null));
                    cred_map.put("package-uuid", parsed_options.getOptionValue("K", null));
                    return cred_map;
                }else if (!parsed_options.hasOption("K")){
                    throw new CommandLineOptionException(optionMissingStr(options.getOption("K")));
                }else {
                    throw new CommandLineOptionException(optionMissingStr(options.getOption("P")));
                }
            }
        }
    }

    public HashMap<String, String> userOptionsHandler(ArrayList<String> args) throws ParseException{

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
            HashMap<String, String> cred_map = new HashMap<String, String>();
            if (parsed_options.hasOption("I")){
                cred_map.put("info", "info");
            }
            return cred_map;
        }
    }

    public HashMap<String, String> toolsOptionsHandler(ArrayList<String> args) throws ParseException{

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
            HashMap<String, String> cred_map = new HashMap<String, String>();
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

    public HashMap<String, String> platformOptionsHandler(ArrayList<String> args) throws ParseException{

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
            HashMap<String, String> cred_map = new HashMap<String, String>();
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

    public HashMap<String, String> logoutOptionsHandler(ArrayList<String> args) throws ParseException{

        Options options = new Options();
        options.addOption(Option.builder("H").required(false).longOpt("help").desc("Shows Help").build());

        String[] cmd_args = (String[]) args.toArray(new String[0]);
        CommandLine parsed_options = new DefaultParser().parse(options, cmd_args);
        if (parsed_options.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Command Line Parameters", options);
            return null;
        }else {
            HashMap<String, String> cred_map = new HashMap<String, String>();
            cred_map.put("logout", "logout");
            return cred_map;
        }

    }

    public HashMap<String, String> assessmentOptionsHandler(ArrayList<String> args) throws ParseException, CommandLineOptionException{

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
        options.addOption(Option.builder("T").required(false).hasArg(true).longOpt("tool-uuid")
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
        HashMap<String, String> cred_map = new HashMap<String, String>();
        if (parsed_options.hasOption("Q")){
            cred_map.put("quiet", "quiet");
        }
        if (parsed_options.hasOption("P")){
            cred_map.put("project-uuid", parsed_options.getOptionValue("P"));
            if (parsed_options.hasOption("K")){
                cred_map.put("pkg-uuid", parsed_options.getOptionValue("K"));
                if (parsed_options.hasOption("T")){
                    if (parsed_options.hasOption("F")){
                        cred_map.put("platform-uuid", parsed_options.getOptionValue("F"));
                    }
                    cred_map.put("tool-uuid", parsed_options.getOptionValue("T"));
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

    public HashMap<String, String> processCliArgs(String command, ArrayList<String> cli_args) throws CommandLineOptionException, ParseException{

        HashMap<String, String> opt_map = null;

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

    public int executeCommands(String command, HashMap<String, String> opt_map) throws IOException, SessionExpiredException, InvalidIdentifierException, IncompatibleAssessmentTupleException {

        if (command.equals("login")) {
        	String host_name = opt_map.get("swamp-host");
            
            String user_uuid = api_wrapper.login(opt_map.get("username"), 
            		opt_map.get("password"),
                    host_name);
            
            if (user_uuid != null){
                System.out.println("Login successful");
                System.out.printf("User UUID: %s", user_uuid + "\n");
                api_wrapper.saveSession();
            }
        }else {
            api_wrapper.restoreSession();
            switch (command) {
            case "project":
                if (opt_map.containsKey("project-name")) {
                    Project my_proj = api_wrapper.getProjectFromName(opt_map.get("project-name"));
                    if (my_proj == null){
                        System.out.printf("Project %s does not exist.\n", opt_map.get("project-name"));
                    }else{
                        System.out.printf(my_proj.getUUIDString());
                    }
                }else{
                    api_wrapper.printAllProjects();
                }
                break;
            case "platform":
                if (opt_map.containsKey("platform-name")) {
                    System.out.printf(api_wrapper.getPlatformFromName(opt_map.get("platform-name")).getUUIDString());
                }else {
                    api_wrapper.printAllPlatforms(opt_map.get("pkg-type"));
                }
                break;
            case "tools":
                if (opt_map.containsKey("tool-name")) {
                    Tool my_tool = api_wrapper.getToolFromName(opt_map.get("tool-name"), opt_map.get("project-uuid"));
                    if (my_tool == null){
                        System.out.printf("Tool %s does not exist.\n", opt_map.get("tool-name"));
                    }else{
                        System.out.println(my_tool.getUUIDString());
                    }
                }else{
                    api_wrapper.printAllTools(opt_map.get("project-uuid"));
                }
                break;
            case "package":
                if (opt_map.containsKey("list")) {
                    api_wrapper.printAllPackages(opt_map.get("project-uuid"), true);
                }else if (opt_map.containsKey("pkg-types")) {
                    for (String pkg_type : api_wrapper.getPackageTypesList()) {
                        System.out.println(pkg_type);
                    }
                }else {

                    String package_uuid = api_wrapper.uploadPackage(opt_map.get("pkg-conf"),
                            opt_map.get("pkg-archive"),
                            opt_map.get("project-uuid"),
                            opt_map.containsKey("new-pkg"));;

                            if (opt_map.containsKey("quiet")){
                                System.out.printf(package_uuid);
                            }else{
                                System.out.printf("Package Version UUID: %s\n", package_uuid);
                            }
                }
                break;

            case "assess":
                if (opt_map.containsKey("run-assess")){
                    String assess_uuid = api_wrapper.runAssessment(opt_map.get("pkg-uuid"), opt_map.get("tool-uuid"),
                            opt_map.get("project-uuid"), opt_map.get("platform-uuid"));
                    if (opt_map.containsKey("quiet")){
                        System.out.printf(assess_uuid);
                    }else{
                        System.out.printf("Assessment UUID: %s\n", assess_uuid);
                    }
                }
                if (opt_map.containsKey("list-assess")){
                    api_wrapper.printAssessments(opt_map.get("project-uuid"), opt_map.containsKey("quiet"));
                }
                if (opt_map.containsKey("assess-uuid")){
                    api_wrapper.printAssessment(opt_map.get("assess-uuid"), opt_map.get("project-uuid"));
                }
                break;
            case "results":
                api_wrapper.getAssessmentResults(opt_map.get("project-uuid"), 
                		opt_map.get("results-uuid"),
                        opt_map.get("file-path"));
                break;
            case "status":
                if (opt_map.containsKey("assess-uuid")){
                    api_wrapper.printAssessmentStatus(opt_map.get("project-uuid"), opt_map.get("assess-uuid"));
                }else{
                    api_wrapper.printAllAssessmentStatus(opt_map.get("project-uuid"));
                }
                break;
            case "user":
                api_wrapper.printUserInfo();
                break;
            case "logout":
                api_wrapper.logout();
                break;
            default:
                break;
            }
        }
        return 0;
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
            HashMap<String, String> opt_map = cli.processCliArgs(command, cli_args);
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
