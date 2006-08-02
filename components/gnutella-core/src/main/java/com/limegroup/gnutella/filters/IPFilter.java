package com.limegroup.gnutella.filters;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.IOUtils;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.ProcessingQueue;

/**
 * Blocks messages and hosts based on IP address.  
 */
public final class IPFilter extends SpamFilter {
    
    private volatile IPList badHosts;
    private volatile IPList goodHosts;
    
    private final ProcessingQueue IP_LOADER = new ProcessingQueue("IpLoader");

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
    
    /** Blocks while loading the new filters. */
    public void refreshHosts() {
        refreshHostsImpl();
    }
    
    /**
     * Refresh the IPFilter's instance.
     */
    public void refreshHosts(final IPFilterCallback callback) {
        IP_LOADER.add(new Runnable() {
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
        File hostiles = new File(CommonUtils.getUserSettingsDir(), "hostiles.txt");
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(hostiles));
            String read = null;
            while( (read = reader.readLine()) != null) {
              //  System.out.println("Loading from hostiles, " + read);
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
        
        badHosts = newBad;
        goodHosts = newGood;
    }
    
    /** Determiens if any blacklisted hosts exist. */
    public boolean hasBlacklistedHosts() {
        return !badHosts.isEmpty();
    }
    
    /** Delegate method for badHosts.logMinDistanceTo(IP) */
    public int logMinDistanceTo(byte[] addr) {
        return badHosts.logMinDistanceTo(new IP(addr));
    }
    
    /** Determines if the given address is allowed. */
    public boolean allow(InetAddress addr) {
        IP ip = new IP(addr.getAddress());
        return goodHosts.contains(ip) || !badHosts.contains(ip);
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
        return goodHosts.contains(ip) || !badHosts.contains(ip);
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
        return goodHosts.contains(ip) || !badHosts.contains(ip);
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
        } else // we dont want to block other kinds of messages
            return true;
    }
    
    public static interface IPFilterCallback {
        public void ipFiltersLoaded();
    }
}



