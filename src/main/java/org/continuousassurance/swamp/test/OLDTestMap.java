package org.continuousassurance.swamp.test;

import java.util.HashMap;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 9/9/14 at  2:59 PM
 */
public class OLDTestMap extends HashMap<String, OLDTestData> {
    public static final String SYSTEM_KEY = "swamp-test:SYSTEM";

    public String getRWSAddress() {
        if (!containsKey(SYSTEM_KEY)) return null;
        Object x = get(SYSTEM_KEY).get(TestConfigReader.RWS_ADDRESS);
        if (x == null) return null;
        return x.toString();
    }

    public String getRefererHeader(){
        if (!containsKey(SYSTEM_KEY)) return null;
        Object x = get(SYSTEM_KEY).get(TestConfigReader.REFERER_HEADER);
        if (x == null) return null;
        return x.toString();
    }

    public String getOriginHeader(){
        if (!containsKey(SYSTEM_KEY)) return null;
        Object x = get(SYSTEM_KEY).get(TestConfigReader.ORIGIN_HEADER);
        if (x == null) return null;
        return x.toString();
    }

    public String getHostHeader(){
        if (!containsKey(SYSTEM_KEY)) return null;
        Object x = get(SYSTEM_KEY).get(TestConfigReader.HOST_HEADER);
        if (x == null) return null;
        return x.toString();
    }
    public String getCSAAdress() {
        if (!containsKey(SYSTEM_KEY)) return null;
        Object x = get(SYSTEM_KEY).get(TestConfigReader.CSA_ADDRESS);
        if (x == null) return null;
        return x.toString();
    }

    public boolean hasRWSAddress() {
        return getRWSAddress() != null;
    }

    public boolean hasCSAAddress() {
        return getCSAAdress() != null;
    }
}
