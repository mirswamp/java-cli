package org.continuousassurance.swamp.util;

import org.apache.commons.codec.binary.Base64;

import java.util.Random;

/**
 * Utilities used by any and all tests.
 * <p>Created by Jeff Gaynor<br>
 * on 11/26/14 at  10:53 AM
 */
public class TestUtil {
    public static int randomStringLength = 8; // default length for random strings

    public static Random getRandom() {
        if (random == null) {
            random = new Random();
        }
        return random;
    }

    static Random random;

    /**
     * Creates a random string of characters of the given length.
     *
     * @param length
     * @return
     */
    public static String getRandomString(int length) {
        // so approximate how long the result will be and add in (at most) 2 characters.
        byte[] bytes = new byte[(int) (Math.round(Math.ceil(length * .75)) + 1)];
        getRandom().nextBytes(bytes);
        // Have to be careful to use only URL safe encoding or random errors can start occurring,
        // especially if using these to make other urls!
        return Base64.encodeBase64URLSafeString(bytes).substring(0, length);
    }

    /**
     * Creates a random string of the default length {@value #randomStringLength}.
     *
     * @return
     */
    public static String getRandomString() {
        return getRandomString(randomStringLength);
    }





}
