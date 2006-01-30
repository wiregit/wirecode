package com.limegroup.gnutella.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.RouterService;

/**
 * An IpPort for my local machine.  Always returns current port & address.
 * 
 * Its OK to put this in HashSets
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

	public String getAddress() {
		return getInetAddress().getHostName();
	}

	public InetAddress getInetAddress() {
		try {
			return InetAddress.getByAddress(RouterService.getAddress());
		} catch (UnknownHostException bad) {
			return localhost;
		}
	}

	public int getPort() {
		return RouterService.getPort();
	}
	
	public String toString() {
		return getAddress() +":"+getPort();
	}
}
