package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AltLocDigest;
import com.limegroup.gnutella.messages.BadGGEPBlockException;
import com.limegroup.gnutella.messages.BadGGEPPropertyException;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.GGEP;

/**
 * An UDP equivalent of the HEAD request method with a twist.
 * 
 * It is routed like a push request to firewalled alternate locations. 
 * As long as the pinging host can receive solicited udp it can be 
 * firewalled as well.
 * 
 * Illustration of [firewalled] NodeA pinging firewalled host NodeB:
 * 
 * 
 * NodeA --------(PUSH_PING,udp)-------->Push
 *    <-------------------(udp)--------- Proxy
 *                                       /|\  | (tcp)
 *                                        |   |
 *                                        |  \|/
 *                                        NodeB
 * 
 * The ping carries a GGEP map with one specific mapping - a mapping whose value
 * gives a hint of which other mappings can be found in the map.  This value is used
 * to convey information to the remote host about what features the pinger supports,
 * as well as any potential metadata about those features.
 * 
 * The value itself is a bitvector with unlimited length. If a bit N is set, then we might
 * find two other mappings in the map :
 * Nm - key containing metadata about the feature such as version, etc.
 * Nd - key containing actual data associated with the feature (optional).
 * 
 * For example, if a pinger wants to convey that it supports bloom filters up to version 2
 * and at the same time send its own bloom filter, and if the bit indicating bloom filter
 * support is the least-significant digit, the ggep map would contain the following:
 * 
 * "P" : 00000.....1
 * "1m" : 0x2
 * "1d" : <... binary data> 
 */

public class HeadPing extends VendorMessage {
	public static final int VERSION = 1;
	
	/**
	 * requsted content of the pong
	 */
	public static final int PLAIN = 0x0;
	public static final int INTERVALS = 0x1;
	public static final int ALT_LOCS = 0x2;
	public static final int PUSH_ALTLOCS=0x4;
	public static final int FWT_PUSH_ALTLOCS=0x8;
	public static final int GGEP_PING=0x10;

	/**
	 * the feature mask.
	 */
	public static final int FEATURE_MASK=0x1F;
	
	//// Various constants related to the properties ggep //////
	/** 
	 * a ggep field name containing the client guid of the node we would like
	 * this ping routed to.
	 */
	private static final String GGEP_PUSH = "PUSH";
	
	
	//////////

	/** The URN of the file being requested */
	private final URN _urn;
	
	/** The format of the response that we desire */
	private final byte _features;

	/** The GGEP fields in this pong, if any */
	GGEP _ggep;
	/** 
	 * The client GUID of the host we wish this ping routed to.
	 * null if pinging directly.
	 */ 
	private final GUID _clientGUID;
	
	/**
	 * The bloom filter carried by this ping, if any
	 */
	private AltLocDigest _digest,_pushDigest;
	
	/**
	 * Whether the pinger supports bloom filters.
	 */
	private final boolean _supportsBloom, _supportsPushBloom;
	
	/**
	 * creates a message object with data from the network.
	 */
	protected HeadPing(byte[] guid, byte ttl, byte hops,
			 int version, byte[] payload)
			throws BadPacketException {
		super(guid, ttl, hops, F_LIME_VENDOR_ID, F_UDP_HEAD_PING, version, payload);
		
		//see if the payload is valid
		if (getVersion() == VERSION && (payload == null || payload.length < 42))
			throw new BadPacketException();
		
		_features = (byte) (payload [0] & FEATURE_MASK);
		
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
		
		// parse the GGEP if any
		GGEP g = null;
		if ((_features  & GGEP_PING) == GGEP_PING) {
			if (payload.length < 43)
				throw new BadPacketException("no ggep was found.");
			try {
				g = new GGEP(payload, 42, null);
			} catch (BadGGEPBlockException bpx) {
				throw new BadPacketException("invalid ggep block");
			}
		}
		_ggep = g;
		
		// extract the client guid if any
		GUID clientGuid = null;
		boolean supportsBloom = false;
		boolean supportsPushBloom = false;
		if (_ggep != null) {
		    
		    // see if there is a client guid
			try {
				clientGuid = new GUID(_ggep.getBytes(GGEP_PUSH));
			} catch (BadGGEPPropertyException noGuid) {}
			
			// see if there is a properties field
			try {
			    byte [] props = _ggep.getBytes(GGEPHeadConstants.GGEP_PROPS);
			    int propSet = (int)props[props.length-1];
			    if ((propSet & GGEPHeadConstants.GGEP_BLOOM) == GGEPHeadConstants.GGEP_BLOOM)
			        supportsBloom = true;
			    if ((propSet & GGEPHeadConstants.GGEP_PUSH_BLOOM) == GGEPHeadConstants.GGEP_PUSH_BLOOM)
			        supportsPushBloom = true;
			} catch (BadGGEPPropertyException bloomNotSupported) {}
        } 
		
        _clientGUID=clientGuid;
        _supportsBloom = supportsBloom;
        _supportsPushBloom = supportsPushBloom;
	}
	
	/**
	 * creates a new udp head request.
	 * @param sha1 the urn to get information about.
	 * @param features which features to include in the response
	 */

	public HeadPing(URN sha1, int features) {
		this (sha1, null, null, features);
	}
	
	
	public HeadPing(URN sha1, GUID clientGUID, AltLocDigest []digest, int features) {
		super(F_LIME_VENDOR_ID, F_UDP_HEAD_PING, VERSION,
		 		derivePayload(sha1, clientGUID, digest, features));
		_features = (byte)((features | GGEP_PING) & FEATURE_MASK);
		_urn = sha1;
		_clientGUID = clientGUID;
		
		if (digest == null || digest.length < 2) {
		    _digest = null;
		    _pushDigest = null;
		} else {
		    _digest = digest[0];
		    _pushDigest = digest[1];
		}
		
		_supportsBloom=true;
		_supportsPushBloom=true;
	}

	
	/**
	 * creates a plain udp head request
	 */
	public HeadPing (URN urn) {
		this(urn, GGEP_PING);
	}
	

	
	private static byte [] derivePayload(URN urn, GUID clientGUID, AltLocDigest []filter, int features) {

		features = features & FEATURE_MASK;

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream daos = new DataOutputStream(baos);
		
		String urnStr = urn.httpStringValue();
		
		//make sure we indicate we'll have ggep.
		GGEP ggep = new GGEP(true);
		features |= GGEP_PING;
		
		// we always support bloom filters, even if we don't carry one
		GGEPHeadConstants.addDefaultGGEPProperties(ggep);
		
		// if we have any filters, serialize them
		if (filter != null && filter.length > 1) {
		    if (filter[0] != null) {
		        ggep.put(new String((char)GGEPHeadConstants.GGEP_BLOOM+GGEPHeadConstants.DATA), 
		                filter[0].toBytes());
		    }
		    if (filter[1] != null) {
		        ggep.put(new String((char)GGEPHeadConstants.GGEP_PUSH_BLOOM+GGEPHeadConstants.DATA), 
		                filter[1].toBytes());
		    }
		}
		
		// is this a push ping?
		if (clientGUID != null) 
			ggep.put(GGEP_PUSH,clientGUID.bytes());  
		
		try {
			daos.writeByte(features);
			daos.writeBytes(urnStr);
			if ( ggep != null) 
				ggep.write(daos);
		}catch (IOException huh) {
			ErrorService.error(huh);
		}
		
		return baos.toByteArray();
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
	
	public boolean requestsFWTPushLocs() {
		return (_features & FWT_PUSH_ALTLOCS) == FWT_PUSH_ALTLOCS;
	}
	
	public byte getFeatures() {
		return _features;
	}
	
	public GUID getClientGuid() {
		return _clientGUID;
	}
	
	/**
	 * @return whether the pinger indicates that it supports altloc digests
	 */
	public boolean supportsDigests() {
	    return _supportsBloom; 
	}
	
	/**
	 * @return whether the pinger indicates that it supports pushloc digests
	 */
	public boolean supportsPushDigests() {
	    return _supportsPushBloom;
	}
	
	/**
	 * @return the altloc digest contained in this ping.  null if none
	 */
	public AltLocDigest getDigest() {
	    if (_digest == null)
	        parseDigest();
	    return _digest; 
	}
	
	/**
	 * @return the pushloc digest contained in this ping.  null if none
	 */
	public AltLocDigest getPushDigest() {
	    if (_pushDigest == null)
	        parsePushDigest();
	    return _pushDigest; 
	}
	
	private void parseDigest() {
	    if (_ggep != null) {
	        //see if there is an altloc digest
	        try {
	            byte [] data = 
	                _ggep.getBytes((char)GGEPHeadConstants.GGEP_BLOOM+GGEPHeadConstants.DATA);
	            _digest = AltLocDigest.parseDigest(data,0,data.length);
	        } catch (BadGGEPPropertyException noBloom){}
	        catch(IOException badBloom) {} //ignore it?
	    }
	}
	
	private void parsePushDigest() {
	    if (_ggep != null) {
	        // see if there is a pushloc digest
	        try {
	            byte [] data = 
	                _ggep.getBytes((char)GGEPHeadConstants.GGEP_PUSH_BLOOM+GGEPHeadConstants.DATA);
	            _pushDigest = AltLocDigest.parseDigest(data,0,data.length);
	        } catch (BadGGEPPropertyException noBloom){}
	        catch(IOException badBloom) {} //ignore it?
	    }
	}
	
	/**
	 * @return a location to which we can get back at the host, if it is firewalled.
	 * null if not firewalled.
	 */
	public PushEndpoint getPushAddress() {
	    return null;
	}

}

/**
 * A class listing some GGEP constants shared between HeadPings and HeadPongs. 
 */
final class GGEPHeadConstants {
    
	/**
	 * the ggep field name containing the various ggep features supported by 
	 * headpings and headpongs
	 */
	static final String GGEP_PROPS = "P";
	
	/**
	 * suffix for a key whose value is actual data carried to the other side
	 */
	static final String DATA = "d";
	
	/**
	 * suffix for a key whose value is metadata describing what data the client
	 * can understand.
	 */
	static final String META = "m";
	
	/**
	 * a flag whose presence in the GGEP_PROPS value indicates the sender supports bloom filters.
	 * The value of this key would be the serialized bloom filter in pings. 
	 */
	static final int GGEP_BLOOM = 0x1;
	static final int GGEP_PUSH_BLOOM = 0x1 << 1;
	
	/**
	 * a flag whose presence in the GGEP_PROPS value indicates the sender understands the
	 * field carrying the PushEndpoint address of the pinger.  If such key exists, it would
	 * contain an updated PE for the sender.  Pings should contain this in order to allow the
	 * remote host to ping them back, and pongs may contain it if they want to update the pinger
	 * about their current set of proxies.  
	 * (not supported yet)
	 */
	static final int GGEP_MYPE = 0x1 << 2;
	
	/**
	 * a flag whose presence indicates support for including available ranges of the file.
	 * the metadata field can indicate whether the ranges are in BitSet or
	 * IntervalSet format, or whether 64 bit values are supported.
	 */
	static final int RANGES = 0x1 << 3;
	
	/**
	 * possible values for the metadata field of the RANGES key.  The same format is prepended
	 * to the content of the data key to facilitate parsing.
	 */
	static final int RANGE_LIST = 0x1;
	static final int LONG_RANGES = 0x1 << 1;
	
	/**
	 * a flag whose presence indicates support for including altlocs in the response.
	 */
	static final int ALTLOCS = 0x1 << 4;
	static final int PUSHLOCS = 0x1 << 5;
	
	/**
	 * a flag whose presence indicates support for reading statistics about the variable-sized
	 * digests.  Without any future metadata modifying it, its value would contain the number 
	 * of altlocs filtered and the total number of altlocs available. 
	 */
	static final int ALT_MESH_STAT = 0x1 << 6;
	static final int PUSH_MESH_STAT = 0x1 << 7;
	
    /**
     * Writes a set of default properties and metadata about what we can support.
     * Similar to the various X-.. headers.
     */
    static void addDefaultGGEPProperties(GGEP dest) {
        
        // add the features we understand
        byte [] supportedFeatures = new byte[] {(byte)
                (GGEP_BLOOM |
                GGEP_PUSH_BLOOM |
                RANGES |
                ALTLOCS |
                PUSHLOCS |
                ALT_MESH_STAT |
                PUSH_MESH_STAT)};
        dest.put(GGEP_PROPS,supportedFeatures);
        
        //also say that we support only the list of 32-bit ranges format.
        dest.put((char)RANGES+META,RANGE_LIST);
        
    }
}
