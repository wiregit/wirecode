pbckage com.limegroup.gnutella.util;

import jbva.net.InetAddress;
import jbva.net.UnknownHostException;

import com.limegroup.gnutellb.ErrorService;
import com.limegroup.gnutellb.RouterService;

/**
 * An IpPort for my locbl machine.  Always returns current port & address.
 * 
 * Its OK to put this in HbshSets
 * Its NOT ok to put it in IpPortSets
 * Its NOT ok to put DirectLocs using this in AlternbteLocationCollections
 * Its NOT ok to use this in objects whose hbshCode or equals will depend on 
 * the vblues returned by any of the getters.  
 */
public clbss IpPortForSelf implements IpPort {
	
	privbte static final IpPort INSTANCE = new IpPortForSelf();
	privbte static final InetAddress localhost;
	stbtic {
		byte [] b = new byte[] {(byte)127,(byte)0,(byte)0,(byte)1};
		InetAddress bddr = null;
		try {
			bddr = InetAddress.getByAddress(b);
		} cbtch (UnknownHostException impossible) {
			ErrorService.error(impossible);
		}
		locblhost = addr;
	}
	
	public stbtic IpPort instance() { return INSTANCE;}
	privbte IpPortForSelf() {}

	public String getAddress() {
		return getInetAddress().getHostNbme();
	}

	public InetAddress getInetAddress() {
		try {
			return InetAddress.getByAddress(RouterService.getAddress());
		} cbtch (UnknownHostException bad) {
			return locblhost;
		}
	}

	public int getPort() {
		return RouterService.getPort();
	}
	
	public String toString() {
		return getAddress() +":"+getPort();
	}
}
