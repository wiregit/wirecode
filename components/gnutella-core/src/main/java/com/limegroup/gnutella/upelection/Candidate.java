/*
 * an ExtendedEndpoint which keeps a ref to the Ultrapeer Connection which advertised it.
 * Decorator pattern to allow parsing.  When comparing, make sure you use the Comparator provided
 * by ExtendedEndpoint!
 */
package com.limegroup.gnutella.upelection;

import java.io.IOException;
import java.io.Writer;
import java.net.InetAddress;
import java.text.ParseException;

import com.limegroup.gnutella.*;
import com.sun.java.util.collections.Iterator;


public class Candidate implements Comparable{
	
	/**
	 * the ultrapeer connection that told us about this host
	 */
	private ReplyHandler _advertiser;
	
	/**
	 * the node that is the best candidate
	 */
	
	private ExtendedEndpoint _endpoint;
	
	
	public Candidate(String line) throws ParseException {
		_endpoint = ExtendedEndpoint.read(line);
	}
	
	/**
	 * @return Returns the advertiser.
	 */
	public ReplyHandler getAdvertiser() {
		return _advertiser;
	}
	
	public void setAdvertiser(ReplyHandler advertiser) {
		_advertiser = advertiser;
	}
	
	
	
	/**
	 * @param o
	 * @return
	 */
	public int compareTo(Object o) {
		return _endpoint.compareTo(o);
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		return _endpoint.equals(obj);
	}
	/**
	 * @return
	 */
	public String getAddress() {
		return _endpoint.getAddress();
	}
	/**
	 * @return
	 */
	public Iterator getConnectionFailures() {
		return _endpoint.getConnectionFailures();
	}
	/**
	 * @return
	 */
	public Iterator getConnectionSuccesses() {
		return _endpoint.getConnectionSuccesses();
	}
	/**
	 * @return
	 */
	public int getConnectivity() {
		return _endpoint.getConnectivity();
	}
	/**
	 * @return
	 */
	public int getDailyUptime() {
		return _endpoint.getDailyUptime();
	}
	/**
	 * @return
	 */
	public InetAddress getInetAddress() {
		return _endpoint.getInetAddress();
	}
	/**
	 * @return
	 */
	public int getPort() {
		return _endpoint.getPort();
	}
	/**
	 * @return
	 */
	public long getTimeRecorded() {
		return _endpoint.getTimeRecorded();
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return _endpoint.hashCode();
	}
	/**
	 * @return
	 */
	public boolean isPrivateAddress() {
		return _endpoint.isPrivateAddress();
	}
	/**
	 * @param other
	 * @return
	 */
	public boolean isSameSubnet(Endpoint other) {
		return _endpoint.isSameSubnet(other);
	}
	/**
	 * @param out
	 * @throws IOException
	 */
	public void write(Writer out) throws IOException {
		_endpoint.write(out);
	}
}