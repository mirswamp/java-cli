package org.continuousassurance.swamp.session.storage;

import org.continuousassurance.swamp.api.Tool;
import org.continuousassurance.swamp.session.handlers.AbstractHandler;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 4/19/16 at  4:21 PM
 */
public class ToolStore<V extends Tool> extends AbstractStore<V> {
    public ToolStore(AbstractHandler<? extends V> handler) {
        super(handler);
    }

    @Override
    public V create() {
        return (V) new Tool(handler.getSession());
    }
}
