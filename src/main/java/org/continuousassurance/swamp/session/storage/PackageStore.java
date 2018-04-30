package org.continuousassurance.swamp.session.storage;

import org.continuousassurance.swamp.session.handlers.PackageHandler;
import org.continuousassurance.swamp.api.PackageThing;
import org.continuousassurance.swamp.session.handlers.AbstractHandler;

/**
 * <p>Created by Jeff Gaynor<br>
 * on 4/19/16 at  11:02 AM
 */
public class PackageStore<V extends PackageThing> extends AbstractStore<V> {
    public PackageStore(AbstractHandler<? extends V> handler) {
        super(handler);
    }

    protected PackageHandler getPackageHandler(){
        return (PackageHandler)handler;
    }
    @Override
    public V create() {
        PackageThing p = getPackageHandler().create("Temp name", "Temp description", 0);
        p.setSession(handler.getSession());
        return (V)p;
    }
}
