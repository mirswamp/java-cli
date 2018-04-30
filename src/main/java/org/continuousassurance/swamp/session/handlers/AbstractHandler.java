package org.continuousassurance.swamp.session.handlers;

import edu.uiuc.ncsa.security.core.Identifier;
import edu.uiuc.ncsa.security.core.util.Iso8601;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import org.continuousassurance.swamp.api.SwampThing;
import org.continuousassurance.swamp.session.MyResponse;
import org.continuousassurance.swamp.session.SWAMPHttpClient;
import org.continuousassurance.swamp.session.Session;
import org.continuousassurance.swamp.session.util.ConversionMapImpl;
import org.continuousassurance.swamp.session.util.Dates;
import org.continuousassurance.swamp.session.util.SWAMPIdentifiers;

import java.util.Collection;
import java.util.Date;

/**
 * Top level handler class. A handler is a class that is responsible for handling the interactions with the
 * server. It unmarshalls (for instance) raw JSON into objects or marshalls objects into their elements. In
 * general there are <b>no</b> user serviceable parts in a handler, meaning that this is the layer of indirection
 * that acts as a facade to the server, presenting the user of this API (=developer) with a uniform coding
 * experience.
 * <p>Created by Jeff Gaynor<br>
 * on 11/20/14 at  10:09 AM
 */
public abstract class AbstractHandler<T extends SwampThing> {
    public static final int DATA_TYPE_IDENTIFIER = 1;
    public static final int DATA_TYPE_STRING = 2;
    public static final int DATA_TYPE_DATE = 3;
    public static final int DATA_TYPE_BOOLEAN = 4;
    public static final int DATA_TYPE_ARRAY = 5;

    public AbstractHandler(Session session) {
        this.session = session;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    Session session;


    protected SWAMPHttpClient getClient() {
        if (session == null) {
            throw new IllegalStateException("Error: No session has been set for this handler");
        }
        return session.getClient();
    }

    protected String createURL(String endpoint) {
        return getSession().createURL(endpoint);
    }

    public abstract Collection<T> getAll();

    /**
     * The base URL for this component.
     *
     * @return
     */
    public abstract String getURL();

    public boolean delete(Identifier identifier) {
        String url = getURL() + "/" + identifier.toString();
        try {
            getClient().delete(url);
        } catch (Throwable t) {
            return false;
        }
        return true;

    }

    public boolean delete(SwampThing a) {
        return delete(a.getIdentifier());
    }

    /**
     * Get this thing by id. Since IDs are not actually scoped, you must ask a handler for the specific item.
     *
     * @param identifier
     * @return
     */
    public T get(Identifier identifier) {
        String url = getURL() + "/" + SWAMPIdentifiers.fromIdentifier(identifier);
        MyResponse mr = getClient().rawGet(url, null);
        if (mr.json == null) return null;
        return fromJSON(mr.json);
/*
        for (T pkg : getAll()) {
            DebugUtil.say(this, "current uuid=" + pkg.getIdentifier() + ", requested uuid=" + identifier);
            if (pkg.getIdentifier().equals(identifier)) {
                return pkg;
            }
        }
        return null;
*/
    }

    /**
     * A utility to get long lists of attributes. Simply put the keys into an array of string and
     * pass that with the map to this method with the data type.
     *
     * @param map
     * @param keys
     * @param json
     * @param dataType
     */
    protected void setAttributes(ConversionMapImpl map, String[] keys, JSONObject json, int dataType) {
        for (String key : keys) {
            try {
                switch (dataType) {
                    case DATA_TYPE_IDENTIFIER:
                        String x = json.getString(key);
                        if (x != null) {
                            map.put(key, SWAMPIdentifiers.toIdentifier(x));
                        }
                        break;
                    case DATA_TYPE_BOOLEAN:
                        if (json.get(key) instanceof Boolean) {
                            map.put(key, json.getBoolean(key));
                        }
                        if (json.get(key) instanceof Integer) {
                            map.put(key, json.getInt(key) != 0);
                        }
                        break;
                    case DATA_TYPE_DATE:
                        map.put(key, Dates.toSWAMPDate(json, key));
                        break;
                    case DATA_TYPE_ARRAY:
                        //map.put(key, json.getJSONArray(key).toString(0));
                        map.put(key, json.getJSONArray(key));
                        break;
                    case DATA_TYPE_STRING:
                    default:
                        Object object = json.get(key);
                        if ((object != null) && !(object instanceof JSONNull)) {
                            map.put(key, object.toString());
                        }
                }
            } catch (JSONException js) {
                map.put(key, null);
                // In this case an attribute was not found. This is not considered to be a fatal error
                // since there is no guarantee that all attributes will always be returned.
            }

        }
    }

    protected int getDataType(Object o) {
        if (o instanceof String) return DATA_TYPE_STRING;
        if (o instanceof Boolean) return DATA_TYPE_BOOLEAN;
        if (o instanceof Identifier) return DATA_TYPE_IDENTIFIER;
        if (o instanceof Date) return DATA_TYPE_DATE;
        if (o instanceof JSONArray) return DATA_TYPE_ARRAY;
        throw new IllegalArgumentException("Error: Unknown data type for object of type " + o.getClass().getCanonicalName());
    }

    /**
     * Convert a {@link SwampThing} to a JSON object. This will simply
     * put each key/value pair into a JSON object. If you need something more involved, you
     * should override this method.
     *
     * @param thing
     * @return
     */
    public JSONObject toJSON(SwampThing thing) {

        JSONObject json = new JSONObject();

        for (String key : thing.getConversionMap().keySet()) {
            Object value = thing.getConversionMap().get(key);
            if (value == null) continue;
            switch (getDataType(value)) {
                case DATA_TYPE_DATE:
                    json.put(key, Iso8601.date2String((Date) value));
                    break;
                case DATA_TYPE_IDENTIFIER:
                    json.put(key, SWAMPIdentifiers.fromIdentifier((Identifier) value));
                    break;
                case DATA_TYPE_BOOLEAN:
                    Boolean b = (Boolean) value;

                    json.put(key, b ? "1" : "0");
                    break;
                default:
                case DATA_TYPE_STRING:
                    json.put(key, value.toString());
            }
        }
        return json;
    }

    /**
     * Convert a JSON object into a local SWAMP object.
     *
     * @param json
     * @return
     */
    protected abstract T fromJSON(JSONObject json);

    public void update(SwampThing t) {
        String url = getURL() + "/" + t.getUUIDString();
        getClient().rawPut(url, toJSON(t));
    }

    public SwampThing create(ConversionMapImpl map) {
        MyResponse mr = getClient().rawPost(getURL(), map);
        SwampThing p = fromJSON(mr.json);
        return p;
    }

    public SwampThing create(JSONObject map) {
        MyResponse mr = getClient().rawPost(getURL(), map);
        SwampThing p = fromJSON(mr.json);
        return p;
    }

    protected JSONObject mapToJSON(ConversionMapImpl map) {
        JSONObject jsonObject = new JSONObject();
        for (String key : map.keySet()) {
            jsonObject.put(key, map.get(key));
        }
        return jsonObject;
    }
}
