package com.limegroup.gnutella.altlocs;


import java.io.IOException;
import java.net.URL;
import java.util.StringTokenizer;

import org.limewire.io.IP;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortForSelf;
import org.limewire.io.IpPortImpl;
import org.limewire.io.NetworkUtils;
import org.limewire.service.ErrorService;

import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.http.HTTPHeaderValue;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.UploadSettings;

/**
 * This class encapsulates the data for an alternate resource location, as 
 * specified in HUGE v0.93.  This also provides utility methods for such 
 * operations as comparing alternate locations based on the date they were 
 * stored.
 * 
 * Firewalled hosts can also be alternate locations, although the format is
 * slightly different.
 */
public abstract class AlternateLocation implements HTTPHeaderValue, Comparable<AlternateLocation> {
    
    /**
     * The vendor to use.
     */
    public static final String ALT_VENDOR = "ALT";

    /**
     * The three types of medium altlocs travel through
     */
	public static final int MESH_PING = 0;
    public static final int MESH_LEGACY = 1;
    public static final int MESH_RESPONSE = 2;
    
	/**
	 * Constant for the sha1 urn for this <tt>AlternateLocation</tt> --
	 * can be <tt>null</tt>.
	 */
	protected final URN SHA1_URN;
	
	/**
	 * Constant for the string to display as the httpStringValue.
	 */
	private String DISPLAY_STRING;
	


	/**
	 * Cached hash code that is lazily initialized.
	 */
	protected volatile int hashCode = 0;
	


    /**
     * LOCKING: obtain this' monitor while changing/accessing _count and 
     * _demoted as multiple threads could be accessing them.
     */
    
    /**
     * maintins a count of how many times this alternate location has been seen.
     * A value of 0 means this alternate location was failed one more time that
     * it has succeeded. Newly created AlternateLocations start out wit a value
     * of 1.
     */
    protected volatile int _count = 0;
    
    /**
     * Two counter objects to keep track of altloc expiration
     */
    private final Average legacy, ping, response;
    
    ////////////////////////"Constructors"//////////////////////////////
    
	/**
	 * Constructs a new <tt>AlternateLocation</tt> instance based on the
	 * specified string argument.  
	 *
	 * @param location a string containing a single alternate location,
	 *  including a full URL for a file and an optional date
	 * @throws <tt>IOException</tt> if there is any problem constructing
	 *  the new instance from the specified string, or if the <tt<location</tt>
	 *  argument is either null or the empty string -- we could (should?) 
	 *  throw NullPointerException here, but since we're already forcing the
	 *  caller to catch IOException, we might as well throw in in both cases
	 */
	public static AlternateLocation create(final String location) 
                                                           throws IOException {
		if(location == null || location.equals(""))
			throw new IOException("null or empty location");

		URL url = AlternateLocation.createUrl(location);
		URN sha1 = URN.createSHA1UrnFromURL(url);
		return new DirectAltLoc(url,sha1);
	}
	
	/**
	 * Constructs a new <tt>AlternateLocation</tt> instance based on the
	 * specified string argument and URN.  The location created this way
	 * assumes the name "ALT" for the file.
	 *
	 * @param location a string containing one of the following:
	 *  "http://my.address.com:port#/uri-res/N2R?urn:sha:SHA1LETTERS" or
	 *  "1.2.3.4[:6346]" or
	 *  http representation of a PushEndpoint.
	 * 
	 * If the first is given, then the SHA1 in the string MUST match
	 * the SHA1 given.
	 * 
	 * @param good whether the proxies contained in the string representation
	 * should be added to or removed from the current set of proxies
	 *
	 * @throws <tt>IOException</tt> if there is any problem constructing
	 *  the new instance.
	 */
	public static AlternateLocation create(final String location,
	                                       final URN urn) throws IOException {
	    if(location == null || location.equals(""))
            throw new IOException("null or empty location");
        if(urn == null)
            throw new IOException("null URN.");
         
        // Case 1. Old-Style direct alt loc.
        if(location.toLowerCase().startsWith("http")) {
            URL url = createUrl(location);
            URN sha1 = URN.createSHA1UrnFromURL(url);
            AlternateLocation al = new DirectAltLoc(url,sha1);
            if(!al.SHA1_URN.equals(urn))
                throw new IOException("mismatched URN");
            return al;
        }
        
        // Case 2. Direct Alt Loc
        if (location.indexOf(";")==-1) {
        	IpPort addr = AlternateLocation.createUrlFromMini(location, urn);
			return new DirectAltLoc(addr, urn);
        }
        
        //Case 3. Push Alt loc
        PushEndpoint pe = new PushEndpoint(location);
        return new PushAltLoc(pe,urn);
    }
	


	/**
	 * Creates a new <tt>AlternateLocation</tt> for the data stored in
	 * a <tt>RemoteFileDesc</tt>.
	 *
	 * @param rfd the <tt>RemoteFileDesc</tt> to use in creating the 
	 *  <tt>AlternateLocation</tt>
	 * @return a new <tt>AlternateLocation</tt>
	 * @throws <tt>IOException</tt> if the <tt>rfd</tt> does not contain
	 *  a valid urn or if it's a private address
	 * @throws <tt>NullPointerException</tt> if the <tt>rfd</tt> is 
	 *  <tt>null</tt>
     * @throws <tt>IOException</tt> if the port is invalid
	 */
	public static AlternateLocation create(final RemoteFileDesc rfd) 
		                                                    throws IOException {
		if(rfd == null)
			throw new NullPointerException("cannot accept null RFD");

		URN urn = rfd.getSHA1Urn();
		if(urn == null)
		    throw new NullPointerException("cannot accept null URN");

		if (!rfd.needsPush()) {
			return new DirectAltLoc(new IpPortImpl(rfd.getHost(),rfd.getPort()), urn);
		} else {
		    PushEndpoint copy;
            if (rfd.getPushAddr() != null) 
                copy = rfd.getPushAddr();
            else 
                copy = new PushEndpoint(rfd.getClientGUID(),IpPort.EMPTY_SET,PushEndpoint.PLAIN,0,null);
		    return new PushAltLoc(copy,urn);
		} 
	}

	/**
	 * Creates a new <tt>AlternateLocation</tt> for a file stored locally 
	 * with the specified <tt>URN</tt>.
	 * 
	 * Note: the altloc created this way does not know the name of the file.
	 *
	 * @param urn the <tt>URN</tt> of the locally stored file
	 */
	public static AlternateLocation create(URN urn) {
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
		        open = RouterService.acceptedIncomingConnection();
		    else
		        open = ConnectionSettings.EVER_ACCEPTED_INCOMING.getValue();
		    
		    
			if (open && NetworkUtils.isValidExternalIpPort(IpPortForSelf.instance()))
				return new DirectAltLoc(urn);
			else 
				return new PushAltLoc(urn);
			
		}catch(IOException bad) {
			ErrorService.error(bad);
			return null;
		}
	}


	protected AlternateLocation(URN sha1) throws IOException {
		if(sha1 == null)
            throw new IOException("null sha1");	
		SHA1_URN=sha1;
        legacy = new Average();
        ping = new Average();
        response = new Average();
	}
	

    //////////////////////////////accessors////////////////////////////

	

	/**
	 * Accessor for the SHA1 urn for this <tt>AlternateLocation</tt>.
     * <p>
	 * @return the SHA1 urn for the this <tt>AlternateLocation</tt>
	 */
	public URN getSHA1Urn() { return SHA1_URN; }	
    
    /**
     * Accessor to find if this has been demoted
     */
    public synchronized int getCount() { return _count; }
    

    
    /**
     * package access, accessor to the value of _demoted
     */ 
    public abstract boolean isDemoted();
    
    ////////////////////////////Mesh utility methods////////////////////////////

	public String httpStringValue() {
		if (DISPLAY_STRING == null) 
			DISPLAY_STRING = generateHTTPString();
	    return DISPLAY_STRING;
    }

	
	/**
	 * Creates a new <tt>RemoteFileDesc</tt> from this AlternateLocation
     *
	 * @param size the size of the file for the new <tt>RemoteFileDesc</tt> 
	 *  -- this is necessary to make sure the download bucketing works 
	 *  correctly
	 * @return new <tt>RemoteFileDesc</tt> based off of this, or 
	 *  <tt>null</tt> if the <tt>RemoteFileDesc</tt> could not be created
	 */

	public abstract RemoteFileDesc createRemoteFileDesc(int size);
	
	/**
	 * 
	 * @return whether this is an alternate location pointing to myself.
	 */
	public abstract boolean isMe();
	
	

    /**
     * increment the count.
     * @see demote
     */
    public synchronized void increment() { _count++; }

    /**
     * package access for demoting this.
     */
    abstract void  demote(); 

    /**
     * package access for promoting this.
     */
    abstract void promote(); 

    /**
     * could return null
     */ 
    public abstract AlternateLocation createClone();
    
    
    public synchronized void send(long now, int meshType) {
        switch(meshType) {
        case MESH_LEGACY :
            legacy.send(now);return;
        case MESH_PING :
            ping.send(now);return;
        case MESH_RESPONSE :
            response.send(now);return;
        default :
            throw new IllegalArgumentException("unknown mesh type");
        }
    }
    
    public synchronized boolean canBeSent(int meshType) {
        switch(meshType) {
        case MESH_LEGACY :
            if (!UploadSettings.EXPIRE_LEGACY.getValue())
                return true;
            return  legacy.canBeSent(UploadSettings.LEGACY_BIAS.getValue(), 
                    UploadSettings.LEGACY_EXPIRATION_DAMPER.getValue());
        case MESH_PING :
            if (!UploadSettings.EXPIRE_PING.getValue())
                return true;
            return ping.canBeSent(UploadSettings.PING_BIAS.getValue(),
                    UploadSettings.PING_EXPIRATION_DAMPER.getValue());
        case MESH_RESPONSE :
            if (!UploadSettings.EXPIRE_RESPONSE.getValue())
                return true; 
            return response.canBeSent(UploadSettings.RESPONSE_BIAS.getValue(),
                    UploadSettings.RESPONSE_EXPIRATION_DAMPER.getValue());
            
        default :
            throw new IllegalArgumentException("unknown mesh type");
        }
    }
    
    public synchronized boolean canBeSentAny() {
        return canBeSent(MESH_LEGACY) || canBeSent(MESH_PING) || canBeSent(MESH_RESPONSE);
    }
    
    synchronized void resetSent() {
        ping.reset();
        legacy.reset();
        response.reset();
    }
    
    ///////////////////////////////helpers////////////////////////////////

	/**
	 * Creates a new <tt>URL</tt> instance based on the URL specified in
	 * the alternate location header.
	 * 
	 * @param locationHeader the alternate location header from an HTTP
	 *  header
	 * @return a new <tt>URL</tt> instance for the URL in the alternate
	 *  location header
	 * @throws <tt>IOException</tt> if the url could not be extracted from
	 *  the header in the expected format
	 * @throws <tt>MalformedURLException</tt> if the enclosed URL is not
	 *  formatted correctly
	 */
	private static URL createUrl(final String locationHeader) 
		throws IOException {
		String locHeader = locationHeader.toLowerCase();
		
		//Doesn't start with http? Bad.
		if(!locHeader.startsWith("http"))
		    throw new IOException("invalid location: " + locationHeader);
		
		//Had multiple http's in it? Bad.
		if(locHeader.lastIndexOf("http://") > 4) 
            throw new IOException("invalid location: " + locationHeader);
            
        String urlStr = AlternateLocation.removeTimestamp(locHeader);
        URL url = new URL(urlStr);
        String host = url.getHost();
        
        // Invalid host? Bad.
        if(host == null || host.equals(""))
            throw new IOException("invalid location: " + locationHeader);        
        // If no port, fake it at 80.
        if(url.getPort()==-1)
            url = new URL("http",url.getHost(),80,url.getFile());

		return url;
	}
	
	/**
	 * Creates a new <tt>URL</tt> based on the IP and port in the location
	 * The location MUST be a dotted IP address.
	 */
	private static IpPort createUrlFromMini(final String location, URN urn)
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
	    
	    return new IpPortImpl(loc,port);
    }

	/**
	 * Removes the timestamp from an alternate location header.  This will
	 * remove the timestamp from an alternate location header string that 
	 * includes the header name, or from an alternate location string that
	 * only contains the alternate location header value.
	 *
	 * @param locationHeader the string containing the full header, or only
	 *  the header value
	 * @return the same string as supplied in the <tt>locationHeader</tt> 
	 *  argument, but with the timestamp removed
	 */
	private static String removeTimestamp(final String locationHeader) {
		StringTokenizer st = new StringTokenizer(locationHeader);
		int numToks = st.countTokens();
		if(numToks == 1) {
			return locationHeader;
		}
		String curTok = null;
		for(int i=0; i<numToks; i++) {
			curTok = st.nextToken();
		}
		
		int tsIndex = locationHeader.indexOf(curTok);
		if(tsIndex == -1) return null;
		return locationHeader.substring(0, tsIndex);
	}

    /////////////////////Object's overridden methods////////////////

	/**
	 * Overrides the equals method to accurately compare 
	 * <tt>AlternateLocation</tt> instances.  <tt>AlternateLocation</tt>s 
	 * are equal if their <tt>URL</tt>s are equal.
	 *
	 * @param obj the <tt>Object</tt> instance to compare to
	 * @return <tt>true</tt> if the <tt>URL</tt> of this
	 *  <tt>AlternateLocation</tt> is equal to the <tt>URL</tt>
	 *  of the <tt>AlternateLocation</tt> location argument,
	 *  and otherwise returns <tt>false</tt>
	 */
	public boolean equals(Object obj) {
		if(obj == this) return true;
		if(!(obj instanceof AlternateLocation)) return false;
		AlternateLocation other = (AlternateLocation)obj;
		
		return SHA1_URN.equals(other.SHA1_URN);
		
	}

    /**
     * The idea is that this is smaller than any AlternateLocation who has a
     * greater value of _count. There is one exception to this rule -- a demoted
     * AlternateLocation has a higher value irrespective of count.
     * <p> 
     * This is because we want to have a sorted set of AlternateLocation where
     * any demoted AlternateLocation is put  at the end of the list
     * because it probably does not work.  
     * <p> 
     * Further we want to get AlternateLocations with smaller counts to be
     * propogated more, since this will serve to get better load balancing of
     * uploader. 
     */
    public int compareTo(AlternateLocation other) {
        int ret = _count - other._count;
        if(ret!=0) 
            return ret;
        
        return ret;
 
    }
    
    protected abstract String generateHTTPString();

	/**
	 * Overrides the hashCode method of Object to meet the contract of 
	 * hashCode.  Since we override equals, it is necessary to also 
	 * override hashcode to ensure that two "equal" alternate locations
	 * return the same hashCode, less we unleash unknown havoc on the
	 * hash-based collections.
	 *
	 * @return a hash code value for this object
	 */
	public int hashCode() {
		
        return 17*37+this.SHA1_URN.hashCode();        
	}

    private static class Average {
        /** The number of times this altloc was given out */
        private int numTimes;
        /** The average time in ms between giving out the altloc */
        private double average;
        /** The last time the altloc was given out */
        private long lastSentTime;
        /** The last calculated threshold, -1 if dirty */
        private double cachedTreshold = -1;
        
        public void send(long now) {
            if (lastSentTime == 0)
                lastSentTime = now;
            
            average =  ( (average * numTimes) + (now - lastSentTime) ) / ++numTimes;
            lastSentTime = now;
            cachedTreshold = -1;
        }
        
        public boolean canBeSent(float bias, float damper) {
            if (numTimes < 2 || average == 0)
                return true;
            
            if (cachedTreshold == -1)
                cachedTreshold = Math.abs(Math.log(average) / Math.log(damper));
            
            return numTimes < cachedTreshold * bias;
        }
        
        public void reset() {
            numTimes = 0;
            average = 0;
            lastSentTime = 0;
        }
    }
}









