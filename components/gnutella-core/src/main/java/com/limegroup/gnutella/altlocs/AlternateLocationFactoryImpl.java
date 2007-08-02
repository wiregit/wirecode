package com.limegroup.gnutella.altlocs;

import java.io.IOException;

import org.limewire.io.ConnectableImpl;
import org.limewire.io.IP;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortForSelf;
import org.limewire.io.NetworkUtils;
import org.limewire.service.ErrorService;

import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.SSLSettings;

public class AlternateLocationFactoryImpl implements AlternateLocationFactory {
    
    private final NetworkManager networkManager;
    
    public AlternateLocationFactoryImpl(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.altlocs.AlternateLocationFactory#create(com.limegroup.gnutella.URN)
     */
    public AlternateLocation create(URN urn) {
    	if(urn == null) throw new NullPointerException("null sha1");
        
    	try {
    	    
    	    // We try to guess whether we are firewalled or not.  If the node
    	    // has just started up and has not yet received an incoming connection
    	    // our best bet is to see if we have received a connection in the past.
    	    //
    	    // However it is entirely possible that we have received connection in 
    	    // the past but are firewalled this session, so if we are connected
    	    // we see if we received a conn this session only.
    	    
    	    boolean open;
    	    
    	    if (RouterService.isConnected())
    	        open = networkManager.acceptedIncomingConnection();
    	    else
    	        open = ConnectionSettings.EVER_ACCEPTED_INCOMING.getValue();
    	    
    	    
    		if (open && NetworkUtils.isValidExternalIpPort(IpPortForSelf.instance())) {
    		    return new DirectAltLoc(new ConnectableImpl(
    		                NetworkUtils.ip2string(networkManager.getAddress()),
    		                networkManager.getPort(),
    		                SSLSettings.isIncomingTLSEnabled())
    		            , urn);
    		} else { 
    			return new PushAltLoc(urn);
    		}
    		
    	}catch(IOException bad) {
    		ErrorService.error(bad);
    		return null;
    	}
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.altlocs.AlternateLocationFactory#create(com.limegroup.gnutella.RemoteFileDesc)
     */
    public AlternateLocation create(final RemoteFileDesc rfd) 
    	                                                    throws IOException {
    	if(rfd == null)
    		throw new NullPointerException("cannot accept null RFD");
    
    	URN urn = rfd.getSHA1Urn();
    	if(urn == null)
    	    throw new NullPointerException("cannot accept null URN");
    
    	if (!rfd.needsPush()) {
    		return new DirectAltLoc(new ConnectableImpl(rfd.getHost(),rfd.getPort(), rfd.isTLSCapable()), urn);
    	} else {
    	    PushEndpoint copy;
            if (rfd.getPushAddr() != null) 
                copy = rfd.getPushAddr();
            else 
                copy = new PushEndpoint(rfd.getClientGUID(),IpPort.EMPTY_SET,PushEndpoint.PLAIN,0,null);
    	    return new PushAltLoc(copy,urn);
    	} 
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.altlocs.AlternateLocationFactory#createPushAltLoc(com.limegroup.gnutella.PushEndpoint, com.limegroup.gnutella.URN)
     */
    public AlternateLocation createPushAltLoc(PushEndpoint pe, URN urn) throws IOException {
        return new PushAltLoc(pe, urn);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.altlocs.AlternateLocationFactory#createDirectDHTAltLoc(org.limewire.io.IpPort, com.limegroup.gnutella.URN, long, byte[])
     */
    public AlternateLocation createDirectDHTAltLoc(IpPort ipp, URN urn, 
            long fileSize, byte[] ttroot) throws IOException {
        return new DirectDHTAltLoc(ipp, urn, fileSize, ttroot);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.altlocs.AlternateLocationFactory#createDirectAltLoc(org.limewire.io.IpPort, com.limegroup.gnutella.URN)
     */
    public AlternateLocation createDirectAltLoc(IpPort ipp, URN urn) throws IOException {
        return new DirectAltLoc(ipp, urn);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.altlocs.AlternateLocationFactory#create(java.lang.String, com.limegroup.gnutella.URN, boolean)
     */
    public AlternateLocation create(String location,
                                           URN urn,
                                           boolean tlsCapable) throws IOException {
        if(location == null || location.equals(""))
            throw new IOException("null or empty location");
        if(urn == null)
            throw new IOException("null URN.");
         
        // Case 1. Direct Alt Loc
        if (location.indexOf(";")==-1) {
        	IpPort addr = AlternateLocationFactoryImpl.createUrlFromMini(location, urn, tlsCapable);
    		return new DirectAltLoc(addr, urn);
        }
        
        //Case 2. Push Alt loc
        PushEndpoint pe = new PushEndpoint(location);
        return new PushAltLoc(pe,urn);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.altlocs.AlternateLocationFactory#create(java.lang.String, com.limegroup.gnutella.URN)
     */
    public AlternateLocation create(final String location,
                                           final URN urn) throws IOException {
        return create(location, urn, false);
    }

    /**
     * Creates a new <tt>URL</tt> based on the IP and port in the location
     * The location MUST be a dotted IP address.
     */
    private static IpPort createUrlFromMini(final String location, URN urn, boolean tlsCapable)
      throws IOException {
        int port = location.indexOf(':');
        final String loc =
            (port == -1 ? location : location.substring(0, port));
        //Use the IP class as a quick test to make sure it numeric
        try {
            new IP(loc);
        } catch(IllegalArgumentException iae) {
            throw new IOException("invalid location: " + location);
        }
        //But, IP still could have passed if it thought there was a submask
        if( loc.indexOf('/') != -1 )
            throw new IOException("invalid location: " + location);
    
        //Then make sure it's a valid IP addr.
        if(!NetworkUtils.isValidAddress(loc))
            throw new IOException("invalid location: " + location);
        
        if( port == -1 )
            port = 6346; // default port if not included.
        else {
            // Not enough room for a port.
            if(location.length() < port+1)
                throw new IOException("invalid location: " + location);
            try {
                port = Short.parseShort(location.substring(port+1));
            } catch(NumberFormatException nfe) {
                throw new IOException("invalid location: " + location);
            }
        }
        
        if(!NetworkUtils.isValidPort(port))
            throw new IOException("invalid port: " + port);
        
        return new ConnectableImpl(loc,port, tlsCapable);
    }

}
