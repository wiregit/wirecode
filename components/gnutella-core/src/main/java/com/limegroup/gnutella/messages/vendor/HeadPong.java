package com.limegroup.gnutella.messages.vendor;



import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.IntervalSet;
import org.limewire.collection.MultiRRIterator;
import org.limewire.io.CountingOutputStream;
import org.limewire.io.InvalidDataException;
import org.limewire.io.IpPort;
import org.limewire.io.NetworkUtils;
import org.limewire.service.ErrorService;
import org.limewire.util.ByteOrder;

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
 * the rest - altlocs (if requested) 
 */
public class HeadPong extends VendorMessage {
	
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
	 * possible values are mutually exclusive though.  DOWNLOADING is
	 * possible only if PARTIAL_FILE is set as well.
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
	private Set<IpPort> _altLocs;
	
	/**
	 * the firewalled altlocs that were sent, if any
	 */
	private Set<PushEndpoint> _pushLocs;
	
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
			 int version, byte[] payload)
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
    		byte code = dais.readByte();
    		
    		//if the other host doesn't have the file, stop parsing
    		if (code == FILE_NOT_FOUND) 
    			return;
    		else
    			_fileFound=true;
    		
    		//is the other host firewalled?
    		if ((code & FIREWALLED) == FIREWALLED)
    			_isFirewalled = true;
    		
    		//read the vendor id
    		_vendorId = new byte[4];
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
    			_altLocs= readLocs(dais);
		} catch(IOException oops) {
			throw new BadPacketException(oops);
		}
	}
	
	/**
	 * creates a message object as a response to a udp head request.
	 */
	public HeadPong(HeadPing ping) {
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
		byte retCode=0;
		byte queueStatus;
		URN urn = ping.getUrn();
		FileDesc desc = _fileManager.getFileDescForUrn(urn);
		boolean didNotSendAltLocs=false;
		boolean didNotSendPushAltLocs = false;
		boolean didNotSendRanges = false;
		
		try {
    		byte features = ping.getFeatures();
    		features &= ~HeadPing.GGEP_PING; 
    		daos.write(features);
    		if (LOG.isDebugEnabled())
    			LOG.debug("writing features "+features);
    		
    		//if we don't have the file..
    		if (desc == null) {
    			LOG.debug("we do not have the file");
    			daos.write(FILE_NOT_FOUND);
    			return baos.toByteArray();
    		}
    		
    		//if we can't receive unsolicited tcp...
    		if (!RouterService.acceptedIncomingConnection())
    			retCode = FIREWALLED;
    		
    		//we have the file... is it complete or not?
    		if (desc instanceof IncompleteFileDesc) {
    			retCode = (byte) (retCode | PARTIAL_FILE);
    			
    			//also check if the file is currently being downloaded 
    			//or is waiting for sources.  This does not care for queued downloads.
    			IncompleteFileDesc idesc = (IncompleteFileDesc)desc;
    			if (idesc.isActivelyDownloading())
    				retCode = (byte) (retCode | DOWNLOADING);
    		}
    		else 
    			retCode = (byte) (retCode | COMPLETE_FILE);
    		
    		daos.write(retCode);
    		
    		if(LOG.isDebugEnabled())
    			LOG.debug("our return code is "+retCode);
    		
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
    		
    		if (LOG.isDebugEnabled())
    			LOG.debug("our queue status is "+queueStatus);
    		
    		//if we sent partial file and the remote asked for ranges, send them 
    		if (retCode == PARTIAL_FILE && ping.requestsRanges()) 
    			didNotSendRanges=!writeRanges(caos,desc);
    		
    		//if we have any firewalled altlocs and enough room in the packet, add them.
    		if (ping.requestsPushLocs()){
    			boolean FWTOnly = (features & HeadPing.FWT_PUSH_ALTLOCS) ==
    				HeadPing.FWT_PUSH_ALTLOCS;
                
                if (FWTOnly) {
                    AlternateLocationCollection<PushAltLoc> push = RouterService.getAltlocManager().getPush(urn,true);
                    synchronized(push) {
                        didNotSendPushAltLocs = !writePushLocs(caos,push.iterator());
                    }
                } else {
                    AlternateLocationCollection<PushAltLoc> push = RouterService.getAltlocManager().getPush(urn,false);
                    AlternateLocationCollection<PushAltLoc> fwt = RouterService.getAltlocManager().getPush(urn,true);
                    synchronized(push) {
                        synchronized(fwt) {
                            didNotSendPushAltLocs = 
                                !writePushLocs(caos,
                                        new MultiRRIterator<PushAltLoc>(push.iterator(),fwt.iterator()));
                        }
                    }
                }
    		}
    		
    		//now add any non-firewalled altlocs in case they were requested. 
    		if (ping.requestsAltlocs()) {
                AlternateLocationCollection<DirectAltLoc> col = RouterService.getAltlocManager().getDirect(urn);
                synchronized(col) {
                    didNotSendAltLocs=!writeLocs(caos, col.iterator());
                }
            }
			
		} catch(IOException impossible) {
			ErrorService.error(impossible);
		}
		
		//done!
		byte []ret = baos.toByteArray();
		
		//if we did not add ranges or altlocs due to constraints, 
		//update the flags now.
		
		if (didNotSendRanges){
			LOG.debug("not sending ranges");
			ret[0] = (byte) (ret[0] & ~HeadPing.INTERVALS);
		}
		if (didNotSendAltLocs){
			LOG.debug("not sending altlocs");
			ret[0] = (byte) (ret[0] & ~HeadPing.ALT_LOCS);
		}
		if (didNotSendPushAltLocs){
			LOG.debug("not sending push altlocs");
			ret[0] = (byte) (ret[0] & ~HeadPing.PUSH_ALTLOCS);
		}
		return ret;
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
	 * @return set of <tt>Endpoint</tt> 
	 * containing any alternate locations this alternate location returned.
	 */
	public Set<IpPort> getAltLocs() {
		return _altLocs;
	}
	
	/**
	 * 
	 * @return set of <tt>PushEndpoint</tt>
	 * containing any firewalled locations this alternate location returned.
	 */
	public Set<PushEndpoint> getPushLocs() {
		return _pushLocs;
	}
	
	/**
	 * @return all altlocs carried in the pong as 
	 * set of <tt>RemoteFileDesc</tt>
	 */
	public Set<RemoteFileDesc> getAllLocsRFD(RemoteFileDesc original){
		Set<RemoteFileDesc> ret = new HashSet<RemoteFileDesc>();
		
		if (_altLocs!=null) {
            for(IpPort current : _altLocs)
				ret.add(new RemoteFileDesc(original,current));
        }
        
		if (_pushLocs!=null) {
            for(PushEndpoint current : _pushLocs)
				ret.add(new RemoteFileDesc(original,current));
        }
		
		return ret;
	}
	
    /**
     * updates the rfd with information in this pong
     */
    public void updateRFD(RemoteFileDesc rfd) {
        // if the rfd claims its busy, ping it again in a minute
        // (we're obviously using HeadPings, so its cheap to ping it sooner 
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
	public String getVendor() {
		return new String(_vendorId);
	}
	
	/**
	 * 
	 * @return whether the remote is firewalled and will need a push
	 */
	public boolean isFirewalled() {
		return _isFirewalled;
	}
	
	public int getQueueStatus() {
		return _queueStatus;
	}
	
	public boolean isBusy() {
		return _queueStatus >= BUSY;
	}
	
	public boolean isDownloading() {
		return _isDownloading;
	}
    
    /**
     * @return whether the host that returned this pong supports ggep
     */
    public boolean isGGEPPong() {
        return (_features & HeadPing.GGEP_PING) != 0;
    }
    
    public String toString() {
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
		byte [] ranges = new byte [rangeLength];
		dais.readFully(ranges);
		return IntervalSet.parseBytes(ranges);
	}
	
	/**
	 * reads firewalled alternate locations from an input stream
	 */
	private final Set<PushEndpoint> readPushLocs(DataInputStream dais) 
		throws IOException, BadPacketException {
		int size = dais.readUnsignedShort();
		byte [] altlocs = new byte[size];
		dais.readFully(altlocs);
		Set<PushEndpoint> ret = new HashSet<PushEndpoint>();
		ret.addAll(unpackPushEPs(new ByteArrayInputStream(altlocs)));
		return ret;
	}

    private static List<PushEndpoint> unpackPushEPs(InputStream is)
      throws BadPacketException, IOException {
        List<PushEndpoint> ret = new LinkedList<PushEndpoint>();
        DataInputStream dais = new DataInputStream(is);
        while (dais.available() > 0) 
            ret.add(PushEndpoint.fromBytes(dais));
        
        return Collections.unmodifiableList(ret);
    }
    
	
	/**
	 * reads non-firewalled alternate locations from an input stream
	 */
	private final Set<IpPort> readLocs(DataInputStream dais) 
      throws IOException, BadPacketException {
		int size = dais.readUnsignedShort();
		byte [] altlocs = new byte[size];
		dais.readFully(altlocs);
		Set<IpPort> ret = new HashSet<IpPort>();
        try {
            ret.addAll(NetworkUtils.unpackIps(altlocs));
        } catch(InvalidDataException ide) {
            throw new BadPacketException(ide);
        }
		return ret;
	}
	
	
	/**
	 * @param daos the output stream to write the ranges to
	 * @return if they were written or not.
	 */
	private static final boolean writeRanges(CountingOutputStream caos,
			FileDesc desc) throws IOException{
		DataOutputStream daos = new DataOutputStream(caos);
		LOG.debug("adding ranges to pong");
		IncompleteFileDesc ifd = (IncompleteFileDesc) desc;
		byte [] ranges =ifd.getRangesAsByte();
		
		//write the ranges only if they will fit in the packet
		if (caos.getAmountWritten()+2 + ranges.length <= PACKET_SIZE) {
			LOG.debug("added ranges");
			daos.writeShort((short)ranges.length);
			caos.write(ranges);
			return true;
		} 
		else { //the ranges will not fit - say we didn't send them.
			LOG.debug("ranges will not fit :(");
			return false;
		}
	}
	
	private static final boolean writePushLocs(CountingOutputStream caos, Iterator<PushAltLoc> pushlocs) 
    throws IOException {
	
        if (!pushlocs.hasNext())
            return false;

        //push altlocs are bigger than normal altlocs, however we 
        //don't know by how much.  The size can be between
        //23 and 47 bytes.  We assume its 47.
        int available = (PACKET_SIZE - (caos.getAmountWritten()+2)) / 47;
        
        // if we don't have any space left, we can't send any pushlocs
        if (available == 0)
            return false;
        
		if (LOG.isDebugEnabled())
			LOG.debug("trying to add up to "+available+ " push locs to pong");
		
        long now = System.currentTimeMillis();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (pushlocs.hasNext() && available > 0) {
            PushAltLoc loc = pushlocs.next();

            if (loc.getPushAddress().getProxies().isEmpty()) {
                pushlocs.remove();
                continue;
            }
            
            if (loc.canBeSent(AlternateLocation.MESH_PING)) {
                baos.write(loc.getPushAddress().toBytes());
                available --;
                loc.send(now,AlternateLocation.MESH_PING);
            } else if (!loc.canBeSentAny())
                pushlocs.remove();
        }
		
		if (baos.size() == 0) {
			//altlocs will not fit or none available - say we didn't send them
			LOG.debug("did not send any push locs");
			return false;
		} else { 
			LOG.debug("adding push altlocs");
            ByteOrder.short2beb((short)baos.size(),caos);
			baos.writeTo(caos);
			return true;
		}
	}
	
	private static final boolean writeLocs(CountingOutputStream caos, Iterator<DirectAltLoc> altlocs) 
    throws IOException {
		
		//do we have any altlocs?
		if (!altlocs.hasNext())
			return false;
        
        //how many can we fit in the packet?
        int toSend = (PACKET_SIZE - (caos.getAmountWritten()+2) ) /6;
        
        if (toSend == 0)
            return false;
        
		if (LOG.isDebugEnabled())
			LOG.debug("trying to add up to "+ toSend +" locs to pong");
        
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int sent = 0;
        long now = System.currentTimeMillis();
		while(altlocs.hasNext() && sent < toSend) {
            DirectAltLoc loc = altlocs.next();
            if (loc.canBeSent(AlternateLocation.MESH_PING)) {
                loc.send(now,AlternateLocation.MESH_PING);
                baos.write(loc.getHost().getInetAddress().getAddress());
                ByteOrder.short2leb((short)loc.getHost().getPort(),baos);
                sent++;
            } else if (!loc.canBeSentAny())
                altlocs.remove();
        }
		
		LOG.debug("adding altlocs");
		ByteOrder.short2beb((short)baos.size(),caos);
		baos.writeTo(caos);
		return true;
			
	}
	
}
	
