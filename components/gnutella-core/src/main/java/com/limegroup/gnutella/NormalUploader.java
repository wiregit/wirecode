/**
 * auth: rsoule
 * file: HTTPUploader.java
 * desc: Read data from disk and write to the net.
 *
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|

package com.limegroup.gnutella;

import java.io.*;
import java.net.*;
import java.util.Date;
import com.sun.java.util.collections.*;

public class NormalUploader extends HTTPUploader {

	private int BUFFSIZE = 1024;

	private Socket _socket;
	private OutputStream _ostream;
	private FileInputStream _fis;

	private int _amountRead;
	private int _uploadBegin;
	private int _fileSize;
	private double _rate;// not sure if i still want this 

	private int _index;
	private String _filename;

	// not sure if i still want this variable
	private int _uploadCount = 0; 
	
	/****************** Constructors ***********************/
	/**
	 * There are two constructors that are necessary.  The 
	 * first is to handle the case where there is a regular
	 * upload.  in that case, the manager class has already
	 * processed a message that looks like: 
	 * GET /get/0/sample.txt HTTP/1.0
	 * and already given a socket connection.  all that we
	 * need to do is actually upload the file to the socket.
	 *
	 * In the second case, we have recieved a push request,
	 * so we are going to need to establish the connection
	 * on this end.  We do this by creating the socket, and
	 * then writing out the GIV 0:99999999/sample.txt
	 * and then wait for the GET to come back.
	 */

	// Regular upload
	public NormalUploader(String file, Socket s, int index,
						 int begin) throws IOException {
		_socket = s;
		_filename = file;
		_index = index;
		_uploadBegin = begin;
		_amountRead = 0;
	}

	// Push requested Upload
	public NormalUploader(String file, String host, int port, int index,
						 String guid) throws IOException {

		// NOTE: Do we know the name of the file?  Can this be
		// passed here? Or do we just know the index?

		_socket = new Socket(host, port);
		_filename = file;
		_index = index;
		_uploadBegin = 0;
		_amountRead = 0;
		
		// try to create the socket.
		try {
			_socket = new Socket(host, port);
		} catch (SecurityException e) {
			throw new IOException();
		}
		try {
			// open a stream for writing to the socket
			_ostream = _socket.getOutputStream();
			// ask chris about Assert
			Assert.that(_filename != null);  
			// write out the giv
			String giv; 
			giv = "GIV " + _index + ":" + guid + "/" + _filename + "\n\n";
			_ostream.write(giv.getBytes());
			_ostream.flush();
			
			// Wait to recieve the GET
			InputStream istream = _socket.getInputStream(); 
			ByteReader in = new ByteReader(istream);
			// set a time out for how long to wait for the push
			int time = SettingsManager.instance().getTimeout();
			_socket.setSoTimeout(time);
			
			// read directly from the socket
			String str;
			str = in.readLine();
			// not sure why we set this to zero, if we set it above
			_socket.setSoTimeout(0);
			
			// check the line, to see what was read.
			if (str == null)
				throw new IOException();
			// check for the 'GET'
			if (! str.startsWith("GET"))
				throw new IOException();
			String command = str.substring(4, str.length());
			// using this utility method, a bit hackey
			String parse[] = HTTPUtil.stringSplit(command, '/');
			// do some safety checks
			if (parse.length != 4) 
				throw new IOException();
			if (! parse[0].equals("get"))
				throw new IOException();
			
			//Check that the filename matches what we sent
			//in the GIV request.  I guess it doesn't need
			//to match technically, but we check to be safe.
			int end = parse[2].lastIndexOf("HTTP") - 1;
			String filename = parse[2].substring(0, end);
			// some safety checks - make sure name and index match.
			if (! filename.equals(_filename))
				throw new IOException();
			int pindex = java.lang.Integer.parseInt(parse[1]);
			if (pindex!= _index)
				throw new IOException();
			// catch any of the possible exceptions
		} catch (IndexOutOfBoundsException e) {
            throw new IOException();
        } catch (NumberFormatException e) {
            throw new IOException();
        } catch (IllegalArgumentException e) {
            throw new IOException();
        }
	}

    
	public void start() throws IOException {
		prepareFile();
		writeHeader();
		doUpload();
	}
	public void stop() {
		try {
			if (_ostream != null)
				_ostream.close();
		} catch (IOException e) {}
		try {
			if (_fis != null)
				_fis.close();
		} catch (IOException e) {}
		try {
			if (_socket != null) 
				_socket.close();
		} catch (IOException e) {}
	}

	/* Public Accessor Methods */
	public int getAmountRead() {return _amountRead;}

	/* Construction time variables */
	public int getIndex() {return _index;}
  	public String getFileName() {return _filename;}
	public int getFileSize() {return _fileSize;}

	/****************** private methods *******************/

	/** eventually this method should determine the 
	 * mime type of a file fill in the details of 
	 * this later. assume binary for now */
	private String getMimeType() {
		String mimetype;                
        mimetype = "application/binary"; 
        return mimetype;                  
	}

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

	private void doUpload() throws IOException {
	    int c = -1;
        int available = 0;
        byte[] buf = new byte[1024];
		
        long a = _fis.skip(_uploadBegin);
        _amountRead+=a;
		
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
                int maxBandwidth=(int)(theoreticalBandwidth*((float)speed/100.)
									   /(float)_uploadCount);
	
			
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
                    burstSent += c;
                }

                Date stop=new Date();

                //3.  Pause as needed so as not to exceed maxBandwidth.
                int elapsed=(int)(stop.getTime()-start.getTime());
                int sleepTime=cycleTime-elapsed;
                if (sleepTime>0) {
                    try {
                        Thread.currentThread().sleep(sleepTime);
                    } catch (InterruptedException e) { }
                }

            }
        }
		_ostream.close();
	}

   	/****************** sending error messages *******************/
	/**
     *   Handle too many upload requests
     */
    private void doLimitReached() throws IOException {
        /* Sends a 503 Service Unavailable message */
		OutputStream ostream = _socket.getOutputStream();
		String str;
		String errMsg = "Server busy.  Too many active downloads.";
		str = "HTTP/1.1 503 Service Unavailable\r\n";
		ostream.write(str.getBytes());
		str = "Server: " + "LimeWire" + "\r\n";
		ostream.write(str.getBytes());
		str = "Content-Type: text/plain\r\n";
		ostream.write(str.getBytes());
		str = "Content-Length: " + errMsg.length() + "\r\n";
		ostream.write(str.getBytes());
		str = "\r\n";
		ostream.write(str.getBytes());
		ostream.write(errMsg.getBytes());
		ostream.flush();
		ostream.close();
		_socket.close();
    }

	/**
     *   Handle a web based freeloader
     */
    private void doFreeloaderResponse() throws IOException {
        /* Sends a 402 Browser Request Denied message */
		OutputStream ostream = _socket.getOutputStream();
		String str;
		String errMsg = HTTPPage.responsePage;
		str = "HTTP 200 OK \r\n";
		ostream.write(str.getBytes());
		str = "Server: " + "LimeWire" + "\r\n";
		ostream.write(str.getBytes());
		str = "Content-Type: text/html\r\n";
		ostream.write(str.getBytes());
		str = "Content-Length: " + errMsg.length() + "\r\n";
		ostream.write(str.getBytes());
		str = "\r\n";
		ostream.write(str.getBytes());
		ostream.write(errMsg.getBytes());
		ostream.flush();
		ostream.close();
		_socket.close();
    }



}
