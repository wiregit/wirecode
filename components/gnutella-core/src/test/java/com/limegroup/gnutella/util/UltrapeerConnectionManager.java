package com.limegroup.gnutella.util;

import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.security.Authenticator;

/**
 * Helper class that always says it is an Ultrapeer.
 */
public final class UltrapeerConnectionManager extends ConnectionManager {

    public UltrapeerConnectionManager(Authenticator auth) {
        super(auth);
    }

    public boolean isSupernode() {
        return true;
    }
}

