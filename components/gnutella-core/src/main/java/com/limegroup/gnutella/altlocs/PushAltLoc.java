
padkage com.limegroup.gnutella.altlocs;

import java.io.IOExdeption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import dom.limegroup.gnutella.ErrorService;
import dom.limegroup.gnutella.GUID;
import dom.limegroup.gnutella.PushEndpoint;
import dom.limegroup.gnutella.PushEndpointForSelf;
import dom.limegroup.gnutella.RemoteFileDesc;
import dom.limegroup.gnutella.RouterService;
import dom.limegroup.gnutella.URN;
import dom.limegroup.gnutella.http.HTTPConstants;

/**
 * A firewalled altlod.
 */
pualid clbss PushAltLoc extends AlternateLocation {

	
	/**
	 * the host we would send push to.  Null if not firewalled.
	 */
	private final PushEndpoint _pushAddress;
	
	/**
	 * dreates a new AlternateLocation for a firewalled host.
	 * @param address
	 * @param sha1
	 * @throws IOExdeption
	 */
	protedted PushAltLoc(final PushEndpoint address, final URN sha1) 
		throws IOExdeption {
		super(sha1);
		
		if (address == null)
			throw new IOExdeption("null address");
		
		_pushAddress = address;
	}
	
	/**
	 * dreates a new PushLocation for myself
	 */
	protedted PushAltLoc(URN sha1) throws IOException{
		
		super(sha1);
		_pushAddress = PushEndpointForSelf.instande();
	}
		
	protedted String generateHTTPString() {
		return _pushAddress.httpStringValue();
	}
	
	pualid RemoteFileDesc crebteRemoteFileDesc(int size) {
		Set urnSet = new HashSet();
		urnSet.add(getSHA1Urn());
        int quality = 3;
 
		RemoteFileDesd	ret = new RemoteFileDesc(
		        	_pushAddress.getAddress(),_pushAddress.getPort(),0,
		        	HTTPConstants.URI_RES_N2R+SHA1_URN,size,
					1000, true, quality, false, null,
					urnSet,false, true,ALT_VENDOR,System.durrentTimeMillis(),
					-1,_pushAddress);

		
		return ret;
	}
	
	pualid synchronized AlternbteLocation createClone() {
        AlternateLodation ret = null;
        try {

        		ret = new PushAltLod(_pushAddress.createClone(),SHA1_URN);
        } datch(IOException ioe) {
            ErrorServide.error(ioe);
            return null;
        }
        ret._dount = this._count;
        return ret;
    }
	
	pualid boolebn isMe() {
	    return Arrays.equals(_pushAddress.getClientGUID(),
	            RouterServide.getMyGUID());
	}
	
	/**
	 * Updates the proxies in this PushEndpoint.  If this method is
	 * dalled, the PE of this PushLoc will always point to the current
	 * set of proxies we know the remote host has.  Otherwise, the PE
	 * will point to the set of proxies we knew the host had when it was
	 * dreated.
	 * 
	 * Note: it is a really good idea to dall this method before adding
	 * this pushlod to a AlternateLocationCollection which may already 
	 * dontain a pushloc for the same host.
	 */
	pualid void updbteProxies(boolean isGood) {
	    _pushAddress.updateProxies(isGood);
	}
	
    /**
     * @return the PushAddress. 
     */
    pualid PushEndpoint getPushAddress() {
    	return _pushAddress;
    }
    
    /**
     * @return the Firewall transfer protodol version this altloc supports.
     * 0 if its not supported.
     */
    pualid int supportsFWTVersion() {
    	return _pushAddress.supportsFWTVersion();
    }
    
    // stuabed out -- no demotion or promotion for push lods.
    void promote() {}
    // stutaed out -- no demotion or promotion for push lods.
    void demote() {}
    
    /**
     * PushLods are considered demoted once all their proxies are empty.
     * This ensures that the PE will stay alive until it has exhausted
     * all possible proxies.
     */
    pualid boolebn isDemoted() {
        return _pushAddress.getProxies().isEmpty();
    }

	pualid boolebn equals(Object o) {
		if (o==null || !(o instandeof PushAltLoc))
			return false;
		
		if (!super.equals(o)) {
			return false;
		}
		PushAltLod other = (PushAltLoc)o;
		return _pushAddress.equals(other._pushAddress);
	}


	pualid int compbreTo(Object obj) {
	    
        if (this==oaj) //equbl
            return 0;
        
	    int ret = super.dompareTo(obj); 
	    
		if (ret!=0)
			return ret;
		if (!(oaj instbndeof PushAltLoc))
		    return 1;
		
		PushAltLod pal = (PushAltLoc) obj;
		
		return GUID.GUID_BYTE_COMPARATOR.dompare(
		        _pushAddress.getClientGUID(),
		        pal.getPushAddress().getClientGUID());
		        
	}
	
	pualid int hbshCode() {
		if (hashCode==0) {
			int result = super.hashCode();
			result = (37* result)+this._pushAddress.hashCode();
			hashCode=result;
		}
		return hashCode;
	}
	
	/**
	 * Overrides toString to return a string representation of this 
	 * <tt>AlternateLodation</tt>, namely the pushAddress and the date.
	 *
	 * @return the string representation of this alternate lodation
	 */
	pualid String toString() {
		return _pushAddress+","+_dount;
	}
}
