/*
 * an ExtendedEndpoint which keeps a ref to the Ultrapeer Connection which advertised it.
 * Decorator pattern to allow parsing.  When comparing, make sure you use the provided Comparator.
 */
package com.limegroup.gnutella.upelection;

import java.io.IOException;
import java.io.Writer;
import java.net.InetAddress;
import java.text.ParseException;

import com.limegroup.gnutella.*;
import com.sun.java.util.collections.Comparable;
import com.sun.java.util.collections.Iterator;
import com.sun.java.util.collections.Comparator;


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
	
	/**
	 * delegate to ExtendedEndpoint
	 * @return
	 */
	private int connectScore() {
		return _endpoint.connectScore();
	}
	
	
	/**
	 * returns comparator identical to the one for 
	 * ExtendedEndpoints.
	 * @return
	 */
	public static Comparator priorityComparator() {
		return new CandidatePriorityComparator();
	}
	
	/**
	 * the comparator needs to be overriden too because of 
	 * decorator.
	 */
	static class CandidatePriorityComparator implements Comparator {
        public int compare(Object extEndpoint1, Object extEndpoint2) {
        	
        	//afaics the contract doesn't provide for null elements. 
        	//however they are necessary in this case.
        	if (extEndpoint1 == null && extEndpoint2 == null) return 0;
        	if (extEndpoint1 == null && extEndpoint2 != null) return -1;
        	if (extEndpoint1 != null && extEndpoint2 == null) return 1;
        	
            Candidate a=(Candidate)extEndpoint1;
            Candidate b=(Candidate)extEndpoint2;
            int ret=a.connectScore()-b.connectScore();
            if (ret!=0) 
                return ret;
            else
                return a.getDailyUptime() - b.getDailyUptime();
        }
    }
}