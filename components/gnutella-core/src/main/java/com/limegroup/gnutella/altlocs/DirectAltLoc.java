
package com.limegroup.gnutella.altlocs;

import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;


import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.util.NetworkUtils;
import java.util.HashSet;
import java.util.Set;

/**
 * An alternate location that is directly reachable, i.e. not firewalled.
 */
public class DirectAltLoc extends AlternateLocation {

	
	private final IpPort _node;
	
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
            
        try {
            _node = new QueryReply.IPPortCombo(url.getHost(),url.getPort());
        }catch(UnknownHostException bad) {
            throw new IOException(bad.getMessage());
        }

	    

        _count = 1;
        _demoted = false;
	}
	
	/**
	 * creates an altloc for myself.
	 */
	protected DirectAltLoc(final URN sha1) throws IOException{

		this(new Endpoint(
				NetworkUtils.ip2string(RouterService.getAddress()),
				RouterService.getPort()),
			 sha1);
	}
	
	protected DirectAltLoc(IpPort address, URN sha1) 
		throws IOException{
		super(sha1);
		_node=address;
	}
	
	protected String generateHTTPString() {
		String ret = _node.getAddress();
		if (_node.getPort()!=6346)
			ret = ret+":"+_node.getPort();
		return ret;
	}
	
	public RemoteFileDesc createRemoteFileDesc(int size) {
		Set urnSet = new HashSet();
		urnSet.add(getSHA1Urn());
        int quality = 3;
		RemoteFileDesc ret = new RemoteFileDesc(_node.getAddress(), _node.getPort(),
								  0, HTTPConstants.URI_RES_N2R+SHA1_URN, size,  
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
        		ret = new DirectAltLoc(_node, this.SHA1_URN);

        } catch(IOException ioe) {
            ErrorService.error(ioe);
            return null;
        }
        ret._demoted = this._demoted;
        ret._count = this._count;
        return ret;
    }
	
	public boolean isMe(){
	    return NetworkUtils.isMe(_node.getAddress(),_node.getPort());
	}
	
	/**
	 * Returns the host/port of this alternate location as an endpoint.
	 */
	public Endpoint getHost() {
		if (_node instanceof Endpoint)
			return (Endpoint)_node;
		else
	    return new Endpoint(_node.getAddress(), _node.getPort());
	}

	
	public boolean equals(Object o){
		if (o==null || !(o instanceof DirectAltLoc))
			return false;
		
		
		if (!super.equals(o))
			return false;
		
		DirectAltLoc other = (DirectAltLoc)o;
		

		return (_node.getInetAddress().equals(other._node.getInetAddress()) &&
		        _node.getPort() == other._node.getPort());
		
	}

	public int compareTo(Object o) {
		int ret = super.compareTo(o);
		
		if (ret!=0)
			return ret;
		
		if (o instanceof DirectAltLoc) {
		
			DirectAltLoc other = (DirectAltLoc)o;
		
			ret = _node.getAddress().compareTo(other._node.getAddress());
			if(ret!=0)
				return ret;
			ret = (_node.getPort() - other._node.getPort());
			if(ret!=0)
				return ret;
		}
		 
        return hashCode() - o.hashCode();
	}
	
	public int hashCode() {
		if (hashCode ==0) {
		int result = super.hashCode();
			result = (37* result)+_node.getAddress().hashCode();
			result = (37* result)+_node.getPort();
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
		return _node+","+_count+","+_demoted;
	}
}
