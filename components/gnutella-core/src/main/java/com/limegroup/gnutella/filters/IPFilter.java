package com.limegroup.gnutella.filters;

import com.sun.java.util.collections.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.util.NetworkUtils;

/**
 * Blocks messages and hosts based on IP address.  Formerly know as
 * BlackListFilter.  Immutable.  
 */
public final class IPFilter extends SpamFilter {
    
    private static IPFilter _instance = new IPFilter();
    
    private final IPList badHosts = new IPList();
    private final IPList goodHosts = new IPList();

    /** Constructs a new BlackListFilter containing the addresses listed
     *  in the SettingsManager. */
    private IPFilter(){        
        String[] allHosts = SettingsManager.instance().getBannedIps();
        for (int i=0; i<allHosts.length; i++)
            badHosts.add(allHosts[i]);
        
        allHosts = SettingsManager.instance().getAllowedIps();
        for (int i=0; i<allHosts.length; i++)
            goodHosts.add(allHosts[i]);        
    }
    
    /**
     * Returns the current active instance of IPFilter.
     */
    public static IPFilter instance() {
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
     * @return false iff host is a banned address, true otherwise.
     */
    public boolean allow(String host) {
        IP ip;
        try {
            ip = new IP(host);
        } catch (IllegalArgumentException e) {
            return false;
        }        
        return goodHosts.contains(ip) || !badHosts.contains(ip);
    }

    /** 
     * Returns false if m is a ping or query reply message with a banned
     * address.
     * @return false iff host is a banned address, true otherwise.     
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



