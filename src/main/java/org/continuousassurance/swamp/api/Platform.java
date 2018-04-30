package org.continuousassurance.swamp.api;

import org.continuousassurance.swamp.session.Session;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.continuousassurance.swamp.session.handlers.PlatformHandler.*;

/**
 * This models a platform used in an assessment. The supported properties are
 * <ul>
 *     <li>Name - the human readable name of this platform</li>
 *     <li>sharing status</li>
 *     <li>create date</li>
 * </ul>
 * <p>Created by Jeff Gaynor<br>
 * on 12/10/14 at  10:54 AM
 */
public class Platform extends SwampThing {
    public Platform(Session session) {
        super(session);
    }
    public Platform(Session session, Map map) {
        super(session, map);
    }

    @Override
    protected SwampThing getNewInstance() {
        return new Platform(getSession());
    }

    @Override
    public String getIDKey() {return PLATFORM_UUID_KEY;}
    public String getName(){return getString(NAME_KEY);}
    public void setName(String name){put(NAME_KEY,name);}
    public String getPlatformSharingStatus(){return getString(PLATFORM_SHARING_STATUS_KEY);}
    public void setPlatformSharingStatus(String sharingStatus){put(PLATFORM_SHARING_STATUS_KEY, sharingStatus);}
    public Date getCreateDate(){return getDate(CREATE_DATE_KEY);}
    public void setCreateDate(Date createDate){put(CREATE_DATE_KEY, createDate);}
    public String getDescription(){return getString(DESCRIPTION_KEY);}
    public List<String> getVersions(){return getAsArray(VERSION_STRINGS_KEY);}
/*
    public Date getUpdateDate(){return getDate(UPDATE_DATE_KEY);}
    public void setUpdateDate(Date updateDate){put(UPDATE_DATE_KEY,updateDate);}
*/

    @Override
    public String toString() {
        return "Platform[uuid=" + getIdentifier() + ", name=" + getName() + ", create date=" + getCreateDate() + "]";
    }
}
