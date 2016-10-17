package edu.illinois.ncsa.swamp;

import edu.uiuc.ncsa.swamp.api.Project;
import edu.uiuc.ncsa.swamp.api.User;
import edu.uiuc.ncsa.swamp.session.handlers.ProjectHandler;
import edu.uiuc.ncsa.swamp.session.handlers.UserHandler;
import edu.uiuc.ncsa.swamp.util.HandlerFactoryUtil;

import java.util.List;

/**
<<<<<<< HEAD
 * List all projects for the user.<br/>
 * <b>NOTE:</b> This class is now officially deprecated. It should not be used except for, perhaps, some low-level
 * code examples. Please see the README that accompanies this project to see how to use the command line client.
 *
 * @deprecated
=======
 * List all projects for the user.
>>>>>>> c2436499c80dabdbb0b51e94a7092aa7f2cbf2dc
 */
public class App {
    public static String SERVER_PROPERTY = "server";
    public static String USERNAME_PROPERTY = "username";
    public static String PASSWORD_PROPERTY = "password";

    public static String PRODUCTION_FLAG = "pd";
    public static String INTEGRATION_FLAG = "it";
    public static String DEVELOPMENT_FLAG = "dt";

    public static void main(String[] args) {


        try {
            // logon -- keep one, single session for all operations then log out at the end
            // Note that the SWAMP will block your system if you do more than a few even successful
            // logins too quickly. The limit is something like 5 or 6 within a few minutes.
            if (!init()) {
                return;
            }

            // Sessions with RWS and CSA are now working and you may just issue calls as you see fit
            // against whatever handlers you want. The handlers here are passed in as arguments, but the
            // methods to access them are static so once the login is done, you can just get them directly.

            // get some information about the current user.
            listUserInfo(HandlerFactoryUtil.getUserH());

            // list projects for the current user.
            listProjects(HandlerFactoryUtil.getProjectH());
            // All done. logout.
            HandlerFactoryUtil.shutdown();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    protected static void listProjects(ProjectHandler projectHandler) {
        List<? extends Project> projects = projectHandler.getAll();
        for (Project project : projects) {
            System.out.println(project);
        }
    }

    protected static void listUserInfo(UserHandler userHandler) {
        User user = userHandler.getCurrentUser();
        System.out.println("User = " + user);
    }

    /**
     * Ok, some routine setup which should spit out meaningful messages.  The result of this is that the handler
     * for a server is set up, or some information is printed.
     *
     * @return
     */
    protected static boolean init() {
        String username = System.getProperty(USERNAME_PROPERTY);
        if (username == null || username.length() == 0) {
            say("No " + USERNAME_PROPERTY + " found. You must specify one.");
            return false;
        }
        String password = System.getProperty(PASSWORD_PROPERTY);
        if (password == null || password.length() == 0) {
            say("No " + PASSWORD_PROPERTY + " found. You must specify one.");
            return false;
        }
        String server = System.getProperty(SERVER_PROPERTY);
        if (server == null || server.length() == 0) {
            say("No " + SERVER_PROPERTY + " found. You must specify one.");
            return false;
        }


        // If it gets to here, then no server was recognized.

        say("No supported server found.");
        return false;
    }

    protected static void say(String x) {
        System.out.println(x);
    }
}
