package com.limegroup.gnutella.uploader;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.http.*;
import com.sun.java.util.collections.*;
import java.io.*;
import java.util.Date;
import com.limegroup.gnutella.util.CommonUtils;

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
    public static final int CYCLE_TIME=1000;
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
					HTTPUtils.writeHeader(HTTPHeaderName.CONTENT_URN, 
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
            int c = -1;
            byte[] buf = new byte[1024];

            long a = _fis.skip(_uploadBegin);
            //_amountRead+=a;
            //_uploader.setAmountUploaded(_amountRead);

            SettingsManager manager=SettingsManager.instance();
            int speed=manager.getUploadSpeed();
            if (speed==100) {
                //Special case: upload as fast as possible
                uploadUnthrottled(ostream);
            } else {
                //Normal case: throttle uploads. Similar to above but we
                //sleep after sending data.
                uploadThrottled(ostream);
            }
        } catch(IOException e) {
			// TODO: set to a state other than UPLOADING????
            //set the connection to be closed, in case of IO exception
            _closeConnection = true;
            throw e;
        }

        _uploader.setState(_uploader.COMPLETE);
	}

    /**
     * Uploads the file at maximum rate possible
     * @exception IOException If there is any I/O problem while uploading file
     */
    private void uploadUnthrottled(OutputStream ostream) throws IOException {
        int c = -1;
        byte[] buf = new byte[1024];
        
        while (true) {
            c = _fis.read(buf);
            if (c == -1)
                break;
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

            //finish uploading if the desired amount has been uploaded
            if(_amountRead >= _amountRequested)
                break;
        }
    }
	
    
    /**
     * Throttles the uploads by sleeping periodically
     * @exception IOException If there is any I/O problem while uploading file
     */
    private void uploadThrottled(OutputStream ostream) throws IOException {
        while (true) {
			int max = RouterService.getUploadManager().calculateBandwidth();
            int burstSize=max*CYCLE_TIME;

            int c = -1;
            byte[] buf = new byte[1024];
            int burstSent=0;
            // Date start=new Date();
            long start = System.currentTimeMillis();
            while (burstSent<burstSize) {
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
            
            // Date stop=new Date();
            long stop = System.currentTimeMillis();

            //3.  Pause as needed so as not to exceed maxBandwidth.
            // int elapsed=(int)(stop.getTime()-start.getTime());
            int elapsed=(int)(stop-start);
            int sleepTime=CYCLE_TIME-elapsed;
            if (sleepTime>0) {
                try {
                    Thread.currentThread().sleep(sleepTime);
                } catch (InterruptedException e) { 
                    throw new IOException();
                }
            }
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


	// overrides Object.toString
	public String toString() {
		return "NormalUploadState:\r\n"+
		       "File Name:  "+_fileName+"\r\n"+
		       "File Size:  "+_fileSize+"\r\n"+
		       "File Index: "+_index+"\r\n"+
		       "File Desc:  "+_fileDesc;
	}    
}


