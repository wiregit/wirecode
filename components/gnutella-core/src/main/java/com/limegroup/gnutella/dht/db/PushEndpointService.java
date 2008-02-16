package com.limegroup.gnutella.dht.db;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.PushEndpoint;

/**
 * A unit that allows for blocking or asynchronous retrieval of 
 * {@link PushEndpoint push endpoints}.
 */
public interface PushEndpointService {

    void findPushEndpoint(GUID guid, SearchListener<PushEndpoint> listener);
    
    PushEndpoint getPushEndpoint(GUID guid);    
}
