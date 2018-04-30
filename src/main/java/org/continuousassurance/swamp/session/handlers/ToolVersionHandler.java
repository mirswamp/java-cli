package org.continuousassurance.swamp.session.handlers;

import org.continuousassurance.swamp.api.Project;
import org.continuousassurance.swamp.api.Tool;
import edu.uiuc.ncsa.security.core.exceptions.NotImplementedException;
import org.continuousassurance.swamp.api.ToolVersion;
import org.continuousassurance.swamp.session.MyResponse;
import org.continuousassurance.swamp.session.Session;
import org.continuousassurance.swamp.session.util.ConversionMapImpl;
import net.sf.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 5/26/16 at  3:10 PM
 */
public class ToolVersionHandler<T extends ToolVersion> extends AbstractHandler<T> {
    public ToolVersionHandler(Session session) {
        super(session);
    }


    @Override
    protected T fromJSON(JSONObject json) {
        T tv = (T) new ToolVersion(getSession());
        ConversionMapImpl map = new ConversionMapImpl();
        String[] sAttrib = {ToolVersion.NOTES_KEY, ToolVersion.VERSION_STRING_KEY, ToolVersion.TOOL_PATH_KEY,
                ToolVersion.TOOL_EXECUTABLE_KEY, ToolVersion.TOOL_ARGUMENTS_KEY, ToolVersion.TOOL_DIRECTORY_KEY};
        String[] uAttrib = {ToolVersion.TOOL_UUID_KEY, ToolVersion.TOOL_VERSION_UUID_KEY};
        String[] dAttrib = {ToolVersion.RELEASE_DATE_KEY, ToolVersion.RETIRE_DATE_KEY};

        setAttributes(map, sAttrib, json, DATA_TYPE_STRING);
        setAttributes(map, dAttrib, json, DATA_TYPE_DATE);
        setAttributes(map, uAttrib, json, DATA_TYPE_IDENTIFIER);

        tv.setConversionMap(map);
        return tv;
    }

    @Override
    public Collection<T> getAll() {
        throw new NotImplementedException("GetAll for tool versions is not implemented.");
    }

    @Override
    public String getURL() {
        return createURL("tools");
    }

    /**
     * Return all versions of the given tool.
     *
     * @param tool
     * @return
     */
    public List<T> getAll(Tool tool) {
        String x = createURL("tools/" + tool.getIdentifierString() + "/versions");
        MyResponse mr = getClient().rawGet(x, null);
        ArrayList<T> tools = new ArrayList<>();
        if (mr.jsonArray == null) {
            return tools;
        }
        for (int i = 0; i < mr.jsonArray.size(); i++) {
            JSONObject json = mr.jsonArray.getJSONObject(i);
            T t = fromJSON(json);
            t.setTool(tool);
            tools.add(t);
        }
        return tools;
    }

    /**
     * Return all versions of the given tool.
     *
     * @param tool
     * @return
     */
    public List<T> getAll(Tool tool, String pkg_type) {
        String x = createURL("tools/" + tool.getIdentifierString() + "/versions");
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("package-type", pkg_type);
        MyResponse mr = getClient().rawGet(x, map);
        ArrayList<T> tools = new ArrayList<>();
        if (mr.jsonArray == null) {
            return tools;
        }
        for (int i = 0; i < mr.jsonArray.size(); i++) {
            JSONObject json = mr.jsonArray.getJSONObject(i);
            T t = fromJSON(json);
            t.setTool(tool);
            tools.add(t);
        }
        return tools;
    }
}
