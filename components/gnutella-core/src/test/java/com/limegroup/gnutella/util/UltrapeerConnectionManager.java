package com.limegroup.gnutella.util;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.security.*;

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

