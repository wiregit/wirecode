
package com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.messages.BadPacketException;

/**
 * a request for a given ultrapeer's ultrapeer connections.
 * Useful for crawling.
 * 
 * Currently it doesn't do any validation of the source, i.e. we 
 * sent our list of ultrapeer to whomever requests it.
 */
public class UDPCrawlerPing extends VendorMessage {
	
	public static final int VERSION = 1;
	
	public static final int ALL=-1;
	
	/**
	 * the number of requested ultrapeer results.  
	 */
	private int _numberUP;
	
	/**
	 * the number of requested leaf results.  
	 */
	private int _numberLeaves;
	
	/**
	 * a bitmask representing the format of the message.  Extensible with up to 8 features.
	 * Add more to the list below, make sure you update the feature mask.
	 */
	private byte _format;
	
	public static final byte PLAIN = 0x0;
	
	public static final byte CONNECTION_TIME = 0x1;
	public static final byte LOCALE_INFO = 0x2;
	public static final byte NEW_ONLY = 0x4;
	public static final byte USER_AGENT = 0x8;
	//public static final byte SAMPLE_FEATURE = 0x10; //its a bitmask, so the next feature would be 0x16, etc.
	
	//all features OR'd.
	public static final byte FEATURE_MASK = 0xF; 
	
	/**
	 * constructs a new ultrapeer request message.
	 * @param guid the guid of the message
	 * @param number the number of ultrapeers desired
	 * @param features the features we want to receive in the pong
	 */
	public UDPCrawlerPing(GUID guid, int numberUP, int numberLeaves, byte features) {
	      super(F_LIME_VENDOR_ID, F_GIVE_ULTRAPEER, VERSION,
	            derivePayload(numberUP,numberLeaves, features));
	      setGUID(guid);
	      _numberUP = numberUP;
	      _numberLeaves = numberLeaves;
	      _format = (byte) (features & FEATURE_MASK);
	}
	
	
	
	/**
	 * constructs a new ultrapeer request message, asking for
	 * all ultrapeers and leafs of the other guy.
	 * @param guid the guid of the message
	 */
	public UDPCrawlerPing(GUID guid) {
	      this(guid,ALL,ALL,PLAIN);
	}
	
	private static byte [] derivePayload(int numberUP, int numberLeaves, byte features) {
		//we don't expect to have more than 255 connections soon
		
		if (numberUP > 255)
			numberUP=255;
		if (numberLeaves > 255)
			numberLeaves=255;
		
		//trim the features to the ones we currently support
		features= (byte) (features & FEATURE_MASK);
		
		byte [] temp = new byte [2];
		byte [] payload = new byte[3];
		
		ByteOrder.short2leb((short)numberUP,temp,0);
		payload[0]=temp[0];
		ByteOrder.short2leb((short)numberLeaves,temp,0);
		payload[1] = temp[0];
		
		//the third byte is the requested format
		payload[2]=features;
		
		return payload;
	}
	
	/**
	 * see superclass comment
	 * 
	 * note this does not have upper limit to the number of requested results 
	 * (other than the 255 byte limit).  One day we may have many more connections..
	 */
	protected UDPCrawlerPing(byte[] guid, byte ttl, byte hops,
			 int version, byte[] payload)
			throws BadPacketException {
		super(guid, ttl, hops, F_LIME_VENDOR_ID, F_GIVE_ULTRAPEER, version, payload);
		
		//see if the payload is valid
		if (getVersion() == VERSION && (payload == null || payload.length != 3))
			throw new BadPacketException();
		
		//a new version would ideally keep the first 3 bytes the same.
		_numberUP = ByteOrder.ubyte2int(payload[0]);
		_numberLeaves = ByteOrder.ubyte2int(payload[1]);
		_format = payload[2];
		
		//trim the features
		_format =(byte)(_format & FEATURE_MASK);
		
	}
	
	
	/**
	 * @return Returns the number of UP neighbor addresses that were requested
	 * with this message.
	 */
	public int getNumberUP() {
		return _numberUP;
	}
	
	/**
	 * @return Returns the number of Leaf neighbor addresses that were requested
	 * with this message.
	 */
	public int getNumberLeaves() {
		return _numberLeaves;
	}
	
	/**
	 * 
	 * @return whether the ping is requesting connection uptimes.
	 */
	public boolean hasConnectionTime() {
		return (byte)(CONNECTION_TIME & _format) == CONNECTION_TIME;
	}
	
	/**
	 * 
	 * @return whether the ping is requesting locale info
	 */
	public boolean hasLocaleInfo() {
		return (byte)(LOCALE_INFO & _format) == LOCALE_INFO;
	}
	
	/**
	 * 
	 * @return whether the ping wants to receive only connections which
	 * support UDP pinging (useful for crawling)
	 */
	public boolean hasNewOnly() {
		return (byte)(NEW_ONLY & _format) == NEW_ONLY;
	}
	
	/**
	 * 
	 * @return whether the ping wants to receive information about the 
	 * User-Agent strings reported by the connections.
	 */
	public boolean hasUserAgent() {
		return (byte)(USER_AGENT & _format) == USER_AGENT;
	}
	
	/**
	 * checks whether ht ping is requesting a specific feature.
	 * @param featureId the byte id of the feature
	 * @return whether the ping is asking for it
	 */
	public boolean hasFeature(byte featureId) {
		return (byte)(featureId & _format) == featureId;
	}
	
	/**
	 * @return Returns the _format.
	 */
	public byte getFormat() {
		return _format;
	}
}
