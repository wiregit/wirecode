package com.limegroup.gnutella.util;

import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ProviderHacks;

/**
 * Helper class that always says it is an Ultrapeer.
 */
public final class UltrapeerConnectionManager extends ConnectionManager {

    public UltrapeerConnectionManager() {
        super(ProviderHacks.getNetworkManager());
    }

    public boolean isSupernode() {
        return true;
    }
}

