package com.limegroup.gnutella;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.filters.HostileFilter;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.filters.SpamFilter;

@Singleton
public class SpamServicesImpl implements SpamServices {
    
    private final Provider<ConnectionManager> connectionManager;
    private final Provider<HostileFilter> hostilesFilter;
    private final Provider<IPFilter> ipFilter;

    @Inject
    public SpamServicesImpl(Provider<ConnectionManager> connectionManager,
            Provider<HostileFilter> hostilesFilter,
            Provider<IPFilter> ipFilter) {
        this.connectionManager = connectionManager;
        this.hostilesFilter = hostilesFilter;
        this.ipFilter = ipFilter;
    }
    

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SpamServices#adjustSpamFilters()
     */
    public void adjustSpamFilters() {
        UDPReplyHandler.setPersonalFilter(SpamFilter.newPersonalFilter());
        
        //Just replace the spam filters.  No need to do anything
        //fancy like incrementally updating them.
        for(ManagedConnection c : connectionManager.get().getConnections()) {
            if(ipFilter.get().allow(c)) {
                c.setPersonalFilter(SpamFilter.newPersonalFilter());
                c.setRouteFilter(SpamFilter.newRouteFilter());
            } else {
                // If the connection isn't allowed now, close it.
                c.close();
            }
        }
        
        // TODO: notify DownloadManager & UploadManager about new banned IP ranges
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SpamServices#reloadIPFilter()
     */
    public void reloadIPFilter() {
        ipFilter.get().refreshHosts(new IPFilter.IPFilterCallback() {
            public void ipFiltersLoaded() {
                adjustSpamFilters();
            }
        });
        hostilesFilter.get().refreshHosts();
    }


}
