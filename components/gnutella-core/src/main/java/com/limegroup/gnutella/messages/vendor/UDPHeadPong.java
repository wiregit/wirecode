
package com.limegroup.gnutella.messages.vendor;

import java.io.IOException;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AlternateLocationCollection;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.util.IntervalSet;

/**
 * a response to an UDPHeadPing.  It is a trimmed down version of the standard HEAD response,
 * since we are trying to keep the sizes of the udp packets small.
 * 
 * Since headpings will be sent by clients who have started to download a file whose download
 * mesh contains  this host, it needs to contain information that will help those clients whether 
 * this host is a good bet to start an http download from.  Therefore, the following information should
 * be included in the response:
 * 
 *  - available ranges of the file 
 *  - queue status
 *  - some altlocs (if space permits)
 * 
 * the queue status can be an integer representing how many people are waiting in the queue.  If 
 * nobody is waiting in the queue and we have slots available, the integer can be negative.  So if
 * we have 3 people on the queue we'd send the integer 3.  If we have nobody on the queue and 
 * two upload slots available we would send -2.  A value of 0 means all upload slots are taken but 
 * the queue is empty.  This information can be used by the downloaders to better judge chances of
 * successful start of the download. 
 */
public class UDPHeadPong extends VendorMessage {
	
	public static final int VERSION = 1;
	
	/**
	 * available ranges
	 */
	IntervalSet _ranges;
	
	/**
	 * the altlocs that were sent, if any
	 */
	AlternateLocationCollection _altLocs;
	
	/**
	 * the queue status, can be negative
	 */
	int _queueStatus;
	
	/**
	 * creates a message object with data from the network.
	 */
	protected UDPHeadPong(byte[] guid, byte ttl, byte hops,
			 int version, byte[] payload)
			throws BadPacketException {
		super(guid, ttl, hops, F_LIME_VENDOR_ID, F_UDP_HEAD_PONG, version, payload);
		throw new Error("not implemented");
	}
	
	/**
	 * creates a message object as a response to a udp head request.
	 */
	public UDPHeadPong(UDPHeadPing ping) {
		super(F_LIME_VENDOR_ID, F_UDP_HEAD_PONG, VERSION,
		 		derivePayload(ping));
	}
	
	private static byte [] derivePayload(UDPHeadPing ping) {
		throw new Error("not implemented");
	}
}
