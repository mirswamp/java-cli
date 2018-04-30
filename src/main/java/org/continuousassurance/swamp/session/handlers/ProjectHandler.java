package org.continuousassurance.swamp.session.handlers;

import org.continuousassurance.swamp.api.Project;
import org.continuousassurance.swamp.session.MyResponse;
import org.continuousassurance.swamp.session.Session;
import org.continuousassurance.swamp.session.util.ConversionMapImpl;
import net.sf.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 11/26/14 at  3:26 PM
 */
public class ProjectHandler<T extends Project> extends AbstractHandler<T> {

    public static final String PROJECT_UID_KEY = "project_uid";
    public static final String FULL_NAME_KEY = "full_name";
    public static final String SHORT_NAME_KEY = "short_name";
    public static final String DESCRIPTION_KEY = "description";
    public static final String TRIAL_PROJECT_FLAG_KEY = "trial_project_flag";
    public static final String CREATE_DATE_KEY = "create_date";
    public static final String OWNER_KEY = "owner";
    public static final String AFFILIATION_KEY = "affiliation";
    public static final String DENIAL_DATE_KEY = "denial_date";
    public static final String DEACTIVATION_DATE_KEY = "deactivation_date";

    public ProjectHandler(Session session) {
        super(session);
    }

    public T create(String fullName, String shortName, String description) {
        // Very annoyingly, SWAMP uses different parameters here for some of the information than in the
        // object itself.
        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put(FULL_NAME_KEY, fullName);
        parameters.put(SHORT_NAME_KEY, shortName);
        parameters.put(DESCRIPTION_KEY, description);
        parameters.put("project_owner_uid", getSession().getUserUID());
        parameters.put("project_type_code", "SW_DEV"); // seems to be fixed  value
        parameters.put("status", "pending"); // seems to be fixed value

        MyResponse mr = getClient().rawPost(createURL("projects"), parameters);
        T project = fromJSON(mr.json);
        return project;
    }

    @Override
    protected T fromJSON(JSONObject json) {
        T project = (T) new Project(getSession());
        ConversionMapImpl map = new ConversionMapImpl();
        String[] sAttrib = {FULL_NAME_KEY, SHORT_NAME_KEY, DESCRIPTION_KEY, AFFILIATION_KEY};
        String[] bAttrib = {TRIAL_PROJECT_FLAG_KEY};
        String[] uAttrib = {PROJECT_UID_KEY};
        String[] dAttrib = {CREATE_DATE_KEY, DENIAL_DATE_KEY, DEACTIVATION_DATE_KEY};

        setAttributes(map, sAttrib, json, DATA_TYPE_STRING);
        setAttributes(map, bAttrib, json, DATA_TYPE_BOOLEAN);
        setAttributes(map, uAttrib, json, DATA_TYPE_IDENTIFIER);
        setAttributes(map, dAttrib, json, DATA_TYPE_DATE);
        project.setConversionMap(map);
        /*
          The user uuid is embedded in the complete record returned. Since this information is already in the
          user object, recording more of it could lead to conflicts so only the uuid is retained.
         */
        JSONObject owner = json.getJSONObject(OWNER_KEY);
        if (owner != null) {
            project.setOwnerUUID(owner.getString(UserHandler.USER_UID_KEY));
        }

        return project;
    }


    public List<T> getAll() {
        String url = createURL("users/" + getSession().getUserUID()) + "/projects";
        MyResponse mr = getClient().rawGet(url, null);
        ArrayList<T> projects = new ArrayList<>();

        for (int i = 0; i < mr.jsonArray.size(); i++) {
            JSONObject json = mr.jsonArray.getJSONObject(i);
            projects.add(fromJSON(json));
        }
        return projects;
    }

    /**
     * Removes the project.  This returns true if the delete worked and false in all other
     * cases, including that some server-side error occurred preventing deletion.
     *
     */
/*
    public boolean delete(Project project) {
        try {
            MyResponse mr = getClient().delete(createURL("projects/" + project.getUUIDString()));
            return true;
        } catch (Throwable t) {
            //t.printStackTrace();
        }
        return false;
    }
*/

    @Override
    public String getURL() {
        return createURL("projects");
    }
}
