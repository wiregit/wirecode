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
import com.limegroup.gnutella.altlocs.AltLocDigest;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationCollection;
import com.limegroup.gnutella.altlocs.DirectAltLoc;
import com.limegroup.gnutella.altlocs.PushAltLoc;
import com.limegroup.gnutella.messages.BadGGEPBlockException;
import com.limegroup.gnutella.messages.BadGGEPPropertyException;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.CountingGGEP;
import com.limegroup.gnutella.messages.GGEP;
import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.gnutella.util.CountingOutputStream;
import com.limegroup.gnutella.util.IntervalSet;
import com.limegroup.gnutella.util.IpPort;
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
 * 
 * Legacy format: 
 * n*8 bytes - n intervals (if requested && file partial && fits in packet)
 * the rest - altlocs and pushlocs (if requested)
 * 
 * Curent format:
 * One big GGEP block.  We do not reply to pings that do not support GGEP, and only existing
 * LimeWire clients will send pongs in the legacy format.
 * 
 * Format for ranges: 
 * if bit 0 of byte 0 is set, then the rest of the field is in the list of 32-bit ranges format.
 * (we don't parse any other format at present).
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
	
	///// some constants related to keeping the size of the 
	///// pong within non-defragmentable limits
	
	/**
	 * try to make packets less than this size
	 */
	private static final int PACKET_SIZE = 580;
	
	/**
	 * how well do we think ranges will compress
	 */
	private static final float RANGE_COMPRESSION = 0.6f;
	
	/**
	 * how well do we think altlocs will compress
	 */
	private static final float ALTLOC_COMPRESSION = 0.7f;
	
	/**
	 * how well do we think pushlocs will compress
	 */
	private static final float PUSHLOC_COMPRESSION = 0.8f;
	
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
	
	public static final int VERSION = 2;
	
	/**
	 * the features contained in this pong.  Same as those of the originating ping
	 */
	private byte _features;
	
	/**
	 * the features field of the ggep block.  For now we parse only 4 bytes
	 */
	private int _ggepFeatures;
	
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
	 * the ggep block contained in this pong
	 */
	private CountingGGEP _ggep;
	
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
    		
    		if (version == VERSION && (_features & HeadPing.GGEP_PING)!= HeadPing.GGEP_PING)
    		    throw new BadPacketException("pong version 2 that is not GGEP??");
    		
    		//read the response code
    		byte code = dais.readByte();
    		
    		//if the other host doesn't have the file, stop parsing
    		if (code == FILE_NOT_FOUND) 
    			return;
    		else
    			_fileFound=true;
    		
    		//is the other host firewalled?
    		if ((code & FIREWALLED) == FIREWALLED)
    			_isFirewalled=true;
    		
    		//read the vendor id
    		_vendorId = new byte[4];
    		dais.readFully(_vendorId);
    		
    		//read the queue status
    		_queueStatus = dais.readByte();

    		if ((code & COMPLETE_FILE) == COMPLETE_FILE) 
		        _completeFile=true;
    		
    	    //check if the host is downloading the file
    	    if ((code & DOWNLOADING) == DOWNLOADING)
    	        _isDownloading=true;
    		
    		// if this is a new pong, parse it as a GGEP map
    		if (version == VERSION) 
    		    parseGGEPPong(payload);
    		else 
    		    parseLegacyPong(code, dais);
    		
		} catch(IOException oops) {
			throw new BadPacketException(oops.getMessage());
		} catch (BadGGEPBlockException oops2) {oops2.getMessage();
		    throw new BadPacketException(oops2.getMessage());
		} catch (BadGGEPPropertyException oops3) {
		    throw new BadPacketException(oops3.getMessage());
		}
	}

	/**
	 * parses the ggep block contained in this pong, and reads which features are available
	 * @param payload
	 * @throws BadGGEPBlockException
	 */
	private void parseGGEPPong(byte [] payload) 
		throws BadGGEPBlockException, BadGGEPPropertyException, BadPacketException {
	    
	    _ggep = new CountingGGEP(payload,7);
	    byte [] props = _ggep.get(GGEPHeadConstants.GGEP_PROPS);
	    if (props.length < 1)
	        throw new BadPacketException("invalid properties field");
	    
	    _ggepFeatures = props[props.length -1]; // eventually parse other bytes too.
	}
	
	private void parseLegacyPong(byte code, DataInputStream dais) 
		throws IOException, BadPacketException {
	    
	    if ((_features & HeadPing.INTERVALS) == HeadPing.INTERVALS)
	        _ranges = read32bitRanges(dais);
	    
	    //parse any included firewalled altlocs
	    if ((_features & HeadPing.PUSH_ALTLOCS) == HeadPing.PUSH_ALTLOCS) 
	        _pushLocs=readPushLocs(dais);
	    
	    //parse any included altlocs
	    if ((_features & HeadPing.ALT_LOCS) == HeadPing.ALT_LOCS) 
	        _altLocs=readLocs(dais);
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
		
		try {
    		byte features = ping.getFeatures();
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
    		
    		//create the GGEP block
    		CountingGGEP ggep = new CountingGGEP(true);
    		
    		//put in our supported features with metadata
    		GGEPHeadConstants.addDefaultGGEPProperties(ggep);
    		
    		//if the ping requests ranges, add them
    		if (ping.requestsRanges() && desc instanceof IncompleteFileDesc)
    		    writeRanges(ggep,desc);
    		//if the ping requests pushlocs, add them
    		if (ping.requestsPushLocs())
    		    writePushLocs(ggep,desc,ping);
    		//if the ping requests altlocs, add them
    		if (ping.requestsAltlocs())
    		    writeLocs(ggep,desc,ping);
    		
    		// write the ggep block out
    		ggep.write(daos);
			
		} catch(IOException impossible) {
			ErrorService.error(impossible);
		}
		
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
	 * @return the available ranges the alternate location has
	 */
	public IntervalSet getRanges() {
	    if (_ranges == null)
	        readRanges();
	    return _ranges;
	
	}
	
	/**
	 * extracts the ranges from the GGEP block.
	 * if the HeadPong is in legacy format, ranges are already parsed.
	 */
	private void readRanges() {
	    if (_ggep == null)
	        return;

	    if ((_ggepFeatures & GGEPHeadConstants.RANGES) != GGEPHeadConstants.RANGES)
	        return;

	    try {
	        byte [] ranges = _ggep.getBytes((char)GGEPHeadConstants.RANGES+GGEPHeadConstants.DATA);
	        // for now we parse only 32 bit range lists
	        if ((ranges[0] & GGEPHeadConstants.RANGE_LIST) == GGEPHeadConstants.RANGE_LIST) {
	            if ((ranges[0] & GGEPHeadConstants.LONG_RANGES) == 0) {
	                ByteArrayInputStream bais = new ByteArrayInputStream(ranges,1,ranges.length -1);
	                _ranges = read32bitRanges(new DataInputStream(bais));
	            }
	            // else parse a 64 bit range list
	        }
	        // else parse a bitset format.
	    }catch(BadGGEPPropertyException bad) {}
	    catch(IOException bad) {}
	}
	
	/**
	 * @return set of <tt>Endpoint</tt> 
	 * containing any alternate locations this alternate location returned.
	 */
	public Set getAltLocs() {
	    if (_altLocs == null)
	        readAltLocs();
		return _altLocs;
	}
	
	/**
	 * extracts the altlocs from the GGEP block.
	 * if the HeadPong is in legacy format, the altlocs are already parsed.
	 */
	private void readAltLocs() {
	    if (_ggep == null)
	        return;

	    if ((_ggepFeatures & GGEPHeadConstants.ALTLOCS) != GGEPHeadConstants.ALTLOCS)
	        return;

	    try {
	        byte [] altlocs = _ggep.getBytes((char)GGEPHeadConstants.ALTLOCS+GGEPHeadConstants.DATA);
	        ByteArrayInputStream bais = new ByteArrayInputStream(altlocs);
	        _altLocs = readLocs(new DataInputStream(bais));
	    } catch (BadGGEPPropertyException bad){}
	    catch(BadPacketException bad){}
	    catch(IOException bad){}
	}
	
	/**
	 * 
	 * @return set of <tt>PushEndpoint</tt>
	 * containing any firewalled locations this alternate location returned.
	 */
	public Set getPushLocs() {
	    if (_pushLocs == null)
	        readPushLocs();
		return _pushLocs;
	}
	
	/**
	 * extracts the pushlocs from the GGEP block.
	 * if the HeadPong is in legacy format, the pushlocs are already parsed.
	 */
	private void readPushLocs() {
	    if (_ggep == null)
	        return;
	    if ((_ggepFeatures & GGEPHeadConstants.PUSHLOCS) != GGEPHeadConstants.PUSHLOCS)
	        return;
	    try {
	        byte [] pushlocs = _ggep.getBytes((char)GGEPHeadConstants.PUSHLOCS+GGEPHeadConstants.DATA);
	        ByteArrayInputStream bais = new ByteArrayInputStream(pushlocs);
	        _pushLocs = readPushLocs(new DataInputStream(bais));
	    } catch (BadGGEPPropertyException bad){}
	    catch(BadPacketException bad){}
	    catch(IOException bad){}
	}
	
	/**
	 * @return all altlocs carried in the pong as 
	 * set of <tt>RemoteFileDesc</tt>
	 */
	public Set getAllLocsRFD(RemoteFileDesc original){
		Set ret = new HashSet();
		
		if (getAltLocs() != null)
			for(Iterator iter = _altLocs.iterator();iter.hasNext();) {
				IpPort current = (IpPort)iter.next();
				ret.add(new RemoteFileDesc(original,current));
			}
		
		if (getPushLocs() != null)
			for(Iterator iter = _pushLocs.iterator();iter.hasNext();) {
				PushEndpoint current = (PushEndpoint)iter.next();
				ret.add(new RemoteFileDesc(original,current));
			}
		
		return ret;
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
	 * @return how many direct or push altlocs collided on the filter the host is replying to
	 */
	public int getSkippedLocs(boolean direct) {
	    return -1;
	}
	
	/**
	 * @return how many direct or push altlocs total the other host knows about
	 */
	public int getLeftLocs(boolean direct) {
	    return -1;
	}
	
	/**
	 * @return whether the host will understand an altloc digest sent to it. 
	 */
	public boolean supportsDigests() {
	    return true;
	}
	
	//*************************************
	//utility methods
	//**************************************
	
	/**
	 * reads available ranges from an inputstream
	 */
	private final IntervalSet read32bitRanges(DataInputStream dais)
		throws IOException{
		int rangeLength=dais.readUnsignedShort();
		byte [] ranges = new byte [rangeLength];
		dais.readFully(ranges);
		return IntervalSet.parseBytes(ranges);
	}
	
	/**
	 * reads firewalled alternate locations from an input stream
	 */
	private final Set readPushLocs(DataInputStream dais) 
		throws IOException, BadPacketException {
		int size = dais.readUnsignedShort();
		byte [] altlocs = new byte[size];
		dais.readFully(altlocs);
		Set ret = new HashSet();
		ret.addAll(NetworkUtils.unpackPushEPs(altlocs));
		return ret;
	}
	
	/**
	 * reads non-firewalled alternate locations from an input stream
	 */
	private final Set readLocs(DataInputStream dais) 
		throws IOException, BadPacketException {
		int size = dais.readUnsignedShort();
		byte [] altlocs = new byte[size];
		dais.readFully(altlocs);
		Set ret = new HashSet();
		ret.addAll(NetworkUtils.unpackIps(altlocs));
		return ret;
	}
	
	private static final void writeRanges(CountingGGEP dest, FileDesc desc){
	    LOG.debug("adding ranges to pong");
		IncompleteFileDesc ifd = (IncompleteFileDesc) desc;
		byte [] ranges =ifd.getRangesAsByte();
		
		// if we estimate we won't be able to fit the ranges, don't put them in.
		if (dest.getEstimatedSize()+3+ ranges.length*RANGE_COMPRESSION > PACKET_SIZE)
		    return;
		
		// write out the format in the first byte
		byte []toZip = new byte[ranges.length+3];
		toZip[0] = GGEPHeadConstants.RANGE_LIST;
		ByteOrder.short2beb((short)ranges.length,toZip,1);
		System.arraycopy(ranges,0,toZip,3,ranges.length);
		dest.putAndCompress((char)GGEPHeadConstants.RANGES + GGEPHeadConstants.DATA,toZip);
	}
	
	
	private static final void writeLocs(CountingGGEP dest, FileDesc desc, HeadPing ping) {
	    AlternateLocationCollection altlocs = desc.getAlternateLocationCollection();
		//do we have any altlocs?
		if (altlocs==null)
			return;
		
		// the total number of altlocs we know about
	    short total = (short)altlocs.getAltLocsSize();
	    
	    // how many altlocs we will have filtered, if any
	    short filtered = 0;
	    
	    // estimate up to how many altlocs we can fit.
	    int toPack = (int)((PACKET_SIZE - dest.getEstimatedSize() ) / (ALTLOC_COMPRESSION * 6));
	    
	    // nothing will fit - too bad.
	    if (toPack == 0)
	        return;
	    if (LOG.isDebugEnabled())
			LOG.debug("trying to add up to "+toPack+"locs to pong");
	    
	    // see if the other side sent a filter
	    AltLocDigest digest = ping.getDigest();

	    // if we have a filter, filter the altlocs
	    if (digest != null || toPack < altlocs.getAltLocsSize()) {
	        AlternateLocationCollection temp = AlternateLocationCollection.create(altlocs.getSHA1Urn());
	        int sent =0;
	        for (Iterator iter = altlocs.iterator();iter.hasNext() && sent < toPack;) {
	            AlternateLocation loc = (AlternateLocation)iter.next();
	            if (loc instanceof PushAltLoc)
	                    continue;
	            
	            if (digest != null && digest.contains(loc)) {
	                filtered++;
	                continue;
	            }
	            temp.add(loc.createClone());
	            sent++;
	        }
	        altlocs = temp;
	    }
	    
	    // if after filtering / trimming we decided not to send anything, return.
	    if (altlocs.getAltLocsSize() == 0)
	        return;
	    
	    // put the response in the ggep
	    byte [] alts = altlocs.toBytes(toPack);
	    byte [] tmp = new byte[alts.length+2];
	    ByteOrder.short2beb((short)alts.length,tmp,0);
	    System.arraycopy(alts,0,tmp,2,alts.length);
	    dest.putAndCompress((char)GGEPHeadConstants.ALTLOCS+GGEPHeadConstants.DATA,tmp);
	    
	    // if the other side indicated support for statistics, add those too
	    byte [] pingFeatures = ping._ggep.get(GGEPHeadConstants.GGEP_PROPS);
	    if ((pingFeatures[0] & GGEPHeadConstants.ALT_MESH_STAT) 
	            == GGEPHeadConstants.ALT_MESH_STAT) {
	        byte [] stats = new byte[4];
	        ByteOrder.short2leb(total,stats,0);
	        ByteOrder.short2leb(filtered,stats,2);
	        dest.put((char)GGEPHeadConstants.ALT_MESH_STAT+GGEPHeadConstants.DATA,stats);
	    }
	}
	
	private static final void writePushLocs(CountingGGEP dest, FileDesc desc, HeadPing ping) {
	    AlternateLocationCollection altlocs = desc.getPushAlternateLocationCollection();
	    
		//do we have any altlocs?
		if (altlocs==null)
			return;
		
		// the total number of altlocs we know about
	    short total = (short)altlocs.getAltLocsSize();
	    
	    // how many altlocs we will have filtered, if any
	    short filtered = 0;
	    
	    // estimate up to how many altlocs we can fit.
	    int toPack = (int)((PACKET_SIZE - dest.getEstimatedSize() ) / (PUSHLOC_COMPRESSION * 47));
	    
	    // nothing will fit - too bad.
	    if (toPack == 0)
	        return;
	    if (LOG.isDebugEnabled())
			LOG.debug("trying to add up to "+toPack+"locs to pong");
	    
	    // see if the other side sent a filter
	    AltLocDigest digest = ping.getPushDigest();

	    // if we have a filter, filter the altlocs
	    if (digest != null || ping.requestsFWTPushLocs() || toPack < altlocs.getAltLocsSize()) {
	        AlternateLocationCollection temp = AlternateLocationCollection.create(altlocs.getSHA1Urn());
	        int sent =0;
	        for (Iterator iter = altlocs.iterator();iter.hasNext() && sent < toPack;) {
	            AlternateLocation loc = (AlternateLocation)iter.next();
	            if (loc instanceof DirectAltLoc)
	                    continue;
	        
	            // skip non-fwt capable pushlocs if requested
	            if (ping.requestsFWTPushLocs()) {
	                PushAltLoc pushalt = (PushAltLoc)loc;
	                if (pushalt.supportsFWTVersion() < 1)
	                    continue;
	            }
	            
	            if (digest != null && digest.contains(loc)) {
	                filtered++;
	                continue;
	            }
	            temp.add(loc.createClone());
	            sent++;
	        }
	        altlocs = temp;
	    }
	    
	    // if after filtering / trimming we decided not to send anything, return
	    if (altlocs.getAltLocsSize() == 0 )
	        return;
	    
	    // put the response in the ggep
	    byte [] alts = altlocs.toBytesPush(toPack);
	    byte [] tmp = new byte[alts.length+2];
	    ByteOrder.short2beb((short)alts.length,tmp,0);
	    System.arraycopy(alts,0,tmp,2,alts.length);
	    dest.putAndCompress((char)GGEPHeadConstants.PUSHLOCS+GGEPHeadConstants.DATA,tmp);
	    
	    // if the other side indicated support for statistics, add those too
	    byte [] pingFeatures = ping._ggep.get(GGEPHeadConstants.GGEP_PROPS);
	    if ((pingFeatures[0] & GGEPHeadConstants.PUSH_MESH_STAT) 
	            == GGEPHeadConstants.PUSH_MESH_STAT) {
	        byte [] stats = new byte[4];
	        ByteOrder.short2leb(total,stats,0);
	        ByteOrder.short2leb(filtered,stats,2);
	        dest.put((char)GGEPHeadConstants.PUSH_MESH_STAT+GGEPHeadConstants.DATA,stats);
	    }
	}
	
}
	
