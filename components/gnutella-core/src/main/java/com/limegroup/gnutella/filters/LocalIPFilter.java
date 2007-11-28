package com.limegroup.gnutella.filters;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectableContainer;
import org.limewire.inspection.InspectionPoint;
import org.limewire.io.IOUtils;
import org.limewire.io.IP;
import org.limewire.util.CommonUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.settings.FilterSettings;


/**
 * Blocks messages and hosts based on IP address.  
 */
@Singleton
public final class LocalIPFilter extends AbstractIPFilter {
    
    private static final Log LOG = LogFactory.getLog(LocalIPFilter.class);
    
    private volatile IPList badHosts;
    private volatile IPList goodHosts;
    /** List contained in hostiles.txt if any.  Loaded on startup only */ 
    private final IPList hostilesTXTHosts = new IPList();
    
    private final IPFilter delegate;
    private final ScheduledExecutorService ipLoader;
    /** Marker for whether or not hostiles need to be loaded. */
    private volatile boolean shouldLoadHostiles;
    
    /** Number of allowings */
    private volatile long allowings;
    /** Number of blockings */
    private volatile long blockings;
    
    /** Constructs an IPFilter that automatically loads the content. */
    @Inject
    public LocalIPFilter(@Named("hostileFilter") IPFilter delegate, 
            @Named("backgroundExecutor") ScheduledExecutorService ipLoader) {
        this(true, delegate, ipLoader);
    }
    
    /** Constructs an IPFilter that can optionally load the content. */
    public LocalIPFilter(boolean load, 
            @Named("hostileFilter") IPFilter delegate, 
            @Named("backgroundExecutor") ScheduledExecutorService ipLoader) {
        this.delegate = delegate;
        this.ipLoader = ipLoader;
        
        File hostiles = new File(CommonUtils.getUserSettingsDir(), "hostiles.txt");
        shouldLoadHostiles = hostiles.exists();
        
        if(load) {
            delegate.refreshHosts();
            refreshHosts();
        } else {
            badHosts = new IPList();
            goodHosts = new IPList();
        }
    }
    
    public void refreshHosts() {
        refreshHosts(null);
    }
    
    public void refreshHosts(final IPFilterCallback callback) {
        Runnable load = new Runnable() {
            public void run() {
                delegate.refreshHosts();
                refreshHostsImpl();
                if (callback != null)
                    callback.ipFiltersLoaded();
            }
        };
        if (!shouldLoadHostiles) 
            load.run();
        else 
            ipLoader.execute(load);
    }
    
    /** Does the work of setting new good  & bad hosts. */
    private void refreshHostsImpl() {
        LOG.debug("refreshing hosts");
        
        // Load basic bad...
        IPList newBad = new IPList();
        String[] allHosts = FilterSettings.BLACK_LISTED_IP_ADDRESSES.getValue();
        for (int i=0; i<allHosts.length; i++) {
            newBad.add(allHosts[i]);
        }
        
        // Load basic good...
        IPList newGood = new IPList();
        allHosts = FilterSettings.WHITE_LISTED_IP_ADDRESSES.getValue();
        for (int i=0; i<allHosts.length; i++) {
            newGood.add(allHosts[i]);
        }

        // Load data from hostiles.txt (if it wasn't already loaded!)...
        if(shouldLoadHostiles) {
            shouldLoadHostiles = false;
            
            LOG.debug("loading hostiles");
            File hostiles = new File(CommonUtils.getUserSettingsDir(), "hostiles.txt");
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(hostiles));
                String read = null;
                while( (read = reader.readLine()) != null) {
                    hostilesTXTHosts.add(read);
                }
            } catch(IOException ignored) {
                LOG.debug("iox loading hostiles",ignored);
            } finally {
                IOUtils.close(reader);
            }
        }
        
        badHosts = new MultiIPList(newBad, hostilesTXTHosts);
        goodHosts = newGood;
    }
    
    /** Determiens if any blacklisted hosts exist. */
    public boolean hasBlacklistedHosts() {
        return delegate.hasBlacklistedHosts() || !badHosts.isEmpty();
    }
    
    /** The logmin distance to bad or hostile ips. */
    public int logMinDistanceTo(IP ip) {
        return Math.min(badHosts.logMinDistanceTo(ip), delegate.logMinDistanceTo(ip));
    }
    
    protected boolean allowImpl(IP ip) {
        boolean ret = goodHosts.contains(ip) ||
        (!badHosts.contains(ip) && delegate.allow(ip));
        if (ret)
            allowings++;
        else
            blockings++;
        return ret;
    }
    
    @InspectableContainer
    @SuppressWarnings("unused")
    private class IPFilterInspectable {
        
        @InspectionPoint("ip filter counts")
        private final Inspectable counts = new Inspectable() {
            public Object inspect() {
                Map<String,Object> ret = new HashMap<String, Object>();
                ret.put("allowed",allowings);
                ret.put("blocked",blockings);
                return ret;
            }
        };
    }
}



