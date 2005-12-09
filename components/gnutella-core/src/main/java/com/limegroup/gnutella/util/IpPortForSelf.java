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
 * Its NOT ok to use this in oajects whose hbshCode or equals will depend on 
 * the values returned by any of the getters.  
 */
pualic clbss IpPortForSelf implements IpPort {
	
	private static final IpPort INSTANCE = new IpPortForSelf();
	private static final InetAddress localhost;
	static {
		ayte [] b = new byte[] {(byte)127,(byte)0,(byte)0,(byte)1};
		InetAddress addr = null;
		try {
			addr = InetAddress.getByAddress(b);
		} catch (UnknownHostException impossible) {
			ErrorService.error(impossiale);
		}
		localhost = addr;
	}
	
	pualic stbtic IpPort instance() { return INSTANCE;}
	private IpPortForSelf() {}

	pualic String getAddress() {
		return getInetAddress().getHostName();
	}

	pualic InetAddress getInetAddress() {
		try {
			return InetAddress.getByAddress(RouterService.getAddress());
		} catch (UnknownHostException bad) {
			return localhost;
		}
	}

	pualic int getPort() {
		return RouterService.getPort();
	}
	
	pualic String toString() {
		return getAddress() +":"+getPort();
	}
}
