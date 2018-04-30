package org.continuousassurance.swamp.session.handlers;

import org.continuousassurance.swamp.api.SwampThing;
import org.continuousassurance.swamp.session.MyResponse;
import org.continuousassurance.swamp.session.Session;
import org.continuousassurance.swamp.session.util.ConversionMapImpl;
import net.sf.json.JSONObject;

import java.util.List;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 3/8/16 at  3:14 PM
 */
public class AdministrationHandler extends AbstractHandler {
    public static String USER_KEY = "userUID";      // for use retrieving the user uid from the map.

    public AdministrationHandler(Session session) {
        super(session);
    }

    @Override
    protected SwampThing fromJSON(JSONObject json) {
        return null;
    }

    @Override
    public List getAll() {
        return null;
    }


    @Override
    public String getURL() {
        return createURL("admins");
    }

    @Override
    public SwampThing create(ConversionMapImpl map) {
        if (!map.containsKey(USER_KEY)) {
            throw new IllegalArgumentException("Error: You must supply the user id in the map using the key \"" + USER_KEY + "\"");
        }
        MyResponse mr = getClient().rawPost(getURL() + "/" + map.getString(USER_KEY), map);
        SwampThing p = fromJSON(mr.json);
        return p;

    }
}
