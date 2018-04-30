package org.continuousassurance.swamp.api;

import java.util.Date;

import org.continuousassurance.swamp.session.Session;

import java.util.Map;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 3/4/15 at  12:39 PM
 */
public class ToolVersion extends SwampThing{
    public static final String TOOL_VERSION_UUID_KEY = "tool_version_uuid";
    public static final String TOOL_UUID_KEY = "tool_uuid";
    public static final String VERSION_STRING_KEY = "version_string";
    public static final String RELEASE_DATE_KEY = "release_date";
    public static final String RETIRE_DATE_KEY = "retire_date";
    public static final String NOTES_KEY = "notes";
    public static final String TOOL_PATH_KEY = "tool_path";
    public static final String TOOL_EXECUTABLE_KEY = "tool_executable";
    public static final String TOOL_ARGUMENTS_KEY = "tool_arguments";
    public static final String TOOL_DIRECTORY_KEY = "tool_directory";

    Tool tool;
    
    public ToolVersion(Session session) {
        super(session);
    }
    public ToolVersion(Session session, Map map) {
        super(session, map);
    }

    @Override
    public String getIDKey() {
        return TOOL_VERSION_UUID_KEY;
    }

    @Override
    protected SwampThing getNewInstance() {
        return new ToolVersion(getSession());
    }

    public String getVersion() {
        return getConversionMap().getString(VERSION_STRING_KEY);
    }

    public Date getReleaseDate() {
        return getConversionMap().getDate(RELEASE_DATE_KEY);
    }
    
    public void setTool(Tool tool) {
        this.tool = tool;
    }
    
    public Tool getTool() {
        return this.tool;
    }
}
