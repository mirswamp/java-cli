package org.continuousassurance.swamp.session.util;

import net.sf.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * At this point, we need a utility to convert to and from SWAMP dates, which are in no standard format.
 * <p>Created by Jeff Gaynor<br>
 * on 12/2/14 at  12:11 PM
 */
public class Dates {

    protected static SimpleDateFormat formatter = null;

    public static SimpleDateFormat getFormatter() {
        if (formatter == null) {
            formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        }
        return formatter;
    }

    public static void setFormatter(SimpleDateFormat formatter) {
        Dates.formatter = formatter;
    }

    /**
     * Parses the given raw string as a SWAMP date. Returns a date object or null if this is
     * unparseable.
     * @param rawDate
     * @return
     */
    public static Date toSWAMPDate(String rawDate) {
        if(rawDate == null || rawDate.length() == 0) return null;
        if(rawDate.equals("null")) return null; // In case JSON returns something odd.
        try {
            Date newDate = getFormatter().parse(rawDate);
            return newDate;
        } catch (Throwable t) {
            //t.printStackTrace();
            //DebugUtil.say("Date not parsed.");
            return null;
        }
    }

    public static Date toSWAMPDate(JSONObject json, String key){
        if(json.get(key) instanceof JSONObject){
            JSONObject d = json.getJSONObject(key);
            return toSWAMPDate(d.getString("date"));
        }
        if(json.get(key) instanceof String){
            return toSWAMPDate(json.getString(key));
        }
        if(json.get(key) instanceof Long){
            Date d = new Date();
            d.setTime(json.getLong(key));
            return d;
        }
        return null;
    }
}
