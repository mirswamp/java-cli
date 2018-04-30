package org.continuousassurance.swamp.api;

import org.continuousassurance.swamp.session.Session;

import java.util.Map;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 9/2/15 at  4:00 PM
 */
public class JavaBytecodePackageVersion extends PackageVersion {
    public JavaBytecodePackageVersion(Session session) {
        super(session);
    }
    public JavaBytecodePackageVersion(Session session, Map map) {
        super(session, map);
    }

    @Override
    protected SwampThing getNewInstance() {
        return new JavaBytecodePackageVersion(getSession());
    }
}
