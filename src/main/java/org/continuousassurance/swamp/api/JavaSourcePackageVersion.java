package org.continuousassurance.swamp.api;

import org.continuousassurance.swamp.session.Session;

import java.util.Map;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 9/2/15 at  3:59 PM
 */
public class JavaSourcePackageVersion extends PackageVersion {
    public JavaSourcePackageVersion(Session session) {
        super(session);
    }
    public JavaSourcePackageVersion(Session session, Map map) {
        super(session, map);
    }

    @Override
    protected SwampThing getNewInstance() {
        return new JavaSourcePackageVersion(getSession());
    }
}
