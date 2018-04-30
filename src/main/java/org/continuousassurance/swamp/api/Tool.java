package org.continuousassurance.swamp.api;

import net.sf.json.JSONArray;
import org.continuousassurance.swamp.session.Session;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.continuousassurance.swamp.session.handlers.ToolHandler.*;

/**
 * Models one of the tools used by an assessment.
 * properties supported are
 * <ul>
 * <li>Create date</li>
 * <li>Name</li>
 * <li>Policy</li>
 * <li>Policy code</li>
 * <li>Tool sharing status</li>
 * <li>Update date</li>
 * <li>is build needed?</li>
 * <li>is owned?</li>
 * </ul>
 * <p>Created by Jeff Gaynor<br>
 * on 12/10/14 at  10:55 AM
 */
public class Tool extends SwampThing {
    public Tool(Session session) {
        super(session);
    }

    public Tool(Session session, Map map) {
        super(session, map);
    }

    @Override
    protected SwampThing getNewInstance() {
        return new Tool(getSession());
    }

    @Override
    public String getIDKey() {
        return TOOL_UUID_KEY;
    }

    public String getName() {
        return getString(NAME_KEY);
    }

    public void setName(String name) {
        put(NAME_KEY, name);
    }

    public String getToolSharingStatus() {
        return getString(TOOL_SHARING_STATUS_KEY);
    }

    public void setToolSharingStatus(String toolSharingStatus) {
        put(TOOL_SHARING_STATUS_KEY, toolSharingStatus);
    }

    public boolean isBuildNeeded() {
        return getBoolean(IS_BUILD_NEEDED_KEY);
    }

    public void setBuildNeeded(boolean buildNeeded) {
        put(IS_BUILD_NEEDED_KEY, buildNeeded);
    }

    public String getPolicyCode() {
        return getString(POLICY_CODE_KEY);
    }

    public void setPolicyCode(String policyCode) {
        put(POLICY_CODE_KEY, policyCode);
    }

    public Date getCreateDate() {
        return getDate(CREATE_DATE_KEY);
    }

    public void setCreateDate(Date createDate) {
        put(CREATE_DATE_KEY, createDate);
    }

    public String getPolicy() {
        return getString(POLICY_KEY);
    }

    public void setPolicy(String policy) {
        put(POLICY_KEY, policy);
    }

    public boolean hasPolicy() {
        return getPolicy() != null;
    }

    public Date getUpdateDate() {
        return getDate(UPDATE_DATE_KEY);
    }

    public void setUpdateDate(Date updateDate) {
        put(UPDATE_DATE_KEY, updateDate);
    }

    public boolean isOwned() {
        return getBoolean(IS_OWNED_KEY);
    }

    public void setOwned(boolean isOwned) {
        put(IS_OWNED_KEY, isOwned);
    }

    public List<String> getSupportedPkgTypes() {
        return getAsArray(PACKAGE_TYPE_NAMES);
    }

    public List<String> getSupportedPlatforms() {
        return getAsArray(PLATFORM_NAMES);
    }

    /**
     * A human-readable description of this tool suitable for display.
     *
     * @return
     */
    public String getDescription() {
        return getString(DESCRIPTION_KEY);
    }

    public void setDescription(String description) {
        put(DESCRIPTION_KEY, description);
    }

    /**
     * Is this restricted
     *
     * @return
     */
    public boolean isRestricted() {
        return getBoolean(IS_RESTRICTED_KEY);
    }

    public void setRestricted(boolean isRestricted) {
        put(IS_RESTRICTED_KEY, isRestricted);
    }

    /**
     * The name of the user that created this tool on the server.
     *
     * @return
     */
    public String getCreateUser() {
        return getString(CREATE_USER_KEY);
    }

    public void setCreateUser(String createUser) {
        put(CREATE_USER_KEY, createUser);
    }

    public List<String> getVersionStrings() {
        return getAsArray(VERSION_STRINGS_KEY);
    }

    public List<String> getViewers() {
        if (this.getConversionMap().get(VIEWER_NAMES_KEY) instanceof JSONArray) {
            return (List<String>) this.getConversionMap().get(VIEWER_NAMES_KEY);
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return "Tool[uuid=" + getIdentifier() + ", name=" + getName() + ", sharing status=" + getToolSharingStatus() + ", create date=" + getCreateDate() + "]";
    }


}
