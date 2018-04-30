package org.continuousassurance.swamp.session.handlers;

import org.continuousassurance.swamp.api.AssessmentRecord;
import org.continuousassurance.swamp.api.Project;
import org.continuousassurance.swamp.session.MyResponse;
import org.continuousassurance.swamp.session.Session;
import org.continuousassurance.swamp.session.util.ConversionMapImpl;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static org.continuousassurance.swamp.session.handlers.PackageHandler.PACKAGE_UUID_KEY;
import static org.continuousassurance.swamp.session.handlers.PlatformHandler.PLATFORM_UUID_KEY;
import static org.continuousassurance.swamp.session.handlers.ToolHandler.TOOL_UUID_KEY;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 12/10/14 at  11:00 AM
 */
public class AssessmentRecordHandler<T extends AssessmentRecord> extends AbstractHandler<T> {
	public static final String EXECUTION_RECORD_UUID = "execution_record_uuid";
	public static final String ASSESSMENT_RUN_UUID = "assessment_run_uuid";
	public static final String ASSESSMENT_RESULT_UUID = "assessment_result_uuid";
    public static final String PROJECT_UUID = "project_uuid";
    public static final String PACKAGE_VERSION_UUID = "package_version_uuid";
    public static final String PLATFORM_VERSION_UUID = "platform_version_uuid";
    public static final String TOOL_VERSION_UUID = "tool_version_uuid";  // tool version uuid.
    public static final String STATUS_STRING = "status";
    public static final String WEAKNESS_COUNT = "weakness_cnt";
    public static final String PACKAGE_NAME = "name";
    public static final String PACKAGE_VERSION = "version_string";
    public static final String TOOL_NAME = "name";
    public static final String TOOL_VERSION = "version_string";
    public static final String PLATFORM_NAME = "name";
    public static final String PLATFORM_VERSION = "version_string";
    public static final String CREATE_DATE = "create_date";
    
    public AssessmentRecordHandler(Session session) {
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
     * efficient to get assessments per project by using {@link #getAllAssessments(edu.uiuc.ncsa.swamp.api.Project)}.
     *
     * @return
     */
    @Override
    public List<T> getAll() {
        List<? extends Project> projects = getProjectHandler().getAll();
        List<T> assessments = new ArrayList<>();
        for (Project project : projects) {
            assessments.addAll(getExecutionRecords(project));
        }
        return assessments;
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
    public List<T> getExecutionRecords(Project project) {
        String url = createURL("projects/" + project.getUUIDString() + "/execution_records");
        MyResponse mr = getClient().rawGet(url, null);
        ArrayList<T> execution_records = new ArrayList<>();
        for (int i = 0; i < mr.jsonArray.size(); i++) {
            JSONObject jo = mr.jsonArray.getJSONObject(i);
            //Object v = jo.get(ASSESSMENT_RUN_UUID); // this is actually the uuid of the assessment run.
            //T record = (T) new AssessmentRecord(getSession());
            //record.setIdentifier(SWAMPIdentifiers.toIdentifier(v.toString()));
            T record = fromJSON(jo);
            record.setProject(project);
            execution_records.add(record);
        }
        return execution_records;
    }


    @Override
    protected T fromJSON(JSONObject json) {
        T a = (T) new AssessmentRecord(getSession());
        ConversionMapImpl map = new ConversionMapImpl();
        String[] uAttrib = {EXECUTION_RECORD_UUID, ASSESSMENT_RUN_UUID, ASSESSMENT_RESULT_UUID,
        					PROJECT_UUID, 
        					 };
        String[] sAttrib = {STATUS_STRING};
        
        setAttributes(map, uAttrib, json, DATA_TYPE_IDENTIFIER);
        setAttributes(map, sAttrib, json, DATA_TYPE_STRING);
        setAttributes(map, new String[] {CREATE_DATE}, json, DATA_TYPE_DATE);

        JSONObject pkg_info = json.getJSONObject("package");
        map.put("package_name", pkg_info.get(PACKAGE_NAME));
        map.put("package_version", pkg_info.get(PACKAGE_VERSION));
        
        setAttributes(map, new String[] {PACKAGE_UUID_KEY, PACKAGE_VERSION_UUID},
                pkg_info, DATA_TYPE_IDENTIFIER);
                
        JSONObject tool_info = json.getJSONObject("tool");
        map.put("tool_name", tool_info.get(TOOL_NAME));
        map.put("tool_version", tool_info.get(TOOL_VERSION));
        
        setAttributes(map, new String[] {TOOL_UUID_KEY, TOOL_VERSION_UUID},
                tool_info, DATA_TYPE_IDENTIFIER);

        JSONObject plat_info = json.getJSONObject("platform");
        map.put("platform_name", plat_info.get(PLATFORM_NAME));
        map.put("platform_version", plat_info.get(PLATFORM_VERSION));
        
        setAttributes(map, new String[] {PLATFORM_UUID_KEY, PLATFORM_VERSION_UUID},
                plat_info, DATA_TYPE_IDENTIFIER);
        
        
        if (map.getString(STATUS_STRING).equals("Finished")) {
            try {
                map.put(WEAKNESS_COUNT, json.getInt("weakness_cnt"));
            }catch(JSONException exception) {
              //In some cases even though the status is Finished, the weakness_cnt is null
                map.put(WEAKNESS_COUNT, 0);
            }
        }else {
            map.put(WEAKNESS_COUNT, 0);
        }

        a.setConversionMap(map);
        return a;
    }

    @Override
    public String getURL() {
        return createURL("assessment_runs");
    }
    
    public boolean deleteAssessmentRecord(AssessmentRecord assessmentRecord){
    	String url = createURL("execution_records/" + assessmentRecord.getUUIDString());
        MyResponse mr = getClient().delete(url);
        return mr.hasJSON();
    }
}
