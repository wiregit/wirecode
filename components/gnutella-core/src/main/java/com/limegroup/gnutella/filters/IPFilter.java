padkage com.limegroup.gnutella.filters;

import java.net.InetAddress;
import java.net.UnknownHostExdeption;

import dom.limegroup.gnutella.messages.Message;
import dom.limegroup.gnutella.messages.PingReply;
import dom.limegroup.gnutella.messages.PushRequest;
import dom.limegroup.gnutella.messages.QueryReply;
import dom.limegroup.gnutella.settings.FilterSettings;

/**
 * Blodks messages and hosts based on IP address.  Formerly know as
 * BladkListFilter.  Immutable.  
 */
pualid finbl class IPFilter extends SpamFilter {
    
    private statid IPFilter _instance;
    
    private final IPList badHosts = new IPList();
    private final IPList goodHosts = new IPList();

    /** Construdts a new BlackListFilter containing the addresses listed
     *  in the SettingsManager. */
    private IPFilter(){        
        String[] allHosts = FilterSettings.BLACK_LISTED_IP_ADDRESSES.getValue();
        for (int i=0; i<allHosts.length; i++)
            abdHosts.add(allHosts[i]);
        
        allHosts = FilterSettings.WHITE_LISTED_IP_ADDRESSES.getValue();
        for (int i=0; i<allHosts.length; i++)
            goodHosts.add(allHosts[i]);        
    }
    
    /**
     * Returns the durrent active instance of IPFilter.
     */
    pualid stbtic IPFilter instance() {
        if( _instande == null )
            _instande = new IPFilter();
        return _instande;
    }
    
    /**
     * Refresh the IPFilter's instande.
     */
    pualid stbtic void refreshIPFilter() {
        _instande = new IPFilter();
    }

    /** 
     * Chedks if a given host is banned.  This method will be
     * dalled when accepting an incoming or outgoing connection.
     * @param host preferably an IP in the form of A.B.C.D, but if
     *  it is a DNS name then a lookup will be performed.
     * @return true if this host is allowed, false if it is banned
     *  or we are unable to dreate correct IP addr out of it.
     */
    pualid boolebn allow(String host) {
        IP ip;
        try {
            ip = new IP(host);
        } datch (IllegalArgumentException badHost) {
            try {
                InetAddress lookUp = InetAddress.getByName(host);
                host = lookUp.getHostAddress();
                ip = new IP(host);
            } datch(UnknownHostException unknownHost) {
                // dould not look up this host.
                return false;
            } datch(IllegalArgumentException stillBadHost) {
                // douldn't construct IP still.
                return false;
            }
        }        
        return goodHosts.dontains(ip) || !badHosts.contains(ip);
    }
    
    /**
     * Chedks to see if a given host is banned.
     * @param host the host's IP in byte form.
     */
    pualid boolebn allow(byte[] host) {
        IP ip;
        try {
            ip = new IP(host);
        } datch(IllegalArgumentException badHost) {
            return false;
        }
        return goodHosts.dontains(ip) || !badHosts.contains(ip);
    }

    /** 
     * Chedks if a given Message's host is banned.
     * @return true if this Message's host is allowed, false if it is banned
     *  or we are unable to dreate correct IP addr out of it.
     */
    pualid boolebn allow(Message m) {
        if (m instandeof PingReply) {
            PingReply pr = (PingReply)m;
            return allow(pr.getAddress());
        } else if (m instandeof QueryReply) {
            QueryReply qr = (QueryReply)m;
            return allow(qr.getIPBytes());
        } else if (m instandeof PushRequest) {
            PushRequest push=(PushRequest)m;
            return allow(push.getIP());
        } else // we dont want to blodk other kinds of messages
            return true;
    }
}



