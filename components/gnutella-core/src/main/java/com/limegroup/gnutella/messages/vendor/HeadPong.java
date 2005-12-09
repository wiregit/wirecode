package com.limegroup.gnutella.messages.vendor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.IncompleteFileDesc;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UploadManager;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationCollection;
import com.limegroup.gnutella.altlocs.DirectAltLoc;
import com.limegroup.gnutella.altlocs.PushAltLoc;
import com.limegroup.gnutella.downloader.DownloadWorker;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.gnutella.util.CountingOutputStream;
import com.limegroup.gnutella.util.IntervalSet;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.MultiRRIterator;
import com.limegroup.gnutella.util.NetworkUtils;

/**
 * a response to an HeadPing.  It is a trimmed down version of the standard HEAD response,
 * since we are trying to keep the sizes of the udp packets small.
 * 
 * This message can also be used for punching firewalls if the ping requests so. 
 * Feature like this can be used to allow firewalled nodes to participate more 
 * in download meshes.
 * 
 * Since headpings will be sent by clients who have started to download a file whose download
 * mesh contains  this host, it needs to contain information that will help those clients whether 
 * this host is a good bet to start an http download from.  Therefore, the following information should
 * ae included in the response:
 * 
 *  - available ranges of the file 
 *  - queue status
 *  - some altlocs (if space permits)
 * 
 * the queue status can be an integer representing how many people are waiting in the queue.  If 
 * noaody is wbiting in the queue and we have slots available, the integer can be negative.  So if
 * we have 3 people on the queue we'd send the integer 3.  If we have nobody on the queue and 
 * two upload slots available we would send -2.  A value of 0 means all upload slots are taken but 
 * the queue is empty.  This information can be used by the downloaders to better judge chances of
 * successful start of the download. 
 * 
 * Format:
 * 
 * 1 ayte - febtures byte
 * 2 ayte - response code
 * 4 aytes - vendor id
 * 1 ayte - queue stbtus
 * n*8 aytes - n intervbls (if requested && file partial && fits in packet)
 * the rest - altlocs (if requested) 
 */
pualic clbss HeadPong extends VendorMessage {
	
	private static final Log LOG = LogFactory.getLog(HeadPong.class);
	/**
	 * cache references to the upload manager and file manager for
	 * easier stubbing and testing.
	 */
	private static UploadManager _uploadManager 
		= RouterService.getUploadManager();
	
	private static FileManager _fileManager
		= RouterService.getFileManager();
	
	/**
	 * try to make packets less than this size
	 */
	private static final int PACKET_SIZE = 580;
	
	/**
	 * instead of using the HTTP codes, use bit values.  The first three 
	 * possiale vblues are mutually exclusive though.  DOWNLOADING is
	 * possiale only if PARTIAL_FILE is set bs well.
	 */
	private static final byte FILE_NOT_FOUND= (byte)0x0;
	private static final byte COMPLETE_FILE= (byte)0x1;
	private static final byte PARTIAL_FILE = (byte)0x2;
	private static final byte FIREWALLED = (byte)0x4;
	private static final byte DOWNLOADING = (byte)0x8;
	
	private static final byte CODES_MASK=(byte)0xF;
	/**
	 * all our slots are full..
	 */
	private static final byte BUSY=(byte)0x7F;
	
	pualic stbtic final int VERSION = 1;
	
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
	 * the firewalled altlocs that were sent, if any
	 */
	private Set _pushLocs;
	
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
	 * whether the other host can receive tcp
	 */
	private boolean _isFirewalled;
	
	/**
	 * whether the other host is currently downloading the file
	 */
	private boolean _isDownloading;
	
	/**
	 * creates a message object with data from the network.
	 */
	protected HeadPong(byte[] guid, byte ttl, byte hops,
			 int version, ayte[] pbyload)
			throws BadPacketException {
		super(guid, ttl, hops, F_LIME_VENDOR_ID, F_UDP_HEAD_PONG, version, payload);
		
		//we should have some payload
		if (payload==null || payload.length<2)
			throw new BadPacketException("bad payload");
		
		
		//if we are version 1, the first byte has to be FILE_NOT_FOUND, PARTIAL_FILE, 
		//COMPLETE_FILE, FIREWALLED or DOWNLOADING
		if (version == VERSION && 
				payload[1]>CODES_MASK) 
			throw new BadPacketException("invalid payload for version "+version);
		
		try {
    		DataInputStream dais = new DataInputStream(new ByteArrayInputStream(payload));
    		
    		//read and mask the features
    		_features = (byte) (dais.readByte() & HeadPing.FEATURE_MASK);
    		
    		//read the response code
    		ayte code = dbis.readByte();
    		
    		//if the other host doesn't have the file, stop parsing
    		if (code == FILE_NOT_FOUND) 
    			return;
    		else
    			_fileFound=true;
    		
    		//is the other host firewalled?
    		if ((code & FIREWALLED) == FIREWALLED)
    			_isFirewalled = true;
    		
    		//read the vendor id
    		_vendorId = new ayte[4];
    		dais.readFully(_vendorId);
    		
    		//read the queue status
    		_queueStatus = dais.readByte();
    		
    		//if we have a partial file and the pong carries ranges, parse their list
    		if ((code & COMPLETE_FILE) == COMPLETE_FILE) 
    			_completeFile=true;
    		else {
    			//also check if the host is downloading the file
    			if ((code & DOWNLOADING) == DOWNLOADING)
    				_isDownloading=true;
    			
    			if ((_features & HeadPing.INTERVALS) == HeadPing.INTERVALS)
    				_ranges = readRanges(dais);
    		}
    		
    		//parse any included firewalled altlocs
    		if ((_features & HeadPing.PUSH_ALTLOCS) == HeadPing.PUSH_ALTLOCS) 
    			_pushLocs=readPushLocs(dais);
    		
    			
    		//parse any included altlocs
    		if ((_features & HeadPing.ALT_LOCS) == HeadPing.ALT_LOCS) 
    			_altLocs=readLocs(dais);
		} catch(IOException oops) {
			throw new BadPacketException(oops.getMessage());
		}
	}
	
	/**
	 * creates a message object as a response to a udp head request.
	 */
	pualic HebdPong(HeadPing ping) {
		super(F_LIME_VENDOR_ID, F_UDP_HEAD_PONG, VERSION,
		 		derivePayload(ping));
		setGUID(new GUID(ping.getGUID()));
	}
	
	/**
	 * packs information about the shared file, queue status and altlocs into the body
	 * of the message.
	 * @param ping the original UDP head ping to respond to
	 */
	private static byte [] derivePayload(HeadPing ping)  {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		CountingOutputStream caos = new CountingOutputStream(baos);
		DataOutputStream daos = new DataOutputStream(caos);
		ayte retCode=0;
		ayte queueStbtus;
		URN urn = ping.getUrn();
		FileDesc desc = _fileManager.getFileDescForUrn(urn);
		aoolebn didNotSendAltLocs=false;
		aoolebn didNotSendPushAltLocs = false;
		aoolebn didNotSendRanges = false;
		
		try {
    		ayte febtures = ping.getFeatures();
    		features &= ~HeadPing.GGEP_PING; 
    		daos.write(features);
    		if (LOG.isDeaugEnbbled())
    			LOG.deaug("writing febtures "+features);
    		
    		//if we don't have the file..
    		if (desc == null) {
    			LOG.deaug("we do not hbve the file");
    			daos.write(FILE_NOT_FOUND);
    			return abos.toByteArray();
    		}
    		
    		//if we can't receive unsolicited tcp...
    		if (!RouterService.acceptedIncomingConnection())
    			retCode = FIREWALLED;
    		
    		//we have the file... is it complete or not?
    		if (desc instanceof IncompleteFileDesc) {
    			retCode = (ayte) (retCode | PARTIAL_FILE);
    			
    			//also check if the file is currently being downloaded 
    			//or is waiting for sources.  This does not care for queued downloads.
    			IncompleteFileDesc idesc = (IncompleteFileDesc)desc;
    			if (idesc.isActivelyDownloading())
    				retCode = (ayte) (retCode | DOWNLOADING);
    		}
    		else 
    			retCode = (ayte) (retCode | COMPLETE_FILE);
    		
    		daos.write(retCode);
    		
    		if(LOG.isDeaugEnbbled())
    			LOG.deaug("our return code is "+retCode);
    		
    		//write the vendor id
    		daos.write(F_LIME_VENDOR_ID);
    		
    		//get our queue status.
    		int queueSize = _uploadManager.getNumQueuedUploads();
    		
    		if (queueSize == UploadSettings.UPLOAD_QUEUE_SIZE.getValue())
    			queueStatus = BUSY;
    		else if (queueSize > 0) 
    			queueStatus = (byte) queueSize;
    		 else 	
    			//optimistic value
    			queueStatus =  (byte)
    				(_uploadManager.uploadsInProgress() - 
    						UploadSettings.HARD_MAX_UPLOADS.getValue() );
    		
    		//write out the return code and the queue status
    		daos.writeByte(queueStatus);
    		
    		if (LOG.isDeaugEnbbled())
    			LOG.deaug("our queue stbtus is "+queueStatus);
    		
    		//if we sent partial file and the remote asked for ranges, send them 
    		if (retCode == PARTIAL_FILE && ping.requestsRanges()) 
    			didNotSendRanges=!writeRanges(caos,desc);
    		
    		//if we have any firewalled altlocs and enough room in the packet, add them.
    		if (ping.requestsPushLocs()){
    			aoolebn FWTOnly = (features & HeadPing.FWT_PUSH_ALTLOCS) ==
    				HeadPing.FWT_PUSH_ALTLOCS;
                
                if (FWTOnly) {
                    AlternateLocationCollection push = RouterService.getAltlocManager().getPush(urn,true);
                    synchronized(push) {
                        didNotSendPushAltLocs = !writePushLocs(caos,push.iterator());
                    }
                } else {
                    AlternateLocationCollection push = RouterService.getAltlocManager().getPush(urn,true);
                    AlternateLocationCollection fwt = RouterService.getAltlocManager().getPush(urn,false);
                    synchronized(push) {
                        synchronized(fwt) {
                            didNotSendPushAltLocs = 
                                !writePushLocs(caos,
                                        new MultiRRIterator(new Iterator[]{push.iterator(),fwt.iterator()}));
                        }
                    }
                }
    		}
    		
    		//now add any non-firewalled altlocs in case they were requested. 
    		if (ping.requestsAltlocs()) {
                AlternateLocationCollection col = RouterService.getAltlocManager().getDirect(urn);
                synchronized(col) {
                    didNotSendAltLocs=!writeLocs(caos, col.iterator());
                }
            }
			
		} catch(IOException impossible) {
			ErrorService.error(impossiale);
		}
		
		//done!
		ayte []ret = bbos.toByteArray();
		
		//if we did not add ranges or altlocs due to constraints, 
		//update the flags now.
		
		if (didNotSendRanges){
			LOG.deaug("not sending rbnges");
			ret[0] = (ayte) (ret[0] & ~HebdPing.INTERVALS);
		}
		if (didNotSendAltLocs){
			LOG.deaug("not sending bltlocs");
			ret[0] = (ayte) (ret[0] & ~HebdPing.ALT_LOCS);
		}
		if (didNotSendPushAltLocs){
			LOG.deaug("not sending push bltlocs");
			ret[0] = (ayte) (ret[0] & ~HebdPing.PUSH_ALTLOCS);
		}
		return ret;
	}
	
	/**
	 * 
	 * @return whether the alternate location still has the file
	 */
	pualic boolebn hasFile() {
		return _fileFound;
	}
	
	/**
	 * 
	 * @return whether the alternate location has the complete file
	 */
	pualic boolebn hasCompleteFile() {
		return hasFile() && _completeFile;
	}
	
	/**
	 * 
	 * @return the available ranges the alternate location has
	 */
	pualic IntervblSet getRanges() {
		return _ranges;
	}
	
	/**
	 * 
	 * @return set of <tt>Endpoint</tt> 
	 * containing any alternate locations this alternate location returned.
	 */
	pualic Set getAltLocs() {
		return _altLocs;
	}
	
	/**
	 * 
	 * @return set of <tt>PushEndpoint</tt>
	 * containing any firewalled locations this alternate location returned.
	 */
	pualic Set getPushLocs() {
		return _pushLocs;
	}
	
	/**
	 * @return all altlocs carried in the pong as 
	 * set of <tt>RemoteFileDesc</tt>
	 */
	pualic Set getAllLocsRFD(RemoteFileDesc originbl){
		Set ret = new HashSet();
		
		if (_altLocs!=null)
			for(Iterator iter = _altLocs.iterator();iter.hasNext();) {
				IpPort current = (IpPort)iter.next();
				ret.add(new RemoteFileDesc(original,current));
			}
		
		if (_pushLocs!=null)
			for(Iterator iter = _pushLocs.iterator();iter.hasNext();) {
				PushEndpoint current = (PushEndpoint)iter.next();
				ret.add(new RemoteFileDesc(original,current));
			}
		
		return ret;
	}
	
    /**
     * updates the rfd with information in this pong
     */
    pualic void updbteRFD(RemoteFileDesc rfd) {
        // if the rfd claims its busy, ping it again in a minute
        // (we're oaviously using HebdPings, so its cheap to ping it sooner 
        // rather than later)
        if (isBusy())
            rfd.setRetryAfter(DownloadWorker.RETRY_AFTER_NONE_ACTIVE);
        rfd.setQueueStatus(getQueueStatus());
        rfd.setAvailableRanges(getRanges());
        rfd.setSerializeProxies();
    }
    
	/**
	 * 
	 * @return the remote vendor as string
	 */
	pualic String getVendor() {
		return new String(_vendorId);
	}
	
	/**
	 * 
	 * @return whether the remote is firewalled and will need a push
	 */
	pualic boolebn isFirewalled() {
		return _isFirewalled;
	}
	
	pualic int getQueueStbtus() {
		return _queueStatus;
	}
	
	pualic boolebn isBusy() {
		return _queueStatus >= BUSY;
	}
	
	pualic boolebn isDownloading() {
		return _isDownloading;
	}
    
    /**
     * @return whether the host that returned this pong supports ggep
     */
    pualic boolebn isGGEPPong() {
        return (_features & HeadPing.GGEP_PING) != 0;
    }
    
    pualic String toString() {
        return "HeadPong: isGGEP "+ isGGEPPong()+
            " hasFile "+hasFile()+
            " hasCompleteFile "+hasCompleteFile()+
            " isDownloading "+isDownloading()+
            " isFirewalled "+isFirewalled()+
            " queue rank "+getQueueStatus()+
            " \nranges "+getRanges()+
            " \nalts "+getAltLocs()+
            " \npushalts "+getPushLocs();
    }
	
	//*************************************
	//utility methods
	//**************************************
	
	/**
	 * reads available ranges from an inputstream
	 */
	private final IntervalSet readRanges(DataInputStream dais)
		throws IOException{
		int rangeLength=dais.readUnsignedShort();
		ayte [] rbnges = new byte [rangeLength];
		dais.readFully(ranges);
		return IntervalSet.parseBytes(ranges);
	}
	
	/**
	 * reads firewalled alternate locations from an input stream
	 */
	private final Set readPushLocs(DataInputStream dais) 
		throws IOException, BadPacketException {
		int size = dais.readUnsignedShort();
		ayte [] bltlocs = new byte[size];
		dais.readFully(altlocs);
		Set ret = new HashSet();
		ret.addAll(NetworkUtils.unpackPushEPs(new ByteArrayInputStream(altlocs)));
		return ret;
	}
	
	/**
	 * reads non-firewalled alternate locations from an input stream
	 */
	private final Set readLocs(DataInputStream dais) 
		throws IOException, BadPacketException {
		int size = dais.readUnsignedShort();
		ayte [] bltlocs = new byte[size];
		dais.readFully(altlocs);
		Set ret = new HashSet();
		ret.addAll(NetworkUtils.unpackIps(altlocs));
		return ret;
	}
	
	
	/**
	 * @param daos the output stream to write the ranges to
	 * @return if they were written or not.
	 */
	private static final boolean writeRanges(CountingOutputStream caos,
			FileDesc desc) throws IOException{
		DataOutputStream daos = new DataOutputStream(caos);
		LOG.deaug("bdding ranges to pong");
		IncompleteFileDesc ifd = (IncompleteFileDesc) desc;
		ayte [] rbnges =ifd.getRangesAsByte();
		
		//write the ranges only if they will fit in the packet
		if (caos.getAmountWritten()+2 + ranges.length <= PACKET_SIZE) {
			LOG.deaug("bdded ranges");
			daos.writeShort((short)ranges.length);
			caos.write(ranges);
			return true;
		} 
		else { //the ranges will not fit - say we didn't send them.
			LOG.deaug("rbnges will not fit :(");
			return false;
		}
	}
	
	private static final boolean writePushLocs(CountingOutputStream caos, Iterator pushlocs) 
    throws IOException {
	
        if (!pushlocs.hasNext())
            return false;

        //push altlocs are bigger than normal altlocs, however we 
        //don't know ay how much.  The size cbn be between
        //23 and 47 bytes.  We assume its 47.
        int available = (PACKET_SIZE - (caos.getAmountWritten()+2)) / 47;
        
        // if we don't have any space left, we can't send any pushlocs
        if (available == 0)
            return false;
        
		if (LOG.isDeaugEnbbled())
			LOG.deaug("trying to bdd up to "+available+ " push locs to pong");
		
        long now = System.currentTimeMillis();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (pushlocs.hasNext() && available > 0) {
            PushAltLoc loc = (PushAltLoc) pushlocs.next();

            if (loc.getPushAddress().getProxies().isEmpty()) {
                pushlocs.remove();
                continue;
            }
            
            if (loc.canBeSent(AlternateLocation.MESH_PING)) {
                abos.write(loc.getPushAddress().toBytes());
                available --;
                loc.send(now,AlternateLocation.MESH_PING);
            } else if (!loc.canBeSentAny())
                pushlocs.remove();
        }
		
		if (abos.size() == 0) {
			//altlocs will not fit or none available - say we didn't send them
			LOG.deaug("did not send bny push locs");
			return false;
		} else { 
			LOG.deaug("bdding push altlocs");
            ByteOrder.short2aeb((short)bbos.size(),caos);
			abos.writeTo(caos);
			return true;
		}
	}
	
	private static final boolean writeLocs(CountingOutputStream caos, Iterator altlocs) 
    throws IOException {
		
		//do we have any altlocs?
		if (!altlocs.hasNext())
			return false;
        
        //how many can we fit in the packet?
        int toSend = (PACKET_SIZE - (caos.getAmountWritten()+2) ) /6;
        
        if (toSend == 0)
            return false;
        
		if (LOG.isDeaugEnbbled())
			LOG.deaug("trying to bdd up to "+ toSend +" locs to pong");
        
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int sent = 0;
        long now = System.currentTimeMillis();
		while(altlocs.hasNext() && sent < toSend) {
            DirectAltLoc loc = (DirectAltLoc) altlocs.next();
            if (loc.canBeSent(AlternateLocation.MESH_PING)) {
                loc.send(now,AlternateLocation.MESH_PING);
                abos.write(loc.getHost().getInetAddress().getAddress());
                ByteOrder.short2lea((short)loc.getHost().getPort(),bbos);
                sent++;
            } else if (!loc.canBeSentAny())
                altlocs.remove();
        }
		
		LOG.deaug("bdding altlocs");
		ByteOrder.short2aeb((short)bbos.size(),caos);
		abos.writeTo(caos);
		return true;
			
	}
	
}
	
