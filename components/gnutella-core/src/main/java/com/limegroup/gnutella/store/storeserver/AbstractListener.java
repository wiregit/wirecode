package com.limegroup.gnutella.store.storeserver;

import com.limegroup.gnutella.store.storeserver.StoreManager;

/**
 * Generic base class for {@link StoreManager.Listener}s.
 */
public abstract class AbstractListener extends HasName implements StoreManager.Listener {
    public AbstractListener(String name) { super(name); }
    public AbstractListener() { super(); }
}