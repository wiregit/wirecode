package com.limegroup.gnutella.filters;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.FilterSettings;
import org.limewire.io.IP;
import org.limewire.io.NetworkInstanceUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class HostileFilter extends  AbstractIPFilter {

    private static final Log LOG = LogFactory.getLog(HostileFilter.class);
        
    private volatile IPList hostileHosts = new IPList();
    
    private final NetworkInstanceUtils networkInstanceUtils;
    
    private final GeoLocationFilter geoLocationFilter;
    
    @Inject
    public HostileFilter(NetworkInstanceUtils networkInstanceUtils,
            GeoLocationFilter geoLocationFilter) {
        this.networkInstanceUtils = networkInstanceUtils;
        this.geoLocationFilter = geoLocationFilter;
    }
    
    /**
     * Refresh the IPFilter's instance.
     */
    @Override
    public void refreshHosts(LoadCallback callback) {
        refreshHosts();
        callback.spamFilterLoaded();
    }
    
    @Override
    public void refreshHosts() {
        LOG.debug("Refreshing hostile IP filter");
        IPList newHostile = new IPList();
        if(!FilterSettings.USE_NETWORK_FILTER.getValue()) {
            hostileHosts = newHostile;
            return;
        }
        // Load hostile IPs from setting, making sure the list is valid
        String [] allHosts = FilterSettings.HOSTILE_IPS.get();
        try {
            for(String ip : allHosts)
                newHostile.add(new IP(ip));
            if(newHostile.isValidFilter(false, networkInstanceUtils)) {
                LOG.debug("Filter was valid");
                hostileHosts = newHostile;
            } else {
                LOG.debug("Filter was invalid");
            }
        } catch(IllegalArgumentException badSimpp){
            LOG.debug("SIMPP was invalid", badSimpp);
        }
    }
    
    @Override
    public boolean hasBlacklistedHosts() {
        return !hostileHosts.isEmpty();
    }
    
    @Override
    protected boolean allowImpl(IP ip) {
        return !hostileHosts.contains(ip); // && geoLocationFilter.allow(ip);
    }
}
