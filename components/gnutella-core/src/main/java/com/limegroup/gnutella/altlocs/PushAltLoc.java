
package com.limegroup.gnutella.altlocs;

import java.io.IOException;
import java.net.URL;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.util.DataUtils;
import com.sun.java.util.collections.HashSet;
import com.sun.java.util.collections.Set;

/**
 * A firewalled altloc.
 */
public class PushAltLoc extends AlternateLocation {

	/**
	 * in case we do not have af ull url store the name here.
	 */
	private String _fileName;
	
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
	protected PushAltLoc(final PushEndpoint address, final URN sha1, final String name) 
		throws IOException {
		super(sha1);
		
		if (address == null)
			throw new IOException("null address");
		if (address.getProxies().isEmpty())
			throw new IOException("no proxies for altloc");
		
		
		_pushAddress = address;
		DISPLAY_STRING= _pushAddress.httpStringValue(); 
		_fileName = name;
	}
	
	public RemoteFileDesc createRemoteFileDesc(int size) {
		Set urnSet = new HashSet();
		urnSet.add(getSHA1Urn());
        int quality = 3;
 
		

		RemoteFileDesc	ret = new RemoteFileDesc("1.1.1.1",6346,0,_fileName,size,
					_pushAddress.getClientGUID(), 1000, true, quality, false, null,
					urnSet,false, true,ALT_VENDOR,System.currentTimeMillis(),
					_pushAddress.getProxies(),-1);

		
		return ret;
	}
	
	public synchronized AlternateLocation createClone() {
        AlternateLocation ret = null;
        try {

        		ret = new PushAltLoc(_pushAddress,SHA1_URN,_fileName);
        } catch(IOException ioe) {
            ErrorService.error(ioe);
            return null;
        }
        ret._demoted = this._demoted;
        ret._count = this._count;
        return ret;
    }
	
	
    /**
     * @return the PushAddress. 
     */
    public PushEndpoint getPushAddress() {
    	return _pushAddress;
    }
	
	public boolean equals(Object o) {
		if (o==null || !(o instanceof PushAltLoc))
			return false;
		
		if (!super.equals(o))
			return false;
		
		PushAltLoc other = (PushAltLoc)o;
		
		return _fileName.equals(other._fileName) &&
			_pushAddress.equals(other._pushAddress);
	}


	public int compareTo(Object o) {
		
		int ret= super.compareTo(o);
		
		if (ret!=0)
			return ret;
		
		if (o instanceof PushAltLoc) {
			PushAltLoc other = (PushAltLoc)o;
			
			
	        ret = other._pushAddress.getProxies().size() - 
				_pushAddress.getProxies().size();
	        if (ret!=0)
	        	return ret;
	        
		}
		
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
