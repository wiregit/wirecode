package com.limegroup.gnutella.altlocs;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.filters.IP;
import com.limegroup.gnutella.http.*;
import com.limegroup.gnutella.util.*;
import java.net.*;
import java.util.StringTokenizer;
import java.io.*;
import com.sun.java.util.collections.Set;
import com.sun.java.util.collections.HashSet;
import com.sun.java.util.collections.Comparable;

/**
 * This class encapsulates the data for an alternate resource location, as 
 * specified in HUGE v0.93.  This also provides utility methods for such 
 * operations as comparing alternate locations based on the date they were 
 * stored.
 */
public final class AlternateLocation implements HTTPHeaderValue, Comparable {
    
    /**
     * The vendor to use.
     */
    public static final String ALT_VENDOR = "ALT";

	/**
	 * A <tt>URL</tt> instance for the URL specified in the header.
	 */
	private final URL URL;

	/**
	 * Constant for the sha1 urn for this <tt>AlternateLocation</tt> --
	 * can be <tt>null</tt>.
	 */
	private final URN SHA1_URN;
	
	/**
	 * Constant for the string to display as the httpStringValue.
	 */
	private final String DISPLAY_STRING;

	/**
	 * Cached hash code that is lazily initialized.
	 */
	private volatile int hashCode = 0;

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
    private volatile int _count = 0;

    /**
     * Remembers if this AltLoc ever failed, if it did _demoted is set. If this
     * succeeds, it may be promoted again resetting the value of _demoted.  The
     * _count attribute does does take into account the case of a good alternate
     * location with a high count, which has recently failed. 
     * <p> 
     * Note that demotion in not intrinsic to the use of this class, some
     * modules like the download may not want to demote an AlternatLocation, 
     * other like the uploader may rely on it.
     */
    private volatile boolean _demoted = false;


    private boolean _old = false;

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
		return new AlternateLocation(url, sha1);
	}
	
	/**
	 * Constructs a new <tt>AlternateLocation</tt> instance based on the
	 * specified string argument and URN.
	 *
	 * @param location a string containing one of the following:
	 *  "http://my.address.com:port#/uri-res/N2R?urn:sha:SHA1LETTERS"
	 *  "1.2.3.4[:6346]"
	 * If the first is given, then the SHA1 in the string MUST match
	 * the SHA1 given.
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
         
        // Case 1.   
        if(location.toLowerCase().startsWith("http")) {
            AlternateLocation al = create(location);
            if(!al.SHA1_URN.equals(urn))
                throw new IOException("mismatched URN");
            return al;
        }
        
        // Case 2.
		URL url = AlternateLocation.createUrlFromMini(location, urn);
		return new AlternateLocation(url, urn);
    }

	/**
	 * Creates a new <tt>AlternateLocation</tt> instance for the given 
	 * <tt>URL</tt> instance.  This constructor creates an alternate
	 * location with the current date and time as its timestamp.
	 * This can be used, for example, for newly uploaded files.
	 *
	 * @param url the <tt>URL</tt> instance for the resource
	 * @throws <tt>NullPointerException</tt> if the <tt>url</tt> argument is 
	 *  <tt>null</tt>
	 * @throws <tt>MalformedURLException</tt> if a copy of the supplied 
	 *  <tt>URL</tt> instance cannot be successfully created
	 * @throws <tt>IOException</tt> if the url argument is not a
	 *  valid location for any reason
	 */
	public static AlternateLocation create(final URL url) 
		                             throws MalformedURLException, IOException {
		if(url == null) 
			throw new NullPointerException("cannot accept null URL");

		// create a new URL instance from the data for the given url
		// and the urn
		URL tempUrl = new URL(url.getProtocol(), url.getHost(), url.getPort(),
							  url.getFile());
		URN sha1 = URN.createSHA1UrnFromURL(tempUrl);
		return new AlternateLocation(tempUrl, sha1);
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
		int port = rfd.getPort();

		URL url = new URL("http", rfd.getHost(), port,						  
						  HTTPConstants.URI_RES_N2R + urn.httpStringValue());
		return new AlternateLocation(url, urn);
	}

	/**
	 * Creates a new <tt>AlternateLocation</tt> for a file stored locally 
	 * with the specified <tt>URN</tt>.
	 *
	 * @param urn the <tt>URN</tt> of the locally stored file
	 */
	public static AlternateLocation create(URN urn) {
		if(urn == null) throw new NullPointerException("null sha1");
		URL url = null;
        try {
            int port = RouterService.getPort();
            String addr = NetworkUtils.ip2string(RouterService.getAddress());
			url = new URL("http", addr, port,
                          HTTPConstants.URI_RES_N2R + urn.httpStringValue());
        } catch(MalformedURLException e) {
            ErrorService.error(e);
        }
        try {
		    return new AlternateLocation(url, urn);
        } catch(IOException ioe) {
            throw new IllegalArgumentException(ioe.getMessage());
        }
	}

	/**
	 * Creates a new <tt>AlternateLocation</tt> with the specified <tt>URL</tt>
	 * and <tt>Date</tt> timestamp.
	 *
	 * @param url the <tt>URL</tt> for the <tt>AlternateLocation</tt>
	 * @param date the <tt>Date</tt> timestamp for the 
	 *  <tt>AlternateLocation</tt>
	 */
	private AlternateLocation(final URL url, final URN sha1)
	  throws IOException {
		if(!NetworkUtils.isValidPort(url.getPort()))
			throw new IOException("invalid port");
        if(!NetworkUtils.isValidAddress(url.getHost()))
            throw new IOException("invalid address");			
        if(NetworkUtils.isPrivateAddress(url.getHost()))
            throw new IOException("invalid address: " + url.getHost());
        if(sha1 == null)
            throw new IOException("invalid/null sha1");	    
	    
		this.URL       = url;
		this.SHA1_URN  = sha1;
		InetAddress ia = InetAddress.getByName(URL.getHost());
		String ip = NetworkUtils.ip2string(ia.getAddress());
		if( URL.getPort() == 6346 )
		    DISPLAY_STRING = ip;
		else
		    DISPLAY_STRING = ip + ":" + URL.getPort();
        _count = 1;
        _demoted = false;
	}

    //////////////////////////////accessors////////////////////////////

	/**
	 * Returns an instance of the <tt>URL</tt> instance for this alternate
	 * location.  
     * <p>
	 * @return a <tt>URL</tt> instance corresponding to the URL for this
	 * alternate location, or <tt>null</tt> if an instance could not be created
	 */
	public URL getUrl() {
		try {
			return new URL(this.URL.getProtocol(), this.URL.getHost(), 
						   this.URL.getPort(),this.URL.getFile());
		} catch(MalformedURLException e) {
			// this should never happen in practice, but retun null nevertheless
			return null;
		}
	}
	
	/**
	 * Returns the host/port of this alternate location as an endpoint.
	 */
	public Endpoint getHost() {
	    return new Endpoint(this.URL.getHost(), this.URL.getPort());
	}

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
    public synchronized boolean getDemoted() { return _demoted; }
    
    ////////////////////////////Mesh utility methods////////////////////////////

	public String httpStringValue() {
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
	public RemoteFileDesc createRemoteFileDesc(int size) {
		Set urnSet = new HashSet();
		urnSet.add(getSHA1Urn());
        int quality = _old ? 2 : 3;
		return new RemoteFileDesc(URL.getHost(), URL.getPort(),
								  0, URL.getFile(), size,  
								  DataUtils.EMPTY_GUID, 1000,
								  true, quality, false, null, urnSet, false,
                                  false, //assume altLoc is not firewalled
                                  ALT_VENDOR,//Never displayed, and we don't know
                                  System.currentTimeMillis(), null);
	}

    /**
     * increment the count.
     * @see demote
     */
    public synchronized void increment() { _count++; }

    /**
     * package access for demoting this.
     */
    synchronized void  demote() { _demoted = true; }

    /**
     * package access for promoting this.
     */
    synchronized void promote() { _demoted = false; }

    /**
     * to set this._old to true, meaning that this is a location from the old
     * mesh.
     */
    public void setOld() { _old = true; }

    /**
     * could return null
     */ 
    public synchronized AlternateLocation createClone() {
        AlternateLocation ret = null;
        try {
            ret = new AlternateLocation(this.URL, this.SHA1_URN);
        } catch(IOException ioe) {
            ErrorService.error(ioe);
            return null;
        }
        ret._demoted = this._demoted;
        ret._count = this._count;
        return ret;
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
		String test = locationHeader.toLowerCase();
		
		//Doesn't start with http? Bad.
		if(!test.startsWith("http"))
		    throw new IOException("invalid location: " + locationHeader);
		
		//Had multiple http's in it? Bad.
		if(test.lastIndexOf("http://") > 4) 
            throw new IOException("invalid location: " + locationHeader);
            
        String urlStr = AlternateLocation.removeTimestamp(locationHeader);
        URL url = new URL(urlStr);
        String host = url.getHost();
        int    port = url.getPort();
        
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
	private static URL createUrlFromMini(final String location, URN urn)
	  throws IOException {
	    int port = location.indexOf(':');
	    final String loc =
	        (port == -1 ? location : location.substring(0, port));
        //Use the IP class as a quick test to make sure it was a valid location
        try {
            new IP(loc);
        } catch(IllegalArgumentException iae) {
            throw new IOException("invalid location: " + location);
        }
        
        //But, IP still could have passed if it thought there was a submask
        if( loc.indexOf('/') != -1 )
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
	    
	    return new URL("http", loc, port,
	                HTTPConstants.URI_RES_N2R + urn.httpStringValue());
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
		AlternateLocation al = (AlternateLocation)obj;
		return URL == null ? al.URL == null : URL.equals(al.URL);
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
    public int compareTo(Object obj) {
        if (this==obj) //equal
            return 0;
        if(obj == null) //I am greater
            return 1;
        if( !(obj instanceof AlternateLocation) ) //I am greater
            return 1;
        AlternateLocation other = (AlternateLocation) obj;
        if(_demoted != other._demoted) {
            if(_demoted) //I am demoted and not him
                return 1; //I am higher
            return -1;//he is demoted, and I am not
        }
        //both have the same value for _demoted
        int ret = _count - other._count;
        if(ret!=0) 
            return ret;
        if (this.URL.equals(other.URL)) 
            return 0;
        ret = this.URL.getHost().compareTo(other.URL.getHost());
        if(ret!=0)
            return ret;
        return (this.URL.getPort() - other.URL.getPort());
    }

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
		if(hashCode == 0)
		    hashCode = 37 * this.URL.hashCode();
		return hashCode;
	}

	/**
	 * Overrides toString to return a string representation of this 
	 * <tt>AlternateLocation</tt>, namely the url and the date.
	 *
	 * @return the string representation of this alternate location
	 */
	public String toString() {
        return this.URL.toExternalForm()+","+_count+","+_demoted;
	}

}









