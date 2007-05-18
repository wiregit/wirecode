package org.limewire.io;



import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;

import org.limewire.service.ErrorService;




/**
 * Returns the current port and address for the local system. A <a 
 * href="http://en.wikipedia.org/wiki/Singleton_pattern"> Singleton</a> 
 * pattern, <code>IpPortForSelf</code> keeps a static reference which can be
 * reused. 
 * <p>
 * It's ok to put <code>IpPortForSelf</code> in {@link HashSet HashSets}.<br>
 * It's not ok to put <code>IpPortForSelf</code> in {@link IpPortSet IpPortSets}.<br>
 * It's not ok to put <code>DirectAltLoc</code>s using this in 
 * <code>AlternateLocationCollections</code>.<br>
 * It's not ok to use <code>IpPortForSelf</code> in objects whose hashCode or 
 * equals will depend on the values returned by any of the getters.  
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
