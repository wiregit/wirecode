
package com.limegroup.gnutella.messages.vendor;

import java.io.*;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.altlocs.AlternateLocationCollection;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.gnutella.util.*;

import com.sun.java.util.collections.*;

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
 * 
 * Format:
 * 
 * 1 byte - features byte
 * 2 byte - response code
 * 4 bytes - vendor id
 * 1 byte - queue status
 * n*8 bytes - n intervals (if requested && file partial && fits in packet)
 * the rest - altlocs (if requested && fits in packet) 
 */
public class UDPHeadPong extends VendorMessage {
	
	/**
	 * try to make packets less than this size
	 */
	private static final int PACKET_SIZE = 512;
	
	/**
	 * instead of using the HTTP codes, use byte-values.
	 */
	private static final byte FILE_NOT_FOUND= (byte)0;
	private static final byte COMPLETE_FILE= (byte)1;
	private static final byte PARTIAL_FILE = (byte)2;
	
	/**
	 * all our slots are full..
	 */
	private static final byte BUSY=(byte)0x7F;
	
	public static final int VERSION = 1;
	
	/**
	 * the features contained in this pong.  Same as those of the originating ping
	 */
	private byte _features;
	
	/**
	 * available ranges
	 */
	private IntervalSet _ranges;
	
	/**
	 * the altlocs that were sent, if any
	 */
	private Set _altLocs;
	
	/**
	 * the queue status, can be negative
	 */
	private int _queueStatus;
	
	/**
	 * whether the other host has the file at all
	 */
	private boolean _fileFound,_completeFile;
	
	/**
	 * the remote host
	 */
	private byte [] _vendorId;
	
	/**
	 * creates a message object with data from the network.
	 */
	protected UDPHeadPong(byte[] guid, byte ttl, byte hops,
			 int version, byte[] payload)
			throws BadPacketException {
		super(guid, ttl, hops, F_LIME_VENDOR_ID, F_UDP_HEAD_PONG, version, payload);
		
		//we should have some payload
		if (payload==null || payload.length<2)
			throw new BadPacketException("empty payload");
		
		
		//if we are version 1, the first byte has to be FILE_NOT_FOUND, PARTIAL_FILE 
		//or COMPLETE_FILE
		if (version == VERSION && 
				payload[1]!=FILE_NOT_FOUND &&
				payload[1]!=PARTIAL_FILE &&
				payload[1]!=COMPLETE_FILE)
			throw new BadPacketException("invalid payload for version "+version);
		
		try {
			
		
		DataInputStream dais = new DataInputStream(new ByteArrayInputStream(payload));
		
		//read and mask the features
		_features = (byte) (dais.readByte() & UDPHeadPing.FEATURE_MASK);
		
		//read the response code
		byte code = dais.readByte();
		
		//if the other host doesn't have the file, stop parsing
		if (code == FILE_NOT_FOUND) 
			return;
		else
			_fileFound=true;
		
		//read the vendor id
		_vendorId = new byte[4];
		dais.readFully(_vendorId);
		
		//read the queue status
		_queueStatus = dais.readByte();
		
		//the queue status can be negative.. check the msb
		if (_queueStatus > BUSY)
			_queueStatus = _queueStatus - 0xFF;
		
		//if we have a partial file and the pong carries ranges, parse their list
		if (code == COMPLETE_FILE) 
			_completeFile=true;
		else 
			if ( (_features & UDPHeadPing.INTERVALS) == 
				UDPHeadPing.INTERVALS){
			
				short rangeLength=dais.readShort();
				byte [] ranges = new byte [rangeLength];
				dais.readFully(ranges);
				_ranges = IntervalSet.parseBytes(ranges);
			}
		
		//parse any included altlocs
		if ((_features & UDPHeadPing.ALT_LOCS) == UDPHeadPing.ALT_LOCS) {
			byte [] altlocs = new byte[dais.available()];
			dais.readFully(altlocs);
			_altLocs = new HashSet();
			_altLocs.addAll(NetworkUtils.unpackIps(altlocs));
		}
		
		}catch(IOException oops) {
			throw new BadPacketException(oops.getMessage());
		}
	}
	
	/**
	 * creates a message object as a response to a udp head request.
	 */
	public UDPHeadPong(UDPHeadPing ping) {
		super(F_LIME_VENDOR_ID, F_UDP_HEAD_PONG, VERSION,
		 		derivePayload(ping));
		setGUID(new GUID(ping.getGUID()));
	}
	
	
	/**
	 * packs information about the shared file, queue status and altlocs into the body
	 * of the message.
	 * @param ping the original UDP head ping to respond to
	 */
	private static byte [] derivePayload(UDPHeadPing ping)  {
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		CountingOutputStream caos = new CountingOutputStream(baos);
		
		final byte retCode;
		byte queueStatus;
		
		
		URN urn = ping.getUrn();
		FileDesc desc = RouterService.getFileManager().getFileDescForUrn(urn);
		
		try{
			
		byte features = ping.getFeatures();
		
		caos.write(features);
		
		//if we don't have the file..
		if (desc == null) {
			caos.write(FILE_NOT_FOUND);
			return baos.toByteArray();
		}
		
		//we have the file... is it complete or not?
		if (desc instanceof IncompleteFileDesc)
			retCode = COMPLETE_FILE;
		else 
			retCode = PARTIAL_FILE;
		caos.write(retCode);
		
		//write the vendor id
		caos.write(F_LIME_VENDOR_ID);
		
		//get our queue status.
		int queueSize = RouterService.getUploadManager().getNumQueuedUploads();
		
		if (queueSize == UploadSettings.UPLOAD_QUEUE_SIZE.getValue())
			queueStatus = BUSY;
		else if (queueSize > 0) 
			queueStatus = (byte) queueSize;
		 else 	
			//optimistic value
			queueStatus =  (byte)
				(RouterService.getUploadManager().uploadsInProgress() - 
						UploadSettings.HARD_MAX_UPLOADS.getValue() );
		
		
		//write out the return code and the queue status
		caos.write(queueStatus);
		
		//if we sent partial file and the remote asked for ranges, send them 
		if (retCode == PARTIAL_FILE && ping.requestsRanges()) {
			IncompleteFileDesc ifd = (IncompleteFileDesc) desc;
			byte [] ranges =ifd.getRangesAsByte();
			
			//write the ranges only if they will fit in the packet
			if (caos.getAmountWritten() + ranges.length <= PACKET_SIZE) {
				caos.write((short)ranges.length);
				caos.write(ranges);
			} 
			else { //the ranges will not fit - say we didn't send them.
				features = (byte) ( features & ~UDPHeadPing.INTERVALS);
				baos.toByteArray()[0] = features;
			}
			
		}
		
		//if we have any altlocs and enough room in the packet, add them.
		AlternateLocationCollection altlocs = desc.getAlternateLocationCollection();
		
		if (altlocs!= null && altlocs.hasAlternateLocations() &&
				ping.requestsAltlocs()) {
			byte [] altbytes = altlocs.toBytes();
			
			if (caos.getAmountWritten() + altbytes.length <= PACKET_SIZE)
				caos.write(altbytes);
			else {
				//altlocs will not fit - say we didn't send them
				features = (byte) ( features & ~UDPHeadPing.ALT_LOCS);
				baos.toByteArray()[0] = features;
			}
				
		}
			
		}catch(IOException impossible) {
			ErrorService.error(impossible);
		}
		
		//done!
		return baos.toByteArray();
	}
	
	/**
	 * 
	 * @return whether the alternate location still has the file
	 */
	public boolean hasFile() {
		return _fileFound;
	}
	
	/**
	 * 
	 * @return whether the alternate location has the complete file
	 */
	public boolean hasCompleteFile() {
		return hasFile() && _completeFile;
	}
	
	/**
	 * 
	 * @return the available ranges the alternate location has
	 */
	public IntervalSet getRanges() {
		return _ranges;
	}
	
	/**
	 * 
	 * @return any alternate locations this alternate location returned.
	 */
	public Set getAltLocs() {
		return _altLocs;
	}
	
	/**
	 * 
	 * @return the remote vendor as string
	 */
	public String getVendor() {
		return new String(_vendorId);
	}
}
	