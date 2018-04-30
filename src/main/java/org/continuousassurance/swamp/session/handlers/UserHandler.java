package org.continuousassurance.swamp.session.handlers;

import org.continuousassurance.swamp.api.User;
import org.continuousassurance.swamp.session.MyResponse;
import org.continuousassurance.swamp.session.Session;
import org.continuousassurance.swamp.session.util.ConversionMapImpl;
import net.sf.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 11/20/14 at  10:08 AM
 */
public class UserHandler<T extends User> extends AbstractHandler<T> {

    public static final String USER_UID_KEY = "user_uid";
    public static final String FIRST_NAME_KEY = "first_name";
    public static final String LAST_NAME_KEY = "last_name";
    public static final String PREFERRED_NAME_KEY = "preferred_name";
    public static final String USERNAME_KEY = "username";
    public static final String EMAIL_KEY = "email";
    public static final String ADDRESS_KEY = "address";
    public static final String PHONE_KEY = "phone";
    public static final String AFFILIATION_KEY = "affiliation";
    public static final String EMAIL_VERIFIED_KEY = "email_verified_flag";
    public static final String ACCOUNT_ENABLED_KEY = "enabled_flag";
    public static final String OWNER_KEY = "owner_flag";
    public static final String SSH_ACCESS_KEY = "ssh_access_flag";
    public static final String ADMIN_ACCESS_KEY = "admin_flag";
    public static final String LAST_URL_KEY = "last_url";
    public static final String CREATE_DATE_KEY = "create_date";
    public static final String UPDATE_DATE_KEY = "update_date";

    public static final String USERS_CURRENT = "users/current";
    public static final String OLD_PASSWORD_KEY = "old_password";
    public static final String NEW_PASSWORD_KEY = "new_password";


    public UserHandler(Session session) {
        super(session);
    }

    public void changePassword(String oldPassword, String newPassword) throws UnsupportedEncodingException {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put(OLD_PASSWORD_KEY, oldPassword);
        map.put(NEW_PASSWORD_KEY, newPassword);
        MyResponse myResponse = getClient().rawPut(createURL("users/" + getSession().getUserUID() + "/change-password"), map);
    }


    public User getCurrentUser() {
        if (getSession() == null || getSession().getUserUID() == null) {
            throw new IllegalStateException("Error: There is no current session or user.");
        }
        MyResponse myResponse = getClient().rawGet(createURL(USERS_CURRENT), null);
        if (myResponse.getHttpResponseCode() == 200){
        	return fromJSON(myResponse.json);
        }else {
        	return null;
        }
    }


    @Override
    public List<T> getAll() {
        return new ArrayList<T>();
    }

    @Override
    protected T fromJSON(JSONObject json) {
        ConversionMapImpl map = new ConversionMapImpl();
        // Too many user attributes.
        String[] sAttrib = {FIRST_NAME_KEY, LAST_NAME_KEY, PREFERRED_NAME_KEY, USERNAME_KEY, EMAIL_KEY, ADDRESS_KEY, PHONE_KEY, AFFILIATION_KEY, LAST_URL_KEY};
        String[] bAttrib = {EMAIL_VERIFIED_KEY, ACCOUNT_ENABLED_KEY, OWNER_KEY, SSH_ACCESS_KEY, ADMIN_ACCESS_KEY};
        String[] dAttrib = {CREATE_DATE_KEY, UPDATE_DATE_KEY};
        String[] uAttrib = {USER_UID_KEY};

        setAttributes(map, uAttrib, json, DATA_TYPE_IDENTIFIER);
        setAttributes(map, sAttrib, json, DATA_TYPE_STRING);
        setAttributes(map, dAttrib, json, DATA_TYPE_DATE);
        setAttributes(map, bAttrib, json, DATA_TYPE_BOOLEAN);
        T user = (T) new User(getSession());
        user.setConversionMap(map);
        return user;
    }

    @Override
    public String getURL() {
        return createURL("users");
    }

    public User getByUsername(String username){
     HashMap<String, Object> map = new HashMap<>();
        map.put("username", username);
        String url = getURL() + "/email/requestUsername";
        MyResponse mr = getClient().rawPost(url, map);
        return fromJSON(mr.json);
    }
}
