package org.continuousassurance.swamp.api;

import org.continuousassurance.swamp.session.Session;

import java.util.Map;

/**
 * A package version for C source code.
 * <p>Created by Jeff Gaynor<br>
 * on 9/2/15 at  3:59 PM
 */
public class CSourcePackageVersion extends PackageVersion {
    public CSourcePackageVersion(Session session) {
        super(session);
    }
    public CSourcePackageVersion(Session session, Map map) {
        super(session, map);
    }

    @Override
    protected SwampThing getNewInstance() {
        return new CSourcePackageVersion(getSession());
    }
}
