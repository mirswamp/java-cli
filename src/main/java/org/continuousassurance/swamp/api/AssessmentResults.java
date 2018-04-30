package org.continuousassurance.swamp.api;

import org.continuousassurance.swamp.session.Session;
import org.continuousassurance.swamp.session.handlers.AssessmentResultHandler;

import java.util.Map;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 3/4/15 at  12:43 PM
 */
public class AssessmentResults extends SwampThing {


    public AssessmentResults(Session session, Project parentProject) {
        super(session);
        this.parentProject = parentProject;
    }

    public AssessmentResults(Session session, Project parentProject, Map map) {
        super(session,map);
        this.parentProject = parentProject;
    }

    Project parentProject;

    public Project getParentProject() {
        return parentProject;
    }

    public void setParentProject(Project parentProject) {
        this.parentProject = parentProject;
    }

    @Override
    public String getIDKey() {
        return AssessmentResultHandler.ASSESSMENT_RESULT_UUID_KEY;
    }

    @Override
    protected SwampThing getNewInstance() {
        return new AssessmentResults(getSession(), (Project) getParentProject().clone());
    }
}
