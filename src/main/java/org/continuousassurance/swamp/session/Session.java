package org.continuousassurance.swamp.session;

import org.apache.http.cookie.Cookie;
import org.continuousassurance.swamp.exceptions.NoJSONReturnedException;
import org.continuousassurance.swamp.session.handlers.UserHandler;
import org.continuousassurance.swamp.session.util.SWAMPServiceEnvironment;

import java.io.Serializable;
import java.util.HashMap;

/**
 * This represents the session between the client and the server (as opposed to the local runtime
 * environment that is in {@link SWAMPServiceEnvironment}).
 * <p>Created by Jeff Gaynor<br>
 * on 11/18/14 at  3:17 PM
 */
public class Session implements Serializable, Cloneable {
    // private static final long serialVersionUID = -6470090944414208496L;

    public boolean isRequireSecureCookies() {
        return requireSecureCookies;
    }

    public void setRequireSecureCookies(boolean requireSecureCookies) {
        this.requireSecureCookies = requireSecureCookies;
    }

    boolean requireSecureCookies = true;
    public static final String USERNAME_KEY = "username";
    public static final String PASSWORD_KEY = "password";
    public static final String ENDPOINT_LOGIN = "login";
    public static final String ENDPOINT_LOGOUT = "logout";
    public static final String SWAMP_REG_SESSION = "swamp_reg_session";
    public static final String SWAMP_CSA_SESSION = "swamp_csa_session";

    private String userUID = null;
    /**
     * Active session flag, initially false.
     */
    private boolean sessionActive = false;

    private SWAMPHttpClient client;
    private String host;

    private String sessionID;
    private String sessionKey;
    private String csaSessionKey;

    /**
     * The constructor. You must set the {@link SWAMPHttpClient} before using this session.
     *
     * @param host String with the host name.
     */
    public Session(String host) {
        setHost(host);
      //  client = new SWAMPHttpClient(this.host);
        sessionActive = false;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        if (host != null && !host.endsWith("/")) {
            this.host = host + "/";
        } else {
            this.host = host;
        }
    }

    /**
     * Copy constructor: effectively clone this session. This is useful when talking to various SWAMP components which
     * share session state (key and id) but reside on different hosts. Set the host and point at this (active)
     * session.
     *
     * @param host         The name of the host.
     * @param otherSession The other session object that we are copying.
     */
    public Session(String host,  Session otherSession) {
        this(host);
        setState(otherSession);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        Session session = new Session(getHost(), this);
        return session;
    }

    /**
     * Takes the state from another session and imports it to this session.
     *
     * @param otherSession
     */
    protected void setState(Session otherSession) {
        setHost(otherSession.getHost());
        this.setUserUID(otherSession.getUserUID());
        this.setSessionKey(otherSession.getSessionKey());
        this.setSessionID(otherSession.getSessionID());
        this.setClient(otherSession.getClient());
        this.setCsaSessionKey(otherSession.getCsaSessionKey());

    }

    public void logout() {
        // only need to logout if the session is active
        if (sessionActive) {
            try {
                client.rawPost(createURL(ENDPOINT_LOGOUT), null);
            } catch (NoJSONReturnedException x) {
                if (x.getMessage() != null && x.getMessage().contains("SESSION_DESTROYED")) ;
                sessionActive = false;
            }
        }
    }


    public void logon(String username, String password) {
        if (sessionActive) {
            throw new IllegalStateException("Error: There is already and active logon. Please log out then try again.");
        }
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put(USERNAME_KEY, username);
        map.put(PASSWORD_KEY, password);
        MyResponse myResponse = getClient().rawPost(createURL(ENDPOINT_LOGIN), map);
        for (Cookie cookie : myResponse.cookies) {
            if (isRequireSecureCookies() && !cookie.isSecure()) {
                throw new SecurityException("Error: cookie named \"" + cookie.getName() + "\" is not secure. Logon aborted");
            }

            //DebugUtil.say(this, "response cookie:" + cookie.getName() + "+" + cookie.getValue());
            if (cookie.getName().equals(SWAMP_REG_SESSION)) {
                setSessionKey(cookie.getValue());
            } else {
                if (cookie.getName().equals(SWAMP_CSA_SESSION)) {
                    setCsaSessionKey(cookie.getValue());
                } else {
                    setSessionID(cookie.getValue());
                }
            }
        }
        setUserUID(myResponse.json.getString(UserHandler.USER_UID_KEY));
        sessionActive = true;
    }

    public SWAMPHttpClient getClient() {
        if (client == null) {
            client = new SWAMPHttpClient(this.host);
        }
        return client;
    }

    public void setClient(SWAMPHttpClient client) {
        this.client = client;
    }

    /**
     * Given an endpoint for this host, return a valid url.
     *
     * @param endpoint
     * @return
     */
    public String createURL(String endpoint) {
        if (host == null || host.length() == 0) {
            throw new IllegalStateException("Error: no host set for this session");
        }
        return host + endpoint;
    }

    public boolean isLoggedOn() {
        return client != null;
    }

    public boolean isValid() {
        return sessionID != null && sessionKey != null;
    }

    public String getSessionID() {
        return sessionID;
    }

    public void setSessionID(String sessionID) {
        this.sessionID = sessionID;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }


    public String getCsaSessionKey() {
        return csaSessionKey;
    }

    public void setCsaSessionKey(String csaSessionKey) {
        this.csaSessionKey = csaSessionKey;
    }


    public String getUserUID() {
        return userUID;
    }

    public void setUserUID(String userUID) {
        this.userUID = userUID;
    }


}
