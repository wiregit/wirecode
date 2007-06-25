package com.limegroup.gnutella.filters;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.Comparators;
import org.limewire.io.IOUtils;
import org.limewire.io.IP;
import org.limewire.util.CommonUtils;

import com.limegroup.gnutella.settings.FilterSettings;


/**
 * Blocks messages and hosts based on IP address.  
 */
public final class IPFilter extends HostileFilter {
    
    private static final Log LOG = LogFactory.getLog(IPFilter.class);
    
    private volatile IPList badHosts;
    private volatile IPList goodHosts;
    /** List contained in hostiles.txt if any.  Loaded on startup only */ 
    private final IPList hostilesTXTHosts = new IPList();
    /** Marker for whether or not hostiles have been loaded. */
    private volatile boolean loadedHostiles = false;
    
    /** Constructs an IPFilter that automatically loads the content. */
    public IPFilter() {
        this(true);
    }
    
    /** Constructs an IPFilter that can optionally load the content. */
    public IPFilter(boolean load) {
        if(load) {
            refreshHosts();
        } else {
            badHosts = new IPList();
            goodHosts = new IPList();
        }
    }
    
    /**
     * Constructs an IPFilter that will load the hosts in the background
     * and notify the callback when it completes.
     */
    public IPFilter(IPFilterCallback callback) {
        // setup some blank lists temporarily.
        badHosts = new IPList();
        goodHosts = new IPList();
        
        refreshHosts(callback);
    }
    
    /** Does the work of setting new good  & bad hosts. */
    protected void refreshHostsImpl() {
        super.refreshHostsImpl();
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
        if(!loadedHostiles) {
            loadedHostiles = true;
            
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
        return super.hasBlackListedHosts() || !badHosts.isEmpty();
    }
    
    /** The logmin distance to bad or hostile ips. */
    public int logMinDistanceTo(byte[] addr) {
        IP ip = new IP(addr);
        return Math.min(badHosts.logMinDistanceTo(ip), super.logMinDistanceTo(ip));
    }
    
    public void ban(SocketAddress addr) {
        if(!(addr instanceof InetSocketAddress)) {
            return;
        }
        banIP(((InetSocketAddress)addr).getAddress().getHostAddress());
    }
    
    private void banIP(String ip) {
        if (LOG.isDebugEnabled())
            LOG.debug("banning "+ip);
        
        String[] bannedIPs = FilterSettings.BLACK_LISTED_IP_ADDRESSES.getValue();
        Arrays.sort(bannedIPs, Comparators.stringComparator());
        if ( Arrays.binarySearch(bannedIPs, ip, 
                                 Comparators.stringComparator()) >= 0 ) {
            return;
        }
        
        String[] more_banned = new String[bannedIPs.length+1];
        System.arraycopy(bannedIPs, 0, more_banned, 0, 
                         bannedIPs.length);
        more_banned[bannedIPs.length] = ip;
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(more_banned);
    }
    

    public static interface IPFilterCallback {
        public void ipFiltersLoaded();
    }
    
    protected boolean allowImpl(IP ip) {
        return goodHosts.contains(ip) ||
        (!badHosts.contains(ip) && super.allowImpl(ip));
    }
}



