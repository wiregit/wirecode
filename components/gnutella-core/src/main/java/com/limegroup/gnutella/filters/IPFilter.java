package com.limegroup.gnutella.filters;

import com.sun.java.util.collections.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.*;

/**
 * Blocks messages and hosts based on IP address.  Formerly know as
 * BlackListFilter.  Immutable.  
 */
public final class IPFilter extends SpamFilter {
    private final IPList badHosts = new IPList();
    private final IPList goodHosts = new IPList();

    /** Constructs a new BlackListFilter containing the addresses listed
     *  in the SettingsManager. */
    public IPFilter(){        
        String[] allHosts = SettingsManager.instance().getBannedIps();
        for (int i=0; i<allHosts.length; i++)
            badHosts.add(allHosts[i]);
        
        allHosts = SettingsManager.instance().getAllowedIps();
        for (int i=0; i<allHosts.length; i++)
            goodHosts.add(allHosts[i]);        
    }

    /** 
     * Checks if a given host is banned.  This method will be
     * called when accepting an incomming or outgoing connection. 
     * @return true iff host is a banned address
     */
    public boolean allow(String host) {
        return goodHosts.contains(host) || !badHosts.contains(host);
    }

    /** 
     * Returns true if m is a ping or query reply message with a banned
     * address.
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
            ip = ip2string(push.getIP());
        }
        else // we dont want to block other kinds of messages
            return true;
        // now check the ip
        return allow(ip); 
    }

    private static String ip2string(byte[] ip) {
        //Hack.  Ideally we'd call Message.ip2string.
        return (new Endpoint(ip, 0)).getHostname();
    }
    
    //unit tests
    /*
    public static void main(String args[]) {        
        SettingsManager.instance().setBannedIps(
            new String[] {"18.239.0.*", "13.0.0.0"});
        SettingsManager.instance().setAllowedIps(new String[] {"18.239.0.144"});
        IPFilter filter = new IPFilter();
        Assert.that(filter.allow("18.240.0.0"));
        Assert.that(! filter.allow("18.239.0.142"));
        Assert.that(filter.allow("18.239.0.144"));
        Assert.that(! filter.allow("13.0.0.0"));
        Assert.that(filter.allow("13.0.0.1"));
        byte[] address={(byte)18, (byte)239, (byte)0, (byte)144};
        Assert.that(filter.allow(
            new PingReply(new byte[16], (byte)3, 6346, address, 0l, 0l)));
        byte[] address2=new byte[] {(byte)18, (byte)239, (byte)0, (byte)143};
        Assert.that(! filter.allow(
            new QueryReply(new byte[16], (byte)3, 6346, address2, 0,
                           new Response[0], new byte[16])));
        Assert.that(filter.allow(new QueryRequest((byte)3, 0, "test")));
        PushRequest push1=new PushRequest( 
            new byte[16], (byte)3, new byte[16], 0l, address, 6346);
        Assert.that(filter.allow(push1));
        PushRequest push2=new PushRequest( 
            new byte[16], (byte)3, new byte[16], 0l, address2, 6346);
        Assert.that(! filter.allow(push2));
        
    }
    */   
}



