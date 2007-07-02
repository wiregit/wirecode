package com.limegroup.gnutella.messages.vendor;



import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.BitNumbers;
import org.limewire.collection.IntervalSet;
import org.limewire.collection.MultiRRIterator;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.CountingOutputStream;
import org.limewire.io.InvalidDataException;
import org.limewire.io.IpPort;
import org.limewire.io.NetworkUtils;
import org.limewire.service.ErrorService;
import org.limewire.util.ByteOrder;
import org.limewire.util.Decorator;

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
import com.limegroup.gnutella.messages.BadGGEPBlockException;
import com.limegroup.gnutella.messages.BadGGEPPropertyException;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.GGEP;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.gnutella.util.DataUtils;

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
 * NEW GGEP FORMAT:
 *   A GGEP block containing:
 *    F: features (optional)
 *     supported features:
 *      0x1 = TLS_CAPABLE
 *    C: response code (required)
 *    V: vendor id (required if not 404)
 *    Q: queue status (required if not 404)
 *    R: ranges (optional, shouldn't be if complete file)
 *    P: push locations (optional)
 *    A: direct locations (optional)
 *    T: indexes of which direct locations support TLS
 * 
 * OLD BINARY FORMAT:
 *   1 byte - features byte
 *   2 byte - response code
 *   4 bytes - vendor id
 *   1 byte - queue status
 *   n*8 bytes - n intervals (if requested && file partial && fits in packet)
 *   the rest - altlocs (if requested) 
 */
public class HeadPong extends VendorMessage {
	
	private static final Log LOG = LogFactory.getLog(HeadPong.class);
    
    /** GGEP fields in the ggep format of the pong. */ 
    private static final String FEATURES  = "F";
    private static final String CODE      = "C";
    private static final String VENDOR    = "V";
    private static final String QUEUE     = "Q";
    private static final String RANGES    = "R";
    private static final String RANGES5    = "R5";
    private static final String PUSH_LOCS = "P";
    private static final String LOCS      = "A";
    private static final String TLS_LOCS  = "T";
    
    /** Features within the GGEP Features block. */
    private static final byte TLS_CAPABLE = 0x1;
    
	/**
	 * cache references to the upload manager and file manager for
	 * easier stubbing and testing.
	 */
	private static UploadManager _uploadManager = RouterService.getUploadManager();	
	private static FileManager _fileManager = RouterService.getFileManager();
	
    /** The real packet size. */
    public static final int DEFAULT_PACKET_SIZE = 1380;
    
	/** The packet size used by this class -- non-final for testing. */
	private static /*final*/ int PACKET_SIZE = DEFAULT_PACKET_SIZE;
	
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
	/** all our slots are full. */
	private static final byte BUSY=(byte)0x7F;
    
    public static final int BINARY_VERSION = 1;
    public static final int GGEP_VERSION = 2;
    public static final int VERSION = 2;
    
	/** available ranges */
	private IntervalSet _ranges;
	/** the altlocs that were sent, if any */
	private Set<IpPort> _altLocs;
	/** the firewalled altlocs that were sent, if any */
	private Set<PushEndpoint> _pushLocs;
	/** the queue status, can be negative */
	private int _queueStatus;
	/** whether the other host has the file at all */
	private boolean _fileFound,_completeFile;
	/** the remote host */
	private byte [] _vendorId;
	/** whether the other host can receive tcp */
	private boolean _isFirewalled;
	/** whether the other host is currently downloading the file */
	private boolean _isDownloading;
    /** Whether the remote host supports TLS. */
    private boolean _tlsCapable;
    /** True if this came from a routed ping. */
    private boolean _routingBroken;
	
	/**
	 * Creates a message object with data from the network.
     * 
     * This will correctly set the fields of this HeadPong, as opposed
     * to the other constructor.
	 */
	protected HeadPong(byte[] guid, byte ttl, byte hops, int version, byte[] payload) throws BadPacketException {
		super(guid, ttl, hops, F_LIME_VENDOR_ID, F_UDP_HEAD_PONG, version, payload);
		
		//we should have some payload
		if (payload==null || payload.length<2)
			throw new BadPacketException("bad payload");
		
        if(version == BINARY_VERSION) {
            setFieldsFromBinary(payload);
        } else if(version >= GGEP_VERSION){
            setFieldsFromGGEP(payload);
        } else {
            throw new BadPacketException("invalid version!");
        }
    }
	
	/**
     * Constructs a message to send in response to the Ping.
     * If the Ping is version 1, this will construct a BINARY FORMAT pong.
     * Otherwise, this will construct a GGEP FORMAT pong.
     * 
     * NOTE: This will NOT set the fields of this class correctly.
     *       This constructor is intended ONLY for sending the reply
     *       through the network.  To access a HeadPong with the
     *       fields set correctly, you can write this to a ByteArrayOutputStream
     *       and reparse the resulting bytes through MessageFactory,
     *       which will construct a HeadPong with the network constructor,
     *       where the fields are correctly set.
	 */
	public HeadPong(HeadPongRequestor ping) {
		super(F_LIME_VENDOR_ID, F_UDP_HEAD_PONG, versionFor(ping), derivePayload(ping));
		setGUID(new GUID(ping.getGUID()));
	}
    

    
    /**
     * Sets all local fields based off the original version of the HeadPong,
     * from which the format was not very extensible.
     * 
     * @param payload
     * @throws BadPacketException
     */
    private void setFieldsFromBinary(byte[] payload) throws BadPacketException {
        //the first byte has to be FILE_NOT_FOUND, PARTIAL_FILE, 
        //COMPLETE_FILE, FIREWALLED or DOWNLOADING
        if (payload[1]>CODES_MASK) 
            throw new BadPacketException("invalid payload!");
        
        try {
            DataInputStream dais = new DataInputStream(new ByteArrayInputStream(payload));          
            //read and mask the features
            byte features = (byte) (dais.readByte() & HeadPing.FEATURE_MASK);            
            // older clients echoed the feature mask a ping sent them,
            // which can sometimes include the GGEP_PING feature.
            // these older clients also didn't correctly route pings to
            // their leaves.  newer clients fixed this, and use this fact
            // to recognize when an older push proxy sends them a bogus
            // response.
            _routingBroken = (features & HeadPing.GGEP_PING) == HeadPing.GGEP_PING;
            
            //read the response code
            byte code = dais.readByte(); 
            if(!setFieldsFromCode(code))
                return;
            
            //read the vendor id
            _vendorId = new byte[4];
            dais.readFully(_vendorId);
            
            //read the queue status
            _queueStatus = dais.readByte();
            
            if(!_completeFile && (features & HeadPing.INTERVALS) == HeadPing.INTERVALS)
                _ranges = readRanges(dais);
            
            //parse any included firewalled altlocs
            if ((features & HeadPing.PUSH_ALTLOCS) == HeadPing.PUSH_ALTLOCS) 
                _pushLocs=readPushLocs(dais);           
                
            //parse any included altlocs
            if ((features & HeadPing.ALT_LOCS) == HeadPing.ALT_LOCS) 
                _altLocs= readLocs(dais);
        } catch(IOException oops) {
            throw new BadPacketException(oops);
        }
    }
    
    /**
     * Sets all fields in the pong based on the GGEP format.
     * 
     * @param payload
     * @throws BadPacketException
     */
    private void setFieldsFromGGEP(byte[] payload) throws BadPacketException {
        GGEP ggep;
        try {
            ggep = new GGEP(payload, 0);
        } catch (BadGGEPBlockException e) {
            throw new BadPacketException(e);
        }
        
        byte[] code = getRequiredGGEPField(ggep, CODE);
        if(!setFieldsFromCode(code[0]))
            return;
        
        // No pongs that support GGEP have routing broken.
        _routingBroken = false;
        
        // Otherwise, there's more required.
        _vendorId = getRequiredGGEPField(ggep, VENDOR);
        _queueStatus = getRequiredGGEPField(ggep, QUEUE)[0];
        byte[] features = getOptionalGGEPField(ggep, FEATURES);
        if(features.length > 0) {
            _tlsCapable = (features[0] & TLS_CAPABLE) == TLS_CAPABLE;
        }
        
        try {
            byte[] ranges = getOptionalGGEPField(ggep, RANGES);
            byte [] ranges5 = getOptionalGGEPField(ggep, RANGES5);
            if(ranges.length > 0 || ranges5.length > 0)
                _ranges = parseRanges(ranges, ranges5);
            
            byte[] pushLocs = getOptionalGGEPField(ggep, PUSH_LOCS);
            if(pushLocs.length > 0)
                _pushLocs = parsePushLocs(pushLocs);
            
            byte[] altTLS = getOptionalGGEPField(ggep, TLS_LOCS);
            BitNumbers tls = null;
            if(altTLS.length > 0)
                tls = new BitNumbers(altTLS);
            
            byte[] altLocs = getOptionalGGEPField(ggep, LOCS);
            if(altLocs.length > 0)
                _altLocs = parseAltLocs(altLocs, tls);            
        } catch(IOException iox) {
            throw new BadPacketException(iox);
        }
    }
    
    /**
     * Returns false if code is FILE_NOT_FOUND.
     * Otherwise, returns true and sets _fileFound, and optionally sets
     * _isFirewalled, _completeFile, and _isDownloading depending on 
     * what is set within coe.
     */
    private boolean setFieldsFromCode(byte code) {
        if (code == FILE_NOT_FOUND) 
            return false;
        
        _fileFound=true;
        
        //is the other host firewalled?
        if ((code & FIREWALLED) == FIREWALLED)
            _isFirewalled = true;
        
        //if we have a partial file and the pong carries ranges, parse their list
        if ((code & COMPLETE_FILE) == COMPLETE_FILE) 
            _completeFile=true;
        //also check if the host is downloading the file
        else if ((code & DOWNLOADING) == DOWNLOADING)
            _isDownloading=true;
        
        return true;
    }
    

    /** Returns a required field, throwing a BadPacketException if it doesn't exist. */
    private byte[] getRequiredGGEPField(GGEP ggep, String header) throws BadPacketException {
        try {
            byte[] bytes = ggep.getBytes(header);
            if(bytes.length == 0)
                throw new BadPacketException("no data for header: " + header + "!");
            return bytes;
        } catch(BadGGEPPropertyException bgpe) {
            throw new BadPacketException(bgpe);
        }
    }
    
    /** Returns the bytes of the field in the GGEP if it exists, otherwise an empty array. */
    private byte[] getOptionalGGEPField(GGEP ggep, String header) {
        if(ggep.hasKey(header)) {
            try {
                return ggep.getBytes(header);
            } catch(BadGGEPPropertyException ignored) {}
        }
        
        return DataUtils.EMPTY_BYTE_ARRAY;
    }
    
    /** Determines the version that will be used based on the requestor. */
    private static int versionFor(HeadPongRequestor ping) {
        if(!ping.isPongGGEPCapable())
            return BINARY_VERSION;
        else
            return VERSION;
    }
	
	/**
	 * Constructs a byte[] that contains the payload of the HeadPong.
     * 
	 * @param ping the original UDP head ping to respond to
	 */
	private static byte [] derivePayload(HeadPongRequestor ping)  {
        if(!ping.isPongGGEPCapable()) {
            return constructBinaryPayload(ping);
        } else {
            return constructGGEPPayload(ping);
        }
    }
    
    /** Constructs the payload in binary format. */
    private static byte[] constructBinaryPayload(HeadPongRequestor ping) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		CountingOutputStream caos = new CountingOutputStream(baos);
		DataOutputStream daos = new DataOutputStream(caos);
		byte retCode=0;
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
    		
    		//if we don't have the file or its too large...
    		if (desc == null || desc.getFileSize() > Integer.MAX_VALUE) {
    			LOG.debug("we do not have the file");
    			daos.write(FILE_NOT_FOUND);
    			return baos.toByteArray();
    		}

            retCode = calculateCode(desc);
    		daos.write(retCode);
    		
    		if(LOG.isDebugEnabled())
    			LOG.debug("our return code is "+retCode);
    		
    		//write the vendor id
    		daos.write(F_LIME_VENDOR_ID);
    
    		//write out the return code and the queue status
    		daos.writeByte(calculateQueueStatus());
    		
    		//if we sent partial file and the remote asked for ranges, send them 
    		if ((retCode & PARTIAL_FILE) == PARTIAL_FILE && ping.requestsRanges()) 
    			didNotSendRanges=!writeRanges(caos,desc);
            
            didNotSendPushAltLocs = addPushLocations(ping, urn, caos, false, caos.getAmountWritten(), true);
            didNotSendAltLocs = addLocations(ping, urn, caos, null, caos.getAmountWritten(), true);
			
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
    
    /** Constructs the payload in GGEP format. */
    private static byte[] constructGGEPPayload(HeadPongRequestor ping) {
        GGEP ggep = new GGEP();
        
        URN urn = ping.getUrn();
        FileDesc desc = _fileManager.getFileDescForUrn(urn);
        // Easy case: no file, add code & exit.
        if(desc == null) {
            ggep.put(CODE, FILE_NOT_FOUND);
            return writeGGEP(ggep);
        }
        
        // OK, we have the file, now what!
        int size = 1;  // begin with 1 because of GGEP magic
        
        // If we're not firewalled and support TLS,
        // spread word about our TLS status.
        if(RouterService.acceptedIncomingConnection() && 
           ConnectionSettings.TLS_INCOMING.getValue() ) {
            ggep.put(FEATURES, TLS_CAPABLE);
            size += 4;
        }
        
        byte code = calculateCode(desc);
        ggep.put(CODE, code); size += ggep.getHeaderOverhead(CODE);
        ggep.put(VENDOR, F_LIME_VENDOR_ID); size += ggep.getHeaderOverhead(VENDOR);
        ggep.put(QUEUE, calculateQueueStatus()); size += ggep.getHeaderOverhead(QUEUE);        
        
        if((code & PARTIAL_FILE) == PARTIAL_FILE && ping.requestsRanges()) {
            IntervalSet.ByteIntervals ranges = deriveRanges(desc);
            if(ranges.length() == 0) {
                // If we have no ranges available, change queue status to busy,
                // so that they come back and ask us later, when we may have
                // more ranges available. (but don't increment size, since that
                // was already done above.)
                ggep.put(QUEUE, BUSY);
            } else if(size + ranges.length() + 7 <= PACKET_SIZE) { //3 for "R" and 4 for "R5"
                if (ranges.ints.length > 0) {
                    ggep.put(RANGES, ranges.ints);
                    size += ggep.getHeaderOverhead(RANGES);
                }
                if (ranges.longs.length > 0) {
                    ggep.put(RANGES5, ranges.longs);
                    size += ggep.getHeaderOverhead(RANGES5);
                }
            }
        }
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        addPushLocations(ping, urn, out, true, size+3, false);
        if(out.size() > 0) {
            byte[] pushLocs = out.toByteArray();
            ggep.put(PUSH_LOCS, pushLocs);
            size += ggep.getHeaderOverhead(PUSH_LOCS);
        }
        
        out.reset();
        AtomicReference<BitNumbers> bnRef = new AtomicReference<BitNumbers>();
        addLocations(ping, urn, out, bnRef, size+3, false);
        if(out.size() > 0) {
            byte[] altLocs = out.toByteArray();
            ggep.put(LOCS, altLocs);
            size += ggep.getHeaderOverhead(LOCS);
        }
        
        // If it went over, we screwed up somewhere.
        assert size <= PACKET_SIZE : "size is too big "+size+" vs "+PACKET_SIZE;
        
        // Here we fudge a bit -- possibly going over PACKET_SIZE.
        BitNumbers bn = bnRef.get();
        if(bn != null) {
            byte[] bnBytes = bn.toByteArray();
            if(bnBytes.length > 0) {
                ggep.put(TLS_LOCS, bnBytes);
                size += ggep.getHeaderOverhead(TLS_LOCS);
            }
        }
        
        byte[] output = writeGGEP(ggep);
        assert output.length == size : "expected: " + size + ", was: " + output.length;
        return output;
    }
    
    /** Calculates the code that should be returned, based on the FileDesc. */
    private static byte calculateCode(FileDesc fd) {
        byte code = 0;
        if(!RouterService.acceptedIncomingConnection()) {
            code = FIREWALLED;
        }
        if(fd instanceof IncompleteFileDesc) {
            code |= PARTIAL_FILE;
            
            IncompleteFileDesc ifd = (IncompleteFileDesc)fd;
            if(ifd.isActivelyDownloading())
                code |= DOWNLOADING;
        } else {
            code |= COMPLETE_FILE;
        }
        
        return code;
    }
    
    /** Calculates the queue status. */
    private static byte calculateQueueStatus() {
        int queueSize = _uploadManager.getNumQueuedUploads();
        
        if (queueSize >= UploadSettings.UPLOAD_QUEUE_SIZE.getValue())
            return BUSY;
        else if (queueSize > 0) 
            return (byte) queueSize;
        else   
            return (byte)(_uploadManager.uploadsInProgress() - 
                          UploadSettings.HARD_MAX_UPLOADS.getValue()
                         );
    }
    
    /** Adds push locations, if possible. */
    private static boolean addPushLocations(HeadPongRequestor ping, URN urn, OutputStream out, boolean includeTLS,
                                            int written, boolean includeSize) {
        if(!ping.requestsPushLocs())
            return true;
        
        try {
            boolean FWTOnly = ping.requestsFWTOnlyPushLocs();           
            if (FWTOnly) {
                AlternateLocationCollection<PushAltLoc> push = RouterService.getAltlocManager().getPushFWT(urn);
                synchronized(push) {
                    return !writePushLocs(out,
                                          push.iterator(),
                                          includeTLS,
                                          written,
                                          includeSize);
                }
            } else {
                AlternateLocationCollection<PushAltLoc> push = RouterService.getAltlocManager().getPushNoFWT(urn);
                AlternateLocationCollection<PushAltLoc> fwt = RouterService.getAltlocManager().getPushFWT(urn);
                synchronized(push) {
                    synchronized(fwt) {
                        return !writePushLocs(out,
                                              new MultiRRIterator<PushAltLoc>(push.iterator(),
                                                                              fwt.iterator()),
                                              includeTLS,
                                              written,
                                              includeSize);
                    }
                }
            }
        } catch(IOException impossible) {
            ErrorService.error(impossible);            
            return false;
        }
    }
    

    /** Adds direct locations, if possible. */
    private static boolean addLocations(HeadPongRequestor ping, URN urn, OutputStream out,
                                        AtomicReference<BitNumbers> tlsIndexes,
                                        int written, boolean includeSize) {
        //now add any non-firewalled altlocs in case they were requested. 
        if (ping.requestsAltlocs()) {
            AlternateLocationCollection<DirectAltLoc> col = RouterService.getAltlocManager().getDirect(urn);
            synchronized(col) {
                try {
                    return !writeLocs(out, col.iterator(), tlsIndexes, written, includeSize);
                } catch(IOException impossible) {
                    ErrorService.error(impossible);
                }
            }
        }
        
        return false;
    }
    
    /** Returns the byte[] of the written GGEP. */
    private static byte[] writeGGEP(GGEP ggep) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ggep.write(out);
        } catch(IOException iox) {
            ErrorService.error(iox);
        }
        return out.toByteArray();
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
    
    /** Whether or not this pong supports TLS. */
    public boolean isTLSCapable() {
        return _tlsCapable;
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
        rfd.setTLSCapable(isTLSCapable());
    }
    
	/**
	 * 
	 * @return the remote vendor as string
	 */
	public String getVendor() {
        if(_vendorId != null)
            return new String(_vendorId);
        else
            return null;
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
     * @return true if this pong came from a host that doesn't support routing
     */
    public boolean isRoutingBroken() {
        return _routingBroken;
    }
    
    public String toString() {
        return "HeadPong: " +
            " isRoutingBroken: "+ isRoutingBroken()+
            ", hasFile: "+hasFile()+
            ", hasCompleteFile: "+hasCompleteFile()+
            ", isDownloading: "+isDownloading()+
            ", isFirewalled: "+isFirewalled()+
            ", queue rank: "+getQueueStatus()+
            ", \nranges: "+getRanges()+
            ", \nalts: "+getAltLocs()+
            ", \npushalts: "+getPushLocs();
    }
	
	//*************************************
	//utility methods
	//**************************************
	
	/**
	 * reads available ranges from an inputstream
	 */
	private final IntervalSet readRanges(DataInputStream dais) throws IOException {
		int rangeLength=dais.readUnsignedShort();
		byte [] ranges = new byte[rangeLength];
		dais.readFully(ranges);
        return parseRanges(ranges, DataUtils.EMPTY_BYTE_ARRAY);
    }
    
    /** Parses available ranges. */
    private IntervalSet parseRanges(byte[] ranges, byte [] ranges5) throws IOException {
		return IntervalSet.parseBytes(ranges, ranges5);
	}
	
	/**
	 * reads firewalled alternate locations from an input stream
	 */
	private final Set<PushEndpoint> readPushLocs(DataInputStream dais) 
		throws IOException, BadPacketException {
		int size = dais.readUnsignedShort();
		byte [] altlocs = new byte[size];
		dais.readFully(altlocs);
        return parsePushLocs(altlocs);
    }
    
    /** Parses push alternate locations from a byte[]. */
    private Set<PushEndpoint> parsePushLocs(byte[] altlocs) throws IOException, BadPacketException {
		Set<PushEndpoint> ret = new HashSet<PushEndpoint>();
		ret.addAll(unpackPushEPs(new ByteArrayInputStream(altlocs)));
		return ret;
	}

    /** Unpacks a stream of Push Endpoints. */
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
        return parseAltLocs(altlocs, null);
    }
    
    /** Parses alternate locations from a byte[]. */
    private Set<IpPort> parseAltLocs(byte[] altlocs, final BitNumbers tlsIdx) throws IOException, BadPacketException {
		Set<IpPort> ret = new HashSet<IpPort>();
        try {
            if(tlsIdx == null) {
                ret.addAll(NetworkUtils.unpackIps(altlocs));
            } else {
                // Decorate the unpacking of the IPs in order to make
                // some of them TLS-capable.
                ret.addAll(NetworkUtils.unpackIps(altlocs, new Decorator<IpPort, IpPort>() {
                    int i = 0; 
                    
                    public IpPort decorate(IpPort input) {
                        if(tlsIdx.isSet(i))
                            input = new ConnectableImpl(input, true);
                        i++;
                        return input;
                    }
                }));
            }
        } catch(InvalidDataException ide) {
            throw new BadPacketException(ide);
        }
		return ret;
	}
	
	
	/**
	 * @param daos the output stream to write the ranges to
	 * @return if they were written or not.
	 */
	private static final boolean writeRanges(CountingOutputStream caos, FileDesc desc) throws IOException{
		DataOutputStream daos = new DataOutputStream(caos);
		LOG.debug("adding ranges to pong");
		IntervalSet.ByteIntervals ranges = deriveRanges(desc);

        // this is a non-ggep pong so we should not be serving long files.
        assert  ranges.longs.length == 0 : "long ranges in legacy pong";
        
		//write the ranges only if they will fit in the packet
		if (caos.getAmountWritten()+2 + ranges.ints.length <= PACKET_SIZE) {
			LOG.debug("added ranges");
			daos.writeShort((short)ranges.ints.length);
			caos.write(ranges.ints);
			return true;
		} 
		else { //the ranges will not fit - say we didn't send them.
			LOG.debug("ranges will not fit :(");
			return false;
		}
	}
    
    /** Returns the byte[] of the ranges. */
    private static final IntervalSet.ByteIntervals deriveRanges(FileDesc desc) {
        return ((IncompleteFileDesc)desc).getRangesAsByte();
    }
	
    /**
     * Writes out PushEndpoints in binary form to the output stream.
     * This will only write as many push locations as possible that can
     * fit in the PACKET_SIZE.  If includeTLS is true, this will include
     * a byte that describes which push proxies of each PushEndpoint
     * are capable of receiving TLS connections.
     * If includeSize is true, the written data will be prepended by the length
     * of the amount written.
     */
	private static final boolean writePushLocs(OutputStream out,
                                               Iterator<PushAltLoc> pushlocs,
                                               boolean includeTLS,
                                               int written, 
                                               boolean includeSize) throws IOException {
	
        if (!pushlocs.hasNext())
            return false;
        

        //push altlocs are bigger than normal altlocs, however we 
        //don't know by how much.  The size can be between
        //23 and 48 bytes.  We assume its 47 if includeTLS is false, 48 otherwise.
        int available = (PACKET_SIZE - (written + (includeSize ? 2 : 0))) / (includeTLS ? 48 : 47);
        
        // if we don't have any space left, we can't send any pushlocs
        if (available == 0)
            return false;
        
		if (LOG.isDebugEnabled())
			LOG.debug("trying to add up to "+available+ " push locs to pong");
		
        long now = System.currentTimeMillis();
        // Optimization: don't duplicate the written byte[] if not needed
		ByteArrayOutputStream baos = includeSize ? new ByteArrayOutputStream() : (ByteArrayOutputStream)out;
        while (pushlocs.hasNext() && available > 0) {
            PushAltLoc loc = pushlocs.next();

            if (loc.getPushAddress().getProxies().isEmpty()) {
                pushlocs.remove();
                continue;
            }
            
            if (loc.canBeSent(AlternateLocation.MESH_PING)) {
                baos.write(loc.getPushAddress().toBytes(includeTLS));
                available--;
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
            if(includeSize) {
                ByteOrder.short2beb((short)baos.size(),out);
    			baos.writeTo(out);
            } // else it's already written to out
			return true;
		}
	}
    
    /**
     * Writes out alternate locations in binary form to the output stream.
     * This will only write as many locations as possible that can
     * fit in the PACKET_SIZE.  If tlsIndexes is non-null, the reference
     * will be set to a BitNumbers whose size is the number of locations
     * that are attempted to write, with the corresponding bits set
     * if the location at the index is tls capable.
     * If includeSize is true, the written data will be prepended by the length
     * of the amount written.
     */
	private static final boolean writeLocs(OutputStream out,
                                           Iterator<DirectAltLoc> altlocs,
                                           AtomicReference<BitNumbers> tlsIndexes,
                                           int written,
                                           boolean includeSize) throws IOException {
		
		//do we have any altlocs?
		if (!altlocs.hasNext())
			return false;
        
        //how many can we fit in the packet?
        int toSend = (PACKET_SIZE - (written + (includeSize ? 2 : 0)) ) / 6;
        if (toSend == 0)
            return false;
        
		if (LOG.isDebugEnabled())
			LOG.debug("trying to add up to "+ toSend +" locs to pong");
        
        BitNumbers bn = null;
        if(tlsIndexes != null) {
            bn = new BitNumbers(toSend);
            tlsIndexes.set(bn);
        }
        
        // optimization: do not duplicate byte[] if not needed
		ByteArrayOutputStream baos = includeSize ? new ByteArrayOutputStream() : (ByteArrayOutputStream)out;
        int sent = 0;
        long now = System.currentTimeMillis();
		while(altlocs.hasNext() && sent < toSend) {
            DirectAltLoc loc = altlocs.next();
            if (loc.canBeSent(AlternateLocation.MESH_PING)) {
                loc.send(now,AlternateLocation.MESH_PING);
                baos.write(loc.getHost().getInetAddress().getAddress());
                ByteOrder.short2leb((short)loc.getHost().getPort(),baos);
                IpPort host = loc.getHost();
                if(bn != null && host instanceof Connectable && ((Connectable)host).isTLSCapable())
                    bn.set(sent);
                sent++;
            } else if (!loc.canBeSentAny())
                altlocs.remove();
        }
		
		LOG.debug("adding altlocs");
        if(includeSize) {
    		ByteOrder.short2beb((short)baos.size(),out);
    		baos.writeTo(out);
        }
		return true;
			
	}
	
}
	
