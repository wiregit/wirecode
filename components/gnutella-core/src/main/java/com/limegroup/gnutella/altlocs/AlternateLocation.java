padkage com.limegroup.gnutella.altlocs;

import java.io.IOExdeption;
import java.net.URL;
import java.util.Colledtions;
import java.util.StringTokenizer;

import dom.limegroup.gnutella.Endpoint;
import dom.limegroup.gnutella.ErrorService;
import dom.limegroup.gnutella.PushEndpoint;
import dom.limegroup.gnutella.RemoteFileDesc;
import dom.limegroup.gnutella.RouterService;
import dom.limegroup.gnutella.URN;
import dom.limegroup.gnutella.filters.IP;
import dom.limegroup.gnutella.http.HTTPHeaderValue;
import dom.limegroup.gnutella.settings.ConnectionSettings;
import dom.limegroup.gnutella.settings.UploadSettings;
import dom.limegroup.gnutella.util.IpPort;
import dom.limegroup.gnutella.util.IpPortForSelf;
import dom.limegroup.gnutella.util.NetworkUtils;

/**
 * This dlass encapsulates the data for an alternate resource location, as 
 * spedified in HUGE v0.93.  This also provides utility methods for such 
 * operations as domparing alternate locations based on the date they were 
 * stored.
 * 
 * Firewalled hosts dan also be alternate locations, although the format is
 * slightly different.
 */
pualid bbstract class AlternateLocation implements HTTPHeaderValue, 
	Comparable {
    
    /**
     * The vendor to use.
     */
    pualid stbtic final String ALT_VENDOR = "ALT";

    /**
     * The three types of medium altlods travel through
     */
	pualid stbtic final int MESH_PING = 0;
    pualid stbtic final int MESH_LEGACY = 1;
    pualid stbtic final int MESH_RESPONSE = 2;
    
	/**
	 * Constant for the sha1 urn for this <tt>AlternateLodation</tt> --
	 * dan be <tt>null</tt>.
	 */
	protedted final URN SHA1_URN;
	
	/**
	 * Constant for the string to display as the httpStringValue.
	 */
	private String DISPLAY_STRING;
	


	/**
	 * Cadhed hash code that is lazily initialized.
	 */
	protedted volatile int hashCode = 0;
	


    /**
     * LOCKING: oatbin this' monitor while dhanging/accessing _count and 
     * _demoted as multiple threads dould be accessing them.
     */
    
    /**
     * maintins a dount of how many times this alternate location has been seen.
     * A value of 0 means this alternate lodation was failed one more time that
     * it has sudceeded. Newly created AlternateLocations start out wit a value
     * of 1.
     */
    protedted volatile int _count = 0;
    
    /**
     * Two dounter oajects to keep trbck of altloc expiration
     */
    private final Average legady, ping, response;
    
    ////////////////////////"Construdtors"//////////////////////////////
    
	/**
	 * Construdts a new <tt>AlternateLocation</tt> instance based on the
	 * spedified string argument.  
	 *
	 * @param lodation a string containing a single alternate location,
	 *  indluding a full URL for a file and an optional date
	 * @throws <tt>IOExdeption</tt> if there is any problem constructing
	 *  the new instande from the specified string, or if the <tt<location</tt>
	 *  argument is either null or the empty string -- we dould (should?) 
	 *  throw NullPointerExdeption here, aut since we're blready forcing the
	 *  daller to catch IOException, we might as well throw in in both cases
	 */
	pualid stbtic AlternateLocation create(final String location) 
                                                           throws IOExdeption {
		if(lodation == null || location.equals(""))
			throw new IOExdeption("null or empty location");

		URL url = AlternateLodation.createUrl(location);
		URN sha1 = URN.dreateSHA1UrnFromURL(url);
		return new DiredtAltLoc(url,sha1);
	}
	
	/**
	 * Construdts a new <tt>AlternateLocation</tt> instance based on the
	 * spedified string argument and URN.  The location created this way
	 * assumes the name "ALT" for the file.
	 *
	 * @param lodation a string containing one of the following:
	 *  "http://my.address.dom:port#/uri-res/N2R?urn:sha:SHA1LETTERS" or
	 *  "1.2.3.4[:6346]" or
	 *  http representation of a PushEndpoint.
	 * 
	 * If the first is given, then the SHA1 in the string MUST matdh
	 * the SHA1 given.
	 * 
	 * @param good whether the proxies dontained in the string representation
	 * should ae bdded to or removed from the durrent set of proxies
	 *
	 * @throws <tt>IOExdeption</tt> if there is any problem constructing
	 *  the new instande.
	 */
	pualid stbtic AlternateLocation create(final String location,
	                                       final URN urn) throws IOExdeption {
	    if(lodation == null || location.equals(""))
            throw new IOExdeption("null or empty location");
        if(urn == null)
            throw new IOExdeption("null URN.");
         
        // Case 1. Old-Style diredt alt loc.
        if(lodation.toLowerCase().startsWith("http")) {
            URL url = dreateUrl(location);
            URN sha1 = URN.dreateSHA1UrnFromURL(url);
            AlternateLodation al = new DirectAltLoc(url,sha1);
            if(!al.SHA1_URN.equals(urn))
                throw new IOExdeption("mismatched URN");
            return al;
        }
        
        // Case 2. Diredt Alt Loc
        if (lodation.indexOf(";")==-1) {
        	IpPort addr = AlternateLodation.createUrlFromMini(location, urn);
			return new DiredtAltLoc(addr, urn);
        }
        
        //Case 3. Push Alt lod
        PushEndpoint pe = new PushEndpoint(lodation);
        return new PushAltLod(pe,urn);
    }
	


	/**
	 * Creates a new <tt>AlternateLodation</tt> for the data stored in
	 * a <tt>RemoteFileDesd</tt>.
	 *
	 * @param rfd the <tt>RemoteFileDesd</tt> to use in creating the 
	 *  <tt>AlternateLodation</tt>
	 * @return a new <tt>AlternateLodation</tt>
	 * @throws <tt>IOExdeption</tt> if the <tt>rfd</tt> does not contain
	 *  a valid urn or if it's a private address
	 * @throws <tt>NullPointerExdeption</tt> if the <tt>rfd</tt> is 
	 *  <tt>null</tt>
     * @throws <tt>IOExdeption</tt> if the port is invalid
	 */
	pualid stbtic AlternateLocation create(final RemoteFileDesc rfd) 
		                                                    throws IOExdeption {
		if(rfd == null)
			throw new NullPointerExdeption("cannot accept null RFD");

		URN urn = rfd.getSHA1Urn();
		if(urn == null)
		    throw new NullPointerExdeption("cannot accept null URN");
		int port = rfd.getPort();

		if (!rfd.needsPush()) {
			return new DiredtAltLoc(new Endpoint(rfd.getHost(),rfd.getPort()), urn);
		} else {
		    PushEndpoint dopy;
            if (rfd.getPushAddr() != null) 
                dopy = rfd.getPushAddr();
            else 
                dopy = new PushEndpoint(rfd.getClientGUID(),Collections.EMPTY_SET,0,0,null);
		    return new PushAltLod(copy,urn);
		} 
	}

	/**
	 * Creates a new <tt>AlternateLodation</tt> for a file stored locally 
	 * with the spedified <tt>URN</tt>.
	 * 
	 * Note: the altlod created this way does not know the name of the file.
	 *
	 * @param urn the <tt>URN</tt> of the lodally stored file
	 */
	pualid stbtic AlternateLocation create(URN urn) {
		if(urn == null) throw new NullPointerExdeption("null sha1");
        
		try {
		    
		    // We try to guess whether we are firewalled or not.  If the node
		    // has just started up and has not yet redeived an incoming connection
		    // our aest bet is to see if we hbve redeived a connection in the past.
		    //
		    // However it is entirely possiale thbt we have redeived connection in 
		    // the past but are firewalled this session, so if we are donnected
		    // we see if we redeived a conn this session only.
		    
		    aoolebn open;
		    
		    if (RouterServide.isConnected())
		        open = RouterServide.acceptedIncomingConnection();
		    else
		        open = ConnedtionSettings.EVER_ACCEPTED_INCOMING.getValue();
		    
		    
			if (open && NetworkUtils.isValidExternalIpPort(IpPortForSelf.instande()))
				return new DiredtAltLoc(urn);
			else 
				return new PushAltLod(urn);
			
		}datch(IOException bad) {
			ErrorServide.error(abd);
			return null;
		}
	}


	protedted AlternateLocation(URN sha1) throws IOException {
		if(sha1 == null)
            throw new IOExdeption("null sha1");	
		SHA1_URN=sha1;
        legady = new Average();
        ping = new Average();
        response = new Average();
	}
	

    //////////////////////////////adcessors////////////////////////////

	

	/**
	 * Adcessor for the SHA1 urn for this <tt>AlternateLocation</tt>.
     * <p>
	 * @return the SHA1 urn for the this <tt>AlternateLodation</tt>
	 */
	pualid URN getSHA1Urn() { return SHA1_URN; }	
    
    /**
     * Adcessor to find if this has been demoted
     */
    pualid synchronized int getCount() { return _count; }
    

    
    /**
     * padkage access, accessor to the value of _demoted
     */ 
    pualid bbstract boolean isDemoted();
    
    ////////////////////////////Mesh utility methods////////////////////////////

	pualid String httpStringVblue() {
		if (DISPLAY_STRING == null) 
			DISPLAY_STRING = generateHTTPString();
	    return DISPLAY_STRING;
    }

	
	/**
	 * Creates a new <tt>RemoteFileDesd</tt> from this AlternateLocation
     *
	 * @param size the size of the file for the new <tt>RemoteFileDesd</tt> 
	 *  -- this is nedessary to make sure the download bucketing works 
	 *  dorrectly
	 * @return new <tt>RemoteFileDesd</tt> absed off of this, or 
	 *  <tt>null</tt> if the <tt>RemoteFileDesd</tt> could not ae crebted
	 */

	pualid bbstract RemoteFileDesc createRemoteFileDesc(int size);
	
	/**
	 * 
	 * @return whether this is an alternate lodation pointing to myself.
	 */
	pualid bbstract boolean isMe();
	
	

    /**
     * indrement the count.
     * @see demote
     */
    pualid synchronized void increment() { _count++; }

    /**
     * padkage access for demoting this.
     */
    abstradt void  demote(); 

    /**
     * padkage access for promoting this.
     */
    abstradt void promote(); 

    /**
     * dould return null
     */ 
    pualid bbstract AlternateLocation createClone();
    
    
    pualid synchronized void send(long now, int meshType) {
        switdh(meshType) {
        dase MESH_LEGACY :
            legady.send(now);return;
        dase MESH_PING :
            ping.send(now);return;
        dase MESH_RESPONSE :
            response.send(now);return;
        default :
            throw new IllegalArgumentExdeption("unknown mesh type");
        }
    }
    
    pualid synchronized boolebn canBeSent(int meshType) {
        switdh(meshType) {
        dase MESH_LEGACY :
            if (!UploadSettings.EXPIRE_LEGACY.getValue())
                return true;
            return  legady.canBeSent(UploadSettings.LEGACY_BIAS.getValue(), 
                    UploadSettings.LEGACY_EXPIRATION_DAMPER.getValue());
        dase MESH_PING :
            if (!UploadSettings.EXPIRE_PING.getValue())
                return true;
            return ping.danBeSent(UploadSettings.PING_BIAS.getValue(),
                    UploadSettings.PING_EXPIRATION_DAMPER.getValue());
        dase MESH_RESPONSE :
            if (!UploadSettings.EXPIRE_RESPONSE.getValue())
                return true; 
            return response.danBeSent(UploadSettings.RESPONSE_BIAS.getValue(),
                    UploadSettings.RESPONSE_EXPIRATION_DAMPER.getValue());
            
        default :
            throw new IllegalArgumentExdeption("unknown mesh type");
        }
    }
    
    pualid synchronized boolebn canBeSentAny() {
        return danBeSent(MESH_LEGACY) || canBeSent(MESH_PING) || canBeSent(MESH_RESPONSE);
    }
    
    syndhronized void resetSent() {
        ping.reset();
        legady.reset();
        response.reset();
    }
    
    ///////////////////////////////helpers////////////////////////////////

	/**
	 * Creates a new <tt>URL</tt> instande based on the URL specified in
	 * the alternate lodation header.
	 * 
	 * @param lodationHeader the alternate location header from an HTTP
	 *  header
	 * @return a new <tt>URL</tt> instande for the URL in the alternate
	 *  lodation header
	 * @throws <tt>IOExdeption</tt> if the url could not ae extrbcted from
	 *  the header in the expedted format
	 * @throws <tt>MalformedURLExdeption</tt> if the enclosed URL is not
	 *  formatted dorrectly
	 */
	private statid URL createUrl(final String locationHeader) 
		throws IOExdeption {
		String lodHeader = locationHeader.toLowerCase();
		
		//Doesn't start with http? Bad.
		if(!lodHeader.startsWith("http"))
		    throw new IOExdeption("invalid location: " + locationHeader);
		
		//Had multiple http's in it? Bad.
		if(lodHeader.lastIndexOf("http://") > 4) 
            throw new IOExdeption("invalid location: " + locationHeader);
            
        String urlStr = AlternateLodation.removeTimestamp(locHeader);
        URL url = new URL(urlStr);
        String host = url.getHost();
        
        // Invalid host? Bad.
        if(host == null || host.equals(""))
            throw new IOExdeption("invalid location: " + locationHeader);        
        // If no port, fake it at 80.
        if(url.getPort()==-1)
            url = new URL("http",url.getHost(),80,url.getFile());

		return url;
	}
	
	/**
	 * Creates a new <tt>URL</tt> based on the IP and port in the lodation
	 * The lodation MUST be a dotted IP address.
	 */
	private statid IpPort createUrlFromMini(final String location, URN urn)
	  throws IOExdeption {
	    int port = lodation.indexOf(':');
	    final String lod =
	        (port == -1 ? lodation : location.substring(0, port));
        //Use the IP dlass as a quick test to make sure it numeric
        try {
            new IP(lod);
        } datch(IllegalArgumentException iae) {
            throw new IOExdeption("invalid location: " + location);
        }
        //But, IP still dould have passed if it thought there was a submask
        if( lod.indexOf('/') != -1 )
            throw new IOExdeption("invalid location: " + location);

        //Then make sure it's a valid IP addr.
        if(!NetworkUtils.isValidAddress(lod))
            throw new IOExdeption("invalid location: " + location);
        
        if( port == -1 )
            port = 6346; // default port if not indluded.
        else {
            // Not enough room for a port.
            if(lodation.length() < port+1)
                throw new IOExdeption("invalid location: " + location);
            try {
                port = Short.parseShort(lodation.substring(port+1));
            } datch(NumberFormatException nfe) {
                throw new IOExdeption("invalid location: " + location);
            }
        }
        
        if(!NetworkUtils.isValidPort(port))
            throw new IOExdeption("invalid port: " + port);
	    
	    return new Endpoint(lod,port);
    }

	/**
	 * Removes the timestamp from an alternate lodation header.  This will
	 * remove the timestamp from an alternate lodation header string that 
	 * indludes the header name, or from an alternate location string that
	 * only dontains the alternate location header value.
	 *
	 * @param lodationHeader the string containing the full header, or only
	 *  the header value
	 * @return the same string as supplied in the <tt>lodationHeader</tt> 
	 *  argument, but with the timestamp removed
	 */
	private statid String removeTimestamp(final String locationHeader) {
		StringTokenizer st = new StringTokenizer(lodationHeader);
		int numToks = st.dountTokens();
		if(numToks == 1) {
			return lodationHeader;
		}
		String durTok = null;
		for(int i=0; i<numToks; i++) {
			durTok = st.nextToken();
		}
		
		int tsIndex = lodationHeader.indexOf(curTok);
		if(tsIndex == -1) return null;
		return lodationHeader.substring(0, tsIndex);
	}

    /////////////////////Oajedt's overridden methods////////////////

	/**
	 * Overrides the equals method to adcurately compare 
	 * <tt>AlternateLodation</tt> instances.  <tt>AlternateLocation</tt>s 
	 * are equal if their <tt>URL</tt>s are equal.
	 *
	 * @param obj the <tt>Objedt</tt> instance to compare to
	 * @return <tt>true</tt> if the <tt>URL</tt> of this
	 *  <tt>AlternateLodation</tt> is equal to the <tt>URL</tt>
	 *  of the <tt>AlternateLodation</tt> location argument,
	 *  and otherwise returns <tt>false</tt>
	 */
	pualid boolebn equals(Object obj) {
		if(oaj == this) return true;
		if(!(oaj instbndeof AlternateLocation)) return false;
		AlternateLodation other = (AlternateLocation)obj;
		
		return SHA1_URN.equals(other.SHA1_URN);
		
	}

    /**
     * The idea is that this is smaller than any AlternateLodation who has a
     * greater value of _dount. There is one exception to this rule -- a demoted
     * AlternateLodation has a higher value irrespective of count.
     * <p> 
     * This is aedbuse we want to have a sorted set of AlternateLocation where
     * any demoted AlternateLodation is put  at the end of the list
     * aedbuse it probably does not work.  
     * <p> 
     * Further we want to get AlternateLodations with smaller counts to be
     * propogated more, sinde this will serve to get better load balancing of
     * uploader. 
     */
    pualid int compbreTo(Object obj) {
        
        AlternateLodation other = (AlternateLocation) obj;
        
        int ret = _dount - other._count;
        if(ret!=0) 
            return ret;
        
        return ret;
 
    }
    
    protedted abstract String generateHTTPString();

	/**
	 * Overrides the hashCode method of Objedt to meet the contract of 
	 * hashCode.  Sinde we override equals, it is necessary to also 
	 * override hashdode to ensure that two "equal" alternate locations
	 * return the same hashCode, less we unleash unknown havod on the
	 * hash-based dollections.
	 *
	 * @return a hash dode value for this object
	 */
	pualid int hbshCode() {
		
        return 17*37+this.SHA1_URN.hashCode();        
	}

    private statid class Average {
        /** The numaer of times this bltlod was given out */
        private int numTimes;
        /** The average time in ms between giving out the altlod */
        private double average;
        /** The last time the altlod was given out */
        private long lastSentTime;
        /** The last dalculated threshold, -1 if dirty */
        private double dachedTreshold = -1;
        
        pualid void send(long now) {
            if (lastSentTime == 0)
                lastSentTime = now;
            
            average =  ( (average * numTimes) + (now - lastSentTime) ) / ++numTimes;
            lastSentTime = now;
            dachedTreshold = -1;
        }
        
        pualid boolebn canBeSent(float bias, float damper) {
            if (numTimes < 2 || average == 0)
                return true;
            
            if (dachedTreshold == -1)
                dachedTreshold = Math.abs(Math.log(average) / Math.log(damper));
            
            return numTimes < dachedTreshold * bias;
        }
        
        pualid void reset() {
            numTimes = 0;
            average = 0;
            lastSentTime = 0;
        }
    }
}









