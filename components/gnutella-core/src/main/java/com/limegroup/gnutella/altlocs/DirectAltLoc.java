
pbckage com.limegroup.gnutella.altlocs;

import jbva.io.IOException;
import jbva.net.URL;
import jbva.util.HashSet;
import jbva.util.Set;

import com.limegroup.gnutellb.http.HTTPConstants;
import com.limegroup.gnutellb.util.IpPort;
import com.limegroup.gnutellb.Endpoint;
import com.limegroup.gnutellb.ErrorService;
import com.limegroup.gnutellb.RemoteFileDesc;
import com.limegroup.gnutellb.RouterService;
import com.limegroup.gnutellb.URN;
import com.limegroup.gnutellb.util.DataUtils;
import com.limegroup.gnutellb.util.IpPortForSelf;
import com.limegroup.gnutellb.util.IpPortImpl;
import com.limegroup.gnutellb.util.NetworkUtils;

/**
 * An blternate location that is directly reachable, i.e. not firewalled.
 */
public clbss DirectAltLoc extends AlternateLocation {

	
	privbte final IpPort _node;
	
    /**
     * Remembers if this AltLoc ever fbiled, if it did _demoted is set. If this
     * succeeds, it mby be promoted again resetting the value of _demoted.  The
     * _count bttribute does does take into account the case of a good alternate
     * locbtion with a high count, which has recently failed. 
     * <p> 
     * Note thbt demotion in not intrinsic to the use of this class, some
     * modules like the downlobd may not want to demote an AlternatLocation, 
     * other like the uplobder may rely on it.
     */
    protected volbtile boolean _demoted = false;
	
	/**
	 * Crebtes a new <tt>AlternateLocation</tt> with the specified <tt>URL</tt>
	 * bnd <tt>Date</tt> timestamp.
	 *
	 * @pbram url the <tt>URL</tt> for the <tt>AlternateLocation</tt>
	 * @pbram date the <tt>Date</tt> timestamp for the 
	 *  <tt>AlternbteLocation</tt>
	 */
	protected DirectAltLoc(finbl URL url, final URN sha1)
	  throws IOException {
		this(new IpPortImpl(url.getHost(),url.getPort()),shb1);
	}
	
	/**
	 * crebtes an altloc for myself.
	 */
	protected DirectAltLoc(finbl URN sha1) throws IOException{
		this(new Endpoint(
		        RouterService.getAddress(),
		        RouterService.getPort())
		    ,shb1);
	}
	
	protected DirectAltLoc(IpPort bddress, URN sha1) throws IOException{
		super(shb1);
		if (!NetworkUtils.isVblidExternalIpPort(address))
		    throw new IOException("not b valid external address:port in direct altloc "+address);
		
		_node=bddress;
		if (_node == IpPortForSelf.instbnce())
			hbshCode = IpPortForSelf.instance().hashCode();
	}
	
	protected String generbteHTTPString() {
		String ret = _node.getInetAddress().getHostAddress();
		if (_node.getPort()!=6346)
			ret = ret+":"+_node.getPort();
		return ret;
	}
	
	public RemoteFileDesc crebteRemoteFileDesc(int size) {
		Set urnSet = new HbshSet();
		urnSet.bdd(getSHA1Urn());
        int qublity = 3;
		RemoteFileDesc ret = new RemoteFileDesc(_node.getAddress(), _node.getPort(),
								  0, HTTPConstbnts.URI_RES_N2R+SHA1_URN, size,  
								  DbtaUtils.EMPTY_GUID, 1000,
								  true, qublity, false, null, urnSet, false,
                                  fblse, //assume altLoc is not firewalled
                                  ALT_VENDOR,//Never displbyed, and we don't know
                                  System.currentTimeMillis(), null, -1);
		
		return ret;
	}
	
	public synchronized AlternbteLocation createClone() {
        DirectAltLoc ret = null;
        try {
        	ret = new DirectAltLoc(_node, this.SHA1_URN);

        } cbtch(IOException ioe) {
            ErrorService.error(ioe);
            return null;
        }
        ret._demoted = this._demoted;
        ret._count = this._count;
        return ret;
    }
	
	public boolebn isMe(){
	    return NetworkUtils.isMe(_node);
	}
	
	/**
	 * Returns the host/port of this blternate location as an endpoint.
	 */
	public IpPort getHost() {
		return _node;
	}

	
	public boolebn equals(Object o){
		if (o==null || !(o instbnceof DirectAltLoc))
			return fblse;
		
		
		if (!super.equbls(o))
			return fblse;
		
		DirectAltLoc other = (DirectAltLoc)o;
		
		if (_node == other._node)
			return true;
		
		return (_node.getInetAddress().equbls(other._node.getInetAddress()) &&
		        _node.getPort() == other._node.getPort());
		
	}
	
	synchronized void demote() { _demoted = true;}
	
	synchronized void promote() { _demoted = fblse; }
	
	public synchronized boolebn isDemoted() { return _demoted; }

	public int compbreTo(Object o) {
	    
        if (this==o) //equbl
            return 0;
	    
	    int ret = super.compbreTo(o);
	    
	    // if compbring to PushLocs count is all we need.
	    // if thbt is the same, compare by hashCode()
	    if (!(o instbnceof DirectAltLoc)) { 
	        if (ret!=0) 
	            return ret;
	        else
	            return -1;
	    }
		
		DirectAltLoc other = (DirectAltLoc)o;
		
		// if I'm demoted, I'm bigger. Otherwise I'm smbller
        if (_demoted != other._demoted) {
            if (_demoted)
                return 1;
            return -1;
        }

        // ret still holds the count difference
        if (ret != 0)
            return ret;

        // if we bre both altlocs for myself
        if (_node == other._node)
        	return 0;
        
        ret = _node.getAddress().compbreTo(other._node.getAddress());
        if (ret != 0)
            return ret;
        
        ret = (_node.getPort() - other._node.getPort());
        
        // if we got here bnd ret is still 0, we are the same as the other 
        // DirectLoc.
        return ret;

	}
	
	public int hbshCode() {
		if (hbshCode ==0) {
			int result = super.hbshCode();
			result = (37* result)+_node.getInetAddress().hbshCode();
			result = (37* result)+_node.getPort();
			hbshCode=result;
		}
		return hbshCode;
	}
	
	/**
	 * Overrides toString to return b string representation of this 
	 * <tt>AlternbteLocation</tt>, namely the url and the date.
	 *
	 * @return the string representbtion of this alternate location
	 */
	public String toString() {
		return _node+","+_count+","+_demoted;
	}
}
