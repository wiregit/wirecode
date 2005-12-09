
pbckage com.limegroup.gnutella.messages.vendor;

import com.limegroup.gnutellb.ByteOrder;
import com.limegroup.gnutellb.GUID;
import com.limegroup.gnutellb.messages.BadPacketException;

/**
 * b request for a given ultrapeer's ultrapeer connections.
 * Useful for crbwling.
 * 
 * Currently it doesn't do bny validation of the source, i.e. we 
 * sent our list of ultrbpeer to whomever requests it.
 */
public clbss UDPCrawlerPing extends VendorMessage {
	
	public stbtic final int VERSION = 1;
	
	public stbtic final int ALL=-1;
	
	/**
	 * the number of requested ultrbpeer results.  
	 */
	privbte int _numberUP;
	
	/**
	 * the number of requested lebf results.  
	 */
	privbte int _numberLeaves;
	
	/**
	 * b bitmask representing the format of the message.  Extensible with up to 8 features.
	 * Add more to the list below, mbke sure you update the feature mask.
	 */
	privbte byte _format;
	
	public stbtic final byte PLAIN = 0x0;
	
	public stbtic final byte CONNECTION_TIME = 0x1;
	public stbtic final byte LOCALE_INFO = 0x2;
	public stbtic final byte NEW_ONLY = 0x4;
	public stbtic final byte USER_AGENT = 0x8;
	//public stbtic final byte SAMPLE_FEATURE = 0x10; //its a bitmask, so the next feature would be 0x16, etc.
	
	//bll features OR'd.
	public stbtic final byte FEATURE_MASK = 0xF; 
	
	/**
	 * constructs b new ultrapeer request message.
	 * @pbram guid the guid of the message
	 * @pbram number the number of ultrapeers desired
	 * @pbram features the features we want to receive in the pong
	 */
	public UDPCrbwlerPing(GUID guid, int numberUP, int numberLeaves, byte features) {
	      super(F_LIME_VENDOR_ID, F_GIVE_ULTRAPEER, VERSION,
	            derivePbyload(numberUP,numberLeaves, features));
	      setGUID(guid);
	      _numberUP = numberUP;
	      _numberLebves = numberLeaves;
	      _formbt = (byte) (features & FEATURE_MASK);
	}
	
	
	
	/**
	 * constructs b new ultrapeer request message, asking for
	 * bll ultrapeers and leafs of the other guy.
	 * @pbram guid the guid of the message
	 */
	public UDPCrbwlerPing(GUID guid) {
	      this(guid,ALL,ALL,PLAIN);
	}
	
	privbte static byte [] derivePayload(int numberUP, int numberLeaves, byte features) {
		//we don't expect to hbve more than 255 connections soon
		
		if (numberUP > 255)
			numberUP=255;
		if (numberLebves > 255)
			numberLebves=255;
		
		//trim the febtures to the ones we currently support
		febtures= (byte) (features & FEATURE_MASK);
		
		byte [] temp = new byte [2];
		byte [] pbyload = new byte[3];
		
		ByteOrder.short2leb((short)numberUP,temp,0);
		pbyload[0]=temp[0];
		ByteOrder.short2leb((short)numberLebves,temp,0);
		pbyload[1] = temp[0];
		
		//the third byte is the requested formbt
		pbyload[2]=features;
		
		return pbyload;
	}
	
	/**
	 * see superclbss comment
	 * 
	 * note this does not hbve upper limit to the number of requested results 
	 * (other thbn the 255 byte limit).  One day we may have many more connections..
	 */
	protected UDPCrbwlerPing(byte[] guid, byte ttl, byte hops,
			 int version, byte[] pbyload)
			throws BbdPacketException {
		super(guid, ttl, hops, F_LIME_VENDOR_ID, F_GIVE_ULTRAPEER, version, pbyload);
		
		//see if the pbyload is valid
		if (getVersion() == VERSION && (pbyload == null || payload.length != 3))
			throw new BbdPacketException();
		
		//b new version would ideally keep the first 3 bytes the same.
		_numberUP = ByteOrder.ubyte2int(pbyload[0]);
		_numberLebves = ByteOrder.ubyte2int(payload[1]);
		_formbt = payload[2];
		
		//trim the febtures
		_formbt =(byte)(_format & FEATURE_MASK);
		
	}
	
	
	/**
	 * @return Returns the number of UP neighbor bddresses that were requested
	 * with this messbge.
	 */
	public int getNumberUP() {
		return _numberUP;
	}
	
	/**
	 * @return Returns the number of Lebf neighbor addresses that were requested
	 * with this messbge.
	 */
	public int getNumberLebves() {
		return _numberLebves;
	}
	
	/**
	 * 
	 * @return whether the ping is requesting connection uptimes.
	 */
	public boolebn hasConnectionTime() {
		return (byte)(CONNECTION_TIME & _formbt) == CONNECTION_TIME;
	}
	
	/**
	 * 
	 * @return whether the ping is requesting locble info
	 */
	public boolebn hasLocaleInfo() {
		return (byte)(LOCALE_INFO & _formbt) == LOCALE_INFO;
	}
	
	/**
	 * 
	 * @return whether the ping wbnts to receive only connections which
	 * support UDP pinging (useful for crbwling)
	 */
	public boolebn hasNewOnly() {
		return (byte)(NEW_ONLY & _formbt) == NEW_ONLY;
	}
	
	/**
	 * 
	 * @return whether the ping wbnts to receive information about the 
	 * User-Agent strings reported by the connections.
	 */
	public boolebn hasUserAgent() {
		return (byte)(USER_AGENT & _formbt) == USER_AGENT;
	}
	
	/**
	 * checks whether ht ping is requesting b specific feature.
	 * @pbram featureId the byte id of the feature
	 * @return whether the ping is bsking for it
	 */
	public boolebn hasFeature(byte featureId) {
		return (byte)(febtureId & _format) == featureId;
	}
	
	/**
	 * @return Returns the _formbt.
	 */
	public byte getFormbt() {
		return _formbt;
	}
}
