/*
 * This class keeps a ref to the Ultrapeer Connection which advertised it.
 * Decorator pattern to allow parsing.  When comparing, make sure you use the provided Comparator.
 */
package com.limegroup.gnutella.upelection;




import java.net.InetAddress;


import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.util.*;

import com.sun.java.util.collections.Comparator;

import java.net.UnknownHostException;


public class RemoteCandidate implements Candidate{
	
	/**
	 * the ultrapeer connection that told us about this host
	 */
	private Connection _advertiser;
	
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
	 * parses a remote cnadidate from network data
	 * @param data the data from the network
	 * @param offset the offset within the array
	 * @throws BadPacketException parsing failed.
	 */
	public RemoteCandidate(byte []data, int offset) throws BadPacketException{
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
	
	public RemoteCandidate(byte [] data) throws BadPacketException {
		this(data,0);
	}
	
	public byte [] toBytes() {
		byte [] ipport = _address.toBytes();
		byte [] ret = new byte[8];
		
		System.arraycopy(ipport,0,ret,0,ipport.length);
		ByteOrder.short2leb(_uptime,ret,6);
		return ret;
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
	 * @return Returns the _advertiser.
	 */
	public Connection getAdvertiser() {
		return _advertiser;
	}
	/**
	 * @param _advertiser The _advertiser to set.
	 */
	public void setAdvertiser(Connection _advertiser) {
		this._advertiser = _advertiser;
	}
	
	/**
	 * Constructs a candidate with explicitly supplied values.  Will probably be used
	 * only in testing.
	 * @param host the host
	 * @param port the port
	 * @param uptime the uptime in minutes
	 */
	public RemoteCandidate(String host, int port, short uptime) throws UnknownHostException {

		_address = new QueryReply.IPPortCombo(host, port);

		_uptime = uptime;
	}
	/**
	 * @return the InetAddress of the candidate
	 */
	public InetAddress getInetAddress() {
		return _address.getInetAddress();
	}
	/**
	 * @return the port of the candidate
	 */
	public int getPort() {
		return _address.getPort();
	}
	
	
	public static int getBytesForVersion(int version) {
		//change this when changing versions.
			return 8;
	}
	
	public boolean isOpen(){
		return _advertiser!=null ? _advertiser.isOpen() : false;
	}
	
	public short getUptime() {
		return _uptime;
	}
	
	public boolean isSame(IpPort o) {
		
		return _address.isSame(o);
	}
	
	public String getAddress() {
		return getInetAddress().getHostAddress();
	}
}