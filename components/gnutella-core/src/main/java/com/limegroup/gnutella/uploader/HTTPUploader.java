package com.limegroup.gnutella.uploader;



import java.io.*;
import java.net.*;
import java.util.Date;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.StringUtils;
import com.limegroup.gnutella.util.URLDecoder;

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
public class HTTPUploader implements Uploader {
    //See accessors for documentation
	protected OutputStream _ostream;
	protected InputStream _fis;
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
	private UploadManager _manager;
	
	private boolean  _chatEnabled;
	private String  _chatHost;
	private int _chatPort;
    private FileManager _fileManager;

    private BandwidthTrackerImpl bandwidthTracker=new BandwidthTrackerImpl();

    //constructors

	/**
     * The constructor used for normal uploads, takes the incoming socket as 
     * a parameter
     * @param file the file to be uploaded
     * @param s the socket on which to upload the file.
     * @param index index of file to upload
     */
	public HTTPUploader(String file, Socket s, int index, UploadManager m,
                        FileManager fm) {
		_socket = s;
		_hostName = _socket.getInetAddress().getHostAddress();
		_filename = file;
		_manager = m;
		_index = index;
		_amountRead = 0;
        _fileManager = fm;
		FileDesc desc;
		boolean indexOut = false;
		boolean ioexcept = false;

		try {
			// This line can't be moved, or FileNotFoundUploadState
			// will have a null pointer exception.
			_ostream = _socket.getOutputStream();
			desc = _fileManager.get(_index);
			_fileSize = desc._size;
		} catch (IndexOutOfBoundsException e) {
			// this is an unlikely case, but if for
			// some reason the index is no longer valid.
			indexOut = true;
		} catch (IOException e) {
			// FileManager.get() throws an IOException if
			// the file has been deleted
			ioexcept = true;
		}
		if (indexOut)
			setState(FILE_NOT_FOUND);
		else if (ioexcept) 
			setState(INTERRUPTED);
		else 
			setState(CONNECTING);
	}
		
	// Push requested Upload
    /**
     * The other constructor that is used for push uploads. This constructor
     * does not take a socket. An uploader created with this constructor 
     * must eventually connect to the downloader using the connect method of 
     * this class
     * @param file The name of the file to be uploaded
     * @param host The downloaders ip address
     * @param port The port at which the downloader is listneing 
     * @param index index of file to be uploaded
     */
	public HTTPUploader(String file, String host, int port, int index,
						String guid, UploadManager m, FileManager fm) {
		_filename = file;
		_manager = m;
		_index = index;
		_uploadBegin = 0;
		_amountRead = 0;
		_hostName = host;
		_guid = guid;
		_port = port;
        _fileManager = fm;
		FileDesc desc;
		try {
			desc = _fileManager.get(_index);
			_fileSize = desc._size;
			setState(CONNECTING);
		} catch (IndexOutOfBoundsException e) {
			setState(PUSH_FAILED);
		}
	}

	/**
     * This method is called in the case of a push only.
     * <p>
     * The method creates the socket, and send the GIV message.
     * When this method returns the socket, is in the same state as a 
     * socket created as a result of a normal upload - ready to receive GET
     * <p>
     * @return The returned socket is used for a normal upload.
     */
	public Socket connect() throws IOException {
        // This method is only called from acceptPushUpload() now. 
        // So this will never happen...but lets just leave it in there.
		if (_socket != null)
			return _socket;

		try {
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
			
            //OK. We conneced and sent the GIV, now just return the socket
            return _socket;
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

    
	/*
	 * Is called by the thread.  makes the
	 * actual call upload the file or appropriate
	 * error information.
	 */
	public void start() {
		try {
			prepareFile();
		} catch (IOException e) {
			setState(FILE_NOT_FOUND);
		}
		try {
			readHeader();
			_state.doUpload(this);
		} catch (FreeloaderUploadingException e) { 
			setState(FREELOADER);
			try {
			    _state.doUpload(this);
			} catch (IOException e2) {};
		} catch (IOException e) {
			setState(INTERRUPTED);
		}
	}

    /**
	 * closes the outputstream, inputstream, and socket
	 * if they are not null.
	 */
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
		case FILE_NOT_FOUND:
			_state = new FileNotFoundUploadState();
		case COMPLETE:
		case INTERRUPTED:
			break;
		}
	}
	

	/****************** accessor methods *****************/


	public OutputStream getOutputStream() {return _ostream;}
	public int getIndex() {return _index;}
	public String getFileName() {return _filename;}
	public int getFileSize() {return _fileSize;}
	public InputStream getInputStream() {return _fis;}
    /**The number of bytes read. The way we calculate the number of bytes 
     * read is a little wierd if the range header begins from the middle of 
     * the file (say from byte x). Then we consider that bytes 0-x have 
     * already been read. 
     * <p>
     * This may lead to some wierd behaviour with chunking. For example if 
     * a host requests the last 10% of a file, the GUI will display 90%
     * downloaded. Later if the same host requests from 20% to 30% the 
     * progress will reduce to 20% onwards. 
     */
	public int amountUploaded() {return _amountRead;}
	public void setAmountUploaded(int amount) {_amountRead = amount;}
    /** The byte offset where we should start the upload. */
	public int getUploadBegin() {return _uploadBegin;}
    /** Returns the offset of the last byte to send <b>PLUS ONE</b>. */
    public int getUploadEnd() {return _uploadEnd;}
	public int getState() {return _stateNum;}
	public String getHost() {return _hostName;}
	public UploadManager getManager() {return _manager;}

	public String getThisHost() {return _manager.getThisHost(); } 
	public int getThisPort() {return _manager.getThisPort(); }

	public boolean chatEnabled() {return _chatEnabled;}
	public String getChatHost() {return _chatHost;}
	public int getChatPort() {return _chatPort;}
	/****************** private methods *******************/


	private void readHeader() throws IOException {

        String str = " ";
        _uploadBegin = 0;
        _uploadEnd = 0;
		String userAgent;
		
		InputStream istream = _socket.getInputStream();
		ByteReader br = new ByteReader(istream);
        
		// NOTE: it might improve readability to move
		// the try and catches around the big loops.

		while (true) {
			// read the line in from the socket.
            str = br.readLine();
			// break out of the loop if it is null or blank
            if ( (str==null) || (str.equals("")) )
                break;

			if (str.toUpperCase().indexOf("CHAT:") != -1) {
				String sub;
				try {
					sub = str.substring(5);
				} catch (IndexOutOfBoundsException e) {
					throw new IOException();
                }
				sub = sub.trim();
				int colon = sub.indexOf(":");
				String host  = sub.substring(0,colon);
				host = host.trim();
				String sport = sub.substring(colon+1);
				sport = sport.trim();

				int port; 
				try {
					port = java.lang.Integer.parseInt(sport);
				} catch (NumberFormatException e) {
					throw new IOException();
                }
				_chatEnabled = true;
				_chatHost = host;
				_chatPort = port;
			}
			// Look for range header of form, "Range: bytes=", "Range:bytes=",
			// "Range: bytes ", etc.  Note that the "=" is required by HTTP, but
            //  old versions of BearShare do not send it.  The value following the
            //  bytes unit will be in the form '-n', 'm-n', or 'm-'.
            if (indexOfIgnoreCase(str, "Range:") == 0) {
                //Set 'sub' to the value after the "bytes=" or "bytes ".  Note
                //that we don't validate the data between "Range:" and the
                //bytes.
				String sub;
				String second;
				try {
                    int i=str.indexOf("bytes");    //TODO: use constant
                    if (i<0)
                        throw new IOException();
                    i+=6;                          //TODO: use constant
					sub = str.substring(i);
				} catch (IndexOutOfBoundsException e) {
					throw new IOException();
				}
				// remove the white space
                sub = sub.trim();   
                char c;
				// get the first character
				try {
					c = sub.charAt(0);
				} catch (IndexOutOfBoundsException e) {
					throw new IOException();
				}
				// - n  
                if (c == '-') {  
					// String second;
					try {
						second = sub.substring(1);
					} catch (IndexOutOfBoundsException e) {
						throw new IOException();
					}
                    second = second.trim();
					try {
                        //A range request for "-3" means return the last 3 bytes
                        //of the file.  (LW used to incorrectly return bytes
                        //0-3.)  
                        _uploadBegin = Math.max(0,
                                            _fileSize-Integer.parseInt(second));
						_uploadEnd = _fileSize;
					} catch (NumberFormatException e) {
						throw new IOException();
					}
                }
                else {                
					// m - n or 0 -
                    int dash = sub.indexOf("-");
					String first;
					try {
						first = sub.substring(0, dash);
					} catch (IndexOutOfBoundsException e) {
						throw new IOException();
					}
                    first = first.trim();
					try {
						_uploadBegin = java.lang.Integer.parseInt(first);
					} catch (NumberFormatException e) {
						throw new IOException();
					}
					try {
						second = sub.substring(dash+1);
					} catch (IndexOutOfBoundsException e) {
						throw new IOException();
					}
                    second = second.trim();
                    if (!second.equals("")) 
						try {
                            //HTTP range requests are inclusive.  So "1-3" means
                            //bytes 1, 2, and 3.  But _uploadEnd is an EXCLUSIVE
                            //index, so increment by 1.
							_uploadEnd = java.lang.Integer.parseInt(second)+1;
                    } catch (NumberFormatException e) {
						throw new IOException();
					}
                }
            }

			// check the User-Agent field of the header information
			if (indexOfIgnoreCase(str, "User-Agent:") != -1) {
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

	/**
	 * a helper method to compare two strings 
	 * ignoring their case.
	 */ 
	private int indexOfIgnoreCase(String str, String section) {
		// convert both strings to lower case
		String aaa = str.toLowerCase();
		String bbb = section.toLowerCase();
		// then look for the index...
		return aaa.indexOf(bbb);
	}


	/**
	 * prepares the file to be read for sending accross the socket
	 */
	private void prepareFile() throws IOException {
		// get the appropriate file descriptor
		FileDesc fdesc;
		try {
			fdesc = _fileManager.get(_index);
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
			if ( !name.equals(_filename) ) {
				throw new IOException();
			}
        }

		// set the file size
        _fileSize = fdesc._size;

		// get the fileInputStream
		_fis = _fileManager.getInputStream(fdesc);

	}
  
    public void measureBandwidth() {
        bandwidthTracker.measureBandwidth(amountUploaded());
    }

    public float getMeasuredBandwidth() {
        return bandwidthTracker.getMeasuredBandwidth();
    }
    
    //inherit doc comment
    public boolean getCloseConnection() {
        return _state.getCloseConnection();
    }
}










