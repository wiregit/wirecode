package com.limegroup.gnutella.store.storeserver;

import com.limegroup.gnutella.store.storeserver.IStoreServer;

/**
 * Generic base class for {@link IStoreServer.Listener}s.
 * 
 * @author jpalm
 */
public abstract class AbstractListener extends HasName implements IStoreServer.Listener {
    public AbstractListener(String name) { super(name); }
    public AbstractListener() { super(); }
}