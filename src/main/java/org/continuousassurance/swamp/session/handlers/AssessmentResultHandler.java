package org.continuousassurance.swamp.session.handlers;

import org.continuousassurance.swamp.api.Project;
import org.continuousassurance.swamp.session.MyResponse;
import org.continuousassurance.swamp.session.Session;
import org.continuousassurance.swamp.api.AssessmentResults;
import org.continuousassurance.swamp.session.util.ConversionMapImpl;
import net.sf.json.JSONObject;

import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 3/23/16 at  3:27 PM
 */
public class AssessmentResultHandler<T extends AssessmentResults> extends AbstractHandler<T> {
    public AssessmentResultHandler(Session session) {
        super(session);
    }

    public static final String ASSESSMENT_RESULT_UUID_KEY = "assessment_result_uuid";
    public static final String CREATE_DATE_KEY = "create_date";
    public static final String CREATE_USER_KEY = "create_user";
    public static final String PACKAGE_NAME_KEY = "package_name";
    public static final String PACKAGE_VERSION_KEY = "package_version";
    public static final String PLATFORM_NAME_KEY = "platform_name";
    public static final String PLATFORM_VERSION_KEY = "platform_version";
    public static final String PROJECT_UUID_KEY = "project_uuid";
    public static final String TOOL_NAME_KEY = "tool_name";
    public static final String TOOL_VERSION_KEY = "tool_version";
    public static final String UPDATE_DATE_KEY = "update_date";
    public static final String UPDATE_USER_KEY = "update_user";
    public static final String WEAKNESS_COUNT_KEY = "weakness_cnt";

    @Override
    protected T fromJSON(JSONObject json) {
        T t = (T) new AssessmentResults(getSession(), null);


        ConversionMapImpl map = new ConversionMapImpl();
        String[] sAttrib = {CREATE_USER_KEY,
                UPDATE_USER_KEY,
                PACKAGE_NAME_KEY,
                PACKAGE_NAME_KEY,
                PACKAGE_VERSION_KEY,
                PLATFORM_NAME_KEY,
                PLATFORM_VERSION_KEY,
                TOOL_NAME_KEY,
                TOOL_VERSION_KEY,
                WEAKNESS_COUNT_KEY};
        String[] uAttrib = {ASSESSMENT_RESULT_UUID_KEY, PROJECT_UUID_KEY};
        String[] dAttrib = {CREATE_DATE_KEY, UPDATE_DATE_KEY};


        setAttributes(map, sAttrib, json, DATA_TYPE_STRING);
        setAttributes(map, uAttrib, json, DATA_TYPE_IDENTIFIER);
        setAttributes(map, dAttrib, json, DATA_TYPE_DATE);
        t.setConversionMap(map);
        return t;
    }

    @Override
    public List<T> getAll() {
        return null;
    }

    /**
     * This will get all of the results for a given project.
     *
     * @param project
     * @return
     */
    public List<T> getAll(Project project) {
        LinkedList<T> list = new LinkedList<>();
        String endpoint = createURL("projects/" + project.getUUIDString() + "/assessment_results");
        MyResponse myResponse = getClient().rawGet(endpoint);
        //myResponse.
        if (myResponse.jsonArray == null) {
            return list; //nothing found.
        }
        for (int i = 0; i < myResponse.jsonArray.size(); i++) {
            T t = fromJSON(myResponse.jsonArray.getJSONObject(i));
            t.setParentProject(project);
            list.add(t);
        }
        return list;
    }

    @Override
    public String getURL() {
        return createURL("v1/assessment_results");
    }


    /**
     * Returns SCARF results or null if there were none/an error happened.
     * @param results
     * @return
     */
    public OutputStream getScarfResults(AssessmentResults results) {
        String url = getURL() + "/" + results.getUUIDString() + "/scarf";
        MyResponse myresponse = getClient().rawGet(url, true);
        // If the response is actually JSON, then there was no scarf result found.
        if(myresponse.hasJSON()) {
            return null;
        }
        return myresponse.getOutputStream();
    }

}
