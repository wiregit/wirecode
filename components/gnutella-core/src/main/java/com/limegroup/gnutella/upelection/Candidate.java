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
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.util.*;

import com.sun.java.util.collections.Comparable;
import com.sun.java.util.collections.Iterator;
import com.sun.java.util.collections.Comparator;

import java.net.UnknownHostException;


public class Candidate implements Comparable{
	
	/**
	 * the ultrapeer connection that told us about this host
	 */
	private ReplyHandler _advertiser;
	
	/**
	 * the uptime of this Candidate in minutes
	 */
	private final short _uptime;
	
	/**
	 * the IpPort of this candidate
	 * 
	 */
	private final QueryReply.IPPortCombo _address;
	
	
	/**
	 * the unit to count the uptime in.  Will be changed by tests, so 
	 * its not final.
	 */
	private static int MINUTE = 60*1000;
	
	
	/**
	 * creates a Candidate from a Connection object
	 * @param c the connection to our leaf
	 * @throws UnknownHostException if the ip address could not be resolved. The caller should
	 * skip such hosts.
	 */
	public Candidate(Connection c) throws UnknownHostException {
		_uptime = (short)( (System.currentTimeMillis() - c.getConnectionTime())/(MINUTE) );
		_address = new QueryReply.IPPortCombo(c.getAddress(),c.getPort());
	}
	
	public Candidate(byte []data, int offset) throws BadPacketException{
		if ((data.length-offset) < 8)
			throw new BadPacketException("invalid size candidate");
		
		 String host = NetworkUtils.ip2string(data, offset);
		 int port = ByteOrder.ubytes2int(ByteOrder.leb2short(data, offset+4));
		 try {
		 	_address = new QueryReply.IPPortCombo(host,port);
		 }catch(UnknownHostException uhx) {
		 	//the advertiser shouldn't have sent such candidate.  Consider the entire packet bad.
		 	throw new BadPacketException("invalid candidate advertised");
		 }
		 
		_uptime  =(short) ByteOrder.ubytes2int(ByteOrder.leb2short(data, offset+6));
	}
	
	public Candidate(byte [] data) throws BadPacketException {
		this(data,0);
	}
	
	public byte [] toBytes() {
		byte [] ipport = _address.toBytes();
		byte [] ret = new byte[8];
		
		System.arraycopy(ipport,0,ret,0,ipport.length);
		ByteOrder.short2leb(_uptime,ret,6);
		return ret;
	}
	
	public int compareTo(Object o) {
		if (o==null)
			return 1;
		Candidate other = (Candidate)o;
		return _uptime-other._uptime;
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
	 * decorator and null values support.
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
            return a._uptime-b._uptime;
            
        }
    }
	
	/**
	 * @return Returns the _advertiser.
	 */
	public ReplyHandler getAdvertiser() {
		return _advertiser;
	}
	/**
	 * @param _advertiser The _advertiser to set.
	 */
	public void setAdvertiser(ReplyHandler _advertiser) {
		this._advertiser = _advertiser;
	}
	
	/**
	 * Constructs a candidate with explicitly supplied values.  Will probably be used
	 * only in testing.
	 * @param host the host
	 * @param port the port
	 * @param uptime the uptime in minutes
	 */
	public Candidate(String host, int port, short uptime) throws UnknownHostException {

		_address = new QueryReply.IPPortCombo(host, port);

		_uptime = uptime;
	}
	/**
	 * @return the InetAddress of the candidate
	 */
	public InetAddress getInetAddress() {
		return _address.getInetAddress();
	}
}