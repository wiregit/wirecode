package com.limegroup.gnutella.uploader;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.http.*;
import com.sun.java.util.collections.*;
import java.io.*;
import com.limegroup.gnutella.util.BandwidthThrottle;

/**
 * An implementation of the UploadState interface for a normal upload situation,
 * i.e., the real uploader.  It should send the appropriate header information,
 * followed by the actual file.  
 */
public final class NormalUploadState implements HTTPMessage {
    /** The amount of time that a send/wait cycle should take for throttled
     *  uploads.  This should be short enough to not be noticeable in the GUI,
     *  but long enough so that waits are not called so often as to be
     *  inefficient. */
    private static final int BLOCK_SIZE=1024;
	private final HTTPUploader _uploader;
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
        new BandwidthThrottle(getUploadSpeed());        

	/**
	 * <tt>FileDesc</tt> instance for the file being uploaded.
	 */
	private final FileDesc FILE_DESC;

	/**
	 * Constructs a new <tt>NormalUploadState</tt>, establishing all 
	 * invariants.
	 *
	 * @param uploaded the <tt>HTTPUploader</tt>
	 */
	public NormalUploadState(HTTPUploader uploader, 
                                    StalledUploadWatchdog watchdog) {
		_uploader = uploader;
		FILE_DESC = _uploader.getFileDesc();
		_index = _uploader.getIndex();	
		_fileName = _uploader.getFileName();
		_fileSize = _uploader.getFileSize();
		_amountWritten = 0;
		_stalledChecker = watchdog; //new StalledUploadWatchdog();
 	}
    
	public void writeMessageHeaders(OutputStream network) throws IOException {
		try {
		    Writer ostream = new StringWriter();
			_fis =  _uploader.getInputStream();
			_uploadBegin =  _uploader.getUploadBegin();
			_uploadEnd =  _uploader.getUploadEnd();
			_amountRequested = _uploader.getAmountRequested();
			//guard clause
			if(_fileSize < _uploadBegin)
				throw new IOException("Invalid Range");
			
            
			// Initial OK	
			if( _uploadBegin==0 && _amountRequested==_fileSize ) {
				ostream.write("HTTP/1.1 200 OK\r\n");
			} else {
				ostream.write("HTTP/1.1 206 Partial Content\r\n");
			}
			
			// Server
            HTTPUtils.writeHeader(HTTPHeaderName.SERVER, 
                ConstantHTTPHeaderValue.SERVER_VALUE, ostream);
            
            // Content Type
            HTTPUtils.writeHeader(HTTPHeaderName.CONTENT_TYPE, 
                getMimeType(), ostream);
            
            // Content Length
            HTTPUtils.writeHeader(HTTPHeaderName.CONTENT_LENGTH, 
                _amountRequested, ostream);
            
            // Date
            HTTPUtils.writeDate(ostream);
			
			// Version 0.5 of limewire misinterpreted Content-range
			// to be 1 - n instead of 0 - (n-1), but because this is
			// an optional field in the regular case, we don't need
			// to send it.
			// 
			// Earlier version of LimeWire mistakenly sent "bytes=" instead of
			// "bytes ".  Thankfully most clients understand both.
			//
			// _uploadEnd is an EXCLUSIVE index internally, but HTTP uses
			// an INCLUSIVE index.
			if (_uploadBegin != 0 || _amountRequested != _fileSize) {
			    ostream.write("Content-Range: bytes " + _uploadBegin  +
				    "-" + ( _uploadEnd - 1 )+ "/" + _fileSize + "\r\n");
			}
			if(FILE_DESC != null) {
				URN urn = FILE_DESC.getSHA1Urn();
				if(urn != null) {
					HTTPUtils.writeHeader(HTTPHeaderName.GNUTELLA_CONTENT_URN, 
										                          urn, ostream);
				}
                Set alts = _uploader.getNextSetOfAltsToSend();
				if(alts.size() > 0) {
					HTTPUtils.writeHeader(HTTPHeaderName.ALT_LOCATION,
                                          new HTTPHeaderValueCollection(alts),
                                          ostream);
				}
                if (FILE_DESC instanceof IncompleteFileDesc) {
                    HTTPUtils.writeHeader(HTTPHeaderName.AVAILABLE_RANGES,
                                          ((IncompleteFileDesc)FILE_DESC),
                                          ostream);
                }
                if (_uploader.isFirstReply()) {
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
            if (_uploader.isFirstReply())
                HTTPUtils.writeFeatures(ostream);

            // write X-Thex-URI header with root hash if we have already 
            // calculated the tigertree
            if (FILE_DESC.getHashTree()!=null)
                HTTPUtils.writeHeader(HTTPHeaderName.THEX_URI,
                                      FILE_DESC.getHashTree(),
                                      ostream);
            
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
            THROTTLE.setRate(getUploadSpeed());

            int c = -1;
            // request the bytes from the throttle
            // BLOCKING (only if we need to throttle)
            int allowed = THROTTLE.request(BLOCK_SIZE);
            int burstSent=0;            
            c = _fis.read(buf, 0, allowed);
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
            _uploader.setAmountUploaded(_amountWritten);
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
    
    /**
     * @return the bandwidth for uploads in bytes per second
     */
    private static float getUploadSpeed() {
	    // if the user chose not to limit his uploads
	    // by setting the upload speed to unlimited
	    // set the upload speed to 3.4E38 bytes per second.
	    // This is de facto not limiting the uploads
	    int uSpeed = UploadSettings.UPLOAD_SPEED.getValue();
	    float ret = ( uSpeed == 100 ) ? Float.MAX_VALUE : 
            // if the uploads are limited, take messageUpstream
            // for ultrapeers into account, - don't allow lower 
            // speeds than 1kb/s so uploads won't stall completely
            // if the user accidently sets his connection speed 
            // lower than his message upstream
            Math.max(
                // connection speed is in kbits per second
                ConnectionSettings.CONNECTION_SPEED.getValue() / 8.f 
                // upload speed is in percent
                * uSpeed / 100.f
                // reduced upload speed if we are an ultrapeer
                - RouterService.getConnectionManager()
                .getMeasuredUpstreamBandwidth()*1.25f, 1.f )
	        // we need bytes per second
	        * 1024;
	    return ret;
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

