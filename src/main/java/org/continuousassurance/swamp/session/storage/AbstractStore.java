package org.continuousassurance.swamp.session.storage;

import edu.uiuc.ncsa.security.core.Identifier;
import edu.uiuc.ncsa.security.core.Store;
import edu.uiuc.ncsa.security.core.exceptions.NotImplementedException;
import org.continuousassurance.swamp.api.SwampThing;
import org.continuousassurance.swamp.session.handlers.AbstractHandler;

import java.util.*;

/**
 * Top-level for all store-like objects. Most of the functionality comes through the handler.
 * <p>Created by Jeff Gaynor<br>
 * on 4/18/16 at  3:53 PM
 */
public abstract class AbstractStore<V extends SwampThing> implements Store<V> {
    public AbstractStore(AbstractHandler<? extends V> handler) {
        this.handler = handler;
    }

    AbstractHandler<? extends V> handler;

    @Override
    public abstract V create();

    @Override
    public void update(V value) {
        handler.update(value);
    }

    @Override
    public void register(V value) {
        update(value);
    }

    @Override
    public void save(V value) {
        update(value);
    }

    @Override
    public int size() {
        return handler.getAll().size();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        if (!(key instanceof Identifier)) {
            return false;
        }
        Identifier id = (Identifier) key;
        return handler.get(id) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        if (!(value instanceof SwampThing)) {
            return false;
        }
        V p = (V) value;
        return containsKey(p.getIdentifier());
    }

    @Override
    public V get(Object key) {
        if (!(key instanceof Identifier)) {
            return null;
        }
        return (V) handler.get((Identifier) key);
    }

    @Override
    public V put(Identifier key, V value) {
        save(value);
        return value;

    }

    @Override
    public V remove(Object key) {
        V value = get(key);

        if (key instanceof Identifier) {
            Identifier id = null;
            id = (Identifier) key;
            handler.delete(id);
        }

        if (key instanceof SwampThing) {
            SwampThing s = (SwampThing) key;
            handler.delete(s);
        }
        return value;
    }

    /**
     * This is <b>not</b> efficient and simply loops.
     *
     * @param m
     */
    @Override
    public void putAll(Map<? extends Identifier, ? extends V> m) {
        for (V value : m.values()) {
            save(value);
        }

    }

    /**
     * This will remove all user-defined objects from the store. It may fail, depending upon the policies and permissions.
     */
    @Override
    public void clear() {

    }

    @Override
    public Set<Identifier> keySet() {
        HashSet<Identifier> arrayList = new HashSet<>();
        Collection<? extends V> allValues = handler.getAll();
        for (V value : allValues) {
            arrayList.add(value.getIdentifier());
        }
        return arrayList;
    }

    @Override
    public Collection<V> values() {
        Collection<? extends V> vv = handler.getAll();
        ArrayList<V> output = new ArrayList<>();
        for (V v : vv) {
            output.add(v);
        }
        return output;
    }

    @Override
    public Set<Entry<Identifier, V>> entrySet() {
        throw new NotImplementedException();
    }
}
