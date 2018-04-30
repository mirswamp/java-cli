package org.continuousassurance.swamp.session.storage;

import org.continuousassurance.swamp.api.Platform;
import org.continuousassurance.swamp.session.handlers.AbstractHandler;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 4/27/17 at  2:27 PM
 */
public class PlatformStore<V extends Platform> extends AbstractStore<V> {
    public PlatformStore(AbstractHandler<? extends V> handler) {
        super(handler);
    }

    @Override
    public V create() {
        return (V) new Platform(handler.getSession());

    }
}
