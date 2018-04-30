package org.continuousassurance.swamp.session.handlers;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.continuousassurance.swamp.api.AssessmentRun;
import org.continuousassurance.swamp.api.Project;
import org.continuousassurance.swamp.api.RunRequest;
import org.continuousassurance.swamp.session.MyResponse;
import org.continuousassurance.swamp.session.Session;
import org.continuousassurance.swamp.session.util.ConversionMapImpl;

import edu.uiuc.ncsa.security.storage.data.ConversionMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 12/22/14 at  3:39 PM
 */
public class RunRequestHandler<T extends RunRequest> extends AbstractHandler<RunRequest> {
    public static final String RUN_REQUEST_UUID_KEY = "run_request_uuid";
    public static final String RUN_REQUEST_NAME_KEY = "name";
    public static final String RUN_REQUEST_DESCRIPTION_KEY = "description";
    public static final String RUN_REQUEST_PROJECT_UUID_KEY = "project_uuid"; // different than used by project itself.

    public RunRequestHandler(Session session) {
        super(session);
    }

    ProjectHandler projectHandler;

    public ProjectHandler getProjectHandler() {
        return projectHandler;
    }

    public void setProjectHandler(ProjectHandler projectHandler) {
        this.projectHandler = projectHandler;
    }

    @Override
    protected RunRequest fromJSON(JSONObject json) {
        RunRequest rr = new RunRequest(getSession());
       // Project project = (Project) getProjectHandler().get(SWAMPIdentifiers.toIdentifier(json.getString(RUN_REQUEST_PROJECT_UUID_KEY)));
       // rr.setProject(project);
        ConversionMapImpl map = new ConversionMapImpl();

        String[] uAttrib = {RUN_REQUEST_UUID_KEY};
        String[] sAttrib = {RUN_REQUEST_NAME_KEY, RUN_REQUEST_DESCRIPTION_KEY};
        setAttributes(map, uAttrib, json, DATA_TYPE_IDENTIFIER);
        setAttributes(map, sAttrib, json, DATA_TYPE_STRING);
        rr.setConversionMap(map);
        return rr;
    }

    @Override
    public List<RunRequest> getAll() {
        return null;
    }

    @Override
    public String getURL() {
        return createURL("run_requests");
    }
    public RunRequest create(Project project, String name, String description){
        ConversionMapImpl map = new ConversionMapImpl();
        map.put(RUN_REQUEST_PROJECT_UUID_KEY, project.getUUIDString() );
        map.put(RUN_REQUEST_NAME_KEY, name);
        map.put(RUN_REQUEST_DESCRIPTION_KEY, description);
        RunRequest rr = (RunRequest) super.create(map);
        rr.setProject(project);
        return rr;
    }
    public boolean submitOneTimeRequest(Collection<AssessmentRun> aRuns, boolean notifyWhenDone){
        String url = createURL("run_requests/one-time");
        ConversionMapImpl parameters = new ConversionMapImpl();
        if(notifyWhenDone) {
            parameters.put("notify-when-done", "true");
        }
        
        JSONArray uuids = new JSONArray();
        for(AssessmentRun arun : aRuns){
            uuids.add(arun.getUUIDString());
        }
        parameters.put("assessment-run-uuids", uuids);
        
        MyResponse myResponse = getClient().rawPost(url, mapToJSON(parameters));

        if (myResponse.jsonArray != null){
        	return true;
        }else {
        	return false;
        }

    }
    public boolean submitOneTimeRequest(AssessmentRun aRun, boolean notifyWhenDone){
        String url = createURL("run_requests/one-time");
        HashMap<String, Object> parameters = new HashMap<>();
        if(notifyWhenDone) {
            parameters.put("notify-when-done", "true");
        }
        parameters.put("assessment-run-uuids[]", aRun.getUUIDString());
        MyResponse myResponse = getClient().rawPost(url, parameters);
        
        //System.out.println(myResponse);
        
        if (myResponse.jsonArray != null){
        	return true;	
        }else {
        	return false;
        }
           /*
        parameters.put(PACKAGE_UUID_KEY, pkg.getUUIDString());
        parameters.put(PLATFORM_UUID_KEY, platform.getUUIDString());
        parameters.put(TOOL_UUID_KEY, tool.getUUIDString());
        AssessmentRun result = fromJSON(myResponse.json);
        result.setProject(project);
        result.setPkg(pkg);
        result.setPlatform(platform);
        result.setTool(tool);
            */
    }
}
