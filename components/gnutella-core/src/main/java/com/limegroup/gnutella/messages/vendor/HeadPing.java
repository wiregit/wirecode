package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.messages.BadPacketException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * An UDP equivalent of the HEAD request method with a twist.
 * 
 * The host sending the ping can request that the pong be sent
 * somewhere else.  This is useful if we want to enable nodes in the
 * download mesh tell firewalled notes to punch holes to other nodes.
 * 
 * In order to prevent easy ddosing, if the ping is intended to go 
 * to a different host it will contain only the minimal information.
 * 
 */

public class HeadPing extends VendorMessage {
	public static final int VERSION = 1;
	
	public static final int PLAIN = 0x0;
	public static final int INTERVALS = 0x1;
	public static final int ALT_LOCS = 0x2;
	public static final int FIREWALL_REDIRECT=0x4;
	public static final int PUSH_ALTLOCS=0x8;
	
	public static final int FEATURE_MASK=0xF;
	
	private final URN _urn;
	
	private final byte _features;
	
	private final IpPort _address;
	
	/**
	 * creates a message object with data from the network.
	 */
	protected HeadPing(byte[] guid, byte ttl, byte hops,
			 int version, byte[] payload)
			throws BadPacketException {
		super(guid, ttl, hops, F_LIME_VENDOR_ID, F_UDP_HEAD_PING, version, payload);
		
		//see if the payload is valid
		if (getVersion() == VERSION && (payload == null || payload.length < 41))
			throw new BadPacketException();
		
		byte features;
		features = (byte) (payload [0] & FEATURE_MASK);
		
		//if this is a request for a redirect, strip all other features
		//to avoid ddosing.
		if ((features & FIREWALL_REDIRECT) == FIREWALL_REDIRECT)
			features = (byte)FIREWALL_REDIRECT;
		
		_features = features;
		
		
		//parse the urn string.
		String urnStr = new String(payload,1,41);
		
		
		if (!URN.isUrn(urnStr))
			throw new BadPacketException("udp head request did not contain an urn");
		
		URN urn = null;
		try {
			urn = URN.createSHA1Urn(urnStr);
		}catch(IOException oops) {
			throw new BadPacketException("failed to parse an urn");
		}finally {
			_urn = urn;
		}
		
		//parse the address that is supposed to receive the pong
		if(_features != FIREWALL_REDIRECT)
			_address=null;
		else {
			String host = NetworkUtils.ip2string(payload,42);
			int port = ByteOrder.leb2short(payload,46);
			_address = new Endpoint(host,port);
		}
	}
	
	/**
	 * creates a new udp head request.
	 * @param sha1 the urn to get information about.
	 * @param features which features to include in the response
	 */
	public HeadPing(URN urn, int features) {
		 super(F_LIME_VENDOR_ID, F_UDP_HEAD_PING, VERSION,
		 		derivePayload(urn, features,null));
		 _urn = urn;
		 _features = (byte) (features & FEATURE_MASK);
		 _address=null;
	}
	
	/**
	 * creates a new udp head request whose response should be
	 * sent to the provided address.
	 * @param urn the sha1 to provide information about
	 * @param address the address to send the reply to.
	 */
	public HeadPing(URN urn, IpPort address) {
		super(F_LIME_VENDOR_ID, F_UDP_HEAD_PING, VERSION,
		 		derivePayload(urn, FIREWALL_REDIRECT,address));
		 _urn = urn;
		 _features = FIREWALL_REDIRECT;
		 _address=address;
	}
	
	/**
	 * creates a plain udp head request
	 */
	public HeadPing (URN urn) {
		this(urn, PLAIN);
	}
	
	private static byte [] derivePayload(URN urn, int features, IpPort address) {
		boolean redirect=false;
		features = features & FEATURE_MASK;
		
		// if this is a ping requesting a redirect, clear the other fields.
		if ((features & FIREWALL_REDIRECT) ==  FIREWALL_REDIRECT) {
			if ( address == null)
				throw new IllegalArgumentException(
					"requested a redirect, but no redirect address provided!");
			else {
				features = features & FIREWALL_REDIRECT;
				redirect=true;
			}
		}
		
		String urnStr = urn.httpStringValue();
		int urnlen = urnStr.getBytes().length;
		byte [] ret = new byte[urnlen+ (redirect ? 7 : 1)];
		ret[0]=(byte)features;
		System.arraycopy(urnStr.getBytes(),0,ret,1,urnlen);
		
		//if the pong is meant to be sent elsewhere, include the ip address.
		if (redirect) {
			System.arraycopy(address.getInetAddress().getAddress(),0,
				ret,urnlen+1,4);
		
			ByteOrder.short2leb((short)address.getPort(),ret,urnlen+5);
		}
		
		return ret;
	}
	
	/**
	 * 
	 * @return the URN carried in this head request.
	 */
	public URN getUrn() {
		return _urn;
	}
	
	public boolean requestsRanges() {
		return (_features & INTERVALS) == INTERVALS;
	}
	
	public boolean requestsAltlocs() {
		return (_features & ALT_LOCS) == ALT_LOCS;
	}
	
	public boolean requestsPushLocs() {
		return (_features & PUSH_ALTLOCS) == PUSH_ALTLOCS;
	}
	
	public byte getFeatures() {
		return _features;
	}
	
	/**
	 * @return the receiver of the pong to this ping.
	 * if null, the reply goes to the originating host.
	 */
	public IpPort getAddress() {
		return _address;
	}
}
