package com.limegroup.gnutella.uploader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.BandwidthThrottle;

import com.limegroup.gnutella.CreationTimeCache;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UploadManager;
import com.limegroup.gnutella.http.ConstantHTTPHeaderValue;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HTTPUtils;

/**
 * An implementation of the UploadState interface for a normal upload situation,
 * i.e., the real uploader.  It should send the appropriate header information,
 * followed by the actual file.  
 */
public final class NormalUploadState extends UploadState {
    /** The amount of time that a send/wait cycle should take for throttled
     *  uploads.  This should be short enough to not be noticeable in the GUI,
     *  but long enough so that waits are not called so often as to be
     *  inefficient. */
    private static final Log LOG = LogFactory.getLog(NormalUploadState.class);
	
    private static final int BLOCK_SIZE=1024;
	
	private final int _index;
	private final String _fileName;
	private final int _fileSize;
	private InputStream _fis;
	private int _amountWritten;
    /** @see HTTPUploader#getUploadBegin */
	private int _uploadBegin;
    /** @see HTTPUploader#getUploadEnd */
    private int _uploadEnd;
    private int _amountRequested;
    
    /**
     * The task that periodically checks to see if the uploader has stalled.
     */
    private StalledUploadWatchdog _stalledChecker;

    /**
     * Throttle for the speed of uploads.  The rate will get dynamically
     * reset during the upload if the rate changes.
     */
    private static final BandwidthThrottle THROTTLE = 
        new BandwidthThrottle(UploadManager.getUploadSpeed(), false);
        
    /**
     * UDP throttle.
     */
    private static final BandwidthThrottle UDP_THROTTLE =
        new BandwidthThrottle(UploadManager.getUploadSpeed(), false);


	/**
	 * Constructs a new <tt>NormalUploadState</tt>, establishing all 
	 * invariants.
	 *
	 * @param uploaded the <tt>HTTPUploader</tt>
	 */
	public NormalUploadState(HTTPUploader uploader, 
                                    StalledUploadWatchdog watchdog) {
		super(uploader);

		LOG.debug("creating a normal upload state");

		
		
		_index = UPLOADER.getIndex();	
		_fileName = UPLOADER.getFileName();
		_fileSize = (int)UPLOADER.getFileSize();

		_amountWritten = 0;
		_stalledChecker = watchdog; //new StalledUploadWatchdog();
 	}
 	
 	public static void setThrottleSwitching(boolean on) {
 	    THROTTLE.setSwitching(on);
    }
    
	public void writeMessageHeaders(OutputStream network) throws IOException {
		LOG.debug("writing message headers");
		try {
		    Writer ostream = new StringWriter();
			_fis =  UPLOADER.getInputStream();
			_uploadBegin =  UPLOADER.getUploadBegin();
			_uploadEnd =  UPLOADER.getUploadEnd();
			_amountRequested = (int)UPLOADER.getAmountRequested();
			//guard clause
			if(_fileSize < _uploadBegin)
				throw new IOException("Invalid Range");
			
            
			// Initial OK	
			if( _uploadBegin==0 && _amountRequested==_fileSize ) {
				ostream.write("HTTP/1.1 200 OK\r\n");
			} else {
				ostream.write("HTTP/1.1 206 Partial Content\r\n");
			}
			
            HTTPUtils.writeHeader(HTTPHeaderName.SERVER, ConstantHTTPHeaderValue.SERVER_VALUE, ostream);
            HTTPUtils.writeHeader(HTTPHeaderName.CONTENT_TYPE, getMimeType(), ostream);
            HTTPUtils.writeHeader(HTTPHeaderName.CONTENT_LENGTH, _amountRequested, ostream);
            HTTPUtils.writeDate(ostream);
            HTTPUtils.writeContentDisposition(_fileName, ostream);
			
			// _uploadEnd is an EXCLUSIVE index internally, but HTTP uses an INCLUSIVE index.
			if (_uploadBegin != 0 || _amountRequested != _fileSize) {
			    ostream.write("Content-Range: bytes " + _uploadBegin  +
				    "-" + ( _uploadEnd - 1 )+ "/" + _fileSize + "\r\n");
			}
			
			writeAlts(ostream);
			writeRanges(ostream);
			writeProxies(ostream);
			
			if(FILE_DESC != null) {
				URN urn = FILE_DESC.getSHA1Urn();
				
                if (UPLOADER.isFirstReply()) {
                    // write the creation time if this is the first reply.
                    // if this is just a continuation, we don't need to send
                    // this information again.
                    // it's possible t do that because we don't use the same
                    // uploader for different files
                    CreationTimeCache cache = CreationTimeCache.instance();
                    if (cache.getCreationTime(urn) != null)
                        HTTPUtils.writeHeader(
                            HTTPHeaderName.CREATION_TIME,
                            cache.getCreationTime(urn).toString(),
                            ostream);
                }
            }
            
            // write x-features header once because the downloader is
            // supposed to cache that information anyway
            if (UPLOADER.isFirstReply())
                HTTPUtils.writeFeatures(ostream);

            // write X-Thex-URI header with root hash if we have already 
            // calculated the tigertree
            if (FILE_DESC.getHashTree()!=null)
                HTTPUtils.writeHeader(HTTPHeaderName.THEX_URI, FILE_DESC.getHashTree(), ostream);
            
			ostream.write("\r\n");
			
			_stalledChecker.activate(network);			
			network.write(ostream.toString().getBytes());
        } finally {
			// we do not need to check the return value because
			// if it was stalled, an IOException would have been thrown
			// causing us to fall out to the catch clause
			_stalledChecker.deactivate();
		} 
	}

	public void writeMessageBody(OutputStream ostream) throws IOException {
		LOG.debug("writing message body");
        try {            
            _fis.skip(_uploadBegin);
            upload(ostream);
        } catch(IOException e) {
            _stalledChecker.deactivate(); // no need to kill now
            throw e;
        }
	}

    /**
     * Upload the file, throttling the upload by making use of the
     * BandwidthThrottle class
     * @exception IOException If there is any I/O problem while uploading file
     */
    private void upload(OutputStream ostream) throws IOException {
        // construct the buffer outside of the loop, so we don't
        // have to reconstruct new byte arrays every BLOCK_SIZE.
        byte[] buf = new byte[BLOCK_SIZE];
        while (true) {
            BandwidthThrottle throttle =
                UPLOADER.isUDPTransfer() ? UDP_THROTTLE : THROTTLE;
            throttle.setRate(UploadManager.getUploadSpeed());

            int c = -1;
            // request the bytes from the throttle
            // BLOCKING (only if we need to throttle)
            int allowed = BLOCK_SIZE;
            if(!UPLOADER.isForcedShare())
                allowed = throttle.request(BLOCK_SIZE);
            int burstSent=0;
            try {
                c = _fis.read(buf, 0, allowed);
            } catch(NullPointerException npe) {
                // happens occasionally :(
                throw new IOException(npe.getMessage());
            }
            if (c == -1)
                return;
            //dont upload more than asked
            if( c > (_amountRequested - _amountWritten))
                c = _amountRequested - _amountWritten;
            _stalledChecker.activate(ostream);
            ostream.write(buf, 0, c);
			// we do not need to check the return value because
			// if it was stalled, an IOException would have been thrown
			// causing us to exit immediately.
			_stalledChecker.deactivate();
            
            _amountWritten += c;
            UPLOADER.setAmountUploaded(_amountWritten);
            burstSent += c;           
            //finish uploading if the desired amount 
            //has been uploaded
            if(_amountWritten >= _amountRequested)
                return;
        }
            
    }

	/** 
	 * Eventually this method should determine the mime type of a file fill 
	 * in the details of this later. Assume binary for now. 
	 */
	private String getMimeType() {
        return "application/binary";                  
	}
    
	public boolean getCloseConnection() {
	    return false;
	}

	// overrides Object.toString
	public String toString() {
		return "NormalUploadState:\r\n"+
		       "File Name:  "+_fileName+"\r\n"+
		       "File Size:  "+_fileSize+"\r\n"+
		       "File Index: "+_index+"\r\n"+
		       "File Desc:  "+FILE_DESC;
	}	
}

