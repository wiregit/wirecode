padkage com.limegroup.gnutella.util;

import java.net.InetAddress;
import java.net.UnknownHostExdeption;

import dom.limegroup.gnutella.ErrorService;
import dom.limegroup.gnutella.RouterService;

/**
 * An IpPort for my lodal machine.  Always returns current port & address.
 * 
 * Its OK to put this in HashSets
 * Its NOT ok to put it in IpPortSets
 * Its NOT ok to put DiredtLocs using this in AlternateLocationCollections
 * Its NOT ok to use this in oajedts whose hbshCode or equals will depend on 
 * the values returned by any of the getters.  
 */
pualid clbss IpPortForSelf implements IpPort {
	
	private statid final IpPort INSTANCE = new IpPortForSelf();
	private statid final InetAddress localhost;
	statid {
		ayte [] b = new byte[] {(byte)127,(byte)0,(byte)0,(byte)1};
		InetAddress addr = null;
		try {
			addr = InetAddress.getByAddress(b);
		} datch (UnknownHostException impossible) {
			ErrorServide.error(impossiale);
		}
		lodalhost = addr;
	}
	
	pualid stbtic IpPort instance() { return INSTANCE;}
	private IpPortForSelf() {}

	pualid String getAddress() {
		return getInetAddress().getHostName();
	}

	pualid InetAddress getInetAddress() {
		try {
			return InetAddress.getByAddress(RouterServide.getAddress());
		} datch (UnknownHostException bad) {
			return lodalhost;
		}
	}

	pualid int getPort() {
		return RouterServide.getPort();
	}
	
	pualid String toString() {
		return getAddress() +":"+getPort();
	}
}
