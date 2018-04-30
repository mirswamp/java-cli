package org.continuousassurance.swamp.session.handlers;

import edu.uiuc.ncsa.security.core.Identifier;
import org.continuousassurance.swamp.api.*;
import org.continuousassurance.swamp.session.MyResponse;
import org.continuousassurance.swamp.session.Session;
import org.continuousassurance.swamp.session.util.ConversionMapImpl;
import org.continuousassurance.swamp.session.util.SWAMPIdentifiers;
import net.sf.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 12/10/14 at  11:00 AM
 */
public class AssessmentRunHandler<T extends AssessmentRun> extends AbstractHandler<T> {
    public static final String ASSESSMENT_RUN_UUID = "assessment_run_uuid";
    public static final String PROJECT_UUID = "project_uuid";
    public static final String PACKAGE_VERSION_UUID = "package_version_uuid";
    public static final String PLATFORM_VERSION_UUID = "platform_version_uuid";
    public static final String TOOL_VERSION_UUID = "tool_version_uuid";  // tool version uuid.

    public AssessmentRunHandler(Session session) {
        super(session);
    }

    public ProjectHandler<? extends Project> getProjectHandler() {
        if (projectHandler == null) {
            projectHandler = new ProjectHandler<>(getSession());
        }
        return projectHandler;
    }

    public void setProjectHandler(ProjectHandler<? extends Project> projectHandler) {
        this.projectHandler = projectHandler;
    }

    ProjectHandler<? extends Project> projectHandler;

    /**
     * Caution: This gets every assessment for every project that the current user has. It is more
     * efficient to get assessments per project by using {@link #getAllAssessments(Project)}.
     *
     * @return
     */
    @Override
    public List<T> getAll() {
        List<? extends Project> projects = getProjectHandler().getAll();
        List<T> assessments = new ArrayList<>();
        for (Project project : projects) {
            assessments.addAll(getAllAssessments(project));
        }
        return assessments;
    }


    public AssessmentRun create(Project project, PackageThing pkg, Platform platform, Tool tool) {
        String url = createURL("assessment_runs");
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("project_uuid", project.getUUIDString());
        parameters.put(PackageHandler.PACKAGE_UUID_KEY, pkg.getUUIDString());
        parameters.put(PlatformHandler.PLATFORM_UUID_KEY, platform.getUUIDString());
        parameters.put(ToolHandler.TOOL_UUID_KEY, tool.getUUIDString());
        MyResponse myResponse = getClient().rawPost(url, parameters);
        AssessmentRun result = fromJSON(myResponse.json);
        result.setProject(project);
        result.setPkg(pkg);
        result.setPlatform(platform);
        result.setTool(tool);
        return result;
    }

    public AssessmentRun create(Project project, PackageVersion pkg_ver, Platform platform, Tool tool) {
        String url = createURL("assessment_runs");
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("project_uuid", project.getUUIDString());
        parameters.put("package_version_uuid", pkg_ver.getUUIDString());
        parameters.put(PackageHandler.PACKAGE_UUID_KEY, pkg_ver.getPackageThing().getUUIDString());
        parameters.put(PlatformHandler.PLATFORM_UUID_KEY, platform.getUUIDString());
        parameters.put(ToolHandler.TOOL_UUID_KEY, tool.getUUIDString()); 
        //parameters.put("tool_version_uuid", tool.getUUIDString());
        MyResponse myResponse = getClient().rawPost(url, parameters);
        AssessmentRun result = fromJSON(myResponse.json);
        result.setProject(project);
        result.setPkg(pkg_ver.getPackageThing());
        result.setPlatform(platform);
        result.setTool(tool);
        return result;
    }

    public AssessmentRun create(Project project, PackageVersion pkg_ver, PlatformVersion platform_version, Tool tool) {
        String url = createURL("assessment_runs");
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("project_uuid", project.getUUIDString());
        parameters.put("package_version_uuid", pkg_ver.getUUIDString());
        parameters.put(PackageHandler.PACKAGE_UUID_KEY, pkg_ver.getPackageThing().getUUIDString());
        //parameters.put(PLATFORM_UUID_KEY, platform_version.getUUIDString());
        parameters.put(PlatformVersion.PLATFORM_UUID_KEY, platform_version.getPlatform().getUUIDString());
        parameters.put(PlatformVersion.PLATFORM_VERSION_UUID_KEY, platform_version.getUUIDString());
        parameters.put(ToolHandler.TOOL_UUID_KEY, tool.getUUIDString()); 
        //parameters.put("tool_version_uuid", tool.getUUIDString());
        MyResponse myResponse = getClient().rawPost(url, parameters);
        AssessmentRun result = fromJSON(myResponse.json);
        result.setProject(project);
        result.setPkg(pkg_ver.getPackageThing());
        result.setPlatform(platform_version.getPlatform());
        result.setPlatformVersion(platform_version);
        result.setTool(tool);
        return result;
    }

    public AssessmentRun create(Project project, PackageVersion pkg_ver, 
            PlatformVersion platform_version, ToolVersion tool_version) {
        String url = createURL("assessment_runs");
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("project_uuid", project.getUUIDString());
        parameters.put("package_version_uuid", pkg_ver.getUUIDString());
        parameters.put(PackageHandler.PACKAGE_UUID_KEY, pkg_ver.getPackageThing().getUUIDString());
        //parameters.put(PLATFORM_UUID_KEY, platform_version.getUUIDString());
        parameters.put(PlatformVersion.PLATFORM_UUID_KEY, platform_version.getPlatform().getUUIDString());
        parameters.put(PlatformVersion.PLATFORM_VERSION_UUID_KEY, platform_version.getUUIDString());
        parameters.put(ToolHandler.TOOL_UUID_KEY, tool_version.getTool().getUUIDString()); 
        parameters.put(TOOL_VERSION_UUID, tool_version.getUUIDString());
        MyResponse myResponse = getClient().rawPost(url, parameters);
        AssessmentRun result = fromJSON(myResponse.json);
        result.setProject(project);
        result.setPkg(pkg_ver.getPackageThing());
        result.setPlatform(platform_version.getPlatform());
        result.setPlatformVersion(platform_version);
        result.setTool(tool_version.getTool());
        return result;
    }

    public T get(Identifier identifier) {
        String url = createURL("assessment_runs/" + SWAMPIdentifiers.fromIdentifier(identifier));
        MyResponse mr = getClient().rawGet(url, null);
        return fromJSON(mr.json);
    }

    /**
     * Extremely goofy responses from SWAMP do not actually return enough information to recover the
     * package, tool or platform. Only names of these are returned and there is no easy way to recover
     * the actual item from its name. Therefore, this call is made and the set of assessments is searched
     * by uuid. Wasteful.
     *
     * @param project
     * @return
     */
    public List<T> getAllAssessments(Project project) {
        String url = createURL("projects/" + project.getUUIDString() + "/assessment_runs");
        MyResponse mr = getClient().rawGet(url, null);
        ArrayList<T> assessments = new ArrayList<>();
        for (int i = 0; i < mr.jsonArray.size(); i++) {
            JSONObject jo = mr.jsonArray.getJSONObject(i);
            T a = fromJSON(jo); //(T) new AssessmentRun(getSession());
            Object v = jo.get(ASSESSMENT_RUN_UUID); // this is actually the uuid of the assessment run.
            a.setIdentifier(SWAMPIdentifiers.toIdentifier(v.toString()));
            a.setProject(project);
            assessments.add(a);
        }
        return assessments;
    }


    public List<? extends AssessmentRun> getAssessments(Project project) {
        String pUrl = createURL("projects/" + project.getUUIDString() + "/assessment_runs");
        // need to get this from the same server that has the assessments, not the projects server.
        MyResponse mr0 = getClient().rawGet(pUrl, null);
        ArrayList<T> list = new ArrayList<>();
        for(int i = 0; i < mr0.jsonArray.size(); i++){
            T a = fromJSON((JSONObject) mr0.jsonArray.get(i));
            list.add(a);
        }
        return list;
    }


    @Override
    protected T fromJSON(JSONObject json) {
        T a = (T) new AssessmentRun(getSession());
        ConversionMapImpl map = new ConversionMapImpl();
        String[] uAttrib = {ASSESSMENT_RUN_UUID, PROJECT_UUID,
                PackageHandler.PACKAGE_UUID_KEY, PACKAGE_VERSION_UUID, ToolHandler.TOOL_UUID_KEY, TOOL_VERSION_UUID,
                PLATFORM_VERSION_UUID, PlatformHandler.PLATFORM_UUID_KEY};
        setAttributes(map, uAttrib, json, DATA_TYPE_IDENTIFIER);

        String[] nAttrib = {"package_name", "package_version_string", 
                "tool_name", "tool_version_string", 
                "platform_name", "platform_version_string"};
        setAttributes(map, nAttrib, json, DATA_TYPE_STRING);

        a.setConversionMap(map);
        return a;
    }

    @Override
    public String getURL() {
        return createURL("assessment_runs");
    }

    public boolean delete(AssessmentRun arun) {
        String url = createURL("assessment_runs/" + arun.getIdentifierString()); 
        MyResponse myResponse = getClient().delete(url);
        AssessmentRun result = fromJSON(myResponse.json);
        if (result != null) {
            return true;
        }else{
            return false;
        }
    }
}

