package com.limegroup.gnutella.uploader;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.http.*;
import com.sun.java.util.collections.*;
import java.io.*;
import java.util.Date;
import com.limegroup.gnutella.util.CommonUtils;
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
	private final FileDesc _fileDesc;

    /** 
	 * Flag indicating whether we should close the connection after serving
     * the request or not.
	 */
    private boolean _closeConnection = false;

	/**
	 * Constant for the MIME type to return.
	 */
	private final String MIME_TYPE = "application/binary";

	/**
	 * Constructs a new <tt>NormalUploadState</tt>, establishing all 
	 * invariants.
	 *
	 * @param uploaded the <tt>HTTPUploader</tt>
	 */
	public NormalUploadState(HTTPUploader uploader, 
                                    StalledUploadWatchdog watchdog) {
		_uploader = uploader;
		_fileDesc = _uploader.getFileDesc();
		_index = _uploader.getIndex();	
		_fileName = _uploader.getFileName();
		_fileSize = _uploader.getFileSize();
		_amountWritten = 0;
		_stalledChecker = watchdog; //new StalledUploadWatchdog();
 	}
    
	public void writeMessageHeaders(OutputStream network) throws IOException {
		try {
		    StringWriter ostream = new StringWriter();
			_uploader.setState(Uploader.UPLOADING);
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
			ostream.write("Server: " + CommonUtils.getHttpServer() + "\r\n");

            // Content Type
            ostream.write("Content-Type: " + getMimeType() + "\r\n");
            
            // Content Length
			ostream.write("Content-Length: "+ _amountRequested + "\r\n");
			
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
			if (_uploadBegin != 0) {
			    ostream.write("Content-Range: bytes " + _uploadBegin  +
				    "-" + ( _uploadEnd - 1 )+ "/" + _fileSize + "\r\n");
			}
			if(_fileDesc != null) {
				URN urn = _fileDesc.getSHA1Urn();
				if(urn != null) {
					HTTPUtils.writeHeader(HTTPHeaderName.GNUTELLA_CONTENT_URN, 
										  urn,
										  ostream);
				}
				if(_fileDesc.hasAlternateLocations()) {
					HTTPUtils.writeHeader(HTTPHeaderName.ALT_LOCATION,
										  _fileDesc.getAlternateLocationCollection(),
										  ostream);
				}
			}
			
			ostream.write("\r\n");
			
			_stalledChecker.activate(network);			
			network.write(ostream.toString().getBytes());			
			if( _stalledChecker.deactivate() )
			    throw new IOException("stalled uploader");
			
			_uploader.setState(_uploader.COMPLETE);
		} catch(IOException e) {
		    _stalledChecker.deactivate(); // no need to kill now.
            //set the connection to be closed, in case of IO exception
            _closeConnection = true;
            throw e;
		} 
	}

	public void writeMessageBody(OutputStream ostream) throws IOException {
        try {            
			_uploader.setState(Uploader.UPLOADING);
            long a = _fis.skip(_uploadBegin);            
            upload(ostream);
        } catch(IOException e) {
            _stalledChecker.deactivate(); // no need to kill now
            _closeConnection = true;
            throw e;
        }

        _uploader.setState(_uploader.COMPLETE);
	}

    /**
     * Upload the file, throttling the upload by making use of the
     * BandwidthThrottle class
     * @exception IOException If there is any I/O problem while uploading file
     */
    private void upload(OutputStream ostream) throws IOException {
        while (true) {
            THROTTLE.setRate(getUploadSpeed());

            int c = -1;
            byte[] buf = new byte[THROTTLE.request(BLOCK_SIZE)];
            int burstSent=0;            
            c = _fis.read(buf);
            if (c == -1)
                return;
            //dont upload more than asked
            if( c > (_amountRequested - _amountWritten))
                c = _amountRequested - _amountWritten;
            try {
                _stalledChecker.activate(ostream);
                ostream.write(buf, 0, c);
                // if it closed the stream
                if( _stalledChecker.deactivate() )
                    throw new IOException("stalled uploader");
            } catch (java.net.SocketException e) {
                throw new IOException("socketexception");
            }
            
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
    
    //inherit doc comment
    public boolean getCloseConnection() {
        return _closeConnection;
    }
    
    /**
     * @return the bandwidth for uploads in bytes per second
     */
    public static float getUploadSpeed() {
	    // if the user chose not to limit his uploads
	    // by setting the upload speed to unlimited
	    // set the upload speed to 3.4E38 bytes per second.
	    // This is de facto not limiting the uploads
	    int uSpeed = UploadSettings.UPLOAD_SPEED.getValue();
	    float ret = ( uSpeed == 100 ) ? Float.MAX_VALUE : 
	        // connection speed is in kbits per second
	        SettingsManager.instance().getConnectionSpeed() / 8.f 
	        // upload speed is in percent
	        * uSpeed / 100.f
	        // wee need bytes per second
	        * 1024;
	    return ret;
    }

	// overrides Object.toString
	public String toString() {
		return "NormalUploadState:\r\n"+
		       "File Name:  "+_fileName+"\r\n"+
		       "File Size:  "+_fileSize+"\r\n"+
		       "File Index: "+_index+"\r\n"+
		       "File Desc:  "+_fileDesc;
	}	
}

