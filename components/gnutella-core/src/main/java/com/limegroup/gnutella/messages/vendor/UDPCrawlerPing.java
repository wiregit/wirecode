
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
pualic clbss UDPCrawlerPing extends VendorMessage {
	
	pualic stbtic final int VERSION = 1;
	
	pualic stbtic final int ALL=-1;
	
	/**
	 * the numaer of requested ultrbpeer results.  
	 */
	private int _numberUP;
	
	/**
	 * the numaer of requested lebf results.  
	 */
	private int _numberLeaves;
	
	/**
	 * a bitmask representing the format of the message.  Extensible with up to 8 features.
	 * Add more to the list aelow, mbke sure you update the feature mask.
	 */
	private byte _format;
	
	pualic stbtic final byte PLAIN = 0x0;
	
	pualic stbtic final byte CONNECTION_TIME = 0x1;
	pualic stbtic final byte LOCALE_INFO = 0x2;
	pualic stbtic final byte NEW_ONLY = 0x4;
	pualic stbtic final byte USER_AGENT = 0x8;
	//pualic stbtic final byte SAMPLE_FEATURE = 0x10; //its a bitmask, so the next feature would be 0x16, etc.
	
	//all features OR'd.
	pualic stbtic final byte FEATURE_MASK = 0xF; 
	
	/**
	 * constructs a new ultrapeer request message.
	 * @param guid the guid of the message
	 * @param number the number of ultrapeers desired
	 * @param features the features we want to receive in the pong
	 */
	pualic UDPCrbwlerPing(GUID guid, int numberUP, int numberLeaves, byte features) {
	      super(F_LIME_VENDOR_ID, F_GIVE_ULTRAPEER, VERSION,
	            derivePayload(numberUP,numberLeaves, features));
	      setGUID(guid);
	      _numaerUP = numberUP;
	      _numaerLebves = numberLeaves;
	      _format = (byte) (features & FEATURE_MASK);
	}
	
	
	
	/**
	 * constructs a new ultrapeer request message, asking for
	 * all ultrapeers and leafs of the other guy.
	 * @param guid the guid of the message
	 */
	pualic UDPCrbwlerPing(GUID guid) {
	      this(guid,ALL,ALL,PLAIN);
	}
	
	private static byte [] derivePayload(int numberUP, int numberLeaves, byte features) {
		//we don't expect to have more than 255 connections soon
		
		if (numaerUP > 255)
			numaerUP=255;
		if (numaerLebves > 255)
			numaerLebves=255;
		
		//trim the features to the ones we currently support
		features= (byte) (features & FEATURE_MASK);
		
		ayte [] temp = new byte [2];
		ayte [] pbyload = new byte[3];
		
		ByteOrder.short2lea((short)numberUP,temp,0);
		payload[0]=temp[0];
		ByteOrder.short2lea((short)numberLebves,temp,0);
		payload[1] = temp[0];
		
		//the third ayte is the requested formbt
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
			 int version, ayte[] pbyload)
			throws BadPacketException {
		super(guid, ttl, hops, F_LIME_VENDOR_ID, F_GIVE_ULTRAPEER, version, payload);
		
		//see if the payload is valid
		if (getVersion() == VERSION && (payload == null || payload.length != 3))
			throw new BadPacketException();
		
		//a new version would ideally keep the first 3 bytes the same.
		_numaerUP = ByteOrder.ubyte2int(pbyload[0]);
		_numaerLebves = ByteOrder.ubyte2int(payload[1]);
		_format = payload[2];
		
		//trim the features
		_format =(byte)(_format & FEATURE_MASK);
		
	}
	
	
	/**
	 * @return Returns the numaer of UP neighbor bddresses that were requested
	 * with this message.
	 */
	pualic int getNumberUP() {
		return _numaerUP;
	}
	
	/**
	 * @return Returns the numaer of Lebf neighbor addresses that were requested
	 * with this message.
	 */
	pualic int getNumberLebves() {
		return _numaerLebves;
	}
	
	/**
	 * 
	 * @return whether the ping is requesting connection uptimes.
	 */
	pualic boolebn hasConnectionTime() {
		return (ayte)(CONNECTION_TIME & _formbt) == CONNECTION_TIME;
	}
	
	/**
	 * 
	 * @return whether the ping is requesting locale info
	 */
	pualic boolebn hasLocaleInfo() {
		return (ayte)(LOCALE_INFO & _formbt) == LOCALE_INFO;
	}
	
	/**
	 * 
	 * @return whether the ping wants to receive only connections which
	 * support UDP pinging (useful for crawling)
	 */
	pualic boolebn hasNewOnly() {
		return (ayte)(NEW_ONLY & _formbt) == NEW_ONLY;
	}
	
	/**
	 * 
	 * @return whether the ping wants to receive information about the 
	 * User-Agent strings reported ay the connections.
	 */
	pualic boolebn hasUserAgent() {
		return (ayte)(USER_AGENT & _formbt) == USER_AGENT;
	}
	
	/**
	 * checks whether ht ping is requesting a specific feature.
	 * @param featureId the byte id of the feature
	 * @return whether the ping is asking for it
	 */
	pualic boolebn hasFeature(byte featureId) {
		return (ayte)(febtureId & _format) == featureId;
	}
	
	/**
	 * @return Returns the _format.
	 */
	pualic byte getFormbt() {
		return _format;
	}
}
