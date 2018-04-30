package org.continuousassurance.swamp.api;

import org.continuousassurance.swamp.session.Session;

import java.util.Date;
import java.util.Map;

import static org.continuousassurance.swamp.session.handlers.ProjectHandler.*;
import static org.continuousassurance.swamp.session.handlers.UserHandler.USER_UID_KEY;

/**
 * This models a SWAMP project. Supported properties are
 * <ul>
 * <li>Affiliation</li>
 * <li>create date</li>
 * <li>Deactivation date</li>
 * <li>Denial date</li>
 * <li>Description</li>
 * <li>Full name</li>
 * <li>Short name</li>
 * <li>Trial project flag</li>
 * </ul>
 * <p>Created by Jeff Gaynor<br>
 * on 11/18/14 at  3:06 PM
 */
public class Project extends SwampThing {


    @Override
    protected SwampThing getNewInstance() {
        return new Project(getSession());
    }

    @Override
    public String getIDKey() {
        return PROJECT_UID_KEY;
    }


    public Project(Session session) {
        super(session);
    }
    public Project(Session session, Map map) {
        super(session, map);
    }


    public String getDescription() {
        return getString(DESCRIPTION_KEY);
    }

    public void setDescription(String description) {
        put(DESCRIPTION_KEY, description);
    }

    public String getFullName() {
        return getString(FULL_NAME_KEY);
    }

    public void setFullName(String fullName) {
        put(FULL_NAME_KEY, fullName);
    }


    public String getShortName() {
        return getString(SHORT_NAME_KEY);
    }

    public void setShortName(String shortName) {
        put(SHORT_NAME_KEY, shortName);
    }

    public String getAffiliation() {
        return getString(AFFILIATION_KEY);
    }

    public void setAffiliation(String affiliation) {
        put(AFFILIATION_KEY, affiliation);
    }

    public Date getCreateDate() {
        return getDate(CREATE_DATE_KEY);
    }

    public void setCreateDate(Date createDate) {
        put(CREATE_DATE_KEY, createDate);
    }

    public Date getDenialDate() {
        return getDate(DENIAL_DATE_KEY);
    }

    public void setDenialDate(Date denialDate) {
        put(DENIAL_DATE_KEY, denialDate);
    }

    public Date getDeactivationDate() {
        return getDate((DEACTIVATION_DATE_KEY));
    }

    public void setDeactivationDate(Date deactivationDate) {
        put(DEACTIVATION_DATE_KEY, deactivationDate);
    }

    public String getOwnerUUID() {
        return getString(USER_UID_KEY);
    }

    public void setOwnerUUID(String ownerUUID) {
        put(USER_UID_KEY, ownerUUID);
    }

    public boolean isTrialProjectFlag() {
        return getBoolean(TRIAL_PROJECT_FLAG_KEY);
    }

    public void setTrialProjectFlag(boolean trialProjectFlag) {
        put(TRIAL_PROJECT_FLAG_KEY, trialProjectFlag);
    }

    @Override
    public String toString() {
        return "Project[name=" + getFullName() + ",description=" + getDescription() + ", owner id=" + getOwnerUUID() +
                ", create date=" + getCreateDate() + ", uuid=" + getIdentifier() + "]";
    }
}
