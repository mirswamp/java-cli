package org.continuousassurance.swamp.session.util;

import edu.uiuc.ncsa.security.core.Identifier;
import edu.uiuc.ncsa.security.core.util.BasicIdentifier;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 12/11/14 at  5:07 PM
 */
public class SWAMPIdentifiers {
    public static String IDENTIFIER_CAPUT = "urn:uuid:";

    public static Identifier toIdentifier(String x) {
        return BasicIdentifier.newID(x);
    }


    public static String fromIdentifier(Identifier identifier) {
        if (identifier == null) return null;
        String x = identifier.toString();
        return x;
    }
}
