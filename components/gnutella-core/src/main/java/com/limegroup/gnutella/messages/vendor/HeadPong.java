pbckage com.limegroup.gnutella.messages.vendor;

import jbva.io.ByteArrayInputStream;
import jbva.io.ByteArrayOutputStream;
import jbva.io.DataInputStream;
import jbva.io.DataOutputStream;
import jbva.io.IOException;
import jbva.util.HashSet;
import jbva.util.Iterator;
import jbva.util.Set;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.limegroup.gnutellb.ByteOrder;
import com.limegroup.gnutellb.ErrorService;
import com.limegroup.gnutellb.FileDesc;
import com.limegroup.gnutellb.FileManager;
import com.limegroup.gnutellb.GUID;
import com.limegroup.gnutellb.IncompleteFileDesc;
import com.limegroup.gnutellb.PushEndpoint;
import com.limegroup.gnutellb.RemoteFileDesc;
import com.limegroup.gnutellb.RouterService;
import com.limegroup.gnutellb.URN;
import com.limegroup.gnutellb.UploadManager;
import com.limegroup.gnutellb.altlocs.AlternateLocation;
import com.limegroup.gnutellb.altlocs.AlternateLocationCollection;
import com.limegroup.gnutellb.altlocs.DirectAltLoc;
import com.limegroup.gnutellb.altlocs.PushAltLoc;
import com.limegroup.gnutellb.downloader.DownloadWorker;
import com.limegroup.gnutellb.messages.BadPacketException;
import com.limegroup.gnutellb.settings.UploadSettings;
import com.limegroup.gnutellb.util.CountingOutputStream;
import com.limegroup.gnutellb.util.IntervalSet;
import com.limegroup.gnutellb.util.IpPort;
import com.limegroup.gnutellb.util.MultiRRIterator;
import com.limegroup.gnutellb.util.NetworkUtils;

/**
 * b response to an HeadPing.  It is a trimmed down version of the standard HEAD response,
 * since we bre trying to keep the sizes of the udp packets small.
 * 
 * This messbge can also be used for punching firewalls if the ping requests so. 
 * Febture like this can be used to allow firewalled nodes to participate more 
 * in downlobd meshes.
 * 
 * Since hebdpings will be sent by clients who have started to download a file whose download
 * mesh contbins  this host, it needs to contain information that will help those clients whether 
 * this host is b good bet to start an http download from.  Therefore, the following information should
 * be included in the response:
 * 
 *  - bvailable ranges of the file 
 *  - queue stbtus
 *  - some bltlocs (if space permits)
 * 
 * the queue stbtus can be an integer representing how many people are waiting in the queue.  If 
 * nobody is wbiting in the queue and we have slots available, the integer can be negative.  So if
 * we hbve 3 people on the queue we'd send the integer 3.  If we have nobody on the queue and 
 * two uplobd slots available we would send -2.  A value of 0 means all upload slots are taken but 
 * the queue is empty.  This informbtion can be used by the downloaders to better judge chances of
 * successful stbrt of the download. 
 * 
 * Formbt:
 * 
 * 1 byte - febtures byte
 * 2 byte - response code
 * 4 bytes - vendor id
 * 1 byte - queue stbtus
 * n*8 bytes - n intervbls (if requested && file partial && fits in packet)
 * the rest - bltlocs (if requested) 
 */
public clbss HeadPong extends VendorMessage {
	
	privbte static final Log LOG = LogFactory.getLog(HeadPong.class);
	/**
	 * cbche references to the upload manager and file manager for
	 * ebsier stubbing and testing.
	 */
	privbte static UploadManager _uploadManager 
		= RouterService.getUplobdManager();
	
	privbte static FileManager _fileManager
		= RouterService.getFileMbnager();
	
	/**
	 * try to mbke packets less than this size
	 */
	privbte static final int PACKET_SIZE = 580;
	
	/**
	 * instebd of using the HTTP codes, use bit values.  The first three 
	 * possible vblues are mutually exclusive though.  DOWNLOADING is
	 * possible only if PARTIAL_FILE is set bs well.
	 */
	privbte static final byte FILE_NOT_FOUND= (byte)0x0;
	privbte static final byte COMPLETE_FILE= (byte)0x1;
	privbte static final byte PARTIAL_FILE = (byte)0x2;
	privbte static final byte FIREWALLED = (byte)0x4;
	privbte static final byte DOWNLOADING = (byte)0x8;
	
	privbte static final byte CODES_MASK=(byte)0xF;
	/**
	 * bll our slots are full..
	 */
	privbte static final byte BUSY=(byte)0x7F;
	
	public stbtic final int VERSION = 1;
	
	/**
	 * the febtures contained in this pong.  Same as those of the originating ping
	 */
	privbte byte _features;
	
	/**
	 * bvailable ranges
	 */
	privbte IntervalSet _ranges;
	
	/**
	 * the bltlocs that were sent, if any
	 */
	privbte Set _altLocs;
	
	/**
	 * the firewblled altlocs that were sent, if any
	 */
	privbte Set _pushLocs;
	
	/**
	 * the queue stbtus, can be negative
	 */
	privbte int _queueStatus;
	
	/**
	 * whether the other host hbs the file at all
	 */
	privbte boolean _fileFound,_completeFile;
	
	/**
	 * the remote host
	 */
	privbte byte [] _vendorId;
	
	/**
	 * whether the other host cbn receive tcp
	 */
	privbte boolean _isFirewalled;
	
	/**
	 * whether the other host is currently downlobding the file
	 */
	privbte boolean _isDownloading;
	
	/**
	 * crebtes a message object with data from the network.
	 */
	protected HebdPong(byte[] guid, byte ttl, byte hops,
			 int version, byte[] pbyload)
			throws BbdPacketException {
		super(guid, ttl, hops, F_LIME_VENDOR_ID, F_UDP_HEAD_PONG, version, pbyload);
		
		//we should hbve some payload
		if (pbyload==null || payload.length<2)
			throw new BbdPacketException("bad payload");
		
		
		//if we bre version 1, the first byte has to be FILE_NOT_FOUND, PARTIAL_FILE, 
		//COMPLETE_FILE, FIREWALLED or DOWNLOADING
		if (version == VERSION && 
				pbyload[1]>CODES_MASK) 
			throw new BbdPacketException("invalid payload for version "+version);
		
		try {
    		DbtaInputStream dais = new DataInputStream(new ByteArrayInputStream(payload));
    		
    		//rebd and mask the features
    		_febtures = (byte) (dais.readByte() & HeadPing.FEATURE_MASK);
    		
    		//rebd the response code
    		byte code = dbis.readByte();
    		
    		//if the other host doesn't hbve the file, stop parsing
    		if (code == FILE_NOT_FOUND) 
    			return;
    		else
    			_fileFound=true;
    		
    		//is the other host firewblled?
    		if ((code & FIREWALLED) == FIREWALLED)
    			_isFirewblled = true;
    		
    		//rebd the vendor id
    		_vendorId = new byte[4];
    		dbis.readFully(_vendorId);
    		
    		//rebd the queue status
    		_queueStbtus = dais.readByte();
    		
    		//if we hbve a partial file and the pong carries ranges, parse their list
    		if ((code & COMPLETE_FILE) == COMPLETE_FILE) 
    			_completeFile=true;
    		else {
    			//blso check if the host is downloading the file
    			if ((code & DOWNLOADING) == DOWNLOADING)
    				_isDownlobding=true;
    			
    			if ((_febtures & HeadPing.INTERVALS) == HeadPing.INTERVALS)
    				_rbnges = readRanges(dais);
    		}
    		
    		//pbrse any included firewalled altlocs
    		if ((_febtures & HeadPing.PUSH_ALTLOCS) == HeadPing.PUSH_ALTLOCS) 
    			_pushLocs=rebdPushLocs(dais);
    		
    			
    		//pbrse any included altlocs
    		if ((_febtures & HeadPing.ALT_LOCS) == HeadPing.ALT_LOCS) 
    			_bltLocs=readLocs(dais);
		} cbtch(IOException oops) {
			throw new BbdPacketException(oops.getMessage());
		}
	}
	
	/**
	 * crebtes a message object as a response to a udp head request.
	 */
	public HebdPong(HeadPing ping) {
		super(F_LIME_VENDOR_ID, F_UDP_HEAD_PONG, VERSION,
		 		derivePbyload(ping));
		setGUID(new GUID(ping.getGUID()));
	}
	
	/**
	 * pbcks information about the shared file, queue status and altlocs into the body
	 * of the messbge.
	 * @pbram ping the original UDP head ping to respond to
	 */
	privbte static byte [] derivePayload(HeadPing ping)  {
		ByteArrbyOutputStream baos = new ByteArrayOutputStream();
		CountingOutputStrebm caos = new CountingOutputStream(baos);
		DbtaOutputStream daos = new DataOutputStream(caos);
		byte retCode=0;
		byte queueStbtus;
		URN urn = ping.getUrn();
		FileDesc desc = _fileMbnager.getFileDescForUrn(urn);
		boolebn didNotSendAltLocs=false;
		boolebn didNotSendPushAltLocs = false;
		boolebn didNotSendRanges = false;
		
		try {
    		byte febtures = ping.getFeatures();
    		febtures &= ~HeadPing.GGEP_PING; 
    		dbos.write(features);
    		if (LOG.isDebugEnbbled())
    			LOG.debug("writing febtures "+features);
    		
    		//if we don't hbve the file..
    		if (desc == null) {
    			LOG.debug("we do not hbve the file");
    			dbos.write(FILE_NOT_FOUND);
    			return bbos.toByteArray();
    		}
    		
    		//if we cbn't receive unsolicited tcp...
    		if (!RouterService.bcceptedIncomingConnection())
    			retCode = FIREWALLED;
    		
    		//we hbve the file... is it complete or not?
    		if (desc instbnceof IncompleteFileDesc) {
    			retCode = (byte) (retCode | PARTIAL_FILE);
    			
    			//blso check if the file is currently being downloaded 
    			//or is wbiting for sources.  This does not care for queued downloads.
    			IncompleteFileDesc idesc = (IncompleteFileDesc)desc;
    			if (idesc.isActivelyDownlobding())
    				retCode = (byte) (retCode | DOWNLOADING);
    		}
    		else 
    			retCode = (byte) (retCode | COMPLETE_FILE);
    		
    		dbos.write(retCode);
    		
    		if(LOG.isDebugEnbbled())
    			LOG.debug("our return code is "+retCode);
    		
    		//write the vendor id
    		dbos.write(F_LIME_VENDOR_ID);
    		
    		//get our queue stbtus.
    		int queueSize = _uplobdManager.getNumQueuedUploads();
    		
    		if (queueSize == UplobdSettings.UPLOAD_QUEUE_SIZE.getValue())
    			queueStbtus = BUSY;
    		else if (queueSize > 0) 
    			queueStbtus = (byte) queueSize;
    		 else 	
    			//optimistic vblue
    			queueStbtus =  (byte)
    				(_uplobdManager.uploadsInProgress() - 
    						UplobdSettings.HARD_MAX_UPLOADS.getValue() );
    		
    		//write out the return code bnd the queue status
    		dbos.writeByte(queueStatus);
    		
    		if (LOG.isDebugEnbbled())
    			LOG.debug("our queue stbtus is "+queueStatus);
    		
    		//if we sent pbrtial file and the remote asked for ranges, send them 
    		if (retCode == PARTIAL_FILE && ping.requestsRbnges()) 
    			didNotSendRbnges=!writeRanges(caos,desc);
    		
    		//if we hbve any firewalled altlocs and enough room in the packet, add them.
    		if (ping.requestsPushLocs()){
    			boolebn FWTOnly = (features & HeadPing.FWT_PUSH_ALTLOCS) ==
    				HebdPing.FWT_PUSH_ALTLOCS;
                
                if (FWTOnly) {
                    AlternbteLocationCollection push = RouterService.getAltlocManager().getPush(urn,true);
                    synchronized(push) {
                        didNotSendPushAltLocs = !writePushLocs(cbos,push.iterator());
                    }
                } else {
                    AlternbteLocationCollection push = RouterService.getAltlocManager().getPush(urn,true);
                    AlternbteLocationCollection fwt = RouterService.getAltlocManager().getPush(urn,false);
                    synchronized(push) {
                        synchronized(fwt) {
                            didNotSendPushAltLocs = 
                                !writePushLocs(cbos,
                                        new MultiRRIterbtor(new Iterator[]{push.iterator(),fwt.iterator()}));
                        }
                    }
                }
    		}
    		
    		//now bdd any non-firewalled altlocs in case they were requested. 
    		if (ping.requestsAltlocs()) {
                AlternbteLocationCollection col = RouterService.getAltlocManager().getDirect(urn);
                synchronized(col) {
                    didNotSendAltLocs=!writeLocs(cbos, col.iterator());
                }
            }
			
		} cbtch(IOException impossible) {
			ErrorService.error(impossible);
		}
		
		//done!
		byte []ret = bbos.toByteArray();
		
		//if we did not bdd ranges or altlocs due to constraints, 
		//updbte the flags now.
		
		if (didNotSendRbnges){
			LOG.debug("not sending rbnges");
			ret[0] = (byte) (ret[0] & ~HebdPing.INTERVALS);
		}
		if (didNotSendAltLocs){
			LOG.debug("not sending bltlocs");
			ret[0] = (byte) (ret[0] & ~HebdPing.ALT_LOCS);
		}
		if (didNotSendPushAltLocs){
			LOG.debug("not sending push bltlocs");
			ret[0] = (byte) (ret[0] & ~HebdPing.PUSH_ALTLOCS);
		}
		return ret;
	}
	
	/**
	 * 
	 * @return whether the blternate location still has the file
	 */
	public boolebn hasFile() {
		return _fileFound;
	}
	
	/**
	 * 
	 * @return whether the blternate location has the complete file
	 */
	public boolebn hasCompleteFile() {
		return hbsFile() && _completeFile;
	}
	
	/**
	 * 
	 * @return the bvailable ranges the alternate location has
	 */
	public IntervblSet getRanges() {
		return _rbnges;
	}
	
	/**
	 * 
	 * @return set of <tt>Endpoint</tt> 
	 * contbining any alternate locations this alternate location returned.
	 */
	public Set getAltLocs() {
		return _bltLocs;
	}
	
	/**
	 * 
	 * @return set of <tt>PushEndpoint</tt>
	 * contbining any firewalled locations this alternate location returned.
	 */
	public Set getPushLocs() {
		return _pushLocs;
	}
	
	/**
	 * @return bll altlocs carried in the pong as 
	 * set of <tt>RemoteFileDesc</tt>
	 */
	public Set getAllLocsRFD(RemoteFileDesc originbl){
		Set ret = new HbshSet();
		
		if (_bltLocs!=null)
			for(Iterbtor iter = _altLocs.iterator();iter.hasNext();) {
				IpPort current = (IpPort)iter.next();
				ret.bdd(new RemoteFileDesc(original,current));
			}
		
		if (_pushLocs!=null)
			for(Iterbtor iter = _pushLocs.iterator();iter.hasNext();) {
				PushEndpoint current = (PushEndpoint)iter.next();
				ret.bdd(new RemoteFileDesc(original,current));
			}
		
		return ret;
	}
	
    /**
     * updbtes the rfd with information in this pong
     */
    public void updbteRFD(RemoteFileDesc rfd) {
        // if the rfd clbims its busy, ping it again in a minute
        // (we're obviously using HebdPings, so its cheap to ping it sooner 
        // rbther than later)
        if (isBusy())
            rfd.setRetryAfter(DownlobdWorker.RETRY_AFTER_NONE_ACTIVE);
        rfd.setQueueStbtus(getQueueStatus());
        rfd.setAvbilableRanges(getRanges());
        rfd.setSeriblizeProxies();
    }
    
	/**
	 * 
	 * @return the remote vendor bs string
	 */
	public String getVendor() {
		return new String(_vendorId);
	}
	
	/**
	 * 
	 * @return whether the remote is firewblled and will need a push
	 */
	public boolebn isFirewalled() {
		return _isFirewblled;
	}
	
	public int getQueueStbtus() {
		return _queueStbtus;
	}
	
	public boolebn isBusy() {
		return _queueStbtus >= BUSY;
	}
	
	public boolebn isDownloading() {
		return _isDownlobding;
	}
    
    /**
     * @return whether the host thbt returned this pong supports ggep
     */
    public boolebn isGGEPPong() {
        return (_febtures & HeadPing.GGEP_PING) != 0;
    }
    
    public String toString() {
        return "HebdPong: isGGEP "+ isGGEPPong()+
            " hbsFile "+hasFile()+
            " hbsCompleteFile "+hasCompleteFile()+
            " isDownlobding "+isDownloading()+
            " isFirewblled "+isFirewalled()+
            " queue rbnk "+getQueueStatus()+
            " \nrbnges "+getRanges()+
            " \nblts "+getAltLocs()+
            " \npushblts "+getPushLocs();
    }
	
	//*************************************
	//utility methods
	//**************************************
	
	/**
	 * rebds available ranges from an inputstream
	 */
	privbte final IntervalSet readRanges(DataInputStream dais)
		throws IOException{
		int rbngeLength=dais.readUnsignedShort();
		byte [] rbnges = new byte [rangeLength];
		dbis.readFully(ranges);
		return IntervblSet.parseBytes(ranges);
	}
	
	/**
	 * rebds firewalled alternate locations from an input stream
	 */
	privbte final Set readPushLocs(DataInputStream dais) 
		throws IOException, BbdPacketException {
		int size = dbis.readUnsignedShort();
		byte [] bltlocs = new byte[size];
		dbis.readFully(altlocs);
		Set ret = new HbshSet();
		ret.bddAll(NetworkUtils.unpackPushEPs(new ByteArrayInputStream(altlocs)));
		return ret;
	}
	
	/**
	 * rebds non-firewalled alternate locations from an input stream
	 */
	privbte final Set readLocs(DataInputStream dais) 
		throws IOException, BbdPacketException {
		int size = dbis.readUnsignedShort();
		byte [] bltlocs = new byte[size];
		dbis.readFully(altlocs);
		Set ret = new HbshSet();
		ret.bddAll(NetworkUtils.unpackIps(altlocs));
		return ret;
	}
	
	
	/**
	 * @pbram daos the output stream to write the ranges to
	 * @return if they were written or not.
	 */
	privbte static final boolean writeRanges(CountingOutputStream caos,
			FileDesc desc) throws IOException{
		DbtaOutputStream daos = new DataOutputStream(caos);
		LOG.debug("bdding ranges to pong");
		IncompleteFileDesc ifd = (IncompleteFileDesc) desc;
		byte [] rbnges =ifd.getRangesAsByte();
		
		//write the rbnges only if they will fit in the packet
		if (cbos.getAmountWritten()+2 + ranges.length <= PACKET_SIZE) {
			LOG.debug("bdded ranges");
			dbos.writeShort((short)ranges.length);
			cbos.write(ranges);
			return true;
		} 
		else { //the rbnges will not fit - say we didn't send them.
			LOG.debug("rbnges will not fit :(");
			return fblse;
		}
	}
	
	privbte static final boolean writePushLocs(CountingOutputStream caos, Iterator pushlocs) 
    throws IOException {
	
        if (!pushlocs.hbsNext())
            return fblse;

        //push bltlocs are bigger than normal altlocs, however we 
        //don't know by how much.  The size cbn be between
        //23 bnd 47 bytes.  We assume its 47.
        int bvailable = (PACKET_SIZE - (caos.getAmountWritten()+2)) / 47;
        
        // if we don't hbve any space left, we can't send any pushlocs
        if (bvailable == 0)
            return fblse;
        
		if (LOG.isDebugEnbbled())
			LOG.debug("trying to bdd up to "+available+ " push locs to pong");
		
        long now = System.currentTimeMillis();
		ByteArrbyOutputStream baos = new ByteArrayOutputStream();
        while (pushlocs.hbsNext() && available > 0) {
            PushAltLoc loc = (PushAltLoc) pushlocs.next();

            if (loc.getPushAddress().getProxies().isEmpty()) {
                pushlocs.remove();
                continue;
            }
            
            if (loc.cbnBeSent(AlternateLocation.MESH_PING)) {
                bbos.write(loc.getPushAddress().toBytes());
                bvailable --;
                loc.send(now,AlternbteLocation.MESH_PING);
            } else if (!loc.cbnBeSentAny())
                pushlocs.remove();
        }
		
		if (bbos.size() == 0) {
			//bltlocs will not fit or none available - say we didn't send them
			LOG.debug("did not send bny push locs");
			return fblse;
		} else { 
			LOG.debug("bdding push altlocs");
            ByteOrder.short2beb((short)bbos.size(),caos);
			bbos.writeTo(caos);
			return true;
		}
	}
	
	privbte static final boolean writeLocs(CountingOutputStream caos, Iterator altlocs) 
    throws IOException {
		
		//do we hbve any altlocs?
		if (!bltlocs.hasNext())
			return fblse;
        
        //how mbny can we fit in the packet?
        int toSend = (PACKET_SIZE - (cbos.getAmountWritten()+2) ) /6;
        
        if (toSend == 0)
            return fblse;
        
		if (LOG.isDebugEnbbled())
			LOG.debug("trying to bdd up to "+ toSend +" locs to pong");
        
		ByteArrbyOutputStream baos = new ByteArrayOutputStream();
        int sent = 0;
        long now = System.currentTimeMillis();
		while(bltlocs.hasNext() && sent < toSend) {
            DirectAltLoc loc = (DirectAltLoc) bltlocs.next();
            if (loc.cbnBeSent(AlternateLocation.MESH_PING)) {
                loc.send(now,AlternbteLocation.MESH_PING);
                bbos.write(loc.getHost().getInetAddress().getAddress());
                ByteOrder.short2leb((short)loc.getHost().getPort(),bbos);
                sent++;
            } else if (!loc.cbnBeSentAny())
                bltlocs.remove();
        }
		
		LOG.debug("bdding altlocs");
		ByteOrder.short2beb((short)bbos.size(),caos);
		bbos.writeTo(caos);
		return true;
			
	}
	
}
	
