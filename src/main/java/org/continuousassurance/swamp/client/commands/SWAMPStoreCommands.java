package org.continuousassurance.swamp.client.commands;

import edu.uiuc.ncsa.security.core.Store;
import edu.uiuc.ncsa.security.core.util.MyLoggingFacade;
import edu.uiuc.ncsa.security.util.cli.StoreCommands;
import org.apache.commons.lang.WordUtils;

import java.util.List;
import java.util.StringTokenizer;

/**
 * Superclass for commands in the SWAMP. Mostly a place to accumulate useful shared code.
 * <p>Created by Jeff Gaynor<br>
 * on 4/27/17 at  1:53 PM
 */
public abstract class SWAMPStoreCommands extends StoreCommands {
    public int WRAP_LENGTH = 80;
    /**
     * This is used in displaying the long format between the attribute and its value, e.g.<br/>
     * <code>Name:xxxxxxxx</code><br/>
     * The ":" is this value. This enforces consistency between different sets of commands
     */
    public static String ATTRIBUTE_DELIMITER = ":";


    public SWAMPStoreCommands(MyLoggingFacade logger, String defaultIndent, Store store) {
        super(logger, defaultIndent, store);
    }

    public SWAMPStoreCommands(MyLoggingFacade logger, Store store) {
        super(logger, store);
    }

    protected void printList(String title, List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }

        String blanks = "                                                                      ";
        while (blanks.length() < title.length()) {
            blanks = blanks + blanks;
        }
        if (values.size() == 1) {
            sayi(title + ATTRIBUTE_DELIMITER + values.get(0));
            return;
        }
        // This is longer list, so print it out
        sayi(title + ATTRIBUTE_DELIMITER);

        for (String v : values) {
            sayi(INDENT + v); // add and extra indent.
        }
    }

    /**
     * Prints an attribute and its value if the value is non trivial.
     * This indents.
     *
     * @param attribute
     * @param value
     */
    protected void printAttributei(String attribute, String value) {
        printAttributei(attribute, value, false);
    }

    protected void printAttributei(String attribute, String value, boolean wrapText) {

        if (value != null && 0 < value.length()) {
            if (wrapText) {
                String start = attribute + ATTRIBUTE_DELIMITER + value;
                if (start.length() <= WRAP_LENGTH) {
                    sayi(start); // nothing to do
                }
                String target = WordUtils.wrap(start, WRAP_LENGTH);
                StringTokenizer tokenizer = new StringTokenizer(target, "\n");
                boolean isFirstPass = true;
                while (tokenizer.hasMoreTokens()) {
                    if (isFirstPass) {
                        sayi(tokenizer.nextToken());
                        isFirstPass = false;
                    } else {
                        sayi(INDENT + tokenizer.nextToken());
                    }
                }
            } else {
                sayi(attribute + ATTRIBUTE_DELIMITER + value);
            }
        }

    }

    /**
     * Prints an attribute and its value if the value is non trivial.
     * This does NOT indent.
     *
     * @param attribute
     * @param value
     */
    protected void printAttribute(String attribute, String value) {
        if (value != null && 0 < value.length()) {
            say(attribute + ATTRIBUTE_DELIMITER + value);
        }
    }

    /**
     * This will only print out a string if the string is non-trivial.
     * @param x
     */

}
