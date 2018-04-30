package org.continuousassurance.swamp.util;

import org.apache.commons.lang.ArrayUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;

import java.util.HashSet;

/**
 * Utilities used for debugging.
 * <p>Created by Jeff Gaynor<br>
 * on 10/8/14 at  11:21 AM
 */
public class DebugUtil {
    public static boolean isDebugOff() {
        return debugOff;
    }

    /**
     * Set this to false to globally turn off all debug prints.
     * @param debugOff
     */
    public static void setDebugOff(boolean debugOff) {
        DebugUtil.debugOff = debugOff;
    }

    static boolean debugOff = false;
    public static void say(Object x) {
        if(debugOff) return;
        System.out.println(x.toString());
    }
    public static void say(String x) {
        if(debugOff) return;
        System.out.println(x);
    }


    public static void say(Object obj, Object x) {
        if(debugOff) return;
        say(obj.getClass().getSimpleName() + x);
    }


    public static boolean isJSONStringEmpty(String x) {
        return x == null || 0 == x.length() || "null".equals(x); // needed if, say, JSON returns the string "null".
    }

    public static Character[] intersection(Character[] x, String y) {
        return intersection(x, ArrayUtils.toObject(y.toCharArray()));
    }

    public static Character[] intersection(String x, String y) {
        return intersection(ArrayUtils.toObject(x.toCharArray()), ArrayUtils.toObject(y.toCharArray()));
    }

    public static Character[] intersection(Character[] x, Character[] y) {
        HashSet<Character> a = new HashSet<Character>(); // cuts out duplicates.
        for (int i = 0; i < x.length; i++) {
            for (int j = 0; j < y.length; j++) {
                if (x[i].equals(y[j])) {
                    a.add(x[i]);
                }
            }
        }
        Character[] z = new Character[a.size()];
        a.toArray(z);
        return z;
    }

    public static void printPost(HttpPost post) {
        printPost(post, null);
    }

    public static void printPost(HttpPost post, HttpEntity entity) {
        if(debugOff) return;

        try {
            say(""); // make a blank line so this is more readable.
            Header[] headers = post.getAllHeaders();
            String content = EntityUtils.toString(entity);

            say(post.toString());
            if (headers != null && 0 < headers.length) {
                for (Header header : headers) {
                    say(header.getName() + ": " + header.getValue());
                }
            } else {
                say("(no headers)");
            }
            if (entity == null) {
                say("No body to post");
            } else {
                say("=========== content");
                say(content);
                say("=========== end content");
            }
        } catch (Throwable t) {
            t.printStackTrace();
            say("Was unable to print the post");
        }
    }
}
