
package com.limegroup.gnutella.altlocs;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.IpPortForSelf;
import com.limegroup.gnutella.util.IpPortImpl;
import com.limegroup.gnutella.util.NetworkUtils;

/**
 * An alternate location that is directly reachable, i.e. not firewalled.
 */
public class DirectAltLoc extends AlternateLocation {

	
	private final IpPort _node;
	
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
    protected volatile boolean _demoted = false;
	
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
		this(new IpPortImpl(url.getHost(),url.getPort()),sha1);
	}
	
	/**
	 * creates an altloc for myself.
	 */
	protected DirectAltLoc(final URN sha1) throws IOException{
		this(new Endpoint(
		        RouterService.getAddress(),
		        RouterService.getPort())
		    ,sha1);
	}
	
	protected DirectAltLoc(IpPort address, URN sha1) throws IOException{
		super(sha1);
		if (!NetworkUtils.isValidExternalIpPort(address))
		    throw new IOException("not a valid external address:port in direct altloc "+address);
		
		_node=address;
		if (_node == IpPortForSelf.instance())
			hashCode = IpPortForSelf.instance().hashCode();
	}
	
	protected String generateHTTPString() {
		String ret = _node.getInetAddress().getHostAddress();
		if (_node.getPort()!=6346)
			ret = ret+":"+_node.getPort();
		return ret;
	}
	
	public RemoteFileDesc createRemoteFileDesc(int size) {
		Set<URN> urnSet = new HashSet<URN>(1);
		urnSet.add(getSHA1Urn());
        int quality = 3;
		RemoteFileDesc ret = new RemoteFileDesc(_node.getAddress(), _node.getPort(),
								  0, HTTPConstants.URI_RES_N2R+SHA1_URN, size,  
								  DataUtils.EMPTY_GUID, 1000,
								  true, quality, false, null, urnSet, false,
                                  false, //assume altLoc is not firewalled
                                  ALT_VENDOR,//Never displayed, and we don't know
                                  null, -1);
		
		return ret;
	}
	
	public synchronized AlternateLocation createClone() {
        DirectAltLoc ret = null;
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
	    return NetworkUtils.isMe(_node);
	}
	
	/**
	 * Returns the host/port of this alternate location as an endpoint.
	 */
	public IpPort getHost() {
		return _node;
	}

	
	public boolean equals(Object o){
		if (o==null || !(o instanceof DirectAltLoc))
			return false;
		
		
		if (!super.equals(o))
			return false;
		
		DirectAltLoc other = (DirectAltLoc)o;
		
		if (_node == other._node)
			return true;
		
		return (_node.getInetAddress().equals(other._node.getInetAddress()) &&
		        _node.getPort() == other._node.getPort());
		
	}
	
	synchronized void demote() { _demoted = true;}
	
	synchronized void promote() { _demoted = false; }
	
	public synchronized boolean isDemoted() { return _demoted; }

	public int compareTo(Object o) {
	    
        if (this==o) //equal
            return 0;
	    
	    int ret = super.compareTo(o);
	    
	    // if comparing to PushLocs count is all we need.
	    // if that is the same, compare by hashCode()
	    if (!(o instanceof DirectAltLoc)) { 
	        if (ret!=0) 
	            return ret;
	        else
	            return -1;
	    }
		
		DirectAltLoc other = (DirectAltLoc)o;
		
		// if I'm demoted, I'm bigger. Otherwise I'm smaller
        if (_demoted != other._demoted) {
            if (_demoted)
                return 1;
            return -1;
        }

        // ret still holds the count difference
        if (ret != 0)
            return ret;

        // if we are both altlocs for myself
        if (_node == other._node)
        	return 0;
        
        ret = _node.getAddress().compareTo(other._node.getAddress());
        if (ret != 0)
            return ret;
        
        ret = (_node.getPort() - other._node.getPort());
        
        // if we got here and ret is still 0, we are the same as the other 
        // DirectLoc.
        return ret;

	}
	
	public int hashCode() {
		if (hashCode ==0) {
			int result = super.hashCode();
			result = (37* result)+_node.getInetAddress().hashCode();
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
