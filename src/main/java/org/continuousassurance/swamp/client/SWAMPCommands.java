package org.continuousassurance.swamp.client;

import org.continuousassurance.swamp.client.commands.PackageCommands;
import org.continuousassurance.swamp.client.commands.ProjectCommands;
import edu.uiuc.ncsa.security.core.util.AbstractEnvironment;
import edu.uiuc.ncsa.security.core.util.ConfigurationLoader;
import edu.uiuc.ncsa.security.core.util.MyLoggingFacade;
import edu.uiuc.ncsa.security.util.cli.CLIDriver;
import edu.uiuc.ncsa.security.util.cli.CommonCommands;
import edu.uiuc.ncsa.security.util.cli.ConfigurableCommandsImpl;
import edu.uiuc.ncsa.security.util.cli.InputLine;
import org.continuousassurance.swamp.session.util.SWAMPConfigurationLoader;
import org.continuousassurance.swamp.session.util.SWAMPServiceEnvironment;
import org.apache.commons.lang.StringUtils;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 4/8/16 at  2:19 PM
 */
public class SWAMPCommands extends ConfigurableCommandsImpl {
    public static final String PROJECTS = "projects";
    public static final String PACKAGES = "packages";
    public static final String VERSIONS = "versions";

    public SWAMPCommands(MyLoggingFacade logger) {
        super(logger);
    }

    @Override
    public String getComponentName() {
        return SWAMPConfigTags.SWAMP_COMPONENT_NAME;
    }

    @Override
    public ConfigurationLoader<? extends AbstractEnvironment> getLoader() {
        return new SWAMPConfigurationLoader<>(getConfigurationNode(), getMyLogger());
    }

    @Override
    public String getPrompt() {
        return "swamp>";
    }

    public static void main(String[] args) {
        try {
            SWAMPCommands swampCommands = new SWAMPCommands(null);
            swampCommands.start(args);
            //SWAMPServiceEnvironment sse = (SWAMPServiceEnvironment) swampCommands.getEnvironment();
            /**
             * Set up all the handlers for this CLI.
             */
       /*     try {
                HandlerFactoryUtil.createHandlerFactory(sse.getRwsAddress().toString(),
                        sse.getCsaAddress().toString(),
                        sse.getHeaders().get(SWAMPConfigTags.ORIGIN_HEADER_TAG),
                        sse.getHeaders().get(SWAMPConfigTags.REFERER_HEADER_TAG),
                        sse.getHeaders().get(SWAMPConfigTags.HOST_HEADER_TAG),
                        sse.getUsername(),
                        sse.getPassword());
                sse.getMyLogger().info("Successful logon");
            } catch (Throwable t) {
                System.out.println("There was an error. Check the logs at \"" + swampCommands.getLogfileName() + "\" for more information.");
                sse.getMyLogger().error("Logon failed!", t);
            }*/
            CLIDriver cli = new CLIDriver(swampCommands);
            cli.start();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    protected void start(String[] args) throws Exception {
        if (!getOptions(args)) {
            say("Warning: no configuration file specified. type in 'load --help' to see how to load one.");
            return;
        }
        initialize();
        about();
    }

    protected String padLineWithBlanks(String x, int width) {
        String xx = StringUtils.rightPad(x, width, " ");
        return xx;
    }

    public void about() {
        int width = 60;
        String stars = StringUtils.rightPad("", width + 1, "*");
        say(stars);
        say(padLineWithBlanks("* SWAMP CLI (Command Line Interface)", width) + "*");
        say(padLineWithBlanks("* Version " + "1.0", width) + "*");
        say(padLineWithBlanks("* By Jeff Gaynor  NCSA", width) + "*");
        say(padLineWithBlanks("*  (National Center for Supercomputing Applications)", width) + "*");
        say(padLineWithBlanks("*", width) + "*");
        say(padLineWithBlanks("* type 'help' for a list of commands", width) + "*");
        say(padLineWithBlanks("*      'exit' or 'quit' to end this session.", width) + "*");
        say(stars);
    }

    @Override
    public boolean use(InputLine inputLine) throws Exception {
        CommonCommands commands = null;
        if (inputLine.hasArg(PROJECTS)) {
            commands = getProjectCommands();
        }

        if (inputLine.hasArg(PACKAGES)) {
            commands = getPackageCommands();
        }

        if (commands != null) {
            CLIDriver cli = new CLIDriver(commands);
            cli.start();
            return true;
        }

        if (super.use(inputLine)) {
            return true;
        }

        return false;
    }

    public CommonCommands getProjectCommands() throws Exception {
        ProjectCommands projectCommands = new ProjectCommands(getMyLogger(), ((SWAMPServiceEnvironment) getEnvironment()).getProjectStore());
        return projectCommands;
    }

    public CommonCommands getPackageCommands() throws Exception {
        PackageCommands packageCommands = new PackageCommands(getMyLogger(), ((SWAMPServiceEnvironment) getEnvironment()).getPackageStore());
        return packageCommands;
    }

    @Override
    public void useHelp() {
        say("This is a prototype command line interface to the SWAMP. You must specify which ");
        say("instance you wish to contact in your configuration file. Currently there are two basic commands implemented.");
        say("projects: list your projects and do basic editing on them");
        say("packages: ditto");
        say("You access these by issuing something like");
        say("use packages");
        say("\nNote that you need to correctly specify the server addresses. If this fails you will not get the corresponding");
        say("prompt for that component.");
    }
}

