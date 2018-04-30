package org.continuousassurance.swamp.api;

import edu.uiuc.ncsa.security.core.Identifiable;
import edu.uiuc.ncsa.security.core.Identifier;
import net.sf.json.JSONArray;
import org.continuousassurance.swamp.session.Session;
import org.continuousassurance.swamp.session.handlers.PackageVersionHandler;
import org.continuousassurance.swamp.session.util.ConversionMapImpl;
import org.continuousassurance.swamp.session.util.SWAMPIdentifiers;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Top level for all SWAMP objects. A few key concepts are that while internally to the SWAMP
 * every object has a uuid in practice these are not actually always uuids -- in at least a
 * few cases they are just integers and cannot be turned into {@link java.util.UUID} objects.
 * Therefore, when marshalling/unmarshalling an object, whatever is reported as a UUID is wrapped in
 * an {@link Identifier}. A convenience method will retrieve the uuid, {@link #getUUIDString()}. <br/><br/>
 * <h3>Use of this class</h3>
 * All SWAMP objects inherit from this class. Internally there is a {@link ConversionMapImpl} which will
 * do many conversions needed to serialize this to JSON. Generally avoid accessing that. Rather, each
 * subclass has customized mutators to get properties
 * (e.g. for a user, the first name) and these mutators should be used rather than the conversion map,
 * since they are closely allied with the API as it comes over the wire.
 * <p/>
 * <p>Created by Jeff Gaynor<br>
 * on 11/18/14 at  4:04 PM
 */
public abstract class SwampThing implements Identifiable {

    /**
     * Set the session for this thing. All things must be tied to a session.
     *
     * @param session
     */
    protected SwampThing(Session session) {
        this.session = session;
    }

    /**
     * A constructor to set the internal state of this object from a map.
     *
     * @param map
     */

    public SwampThing(Session session, Map map) {
        // Fixes IAM-109
        this.session = session;
        getConversionMap().putAll(map);
    }

    /**
     * This returns a new instance of whatever the current Swamp thing is. It is invoked in the clone method mostly.
     * This effectively lets you call the constructor you want on the current object.
     *
     * @return
     */
    protected abstract SwampThing getNewInstance();

    @Override
    public Identifiable clone() {
        SwampThing p = getNewInstance();
        p.setConversionMap(getConversionMap());
        return p;
    }

    /**
     * note that since identifiers are actually stored in the conversion map,
     * you must access them with the correct uuid key.
     */

    public abstract String getIDKey();

    @Override
    public void setIdentifier(Identifier identifier) {
        getConversionMap().put(getIDKey(), identifier);
    }

    @Override
    public Identifier getIdentifier() {
        return getConversionMap().getIdentifier(getIDKey());
    }

    /**
     * Records if the object has been changed. If so, this will be true until this flag is cleared. This
     * allows for session management since objects then track if they have changed, rather than the user.
     * Note that all changes to the values of this should go through the single {@link #put(String, Object)}
     * method below. Any extensions to this class require you to set this flag if there are changes to the data
     * or they might not be persisted.
     *
     * @return
     */
    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    boolean changed = false;

    @Override
    public String getIdentifierString() {
        if (getIdentifier() == null) return null;
        return getIdentifier().toString();
    }

    /**
     * Convenience to recover the UUID, such as it is, from the identifier. Note that since the "uuid" is
     * in general not a valid {@link java.util.UUID}, this is simply a string.
     *
     * @return
     */

    public String getUUIDString() {
        if (getIdentifier() == null) return null;
        return SWAMPIdentifiers.fromIdentifier(getIdentifier());
    }


    public String getFilename() {
        return getConversionMap().getString(PackageVersionHandler.FILENAME);
    }

    public void setFilename(String filename) {
        getConversionMap().put(PackageVersionHandler.FILENAME, filename);
    }

    /**
     * An internally used map to manage values and convert between them when they are marshalled or unmarshalled.
     * This method is public because of Java requirements on visibility to utilities in other packages. Generally
     * you should not need to access properties for this object through this map.
     *
     * @return
     */
    public ConversionMapImpl getConversionMap() {
        if (conversionMap == null) {
            conversionMap = new ConversionMapImpl();
        }
        return conversionMap;
    }

    public void setConversionMap(ConversionMapImpl conversionMap) {
        this.conversionMap = conversionMap;
    }

    ConversionMapImpl conversionMap;

    protected void put(String key, Object object) {
        getConversionMap().put(key, object);
        setChanged(true);
    }

    protected String getString(String key) {
        return getConversionMap().getString(key);
    }

    protected long getLong(String key) {
        return getConversionMap().getLong(key);
    }

    protected Identifier getAsID(String key) {
        return getConversionMap().getIdentifier(key);
    }

    protected boolean getBoolean(String key) {
        return getConversionMap().getBoolean(key);
    }

    protected Date getDate(String key) {
        return getConversionMap().getDate(key);
    }


    Session session;

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    /**
     * Use this after the object has been changed.
     *
     * @return
     */
    public void clearChangedFlag() {
        changed = false;
    }

    /**
     * test that an object is really a JSONArray and return it as a list of strings.
     * If it is not, it is null or is empty an empty list is returned.
     *
     * @param object
     * @return
     */
    public List<String> getAsArray(Object object) {
        if (object == null) {
            return new LinkedList<>();
        }
        if (object instanceof JSONArray) {
            return (List<String>) object;
        } else {
            return new LinkedList<String>();
        }
    }

    public List<String> getAsArray(String key) {
        return getAsArray(getConversionMap().get(key));
    }


    @Override
    public String toString() {
        String x = getClass().getSimpleName() + "[";
        if (getConversionMap() == null) {
            return x + "(empty)]";
        }
        boolean firstPass = true;
        for (String z : getConversionMap().keySet()) {
            x = x + (firstPass ? "" : ",") + z + "=" + getConversionMap().get(z);
            if (firstPass) {
                firstPass = false;
            }
        }

        x = x + "]";
        return x;
    }
}
