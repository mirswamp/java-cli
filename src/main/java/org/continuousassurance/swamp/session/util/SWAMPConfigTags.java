package org.continuousassurance.swamp.session.util;

import edu.uiuc.ncsa.security.core.configuration.ConfigurationTags;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 4/11/16 at  10:44 AM
 */
public interface SWAMPConfigTags extends ConfigurationTags {


    /**
     * this is the tag to look for that holds a SWAMP configuration.
     */
    public static final String SWAMP_COMPONENT_NAME = "swamp";
    public static final String SERVER_ADDRESSES_TAG = "addresses";
    public static final String FRONTEND_ADDRESS_TAG = "server";
    public static final String RWS_ADDRESS_TAG = "rws";
    public static final String CSA_ADDRESS_TAG = "csa";
    public static final String HEADERS_TAG = "headers";
    public static final String REFERER_HEADER_TAG = "referer";
    public static final String ORIGIN_HEADER_TAG = "origin";
    public static final String HOST_HEADER_TAG = "host";
    public static final String USERNAME_TAG = "username";
    public static final String PASSWORD_TAG = "password";
    public static final String PROXY_TAG = "proxy";
    public static final String PROXY_PORT_TAG = "port";
    public static final String PROXY_HOST_TAG = "host";
    public static final String PROXY_SCHEME_TAG = "scheme";


}
