package com.limegroup.gnutella;

/**
 * auth: rsoule
 * file: HTTPUploader.java
 * desc: Read data from disk and write to the net.
 *
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|

import java.io.*;
import java.net.*;
import java.util.Date;
import com.sun.java.util.collections.*;

public class HTTPUploader implements Uploader {

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
	protected String _guid;
	protected int _port;
	protected int _stateNum = CONNECTING;

	private UploadState _state;
	
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
	public HTTPUploader(String file, Socket s, int index) {
		_socket = s;
		_hostName = _socket.getInetAddress().getHostAddress();
		_filename = file;
		_index = index;
		_amountRead = 0;
		FileDesc desc = FileManager.instance().get(_index);
		_fileSize = desc._size;
		setState(CONNECTING);
		try {
		    _ostream = _socket.getOutputStream();
		} catch (IOException e) {
		    setState(COULDNT_CONNECT);
		}
	}
		
	// Push requested Upload
	public HTTPUploader(String file, String host, int port, int index,
						String guid) {
		_filename = file;
		_index = index;
		_uploadBegin = 0;
		_amountRead = 0;
		_hostName = host;
		_guid = guid;
		_port = port;
		FileDesc desc = FileManager.instance().get(_index);
		_fileSize = desc._size;
		setState(CONNECTING);
	}

	// This method must be called in the case of a push.
	public void connect() throws IOException {

		// the socket should not be null if this is a non-push
		// connect.  in case connect is called after a non-push,
		// we just return.
		if (_socket != null)
			return;

		try {
			// this.setState(UPLOADING);
			// try to create the socket.
			_socket = new Socket(_hostName, _port);
			// open a stream for writing to the socket
			_ostream = _socket.getOutputStream();
			// ask chris about Assert
			Assert.that(_filename != null);  
			// write out the giv

			Assert.that(_filename != "");  

			String giv; 
			giv = "GIV " + _index + ":" + _guid + "/" + _filename + "\n\n";
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
		} catch (SecurityException e) {
			this.setState(Uploader.PUSH_FAILED);
			throw new IOException();
		} catch (IndexOutOfBoundsException e) {
			this.setState(Uploader.PUSH_FAILED);
            throw new IOException();
        } catch (NumberFormatException e) {
			this.setState(Uploader.PUSH_FAILED);
            throw new IOException();
        } catch (IllegalArgumentException e) {
			this.setState(Uploader.PUSH_FAILED);
            throw new IOException();
        } catch (IOException e) {
			this.setState(Uploader.PUSH_FAILED);
			throw new IOException();
		}
	}

    
	public void start() {
		try {
			readHeader();
			_state.doUpload(this);
		} catch (FreeloaderUploadingException e) { 
			setState(FREELOADER);
		} catch (IOException e) {
			e.printStackTrace();
			setState(INTERRUPTED);
		}
		stop();
		
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

	/**
	 * This method changes the appropriate state class based on
	 * the integer representing the state.  I'm not sure if this
	 * is a good idea, since it results in a case statement, that
	 * i was trying to avoid with 
	 */
	public void setState(int state) {
		_stateNum = state;
		switch (state) {
		case CONNECTING:
			_state = new NormalUploadState();
			break;
		case LIMIT_REACHED:
			_state = new LimitReachedUploadState();
			break;
		case PUSH_FAILED:
			_state = new PushFailedUploadState();
			break;
		case FREELOADER:     
			_state = new FreeloaderUploadState();
			break;
		case COMPLETE:
			break;
		}
	}
	

	/****************** accessor methods *****************/


	public OutputStream getOutputStream() {return _ostream;}
	public int getIndex() {return _index;}
	public String getFileName() {return _filename;}
	public int getFileSize() {return _fileSize;}
	public FileInputStream getFileInputStream() {return _fis;}
	public int amountUploaded() {return _amountRead;}
	public void setAmountUploaded(int amount) {_amountRead = amount;}
	public int getUploadBegin() {return _uploadBegin;}
	public int getState() {return _stateNum;}
	public String getHost() {return _hostName;}

	/****************** private methods *******************/


	private void readHeader() throws IOException {
        String str = " ";
        _uploadBegin = 0;
        _uploadEnd = 0;
		String userAgent;
		
		InputStream istream = _socket.getInputStream();
		ByteReader br = new ByteReader(istream);
        
		while (true) {
			// read the line in from the socket.
            str = br.readLine();
			// break out of the loop if it is null or blank
            if ( (str==null) || (str.equals("")) )
                break;
			// Look for the Range: header
			// it will be in one of three forms.  either
			// ' - n ', ' m - n', or ' 0 - '
            if (str.indexOf("Range: bytes=") != -1) {
                String sub = str.substring(13);
				// remove the white space
                sub = sub.trim();   
                char c;
				// get the first character
                c = sub.charAt(0);
				// - n  
                if (c == '-') {  
                    String second = sub.substring(1);
                    second = second.trim();
                    _uploadEnd = java.lang.Integer.parseInt(second);
                }
                else {                
					// m - n or 0 -
                    int dash = sub.indexOf("-");
                    String first = sub.substring(0, dash);
                    first = first.trim();
                    _uploadBegin = java.lang.Integer.parseInt(first);
                    String second = sub.substring(dash+1);
                    second = second.trim();
                    if (!second.equals("")) 
                        _uploadEnd = java.lang.Integer.parseInt(second);
                    
                }
            }

			// check the User-Agent field of the header information
			if (str.indexOf("User-Agent:") != -1) {
				// check for netscape, internet explorer,
				// or other free riding downoaders
				if (SettingsManager.instance().getAllowBrowser() == false) {
					// if we are not supposed to read from them
					// throw an exception
					if( (str.indexOf("Mozilla") != -1) ||
						(str.indexOf("DA") != -1) ||
						(str.indexOf("Download") != -1) ||
						(str.indexOf("FlashGet") != -1) ||
						(str.indexOf("GetRight") != -1) ||
						(str.indexOf("Go!Zilla") != -1) ||
						(str.indexOf("Inet") != -1) ||
						(str.indexOf("MIIxpc") != -1) ||
						(str.indexOf("MSProxy") != -1) ||
						(str.indexOf("Mass") != -1) ||
						(str.indexOf("MyGetRight") != -1) ||
						(str.indexOf("NetAnts") != -1) ||
						(str.indexOf("NetZip") != -1) ||
						(str.indexOf("RealDownload") != -1) ||
						(str.indexOf("SmartDownload") != -1) ||
						(str.indexOf("Teleport") != -1) ||
						(str.indexOf("WebDownloader") != -1) ) {
						throw new FreeloaderUploadingException();
					}
				}
				userAgent = str.substring(11).trim();
			}
		}

		if (_uploadEnd == 0)
			_uploadEnd = _fileSize;
	}

}










