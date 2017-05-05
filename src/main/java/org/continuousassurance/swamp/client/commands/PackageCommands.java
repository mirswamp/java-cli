package org.continuousassurance.swamp.client.commands;

import edu.uiuc.ncsa.security.core.Identifiable;
import edu.uiuc.ncsa.security.core.Store;
import edu.uiuc.ncsa.security.core.util.MyLoggingFacade;
import org.continuousassurance.swamp.api.PackageThing;
import org.continuousassurance.swamp.api.PackageVersion;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 4/19/16 at  11:06 AM
 */
public class PackageCommands extends SWAMPStoreCommands {
    public PackageCommands(MyLoggingFacade logger, String defaultIndent, Store store) {
        super(logger, defaultIndent, store);
    }

    public PackageCommands(MyLoggingFacade logger, Store store) {
        super(logger, store);
    }

    @Override
    public void extraUpdates(Identifiable identifiable) {

    }

    @Override
    public String getName() {
        return defaultIndent + "packages";
    }

    @Override
    public boolean update(Identifiable identifiable) {
        PackageThing p = (PackageThing) identifiable;
        info("Starting package update for id = " + p.getIdentifierString());
        say("Update the values. A return accepts the existing or default value in []'s");
        p.setName(getInput("Name", p.getName()));
        p.setDescription(getInput("Description", p.getDescription()));
        p.setType(getInput("Type", p.getType()));
        sayi2("save [y/n]?");

        if (isOk(readline())) {
            sayi("package updated.");
            info("package with id " + p.getIdentifierString() + " saving...");
            return true;
        }
        return false;
    }

    @Override
    protected String format(Identifiable identifiable) {
        PackageThing p = (PackageThing) identifiable;
        String rc = p.getName() + ", " + p.getDescription() + " (uid" + ATTRIBUTE_DELIMITER + p.getIdentifierString() + ")";
        return rc;
    }

    @Override
    protected void longFormat(Identifiable identifiable) {
        PackageThing p = (PackageThing) identifiable;
        printAttribute("package name", p.getName());
        if (p.getFilename() != null) {
            printAttributei("file name", p.getFilename());
        }
        printAttributei("uid", p.getUUIDString());
        printAttributei("description", p.getDescription(), true);
        if (p.getVersions() == null || p.getVersions().isEmpty()) {
            sayi("no versions");
        } else {
            for (PackageVersion x : p.getVersions()) {
                sayi("  version" + ATTRIBUTE_DELIMITER + x.getFilename() + ", uid" + ATTRIBUTE_DELIMITER + x.getIdentifierString());
            }
        }
    }
}
