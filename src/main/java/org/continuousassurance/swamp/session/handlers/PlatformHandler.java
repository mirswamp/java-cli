package org.continuousassurance.swamp.session.handlers;

import net.sf.json.JSONObject;
import org.continuousassurance.swamp.api.Platform;
import org.continuousassurance.swamp.session.MyResponse;
import org.continuousassurance.swamp.session.Session;
import org.continuousassurance.swamp.session.util.ConversionMapImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 12/10/14 at  2:18 PM
 */
public class PlatformHandler<T extends Platform> extends AbstractHandler<T> {
    public PlatformHandler(Session session) {
        super(session);
    }

    public static final String PLATFORM_UUID_KEY = "platform_uuid";
    public static final String NAME_KEY = "name";
    public static final String DESCRIPTION_KEY = "description";
    public static final String VERSION_STRINGS_KEY = "version_strings";
    public static final String PLATFORM_SHARING_STATUS_KEY = "platform_sharing_status";
    public static final String CREATE_DATE_KEY = "create_date";
    public static final String UPDATE_DATE_KEY = "update_date";

    @Override
    public List<T> getAll() {
        String url = createURL("platforms/public");
        MyResponse mr = getClient().rawGet(url, null);
        ArrayList<T> platforms = new ArrayList<>();
        for (int i = 0; i < mr.jsonArray.size(); i++) {
            JSONObject json = mr.jsonArray.getJSONObject(i);
            platforms.add(fromJSON(json));
        }
        return platforms;
    }

    protected T fromJSON(JSONObject json) {
        T platform = (T) new Platform(getSession());
        ConversionMapImpl map = new ConversionMapImpl();
        String[] sAttrib = {NAME_KEY, PLATFORM_SHARING_STATUS_KEY, DESCRIPTION_KEY};
        String[] uAttrib = {PLATFORM_UUID_KEY};
        String[] dAttrib = {CREATE_DATE_KEY, UPDATE_DATE_KEY};
        String[] aAttrib = {VERSION_STRINGS_KEY};
        setAttributes(map, sAttrib, json, DATA_TYPE_STRING);
        setAttributes(map, dAttrib, json, DATA_TYPE_DATE);
        setAttributes(map, uAttrib, json, DATA_TYPE_IDENTIFIER);
        setAttributes(map, aAttrib, json, DATA_TYPE_ARRAY);
        platform.setConversionMap(map);
        return platform;
    }

    public Platform find(String name) {
        for (Platform p : getAll()) {
            if (p.getName().equals(name)) {
                return p;
            }
        }
        return null;
    }

    @Override
    public String getURL() {
        return createURL("platforms");
    }
}
