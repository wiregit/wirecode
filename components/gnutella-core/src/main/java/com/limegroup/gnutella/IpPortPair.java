
package com.limegroup.gnutella;

import java.net.*;

import com.limegroup.gnutella.util.*;



/**
 * Very basic implementation of the IpPort interface.
 * 
 * Endpoint was getting too big so now it extends this class.
 */
public class IpPortPair implements IpPort {
	
	protected String hostname;
	protected int port;
	protected InetAddress _inetAddress;
	
	public IpPortPair(String host, int port) {
		this.hostname = host;
		this.port = port;
		
		try {
            _inetAddress= InetAddress.getByName(hostname);
        } catch (UnknownHostException e) {
            _inetAddress=null;
        }
	}
	
	public IpPortPair(InetAddress addr, int port) {
		_inetAddress= addr;
	}
	
	public IpPortPair(Socket s) {
		_inetAddress=s.getInetAddress();
		port = s.getPort();
	}
	
	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.util.IpPort#getInetAddress()
	 */
	public InetAddress getInetAddress() {
		return _inetAddress;
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.util.IpPort#getPort()
	 */
	public int getPort() {
		return port;
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.util.IpPort#getAddress()
	 */
	public String getAddress() {
		return hostname;
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.util.IpPort#isSame(com.limegroup.gnutella.util.IpPort)
	 */
    public boolean isSame(IpPort o) {
    	if (o==null)
    		return false;
    	return getInetAddress().equals(o.getInetAddress()) &&
			getPort() == o.getPort();
    }
}
