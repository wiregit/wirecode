
package com.limegroup.gnutella.altlocs;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;

import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.util.NetworkUtils;
import com.sun.java.util.collections.HashSet;
import com.sun.java.util.collections.Set;

/**
 * An alternate location that is directly reachable, i.e. not firewalled.
 */
public class DirectAltLoc extends AlternateLocation {

	/**
	 * A <tt>URL</tt> instance for the URL specified in the header.
	 */
	private final URL URL;
	
	
	/**
	 * Creates a new <tt>AlternateLocation</tt> with the specified <tt>URL</tt>
	 * and <tt>Date</tt> timestamp.
	 *
	 * @param url the <tt>URL</tt> for the <tt>AlternateLocation</tt>
	 * @param date the <tt>Date</tt> timestamp for the 
	 *  <tt>AlternateLocation</tt>
	 */
	protected DirectAltLoc(final URL url, final URN sha1)
	  throws IOException {
		super(sha1);
		if(!NetworkUtils.isValidPort(url.getPort()))
			throw new IOException("invalid port: " + url.getPort());
        if(!NetworkUtils.isValidAddress(url.getHost()))
            throw new IOException("invalid address: " + url.getHost());
        if(NetworkUtils.isPrivateAddress(url.getHost()))
            throw new IOException("invalid address: " + url.getHost());
            
	    
		this.URL       = url;
		InetAddress ia = InetAddress.getByName(URL.getHost());
		String ip = NetworkUtils.ip2string(ia.getAddress());
		if( URL.getPort() == 6346 )
		    DISPLAY_STRING = ip;
		else
		    DISPLAY_STRING = ip + ":" + URL.getPort();
        _count = 1;
        _demoted = false;
	}
	
	public RemoteFileDesc createRemoteFileDesc(int size) {
		Set urnSet = new HashSet();
		urnSet.add(getSHA1Urn());
        int quality = 3;
		RemoteFileDesc ret = new RemoteFileDesc(URL.getHost(), URL.getPort(),
								  0, URL.getFile(), size,  
								  DataUtils.EMPTY_GUID, 1000,
								  true, quality, false, null, urnSet, false,
                                  false, //assume altLoc is not firewalled
                                  ALT_VENDOR,//Never displayed, and we don't know
                                  System.currentTimeMillis(), null, -1);
		
		return ret;
	}
	
	public synchronized AlternateLocation createClone() {
        AlternateLocation ret = null;
        try {
        		ret = new DirectAltLoc(this.URL, this.SHA1_URN);

        } catch(IOException ioe) {
            ErrorService.error(ioe);
            return null;
        }
        ret._demoted = this._demoted;
        ret._count = this._count;
        return ret;
    }
	
	/**
	 * Returns the host/port of this alternate location as an endpoint.
	 */
	public Endpoint getHost() {
	    return new Endpoint(this.URL.getHost(), this.URL.getPort());
	}

	
	public boolean equals(Object o){
		if (o==null || !(o instanceof DirectAltLoc))
			return false;
		
		
		if (!super.equals(o))
			return false;
		
		DirectAltLoc other = (DirectAltLoc)o;
		

		return (URL.getHost().equals(other.URL.getHost()) &&
                URL.getPort() == other.URL.getPort() &&
                SHA1_URN.equals(other.SHA1_URN) &&
                URL.getProtocol().equals(other.URL.getProtocol()) );
		
	}

	public int compareTo(Object o) {
		int ret = super.compareTo(o);
		
		if (ret!=0)
			return ret;
		
		if (o instanceof DirectAltLoc) {
		
			DirectAltLoc other = (DirectAltLoc)o;
		
			ret = this.URL.getHost().compareTo(other.URL.getHost());
			if(ret!=0)
				return ret;
			ret = (this.URL.getPort() - other.URL.getPort());
			if(ret!=0)
				return ret;
		}
		 
        return hashCode() - o.hashCode();
	}
	
	public int hashCode() {
		if (hashCode ==0) {
		int result = super.hashCode();
			result = (37* result)+this.URL.getHost().hashCode();
			result = (37* result)+this.URL.getPort();
			result = (37* result)+this.URL.getProtocol().hashCode();
			hashCode=result;
		}
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
