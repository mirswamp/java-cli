package org.continuousassurance.swamp.client.commands;

import edu.uiuc.ncsa.security.core.Identifiable;
import edu.uiuc.ncsa.security.core.Store;
import edu.uiuc.ncsa.security.core.util.MyLoggingFacade;
import org.continuousassurance.swamp.api.AssessmentResults;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 5/1/17 at  1:41 PM
 */
public class AssessmentResultsCommands extends SWAMPStoreCommands {
    public AssessmentResultsCommands(MyLoggingFacade logger, String defaultIndent, Store store) {
        super(logger, defaultIndent, store);
    }

    public AssessmentResultsCommands(MyLoggingFacade logger, Store store) {
        super(logger, store);
    }

    @Override
    public void extraUpdates(Identifiable identifiable) {

    }

    @Override
    public String getName() {
        return "results";
    }

    @Override
    public boolean update(Identifiable identifiable) {
        return false;
    }

    @Override
    protected String format(Identifiable identifiable) {
        AssessmentResults ar = (AssessmentResults)identifiable;

        return ar.getIdentifierString();
    }

    @Override
    protected void longFormat(Identifiable identifiable) {
        AssessmentResults ar = (AssessmentResults)identifiable;

        printAttributei("parent project",ar.getParentProject().getShortName());
        printAttributei("parent project",ar.getParentProject().getShortName());

    }
}
