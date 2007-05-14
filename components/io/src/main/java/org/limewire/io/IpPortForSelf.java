package org.limewire.io;



import java.net.InetAddress;
import java.net.UnknownHostException;

import org.limewire.service.ErrorService;


/**
 * Returns the current port and address for the local system. 
 * <p>
 * You can use <code>IpPortForSelf</code> for {@link HashSets}.
 */ 
/* Its OK to put this in HashSets
 * Its NOT ok to put it in IpPortSets
 * Its NOT ok to put DirectLocs using this in AlternateLocationCollections
 * Its NOT ok to use this in objects whose hashCode or equals will depend on 
 * the values returned by any of the getters.  
 */
public class IpPortForSelf implements IpPort {
	
	private static final IpPort INSTANCE = new IpPortForSelf();
	private static final InetAddress localhost;
	static {
		byte [] b = new byte[] {(byte)127,(byte)0,(byte)0,(byte)1};
		InetAddress addr = null;
		try {
			addr = InetAddress.getByAddress(b);
		} catch (UnknownHostException impossible) {
			ErrorService.error(impossible);
		}
		localhost = addr;
	}
	
	public static IpPort instance() { return INSTANCE;}
	private IpPortForSelf() {}
    
    public byte[] getAddressAsBytes() {
        return LocalSocketAddressService.getLocalAddress();
    }

	public String getAddress() {
		return getInetAddress().getHostName();
	}

	public InetAddress getInetAddress() {
		try {
			return InetAddress.getByAddress(LocalSocketAddressService.getLocalAddress());
		} catch (UnknownHostException bad) {
			return localhost;
		}
	}

	public int getPort() {
		return LocalSocketAddressService.getLocalPort();
	}
	
	public String toString() {
		return getAddress() +":"+getPort();
	}
}
