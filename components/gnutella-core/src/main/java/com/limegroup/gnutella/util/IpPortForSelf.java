package com.limegroup.gnutella.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.RouterService;

/**
 * An IpPort for my local machine.  Always returns current port & address.
 * 
 * Do NOT put this in IpPortSets as it will probably break the invariants.  
 */
public class IpPortForSelf implements IpPort {
	
	private static final IpPort INSTANCE = new IpPortForSelf();
	public static IpPort instance() { return INSTANCE;}
	private IpPortForSelf() {}

	public String getAddress() {
		return getInetAddress().getHostName();
	}

	public InetAddress getInetAddress() {
		try {
			return InetAddress.getByAddress(RouterService.getAddress());
		} catch (UnknownHostException bad) {
			ErrorService.error(bad);
			return null;
		}
	}

	public int getPort() {
		return RouterService.getPort();
	}
}
