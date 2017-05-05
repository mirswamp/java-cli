package org.continuousassurance.swamp.client.commands;

import edu.uiuc.ncsa.security.core.Identifiable;
import edu.uiuc.ncsa.security.core.Store;
import edu.uiuc.ncsa.security.core.util.Iso8601;
import edu.uiuc.ncsa.security.core.util.MyLoggingFacade;
import edu.uiuc.ncsa.security.util.cli.InputLine;
import org.continuousassurance.swamp.api.AssessmentResults;
import org.continuousassurance.swamp.api.Project;
import org.continuousassurance.swamp.session.storage.ProjectStore;

import java.util.List;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 4/11/16 at  2:55 PM
 */
public class ProjectCommands extends SWAMPStoreCommands {
    public ProjectCommands(MyLoggingFacade logger, String defaultIndent, Store store) {
        super(logger, defaultIndent, store);
    }

    public ProjectCommands(MyLoggingFacade logger, Store store) {
        super(logger, store);
    }

   protected ProjectStore getProjectStore(){
       return (ProjectStore)getStore();
   }
    @Override
    public String getPrompt() {
        return "projects>";
    }

    @Override
    public void extraUpdates(Identifiable identifiable) {

    }

    protected void showResultsHelp() {
        say("Shows the assessment results for this package.");
        say("Syntax is ");
        say("results index|uid");
        say("");
    }

    public void results(InputLine inputLine) {
        if (showHelp(inputLine)) {
            showResultsHelp();
            return;
        }

        Project p = null;
        if (inputLine.size() == 1) {
            say("You must supply the index or id of the item to update");
            return;
        }
        Identifiable identifiable = findItem(inputLine);
        if (identifiable != null) {
            p = (Project) identifiable;
           List<AssessmentResults> ars = getProjectStore().getAssessmentResults(p);
           say("   There are " + ars.size() + " assessment results.");
           for(AssessmentResults ar : ars){
              // ar.
           }
        } else {
            say("Sorry, project not found");
        }

    }

    @Override
    public String getName() {
        return defaultIndent + "projects";
    }

    @Override
    public boolean update(Identifiable identifiable) {
        Project p = (Project) identifiable;
        info("Starting client update for id = " + p.getIdentifierString());
        say("Update the values. A return accepts the existing or default value in []'s");
        say("uuid=" + p.getUUIDString() + ", create date=" + Iso8601.date2String(p.getCreateDate()));
        p.setFullName(getInput("Full name", p.getFullName()));
        p.setShortName(getInput("Short name", p.getShortName()));
        p.setDescription(getInput("Description", p.getDescription()));
        p.setAffiliation(getInput("Affiliation", p.getAffiliation()));

        sayi2("save [y/n]?");
        if (isOk(readline())) {
            sayi("Project updating...");
            return true;
        }

        sayi("Project not updated, losing changes...");
        info("User terminated updates for project with id " + p.getIdentifierString());

        return false;

    }


    @Override
    protected String format(Identifiable identifiable) {
        Project p = (Project) identifiable;
        String rc = p.getShortName() + " (uuid" + ATTRIBUTE_DELIMITER + p.getUUIDString() + ")";
        return rc;
    }

    @Override
    protected void longFormat(Identifiable identifiable) {
        Project p = (Project) identifiable;
        printAttribute("full name", p.getFullName());
        printAttributei("short name", p.getShortName());
        printAttributei("uuid", p.getUUIDString());
        printAttributei("description", p.getDescription(), true);
        printAttributei("owner uuid", p.getOwnerUUID());
    }
}
