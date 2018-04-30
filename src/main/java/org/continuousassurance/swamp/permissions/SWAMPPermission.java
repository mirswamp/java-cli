package org.continuousassurance.swamp.permissions;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 11/18/14 at  3:07 PM
 */
public abstract class SWAMPPermission extends java.security.Permission {
    protected SWAMPPermission(String name) {
        super(name);
    }
}
