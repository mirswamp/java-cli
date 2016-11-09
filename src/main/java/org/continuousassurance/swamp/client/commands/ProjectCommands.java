package org.continuousassurance.swamp.client.commands;

import edu.uiuc.ncsa.security.core.Identifiable;
import edu.uiuc.ncsa.security.core.Store;
import edu.uiuc.ncsa.security.core.util.Iso8601;
import edu.uiuc.ncsa.security.core.util.MyLoggingFacade;
import edu.uiuc.ncsa.security.util.cli.StoreCommands;
import org.continuousassurance.swamp.api.Project;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 4/11/16 at  2:55 PM
 */
public class ProjectCommands extends StoreCommands {
    public ProjectCommands(MyLoggingFacade logger, String defaultIndent, Store store) {
        super(logger, defaultIndent, store);
    }

    public ProjectCommands(MyLoggingFacade logger, Store store) {
        super(logger, store);
    }


    @Override
    public String getPrompt() {
        return "projects>";
    }

    @Override
    public void extraUpdates(Identifiable identifiable) {

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
        String rc = p.getShortName() + " (uuid=" + p.getUUIDString()+")";
        return rc;
    }

    @Override
    protected void longFormat(Identifiable identifiable) {
        Project p = (Project) identifiable;
        say("Project full name= \"" + p.getFullName() + "\"");
        sayi("short name=\"" + p.getShortName() + "\"");
        sayi("uuid=" + p.getUUIDString());
        sayi("description=" + p.getDescription());
        sayi("owner uuid=" + p.getOwnerUUID());
    }
}
