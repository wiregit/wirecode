package com.limegroup.gnutella.util;

import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.security.Authenticator;

/**
 * Helper class that always says it is a leaf.
 */
public final class LeafConnectionManager extends ConnectionManager {

    public LeafConnectionManager(Authenticator auth) {
        super(auth);
    }

    public boolean isSupernode() {
        return false;
    }
}

