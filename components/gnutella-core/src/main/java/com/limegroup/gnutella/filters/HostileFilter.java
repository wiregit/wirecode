package com.limegroup.gnutella.filters;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.io.IP;
import org.limewire.io.IpPort;
import org.limewire.mojito.messages.DHTMessage;

import com.limegroup.gnutella.filters.IPFilter.IPFilterCallback;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.settings.FilterSettings;

public class HostileFilter extends SpamFilter {

    private static final Log LOG = LogFactory.getLog(HostileFilter.class);
    
    private static final ExecutorService IP_LOADER = ExecutorsHelper.newProcessingQueue("IpLoader");
    
    private volatile IPList hostileHosts = new IPList();
    
    
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
    
    /** 
     * Checks if a given Message's host is banned.
     * @return true if this Message's host is allowed, false if it is banned
     *  or we are unable to create correct IP addr out of it.
     */
    @Override
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
            InetSocketAddress addr = 
                (InetSocketAddress) message.getContact().getContactAddress();
            if (addr != null && addr.getAddress() instanceof Inet4Address)
                return allow(addr.getAddress());
            // dht messages do not require contact address.
            return true;
        } else {
            // we dont want to block other kinds of messages
            return true;
        }
    }

    public boolean allow(SocketAddress addr) {
        if(!(addr instanceof InetSocketAddress)) {
            return false;
        }
        return allow(((InetSocketAddress)addr).getAddress());
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
        return allowLogged(ip);
    }
    
    private boolean allowLogged(IP ip) {
        boolean yes = allowImpl(ip);
        if (LOG.isDebugEnabled())
            LOG.debug("" + (yes ? "" : "NOT ")+" allowing "+ip);
        return yes;
    }
    
    protected boolean allowImpl(IP ip) {
        return !hostileHosts.contains(ip);
    }
    
    
    /** Blocks while loading the new filters. */
    public void refreshHosts() {
        refreshHostsImpl();
    }
    
    protected void refreshHostsImpl() {
        // Load hostile, making sure the list is valid
        IPList newHostile = new IPList();
        String [] allHosts = FilterSettings.HOSTILE_IPS.getValue();
        try {
            for (String ip : allHosts)
                newHostile.add(new IP(ip));
            if (newHostile.isValidFilter(false))
                hostileHosts = newHostile;
        } catch (IllegalArgumentException badSimpp){}
    }
    
    public boolean hasBlackListedHosts() {
        return !hostileHosts.isEmpty();
    }
    
    public int logMinDistanceTo(IP ip) {
        return hostileHosts.logMinDistanceTo(ip);
    }
    
    /** Determines if the given IpPort is allowed. */
    public boolean allow(IpPort ipp) {
        return allow(ipp.getAddress());
    }
    
    public boolean allow(InetAddress addr) {
        IP ip = null;
        try {
            ip = new IP(addr.getAddress());
        } catch (IllegalArgumentException bad) {
            return false;
        }
        return allowLogged(ip);
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
                if (LOG.isDebugEnabled())
                    LOG.debug("doing dns lookup for "+host);
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
        return allowLogged(ip);
    }

}
