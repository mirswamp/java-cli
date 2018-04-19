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
import org.continuousassurance.swamp.api.ToolVersion;
import org.continuousassurance.swamp.api.User;
import org.continuousassurance.swamp.session.HTTPException;
import org.continuousassurance.swamp.session.util.Proxy;
import org.apache.commons.cli.*;
import org.apache.log4j.varia.NullAppender;
import org.continuousassurance.swamp.cli.exceptions.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Pattern;


public class Cli {

    public static final String VERSION = "1.5.0";
    
    //for backwards compatibility
    public static final ArrayList<String> DISPLAY_COMMANDS = new ArrayList<String>(Arrays.asList(
            "login", 
            "logout", 
            "package OR packages",
            "assess OR assessments", 
            "results",
            //"runs",
            "project OR projects",
            "tool OR tools",
            "platform OR platforms",
            "status",
            "user"));

    public static final ArrayList<String> COMMANDS = new ArrayList<String>(Arrays.asList(
            "login", 
            "logout",
            "package",
            "packages",
            "assess",
            "assessments", 
            "results",
            //"runs",
            "project",
            "projects",
            "tool",
            "tools",
            "platform",
            "platforms",
            "status",
            "user"));


    public static final String DEFAULT_PROJECT = "MyProject";
    
    SwampApiWrapper api_wrapper;

    public static void printHelp() {
        System.out.println("------------------------------------------------------------------------");
        System.out.println("Usage: <program> <command> <options>");
        System.out.println("------------------------------------------------------------------------");
        System.out.println("<command> must be one of the following:");
        //for (String cmd :  COMMANDS) {
        for (String cmd :  DISPLAY_COMMANDS) {
            System.out.println("\t" + cmd);
        }
        System.out.println("------------------------------------------------------------------------");
        System.out.println("For information on the <options> for a <command> execute:");
        System.out.println("\t<program> <command> --help or <program> <command> -H");
        System.out.println("------------------------------------------------------------------------");
        System.out.println("For version: <program> --version or <program> -V");
        System.out.println("For help: <program> --help or <program> -H");
    }

    private static void printVersion() {
        System.out.printf("%s\n", VERSION);
    }
    
    public static boolean isUuid(String str) {

        return (str.length() == 36 && 
                Pattern.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}", str));
    }

    public Cli() throws Exception {
        api_wrapper = new SwampApiWrapper();
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

    public ArrayList<String> reformatArgs(Options main_options, ArrayList<String> args) {
        ArrayList<String> new_args = new ArrayList<String>();

            for(Option opt : main_options.getOptions()) {
                String short_opt_str = "-" + opt.getOpt();
                String long_opt_str = "--" + opt.getLongOpt();
                
                if (args.contains(short_opt_str) || (opt.hasLongOpt() && args.contains(long_opt_str))) {
                    String opt_str = short_opt_str;
                    
                    if (opt.hasLongOpt() && args.contains(long_opt_str)) {
                        opt_str = long_opt_str;
                    }
                    
                    if (args.indexOf(opt_str) > 0) {
                        args.remove(opt_str);
                        new_args.add(opt_str);
                    }
                    new_args.addAll(args);
                    return new_args;
                }
            }

            new_args.addAll(args);
            return new_args;
    }

    public static Map<String, String> getProxyMap(String proxy_str) throws MalformedURLException {
        Map<String, String> hash_map = new HashMap<String, String>();
       
        if (proxy_str != null) {
            URL url = new URL(proxy_str);

            hash_map.put("proxy-host", url.getHost());
            hash_map.put("proxy-scheme", url.getProtocol());

            if (url.getPort() == -1) {
                hash_map.put("proxy-port", url.getProtocol().equals("https") ? "443" : "80");
            }

            if(url.getUserInfo() != null) {
                String userinfo = url.getUserInfo();

                hash_map.put("proxy-user", userinfo.substring(0, userinfo.indexOf(':')));
                hash_map.put("proxy-password", userinfo.substring(userinfo.indexOf(':') + 1));
            }
        }
        
        return hash_map;
    }

 
     public HashMap<String, Object> loginOptionsHandler(String cmd_name, ArrayList<String> args) throws ParseException, CommandLineOptionException, FileNotFoundException, IOException {

        Options options = new Options();
        OptionGroup opt_grp = new OptionGroup();
        opt_grp.setRequired(true);

        opt_grp.addOption(Option.builder("H").required(false).longOpt("help").desc("Shows Help").build());
        opt_grp.addOption(Option.builder("F").required(false).hasArg().longOpt("filepath").argName("CREDENTIALS_FILEPATH")
                .desc("Properties file containing username, password, proxy settings, keystore file path").build());
        opt_grp.addOption(Option.builder("C").required(false).hasArg(false).longOpt("console")
                .desc("Accepts username and password from the terminal").build());
        options.addOptionGroup(opt_grp);

        options.addOption(Option.builder("X").required(false).hasArg().longOpt("proxy").argName("PROXY")
                .desc("URL for http proxy, format: http[s]://<username>:<passoword>@<proxy_host>:<proxy_port>").build());
        
        options.addOption(Option.builder("K").required(false).hasArg().longOpt("keystore").argName("KEYSTORE")
                .desc("Custom keystore file path").build());
        
        options.addOption(Option.builder("S").required(false).hasArg().longOpt("swamp-host").argName("SWAMP_HOST")
                .desc("URL for SWAMP host: default is " + SwampApiWrapper.SWAMP_HOST_NAME).build());
        
        options.addOption(Option.builder("Q").required(false).hasArg(false).longOpt("quiet")
                .desc("Does not show login status message").build());

        if (args.size() == 0) {
            args.add("-H");
        }else {
            args = reformatArgs(options, args);
        }
        
        CommandLine parsed_options = new DefaultParser().parse(options, args.toArray(new String[0]));
        
        if (parsed_options.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(new PrintWriter(System.out, true), 120, 
                    cmd_name + "", 
                    "", options, 4, 4, "", true);
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
                cred_map.putAll(getProxyMap(prop.getProperty("proxy")));
                cred_map.put("keystore", prop.getProperty("keyStore"));
                
            }else if (parsed_options.hasOption("C")) {
                System.out.print("swamp-username: ");
                String username = System.console().readLine();
                System.out.print("swamp-password: ");
                String password = new String(System.console().readPassword());

                cred_map.put("username", username);
                cred_map.put("password", password);

            }
            
            if ((cred_map.get("username") == null ) || (cred_map.get("password") == null)){
                throw new CommandLineOptionException(String.format("No username or password in the file: %s\n",
                        parsed_options.getOptionValue("F")));
            }
            
            if (parsed_options.hasOption("X")) {
                cred_map.putAll(getProxyMap(parsed_options.getOptionValue("X")));
            }
            
            cred_map.put("keystore", parsed_options.getOptionValue("K"));
            
            return cred_map;
        }
        else {
            throw new CommandLineOptionException("Unknown / Incompatible options");
        }

    }

    public HashMap<String, Object> projectOptionsHandler(String cmd_name, ArrayList<String> args) throws ParseException {

            Options options = new Options();
            OptionGroup opt_grp = new OptionGroup();
            opt_grp.setRequired(true);

            opt_grp.addOption(Option.builder("H").required(false).longOpt("help").desc("Shows Help").build());
            opt_grp.addOption(Option.builder("L").required(false).hasArg(false).longOpt("list")
                    .desc("list all projects").build());
            opt_grp.addOption(Option.builder("U").required(false).hasArg(false).longOpt("uuid")
                    .desc("list uuid for a project").build());
            options.addOptionGroup(opt_grp);

            Options uuid_options = new Options();
            {
                uuid_options.addOption(Option.builder("N").required(true).hasArg().argName("PROJECT_NAME").longOpt("name")
                        .desc("Name of the project").build());
            }

            Options list_options = new Options();
            {
                OptionGroup list_opt_grps = new OptionGroup();
                list_opt_grps.addOption(Option.builder("Q").required(false).hasArg(false).longOpt("quiet")
                        .desc("Do not print Headers, Description, Type ").build());
                list_opt_grps.addOption(Option.builder("V").required(false).hasArg(false).longOpt("verbose")
                        .desc("Print UUIDs also").build());
                list_options.addOptionGroup(list_opt_grps);
            }

            if (args.size() == 0 ) {
                args.add("-H");
            }else {
                args = reformatArgs(options, args);
            }

            CommandLine main_options = new DefaultParser().parse(options, args.toArray(new String[0]), true);
            if (main_options.hasOption("help") || args.contains("-H") || args.contains("--help")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("sub-commands", options, true);
                formatter.printHelp(new PrintWriter(System.out, true), 120, 
                        cmd_name + " --" + options.getOption("-L").getLongOpt(), 
                        "", list_options, 4, 4, "", true);
                formatter.printHelp(new PrintWriter(System.out, true), 120,
                        cmd_name + " --" + options.getOption("-U").getLongOpt(), 
                        "", uuid_options, 4, 4, "", true);
                return null;
            }

            HashMap<String, Object> cred_map = new HashMap<String, Object>();

            if (main_options.hasOption("L")){
                CommandLine parsed_options = new DefaultParser().parse(list_options, main_options.getArgList().toArray(new String[0]));
                cred_map.put("sub-command", "list");
                cred_map.put("quiet", parsed_options.hasOption("Q"));
                cred_map.put("verbose", parsed_options.hasOption("V"));
             }else if (main_options.hasOption("U")) {
                CommandLine parsed_options = new DefaultParser().parse(uuid_options, main_options.getArgList().toArray(new String[0]));
                cred_map.put("sub-command", "uuid");
                cred_map.put("project", parsed_options.getOptionValue("N", DEFAULT_PROJECT));
            }
            return cred_map;
        }

    public HashMap<String, Object> resultsOptionsHandler(String cmd_name, ArrayList<String> args) throws ParseException, CommandLineOptionException {

        Options options = new Options();
        OptionGroup opt_grp = new OptionGroup();
        opt_grp.setRequired(true);

        opt_grp.addOption(Option.builder("H").required(false).longOpt("help").desc("Shows Help").build());
        opt_grp.addOption(Option.builder("L").required(false).longOpt("list").hasArg(false).desc("List results").build());
        opt_grp.addOption(Option.builder("D").required(false).longOpt("download").hasArg(false).desc("Download SCARF ").build());
        options.addOptionGroup(opt_grp);

        Options list_options = new Options();
        {
            OptionGroup list_opt_grps = new OptionGroup();
            list_opt_grps.addOption(Option.builder("V").required(false).hasArg(false).longOpt("verbose")
                    .desc("Print Assessment Results UUID as well").build());
            list_options.addOptionGroup(list_opt_grps);
            list_options.addOption(Option.builder("PK").required(false).hasArg().argName("PACKAGE").longOpt("package")
                    .desc("Show results for this package").build());
            list_options.addOption(Option.builder("TL").required(false).hasArg().argName("TOOL").longOpt("tool")
                    .desc("Show results for this tool").build());
            list_options.addOption(Option.builder("PJ").required(false).hasArg().argName("PROJECT").longOpt("project")
                    .desc("Show result for this Project").build());
            list_options.addOption(Option.builder("PL").required(false).hasArg().argName("PLATFORM").longOpt("platform")
                    .desc("Show results for this Platform").build());
        }

        Options download_options = new Options();
        {
            download_options.addOption(Option.builder("V").required(false).hasArg(false).longOpt("quiet")
                    .desc("Do not print Headers").build());
            download_options.addOption(Option.builder("R").required(false).hasArg(true).longOpt("results-uuid").argName("RESULTS_UUID")
                    .desc("Assessment Results UUID").build());
            download_options.addOption(Option.builder("P").required(false).hasArg(true).longOpt("project-uuid").argName("PROJECT_UUID")
                    .desc("Project UUID of a project (this option is deprecated)").build());
            download_options.addOption(Option.builder("F").required(false).hasArg(true).longOpt("file-path").argName("SCARF_FILEPATH")
                    .desc("Filepath to write SCARF Results into, DEFAULT: ./<RESULTS_UUID>.xml").build());
            
            download_options.addOption(Option.builder("PK").required(false).hasArg().argName("PACKAGE").longOpt("package")
                    .desc("Download results for this package name").build());
            download_options.addOption(Option.builder("TL").required(false).hasArg().argName("TOOL").longOpt("tool")
                    .desc("Download results for this tool").build());
            download_options.addOption(Option.builder("PL").required(false).hasArg().argName("PLATFORM").longOpt("platform")
                    .desc("Download results for this platform").build());
        }

        if (args.size() == 0 ) {
            args.add("-H");
        }else {
            args = reformatArgs(options, args);
        }

        CommandLine main_options = null;
        try {
            main_options = new DefaultParser().parse(options, args.toArray(new String[0]), true);
        } catch (ParseException e) {
            //For backwards compatibility, adding option -D
            ArrayList<String> new_args = new ArrayList<String>();
            new_args.add("-D");
            new_args.addAll(args);
            args = new_args;
            main_options = new DefaultParser().parse(options, args.toArray(new String[0]), true);
        }

        if (main_options.hasOption("help") || args.contains("-H") || args.contains("--help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("sub-commands", options, true);
            formatter.printHelp(new PrintWriter(System.out, true), 120, 
                    cmd_name + " --" + options.getOption("-L").getLongOpt(), 
                    "", list_options, 4, 4, "", true);
            formatter.printHelp(new PrintWriter(System.out, true), 120, 
                    cmd_name + " --" + options.getOption("-D").getLongOpt(), 
                    "", download_options, 4, 4, "", true);
            return null;
        }

        HashMap<String, Object> cred_map = new HashMap<String, Object>();

        if (main_options.hasOption("D")) {
            CommandLine parsed_options = new DefaultParser().parse(download_options, main_options.getArgList().toArray(new String[0]));
            cred_map.put("sub-command", "download");

            cred_map.put("quiet", parsed_options.hasOption("Q"));
            cred_map.put("filepath", parsed_options.getOptionValue("F"));
            cred_map.put("results-uuid", parsed_options.getOptionValue("R"));
            cred_map.put("project-uuid", parsed_options.getOptionValue("P"));
            cred_map.put("package", parsed_options.getOptionValue("PK"));
            cred_map.put("tool", parsed_options.getOptionValue("TL"));
            cred_map.put("platform", parsed_options.getOptionValue("PL"));           

            if (cred_map.get("results-uuid") == null && 
                    (cred_map.get("package") == null || cred_map.get("tool") == null)) {
                throw new CommandLineOptionException("Specify options (--package <package> AND --tool <tool>) OR --results-uuid");
            }
            
            return cred_map;
        }else if (main_options.hasOption("L")){
            CommandLine parsed_options = new DefaultParser().parse(list_options, main_options.getArgList().toArray(new String[0]));
            cred_map.put("sub-command", "list");
            cred_map.put("verbose", parsed_options.hasOption("V"));
            cred_map.put("package", parsed_options.getOptionValue("PK"));
            cred_map.put("project", main_options.getOptionValue("PJ"));
            cred_map.put("tool", parsed_options.getOptionValue("TL"));
            cred_map.put("platform", parsed_options.getOptionValue("PL"));           

            return cred_map;
        }
        return cred_map;
    }

    public HashMap<String, Object> statusOptionsHandler(String cmd_name, ArrayList<String> args) throws ParseException, CommandLineOptionException {

        Options options = new Options();
        options.addOption(Option.builder("H").required(false).longOpt("help").desc("Shows Help").build());
        options.addOption(Option.builder("P").required(false).hasArg(true).argName("PROJECT_UUID").longOpt("project-uuid")
                .desc("Project UUID of the project").build());
        options.addOption(Option.builder("A").required(false).hasArg(true).argName("ASSESSMENT_UUID").longOpt("assess-uuid")
                .desc("assessment UUID of an assessment run").build());

        if (args.size() == 0 ) {
            args.add("-H");
        }else {
            args = reformatArgs(options, args);
        }
       
        CommandLine parsed_options = new DefaultParser().parse(options, args.toArray(new String[0]));
        
         if (parsed_options.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(new PrintWriter(System.out, true), 120, 
                    cmd_name + "", 
                    "", options, 4, 4, "", true);
            return null;
        }else {

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
            upload_options.addOption(Option.builder("PJ").required(false).hasArg().argName("PROJECT").longOpt("project")
                    .desc("Name or UUID of the project that this package must be added to.  Default: MyProject").build());
            upload_options.addOption(Option.builder("N").required(false).hasArg(false).longOpt("new-pkg")
                    .desc("Flag if this package must be added as a new package, and not as a new version of an existing package").build());
            upload_options.addOption(Option.builder("O").argName("property=value").numberOfArgs(2).valueSeparator('=').longOpt("os-deps")
                    .desc("use value for given property" ).build());
            upload_options.addOption(Option.builder("P").required(false).hasArg().argName("PROJECT_UUID").longOpt("project-uuid")
                    .desc("UUID of the project that this package must be added to (this option is deprecated, use -PJ)").build());
        }

        Options delete_options = new Options();
        {
            delete_options.addOption(Option.builder("Q").required(false).hasArg(false).longOpt("quiet")
                    .desc("Do not print anything").build());
            delete_options.addOption(Option.builder("PJ").required(false).hasArg().argName("PROJECT").longOpt("project")
                    .desc("Delete packages in this project. if --packages option is not specified, delete all").build());
            delete_options.addOption(Option.builder("PK").required(false).hasArgs().argName("PACKAGES").longOpt("packages")
                    .desc("Delete packages with these names or UUIDs. Accepts multiple names or UUIDs").build());
            delete_options.addOption(Option.builder("I").required(false).hasArgs().argName("PACKAGE_UUID").longOpt("pkg-uuid")
                    .desc("Package Version UUIDs (this option is deprecated, use -PK)").build());
            delete_options.addOption(Option.builder("P").required(false).hasArg().argName("PROJECT_UUID").longOpt("project-uuid")
                    .desc("Delete packages in this project UUID (this option is deprecated, use -PJ)").build());
        }

        Options list_options = new Options();
        {
            OptionGroup list_opt_grps = new OptionGroup();
            list_opt_grps.addOption(Option.builder("Q").required(false).hasArg(false).longOpt("quiet")
                    .desc("Do not print Headers, Description, Type ").build());
            list_opt_grps.addOption(Option.builder("V").required(false).hasArg(false).longOpt("verbose")
                    .desc("Print UUIDs also").build());
            list_options.addOption(Option.builder("PJ").required(false).hasArg().argName("PROJECT").longOpt("project")
                    .desc("Only show packages in this Project (Name or UUID)").build());
            list_options.addOption(Option.builder("KT").required(false).hasArgs().argName("PACKAGE_TYPE").longOpt("pkg-type")
                    .desc("Only show packages of this Type").build());
            list_options.addOption(Option.builder("P").required(false).hasArg().argName("PROJECT_UUID").longOpt("project-uuid")
                    .desc("Only show packages in this Project UUID (this option is deprecated, use -PJ)").build());

            list_options.addOptionGroup(list_opt_grps);
        }

        if (args.size() == 0 ) {
            args.add("-H");
        }else {
            args = reformatArgs(options, args);
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

        HashMap<String, Object> cred_map = new HashMap<String, Object>();

        if (main_options.hasOption("T")){
            cred_map.put("sub-command", "types");
        }else if (main_options.hasOption("L")){
            CommandLine parsed_options = new DefaultParser().parse(list_options, main_options.getArgList().toArray(new String[0]));
            cred_map.put("sub-command", "list");
            cred_map.put("quiet", parsed_options.hasOption("Q"));
            cred_map.put("verbose", parsed_options.hasOption("V"));
            cred_map.put("project", parsed_options.getOptionValue("PJ"));
            if (cred_map.get("project") == null) {
                //Use the deprecated option
                cred_map.put("project", parsed_options.getOptionValue("P"));
            }
            cred_map.put("pkg-type", parsed_options.getOptionValue("KT"));
        }else if (main_options.hasOption("U")) {
            CommandLine parsed_options = new DefaultParser().parse(upload_options, main_options.getArgList().toArray(new String[0]));
            cred_map.put("sub-command", "upload");
            cred_map.put("quiet", parsed_options.hasOption("Q"));
            cred_map.put("pkg-archive", parsed_options.getOptionValue("A"));
            cred_map.put("pkg-conf", parsed_options.getOptionValue("C"));
            cred_map.put("project", parsed_options.getOptionValue("PJ"));
            if (cred_map.get("project") == null) {
                //Use the deprecated option
                cred_map.put("project", parsed_options.getOptionValue("P", DEFAULT_PROJECT));
            }
            cred_map.put("new-pkg", parsed_options.hasOption("N"));	

            if(parsed_options.hasOption("O")){
                Properties prop = parsed_options.getOptionProperties("O");
                cred_map.put("os-deps-map", prop);
            }
        }else if (main_options.hasOption("D")) {
            CommandLine parsed_options = new DefaultParser().parse(delete_options, main_options.getArgList().toArray(new String[0]));			
            cred_map.put("sub-command", "delete");
            cred_map.put("quiet", parsed_options.hasOption("Q"));
            cred_map.put("project", parsed_options.getOptionValue("PJ"));
            if (cred_map.get("project") == null) {
                //Use the deprecated option
                cred_map.put("project", parsed_options.getOptionValue("P"));
            }
            if (parsed_options.getOptionValues("PK") != null) {
                cred_map.put("packages", Arrays.asList(parsed_options.getOptionValues("PK")));
            }else if (parsed_options.getOptionValues("I") != null) {
                //Use the deprecated option
                cred_map.put("packages", Arrays.asList(parsed_options.getOptionValues("I")));
            } else {
                cred_map.put("packages", null);
            }

            if (cred_map.get("project") == null && cred_map.get("packages") == null) {
                throw new CommandLineOptionException(optionMissingStr(delete_options.getOption("PJ")) + 
                        " or|and " + optionMissingStr(delete_options.getOption("PK")));
            }
        }
        return cred_map;
    }

    public HashMap<String, Object> userOptionsHandler(String cmd_name, ArrayList<String> args) throws ParseException{

        Options options = new Options();
        OptionGroup opt_grp = new OptionGroup();
        opt_grp.setRequired(true);

        opt_grp.addOption(Option.builder("H").required(false).longOpt("help").desc("Shows Help").build());
        opt_grp.addOption(Option.builder("I").required(false).hasArg(false).longOpt("info")
                .desc("Displays info about the currently logged in user").build());
        options.addOptionGroup(opt_grp);

        if (args.size() == 0 ) {
            args.add("-H");
        }else {
            args = reformatArgs(options, args);
        }
        
        CommandLine parsed_options = new DefaultParser().parse(options, args.toArray(new String[0]));        

        if (parsed_options.hasOption("H")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(new PrintWriter(System.out, true), 120, 
                    cmd_name + "", 
                    "", options, 4, 4, "", true);
            return null;
        }else {
            HashMap<String, Object> cred_map = new HashMap<String, Object>();
            if (parsed_options.hasOption("I")){
                cred_map.put("info", "info");
            }
            return cred_map;
        }
    }

    public HashMap<String, Object> toolsOptionsHandler(String cmd_name, ArrayList<String> args) throws ParseException {

        Options options = new Options();
        OptionGroup opt_grp = new OptionGroup();
        opt_grp.setRequired(true);

        opt_grp.addOption(Option.builder("H").required(false).longOpt("help").desc("Shows Help").build());
        opt_grp.addOption(Option.builder("L").required(false).hasArg(false).longOpt("list")
                .desc("list all tools").build());
        opt_grp.addOption(Option.builder("U").required(false).hasArg(false).longOpt("uuid")
                .desc("list uuid for a tool").build());
        options.addOptionGroup(opt_grp);

        Options uuid_options = new Options();
        {
            uuid_options.addOption(Option.builder("N").required(false).hasArg().argName("TOOL_NAME").longOpt("name")
                    .desc("Name of the tool").build());
            uuid_options.addOption(Option.builder("P").required(false).hasArg().argName("PROJECT").longOpt("project")
                    .desc("Project Name or UUID").build());
        }

        Options list_options = new Options();
        {
            OptionGroup list_opt_grps = new OptionGroup();
            list_opt_grps.addOption(Option.builder("Q").required(false).hasArg(false).longOpt("quiet")
                    .desc("Do not print Headers, Description, Type ").build());
            list_opt_grps.addOption(Option.builder("V").required(false).hasArg(false).longOpt("verbose")
                    .desc("Print UUIDs also").build());
            list_options.addOptionGroup(list_opt_grps);
            
            list_options.addOption(Option.builder("P").required(false).hasArg().argName("PROJECT").longOpt("project")
                    .desc("Project Name or UUID").build());
            
        }

        if (args.size() == 0 ) {
            args.add("-H");
        }else {
            args = reformatArgs(options, args);
        }

        CommandLine main_options = new DefaultParser().parse(options, args.toArray(new String[0]), true);
        if (main_options.hasOption("help") || args.contains("-H") || args.contains("--help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("sub-commands", options, true);
            formatter.printHelp(new PrintWriter(System.out, true), 120, 
                    cmd_name + " --" + options.getOption("-L").getLongOpt(), 
                    "", list_options, 4, 4, "", true);
            formatter.printHelp(new PrintWriter(System.out, true), 120,
                    cmd_name + " --" + options.getOption("-U").getLongOpt(), 
                    "", uuid_options, 4, 4, "", true);
            return null;
        }

        HashMap<String, Object> cred_map = new HashMap<String, Object>();
        
        if (main_options.hasOption("L")){
            CommandLine parsed_options = new DefaultParser().parse(list_options, main_options.getArgList().toArray(new String[0]));
            cred_map.put("sub-command", "list");
            cred_map.put("quiet", parsed_options.hasOption("Q"));
            cred_map.put("verbose", parsed_options.hasOption("V"));
            cred_map.put("project", parsed_options.getOptionValue("P", DEFAULT_PROJECT));
         }else if (main_options.hasOption("U")) {
            CommandLine parsed_options = new DefaultParser().parse(uuid_options, main_options.getArgList().toArray(new String[0]));
            cred_map.put("sub-command", "uuid");
            cred_map.put("tool", parsed_options.getOptionValue("N"));
            cred_map.put("project", parsed_options.getOptionValue("P", DEFAULT_PROJECT));
        }
        return cred_map;
    }

    public HashMap<String, Object> platformOptionsHandler(String cmd_name, ArrayList<String> args) throws ParseException {

        Options options = new Options();
        OptionGroup opt_grp = new OptionGroup();
        opt_grp.setRequired(true);

        opt_grp.addOption(Option.builder("H").required(false).longOpt("help").desc("Shows Help").build());
        opt_grp.addOption(Option.builder("L").required(false).hasArg(false).longOpt("list")
                .desc("list all platforms").build());
        opt_grp.addOption(Option.builder("U").required(false).hasArg(false).longOpt("uuid")
                .desc("list uuid for a platform").build());
        options.addOptionGroup(opt_grp);

        Options uuid_options = new Options();
        {
            uuid_options.addOption(Option.builder("N").required(false).hasArg().argName("PLATFORM_NAME").longOpt("name")
                    .desc("Name of the project").build());
        }

        Options list_options = new Options();
        {
            OptionGroup list_opt_grps = new OptionGroup();
            list_opt_grps.addOption(Option.builder("Q").required(false).hasArg(false).longOpt("quiet")
                    .desc("Do not print Headers, Description, Type ").build());
            list_opt_grps.addOption(Option.builder("V").required(false).hasArg(false).longOpt("verbose")
                    .desc("Print UUIDs also").build());
            list_options.addOptionGroup(list_opt_grps);
        }

        if (args.size() == 0 ) {
            args.add("-H");
        }else {
            args = reformatArgs(options, args);
        }

        CommandLine main_options = new DefaultParser().parse(options, args.toArray(new String[0]), true);
        if (main_options.hasOption("help") || args.contains("-H") || args.contains("--help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("sub-commands", options, true);
            formatter.printHelp(new PrintWriter(System.out, true), 120, 
                    cmd_name + " --" + options.getOption("-L").getLongOpt(), 
                    "", list_options, 4, 4, "", true);
            formatter.printHelp(new PrintWriter(System.out, true), 120,
                    cmd_name + " --" + options.getOption("-U").getLongOpt(), 
                    "", uuid_options, 4, 4, "", true);
            return null;
        }

        HashMap<String, Object> cred_map = new HashMap<String, Object>();

        if (main_options.hasOption("L")){
            CommandLine parsed_options = new DefaultParser().parse(list_options, main_options.getArgList().toArray(new String[0]));
            cred_map.put("sub-command", "list");
            cred_map.put("quiet", parsed_options.hasOption("Q"));
            cred_map.put("verbose", parsed_options.hasOption("V"));
         }else if (main_options.hasOption("U")) {
            CommandLine parsed_options = new DefaultParser().parse(uuid_options, main_options.getArgList().toArray(new String[0]));
            cred_map.put("sub-command", "uuid");
            cred_map.put("platform", parsed_options.getOptionValue("N"));
        }
        return cred_map;
    }

    public HashMap<String, Object> logoutOptionsHandler(String cmd_name, ArrayList<String> args) throws ParseException{

        Options options = new Options();
        options.addOption(Option.builder("H").required(false).longOpt("help").desc("Shows Help").build());
        options.addOption(Option.builder("Q").required(false).hasArg(false).longOpt("quiet")
                .desc("Less verbose output").build());

        String[] cmd_args = (String[]) args.toArray(new String[0]);
        CommandLine parsed_options = new DefaultParser().parse(options, cmd_args);
        if (parsed_options.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(new PrintWriter(System.out, true), 120, 
                    cmd_name + "", 
                    "", options, 4, 4, "", true);
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
        opt_grp.addOption(Option.builder("R").required(false).longOpt("run").hasArg(false).desc("Run new assessments").build());
        opt_grp.addOption(Option.builder("L").required(false).longOpt("list").hasArg(false).desc("List assessments").build());
        options.addOptionGroup(opt_grp);

        Options run_options = new Options();
        {
            run_options.addOption(Option.builder("Q").required(false).hasArg(false).longOpt("quiet")
                    .desc("Print only the Package UUID with no formatting").build());
            run_options.addOption(Option.builder("PK").required(true).hasArg().argName("PACKAGE").longOpt("package")
                    .desc("Package name or UUID").build());
            //run_options.addOption(Option.builder("PV").required(false).hasArg().argName("PACKAGE_VERSION").longOpt("pkg-version")
            //        .desc("Package version").build());
            run_options.addOption(Option.builder("TL").required(true).hasArgs().argName("TOOL").longOpt("tool")
                    .desc("Tool name or UUID").build());
            //run_options.addOption(Option.builder("TV").required(false).hasArg().argName("TOOL_VERSION").longOpt("tool-version")
            //        .desc("Tool version").build());
            run_options.addOption(Option.builder("PL").required(false).hasArgs().argName("PLATFORM").longOpt("platform")
                    .desc("Platform name or UUID").build());

            run_options.addOption(Option.builder("K").required(false).hasArg().argName("PACKAGE_VERSION_UUID").longOpt("pkg-uuid")
                    .desc("Package version UUID (this option is deprecated, use -PK and -PV)").build());
            run_options.addOption(Option.builder("T").required(false).hasArgs().argName("TOOL_UUID").longOpt("tool-uuid")
                    .desc("Tool UUID (this option is deprecated, use -TL and -TV)").build());
            run_options.addOption(Option.builder("P").required(false).hasArg().argName("PROJECT_UUID").longOpt("project-uuid")
                    .desc("Project UUID (this option is deprecated)").build());
            run_options.addOption(Option.builder("F").required(false).hasArgs().argName("PLATFORM_UUID").longOpt("platform-uuid")
                    .desc("Platform UUID (this option is deprecated, use -PL)").build());
        }

        Options list_options = new Options();
        {
            OptionGroup list_opt_grps = new OptionGroup();
            list_opt_grps.addOption(Option.builder("Q").required(false).hasArg(false).longOpt("quiet")
                    .desc("Do not print Headers, Description, Type ").build());
            list_opt_grps.addOption(Option.builder("V").required(false).hasArg(false).longOpt("verbose")
                    .desc("Print UUIDs also").build());
            list_options.addOptionGroup(list_opt_grps);
            list_options.addOption(Option.builder("PK").required(false).hasArg().argName("PACKAGE").longOpt("package")
                    .desc("Package name or UUID").build());
            list_options.addOption(Option.builder("TL").required(false).hasArg().argName("TOOL").longOpt("tool")
                    .desc("Package version").build());
            list_options.addOption(Option.builder("PJ").required(false).hasArg().argName("PROJECT").longOpt("project")
                    .desc("Only show packages in this Project (Name or UUID)").build());
            list_options.addOption(Option.builder("PL").required(false).hasArg().argName("PLATFORM").longOpt("platform")
                    .desc("Platform name").build());
        }

        if (args.size() == 0 ) {
            args.add("-H");
        }else {
            args = reformatArgs(options, args);
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
                    "", run_options, 4, 4, "", true);
            return null;
        }

        HashMap<String, Object> cred_map = new HashMap<String, Object>();

        if (main_options.hasOption("R")) {

            try {
                CommandLine parsed_options = new DefaultParser().parse(run_options, main_options.getArgList().toArray(new String[0]));
            }catch (MissingOptionException e) {
                for(String missing_opt : (List<String>)e.getMissingOptions())   {
                    if (missing_opt.equals("PK")) {
                        List<String> arg_list = main_options.getArgList(); 
                        if (arg_list.contains("-P")) {
                            arg_list.add("-PK");
                            arg_list.add(arg_list.get(arg_list.indexOf("-K") + 1));
                        }else if (arg_list.contains("--pkg-uuid")) {
                            arg_list.add("-PK");
                            arg_list.add(arg_list.get(arg_list.indexOf("--pkg-uuid") + 1));
                        }
                    }

                    if (missing_opt.equals("TL")) {
                        List<String> arg_list = main_options.getArgList(); 
                        if (arg_list.contains("-T")) {
                            arg_list.add("-TL");
                            arg_list.add(arg_list.get(arg_list.indexOf("-T") + 1));
                        }else if (arg_list.contains("--tool-uuid")) {
                            arg_list.add("-TL");
                            arg_list.add(arg_list.get(arg_list.indexOf("--tool-uuid") + 1));
                        }
                    }
                }
            }

            CommandLine parsed_options = new DefaultParser().parse(run_options, main_options.getArgList().toArray(new String[0]));
            cred_map.put("sub-command", "run");

            cred_map.put("quiet", parsed_options.hasOption("Q"));
            cred_map.put("package", parsed_options.getOptionValue("PK"));
            //cred_map.put("pkg-version", parsed_options.getOptionValue("PV"));
            cred_map.put("tool", Arrays.asList(parsed_options.getOptionValues("TL")));
            //cred_map.put("tool-version", parsed_options.getOptionValue("TV"));			
            if (parsed_options.hasOption("PL")){
                cred_map.put("platform", Arrays.asList(parsed_options.getOptionValues("PL")));
            }else if (parsed_options.hasOption("F")){
                cred_map.put("platform", Arrays.asList(parsed_options.getOptionValues("F")));
            }

            return cred_map;
        }else if (main_options.hasOption("L")){
            CommandLine parsed_options = new DefaultParser().parse(list_options, main_options.getArgList().toArray(new String[0]));
            cred_map.put("sub-command", "list");
            cred_map.put("quiet", parsed_options.hasOption("Q"));
            cred_map.put("verbose", parsed_options.hasOption("V"));
            cred_map.put("package", parsed_options.getOptionValue("PK"));
            cred_map.put("project", main_options.getOptionValue("PJ"));
            cred_map.put("tool", parsed_options.getOptionValue("TL"));
            cred_map.put("platform", parsed_options.getOptionValue("PL"));			

            return cred_map;
        }

        return cred_map;

    }

    public HashMap<String, Object> processCliArgs(String command, ArrayList<String> cli_args) throws CommandLineOptionException, ParseException, FileNotFoundException, IOException{

        HashMap<String, Object> opt_map = null;

        switch (command) {
        case "login":
            opt_map = loginOptionsHandler(command, cli_args);
            break;
        case "package":
        case "packages":
            opt_map = packageOptionsHandler(command, cli_args);
            break;
        case "project":
        case "projects":
            opt_map = projectOptionsHandler(command, cli_args);
            break;
        case "tool":    
        case "tools":
            opt_map = toolsOptionsHandler(command, cli_args);
            break;
        case "assess":
        case "assessments":
            opt_map = assessmentOptionsHandler(command, cli_args);
            break;
        case "platform":
        case "platforms":
            opt_map = platformOptionsHandler(command, cli_args);
            break;
        case "results":
            opt_map = resultsOptionsHandler(command, cli_args);
            break;
        case "status":
            opt_map = statusOptionsHandler(command, cli_args);
            break;
        case "user":
            opt_map = userOptionsHandler(command, cli_args);
            break;
        case "logout":
            opt_map = logoutOptionsHandler(command, cli_args);
            break;
        default:
            break;
        }
        return opt_map;
    }

    public void loginHandler(HashMap<String, Object> opt_map) throws MalformedURLException {
        String host_name = (String)opt_map.get("swamp-host");

        Proxy proxy = SwampApiWrapper.getProxy((String)opt_map.get("proxy-scheme"),
                (String)opt_map.get("proxy-host"),
                (String)opt_map.get("proxy-port"),
                (String)opt_map.get("proxy-user"),
                (String)opt_map.get("proxy-password"));
        
        String user_uuid = api_wrapper.login((String)opt_map.get("username"), 
                (String)opt_map.get("password"),
                host_name,
                proxy,
                (String)opt_map.get("keystore"));

        if (user_uuid != null){

            if ((boolean)opt_map.get("quiet") == false) {
                System.out.println("Login successful");
            }
            //System.out.printf("User UUID: %s", user_uuid + "\n");
            api_wrapper.saveSession();
        }else {
            if ((boolean)opt_map.get("quiet") == false) {
                System.out.println("Login failed");
            }
        }
    }

    public void printAllProjects(boolean quiet, boolean verbose) {
        if (verbose) {
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
                        toCurrentTimeZone(proj.getCreateDate()));
            }
        }else if (quiet) {
            for(Project proj : api_wrapper.getProjectsList()) {
                System.out.printf("%-37s %-25s\n", 
                        proj.getUUIDString(), 
                        proj.getFullName());
            }
        }else {
            System.out.printf("%-25s %-40s %-15s\n", 
                    "Project",
                    "Description",
                    "Date Added");

            for(Project proj : api_wrapper.getProjectsList()) {
                System.out.printf("%-25s %-40s %-15s\n", 
                        proj.getFullName(),
                        proj.getDescription(),
                        toCurrentTimeZone(proj.getCreateDate()));
            }  
        }
    }

    public void projectHandler(HashMap<String, Object> opt_map) {
        
        if (opt_map.get("sub-command").equals("list")) {
            printAllProjects((boolean)opt_map.get("quiet"), (boolean)opt_map.get("verbose"));
        }else {
            Project my_proj = getProject((String)opt_map.get("project"));
            System.out.println(my_proj.getUUIDString());
        }
    }

    public void platformHandler(HashMap<String, Object> opt_map) {
        
        if (opt_map.get("sub-command").equals("list")) {
            if ((boolean)opt_map.get("verbose")) {
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
        }else {
            
            List<String> platform_names = new ArrayList<String>();
            platform_names.add((String)opt_map.get("platform"));
            List<PlatformVersion> platform_versions = getPlatformVersions(platform_names, null);
            System.out.println(platform_versions.get(0).getUUIDString());
        }
    }

    public void toolHandler(HashMap<String, Object> opt_map) {
        
        if (opt_map.get("sub-command").equals("list")) {
            printTools((String)opt_map.get("project"), 
                    (boolean)opt_map.get("quiet"),
                    (boolean)opt_map.get("verbose"));
        }else {
            List<String> tool_names = new ArrayList<String>();
            tool_names.add((String)opt_map.get("tool"));
            List<Tool> tools = getTools(tool_names, (String)opt_map.get("project"), null);
            List<ToolVersion> tool_versions = api_wrapper.getToolVersions(tools.get(0));
            System.out.println(tool_versions.get(0).getUUIDString());
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
                    "UUID", "Package", "Description","Type", "Versions");

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

    public Project getProject(String project_name)  {
        List<Project> projects = new ArrayList<Project>();

        if (project_name != null) {
            if (Cli.isUuid(project_name)) {
                for (Project proj : api_wrapper.getProjectsList()) {
                    if (proj.getUUIDString().equals(project_name)) {
                        projects.add(proj);
                        break;
                    }
                }
            }else {
                for (Project proj : api_wrapper.getProjectsList()) {
                    if (proj.getFullName().equals(project_name)) {
                        projects.add(proj);
                    }
                } 
            }
        }

        if (projects.size() == 0) {
            if (project_name == null) {
                throw new InvalidNameException("Project name/UUID cannot be " + project_name);
            }else if (Cli.isUuid(project_name)) {
                throw new InvalidIdentifierException("No Project found with UUID: " + project_name);
            }else {
                throw new InvalidNameException("No Project found with name: " + project_name);
            }
        }else if (projects.size() > 1) {
            throw new ConflictingNamesException("More than one project has the same name, retrive using UUID");
        }

        return projects.get(0);
    }

    public PackageThing getPackage(String package_name, Project project) {

        List<PackageThing> packages = new ArrayList<PackageThing>();

        if (Cli.isUuid(package_name)) {
            for (PackageThing pkg_thing : api_wrapper.getPackagesList(project.getIdentifierString())) {
                if (pkg_thing.getUUIDString().equals(package_name)) {
                    packages.add(pkg_thing);
                    break;
                }
            }
        }else {

            for (PackageThing pkg_thing : api_wrapper.getPackagesList(project.getIdentifierString())) {
                if (pkg_thing.getName().equals(package_name)) {
                    packages.add(pkg_thing);
                }
            }
        }

        if (packages.size() == 0) {
            if (package_name == null) {
                throw new InvalidNameException("Package name/UUID cannot be " + package_name);
            }else if (Cli.isUuid(package_name)) {
                throw new InvalidIdentifierException("No Package found with UUID: " + package_name);
            }else {
                throw new InvalidNameException("No Package found with name: " + package_name);
            }
        }else if (packages.size() > 1) {
            throw new ConflictingNamesException("More than one package has the same name, retrive using UUID");
        }

        return packages.get(0);
    }

    public PackageVersion getPackageVersion(String package_version, PackageThing package_thing, Project project) {
        List<PackageVersion> target_pkg_vers = new ArrayList<PackageVersion>();

        List<PackageVersion> all_pkg_vers = api_wrapper.getPackageVersions(package_thing);

        if (package_version == null) {
            //Return the latest version
            Collections.sort(all_pkg_vers, new Comparator<PackageVersion>() {
                public int compare(PackageVersion i1, PackageVersion i2) {
                    return (i1.getVersionString().compareTo(i2.getVersionString()));
                }
            });
            target_pkg_vers.add(all_pkg_vers.get(all_pkg_vers.size() - 1));
        }else {
            for (PackageVersion pkg_ver : all_pkg_vers) {
                if (pkg_ver.getVersionString().equals(package_version)) {
                    target_pkg_vers.add(pkg_ver);
                }
            }
        }

        if (target_pkg_vers.size() == 0) {
            if (Cli.isUuid(package_version)) {
                throw new InvalidIdentifierException("No Package Version found with UUID: " + package_version);
            }else {
                throw new InvalidNameException("No Package Version found with name: " + package_version);
            }
        }else if (target_pkg_vers.size() > 1) {
            throw new ConflictingNamesException("More than one package versions has the same name, retrive using UUID");
        }

        return target_pkg_vers.get(0);
    }

    public PackageVersion getPackageVersion(String package_name, String package_version_name, String project_name) {
        Project project = getProject(project_name);
        PackageThing package_thing = getPackage(package_name, project);
        return getPackageVersion(package_version_name, package_thing, project);   
    }
    
    public List<PlatformVersion> getPlatformVersions(List<String> platform_version_names, 
            String package_type) {
        
        List<PlatformVersion> valid_platforms = new ArrayList<PlatformVersion>();
        
        for (PlatformVersion plat_ver: api_wrapper.getAllPlatformVersionsList()) {
            for (String plat_name : platform_version_names) {
                if (Cli.isUuid(plat_name) && plat_name.equals(plat_ver.getUUIDString())) {
                    if (package_type != null) {
                        if (package_type.equalsIgnoreCase("C/C++")) {
                            valid_platforms.add(plat_ver);
                        }
                    }else {
                        valid_platforms.add(plat_ver);
                    }
                }else if (plat_name.equals(plat_ver.getDisplayString())) {
                    if (package_type != null) {
                        if (package_type.equalsIgnoreCase("C/C++")) {
                            valid_platforms.add(plat_ver);
                        }
                    }else {
                        valid_platforms.add(plat_ver);
                    }                        
                }
            }
        }
        
        //Use default platform
        if (valid_platforms.size() == 0) {
            valid_platforms.add(api_wrapper.getDefaultPlatformVersion(package_type));
        }
        
        return valid_platforms;
    }

    public List<Tool> getTools(List<String> tool_names, String project_name, String package_type) {
      
        List<Tool> valid_tools = new ArrayList<Tool>();
        String project_uuid = null;
        if (project_name != null) {
            project_uuid = getProject(project_name).getUUIDString();
        }
        
        for (Tool tool : api_wrapper.getAllTools(project_uuid).values()) {
            for (String tool_name: tool_names) {
                if (tool.getName().equalsIgnoreCase(tool_name)) {

                    if (package_type != null) {
                        if(tool.getSupportedPkgTypes().contains(package_type)) {
                            valid_tools.add(tool);
                        }
                    }else {
                        valid_tools.add(tool);
                    }
                }
            }
        }

        return valid_tools;
    }
    
    public ToolVersion getToolVersionFromUUID(String tool_version_uuid) {

        for (Tool tool : api_wrapper.getAllTools(null).values()) {
            for (ToolVersion tool_version : api_wrapper.getToolVersions(tool)) {
                if (tool_version.getUUIDString().equalsIgnoreCase(tool_version_uuid)) {
                    return tool_version;
                }
            }
        }
        throw new InvalidIdentifierException("No Tool Version found with UUID: " + tool_version_uuid); 
    }
    
    public ToolVersion getToolVersion(String tool_name, String tool_version_num) {

        if (tool_version_num != null) {
            
            if (Cli.isUuid(tool_name)) {
                String tool_version_uuid = tool_name;
                
                for (Tool tool : api_wrapper.getAllTools(null).values()) {
                    for (ToolVersion tool_version : api_wrapper.getToolVersions(tool)) {
                        if (tool_version.getUUIDString().equalsIgnoreCase(tool_version_uuid)) {
                            return tool_version;
                        }
                    }
                }
            }else {
                for (Tool tool : api_wrapper.getAllTools(null).values()) {
                    if (tool.getName().equalsIgnoreCase(tool_name)) {
                        for (ToolVersion tool_version : api_wrapper.getToolVersions(tool)) {
                            if (tool_version.getVersion().equalsIgnoreCase(tool_version_num)) {
                                return tool_version;
                            }
                        }
                    }
                }
            }
        }else {
            if (Cli.isUuid(tool_name)) {
                String tool_version_uuid = tool_name;
                
                for (Tool tool : api_wrapper.getAllTools(null).values()) {
                    for (ToolVersion tool_version : api_wrapper.getToolVersions(tool)) {
                        if (tool_version.getUUIDString().equalsIgnoreCase(tool_version_uuid)) {
                            return tool_version;
                        }
                    }
                }
            }else {
                for (Tool tool : api_wrapper.getAllTools(null).values()) {
                    if (tool.getName().equalsIgnoreCase(tool_name)) {
                        return api_wrapper.getToolVersions(tool).get(0);
                    }
                }
            }
        }
        
        if (Cli.isUuid(tool_name)) {
            throw new InvalidIdentifierException("No Tool Version found with UUID: " + tool_name); 
        }else {
            throw new InvalidNameException("No Tool Version found with name: " + tool_name + " version: " + tool_version_num); 
        }
    }
    
    public List<ToolVersion> getToolVersions(List<String> tool_names) {
          
        List<ToolVersion> valid_tools = new ArrayList<ToolVersion>();
        
        for (String tool_name : tool_names) {
            String tool_version_num = null;
            
            if (tool_name.contains(":")) {
                tool_version_num = tool_name.substring(tool_name.indexOf(':') + 1, tool_name.length());
                tool_name = tool_name.substring(0, tool_name.indexOf(':'));
            }

            valid_tools.add(getToolVersion(tool_name, tool_version_num));
        }
        
        
        return valid_tools;
    }
    
    public void runAssessments(String package_name,
            List<String> tool_names,
            List<String> platforms,
            boolean quiet) {

        Project target_project = null;
        PackageVersion target_pkg = null;
        String pkg_ver_num = null;
        
        if (package_name.contains(":")) {
            pkg_ver_num = package_name.substring(package_name.indexOf(':') + 1, package_name.length());
            package_name = package_name.substring(0, package_name.indexOf(':'));
        }
        
        for (Project project : api_wrapper.getProjectsList()) {
            if (!Cli.isUuid(package_name)) {
                for (PackageThing pkg_thing : api_wrapper.getPackagesList(project.getIdentifierString())) {
                    if (pkg_thing.getName().equals(package_name)) {
                        List<PackageVersion> pkg_vers = api_wrapper.getPackageVersions(pkg_thing);

                        if (pkg_ver_num == null) {
                            Collections.sort(pkg_vers, new Comparator<PackageVersion>() {
                                public int compare(PackageVersion i1, PackageVersion i2) {
                                    return (i1.getVersionString().compareTo(i2.getVersionString()));
                                }
                            });
                            target_pkg = pkg_vers.get(pkg_vers.size() - 1);
                        }else {
                            for (PackageVersion pkg_ver : pkg_vers) {
                                if (pkg_ver.getVersionString().equals(pkg_ver_num)) {
                                    target_pkg = pkg_ver;
                                    break;
                                }
                            }
                        }
                        target_project = project;
                    }
                }
            }else {
                for (PackageVersion pkg_ver : api_wrapper.getPackageVersionsList(project.getIdentifierString())) {
                    if (pkg_ver.getIdentifierString().equals(package_name)) {
                        target_pkg = pkg_ver;
                        target_project = project;
                        break;
                    }
                }

                if (target_pkg  == null) {
                    for (PackageThing pkg_thing : api_wrapper.getPackagesList(project.getIdentifierString())) {
                        if (pkg_thing.getIdentifierString().equals(package_name)) {
                            List<PackageVersion> pkg_vers = api_wrapper.getPackageVersions(pkg_thing);
                            Collections.sort(pkg_vers, new Comparator<PackageVersion>() {
                                public int compare(PackageVersion i1, PackageVersion i2) {
                                    return (i1.getVersionString().compareTo(i2.getVersionString()));
                                }
                            });

                            if (pkg_ver_num == null) {
                                target_pkg = pkg_vers.get(pkg_vers.size()-1);
                                target_project = project;
                                break;
                            }else {
                                for (PackageVersion pkg_ver : pkg_vers) {
                                    if (pkg_ver.getVersionString().equals(pkg_ver_num)) {
                                        target_pkg = pkg_ver;
                                        target_project = project;
                                        break;										
                                    }
                                }

                                if (target_pkg != null && target_project != null) {
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (target_pkg == null) {
            if (Cli.isUuid(package_name)) {
                throw new InvalidIdentifierException("Invalid Package UUID: " + package_name);
            }else {
                if (pkg_ver_num != null) {
                    throw new InvalidNameException(String.format("package: %s-%s not found\n", package_name, pkg_ver_num));
                }else {
                    throw new InvalidNameException(String.format("package: %s not found\n", package_name));
                }
            }
        }

        List<PlatformVersion> valid_platforms = new ArrayList<PlatformVersion>();
        if (platforms != null && target_pkg.getPackageThing().getType().equalsIgnoreCase("C/C++")) {
            for (PlatformVersion plat_ver: api_wrapper.getAllPlatformVersionsList()) {
                for (String plat_name : platforms) {
                    if (Cli.isUuid(plat_name) && plat_name.equals(plat_ver.getUUIDString())) {
                        valid_platforms.add(plat_ver);
                    }else if (plat_name.equals(plat_ver.getDisplayString())) {
                        valid_platforms.add(plat_ver);                        
                    }
                }
            }
        }

        //Use default platform
        if (valid_platforms.size() == 0) {
            valid_platforms.add(api_wrapper.getDefaultPlatformVersion(target_pkg.getPackageThing().getType()));
        }

        List<AssessmentRun> assessment_run = new ArrayList<AssessmentRun>();

        for (ToolVersion tool_version : getToolVersions(tool_names)) {
            assessment_run.addAll(api_wrapper.runAssessment(target_pkg, tool_version, target_project, valid_platforms));
        }
          
        if (assessment_run.size() > 0) {
            if (!quiet) {
                System.out.println("Assessment UUIDs"); 
            }
            for (AssessmentRun arun : assessment_run) {
                System.out.println(arun.getUUIDString()); 
            }
        }else {
            throw new IncompatibleAssessmentTupleException("Could not create assessment run");
        }
    }

    public void listAssessments(String project_name, 
            String package_name, 
            String tool_name,
            String plat_name,
            boolean quiet, 
            boolean verbose) {

        List<AssessmentRun> all_assessments = new ArrayList<AssessmentRun>();

        if (project_name != null) {
            all_assessments.addAll(api_wrapper.getAllAssessments(api_wrapper.getProjectFromName(project_name).getIdentifierString()));
        }else {
            for (Project project : api_wrapper.getProjectsList()) {
                all_assessments.addAll(api_wrapper.getAllAssessments(project.getIdentifierString()));
            }
        }

        if (verbose) {
            System.out.printf("%-37s %-40s %-30s %-20s\n",
                    "UUID", "Package", "Tool","Platform");
        }else if (!quiet) {
            System.out.printf("%-40s %-30s %-20s\n",
                    "Package", "Tool","Platform");
        }		

        for (AssessmentRun arun : all_assessments) {
            if (package_name != null && !arun.getPackageName().equals(package_name)) {
                continue;
            }

            if (tool_name != null && !arun.getToolName().equals(tool_name)) {
                continue;
            }

            if (plat_name != null && !arun.getPlatformName().equals(plat_name)) {
                continue;
            }

            if (verbose) {
                System.out.printf("%-37s %-40s %-30s %-20s\n",
                        arun.getIdentifierString(),
                        arun.getPackageName() + ":" + arun.getPackageVersion(),
                        arun.getToolName() + ":" + arun.getToolVersion(),
                        PlatformVersion.getDisplayString(arun.getPlatformName(),
                                arun.getPlatformVersion()));
            }else {
                System.out.printf("%-40s %-30s %-20s\n",
                        arun.getPackageName() + ":" + arun.getPackageVersion(),
                        arun.getToolName() + ":" + arun.getToolVersion(),
                        PlatformVersion.getDisplayString(arun.getPlatformName(),
                                arun.getPlatformVersion()));
            }
        }
    }

    public void assessmentHandler(HashMap<String, Object> opt_map) {

        if (((String)opt_map.get("sub-command")).equalsIgnoreCase("run")) {

            runAssessments((String)opt_map.get("package"),
                    (List<String>)opt_map.get("tool"), 
                    (List<String>)opt_map.get("platform"),
                    (boolean)opt_map.get("verbose"));

        }else if (((String)opt_map.get("sub-command")).equalsIgnoreCase("list")) {
            listAssessments((String)opt_map.get("project"), 
                    (String)opt_map.get("package"), 
                    (String)opt_map.get("tool"),
                    (String)opt_map.get("platform"),
                    (boolean)opt_map.get("quiet"),
                    (boolean)opt_map.get("verbose"));
        }

    }

    public void resultsHandler(HashMap<String, Object> opt_map) throws IOException{
        if (opt_map.get("sub-command").equals("download")) {
            if ((String)opt_map.get("results-uuid") != null) {
                downloadScarf((String)opt_map.get("project-uuid"), 
                        (String)opt_map.get("results-uuid"),
                        (String)opt_map.get("filepath"),
                        (boolean)opt_map.get("quiet"));
            }else {
                downloadScarf((String)opt_map.get("package"), 
                        (String)opt_map.get("tool"),
                        (String)opt_map.get("platform"),
                        (String)opt_map.get("filepath"),
                        (boolean)opt_map.get("quiet"));
            }
        }else {
            listResults((String)opt_map.get("project"), 
                    (String)opt_map.get("package"), 
                    (String)opt_map.get("tool"),
                    (String)opt_map.get("platform"),
                    (boolean)opt_map.get("verbose"));
        }
    }

    public void downloadScarf(String project_uuid, 
            String asssess_result_uuid, 
            String filepath, 
            boolean quiet) throws IOException {
        
        if (filepath == null) {
            filepath = "./" + asssess_result_uuid + ".xml";
        }

        boolean status = false;
        if (project_uuid != null) {
            status =  api_wrapper.getAssessmentResults(project_uuid, asssess_result_uuid, filepath); 
        }else {
            status =  api_wrapper.getAssessmentResults(asssess_result_uuid, filepath);
        }

        if (!quiet) {
            if (status) {
                System.out.println("Downloaded SCARF into: " + filepath);
            }else {
                System.out.println("Downloaded SCARF for " + asssess_result_uuid + " failed" );
            }
        }
    }

    public void downloadScarf(String package_name, 
            String tool_name,
            String platform, 
            String filepath, 
            boolean verbose) throws IOException {
        
        List<AssessmentRecord> results = new ArrayList<AssessmentRecord>();

        String package_version = null;
        if (package_name.contains(":")) {
            package_version = package_name.substring(package_name.indexOf(':') + 1, package_name.length());
            package_name = package_name.substring(0, package_name.indexOf(':'));
        }
        
        String tool_version = null;
        if (tool_name.contains(":")) {
            tool_version = tool_name.substring(tool_name.indexOf(':') + 1, tool_name.length());
            tool_name = tool_name.substring(0, tool_name.indexOf(':'));
        }
        
        for (Project project : api_wrapper.getProjectsList()) {
            for (AssessmentRecord arecord : api_wrapper.getAllAssessmentRecords(project.getUUIDString())) {
                if (package_name != null && !package_name.equalsIgnoreCase(arecord.getConversionMap().getString("package_name"))) {
                    continue;
                }
                
                if (package_version != null && !package_version.equalsIgnoreCase(arecord.getConversionMap().getString("package_version"))) {
                    continue;
                }
                 
                if (tool_name != null &&  !tool_name.equalsIgnoreCase(arecord.getConversionMap().getString("tool_name"))) {
                    continue;
                }
                
                if(tool_version != null && !tool_version.equalsIgnoreCase(arecord.getConversionMap().getString("tool_version"))) {
                    continue;
                }
                 
                if(platform != null && platform.equalsIgnoreCase(PlatformVersion.getDisplayString(arecord.getConversionMap().getString("platform_name"),
                                arecord.getConversionMap().getString("platform_version")))) {
                    continue;
                }
                results.add(arecord);
            }
        }
        
        if (results.size() > 1) {
            throw new ConflictingNamesException("More than one assessment records have same " +
                    "package_name, package_version, tool_name, tool_version, platform\n, Use Assessment result UUID to download results");
        }
        
        
        if (filepath == null) {
            filepath = "./" + results.get(0).getAssessmentResultUUID() + ".xml";
        }

        boolean status = api_wrapper.getAssessmentResults(results.get(0).getProjectUUID(), 
                results.get(0).getAssessmentResultUUID(), 
                filepath); 
        

        if (!verbose) {
            if (status) {
                System.out.println("Downloaded SCARF into: " + filepath);
            }else {
                System.out.println("Downloading SCARF for " + results.get(0).getAssessmentResultUUID() + " failed" );
            }
        }
    }
    
    public String toCurrentTimeZone(Date date) {
        String converted_date = "";

        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);

            TimeZone this_time_zone = Calendar.getInstance().getTimeZone();
            if (!this_time_zone.inDaylightTime(date)) {
                calendar.add(Calendar.MILLISECOND, this_time_zone.getDSTSavings());
            }

            SimpleDateFormat date_format = new SimpleDateFormat("MM/dd/yyyy HH:mm");
            date_format.setTimeZone(Calendar.getInstance().getTimeZone());
            converted_date =  date_format.format(calendar.getTime());
        }catch (Exception e){ 
            e.printStackTrace();
        }

        return converted_date;
    }

    public void listResults(String project_name, 
            String package_name, 
            String tool_name,
            String plat_name,
            boolean verbose) {

        List<AssessmentRecord> all_results = new ArrayList<AssessmentRecord>();

        if (project_name != null) {
            all_results.addAll(api_wrapper.getAllAssessmentRecords(getProject(project_name).getUUIDString()));
        }else {
            for (Project project : api_wrapper.getProjectsList()) {
                all_results.addAll(api_wrapper.getAllAssessmentRecords(project.getUUIDString()));
            }
        }

        Collections.sort(all_results, new Comparator<AssessmentRecord>() {
            public int compare(AssessmentRecord i1, AssessmentRecord i2) {
                return (i2.getConversionMap().getDate("create_date").compareTo(i1.getConversionMap().getDate("create_date")));
            }
        });

        if (verbose) {
            System.out.printf("%-37s %-40s %-30s %-20s %-20s %-20s %10s\n",
                    "Assessment Result UUID", "Package", "Tool","Platform", "Date", "Status", "Results");      
        }else {
            System.out.printf("%-40s %-30s %-20s %-20s %-20s %10s\n",
                    "Package", "Tool", "Platform", "Date", "Status", "Results");      
        }

        for (AssessmentRecord arun : all_results) {
            if (package_name != null && !arun.getPkg().getName().equals(package_name)) {
                continue;
            }

            if (tool_name != null && !arun.getTool().getName().equals(tool_name)) {
                continue;
            }

            if (plat_name != null && !arun.getPlatform().getName().equals(plat_name)) {
                continue;
            }

            if (verbose) {
                System.out.printf("%-37s %-40s %-30s %-20s %-20s %-20s %10s\n",
                        arun.getAssessmentResultUUID(),
                        arun.getConversionMap().getString("package_name") + "-" + arun.getConversionMap().getString("package_version"),
                        arun.getConversionMap().getString("tool_name") + "-" + arun.getConversionMap().getString("tool_version"),
                        PlatformVersion.getDisplayString(arun.getConversionMap().getString("platform_name"), arun.getConversionMap().getString("platform_version")),
                        toCurrentTimeZone(arun.getConversionMap().getDate("create_date")),
                        arun.getConversionMap().getString("status"),
                        arun.getWeaknessCount());    
            }else {

                System.out.printf("%-40s %-30s %-20s %-20s %-20s %10s\n",
                        arun.getConversionMap().getString("package_name") + ":" + arun.getConversionMap().getString("package_version"),
                        arun.getConversionMap().getString("tool_name") + ":" + arun.getConversionMap().getString("tool_version"),
                        PlatformVersion.getDisplayString(arun.getConversionMap().getString("platform_name"), arun.getConversionMap().getString("platform_version")),
                        toCurrentTimeZone(arun.getConversionMap().getDate("create_date")),
                        arun.getConversionMap().getString("status"),
                        arun.getWeaknessCount()); 
            }
        }
    }

    public void statusHandler(HashMap<String, Object> opt_map) {
        printAssessmentStatus((String)opt_map.get("project-uuid"), (String)opt_map.get("assess-uuid"));
    }

    public void printUserInfo(HashMap<String, Object> opt_map) {
        User user = api_wrapper.getUserInfo();
        System.out.printf("%s\n", "User:\t" + user.getFirstName() + " " + user.getLastName());
        System.out.printf("%s\n", "Email:\t" + user.getEmail());
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
            case "project":
            case "projects":
                projectHandler(opt_map);
                break;
            case "platform":
            case "platforms":
                platformHandler(opt_map);
                break;
            case "tool":    
            case "tools":
                toolHandler(opt_map);
                break;
            case "package":    
            case "packages":
                packageHandler(opt_map);
                break;
            case "assess":
            case "assessments":
                assessmentHandler(opt_map);
                break;
            case "results":
                resultsHandler(opt_map);
                break;
            case "status":
                statusHandler(opt_map);
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


    public void printTools(String project_name, boolean quiet, boolean verbose) throws InvalidIdentifierException {
        
        Project project = getProject(project_name);
        
        if(quiet){
            for(Tool tool : api_wrapper.getAllTools(project.getUUIDString()).values()){			
                System.out.printf("%-21s\n", tool.getName());
            }
        }else if(verbose){
            System.out.printf("%-37s %-21s %15s %-40s\n",
                    "UUID",
                    "Tool",
                    "Version",
                    "Supported Package Types");

            for(Tool tool : api_wrapper.getAllTools(project.getUUIDString()).values()) {
                for (ToolVersion tool_version : api_wrapper.getToolVersions(tool)) {
                    System.out.printf("%-37s %-21s %15s %-40s\n",
                            tool_version.getUUIDString(),
                            tool_version.getTool().getName(),
                            tool_version.getVersion(),
                            tool.getSupportedPkgTypes());
                }
            }
        }else {
            System.out.printf("%-21s %15s %-40s\n",
                    "Tool",
                    "Version",
                    "Supported Package Types");

            for(Tool tool : api_wrapper.getAllTools(project.getUUIDString()).values()) {
                for (ToolVersion tool_version : api_wrapper.getToolVersions(tool)) {
                    System.out.printf("%-21s %15s %-40s\n",
                            tool_version.getTool().getName(),
                            tool_version.getVersion(),
                            tool.getSupportedPkgTypes());
                }
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

        AssessmentRecord assessment_record = null;

        if (project_uuid != null) {
            assessment_record = api_wrapper.getAssessmentRecord(project_uuid, assessment_uuid);
        }else {
            for (Project project : api_wrapper.getProjectsList()) {
                for (AssessmentRecord record : api_wrapper.getAllAssessmentRecords(project.getUUIDString())) {

                    if (record.getAssessmentRunUUID().equals(assessment_uuid)) {
                        assessment_record = record;
                        break;
                    }
                }
            }
        }

        if (assessment_record != null) {
            System.out.printf("%s, %d", 
                    //AssessmentStatus.translateAssessmentStatus(assessment_record.getStatus()),
                    assessment_record.getStatus(),
                    assessment_record.getWeaknessCount());

            if (assessment_record.getAssessmentResultUUID() == null){
                System.out.printf("\n");
            }else{
                System.out.printf(", %-37s\n", assessment_record.getAssessmentResultUUID());
            }
        }else {
            throw new InvalidIdentifierException("Invalid Assessment UUID: " + assessment_uuid);  
        }
    }

    public static void main(String[] args) throws Exception {

        org.apache.log4j.BasicConfigurator.configure(new NullAppender());

        if (args.length == 1 && (args[0].equals("-V") || args[0].equals("-v") ||
                args[0].equals("--version"))){
            Cli.printVersion();
            return;
        }else if (args.length == 0 || args[0].equals("-H") || args[0].equals("-h") ||
                args[0].equals("--help")) {
            printHelp();
            return;
        } else if (!Cli.COMMANDS.contains(args[0]) ){
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
