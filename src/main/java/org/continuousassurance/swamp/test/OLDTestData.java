package org.continuousassurance.swamp.test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Every test has data. This is a simple hashmap that contains it. Also included is whether the test
 * has been disabled in the test or not. Each test should ideally check if the test is enabled
 * and simply skip any test that is disabled.
 * <p>Created by Jeff Gaynor<br>
 * on 9/6/14 at  2:07 PM
 */
public class OLDTestData extends HashMap<String, Object> {
    public static final String TEST_ENABLE_KEY = "test:enabled";

    public String getString(String key) {
        return get(key).toString();
    }

    public List<String> getList(String key) {
        Object x = get(key);
        if (x instanceof List) {
            return (List<String>) x;
        }
        LinkedList<String> list = new LinkedList<>();
        list.add(x.toString());
        return list;
    }

    public boolean isEnabled() {
        return (Boolean) get(TEST_ENABLE_KEY);
    }


}
