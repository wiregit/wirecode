package com.limegroup.gnutella.filters;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.settings.FilterSettings;

/**
 * Blocks messages and hosts based on IP address.  Formerly know as
 * BlackListFilter.  Immutable.  
 */
public final class IPFilter extends SpamFilter {
    
    private volatile static IPFilter _instance;
    
    private final IPList badHosts = new IPList();
    private final IPList goodHosts = new IPList();
    private final IPList hostileHosts;

    /** Constructs a new BlackListFilter containing the addresses listed
     *  in the SettingsManager. */
    private IPFilter(){        
        String[] allHosts = FilterSettings.BLACK_LISTED_IP_ADDRESSES.getValue();
        for (int i=0; i<allHosts.length; i++)
            badHosts.add(allHosts[i]);
        
        allHosts = FilterSettings.WHITE_LISTED_IP_ADDRESSES.getValue();
        for (int i=0; i<allHosts.length; i++)
            goodHosts.add(allHosts[i]);      
        
        // Load hostile, making sure the list is valid
        IPList newHostile = new IPList();
        allHosts = FilterSettings.HOSTILE_IPS.getValue();
        try {
            for (int i = 0; i < allHosts.length; i++)
                newHostile.add(new IP(allHosts[i]));
            if (!newHostile.isValidFilter(false))
                newHostile = new IPList();
        } catch (IllegalArgumentException badSimpp){
            newHostile = new IPList();
        }
        hostileHosts = newHostile;
    }
    
    /**
     * Returns the current active instance of IPFilter.
     */
    public static IPFilter instance() {
        if( _instance == null )
            _instance = new IPFilter();
        return _instance;
    }
    
    /**
     * Refresh the IPFilter's instance.
     */
    public static void refreshIPFilter() {
        _instance = new IPFilter();
    }

    /**
     * Return the badList of the instance
     */
    public IPList getBadHosts() {
        return badHosts;
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
}



