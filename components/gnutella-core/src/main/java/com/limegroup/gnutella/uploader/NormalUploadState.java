package com.limegroup.gnutella.uploader;

import com.limegroup.gnutella.*;
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
	private int _amountRead;
    /** @see HTTPUploader#getUploadBegin */
	private int _uploadBegin;
    /** @see HTTPUploader#getUploadEnd */
    private int _uploadEnd;
    private int _amountRequested;
    private static BandwidthThrottle _throttle = null;

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
	public NormalUploadState(HTTPUploader uploader) {
		_uploader = uploader;
		_fileDesc = _uploader.getFileDesc();
		_index = _uploader.getIndex();	
		_fileName = _uploader.getFileName();
		_fileSize = _uploader.getFileSize();
        if (_throttle == null)
            _throttle = new BandwidthThrottle(getUploadSpeed());
 	}
    
	public void writeMessageHeaders(OutputStream ostream) throws IOException {
		try {
			_uploader.setState(Uploader.UPLOADING);
			_fis =  _uploader.getInputStream();
			_amountRead = _uploader.amountUploaded();
			_uploadBegin =  _uploader.getUploadBegin();
			_uploadEnd =  _uploader.getUploadEnd();
			_amountRequested = _uploader.getAmountRequested();
			//guard clause
			if(_fileSize < _uploadBegin)
				throw new IOException("Invalid Range");
		    
			String str;
			if( _uploadBegin==0 && _amountRequested==_fileSize ) {
				str = "HTTP/1.1 200 OK\r\n";
			} else {
				str = "HTTP/1.1 206 Partial Content\r\n";
			}
			ostream.write(str.getBytes());
			str = "Server: "+CommonUtils.getHttpServer()+"\r\n";
			ostream.write(str.getBytes());
			String type = getMimeType();       // write this method later  
			str = "Content-Type: " + type + "\r\n";
			ostream.write(str.getBytes());
			str = "Content-Length: "+ (_amountRequested) + "\r\n";
			ostream.write(str.getBytes());
			
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
				str = "Content-Range: bytes " + _uploadBegin  +
				"-" + ( _uploadEnd - 1 )+ "/" + _fileSize + "\r\n";
				ostream.write(str.getBytes());
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
			
			str = "\r\n";
			ostream.write(str.getBytes());
			_uploader.setState(_uploader.COMPLETE);
		} catch(IOException e) {
            //set the connection to be closed, in case of IO exception
            _closeConnection = true;
            throw e;
		} 
	}

	public void writeMessageBody(OutputStream ostream) throws IOException {
        try {            
			_uploader.setState(Uploader.UPLOADING);
            // write the file to the socket 
            //int c = -1;
            //byte[] buf = new byte[1024];

            long a = _fis.skip(_uploadBegin);
            //_amountRead+=a;
            //_uploader.setAmountUploaded(_amountRead);

            //SettingsManager manager=SettingsManager.instance();
            upload(ostream);
        } catch(IOException e) {
			// TODO: set to a state other than UPLOADING????
            //set the connection to be closed, in case of IO exception
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
            _throttle.setRate(getUploadSpeed());

            int c = -1;
            byte[] buf = new byte[_throttle.request(BLOCK_SIZE)];
            int burstSent=0;            
            c = _fis.read(buf);
            if (c == -1)
                return;
            //dont upload more than asked
            if( c > (_amountRequested - _amountRead))
                c = _amountRequested - _amountRead;
            try {
                ostream.write(buf, 0, c);
            } catch (java.net.SocketException e) {
                throw new IOException();
            }			
            _amountRead += c;
            _uploader.setAmountUploaded(_amountRead);
            burstSent += c;           
            //finish uploading if the desired amount 
            //has been uploaded
            if(_amountRead >= _amountRequested)
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
    public float getUploadSpeed() {
	    // if the user chose not to limit his uploads
	    // by setting the upload speed to unlimited
	    // set the upload speed to 3.4E38 bytes per second.
	    // This is de facto not limiting the uploads
	    int uSpeed = SettingsManager.instance().getUploadSpeed();
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


