package com.limegroup.gnutella.util;

import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ProviderHacks;

/**
 * Helper class that always says it is a leaf.
 */
public final class LeafConnectionManager extends ConnectionManager {

    public LeafConnectionManager() {
        super(ProviderHacks.getNetworkManager());
    }

    public boolean isSupernode() {
        return false;
    }
}

