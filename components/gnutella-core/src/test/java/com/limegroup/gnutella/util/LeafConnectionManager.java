package com.limegroup.gnutella.util;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.connection.*;
import com.limegroup.gnutella.security.*;

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

