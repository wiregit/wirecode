package com.limegroup.gnutella;

import java.io.*;
import java.net.*;
import com.sun.java.util.collections.*;


/**
 * auth: rsoule
 * file: HTTPUploader.java
 * desc: This is the abstract super class from which both
 * the NormalUploader and the ErrorDownloader will
 * inherit.  It will also implement the Uploader 
 * interface, which will be used to interact with 
 * the gui.
 *
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public abstract class HTTPUploader implements Uploader {
                    
	protected int BUFFSIZE = 1024;
	
	protected OutputStream _ostream;
	protected FileInputStream _fis;
	protected Socket _socket;
	protected int _amountRead;
	protected int _uploadBegin;
	protected int _uploadEnd;
	protected int _fileSize;
	protected int _index;
	protected String _filename;
	protected String _hostName;
	protected int _state;
	
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
	public HTTPUploader(String file, Socket s, int index)
		throws IOException {
		if (_socket == null)
			throw new IOException("Socket is null");
		_socket = s;
		_hostName = _socket.getInetAddress().getHostAddress();
		_hostName = 
		_filename = file;
		_index = index;
		_amountRead = 0;
	}
		
	// Push requested Upload
	public HTTPUploader(String file, String host, int port, int index,
						String guid) throws IOException {
		
		// NOTE: Do we know the name of the file?  Can this be
		// passed here? Or do we just know the index?
		_socket = new Socket(host, port);
		_filename = file;
		_index = index;
		_uploadBegin = 0;
		_amountRead = 0;
		_hostName = host;

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

	/**
	 * Start the upload
	 */
	public void start() throws IOException {}
	
	/**
	 * Stops this upload.  If the download is already 
	 * stopped, it does nothing.
	 */ 
	public void stop() {}

	/**
	 * returns the name of the file being uploaded.
	 */
	public String getFileName() {return _filename;}
	
	/**
	 * returns the length of the file being uploaded.
	 */ 
	 public int getFileSize() {return _fileSize;}

	/**
	 * returns the index of the file being uploaded.
	 */ 
	public int getIndex() {return _index;}

	/**
	 * returns the amount that of data that has been uploaded.
	 * this method was previously called "amountRead", but the
	 * name was changed to make more sense.
	 */ 
	public int amountUploaded() {return _amountRead;}

	/**
	 * returns the string representation of the IP Address
	 * of the host being uploaded to.
	 */
	public String getHost() {return _hostName;}
	
	/**
	 * returns an integer value that represents a state, such
	 * as CONNECTING, or ERROR.
	 */ 
	// public int getState() {return _state;}

}
