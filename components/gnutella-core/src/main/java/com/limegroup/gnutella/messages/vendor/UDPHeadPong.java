
package com.limegroup.gnutella.messages.vendor;

import java.io.*;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.altlocs.AlternateLocationCollection;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.gnutella.util.*;

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
	
	public static final int VERSION = 1;
	
	/**
	 * available ranges
	 */
	private IntervalSet _ranges;
	
	/**
	 * the altlocs that were sent, if any
	 */
	private AlternateLocationCollection _altLocs;
	
	/**
	 * the queue status, can be negative
	 */
	private int _queueStatus;
	
	/**
	 * whether the other host has the file at all
	 */
	private boolean _fileFound,_completeFile;
	
	/**
	 * creates a message object with data from the network.
	 */
	protected UDPHeadPong(byte[] guid, byte ttl, byte hops,
			 int version, byte[] payload)
			throws BadPacketException {
		super(guid, ttl, hops, F_LIME_VENDOR_ID, F_UDP_HEAD_PONG, version, payload);
		
		//we should have some payload
		if (payload==null || payload.length==0)
			throw new BadPacketException("empty payload");
		
		//if we are version 1, the first byte has to be FILE_NOT_FOUND, PARTIAL_FILE 
		//or COMPLETE_FILE
		if (version == VERSION && 
				payload[0]!=FILE_NOT_FOUND &&
				payload[0]!=PARTIAL_FILE &&
				payload[0]!=COMPLETE_FILE)
			throw new BadPacketException("invalid payload for version "+version);
		
		try {
			
		
		DataInputStream dais = new DataInputStream(new ByteArrayInputStream(payload));
		
		//read the response code
		byte code = dais.readByte();
		
		//if the other host doesn't have the file, stop parsing
		if (code == FILE_NOT_FOUND) 
			return;
		else
			_fileFound=true;
		
		//read the queue status
		_queueStatus = dais.readByte();
		
		//the queue status can be negative.. check the msb
		if (_queueStatus > 127)
			_queueStatus = 255 - _queueStatus;
		
		//if we have a partial file, parse the list of ranges
		if (code == COMPLETE_FILE) 
			_completeFile=true;
		else{
			short rangeLength=dais.readShort();
			byte [] ranges = new byte [rangeLength];
			dais.readFully(ranges);
			_ranges = IntervalSet.parseBytes(ranges);
		}
		
		//if there is more data in the packet, it must be alternate locations.
		if (dais.available() > 0) {
			byte [] altlocs = new byte[dais.available()];
			dais.readFully(altlocs);
			_altLocs = 
				AlternateLocationCollection.createCollectionFromHttpValue(new String(altlocs));
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
		
		//get our queue status.
		int queueSize = RouterService.getUploadManager().getNumQueuedUploads();
		
		if (queueSize > 0) 
			queueStatus = (byte) queueSize;
		 else 	
			//optimistic value
			queueStatus =  (byte)
				(RouterService.getUploadManager().uploadsInProgress() - 
						UploadSettings.HARD_MAX_UPLOADS.getValue() );
		
		
		//write out the return code and the queue status
		caos.write(queueStatus);
		
		//if we sent partial file, we need to send the available ranges
		if (retCode == PARTIAL_FILE) {
			IncompleteFileDesc ifd = (IncompleteFileDesc) desc;
			byte [] ranges =ifd.getRangesAsByte(); 
			caos.write((short)ranges.length);
			caos.write(ranges);
		}
		
		//if we have any altlocs and enough room in the packet, add them.
		AlternateLocationCollection altlocs = desc.getAlternateLocationCollection();
		
		if (altlocs!= null && altlocs.hasAlternateLocations()) {
			byte [] altbytes = altlocs.httpStringValue().getBytes();
			
			if (caos.getAmountWritten() + altbytes.length <= PACKET_SIZE)
				caos.write(altbytes);
				
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
	public AlternateLocationCollection getAltLocs() {
		return _altLocs;
	}
}
	