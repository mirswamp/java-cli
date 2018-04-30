package org.continuousassurance.swamp.session.util;

import edu.uiuc.ncsa.security.core.Identifier;
import edu.uiuc.ncsa.security.storage.data.ConversionMap;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;

/**

 * A {@link HashMap} that has custom built-in conversion methods. You put objects of the required type
 * into this map and retrieve then with the appropriate getter (which casts or whatever is needed).
 * <p>Created by Jeff Gaynor<br>
 * on 12/2/14 at  2:37 PM
 */
public class ConversionMapImpl extends HashMap<String, Object> implements ConversionMap<String, Object> {
    public static final boolean BOOLEAN_DEFAULT = false;
    public static final long LONG_DEFAULT = 0L;


    @Override
    public Date getDate(java.lang.String key) {
        return (Date) get(key);
    }

    @Override
    public boolean getBoolean(java.lang.String key) {
        Object x = get(key);
        if(x == null) return BOOLEAN_DEFAULT;
        return (boolean) x;
    }

    @Override
    public long getLong(java.lang.String key) {
        if(get(key) == null) return LONG_DEFAULT;
        return (long) get(key);
    }

    @Override
    public java.lang.String getString(String key) {
        Object x = get(key);
        if (x == null) return null;
        return x.toString();
    }


    @Override
    public Identifier getIdentifier(String key) {
        return (Identifier) get(key);
    }


    @Override
    public URI getURI(java.lang.String key) {
        Object x = get(key);
        if (x == null) return null;
        return URI.create(x.toString());
    }

    @Override
    public byte[] getBytes(java.lang.String key) {
        Object x = get(key);
        if (x.getClass().isArray()) {
            // try to return it...
            return (byte[]) x;
        }
        return null;
    }
}
