package org.continuousassurance.swamp.client.commands;

import edu.uiuc.ncsa.security.core.Identifiable;
import edu.uiuc.ncsa.security.core.Store;
import edu.uiuc.ncsa.security.core.util.MyLoggingFacade;
import org.continuousassurance.swamp.api.Tool;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 4/26/17 at  5:44 PM
 */
public class ToolCommands extends SWAMPStoreCommands {
    public ToolCommands(MyLoggingFacade logger, String defaultIndent, Store store) {
        super(logger, defaultIndent, store);
    }

    public ToolCommands(MyLoggingFacade logger, Store store) {
        super(logger, store);

    }

    @Override
    public void extraUpdates(Identifiable identifiable) {

    }

    @Override
    public String getName() {
        return "tools";
    }

    @Override
    public boolean update(Identifiable identifiable) {
        return false;
    }

    @Override
    protected String format(Identifiable identifiable) {
        Tool tool = (Tool) identifiable;
        return tool.getName() + " (uid" + ATTRIBUTE_DELIMITER +  tool.getIdentifierString() + ")";
    }

    @Override
    protected void longFormat(Identifiable identifiable) {
        Tool tool = (Tool) identifiable;
        printAttribute("tool name", tool.getName());
        printList("versions", tool.getVersionStrings());
        printAttributei("description", tool.getDescription(), true);
        printAttributei("file name", tool.getFilename());
        printAttributei("uuid", tool.getUUIDString());
        printAttributei("policy", tool.getPolicy());
        printAttributei("policy code", tool.getPolicyCode());
        printAttributei("sharing status", tool.getToolSharingStatus());
        printList("supported packages", tool.getSupportedPkgTypes());
        printList("supported platforms", tool.getSupportedPlatforms());
        printList("viewers", tool.getViewers());
    }


}
