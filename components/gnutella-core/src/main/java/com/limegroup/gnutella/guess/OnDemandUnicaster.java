package com.limegroup.gnutella.guess;

import com.limegroup.gnutella.URN;

/** Utility class for sending GUESS queries.
 */
public class OnDemandUnicaster {

    private final static OnDemandUnicaster instance = new OnDemandUnicaster();
    public static OnDemandUnicaster instance() {
        return instance;
    }

    private OnDemandUnicaster() {
    }

    public void query(GUESSEndpoint ep, URN queryURN) {
    }

}