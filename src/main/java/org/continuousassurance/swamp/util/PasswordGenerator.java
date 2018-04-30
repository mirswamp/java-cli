package org.continuousassurance.swamp.util;

import org.apache.commons.lang.ArrayUtils;

import java.security.SecureRandom;
import java.util.ArrayList;

import static org.continuousassurance.swamp.util.DebugUtil.intersection;

/**
 * This is a class that will generate a random SWAMP password according to the spec.
 * This means that a password must have at least one of each of 9 characters long<br/>
 * <ul>
 * <li>lower case</li>
 * <li>upper case</li>
 * <li>number</li>
 * <li>special character</li>
 * </ul>
 * ~ ` ! @ # $ % ^ & * ( ) _ + = | \ [ ] { } ? / . , < > ; : ' " and space “ “
 * <p/>
 * <p>Created by Jeff Gaynor<br>
 * on 10/8/14 at  3:18 PM
 */
public class PasswordGenerator {
    protected static void init() {
        if (isInit) return;
        String s = "abcdefghijklmnopqrstuvwxyz";
        LOWER_CASE = ArrayUtils.toObject(s.toCharArray());
        UPPER_CASE = ArrayUtils.toObject(s.toUpperCase().toCharArray());
        s = "0123456789";
        NUMBERS = ArrayUtils.toObject(s.toCharArray());
        // %<>&
        s = "~`!@#$^*()_+=|\\[]{}?/.,;:'\" ";
        SPECIAL_CHARS = ArrayUtils.toObject(s.toCharArray());
        CHARACTER_CLASSES = new Object[4];
        CHARACTER_CLASSES[0] = LOWER_CASE;
        CHARACTER_CLASSES[1] = UPPER_CASE;
        CHARACTER_CLASSES[2] = NUMBERS;
        CHARACTER_CLASSES[3] = SPECIAL_CHARS;
        isInit = true;
    }

    public static Character[] specialCharacters(){
        init();
        // Do not return the set of special characters, just a copy since otherwise someone might
        // inadvertently change it.
        Character[] x = new Character[SPECIAL_CHARS.length];
        System.arraycopy(SPECIAL_CHARS, 0, x, 0, x.length);
        return x;
    }
    static Character[] LOWER_CASE;
    static Character[] UPPER_CASE;
    static Character[] NUMBERS;
    static Character[] SPECIAL_CHARS;
    static Object[] CHARACTER_CLASSES;
    static boolean isInit = false;

    /**
     * Create a password of the given length using <b>all</b> password character classes.
     * @param length
     * @return
     */
    public static String generate(int length) {
              return generate(length, 4);
    }
    public static String generate(int length, int characterClassCount) {
        init();
        ArrayList<Character> arrayList = new ArrayList<Character>();

        int[] partion = partition(length, characterClassCount);
        for (int i = 0; i < characterClassCount; i++) {
            arrayList.addAll(generate((Character[]) CHARACTER_CLASSES[i], partion[i]));
        }
        java.util.Collections.shuffle(arrayList);
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < arrayList.size(); i++) {
            sb.append(arrayList.get(i));
        }
        return sb.toString();
    }

    /**
     * Partitions the integer x into the number of partitions. A partition
     * meaning that the sum of the resulting integers = x. E.g. to partition
     * the number 9 into 4 partitions you might get 1,3,2,3 or perhaps 6,1,1,1
     *
     * @param x
     * @param partitions
     * @return
     */
    protected static int[] partition(int x, int partitions) {

        int[] out = new int[partitions];
        for (int i = 0; i < out.length; i++) {
            out[i] = 1; // required since need at least one in each slot
        }
        int seed = x - partitions;
        for (int i = 0; i < out.length - 1; i++) {
            int r = secureRandom.nextInt(seed);
            out[i] = out[i] + r;
            seed = seed - r;
        }
        out[out.length - 1] = seed + out[out.length - 1];
        return out;

    }

    public static SecureRandom secureRandom = new SecureRandom();

    /**
     * Given a character class and an integer, n, get n random characters from the class.
     *
     * @param chars
     * @param n
     * @return
     */
    protected static ArrayList<Character> generate(Character[] chars, int n) {
        ArrayList<Character> sb = new ArrayList<Character>();
        for (int i = 0; i < n; i++) {
            sb.add(chars[secureRandom.nextInt(chars.length)]);
        }
        return sb;
    }

    public static void main(String[] args) {
        for (int i = 0; i < 5; i++) {
            System.out.println("pwd(9,4)=" + generate(9, 4));
            System.out.println("pwd(10,3)=" + generate(10, 3));
        }
        String x = generate(10, 3);
        String y = generate(10, 3);
        System.out.println("generating intersection of passwords " + x + ", and " + y);
        Character[] zz = intersection(x, y);

        //zz = intersection(zz, x);
        if (zz.length == 0) {
            System.out.println("(none)");
        } else {
            for (Character q : zz) {
                System.out.print(q);
            }
        }
    }
}
