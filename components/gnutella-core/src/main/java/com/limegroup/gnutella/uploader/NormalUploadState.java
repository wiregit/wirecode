package com.limegroup.gnutella.uploader;

import com.limegroup.gnutella.*;
import java.io.*;
import java.util.Date;
import com.limegroup.gnutella.util.CommonUtils;

/**
 * An implementation of the UploadState interface for a normal upload situation,
 * i.e., the real uploader.  It should send the appropriate header information,
 * followed by the actual file.  
 */
public class NormalUploadState implements UploadState {
    /** The amount of time that a send/wait cycle should take for throttled
     *  uploads.  This should be short enough to not be noticeable in the GUI,
     *  but long enough so that waits are not called so often as to be
     *  inefficient. */
    public static final int CYCLE_TIME=1000;
	private HTTPUploader _uploader;
	private OutputStream _ostream;	  
	private int _index;
	private String _filename;
	private int _fileSize;
	private InputStream _fis;
	private int _amountRead;
    /** @see HTTPUploader#getUploadBegin */
	private int _uploadBegin;
    /** @see HTTPUploader#getUploadEnd */
    private int _uploadEnd;

    /** Flag indicating whether we should close the connection after serving
     * the request or not */
    private boolean _closeConnection = false;
    
	/**
	 * This class implements a succesful upload version
	 * of the doUpload method.  It prepares a file, writes
	 * the appropriate header, then sends the file.
	 */
	public void doUpload(HTTPUploader uploader) throws IOException {
        try {
            _uploader = uploader;
            _uploader.setState(_uploader.UPLOADING);
            /* initialize the global variables */
            _ostream = _uploader.getOutputStream();
            _index = _uploader.getIndex();
            _filename = _uploader.getFileName();
            _fileSize = _uploader.getFileSize();
            _fis =  _uploader.getInputStream();
            _amountRead = _uploader.amountUploaded();
            _uploadBegin =  _uploader.getUploadBegin();
            _uploadEnd =  _uploader.getUploadEnd();
            
            //guard clause
            if(_fileSize < _uploadBegin)
                throw new IOException("Invalid Range");
            
            //if invalid end-index, then upload upto the end of file
            if(_uploadEnd <= 0 
                || _uploadEnd <= _uploadBegin 
                || _uploadEnd > _fileSize)
                _uploadEnd = _fileSize;

            /* write the header information to the socket */
            writeHeader();
            /* write the file to the socket */
            int c = -1;
            byte[] buf = new byte[1024];

            long a = _fis.skip(_uploadBegin);
            _amountRead+=a;
            _uploader.setAmountUploaded(_amountRead);

            SettingsManager manager=SettingsManager.instance();
            int speed=manager.getUploadSpeed();
            if (speed==100) {
                //Special case: upload as fast as possible
                uploadUnthrottled();
            } else {
                //Normal case: throttle uploads. Similar to above but we
                //sleep after sending data.
                uploadThrottled();
            }
        } catch(IOException ioe) {
            //set the connection to be closed, in case of IO exception
            _closeConnection = true;
            throw ioe;
        }

        _uploader.setState(_uploader.COMPLETE);
	}

    /**
     * Uploads the file at maximum rate possible
     * @exception IOException If there is any I/O problem while uploading file
     */
    private void uploadUnthrottled() throws IOException {
        int c = -1;
        byte[] buf = new byte[1024];
        
        while (true) {
            c = _fis.read(buf);
            if (c == -1)
                break;
            //dont upload more than asked
            if( c > (_uploadEnd - _amountRead))
                c = _uploadEnd - _amountRead;
            try {
                _ostream.write(buf, 0, c);
            } catch (java.net.SocketException e) {
                throw new IOException();
            }
            _amountRead += c;
            _uploader.setAmountUploaded(_amountRead);

            //finish uploading if the desired amount has been uploaded
            if(_amountRead >= _uploadEnd)
                break;
        }
    }
    
    /**
     * Throttles the uploads by sleeping periodically
     * @exception IOException If there is any I/O problem while uploading file
     */
    private void uploadThrottled() throws IOException {
        while (true) {
            // int max = _uploader.getManager().calculateBurstSize();
            int max = _uploader.getManager().calculateBandwidth();
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
                    if( c > (_uploadEnd - _amountRead))
                        c = _uploadEnd - _amountRead;
                try {
                    _ostream.write(buf, 0, c);
                } catch (java.net.SocketException e) {
                    throw new IOException();
                }
                _amountRead += c;
                _uploader.setAmountUploaded(_amountRead);
                burstSent += c;
                //finish uploading if the desired amount 
                //has been uploaded
                if(_amountRead >= _uploadEnd)
                    break;
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
    
	/************************* PRIVATE METHODS ***********************/
	
	/**
	 * prepares the file to be read for sending accross the socket
	 */
//  	private void prepareFile() throws IOException {
//  		// get the appropriate file descriptor
//  		FileDesc fdesc;
//  		try {
//  			fdesc = FileManager.instance().get(_index);
//  		} catch (IndexOutOfBoundsException e) {
//  			throw new IOException();
//  		}
		
//  		/* For regular (client-side) uploads, get name. 
//  		 * For pushed (server-side) uploads, check to see that 
//  		 * the index matches the filename. */
//  		String name = fdesc._name;
//  		if (_filename == null) {
//              _filename = name;
//          } else {
//  			/* matches the name */
//  			if ( !name.equals(_filename) ) {
//  				throw new IOException();
//  			}
//          }

//  		// set the file size
//          _fileSize = fdesc._size;

//  		// get the fileInputStream
//  		String path = fdesc._path;
//  		File myFile = new File(path);
//  		_fis = new FileInputStream(myFile);

//  	}

	/** eventually this method should determine the 
	 * mime type of a file fill in the details of 
	 * this later. assume binary for now */
	private String getMimeType() {
		String mimetype;                
        mimetype = "application/binary"; 
        return mimetype;                  
	}

	/**
	 * writes the appropriate header information to the socket
	 */
	private void writeHeader() throws IOException {
		String str;
		str = "HTTP 200 OK \r\n";
		_ostream.write(str.getBytes());
		str = "Server: "+CommonUtils.getVendor()+"\r\n";
		_ostream.write(str.getBytes());
		String type = getMimeType();       /* write this method later  */
		str = "Content-type:" + type + "\r\n";
		_ostream.write(str.getBytes());
		str = "Content-length:"+ (_uploadEnd - _uploadBegin) + "\r\n";
		_ostream.write(str.getBytes());
		
		// Version 0.5 of limewire misinterpreted Content-range
		// to be 1 - n instead of 0 - (n-1), but because this is
		// an optional field in the regular case, we don't need
		// to send it.
        // 
        // Earlier version of LimeWire mistakenly sent "bytes=" instead of
        // "bytes ".  Thankfully most clients understand both.
		if (_uploadBegin != 0) {
			str = "Content-range: bytes " + _uploadBegin  +
			"-" + ( _fileSize - 1 )+ "/" + _fileSize + "\r\n";
			_ostream.write(str.getBytes());
		}
		 str = "\r\n";
		_ostream.write(str.getBytes());
		
	}
    
    //inherit doc comment
    public boolean getCloseConnection()
    {
        return _closeConnection;
    }
    
}


