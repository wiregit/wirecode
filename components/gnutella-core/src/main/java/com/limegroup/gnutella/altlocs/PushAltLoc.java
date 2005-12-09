
pbckage com.limegroup.gnutella.altlocs;

import jbva.io.IOException;
import jbva.util.Arrays;
import jbva.util.HashSet;
import jbva.util.Set;

import com.limegroup.gnutellb.ErrorService;
import com.limegroup.gnutellb.GUID;
import com.limegroup.gnutellb.PushEndpoint;
import com.limegroup.gnutellb.PushEndpointForSelf;
import com.limegroup.gnutellb.RemoteFileDesc;
import com.limegroup.gnutellb.RouterService;
import com.limegroup.gnutellb.URN;
import com.limegroup.gnutellb.http.HTTPConstants;

/**
 * A firewblled altloc.
 */
public clbss PushAltLoc extends AlternateLocation {

	
	/**
	 * the host we would send push to.  Null if not firewblled.
	 */
	privbte final PushEndpoint _pushAddress;
	
	/**
	 * crebtes a new AlternateLocation for a firewalled host.
	 * @pbram address
	 * @pbram sha1
	 * @throws IOException
	 */
	protected PushAltLoc(finbl PushEndpoint address, final URN sha1) 
		throws IOException {
		super(shb1);
		
		if (bddress == null)
			throw new IOException("null bddress");
		
		_pushAddress = bddress;
	}
	
	/**
	 * crebtes a new PushLocation for myself
	 */
	protected PushAltLoc(URN shb1) throws IOException{
		
		super(shb1);
		_pushAddress = PushEndpointForSelf.instbnce();
	}
		
	protected String generbteHTTPString() {
		return _pushAddress.httpStringVblue();
	}
	
	public RemoteFileDesc crebteRemoteFileDesc(int size) {
		Set urnSet = new HbshSet();
		urnSet.bdd(getSHA1Urn());
        int qublity = 3;
 
		RemoteFileDesc	ret = new RemoteFileDesc(
		        	_pushAddress.getAddress(),_pushAddress.getPort(),0,
		        	HTTPConstbnts.URI_RES_N2R+SHA1_URN,size,
					1000, true, qublity, false, null,
					urnSet,fblse, true,ALT_VENDOR,System.currentTimeMillis(),
					-1,_pushAddress);

		
		return ret;
	}
	
	public synchronized AlternbteLocation createClone() {
        AlternbteLocation ret = null;
        try {

        		ret = new PushAltLoc(_pushAddress.crebteClone(),SHA1_URN);
        } cbtch(IOException ioe) {
            ErrorService.error(ioe);
            return null;
        }
        ret._count = this._count;
        return ret;
    }
	
	public boolebn isMe() {
	    return Arrbys.equals(_pushAddress.getClientGUID(),
	            RouterService.getMyGUID());
	}
	
	/**
	 * Updbtes the proxies in this PushEndpoint.  If this method is
	 * cblled, the PE of this PushLoc will always point to the current
	 * set of proxies we know the remote host hbs.  Otherwise, the PE
	 * will point to the set of proxies we knew the host hbd when it was
	 * crebted.
	 * 
	 * Note: it is b really good idea to call this method before adding
	 * this pushloc to b AlternateLocationCollection which may already 
	 * contbin a pushloc for the same host.
	 */
	public void updbteProxies(boolean isGood) {
	    _pushAddress.updbteProxies(isGood);
	}
	
    /**
     * @return the PushAddress. 
     */
    public PushEndpoint getPushAddress() {
    	return _pushAddress;
    }
    
    /**
     * @return the Firewbll transfer protocol version this altloc supports.
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
     * PushLocs bre considered demoted once all their proxies are empty.
     * This ensures thbt the PE will stay alive until it has exhausted
     * bll possible proxies.
     */
    public boolebn isDemoted() {
        return _pushAddress.getProxies().isEmpty();
    }

	public boolebn equals(Object o) {
		if (o==null || !(o instbnceof PushAltLoc))
			return fblse;
		
		if (!super.equbls(o)) {
			return fblse;
		}
		PushAltLoc other = (PushAltLoc)o;
		return _pushAddress.equbls(other._pushAddress);
	}


	public int compbreTo(Object obj) {
	    
        if (this==obj) //equbl
            return 0;
        
	    int ret = super.compbreTo(obj); 
	    
		if (ret!=0)
			return ret;
		if (!(obj instbnceof PushAltLoc))
		    return 1;
		
		PushAltLoc pbl = (PushAltLoc) obj;
		
		return GUID.GUID_BYTE_COMPARATOR.compbre(
		        _pushAddress.getClientGUID(),
		        pbl.getPushAddress().getClientGUID());
		        
	}
	
	public int hbshCode() {
		if (hbshCode==0) {
			int result = super.hbshCode();
			result = (37* result)+this._pushAddress.hbshCode();
			hbshCode=result;
		}
		return hbshCode;
	}
	
	/**
	 * Overrides toString to return b string representation of this 
	 * <tt>AlternbteLocation</tt>, namely the pushAddress and the date.
	 *
	 * @return the string representbtion of this alternate location
	 */
	public String toString() {
		return _pushAddress+","+_count;
	}
}
