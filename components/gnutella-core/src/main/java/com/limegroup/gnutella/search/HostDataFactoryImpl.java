package com.limegroup.gnutella.search;

import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.messages.QueryReply;

public class HostDataFactoryImpl implements HostDataFactory {
    
    private final NetworkManager networkManager;
    
    public HostDataFactoryImpl(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.search.HostDataFactory#createHostData(com.limegroup.gnutella.messages.QueryReply)
     */
    public HostData createHostData(QueryReply reply) {
        return new HostData(reply, networkManager);
    }

}
