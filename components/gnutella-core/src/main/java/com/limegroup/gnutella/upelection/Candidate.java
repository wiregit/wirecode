/*
 * An interface representing a candidate for an ultrapeer.
 * 
 * Candidates are IpPorts that have uptime and an advertiser.
 */
package com.limegroup.gnutella.upelection;


import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.util.IpPort;

/**
 * An interface representing a candidate for election.
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
	public abstract void setAdvertiser(Connection advertiser);
	
	/**
	 * @return whether the route to the candidate is open.
	 */
	public boolean isOpen();
	
	/**
	 * @return the uptime of this candidate, in minutes.
	 */
	public short getUptime();
}