
package com.limegroup.gnutella.altlocs;

import java.io.IOException;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.PushEndpointForSelf;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.http.HTTPConstants;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A firewalled altloc.
 */
public class PushAltLoc extends AlternateLocation {

	
	/**
	 * the host we would send push to.  Null if not firewalled.
	 */
	private final PushEndpoint _pushAddress;
	
	/**
	 * creates a new AlternateLocation for a firewalled host.
	 * @param address
	 * @param sha1
	 * @throws IOException
	 */
	protected PushAltLoc(final PushEndpoint address, final URN sha1) 
		throws IOException {
		super(sha1);
		
		if (address == null)
			throw new IOException("null address");
		
		_pushAddress = address;
	}
	
	/**
	 * creates a new PushLocation for myself
	 */
	protected PushAltLoc(URN sha1) throws IOException{
		
		super(sha1);
		_pushAddress = PushEndpointForSelf.instance();
	}
		
	protected String generateHTTPString() {
		return _pushAddress.httpStringValue();
	}
	
	public RemoteFileDesc createRemoteFileDesc(int size) {
		Set urnSet = new HashSet();
		urnSet.add(getSHA1Urn());
        int quality = 3;
 
		
        //invalid ip address - not important.
		RemoteFileDesc	ret = new RemoteFileDesc(
		        	"1.1.1.1",6346,0,HTTPConstants.URI_RES_N2R+SHA1_URN,size,
					1000, true, quality, false, null,
					urnSet,false, true,ALT_VENDOR,System.currentTimeMillis(),
					-1,_pushAddress);

		
		return ret;
	}
	
	public synchronized AlternateLocation createClone() {
        AlternateLocation ret = null;
        try {

        		ret = new PushAltLoc(_pushAddress,SHA1_URN);
        } catch(IOException ioe) {
            ErrorService.error(ioe);
            return null;
        }
        ret._demoted = this._demoted;
        ret._count = this._count;
        return ret;
    }
	
	public boolean isMe() {
	    return Arrays.equals(_pushAddress.getClientGUID(),
	            RouterService.getMyGUID());
	}
	
	/**
	 * Updates the proxies in this PushEndpoint.
	 */
	public void updateProxies(boolean isGood) {
	    _pushAddress.updateProxies(isGood);
	}
	
    /**
     * @return the PushAddress. 
     */
    public PushEndpoint getPushAddress() {
    	return _pushAddress;
    }
    
    /**
     * @return the Firewall transfer protocol version this altloc supports.
     * 0 if its not supported.
     */
    public int supportsFWTVersion() {
    	return _pushAddress.supportsFWTVersion();
    }
    
    // stubbed out -- no demotion or promotion for push locs.
    void promote() {}
    // stutbed out -- no demotion or promotion for push locs.
    void demote() {}
    
    /**
     * PushLocs are considered demoted once all their proxies are empty.
     * This ensures that the PE will stay alive until it has exhausted
     * all possible proxies.
     */
    public boolean isDemoted() {
        return _pushAddress.getProxies().isEmpty();
    }

	public boolean equals(Object o) {
		if (o==null || !(o instanceof PushAltLoc))
			return false;
		
		if (!super.equals(o)) {
			return false;
		}
		PushAltLoc other = (PushAltLoc)o;
		return _pushAddress.equals(other._pushAddress);
	}


	public int compareTo(Object o) {
		
		int ret= super.compareTo(o);
		
		if (ret!=0)
			return ret;

        // this doesn't work, because the size of proxies
        // can change while it is stored inside the TreeMap
        // nothing that is in a SortedSet can be mutated
        // if it is used in the compareTo.
        
//		if (o instanceof PushAltLoc) {
//			PushAltLoc other = (PushAltLoc)o;
//			
//			
//	        ret = other._pushAddress.getProxies().size() - 
//				_pushAddress.getProxies().size();
//	        if (ret!=0)
//	        	return ret;
//	        
//		}
		
		return hashCode() - o.hashCode();
	}
	
	public int hashCode() {
		if (hashCode==0) {
			int result = super.hashCode();
			result = (37* result)+this._pushAddress.hashCode();
			hashCode=result;
		}
		return hashCode;
	}
	
	/**
	 * Overrides toString to return a string representation of this 
	 * <tt>AlternateLocation</tt>, namely the pushAddress and the date.
	 *
	 * @return the string representation of this alternate location
	 */
	public String toString() {
		return _pushAddress+","+_count+","+_demoted;
	}
}
