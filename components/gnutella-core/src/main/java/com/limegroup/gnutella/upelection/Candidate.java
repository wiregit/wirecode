/*
 * Created on Apr 16, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.limegroup.gnutella.upelection;


import java.net.InetAddress;
import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.util.IpPort;

/**
 * A class representing a candidate for election.
 */
public interface Candidate extends IpPort{
	
	/**
	 * @return the byte[] representation of this candidate to be
	 * transmitted over the network.
	 */
	public abstract byte[] toBytes();
	
	/**
	 * @return Returns the ReplyHandler which should be used when
	 * forwarding the promotion request.
	 */
	public abstract Connection getAdvertiser();
	/**
	 * @param _advertiser The ReplyHandler which should be used
	 * when forwarding the promotion request
	 */
	public abstract void setAdvertiser(Connection _advertiser);
	/**
	 * @return the InetAddress of the candidate
	 */
	public abstract InetAddress getInetAddress();
	/**
	 * @return the port of the candidate
	 */
	public abstract int getPort();
	
	/**
	 * @return whether the route to the candidate is open.
	 */
	public boolean isOpen();
	
	/**
	 * @return the uptime of this candidate, in minutes.
	 */
	public short getUptime();
}