package org.continuousassurance.swamp.client.commands;

import edu.uiuc.ncsa.security.core.Identifiable;
import edu.uiuc.ncsa.security.core.Store;
import edu.uiuc.ncsa.security.core.util.MyLoggingFacade;
import org.continuousassurance.swamp.api.Platform;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 4/27/17 at  1:46 PM
 */
public class PlatformCommands extends SWAMPStoreCommands {
    public PlatformCommands(MyLoggingFacade logger, String defaultIndent, Store store) {
        super(logger, defaultIndent, store);
    }

    public PlatformCommands(MyLoggingFacade logger, Store store) {
        super(logger, store);
    }

    @Override
    public void extraUpdates(Identifiable identifiable) {

    }

    @Override
    public String getName() {
        return "platforms";
    }

    @Override
    public boolean update(Identifiable identifiable) {
        return false;
    }

    @Override
    protected String format(Identifiable identifiable) {
        Platform platform = (Platform) identifiable;
        return platform.getName();
    }

    @Override
    protected void longFormat(Identifiable identifiable) {
        Platform platform = (Platform) identifiable;
        printAttribute("name", platform.getName());
        printAttributei("description", platform.getDescription());
        printList("versions", platform.getVersions());
        printAttributei("uid", platform.getIdentifierString());
        printAttributei("sharing status", platform.getPlatformSharingStatus());
    }
}
