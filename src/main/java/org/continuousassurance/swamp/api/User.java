package org.continuousassurance.swamp.api;

import org.continuousassurance.swamp.session.Session;
import org.continuousassurance.swamp.session.handlers.UserHandler;

import java.util.Date;
import java.util.Map;

/**
 * This models a user in the swamp.
 * <p>Created by Jeff Gaynor<br>
 * on 11/18/14 at  3:06 PM
 */
public class User extends SwampThing {
    public User(Session session) {
        super(session);
    }
    public User(Session session, Map map) {
        super(session, map);
    }

    @Override
    protected SwampThing getNewInstance() {
        return new User(getSession());
    }

    @Override
    public String getIDKey() {return UserHandler.USER_UID_KEY;}
    public String getFirstName(){return getString(UserHandler.FIRST_NAME_KEY);}
    public void setFirstName(String firstName){put(UserHandler.FIRST_NAME_KEY, firstName);}
    public String getLastName(){return getString(UserHandler.LAST_NAME_KEY);}
    public void setLastName(String lastName){put(UserHandler.LAST_NAME_KEY, lastName);}
    public String getPreferredName(){return getString(UserHandler.PREFERRED_NAME_KEY);}
    public void setPreferredName(String preferredName){put(UserHandler.PREFERRED_NAME_KEY, preferredName);}
    public String getEmail(){return getString(UserHandler.EMAIL_KEY);}
    public void setEmail(String email){put(UserHandler.EMAIL_KEY, email);}
    public String getAddress(){return getString(UserHandler.ADDRESS_KEY);}
    public void setAddress(String address){put(UserHandler.ADDRESS_KEY, address);}
    public String getPhone(){return getString(UserHandler.PHONE_KEY);}
    public void setPhoneName(String phone){put(UserHandler.PHONE_KEY, phone);}
    public String getAffiliation(){return getString(UserHandler.AFFILIATION_KEY);}
    public void setAffiliation(String affiliation){put(UserHandler.AFFILIATION_KEY, affiliation);}
    public boolean isEmailVerified(){return getBoolean(UserHandler.EMAIL_VERIFIED_KEY);}
    public void setEmailVerified(boolean emailVerified){put(UserHandler.EMAIL_VERIFIED_KEY, emailVerified);}
    public boolean isAccountEnabled(){return getBoolean(UserHandler.ACCOUNT_ENABLED_KEY);}
    public void setAccountEnabled(boolean accountEnabled){put(UserHandler.ACCOUNT_ENABLED_KEY, accountEnabled);}
    public boolean isOwner(){return getBoolean(UserHandler.OWNER_KEY);}
    public void setOwner(boolean owner){put(UserHandler.OWNER_KEY, owner);}
    public boolean hasSSHAccess(){return getBoolean(UserHandler.SSH_ACCESS_KEY);}
    public void setSSHAccess(boolean sshAccess){put(UserHandler.SSH_ACCESS_KEY, sshAccess);}
    public boolean hasAdminAccess(){return getBoolean(UserHandler.ADMIN_ACCESS_KEY);}
    public void setAdminAccess(boolean adminAccess){put(UserHandler.ADMIN_ACCESS_KEY, adminAccess);}
    public String getLastURL(){return getString(UserHandler.LAST_URL_KEY);}
    public void setLastURL(String lastURL){put(UserHandler.LAST_URL_KEY, lastURL);}
    public Date getCreateDate(){return getDate(UserHandler.CREATE_DATE_KEY);}
    public void setCreateDate(Date date){put(UserHandler.CREATE_DATE_KEY, date);}
    public Date getUpdateDate(){return getDate(UserHandler.UPDATE_DATE_KEY);}
    public void setUpdateDate(Date date){put(UserHandler.UPDATE_DATE_KEY, date);}

    @Override
    public String toString() {
        return "User[uuid=" + getIdentifier() + ", last name=" + getLastName() + ", first name=" + getFirstName() + ", login name=" +
                getPreferredName() +
                ", email=" + getEmail() +
                ", phone=" + getPhone() +
                ", email verified? " + isEmailVerified() +
                ", account enabled? " + isAccountEnabled() +
                "]";
    }
}
