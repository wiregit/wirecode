package com.limegroup.gnutella.filters;

import com.sun.java.util.collections.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.util.NetworkUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Blocks messages and hosts based on IP address.  Formerly know as
 * BlackListFilter.  Immutable.  
 */
public final class IPFilter extends SpamFilter {
    
    private static IPFilter _instance;
    
    private final IPList badHosts = new IPList();
    private final IPList goodHosts = new IPList();

    /** Constructs a new BlackListFilter containing the addresses listed
     *  in the SettingsManager. */
    private IPFilter(){        
        String[] allHosts = FilterSettings.BLACK_LISTED_IP_ADDRESSES.getValue();
        for (int i=0; i<allHosts.length; i++)
            badHosts.add(allHosts[i]);
        
        allHosts = FilterSettings.WHITE_LISTED_IP_ADDRESSES.getValue();
        for (int i=0; i<allHosts.length; i++)
            goodHosts.add(allHosts[i]);        
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
     * Checks if a given host is banned.  This method will be
     * called when accepting an incomming or outgoing connection.
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
     * Checks if a given Message's host is banned.
     * @return true if this Message's host is allowed, false if it is banned
     *  or we are unable to create correct IP addr out of it.
     */
    public boolean allow(Message m){
        String ip;
        if( (m instanceof PingReply)){
            PingReply pr = (PingReply)m;
            ip = pr.getIP();
        }
        else if ( (m instanceof QueryReply) ){
            QueryReply qr = (QueryReply)m;
            ip = qr.getIP();
        } else if (m instanceof PushRequest) {
            PushRequest push=(PushRequest)m;
            ip = NetworkUtils.ip2string(push.getIP());
        }
        else // we dont want to block other kinds of messages
            return true;
        // now check the ip
        return allow(ip); 
    }
}



