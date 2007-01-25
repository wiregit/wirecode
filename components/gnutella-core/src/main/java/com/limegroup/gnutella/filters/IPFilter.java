package com.limegroup.gnutella.filters;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;

import org.limewire.collection.Comparators;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.io.IOUtils;
import org.limewire.io.IP;
import org.limewire.io.IpPort;
import org.limewire.mojito.messages.DHTMessage;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.util.LimeWireUtils;


/**
 * Blocks messages and hosts based on IP address.  
 */
public final class IPFilter extends SpamFilter {
    
    private volatile IPList badHosts;
    private volatile IPList goodHosts;
    private volatile IPList hostileHosts;
    
    private final ExecutorService IP_LOADER = ExecutorsHelper.newProcessingQueue("IpLoader");

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
        hostileHosts = new IPList();
    }
    
    /**
     * Constructs an IPFilter that will load the hosts in the background
     * and notify the callback when it completes.
     */
    public IPFilter(IPFilterCallback callback) {
        // setup some blank lists temporarily.
        badHosts = new IPList();
        goodHosts = new IPList();
        hostileHosts = new IPList();
        
        refreshHosts(callback);
    }
    
    /** Blocks while loading the new filters. */
    public void refreshHosts() {
        refreshHostsImpl();
    }
    
    /**
     * Refresh the IPFilter's instance.
     */
    public void refreshHosts(final IPFilterCallback callback) {
        IP_LOADER.execute(new Runnable() {
            public void run() {
                refreshHostsImpl();
                callback.ipFiltersLoaded();
            }
        });
    }
    
    /** Does the work of setting new good  & bad hosts. */
    private void refreshHostsImpl() {        
        // Load basic bad...
        IPList newBad = new IPList();
        String[] allHosts = FilterSettings.BLACK_LISTED_IP_ADDRESSES.getValue();
        for (int i=0; i<allHosts.length; i++) {
            newBad.add(allHosts[i]);
        }
        
        // Load data from hostiles.txt...
        File hostiles = new File(LimeWireUtils.getUserSettingsDir(), "hostiles.txt");
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(hostiles));
            String read = null;
            while( (read = reader.readLine()) != null) {
                newBad.add(read);
            }
        } catch(IOException ignored) {
        } finally {
            IOUtils.close(reader);
        }
        
        // Load basic good...
        IPList newGood = new IPList();
        allHosts = FilterSettings.WHITE_LISTED_IP_ADDRESSES.getValue();
        for (int i=0; i<allHosts.length; i++) {
            newGood.add(allHosts[i]);
        }
        
        // Load hostile, making sure the list is valid
        IPList newHostile = new IPList();
        allHosts = FilterSettings.HOSTILE_IPS.getValue();
        try {
            for (String ip : allHosts)
                newHostile.add(new IP(ip));
            if (newHostile.isValidFilter(false))
                hostileHosts = newHostile;
        } catch (IllegalArgumentException badSimpp){}
        
        badHosts = newBad;
        goodHosts = newGood;
    }
    
    /** Determiens if any blacklisted hosts exist. */
    public boolean hasBlacklistedHosts() {
        return !badHosts.isEmpty() || !hostileHosts.isEmpty();
    }
    
    /** The logmin distance to bad or hostile ips. */
    public int logMinDistanceTo(byte[] addr) {
        IP ip = new IP(addr);
        return Math.min(badHosts.logMinDistanceTo(ip), hostileHosts.logMinDistanceTo(ip));
    }
    
    /** Determines if the given address is allowed. */
    public boolean allow(InetAddress addr) {
        IP ip = new IP(addr.getAddress());
        return goodHosts.contains(ip) || 
            !(badHosts.contains(ip) || hostileHosts.contains(ip));
    }
    
    /** Determines if the given IpPort is allowed. */
    public boolean allow(IpPort ipp) {
        return allow(ipp.getAddress());
    }
    
    /** 
     * Checks if a given host is banned.  This method will be
     * called when accepting an incoming or outgoing connection.
     * @param host preferably an IP in the form of A.B.C.D, but if
     *  it is a DNS name then a lookup will be performed.
     * @return true if this host is allowed, false if it is banned
     *  or we are unable to create correct IP addr out of it.
     */
    public boolean allow(String host) {
        IP ip;
        try {
            ip = new IP(host);
        } catch (IllegalArgumentException badHost) {
            try {
                InetAddress lookUp = InetAddress.getByName(host);
                host = lookUp.getHostAddress();
                ip = new IP(host);
            } catch(UnknownHostException unknownHost) {
                // could not look up this host.
                return false;
            } catch(IllegalArgumentException stillBadHost) {
                // couldn't construct IP still.
                return false;
            }
        }        
        return goodHosts.contains(ip) || 
            !(badHosts.contains(ip) || hostileHosts.contains(ip));
    }
    
    /**
     * Checks to see if a given host is banned.
     * @param host the host's IP in byte form.
     */
    public boolean allow(byte[] host) {
        IP ip;
        try {
            ip = new IP(host);
        } catch(IllegalArgumentException badHost) {
            return false;
        }
        return goodHosts.contains(ip) || 
            !(badHosts.contains(ip) || hostileHosts.contains(ip));
    }
    
    public boolean allow(SocketAddress addr) {
        if(!(addr instanceof InetSocketAddress)) {
            return false;
        }
        return allow(((InetSocketAddress)addr).getAddress());
    }

    public void ban(SocketAddress addr) {
        if(!(addr instanceof InetSocketAddress)) {
            return;
        }
        banIP(((InetSocketAddress)addr).getAddress().getHostAddress());
    }
    
    private void banIP(String ip) {
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
    

    /** 
     * Checks if a given Message's host is banned.
     * @return true if this Message's host is allowed, false if it is banned
     *  or we are unable to create correct IP addr out of it.
     */
    public boolean allow(Message m) {
        if (m instanceof PingReply) {
            PingReply pr = (PingReply)m;
            return allow(pr.getAddress());
        } else if (m instanceof QueryReply) {
            QueryReply qr = (QueryReply)m;
            return allow(qr.getIPBytes());
        } else if (m instanceof PushRequest) {
            PushRequest push=(PushRequest)m;
            return allow(push.getIP());
        } else if (m instanceof DHTMessage){
            DHTMessage message = (DHTMessage)m;
            return allow(message.getContact().getContactAddress());
        } else {
            // we dont want to block other kinds of messages
            return true;
        }
    }
    
    public static interface IPFilterCallback {
        public void ipFiltersLoaded();
    }
}



