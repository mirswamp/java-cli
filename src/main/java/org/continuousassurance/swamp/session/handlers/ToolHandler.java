package org.continuousassurance.swamp.session.handlers;

import org.continuousassurance.swamp.api.PackageThing;
import org.continuousassurance.swamp.api.Project;
import org.continuousassurance.swamp.api.Tool;
import org.continuousassurance.swamp.session.MyResponse;
import org.continuousassurance.swamp.session.Session;
import org.continuousassurance.swamp.session.util.ConversionMapImpl;
import net.sf.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 12/10/14 at  11:01 AM
 */
public class ToolHandler<T extends Tool> extends AbstractHandler<T> {
    public ToolHandler(Session session) {
        super(session);
    }

    public static final String TOOL_UUID_KEY = "tool_uuid";
    public static final String NAME_KEY = "name";
    public static final String TOOL_SHARING_STATUS_KEY = "tool_sharing_status";
    public static final String IS_BUILD_NEEDED_KEY = "is_build_needed";
    public static final String POLICY_CODE_KEY = "policy_code";
    public static final String CREATE_DATE_KEY = "create_date";
    public static final String CREATE_USER_KEY = "create_user";
    public static final String UPDATE_DATE_KEY = "update_date";
    public static final String IS_OWNED_KEY = "is_owned";
    public static final String IS_RESTRICTED_KEY = "is_restricted";
    public static final String POLICY_KEY = "policy";
    public static final String PACKAGE_TYPE_NAMES = "package_type_names";
    public static final String PLATFORM_NAMES = "platform_names";
    public static final String DESCRIPTION_KEY = "description";
    public static final String VERSION_STRINGS_KEY = "version_strings";
    public static final String VIEWER_NAMES_KEY= "viewer_names";

     /*
     {"tool_uuid":"56ce7899-b741-11e6-bf70-001a4a81450b"
     ,"name":"PHPMD",
     "description":"PHP Mess Detector. <a href=\"https:\/\/phpmd.org\/\">https:\/\/phpmd.org
     \/<\/a>",
     "tool_sharing_status":"PUBLIC",
     "is_build_needed":0,
     "policy_code":null,
     "create_user":"root@localhost"
     ,"create_date":"2017-01-30 16:17:35",
     "update_user":null,
     "update_date":null,
     "package_type_names":["Web Scripting"],
     "version_strings":["2.5.0"],
     "platform_names":["Scientific Linux 5 64-bit","CentOS Linux
      6 64-bit","Ubuntu Linux","CentOS Linux 5 32-bit","Android","Fedora Linux","Scientific Linux 6 32-bit"
     ,"CentOS Linux 5 64-bit","Scientific Linux 5 32-bit","Red Hat Enterprise Linux 6 32-bit","Scientific
      Linux 6 64-bit","Debian Linux","CentOS Linux 6 32-bit","Red Hat Enterprise Linux 6 64-bit",
      "CentOS Linux 7 64-bit", "CentOS Linux 7 32-bit",
      "Scientific Linux 7 64 bit",
      "Red Hat Enterprise Linux 7 64 bit"],
      "viewer_names"
     :["Code Dx","Native"],
     "is_owned":false,
     "is_restricted":false}
      */
    @Override
    public List<T> getAll() {
        String url = createURL("tools/public");
        MyResponse mr = getClient().rawGet(url, null);
        ArrayList<T> tools = new ArrayList<>();
        if(mr.jsonArray == null){
            return tools;
        }
        for (int i = 0; i < mr.jsonArray.size(); i++) {
            JSONObject json = mr.jsonArray.getJSONObject(i);
            tools.add(fromJSON(json));
        }
        return tools;
    }

    /*
     * Gets Private Tools for the Project
     */
    public List<T> getAll(Project project) {
        String url = createURL("tools/protected/" + project.getUUIDString());
        MyResponse mr = getClient().rawGet(url, null);
        ArrayList<T> tools = new ArrayList<>();
        if(mr.jsonArray == null){
            return tools;
        }
        for (int i = 0; i < mr.jsonArray.size(); i++) {
            JSONObject json = mr.jsonArray.getJSONObject(i);
            tools.add(fromJSON(json));
        }
        return tools;
    }

    public boolean hasPermission(Tool tool, Project project, PackageThing package_thing) {
    	String url = createURL("tools/" + tool.getUUIDString() + "/permission");
    	HashMap<String, Object> map = new HashMap<String, Object>();
    	map.put("package_uuid", package_thing.getUUIDString());
    	map.put("project_uid", project.getUUIDString());
        MyResponse mr = getClient().rawPost(url, map);
        if(mr.jsonArray == null){
            return false;
        }else{
        	return mr.jsonArray.getString(0).equals("granted");
        }
    }
    
    protected T fromJSON(JSONObject json) {
        T tool = (T) new Tool(getSession());
        ConversionMapImpl map = new ConversionMapImpl();
        String[] sAttrib = {NAME_KEY,TOOL_SHARING_STATUS_KEY,POLICY_CODE_KEY,POLICY_KEY, DESCRIPTION_KEY, CREATE_USER_KEY};
        String[] uAttrib = {TOOL_UUID_KEY};
        String[] dAttrib = {CREATE_DATE_KEY,UPDATE_DATE_KEY};
        String[] bAttrib = {IS_BUILD_NEEDED_KEY,IS_OWNED_KEY, IS_RESTRICTED_KEY};
        String[] aAttrib = {PACKAGE_TYPE_NAMES, PLATFORM_NAMES,VERSION_STRINGS_KEY, VIEWER_NAMES_KEY};
        
        setAttributes(map, sAttrib, json, DATA_TYPE_STRING);
        setAttributes(map, dAttrib, json, DATA_TYPE_DATE);
        setAttributes(map, uAttrib, json, DATA_TYPE_IDENTIFIER);
        setAttributes(map, bAttrib, json, DATA_TYPE_BOOLEAN);
        setAttributes(map, aAttrib, json, DATA_TYPE_ARRAY);
        
/*
        tool.setUUID(UUID.fromString(json.getString(TOOL_UUID_KEY)));
        tool.setName(json.getString(NAME_KEY));
        tool.setToolSharingStatus(json.getString(TOOL_SHARING_STATUS_KEY));
        tool.setBuildNeeded(json.getInt(IS_BUILD_NEEDED_KEY) != 0);
        tool.setPolicyCode(json.getString(POLICY_CODE_KEY));
        tool.setCreateDate(toSWAMPDate(json, CREATE_DATE_KEY));
        tool.setUpdateDate(toSWAMPDate(json, UPDATE_DATE_KEY));
        tool.setOwned(json.getBoolean(IS_OWNED_KEY));
        tool.setPolicy(json.getString(POLICY_KEY));
*/
        tool.setConversionMap(map);
        return tool;
    }

    public Tool find(String name) {
        for (Tool tool : getAll()) {
            if (tool.getName().equals(name)) {
                return tool;
            }
        }
        return null;
    }

    @Override
    public String getURL() {
        return createURL("tools");
    }
}
