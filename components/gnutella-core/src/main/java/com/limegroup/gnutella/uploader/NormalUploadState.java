package com.limegroup.gnutella;

import java.io.*;
import java.util.Date;

/**
 * auth: rsoule
 * file: NormalUploadState.java
 * desc: an implementaiton of the UploadState interface
 *       for a normal upload situation.  it should send
 *       the appropriate header information, followed by 
 *       the actual file.
 */

public class NormalUploadState implements UploadState {

	private HTTPUploader _uploader;
	private OutputStream _ostream;	  
	private int _index;
	private String _filename;
	private int _fileSize;
	private FileInputStream _fis;
	private int _amountRead;
	private int _uploadBegin;

	/**
	 * This class implements a succesful upload version
	 * of the doUpload method.  It prepares a file, writes
	 * the appropriate header, then sends the file.
	 */
	public void doUpload(HTTPUploader uploader) throws IOException {

		_uploader = uploader;
		_uploader.setState(_uploader.UPLOADING);
		/* initialize the global variables */
		_ostream = _uploader.getOutputStream();
		_index = _uploader.getIndex();
		_filename = _uploader.getFileName();
		_fileSize = _uploader.getFileSize();
		_fis =  _uploader.getFileInputStream();
		_amountRead = _uploader.amountUploaded();
		_uploadBegin =  _uploader.getUploadBegin();

		/* prepare the file to be read */
		prepareFile();
		/* write the header information to the socket */
		writeHeader();
		/* write the file to the socket */
	    int c = -1;
        int available = 0;
        byte[] buf = new byte[1024];
		
        long a = _fis.skip(_uploadBegin);
        _amountRead+=a;
		_uploader.setAmountUploaded(_amountRead);
		
        SettingsManager manager=SettingsManager.instance();
        int speed=manager.getUploadSpeed();
        if (speed==100) {
            //Special case: upload as fast as possible
            while (true) {
				c = _fis.read(buf);
                if (c == -1)
                    break;
				_ostream.write(buf, 0, c);
                _amountRead += c;
				_uploader.setAmountUploaded(_amountRead);
            }

        } else {
            //Normal case: throttle uploads. Similar to above but we
            //sleep after sending data.
            final int cycleTime=1000;
        outerLoop:
            while (true) {
                //1. Calculate max upload bandwidth for this connection in
                // kiloBYTES/sec.  The user has specified a theoretical link 
				// bandwidth (in kiloBITS/s) and the percentage of this 
                // bandwidth to use for uploads. We divide this bandwidth 
                // equally among all the uploads in progress.  
				// TODO: if one connection isn't using all
                // the bandwidth, some coul get more.
                int theoreticalBandwidth=
				(int)(((float)manager.getConnectionSpeed())/8.f);

				// NEED TO CHANGE THIS!!!!!! THIS IS A HACK TO GET
				// COMPILING WORKING
				// int maxBandwidth
				//    =(int)(theoreticalBandwidth*((float)speed/100.)
				//(float)_uploadCount);
				int maxBandwidth = 1;

                // 2. Send burstSize bytes of data as fast as possible, 
				// recording the time to send this.  How big should 
				// burstSize be?  We want the total time to complete 
				// one send/sleep iteration to be about one second. 
                // (Any less is inefficient.  Any more might cause
                // the user to see erratic transfer speeds.)  So we send
                // 1000*maxBandwidth bytes.
                int burstSize=maxBandwidth*cycleTime;
                int burstSent=0;
                Date start=new Date();
                while (burstSent<burstSize) {
					c = _fis.read(buf);
                    if (c == -1)
                        break outerLoop;  //get out of BOTH loops
					_ostream.write(buf, 0, c);
                    _amountRead += c;
					_uploader.setAmountUploaded(_amountRead);
                    burstSent += c;
                }

                Date stop=new Date();

                //3.  Pause as needed so as not to exceed maxBandwidth.
                int elapsed=(int)(stop.getTime()-start.getTime());
                int sleepTime=cycleTime-elapsed;
                if (sleepTime>0) {
                    try {
                        Thread.currentThread().sleep(sleepTime);
                    } catch (InterruptedException e) { 
						throw new IOException();
					}
                }

            }
        }

		_uploader.setState(_uploader.COMPLETE);
		_ostream.close();
	}

	/************************* PRIVATE METHODS ***********************/
	
	/**
	 * prepares the file to be read for sending accross the socket
	 */
	private void prepareFile() throws IOException {
		// get the appropriate file descriptor
		FileDesc fdesc;
		try {
			fdesc = FileManager.instance().get(_index);
		} catch (IndexOutOfBoundsException e) {
			throw new IOException();
		}
		
		/* For regular (client-side) uploads, get name. 
		 * For pushed (server-side) uploads, check to see that 
		 * the index matches the filename. */
		String name = fdesc._name;
		if (_filename == null) {
            _filename = name;
        } else {
			/* matches the name */
           if ( !name.equals(_filename) )  
               throw new IOException();
        }

		// set the file size
        _fileSize = fdesc._size;

		// get the fileInputStream
		String path = fdesc._path;
		File myFile = new File(path);
		_fis = new FileInputStream(myFile);

	}

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
		String version = SettingsManager.instance().getCurrentVersion();
		str = "Server: LimeWire " + version + " \r\n";
		_ostream.write(str.getBytes());
		String type = getMimeType();       /* write this method later  */
		str = "Content-type:" + type + "\r\n";
		_ostream.write(str.getBytes());
		str = "Content-length:"+ (_fileSize - _uploadBegin) + "\r\n";
		_ostream.write(str.getBytes());
		
		// Version 0.5 of limewire misinterpreted Content-range
		// to be 1 - n instead of 0 - (n-1), but because this is
		// an optional field in the regular case, we don't need
		// to send it.
		if (_uploadBegin != 0) {
			str = "Content-range: bytes=" + _uploadBegin  +
			"-" + ( _fileSize - 1 )+ "/" + _fileSize + "\r\n";
			_ostream.write(str.getBytes());
		}
		str = "\r\n";
		_ostream.write(str.getBytes());
		
	}
}


