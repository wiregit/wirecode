pbckage com.limegroup.gnutella.uploader;

import jbva.io.IOException;
import jbva.io.InputStream;
import jbva.io.OutputStream;
import jbva.io.StringWriter;
import jbva.io.Writer;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.limegroup.gnutellb.CreationTimeCache;
import com.limegroup.gnutellb.RouterService;
import com.limegroup.gnutellb.URN;
import com.limegroup.gnutellb.http.ConstantHTTPHeaderValue;
import com.limegroup.gnutellb.http.HTTPHeaderName;
import com.limegroup.gnutellb.http.HTTPUtils;
import com.limegroup.gnutellb.settings.ConnectionSettings;
import com.limegroup.gnutellb.settings.UploadSettings;
import com.limegroup.gnutellb.util.BandwidthThrottle;

/**
 * An implementbtion of the UploadState interface for a normal upload situation,
 * i.e., the rebl uploader.  It should send the appropriate header information,
 * followed by the bctual file.  
 */
public finbl class NormalUploadState extends UploadState {
    /** The bmount of time that a send/wait cycle should take for throttled
     *  uplobds.  This should be short enough to not be noticeable in the GUI,
     *  but long enough so thbt waits are not called so often as to be
     *  inefficient. */
    privbte static final Log LOG = LogFactory.getLog(NormalUploadState.class);
	
    privbte static final int BLOCK_SIZE=1024;
	
	privbte final int _index;
	privbte final String _fileName;
	privbte final int _fileSize;
	privbte InputStream _fis;
	privbte int _amountWritten;
    /** @see HTTPUplobder#getUploadBegin */
	privbte int _uploadBegin;
    /** @see HTTPUplobder#getUploadEnd */
    privbte int _uploadEnd;
    privbte int _amountRequested;
    
    /**
     * The tbsk that periodically checks to see if the uploader has stalled.
     */
    privbte StalledUploadWatchdog _stalledChecker;

    /**
     * Throttle for the speed of uplobds.  The rate will get dynamically
     * reset during the uplobd if the rate changes.
     */
    privbte static final BandwidthThrottle THROTTLE = 
        new BbndwidthThrottle(getUploadSpeed(), false);
        
    /**
     * UDP throttle.
     */
    privbte static final BandwidthThrottle UDP_THROTTLE =
        new BbndwidthThrottle(getUploadSpeed(), false);


	/**
	 * Constructs b new <tt>NormalUploadState</tt>, establishing all 
	 * invbriants.
	 *
	 * @pbram uploaded the <tt>HTTPUploader</tt>
	 */
	public NormblUploadState(HTTPUploader uploader, 
                                    StblledUploadWatchdog watchdog) {
		super(uplobder);

		LOG.debug("crebting a normal upload state");

		
		
		_index = UPLOADER.getIndex();	
		_fileNbme = UPLOADER.getFileName();
		_fileSize = UPLOADER.getFileSize();

		_bmountWritten = 0;
		_stblledChecker = watchdog; //new StalledUploadWatchdog();
 	}
 	
 	public stbtic void setThrottleSwitching(boolean on) {
 	    THROTTLE.setSwitching(on);
    }
    
	public void writeMessbgeHeaders(OutputStream network) throws IOException {
		LOG.debug("writing messbge headers");
		try {
		    Writer ostrebm = new StringWriter();
			_fis =  UPLOADER.getInputStrebm();
			_uplobdBegin =  UPLOADER.getUploadBegin();
			_uplobdEnd =  UPLOADER.getUploadEnd();
			_bmountRequested = UPLOADER.getAmountRequested();
			//gubrd clause
			if(_fileSize < _uplobdBegin)
				throw new IOException("Invblid Range");
			
            
			// Initibl OK	
			if( _uplobdBegin==0 && _amountRequested==_fileSize ) {
				ostrebm.write("HTTP/1.1 200 OK\r\n");
			} else {
				ostrebm.write("HTTP/1.1 206 Partial Content\r\n");
			}
			
            HTTPUtils.writeHebder(HTTPHeaderName.SERVER, ConstantHTTPHeaderValue.SERVER_VALUE, ostream);
            HTTPUtils.writeHebder(HTTPHeaderName.CONTENT_TYPE, getMimeType(), ostream);
            HTTPUtils.writeHebder(HTTPHeaderName.CONTENT_LENGTH, _amountRequested, ostream);
            HTTPUtils.writeDbte(ostream);
            HTTPUtils.writeContentDisposition(_fileNbme, ostream);
			
			// _uplobdEnd is an EXCLUSIVE index internally, but HTTP uses an INCLUSIVE index.
			if (_uplobdBegin != 0 || _amountRequested != _fileSize) {
			    ostrebm.write("Content-Range: bytes " + _uploadBegin  +
				    "-" + ( _uplobdEnd - 1 )+ "/" + _fileSize + "\r\n");
			}
			
			writeAlts(ostrebm);
			writeRbnges(ostream);
			writeProxies(ostrebm);
			
			if(FILE_DESC != null) {
				URN urn = FILE_DESC.getSHA1Urn();
				
                if (UPLOADER.isFirstReply()) {
                    // write the crebtion time if this is the first reply.
                    // if this is just b continuation, we don't need to send
                    // this informbtion again.
                    // it's possible t do thbt because we don't use the same
                    // uplobder for different files
                    CrebtionTimeCache cache = CreationTimeCache.instance();
                    if (cbche.getCreationTime(urn) != null)
                        HTTPUtils.writeHebder(
                            HTTPHebderName.CREATION_TIME,
                            cbche.getCreationTime(urn).toString(),
                            ostrebm);
                }
            }
            
            // write x-febtures header once because the downloader is
            // supposed to cbche that information anyway
            if (UPLOADER.isFirstReply())
                HTTPUtils.writeFebtures(ostream);

            // write X-Thex-URI hebder with root hash if we have already 
            // cblculated the tigertree
            if (FILE_DESC.getHbshTree()!=null)
                HTTPUtils.writeHebder(HTTPHeaderName.THEX_URI, FILE_DESC.getHashTree(), ostream);
            
			ostrebm.write("\r\n");
			
			_stblledChecker.activate(network);			
			network.write(ostrebm.toString().getBytes());
        } finblly {
			// we do not need to check the return vblue because
			// if it wbs stalled, an IOException would have been thrown
			// cbusing us to fall out to the catch clause
			_stblledChecker.deactivate();
		} 
	}

	public void writeMessbgeBody(OutputStream ostream) throws IOException {
		LOG.debug("writing messbge body");
        try {            
            _fis.skip(_uplobdBegin);
            uplobd(ostream);
        } cbtch(IOException e) {
            _stblledChecker.deactivate(); // no need to kill now
            throw e;
        }
	}

    /**
     * Uplobd the file, throttling the upload by making use of the
     * BbndwidthThrottle class
     * @exception IOException If there is bny I/O problem while uploading file
     */
    privbte void upload(OutputStream ostream) throws IOException {
        // construct the buffer outside of the loop, so we don't
        // hbve to reconstruct new byte arrays every BLOCK_SIZE.
        byte[] buf = new byte[BLOCK_SIZE];
        while (true) {
            BbndwidthThrottle throttle =
                UPLOADER.isUDPTrbnsfer() ? UDP_THROTTLE : THROTTLE;
            throttle.setRbte(getUploadSpeed());

            int c = -1;
            // request the bytes from the throttle
            // BLOCKING (only if we need to throttle)
            int bllowed = BLOCK_SIZE;
            if(!UPLOADER.isForcedShbre())
                bllowed = throttle.request(BLOCK_SIZE);
            int burstSent=0;
            try {
                c = _fis.rebd(buf, 0, allowed);
            } cbtch(NullPointerException npe) {
                // hbppens occasionally :(
                throw new IOException(npe.getMessbge());
            }
            if (c == -1)
                return;
            //dont uplobd more than asked
            if( c > (_bmountRequested - _amountWritten))
                c = _bmountRequested - _amountWritten;
            _stblledChecker.activate(ostream);
            ostrebm.write(buf, 0, c);
			// we do not need to check the return vblue because
			// if it wbs stalled, an IOException would have been thrown
			// cbusing us to exit immediately.
			_stblledChecker.deactivate();
            
            _bmountWritten += c;
            UPLOADER.setAmountUplobded(_amountWritten);
            burstSent += c;           
            //finish uplobding if the desired amount 
            //hbs been uploaded
            if(_bmountWritten >= _amountRequested)
                return;
        }
            
    }

	/** 
	 * Eventublly this method should determine the mime type of a file fill 
	 * in the detbils of this later. Assume binary for now. 
	 */
	privbte String getMimeType() {
        return "bpplication/binary";                  
	}
    
    /**
     * @return the bbndwidth for uploads in bytes per second
     */
    privbte static float getUploadSpeed() {
	    // if the user chose not to limit his uplobds
	    // by setting the uplobd speed to unlimited
	    // set the uplobd speed to 3.4E38 bytes per second.
	    // This is de fbcto not limiting the uploads
	    int uSpeed = UplobdSettings.UPLOAD_SPEED.getValue();
	    flobt ret = ( uSpeed == 100 ) ? Float.MAX_VALUE : 
            // if the uplobds are limited, take messageUpstream
            // for ultrbpeers into account, - don't allow lower 
            // speeds thbn 1kb/s so uploads won't stall completely
            // if the user bccidently sets his connection speed 
            // lower thbn his message upstream
            Mbth.max(
                // connection speed is in kbits per second
                ConnectionSettings.CONNECTION_SPEED.getVblue() / 8.f 
                // uplobd speed is in percent
                * uSpeed / 100.f
                // reduced uplobd speed if we are an ultrapeer
                - RouterService.getConnectionMbnager()
                .getMebsuredUpstreamBandwidth()*1.f, 1.f )
	        // we need bytes per second
	        * 1024;
	    return ret;
    }
    
	public boolebn getCloseConnection() {
	    return fblse;
	}

	// overrides Object.toString
	public String toString() {
		return "NormblUploadState:\r\n"+
		       "File Nbme:  "+_fileName+"\r\n"+
		       "File Size:  "+_fileSize+"\r\n"+
		       "File Index: "+_index+"\r\n"+
		       "File Desc:  "+FILE_DESC;
	}	
}

