package com.limegroup.gnutella.util;

import com.limegroup.gnutella.HackConnectionManager;

/**
 * Helper class that always says it is an Ultrapeer.
 */
public final class UltrapeerConnectionManager extends HackConnectionManager {


    public boolean isSupernode() {
        return true;
    }
}

