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
import org.apache.log4j.Logger;
import org.apache.log4j.varia.NullAppender;
import org.continuousassurance.swamp.cli.exceptions.*;
import org.continuousassurance.swamp.cli.util.AssessmentStatus;

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

    protected static final String VERSION = "1.5.0";
    protected static final String VERSION_SEPERATOR = "::";
    
    //Sub Commands
    protected static final String SC_LOGIN = "login";
    protected static final String SC_LOGOUT = "logout";  
    protected static final String SC_PACKAGES = "packages";
    protected static final String SC_TOOLS = "tools";
    protected static final String SC_ASSESSMENTS = "assessments";
    protected static final String SC_PROJECTS = "projects";
    protected static final String SC_RESULTS = "results";
    protected static final String SC_PLATFORMS = "platforms";
    protected static final String SC_STATUS = "status";
    
    //for backwards compatibility
    protected static final ArrayList<String> DISPLAY_COMMANDS = new ArrayList<String>(Arrays.asList(
            SC_LOGIN, 
            SC_LOGOUT, 
            "package" + " OR " + SC_PACKAGES,
            "tool" + " OR " + SC_TOOLS,
            "assess" + " OR " + SC_ASSESSMENTS, 
            SC_RESULTS,
            //"runs",
            "project" + " OR " + SC_PROJECTS,
            "platform" + " OR " + SC_PLATFORMS,
            SC_STATUS,
            "user"));

    protected static final ArrayList<String> COMMANDS = new ArrayList<String>(Arrays.asList(
            SC_LOGIN, 
            SC_LOGOUT,
            "package",
            SC_PACKAGES,
            "assess",
            SC_ASSESSMENTS, 
            SC_RESULTS,
            //"runs",
            "project",
            SC_PROJECTS,
            "tool",
            SC_TOOLS,
            "platform",
            SC_PLATFORMS,
            SC_STATUS,
            "user"));

    static final Logger LOGGER = Logger.getLogger(Cli.class);
    protected static final String DEFAULT_PROJECT = "MyProject";

    SwampApiWrapper apiWrapper;

    public static void printHelp() {
        System.out.println("------------------------------------------------------------------------");
        System.out.println("Usage: <program> <sub-command> <options>");
        System.out.println("------------------------------------------------------------------------");
        System.out.println("<sub-command> must be one of the following:");
        //for (String cmd :  COMMANDS) {
        for (String cmd :  DISPLAY_COMMANDS) {
            System.out.println("\t" + cmd);
        }
        System.out.println("------------------------------------------------------------------------");
        System.out.println("For information on the <options> for a <sub-command> execute:");
        System.out.println("\t<program> <sub-command> --help or <program> <sub-command> -H");
        System.out.println("------------------------------------------------------------------------");
        System.out.println("For version: <program> --version or <program> -V");
        System.out.println("For help: <program> --help or <program> -H");
        System.out.println("------------------------------------------------------------------------");
    }

    private static void printVersion() {
        System.out.println(VERSION);
    }

    public static boolean isUuid(String str) {

        return (str.length() == 36 && 
                Pattern.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}", str));
    }

    public Cli() throws Exception {
        apiWrapper = new SwampApiWrapper();
    }

    protected HashMap<String, Object> getUserCredentials(String filename) throws FileNotFoundException, IOException {
        HashMap<String, Object> cred_map = new HashMap<String, Object>();
        Properties prop = new Properties();
      
        prop.load(new FileInputStream(filename));
        cred_map.put("username", prop.getProperty("username"));
        cred_map.put("password", prop.getProperty("password"));
        
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

    public ArrayList<String> reformatArgs(Options mainOptions, ArrayList<String> args) {
        ArrayList<String> new_args = new ArrayList<String>();
        
        for(Option opt : mainOptions.getOptions()) {

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

    
    public static Map<String, String> getProxyMap(String proxyStr) throws MalformedURLException {
        Map<String, String> hash_map = new HashMap<String, String>();

        if (proxyStr != null) {
            URL url = new URL(proxyStr);

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

    protected static String[] getNameAndVersion(String name) {
        String[] name_version = new String[2];
        
        if (name.contains(VERSION_SEPERATOR)) {
            int sep_index = name.indexOf(VERSION_SEPERATOR);
            name_version[0] = name.substring(0, sep_index);
            name_version[1] = name.substring(sep_index + VERSION_SEPERATOR.length());
            
        }else {
            name_version[0] = name;
            name_version[1] = null;
        }
        
        return name_version;
    }


    public HashMap<String, Object> loginOptionsHandler(String cmdName, 
            ArrayList<String> args) throws ParseException, CommandLineOptionException, FileNotFoundException, IOException {

        Options options = new Options();
        OptionGroup opt_grp = new OptionGroup();
        opt_grp.setRequired(true);

        opt_grp.addOption(Option.builder("H").required(false).longOpt("help").desc("Shows Help").build());
        opt_grp.addOption(Option.builder("F").required(false).hasArg().longOpt("filepath").argName("CREDENTIALS_FILEPATH")
                .desc("Properties file containing username, password, proxy settings, keystore file path").build());
        opt_grp.addOption(Option.builder("C").required(false).hasArg(false).longOpt("console")
                .desc("Accepts username and password from the terminal").build());
        options.addOptionGroup(opt_grp);

        options.addOption(Option.builder("S").required(false).hasArg().longOpt("swamp-host").argName("SWAMP_HOST")
                .desc("URL for SWAMP host: default is " + SwampApiWrapper.SWAMP_HOST_NAME).build());

        options.addOption(Option.builder("X").required(false).hasArg().longOpt("proxy").argName("PROXY")
                .desc("URL for http proxy, format: http[s]://[<username>:<passoword>]@<proxy_host>[:<proxy_port>]").build());

        options.addOption(Option.builder("K").required(false).hasArg().longOpt("keystore").argName("KEYSTORE")
                .desc("Custom keystore (that has SSL/TLS certificate for SiB) file path").build());

        options.addOption(Option.builder("Q").required(false).hasArg(false).longOpt("quiet")
                .desc("Does not show login status message").build());

        if (args.isEmpty()) {
            args.add("-H");
        }
        
        /*else {
            args = reformatArgs(options, args);
        }*/

        CommandLine parsed_options = new DefaultParser().parse(options, args.toArray(new String[0]));

        if (parsed_options.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(new PrintWriter(System.out, true), 120, 
                    cmdName + "", 
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

    public HashMap<String, Object> projectOptionsHandler(String cmdName, 
            ArrayList<String> args) throws ParseException {

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

        if (args.isEmpty() ) {
            args.add("-H");
        }else {
            args = reformatArgs(options, args);
        }

        CommandLine main_options = new DefaultParser().parse(options, args.toArray(new String[0]), true);
        if (main_options.hasOption("help") || args.contains("-H") || args.contains("--help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("sub-commands", options, true);
            formatter.printHelp(new PrintWriter(System.out, true), 120, 
                    cmdName + " --" + options.getOption("-L").getLongOpt(), 
                    "", list_options, 4, 4, "", true);
            formatter.printHelp(new PrintWriter(System.out, true), 120,
                    cmdName + " --" + options.getOption("-U").getLongOpt(), 
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

    public HashMap<String, Object> resultsOptionsHandler(String cmdName, 
            ArrayList<String> args) throws ParseException, CommandLineOptionException {

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
            list_opt_grps.addOption(Option.builder("Q").required(false).hasArg(false).longOpt("quiet")
                    .desc("Do not print Headers").build());
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
            download_options.addOption(Option.builder("Q").required(false).hasArg(false).longOpt("quiet")
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

        if (args.isEmpty() ) {
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
                    cmdName + " --" + options.getOption("-L").getLongOpt(), 
                    "", list_options, 4, 4, "", true);
            formatter.printHelp(new PrintWriter(System.out, true), 120, 
                    cmdName + " --" + options.getOption("-D").getLongOpt(), 
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

    public HashMap<String, Object> statusOptionsHandler(String cmdName, 
            ArrayList<String> args) throws ParseException, CommandLineOptionException {

        Options options = new Options();
        OptionGroup opt_grp = new OptionGroup();
        opt_grp.setRequired(true);
        opt_grp.addOption(Option.builder("H").required(false).longOpt("help").desc("Shows Help").build());
        opt_grp.addOption(Option.builder("A").required().hasArg(true).argName("ASSESSMENT_UUID").longOpt("assess-uuid")
                .desc("UUID of an assessment run").build());
        options.addOption(Option.builder("P").required(false).hasArg(true).argName("PROJECT_UUID").longOpt("project-uuid")
                .desc("Project UUID of the project. This option is deprecated").build());
        options.addOption(Option.builder("Q").required(false).hasArg(false).longOpt("quiet")
                .desc("Do not print Headers").build());
        options.addOptionGroup(opt_grp);

        if (args.isEmpty() ) {
            args.add("-H");
        }else {
            args = reformatArgs(options, args);
        }

        CommandLine parsed_options = new DefaultParser().parse(options, args.toArray(new String[0]));

        if (parsed_options.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(new PrintWriter(System.out, true), 120, 
                    cmdName + "", 
                    "", options, 4, 4, "", true);
            return null;
        }else {

            if(!parsed_options.hasOption("assess-uuid")){
                throw new CommandLineOptionException(optionMissingStr(options.getOption("A")));
            }
            HashMap<String, Object> cred_map = new HashMap<String, Object>();
            cred_map.put("project-uuid", parsed_options.getOptionValue("project-uuid"));
            cred_map.put("assess-uuid", parsed_options.getOptionValue("assess-uuid"));
            cred_map.put("quiet", parsed_options.hasOption("Q"));
            return cred_map;
        }
    }

    public HashMap<String, Object> packageOptionsHandler(String cmdName, 
            ArrayList<String> args) throws ParseException, CommandLineOptionException {

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
                    .desc("Delete packages in this project only." + 
            " if --packages option is not specified, and --all-pkgs is specified then deletes all package").build());
            delete_options.addOption(Option.builder("PK").required(false).hasArgs().argName("PACKAGE").longOpt("package")
                    .desc("Delete packages with these names or UUIDs. Accepts multiple names or UUIDs." + 
            " To delete a particular version of a package, the option value must be <package_name>::<package_version>." + 
                            " If no version is specified, deletes all versions of the package").build());
            delete_options.addOption(Option.builder("A").required(false).hasArg(false).longOpt("all-pkgs")
                    .desc("Delete all packages in the project. This option must be used in conjunction with --project option").build());           

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

        if (args.isEmpty()) {
            args.add("-H");
        }else {
            args = reformatArgs(options, args);
        }

        CommandLine main_options = new DefaultParser().parse(options, args.toArray(new String[0]), true);
        if (main_options.hasOption("help") || args.contains("-H") || args.contains("--help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("sub-commands", options, true);
            formatter.printHelp(new PrintWriter(System.out, true), 120, 
                    cmdName + " --" + options.getOption("-D").getLongOpt(), 
                    "", delete_options, 4, 4, "", true);
            formatter.printHelp(new PrintWriter(System.out, true), 120, 
                    cmdName + " --" + options.getOption("-L").getLongOpt(), 
                    "", list_options, 4, 4, "", true);
            formatter.printHelp(new PrintWriter(System.out, true), 120, 
                    cmdName + " --" + options.getOption("-T").getLongOpt(), 
                    "", new Options(), 4, 4, "", true);
            formatter.printHelp(new PrintWriter(System.out, true), 120,
                    cmdName + " --" + options.getOption("-U").getLongOpt(), 
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
                cred_map.put("package", Arrays.asList(parsed_options.getOptionValues("PK")));
            }else if (parsed_options.getOptionValues("I") != null) {
                //Use the deprecated option
                cred_map.put("package", Arrays.asList(parsed_options.getOptionValues("I")));
            } else {
                cred_map.put("package", null);
            }

            cred_map.put("delete-all", parsed_options.hasOption("A"));

            if (cred_map.get("project") == null && cred_map.get("package") == null) {
                throw new CommandLineOptionException(optionMissingStr(delete_options.getOption("PJ")) + 
                        " or|and " + optionMissingStr(delete_options.getOption("PK")));
            }
            
            if (cred_map.get("project") != null && cred_map.get("package") == null && (boolean)cred_map.get("delete-all") == false) {
                throw new CommandLineOptionException(optionMissingStr(delete_options.getOption("A")));
            }
        }
        return cred_map;
    }

    public HashMap<String, Object> userOptionsHandler(String cmdName, 
            ArrayList<String> args) throws ParseException{

        Options options = new Options();
        OptionGroup opt_grp = new OptionGroup();
        opt_grp.setRequired(true);

        opt_grp.addOption(Option.builder("H").required(false).longOpt("help").desc("Shows Help").build());
        opt_grp.addOption(Option.builder("I").required(false).hasArg(false).longOpt("info")
                .desc("Displays info about the currently logged in user").build());
        options.addOptionGroup(opt_grp);

        if (args.isEmpty()) {
            args.add("-H");
        }else {
            args = reformatArgs(options, args);
        }

        CommandLine parsed_options = new DefaultParser().parse(options, args.toArray(new String[0]));        

        if (parsed_options.hasOption("H")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(new PrintWriter(System.out, true), 120, 
                    cmdName + "", 
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

    public HashMap<String, Object> toolsOptionsHandler(String cmdName, 
            ArrayList<String> args) throws ParseException {

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
                    .desc("Project Name or UUID. This option is deprecated.").build());
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
                    .desc("Project Name or UUID. This option is deprecated.").build());

        }

        if (args.isEmpty()) {
            args.add("-H");
        }else {
            args = reformatArgs(options, args);
        }

        CommandLine main_options = new DefaultParser().parse(options, args.toArray(new String[0]), true);
        if (main_options.hasOption("help") || args.contains("-H") || args.contains("--help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("sub-commands", options, true);
            formatter.printHelp(new PrintWriter(System.out, true), 120, 
                    cmdName + " --" + options.getOption("-L").getLongOpt(), 
                    "", list_options, 4, 4, "", true);
            formatter.printHelp(new PrintWriter(System.out, true), 120,
                    cmdName + " --" + options.getOption("-U").getLongOpt(), 
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

    public HashMap<String, Object> platformOptionsHandler(String cmdName, 
            ArrayList<String> args) throws ParseException {

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

        if (args.isEmpty() ) {
            args.add("-H");
        }else {
            args = reformatArgs(options, args);
        }

        CommandLine main_options = new DefaultParser().parse(options, args.toArray(new String[0]), true);
        if (main_options.hasOption("help") || args.contains("-H") || args.contains("--help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("sub-commands", options, true);
            formatter.printHelp(new PrintWriter(System.out, true), 120, 
                    cmdName + " --" + options.getOption("-L").getLongOpt(), 
                    "", list_options, 4, 4, "", true);
            formatter.printHelp(new PrintWriter(System.out, true), 120,
                    cmdName + " --" + options.getOption("-U").getLongOpt(), 
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

    public HashMap<String, Object> logoutOptionsHandler(String cmdName, 
            ArrayList<String> args) throws ParseException{

        Options options = new Options();
        options.addOption(Option.builder("H").required(false).longOpt("help").desc("Shows Help").build());
        options.addOption(Option.builder("Q").required(false).hasArg(false).longOpt("quiet")
                .desc("Less verbose output").build());

        String[] cmd_args = (String[]) args.toArray(new String[0]);
        CommandLine parsed_options = new DefaultParser().parse(options, cmd_args);
        if (parsed_options.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(new PrintWriter(System.out, true), 120, 
                    cmdName + "", 
                    "", options, 4, 4, "", true);
            return null;
        }else {
            HashMap<String, Object> cred_map = new HashMap<String, Object>();
            cred_map.put(SC_LOGOUT, SC_LOGOUT);
            if (parsed_options.hasOption("Q")){
                cred_map.put("quiet", true);
            }else {
                cred_map.put("quiet", false);
            }
            return cred_map;
        }

    }

    public HashMap<String, Object> assessmentOptionsHandler(String cmdName, 
            ArrayList<String> args) throws ParseException, CommandLineOptionException{

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
            run_options.addOption(Option.builder("TL").required(true).hasArgs().argName("TOOL").longOpt("tool")
                    .desc("Tool name or UUID").build());
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

        if (args.isEmpty()) {
            args.add("-H");
        }else {
            args = reformatArgs(options, args);
        }

        CommandLine main_options = new DefaultParser().parse(options, args.toArray(new String[0]), true);
        if (main_options.hasOption("help") || args.contains("-H") || args.contains("--help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("sub-commands", options, true);
            formatter.printHelp(new PrintWriter(System.out, true), 120, 
                    cmdName + " --" + options.getOption("-L").getLongOpt(), 
                    "", list_options, 4, 4, "", true);
            formatter.printHelp(new PrintWriter(System.out, true), 120, 
                    cmdName + " --" + options.getOption("-R").getLongOpt(), 
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

    public HashMap<String, Object> processCliArgs(String command, 
            ArrayList<String> cliArgs) throws CommandLineOptionException, ParseException, FileNotFoundException, IOException{

        HashMap<String, Object> opt_map = null;

        switch (command) {
        case SC_LOGIN:
            opt_map = loginOptionsHandler(command, cliArgs);
            break;
        case "package":
        case SC_PACKAGES:
            opt_map = packageOptionsHandler(command, cliArgs);
            break;
        case "project":
        case SC_PROJECTS:
            opt_map = projectOptionsHandler(command, cliArgs);
            break;
        case "tool":    
        case SC_TOOLS:
            opt_map = toolsOptionsHandler(command, cliArgs);
            break;
        case "assess":
        case SC_ASSESSMENTS:
            opt_map = assessmentOptionsHandler(command, cliArgs);
            break;
        case "platform":
        case SC_PLATFORMS:
            opt_map = platformOptionsHandler(command, cliArgs);
            break;
        case SC_RESULTS:
            opt_map = resultsOptionsHandler(command, cliArgs);
            break;
        case SC_STATUS:
            opt_map = statusOptionsHandler(command, cliArgs);
            break;
        case "user":
            opt_map = userOptionsHandler(command, cliArgs);
            break;
        case SC_LOGOUT:
            opt_map = logoutOptionsHandler(command, cliArgs);
            break;
        default:
            break;
        }
        return opt_map;
    }

    public void loginHandler(HashMap<String, Object> optMap) throws MalformedURLException {
        String host_name = (String)optMap.get("swamp-host");

        Proxy proxy = SwampApiWrapper.getProxy((String)optMap.get("proxy-scheme"),
                (String)optMap.get("proxy-host"),
                (String)optMap.get("proxy-port"),
                (String)optMap.get("proxy-user"),
                (String)optMap.get("proxy-password"));

        String user_uuid = apiWrapper.login((String)optMap.get("username"), 
                (String)optMap.get("password"),
                host_name,
                proxy,
                (String)optMap.get("keystore"));

        if (user_uuid != null){

            if ((boolean)optMap.get("quiet") == false) {
                System.out.println("Login successful");
            }

            apiWrapper.saveSession();
        }else {
            if ((boolean)optMap.get("quiet") == false) {
                System.out.println("Login failed");
            }
        }
    }

    public void printAllProjects(boolean quiet, boolean verbose) {
        if (verbose) {
            System.out.println(String.format("%-37s %-25s %-40s %-15s", 
                    "UUID",
                    "Project",
                    "Description",
                    "Date Added"));
            System.out.println("------------------------------------------------------------------------");
            
            for(Project proj : apiWrapper.getProjectsList()) {
                System.out.println(String.format("%-37s %-25s %-40s %-15s", 
                        proj.getUUIDString(), 
                        proj.getFullName(),
                        proj.getDescription(),
                        toCurrentTimeZone(proj.getCreateDate())));
            }
        }else if (!quiet) {
            System.out.println(String.format("%-25s %-40s %-15s", 
                    "Project",
                    "Description",
                    "Date Added"));
            System.out.println("------------------------------------------------------------------------");
            
            for(Project proj : apiWrapper.getProjectsList()) {
                System.out.println(String.format("%-25s %-40s %-15s", 
                        proj.getFullName(),
                        proj.getDescription(),
                        toCurrentTimeZone(proj.getCreateDate())));
            }  
        }else {
            for(Project proj : apiWrapper.getProjectsList()) {
                System.out.println(proj.getFullName());
            }
        }
    }

    public void projectHandler(HashMap<String, Object> optMap) {

        if (optMap.get("sub-command").equals("list")) {
            printAllProjects((boolean)optMap.get("quiet"), (boolean)optMap.get("verbose"));
        }else {
            Project my_proj = getProject((String)optMap.get("project"));
            System.out.println(my_proj.getUUIDString());
        }
    }

    public void platformHandler(HashMap<String, Object> optMap) {

        if (optMap.get("sub-command").equals("list")) {
            if ((boolean)optMap.get("verbose")) {
                System.out.println(String.format("%-37s %-30s", "UUID", "Platform"));
                System.out.println("------------------------------------------------------------------------");
                
                for (PlatformVersion platform_version : apiWrapper.getAllPlatformVersionsList()){
                    System.out.println(String.format("%-37s %-30s",
                            platform_version.getIdentifierString(),
                            platform_version.getDisplayString()));
                }
            }else {
                if (!(boolean)optMap.get("quiet")) {
                    System.out.println("Platform");
                    System.out.println("------------------------------------------------------------------------");
                }
                for (PlatformVersion platform_version : apiWrapper.getAllPlatformVersionsList()){
                    System.out.println(platform_version.getDisplayString());
                }
            }
        }else {

            List<String> platform_names = new ArrayList<String>();
            platform_names.add((String)optMap.get("platform"));
            List<PlatformVersion> platform_versions = getPlatformVersions(platform_names, null);
            System.out.println(platform_versions.get(0).getUUIDString());
        }
    }

    public void toolHandler(HashMap<String, Object> optMap) {

        if (optMap.get("sub-command").equals("list")) {
            printTools((String)optMap.get("project"), 
                    (boolean)optMap.get("quiet"),
                    (boolean)optMap.get("verbose"));
        }else {
            List<String> tool_names = new ArrayList<String>();
            tool_names.add((String)optMap.get("tool"));
            List<Tool> tools = getTools(tool_names, (String)optMap.get("project"), null);
            List<ToolVersion> tool_versions = apiWrapper.getToolVersions(tools.get(0));
            System.out.println(tool_versions.get(0).getUUIDString());
        }
    }

    protected void deletePackages(String project, List<String> packages, boolean deleteAll, boolean quiet) {

        if (project != null && !Cli.isUuid(project)) {
            project = apiWrapper.getProjectFromName(project).getUUIDString();
        }

        if (packages == null && deleteAll) {
            //delete all packages
            for (PackageVersion pkg_ver : apiWrapper.getPackageVersionsList(project)) {
                if (apiWrapper.deletePackageVersion(pkg_ver) && !quiet) {
                    System.out.println(String.format("Deleted 'Name: %s, Version: %s'", 
                            pkg_ver.getPackageThing().getName(), 
                            pkg_ver.getVersionString()));
                }
            }
        }else {
            List<PackageVersion>all_packages = apiWrapper.getPackageVersionsList(project);

            Set<PackageVersion>for_deletion = new HashSet<PackageVersion>();

            for (String pkg : packages) {

                if (Cli.isUuid(pkg)) {
                    for (PackageVersion pkg_ver : all_packages) {
                        if (pkg_ver.getIdentifierString().equals(pkg)) {
                            for_deletion.add(pkg_ver);
                            break;
                        }
                    }
                }else {
                    
                    //delete all versions
                    if (!pkg.contains(VERSION_SEPERATOR)) {
                        String pkg_thing = pkg;
                        for (PackageVersion pkg_ver : all_packages) {
                            if (pkg_ver.getPackageThing().getName().equals(pkg)) {
                                for_deletion.add(pkg_ver);
                            }
                        }
                    }else {
                        String[] name_version = getNameAndVersion(pkg);
                        for (PackageVersion pkg_ver : all_packages) {
                            if (pkg_ver.getPackageThing().getName().equals(name_version[0]) &&
                                    pkg_ver.getVersionString().equals(name_version[1])) {
                                for_deletion.add(pkg_ver);
                            }
                        }
                    }
                }			
            }

            for (PackageVersion pkg_ver : for_deletion) {

                if (apiWrapper.deletePackageVersion(pkg_ver) && !quiet) {
                    System.out.println(String.format("Deleted 'Name: %s, Version: %s'", 
                            pkg_ver.getPackageThing().getName(), 
                            pkg_ver.getVersionString()));

                }
            }
        }
    }

    protected void uploadPackage(String pkgConf, String pkgArchive,
            String project, Map<String, String> osDeps, boolean newPkg, boolean quiet) {
        String package_uuid = null;

        if (osDeps != null) {
            Set<String> unknown_platforms = osDeps.keySet();
            for (String platform: osDeps.keySet()) {
                for (PlatformVersion platform_version : apiWrapper.getAllPlatformVersionsList()) {
                    if (platform_version.getDisplayString().equalsIgnoreCase(platform)) {
                        unknown_platforms.remove(platform);
                    }
                }

                if (!unknown_platforms.isEmpty()) {
                    throw new CommandLineOptionException("Platform " + unknown_platforms + " do not exist");
                }
            }
        }

        String project_uuid = null;
        if (project != null) {
            if (!Cli.isUuid(project)) {
                project = apiWrapper.getProjectFromName(project).getUUIDString();
            }
        }else {
            project = (String)apiWrapper.getProjectFromName("MyProject").getUUIDString();
        }

        package_uuid = apiWrapper.uploadPackage(pkgConf, pkgArchive, project, osDeps, newPkg);

        if (!quiet){
            System.out.println("Package Version UUID");
            System.out.println("------------------------------------------------------------------------");
        }

        System.out.println(package_uuid);
    }

    public void packageHandler(HashMap<String, Object> optMap) {

        String sub_command = (String)optMap.get("sub-command");

        if (sub_command.equalsIgnoreCase("list")) {

            printAllPackages((String)optMap.get("project"),
                    (String)optMap.get("pkg-type"),
                    (boolean)optMap.get("quiet"),
                    (boolean)optMap.get("verbose"));

        }else if (sub_command.equalsIgnoreCase("types")) {
            for (String pkg_type : getPackageTypes()) {
                System.out.println(pkg_type);
            }
        }else if (sub_command.equalsIgnoreCase("delete")) {
            deletePackages((String)optMap.get("project"),
                    (List<String>)optMap.get("package"),
                    (boolean)optMap.get("delete-all"),
                    (boolean)optMap.get("quiet"));
        }else {
            uploadPackage((String)optMap.get("pkg-conf"),
                    (String)optMap.get("pkg-archive"),
                    (String)optMap.get("project"),
                    (Map<String, String>)optMap.get("os-deps-map"),
                    (boolean)optMap.get("new-pkg"),
                    (boolean)optMap.get("quiet"));
        }
    }

    public List<String> getPackageTypes() {
        List<String> pkg_types = apiWrapper.getPackageTypesList();

        Collections.sort(pkg_types, new Comparator<String>() {
            public int compare(String i1, String i2) {
                return (i1.compareTo(i2));
            }
        });

        return pkg_types;
    }

    public void printAllPackages(String project, String pkgType, boolean quiet, boolean verbose) {

        if (pkgType != null) {
            if (!getPackageTypes().contains(pkgType)) {
                throw new InvalidNameException("Package type '" + pkgType + "' not valid");
            }
        }

        if (project != null) {
            if (!Cli.isUuid(project)) {
                project = apiWrapper.getProjectFromName(project).getUUIDString();	
            }
        }

        if(quiet){
            for(PackageThing pkg : apiWrapper.getPackagesList(project)) {
                if (pkgType == null ||
                        pkg.getType().equalsIgnoreCase(pkgType)) {

                    for(PackageVersion pkg_ver : apiWrapper.getPackageVersions(pkg)) {
                        System.out.println(String.format("%-25s %-25s",
                                pkg_ver.getPackageThing().getName(),
                                pkg_ver.getVersionString()));
                    }
                }
            }
        }else if(verbose) {
            System.out.println(String.format("%-37s %-25s %-40s %-25s %-25s",
                    "UUID", "Package", "Description","Type", "Version"));
            System.out.println("------------------------------------------------------------------------");
            
            for(PackageThing pkg : apiWrapper.getPackagesList(project)) {
                if (pkgType == null ||
                        pkg.getType().equalsIgnoreCase(pkgType)) {

                    for(PackageVersion pkg_ver : apiWrapper.getPackageVersions(pkg)) {
                        System.out.println(String.format("%-37s %-25s %-40s %-25s %-25s", 
                                pkg_ver.getUUIDString(),
                                pkg_ver.getPackageThing().getName(),
                                pkg_ver.getPackageThing().getDescription(),
                                pkg_ver.getPackageThing().getType(),
                                pkg_ver.getVersionString()));
                    }
                }
            }
        }else {
            System.out.println(String.format("%-25s %-40s %-25s %-25s",
                    "Package", "Description","Type", "Version"));
            System.out.println("------------------------------------------------------------------------");
            
            for(PackageThing pkg : apiWrapper.getPackagesList(project)) {
                if (pkgType == null ||
                        pkg.getType().equalsIgnoreCase(pkgType)) {

                    for(PackageVersion pkg_ver : apiWrapper.getPackageVersions(pkg)) {
                        System.out.println(String.format("%-25s %-40s %-25s %-25s",
                                pkg_ver.getPackageThing().getName(),
                                pkg_ver.getPackageThing().getDescription(),
                                pkg_ver.getPackageThing().getType(),
                                pkg_ver.getVersionString()));
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

    public Project getProject(String projectName)  {
        List<Project> projects = new ArrayList<Project>();

        if (projectName != null) {
            if (Cli.isUuid(projectName)) {
                for (Project proj : apiWrapper.getProjectsList()) {
                    if (proj.getUUIDString().equals(projectName)) {
                        projects.add(proj);
                        break;
                    }
                }
            }else {
                for (Project proj : apiWrapper.getProjectsList()) {
                    if (proj.getFullName().equals(projectName)) {
                        projects.add(proj);
                    }
                } 
            }
        }

        if (projects.isEmpty()) {
            if (projectName == null) {
                throw new InvalidNameException("Project name/UUID cannot be " + projectName);
            }else if (Cli.isUuid(projectName)) {
                throw new InvalidIdentifierException("No Project found with UUID: " + projectName);
            }else {
                throw new InvalidNameException("No Project found with name: " + projectName);
            }
        }else if (projects.size() > 1) {
            throw new ConflictingNamesException("More than one project has the same name, retrive using UUID");
        }

        return projects.get(0);
    }

    public PackageThing getPackage(String packageName, Project project) {

        List<PackageThing> packages = new ArrayList<PackageThing>();

        if (Cli.isUuid(packageName)) {
            for (PackageThing pkg_thing : apiWrapper.getPackagesList(project.getIdentifierString())) {
                if (pkg_thing.getUUIDString().equals(packageName)) {
                    packages.add(pkg_thing);
                    break;
                }
            }
        }else {

            for (PackageThing pkg_thing : apiWrapper.getPackagesList(project.getIdentifierString())) {
                if (pkg_thing.getName().equals(packageName)) {
                    packages.add(pkg_thing);
                }
            }
        }

        if (packages.isEmpty()) {
            if (packageName == null) {
                throw new InvalidNameException("Package name/UUID cannot be " + packageName);
            }else if (Cli.isUuid(packageName)) {
                throw new InvalidIdentifierException("No Package found with UUID: " + packageName);
            }else {
                throw new InvalidNameException("No Package found with name: " + packageName);
            }
        }else if (packages.size() > 1) {
            throw new ConflictingNamesException("More than one package has the same name, retrive using UUID");
        }

        return packages.get(0);
    }

    public PackageVersion getPackageVersion(String packageVersion, 
            PackageThing packageThing, Project project) {
        List<PackageVersion> target_pkg_vers = new ArrayList<PackageVersion>();

        List<PackageVersion> all_pkg_vers = apiWrapper.getPackageVersions(packageThing);

        if (packageVersion == null) {
            //Return the latest version
            Collections.sort(all_pkg_vers, new Comparator<PackageVersion>() {
                public int compare(PackageVersion i1, PackageVersion i2) {
                    return (i1.getVersionString().compareTo(i2.getVersionString()));
                }
            });
            target_pkg_vers.add(all_pkg_vers.get(all_pkg_vers.size() - 1));
        }else {
            for (PackageVersion pkg_ver : all_pkg_vers) {
                if (pkg_ver.getVersionString().equals(packageVersion)) {
                    target_pkg_vers.add(pkg_ver);
                }
            }
        }

        if (target_pkg_vers.isEmpty()) {
            if (Cli.isUuid(packageVersion)) {
                throw new InvalidIdentifierException("No Package Version found with UUID: " + packageVersion);
            }else {
                throw new InvalidNameException("No Package Version found with name: " + packageVersion);
            }
        }else if (target_pkg_vers.size() > 1) {
            throw new ConflictingNamesException("More than one package versions has the same name, retrive using UUID");
        }

        return target_pkg_vers.get(0);
    }

    public PackageVersion getPackageVersion(String packageName, 
            String packageVersionName, String projectName) {
        Project project = getProject(projectName);
        PackageThing package_thing = getPackage(packageName, project);
        return getPackageVersion(packageVersionName, package_thing, project);   
    }

    public List<PlatformVersion> getPlatformVersions(List<String> platformVersionNames, 
            String packageType) {

        List<PlatformVersion> valid_platforms = new ArrayList<PlatformVersion>();

        for (PlatformVersion plat_ver: apiWrapper.getAllPlatformVersionsList()) {
            for (String plat_name : platformVersionNames) {
                if (Cli.isUuid(plat_name) && plat_name.equals(plat_ver.getUUIDString())) {
                    if (packageType != null) {
                        if (packageType.equalsIgnoreCase("C/C++")) {
                            valid_platforms.add(plat_ver);
                        }
                    }else {
                        valid_platforms.add(plat_ver);
                    }
                }else if (plat_name.equals(plat_ver.getDisplayString())) {
                    if (packageType != null) {
                        if (packageType.equalsIgnoreCase("C/C++")) {
                            valid_platforms.add(plat_ver);
                        }
                    }else {
                        valid_platforms.add(plat_ver);
                    }                        
                }
            }
        }

        //Use default platform
        if (valid_platforms.isEmpty()) {
            valid_platforms.add(apiWrapper.getDefaultPlatformVersion(packageType));
        }

        return valid_platforms;
    }

    public List<Tool> getTools(List<String> toolNames, String projectName, String packageType) {

        List<Tool> valid_tools = new ArrayList<Tool>();
        String project_uuid = null;
        if (projectName != null) {
            project_uuid = getProject(projectName).getUUIDString();
        }

        for (Tool tool : apiWrapper.getAllTools(project_uuid).values()) {
            for (String tool_name: toolNames) {
                if (tool.getName().equalsIgnoreCase(tool_name)) {

                    if (packageType != null) {
                        if(tool.getSupportedPkgTypes().contains(packageType)) {
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

        for (Tool tool : apiWrapper.getAllTools(null).values()) {
            for (ToolVersion tool_version : apiWrapper.getToolVersions(tool)) {
                if (tool_version.getUUIDString().equalsIgnoreCase(tool_version_uuid)) {
                    return tool_version;
                }
            }
        }
        throw new InvalidIdentifierException("No Tool Version found with UUID: " + tool_version_uuid); 
    }

    public ToolVersion getToolVersion(String toolName, String toolVersionNum) {

        if (toolVersionNum != null) {

            if (Cli.isUuid(toolName)) {
                String tool_version_uuid = toolName;

                for (Tool tool : apiWrapper.getAllTools(null).values()) {
                    for (ToolVersion tool_version : apiWrapper.getToolVersions(tool)) {
                        if (tool_version.getUUIDString().equalsIgnoreCase(tool_version_uuid)) {
                            return tool_version;
                        }
                    }
                }
            }else {
                for (Tool tool : apiWrapper.getAllTools(null).values()) {
                    if (tool.getName().equalsIgnoreCase(toolName)) {
                        for (ToolVersion tool_version : apiWrapper.getToolVersions(tool)) {
                            if (tool_version.getVersion().equalsIgnoreCase(toolVersionNum)) {
                                return tool_version;
                            }
                        }
                    }
                }
            }
        }else {
            if (Cli.isUuid(toolName)) {
                String tool_version_uuid = toolName;

                for (Tool tool : apiWrapper.getAllTools(null).values()) {
                    for (ToolVersion tool_version : apiWrapper.getToolVersions(tool)) {
                        if (tool_version.getUUIDString().equalsIgnoreCase(tool_version_uuid)) {
                            return tool_version;
                        }
                    }
                }
            }else {
                for (Tool tool : apiWrapper.getAllTools(null).values()) {
                    if (tool.getName().equalsIgnoreCase(toolName)) {
                        return apiWrapper.getToolVersions(tool).get(0);
                    }
                }
            }
        }

        if (Cli.isUuid(toolName)) {
            throw new InvalidIdentifierException("No Tool Version found with UUID: " + toolName); 
        }else {
            throw new InvalidNameException("No Tool Version found with name: " + toolName + " version: " + toolVersionNum); 
        }
    }

    public List<ToolVersion> getToolVersions(List<String> toolNames) {

        List<ToolVersion> valid_tools = new ArrayList<ToolVersion>();

        for (String tool_name : toolNames) {
            String tool_version = null;

            if (tool_name.contains(VERSION_SEPERATOR)) {
                String name_version[] = getNameAndVersion(tool_name);
                tool_name = name_version[0];
                tool_version =  name_version[1];
            }

            valid_tools.add(getToolVersion(tool_name, tool_version));
        }


        return valid_tools;
    }

    public void runAssessments(String packageName,
            List<String> toolNames,
            List<String> platforms,
            boolean quiet) {

        Project target_project = null;
        PackageVersion target_pkg = null;
        String package_version = null;

        if (packageName.contains(VERSION_SEPERATOR)) {
            String name_version[] = getNameAndVersion(packageName);
            packageName = name_version[0];
            package_version = name_version[1];
        }

        for (Project project : apiWrapper.getProjectsList()) {
            if (!Cli.isUuid(packageName)) {
                for (PackageThing pkg_thing : apiWrapper.getPackagesList(project.getIdentifierString())) {
                    if (pkg_thing.getName().equals(packageName)) {
                        List<PackageVersion> pkg_vers = apiWrapper.getPackageVersions(pkg_thing);

                        if (package_version == null) {
                            Collections.sort(pkg_vers, new Comparator<PackageVersion>() {
                                public int compare(PackageVersion i1, PackageVersion i2) {
                                    return (i1.getVersionString().compareTo(i2.getVersionString()));
                                }
                            });
                            target_pkg = pkg_vers.get(pkg_vers.size() - 1);
                        }else {
                            for (PackageVersion pkg_ver : pkg_vers) {
                                if (pkg_ver.getVersionString().equals(package_version)) {
                                    target_pkg = pkg_ver;
                                    break;
                                }
                            }
                        }
                        
                        if (target_pkg != null) {
                            target_project = project;
                            break;
                        }
                    }
                }
                
                 
                
            }else {
                for (PackageVersion pkg_ver : apiWrapper.getPackageVersionsList(project.getIdentifierString())) {
                    if (pkg_ver.getIdentifierString().equals(packageName)) {
                        target_pkg = pkg_ver;
                        target_project = project;
                        break;
                    }
                }

                if (target_pkg  == null) {
                    for (PackageThing pkg_thing : apiWrapper.getPackagesList(project.getIdentifierString())) {
                        if (pkg_thing.getIdentifierString().equals(packageName)) {
                            List<PackageVersion> pkg_vers = apiWrapper.getPackageVersions(pkg_thing);
                            Collections.sort(pkg_vers, new Comparator<PackageVersion>() {
                                public int compare(PackageVersion i1, PackageVersion i2) {
                                    return (i1.getVersionString().compareTo(i2.getVersionString()));
                                }
                            });

                            if (package_version == null) {
                                target_pkg = pkg_vers.get(pkg_vers.size()-1);
                                target_project = project;
                                break;
                            }else {
                                for (PackageVersion pkg_ver : pkg_vers) {
                                    if (pkg_ver.getVersionString().equals(package_version)) {
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
            if (Cli.isUuid(packageName)) {
                throw new InvalidIdentifierException("Invalid Package UUID: " + packageName);
            }else {
                if (package_version != null) {
                    throw new InvalidNameException(String.format("package: %s-%s not found\n", packageName, package_version));
                }else {
                    throw new InvalidNameException(String.format("package: %s not found\n", packageName));
                }
            }
        }

        List<PlatformVersion> valid_platforms = new ArrayList<PlatformVersion>();
        if (platforms != null && target_pkg.getPackageThing().getType().equalsIgnoreCase("C/C++")) {
            for (PlatformVersion plat_ver: apiWrapper.getAllPlatformVersionsList()) {
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
        if (valid_platforms.isEmpty()) {
            valid_platforms.add(apiWrapper.getDefaultPlatformVersion(target_pkg.getPackageThing().getType()));
        }

        List<AssessmentRun> assessment_run = new ArrayList<AssessmentRun>();

        for (ToolVersion tool_version : getToolVersions(toolNames)) {
            assessment_run.addAll(apiWrapper.runAssessment(target_pkg, tool_version, target_project, valid_platforms));
        }

        if (assessment_run.size() > 0) {
            if (!quiet) {
                System.out.println("Assessment UUID");
                System.out.println("------------------------------------------------------------------------");
            }
            for (AssessmentRun arun : assessment_run) {
                System.out.println(arun.getUUIDString()); 
            }
        }else {
            throw new IncompatibleAssessmentTupleException("Could not create assessment run");
        }
    }

    public void listAssessments(String projectName, 
            String packageName, 
            String toolName,
            String platName,
            boolean quiet, 
            boolean verbose) {

        List<AssessmentRun> all_assessments = new ArrayList<AssessmentRun>();

        if (projectName != null) {
            all_assessments.addAll(apiWrapper.getAllAssessments(apiWrapper.getProjectFromName(projectName).getIdentifierString()));
        }else {
            for (Project project : apiWrapper.getProjectsList()) {
                all_assessments.addAll(apiWrapper.getAllAssessments(project.getIdentifierString()));
            }
        }

        if (verbose) {
            System.out.println(String.format("%-37s %-40s %-30s %-20s",
                    "UUID", "Package", "Tool","Platform"));
            System.out.println("------------------------------------------------------------------------");
        }else if (!quiet) {
            System.out.println(String.format("%-40s %-30s %-20s",
                    "Package", "Tool","Platform"));
            System.out.println("------------------------------------------------------------------------");
        }		

        for (AssessmentRun arun : all_assessments) {
            if (packageName != null && !arun.getPackageName().equals(packageName)) {
                continue;
            }

            if (toolName != null && !arun.getToolName().equals(toolName)) {
                continue;
            }

            if (platName != null && !arun.getPlatformName().equals(platName)) {
                continue;
            }

            if (verbose) {
                System.out.println(String.format("%-37s %-40s %-30s %-20s",
                        arun.getIdentifierString(),
                        arun.getPackageName() + VERSION_SEPERATOR + arun.getPackageVersion(),
                        arun.getToolName() + VERSION_SEPERATOR + arun.getToolVersion(),
                        PlatformVersion.getDisplayString(arun.getPlatformName(),
                                arun.getPlatformVersion())));
            }else {
                System.out.println(String.format("%-40s %-30s %-20s",
                        arun.getPackageName() + VERSION_SEPERATOR + arun.getPackageVersion(),
                        arun.getToolName() + VERSION_SEPERATOR + arun.getToolVersion(),
                        PlatformVersion.getDisplayString(arun.getPlatformName(),
                                arun.getPlatformVersion())));
            }
        }
    }

    public void assessmentHandler(HashMap<String, Object> optMap) {

        if (((String)optMap.get("sub-command")).equalsIgnoreCase("run")) {

            runAssessments((String)optMap.get("package"),
                    (List<String>)optMap.get("tool"), 
                    (List<String>)optMap.get("platform"),
                    (boolean)optMap.get("quiet"));

        }else if (((String)optMap.get("sub-command")).equalsIgnoreCase("list")) {
            listAssessments((String)optMap.get("project"), 
                    (String)optMap.get("package"), 
                    (String)optMap.get("tool"),
                    (String)optMap.get("platform"),
                    (boolean)optMap.get("quiet"),
                    (boolean)optMap.get("verbose"));
        }

    }

    public void resultsHandler(HashMap<String, Object> optMap) throws IOException{
        if (optMap.get("sub-command").equals("download")) {
            if ((String)optMap.get("results-uuid") != null) {
                downloadScarf((String)optMap.get("project-uuid"), 
                        (String)optMap.get("results-uuid"),
                        (String)optMap.get("filepath"),
                        (boolean)optMap.get("quiet"));
            }else {
                downloadScarf((String)optMap.get("package"), 
                        (String)optMap.get("tool"),
                        (String)optMap.get("platform"),
                        (String)optMap.get("filepath"),
                        (boolean)optMap.get("quiet"));
            }
        }else {
            listResults((String)optMap.get("project"), 
                    (String)optMap.get("package"), 
                    (String)optMap.get("tool"),
                    (String)optMap.get("platform"),
                    (boolean)optMap.get("verbose"),
                    (boolean)optMap.get("quiet"));
        }
    }

    public void downloadScarf(String projectUuid, 
            String asssessResultUuid, 
            String filepath, 
            boolean quiet) throws IOException {

        if (filepath == null) {
            filepath = "./" + asssessResultUuid + ".xml";
        }

        boolean status = false;
        if (projectUuid != null) {
            status =  apiWrapper.getAssessmentResults(projectUuid, asssessResultUuid, filepath); 
        }else {
            status =  apiWrapper.getAssessmentResults(asssessResultUuid, filepath);
        }

        if (!quiet) {
            if (status) {
                System.out.println("Downloaded SCARF into: " + filepath);
            }else {
                System.out.println("Downloaded SCARF for " + asssessResultUuid + " failed" );
            }
        }
    }

    public void downloadScarf(String packageName, 
            String toolName,
            String platform, 
            String filepath, 
            boolean quiet) throws IOException {

        List<AssessmentRecord> results = new ArrayList<AssessmentRecord>();

        String package_version = null;
        if (packageName.contains(VERSION_SEPERATOR)) {
            String name_version[] = getNameAndVersion(packageName);
            packageName = name_version[0];
            package_version = name_version[1];
        }
        
        String tool_version = null;
        if (toolName.contains(VERSION_SEPERATOR)) {
            String name_version[] = getNameAndVersion(toolName);
            toolName = name_version[0];
            tool_version =  name_version[1];
        }

        for (Project project : apiWrapper.getProjectsList()) {
            for (AssessmentRecord arecord : apiWrapper.getAllAssessmentRecords(project.getUUIDString())) {                

                if (packageName != null && !packageName.equalsIgnoreCase(arecord.getConversionMap().getString("package_name"))) {
                    continue;
                }

                if (package_version != null && !package_version.equalsIgnoreCase(arecord.getConversionMap().getString("package_version"))) {
                    continue;
                }

                if (toolName != null &&  !toolName.equalsIgnoreCase(arecord.getConversionMap().getString("tool_name"))) {
                    continue;
                }

                if(tool_version != null && !tool_version.equalsIgnoreCase(arecord.getConversionMap().getString("tool_version"))) {
                    continue;
                }

                if(platform != null && !platform.equalsIgnoreCase(PlatformVersion.getDisplayString(arecord.getConversionMap().getString("platform_name"),
                        arecord.getConversionMap().getString("platform_version")))) {
                    continue;
                }
                results.add(arecord);
            }
        }

        if (results.isEmpty()) {
            throw new ConflictingNamesException(String.format("No assessment records found with " +
                    "package_name: %s, package_version: %s , tool_name: %s, tool_version: %s, platform: %s\n", 
                    packageName, package_version,
                    toolName, tool_version,
                    platform));           
        }else if (results.size() > 1) {
            throw new ConflictingNamesException("More than one assessment records have the same " +
                    "(package_name, package_version, tool_name, tool_version, platform)\n, "
                    + "Use Assessment result UUID to download results");
        }

        if (filepath == null) {
            filepath = "./" + results.get(0).getAssessmentResultUUID() + ".xml";
        }

        boolean status = apiWrapper.getAssessmentResults(results.get(0).getProjectUUID(), 
                results.get(0).getAssessmentResultUUID(), 
                filepath); 


        if (!quiet) {
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

            //SimpleDateFormat date_format = new SimpleDateFormat("MM/dd/yyyy HH:mm");
            SimpleDateFormat date_format = new SimpleDateFormat("yyyy/MM/dd'T'HH:mm");
            date_format.setTimeZone(Calendar.getInstance().getTimeZone());
            converted_date =  date_format.format(calendar.getTime());
        }catch (Exception e){ 
            LOGGER.error(e.getMessage(), e);
            converted_date = date.toString();
        }

        return converted_date;
    }

    public void listResults(String projectName, 
            String packageName, 
            String toolName,
            String platName,
            boolean verbose,
            boolean quiet) {

        List<AssessmentRecord> all_results = new ArrayList<AssessmentRecord>();

        if (projectName != null) {
            all_results.addAll(apiWrapper.getAllAssessmentRecords(getProject(projectName).getUUIDString()));
        }else {
            for (Project project : apiWrapper.getProjectsList()) {
                all_results.addAll(apiWrapper.getAllAssessmentRecords(project.getUUIDString()));
            }
        }

        Collections.sort(all_results, new Comparator<AssessmentRecord>() {
            public int compare(AssessmentRecord i1, AssessmentRecord i2) {
                return (i2.getConversionMap().getDate("create_date").compareTo(i1.getConversionMap().getDate("create_date")));
            }
        });

        String package_version = null;
        if (packageName != null && packageName.contains(VERSION_SEPERATOR)) {
            String name_version[] = getNameAndVersion(packageName);
            packageName = name_version[0];
            package_version = name_version[1];
        }
        
        String tool_version = null;
        if (toolName != null && toolName.contains(VERSION_SEPERATOR)) {
            String name_version[] = getNameAndVersion(toolName);
            toolName = name_version[0];
            tool_version =  name_version[1];
        }
        
        if (verbose) {
            System.out.println(String.format("%-37s %-40s %-30s %-20s %-20s %-20s %10s",
                    "Assessment Result UUID", "Package", "Tool","Platform", "Date", "Status", "Results"));
            System.out.println("------------------------------------------------------------------------");
        }else if (!quiet) {
            System.out.println(String.format("%-40s %-30s %-20s %-20s %-20s %10s",
                    "Package", "Tool", "Platform", "Date", "Status", "Results"));
            System.out.println("------------------------------------------------------------------------");
        }

        for (AssessmentRecord arun : all_results) {
            if (packageName != null && !arun.getConversionMap().getString("package_name").equals(packageName)) {
                continue;
            }

            if (package_version != null && !arun.getConversionMap().getString("package_version").equals(package_version)) {
                continue;
            }
            
            if (toolName != null && !arun.getConversionMap().getString("tool_name").equals(toolName)) {
                continue;
            }

            if (tool_version != null && !arun.getConversionMap().getString("tool_version").equals(tool_version)) {
                continue;
            }
            
            if (platName != null && !PlatformVersion.getDisplayString(arun.getConversionMap().getString("platform_name"), 
                    arun.getConversionMap().getString("platform_version")).equals(platName)) {
                continue;
            }

            if (verbose) {
                System.out.println(String.format("%-37s %-40s %-30s %-20s %-20s %-20s %10s",
                        arun.getAssessmentResultUUID(),
                        arun.getConversionMap().getString("package_name") + VERSION_SEPERATOR + arun.getConversionMap().getString("package_version"),
                        arun.getConversionMap().getString("tool_name") + VERSION_SEPERATOR + arun.getConversionMap().getString("tool_version"),
                        PlatformVersion.getDisplayString(arun.getConversionMap().getString("platform_name"), arun.getConversionMap().getString("platform_version")),
                        toCurrentTimeZone(arun.getConversionMap().getDate("create_date")),
                        arun.getConversionMap().getString(SC_STATUS),
                        arun.getWeaknessCount()));    
            }else {
                System.out.println(String.format("%-40s %-30s %-20s %-20s %-20s %10s",
                        arun.getConversionMap().getString("package_name") + VERSION_SEPERATOR + arun.getConversionMap().getString("package_version"),
                        arun.getConversionMap().getString("tool_name") + VERSION_SEPERATOR + arun.getConversionMap().getString("tool_version"),
                        PlatformVersion.getDisplayString(arun.getConversionMap().getString("platform_name"), arun.getConversionMap().getString("platform_version")),
                        toCurrentTimeZone(arun.getConversionMap().getDate("create_date")),
                        arun.getConversionMap().getString(SC_STATUS),
                        arun.getWeaknessCount())); 
            }
        }
    }

    public void statusHandler(HashMap<String, Object> optMap) {
        printAssessmentStatus((String)optMap.get("project-uuid"),
                (String)optMap.get("assess-uuid"),
                (boolean)optMap.get("quiet"));
    }

    public void printUserInfo(HashMap<String, Object> optMap) {
        User user = apiWrapper.getUserInfo();
        System.out.println(String.format("%s", "User:\t" + user.getFirstName() + " " + user.getLastName()));
        System.out.println(String.format("%s", "Email:\t" + user.getEmail()));
        System.out.println(String.format("%s", "UUID:\t" + user.getUUIDString()));
    }

    public void logoutHandler(HashMap<String, Object> optMap) {
        apiWrapper.logout();
        if ((boolean)optMap.get("quiet") == false) {
            System.out.println("Logout successful");
        }
    }

    public int executeCommands(String command, HashMap<String, Object> optMap) throws IOException, SessionExpiredException, InvalidIdentifierException, IncompatibleAssessmentTupleException {

        if (command.equals(SC_LOGIN)) {
            loginHandler(optMap);
        }else {
            apiWrapper.restoreSession();
            switch (command) {
            case "project":
            case SC_PROJECTS:
                projectHandler(optMap);
                break;
            case "platform":
            case SC_PLATFORMS:
                platformHandler(optMap);
                break;
            case "tool":    
            case SC_TOOLS:
                toolHandler(optMap);
                break;
            case "package":    
            case SC_PACKAGES:
                packageHandler(optMap);
                break;
            case "assess":
            case "assessments":
                assessmentHandler(optMap);
                break;
            case SC_RESULTS:
                resultsHandler(optMap);
                break;
            case SC_STATUS:
                statusHandler(optMap);
                break;
            case "user":
                printUserInfo(optMap);
                break;
            case SC_LOGOUT:
                logoutHandler(optMap);
                break;
            default:
                break;
            }
        }
        return 0;
    }


    public void printTools(String projectName, boolean quiet, boolean verbose) throws InvalidIdentifierException {

        Project project = getProject(projectName);
            
        if(verbose){
            System.out.println(String.format("%-37s %-21s %15s %-40s",
                    "UUID",
                    "Tool",
                    "Version",
                    "Supported Package Types"));
            System.out.println("------------------------------------------------------------------------");
            
            for(Tool tool : apiWrapper.getAllTools(project.getUUIDString()).values()) {
                for (ToolVersion tool_version : apiWrapper.getToolVersions(tool)) {
                    System.out.println(String.format("%-37s %-21s %15s %-40s",
                            tool_version.getUUIDString(),
                            tool_version.getTool().getName(),
                            tool_version.getVersion(),
                            tool.getSupportedPkgTypes()));
                }
            }
        }else if(!quiet) {
            System.out.println(String.format("%-21s %15s %-40s",
                    "Tool",
                    "Version",
                    "Supported Package Types"));
            System.out.println("------------------------------------------------------------------------");
            
            for(Tool tool : apiWrapper.getAllTools(project.getUUIDString()).values()) {
                for (ToolVersion tool_version : apiWrapper.getToolVersions(tool)) {
                    System.out.println(String.format("%-21s %15s %-40s",
                            tool_version.getTool().getName(),
                            tool_version.getVersion(),
                            tool.getSupportedPkgTypes()));
                }
            }
        }else {
            for(Tool tool : apiWrapper.getAllTools(project.getUUIDString()).values()) {
                System.out.println(tool.getName());
            }
        }
    }

    public void printAssessments(String projectUuid, boolean quiet) {

        if (!quiet){
            System.out.println(String.format("%-37s %-15s %-15s %-15s %-15s %-15s %-15s", 
                    "UUID",
                    "Package Name", "Package Version",
                    "Tool Name", "Tool Version",
                    "Platform Name", "Platform Version"));
            System.out.println("------------------------------------------------------------------------");
            
            for (AssessmentRun arun : apiWrapper.getAllAssessments(projectUuid)){

                System.out.println(String.format("%-37s %-15s %-15s %-15s %-15s %-15s %-15s", 
                        arun.getUUIDString(), 
                        arun.getPackageName(), 
                        arun.getPackageVersion(),
                        arun.getToolName(),
                        arun.getToolVersion(),
                        arun.getPlatformName(),
                        arun.getPlatformVersion()));
            }
        }else{
            for (AssessmentRun arun : apiWrapper.getAllAssessments(projectUuid)){
                System.out.println(arun.getUUIDString());
            }
        }
    }

    public void printAssessment(String assessmentUuid, String projectUuid) {
        AssessmentRun arun = apiWrapper.getAssessment(assessmentUuid, projectUuid);
        if (arun == null){
            System.out.println("Assessment " + assessmentUuid + " not found. Please verify the UUID");
        }else{
            System.out.println("Assessment Results on " + arun.getIdentifierString());
            System.out.println("Package: \t" + (arun.getPkg() == null ? "N/A" : arun.getPkg().getName()));
            System.out.println("Project: \t" + (arun.getProject() == null ? "N/A" : arun.getProject().getFullName()));
            System.out.println("Tool:    \t" + (arun.getTool() == null ? "N/A" : arun.getTool().getName()));
            //TODO: this must be platform version
            System.out.println("Platform:\t" + (arun.getPlatform() == null ? "N/A" : arun.getPlatform().getName()));
        }
    }

    public void printAssessmentStatus(String projectUuid, String assessmentUuid, boolean quiet) {

        AssessmentRecord assessment_record = null;

        if (projectUuid != null) {
            assessment_record = apiWrapper.getAssessmentRecord(projectUuid, assessmentUuid);
        }else {
            for (Project project : apiWrapper.getProjectsList()) {
                for (AssessmentRecord record : apiWrapper.getAllAssessmentRecords(project.getUUIDString())) {

                    if (record.getAssessmentRunUUID().equals(assessmentUuid)) {
                        assessment_record = record;
                        break;
                    }
                }
            }
        }

        if (assessment_record != null) {
            if (!quiet) {
                System.out.println(String.format("%-15s %-15s %-37s", 
                        "Status", "Weakness", "Assessments Result UUID"));
                System.out.println("------------------------------------------------------------------------");
            }
            
            System.out.println(String.format("%-15s %-15d %-37s", 
                    AssessmentStatus.translateAssessmentStatus(assessment_record.getStatus()),
                    //assessment_record.getStatus(),
                    assessment_record.getWeaknessCount(),
                    assessment_record.getAssessmentResultUUID() != null ? assessment_record.getAssessmentResultUUID() : ""));
                
        }else {
            throw new InvalidIdentifierException("Invalid Assessment UUID: " + assessmentUuid);  
        }
    }

    public static void main(String[] args) throws Exception {

        //org.apache.log4j.BasicConfigurator.configure(new NullAppender());

        if (args.length == 1 && (args[0].equals("-V") || args[0].equals("-v") ||
                args[0].equals("--version"))){
            Cli.printVersion();
            return;
        }else if (args.length == 0 || args[0].equals("-H") || args[0].equals("-h") ||
                args[0].equals("--help") || !Cli.COMMANDS.contains(args[0])) {
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
            LOGGER.error(e.getMessage());
            System.exit(e.getExitCode());
        }catch(ParseException e){
            LOGGER.error(e.getMessage());
            System.exit(SwampApiWrapperExitCodes.CLI_PARSER_ERROR.getExitCode());
        }catch (HTTPException e) {
            LOGGER.error(e.getMessage());
            System.exit(SwampApiWrapperExitCodes.NormalizeHttpExitCode(((HTTPException)e).getStatusCode()));
        }catch(GeneralException e){
            LOGGER.error(e.getMessage(), e);
            System.exit(SwampApiWrapperExitCodes.HTTP_GENERAL_EXCEPTION.getExitCode());
        }catch(IllegalStateException e){
            LOGGER.error(e.getMessage());
            System.exit(SwampApiWrapperExitCodes.HTTP_GENERAL_EXCEPTION.getExitCode());
        }

        System.exit(SwampApiWrapperExitCodes.NO_ERRORS.getExitCode());
    }

}
