package org.continuousassurance.swamp.api;

import org.continuousassurance.swamp.session.Session;
import org.continuousassurance.swamp.session.handlers.RunRequestHandler;

import java.util.Map;

/**
 * This models a run request. Properties supported are
 * <ul>
 *     <li>Description</li>
 *     <li>Name</li>
 *     <li>Project</li>
 * </ul>
 * <p>Created by Jeff Gaynor<br>
 * on 12/22/14 at  11:37 AM
 */
public class RunRequest extends SwampThing{
    public RunRequest(Session session) {
        super(session);
    }
    public RunRequest(Session session, Map map) {
        super(session, map);
    }

    @Override
    protected SwampThing getNewInstance() {
        return new RunRequest(getSession());
    }

    @Override
    public String getIDKey() {
        return RunRequestHandler.RUN_REQUEST_UUID_KEY;
    }

    public String getName(){return getString(RunRequestHandler.RUN_REQUEST_NAME_KEY);}
    public void setName(String name){put(RunRequestHandler.RUN_REQUEST_NAME_KEY, name);}
    public String getDescription(){return getString(RunRequestHandler.RUN_REQUEST_DESCRIPTION_KEY);}
    public void setDescription(String description){put(RunRequestHandler.RUN_REQUEST_DESCRIPTION_KEY, description);}

    Project project = null;
    public Project getProject(){return project;}
    public void setProject(Project project){this.project= project;}

    @Override
    public String toString() {
        return "RunRequest[uuid=" + getUUIDString() + ", name=" + getName() + ", description=" + getDescription() + ", project uuid=" + project.getUUIDString() + "]";
    }
}
