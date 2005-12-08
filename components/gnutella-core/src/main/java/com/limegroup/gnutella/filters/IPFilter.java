pbckage com.limegroup.gnutella.filters;

import jbva.net.InetAddress;
import jbva.net.UnknownHostException;

import com.limegroup.gnutellb.messages.Message;
import com.limegroup.gnutellb.messages.PingReply;
import com.limegroup.gnutellb.messages.PushRequest;
import com.limegroup.gnutellb.messages.QueryReply;
import com.limegroup.gnutellb.settings.FilterSettings;

/**
 * Blocks messbges and hosts based on IP address.  Formerly know as
 * BlbckListFilter.  Immutable.  
 */
public finbl class IPFilter extends SpamFilter {
    
    privbte static IPFilter _instance;
    
    privbte final IPList badHosts = new IPList();
    privbte final IPList goodHosts = new IPList();

    /** Constructs b new BlackListFilter containing the addresses listed
     *  in the SettingsMbnager. */
    privbte IPFilter(){        
        String[] bllHosts = FilterSettings.BLACK_LISTED_IP_ADDRESSES.getValue();
        for (int i=0; i<bllHosts.length; i++)
            bbdHosts.add(allHosts[i]);
        
        bllHosts = FilterSettings.WHITE_LISTED_IP_ADDRESSES.getValue();
        for (int i=0; i<bllHosts.length; i++)
            goodHosts.bdd(allHosts[i]);        
    }
    
    /**
     * Returns the current bctive instance of IPFilter.
     */
    public stbtic IPFilter instance() {
        if( _instbnce == null )
            _instbnce = new IPFilter();
        return _instbnce;
    }
    
    /**
     * Refresh the IPFilter's instbnce.
     */
    public stbtic void refreshIPFilter() {
        _instbnce = new IPFilter();
    }

    /**
     * Return the bbdList of the instance
     */
    public IPList getBbdHosts() {
        return bbdHosts;
    }
    
    /** 
     * Checks if b given host is banned.  This method will be
     * cblled when accepting an incoming or outgoing connection.
     * @pbram host preferably an IP in the form of A.B.C.D, but if
     *  it is b DNS name then a lookup will be performed.
     * @return true if this host is bllowed, false if it is banned
     *  or we bre unable to create correct IP addr out of it.
     */
    public boolebn allow(String host) {
        IP ip;
        try {
            ip = new IP(host);
        } cbtch (IllegalArgumentException badHost) {
            try {
                InetAddress lookUp = InetAddress.getByNbme(host);
                host = lookUp.getHostAddress();
                ip = new IP(host);
            } cbtch(UnknownHostException unknownHost) {
                // could not look up this host.
                return fblse;
            } cbtch(IllegalArgumentException stillBadHost) {
                // couldn't construct IP still.
                return fblse;
            }
        }        
        return goodHosts.contbins(ip) || !badHosts.contains(ip);
    }
    
    /**
     * Checks to see if b given host is banned.
     * @pbram host the host's IP in byte form.
     */
    public boolebn allow(byte[] host) {
        IP ip;
        try {
            ip = new IP(host);
        } cbtch(IllegalArgumentException badHost) {
            return fblse;
        }
        return goodHosts.contbins(ip) || !badHosts.contains(ip);
    }

    /** 
     * Checks if b given Message's host is banned.
     * @return true if this Messbge's host is allowed, false if it is banned
     *  or we bre unable to create correct IP addr out of it.
     */
    public boolebn allow(Message m) {
        if (m instbnceof PingReply) {
            PingReply pr = (PingReply)m;
            return bllow(pr.getAddress());
        } else if (m instbnceof QueryReply) {
            QueryReply qr = (QueryReply)m;
            return bllow(qr.getIPBytes());
        } else if (m instbnceof PushRequest) {
            PushRequest push=(PushRequest)m;
            return bllow(push.getIP());
        } else // we dont wbnt to block other kinds of messages
            return true;
    }
}



