package com.limegroup.gnutella.uploader;

import java.io.*;
import java.net.*;
import java.util.Date;
import java.util.StringTokenizer;
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

	private OutputStream _ostream;
	private InputStream _fis;
	private Socket _socket;
	private int _amountRead;
	private int _uploadBegin;
	private int _uploadEnd;
	private int _fileSize;
	private final int _index;
	private final String _fileName;
	private final String _hostName;
	private final String _guid;
	private final int _port;
	private int _stateNum = CONNECTING;

	private UploadState _state;
	private final UploadManager _manager;
    private final MessageRouter _router;
	
	private boolean _chatEnabled;
	private String _chatHost;
	private int _chatPort;
    private final FileManager _fileManager;

	/**
	 * The URN specified in the X-Gnutella-Content-URN header, if any.
	 */
	private URN _requestedURN;

	/**
	 * The descriptor for the file we're uploading.
	 */
	private FileDesc _fileDesc;
    
    /**
     * Indicates that the client to which we are uploading is capable of
     * accepting Queryreplies in the response.
     */
    private boolean _clientAcceptsXGnutellaQueryreplies = false;

    private final BandwidthTrackerImpl bandwidthTracker=
		new BandwidthTrackerImpl();

	/**
	 * Stores any alternate locations specified in the HTTP headers for 
	 * this upload.
	 */
	private AlternateLocationCollection _alternateLocationCollection;

	/**
	 * Consructor for a "normal" non-push upload.  Note that this can
	 * be a URN get request.
	 *
	 * @param fileName the name of the file
	 * @param socket the <tt>Socket</tt> instance to serve the upload over
	 * @param index the index of the file in the set of shared files
	 * @param um a reference to the <tt>UploadManager</tt> instance 
	 * @param fm a reference to the <tt>FileManager</tt> instance
	 */
	public HTTPUploader(String fileName, Socket socket, int index, 
						UploadManager um, FileManager fm, MessageRouter router) {
		_socket = socket;
		_hostName = _socket.getInetAddress().getHostAddress();
		_fileName = fileName;
		_manager = um;
		_index = index;
		_amountRead = 0;
        _fileManager = fm;
        _router = router;        
		_guid = null;
		_port = 0;
		try {			
			_ostream = _socket.getOutputStream();

            //special case for browse host
            if(index == UploadManager.BROWSE_HOST_FILE_INDEX) {
                setState(BROWSE_HOST);
                return;
            } 

			_fileDesc = _fileManager.get(_index);
			_fileSize = (int)_fileDesc.getSize();

            // if the requested name does not match our name on disk,
			// report File Not Found
			if(!_fileName.equals(_fileDesc.getName())) {
				setState(FILE_NOT_FOUND);
			}
			else {
				setState(CONNECTING);
				// get the fileInputStream
				_fis = _fileDesc.createInputStream();
			}
		} catch (IndexOutOfBoundsException e) {
			setState(FILE_NOT_FOUND);
		} catch (IOException e) {
			// this occurs if the output stream could not be opened to
			// the socket or if the input stream could not be created
			// from the file
			setState(INTERRUPTED);
		}
	}
		

	// Push requested Upload
    /**
     * The other constructor that is used for push uploads. This constructor
     * does not take a socket. An uploader created with this constructor 
     * must eventually connect to the downloader using the connect method of 
     * this class
     * @param fileName The name of the file to be uploaded
     * @param host The downloaders ip address
     * @param port The port at which the downloader is listneing 
     * @param index index of file to be uploaded
     */
	public HTTPUploader(String fileName, String host, int port, int index,
						String guid, UploadManager um, FileManager fm,
                        MessageRouter router) {
		_fileName = fileName;
		_manager = um;
		_index = index;
		_uploadBegin = 0;
		_amountRead = 0;
		_hostName = host;
		_guid = guid;
		_port = port;
        _fileManager = fm;
        _router = router;
		try {
			_fileDesc = _fileManager.get(_index);
			_fileSize = _fileDesc._size;
            // if the requested name does not match our name on disk,
			// report File Not Found
			if(!_fileName.equals(_fileDesc.getName())) {
				setState(FILE_NOT_FOUND);
			}
			else {
				setState(CONNECTING);
				_fis = _fileDesc.createInputStream();
			}
		} catch (IndexOutOfBoundsException e) {
			setState(PUSH_FAILED);
		} catch (IOException e) {
			setState(FILE_NOT_FOUND);
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
			Assert.that(_fileName != null);  
			// write out the giv

			Assert.that(_fileName != "");  

			String giv = "GIV " + _index + ":" + _guid + "/" + 
			             _fileName + "\n\n";
			_ostream.write(giv.getBytes());
			_ostream.flush();

            InputStream in = _socket.getInputStream(); 
            //dont read a word of size more than 3
            String word = IOUtils.readWord(in, 3);
            if (!word.equalsIgnoreCase("get"))
                throw new IOException();

            //OK. We connected, sent the GIV, and confirmed the get, 
            //now just return the socket
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

    
	/**
	 * Starts "uploading" the requested file.  The behavior of the upload,
	 * however, depends on the current upload state.  If the file was not
	 * found, for example, the upload sends a 404 Not Found message, whereas
	 * in the case of a normal upload, the file is transferred as expected.<p>
	 *
	 * This method also handles storing any newly discovered alternate 
	 * locations for this file in the corresponding <tt>FileDesc</tt>.  The
	 * new alternate locations are discovered through the requesting client's
	 * HTTP headers.
	 *
	 * Implements the <tt>Uploader</tt> interface.
	 */
	public void start() {
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

		if(_alternateLocationCollection != null) {
			// making this call now is necessary to avoid writing the 
			// same alternate locations back to the requester as were 
			// sent in the request headers
			_fileDesc.addAlternateLocationCollection(_alternateLocationCollection);
		}
	}

    /**
	 * Closes the outputstream, inputstream, and socket for this upload 
	 * connection if they are not null.
	 *
	 * Implements the <tt>Uploader</tt> interface.
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
	 * i was trying to avoid with.
	 *
	 * Implements the <tt>Uploader</tt> interface.
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
        case BROWSE_HOST:
            _state = new BrowseHostUploadState(_fileManager, _router);
            break;
		case FILE_NOT_FOUND:
			_state = new FileNotFoundUploadState();
		case COMPLETE:
		case INTERRUPTED:
			break;
		}
	}
	
	OutputStream getOutputStream() {return _ostream;}
	InputStream getInputStream() {return _fis;}

	void setAmountUploaded(int amount) {_amountRead = amount;}
    /** The byte offset where we should start the upload. */
	int getUploadBegin() {return _uploadBegin;}
    /** Returns the offset of the last byte to send <b>PLUS ONE</b>. */
    int getUploadEnd() {return _uploadEnd;}
	
	/**
	 * Accessor for the <tt>UploadManager</tt>.
	 *
	 * @return the <tt>UploadManager</tt>
	 */
	UploadManager getManager() {return _manager;}

	// implements the Uploader interface
	public int getFileSize() {return _fileSize;}

	// implements the Uploader interface
	public int getIndex() {return _index;}

	// implements the Uploader interface
	public String getFileName() {return _fileName;}

	// implements the Uploader interface
	public int getState() {return _stateNum;}

	// implements the Uploader interface
	public String getHost() {return _hostName;}

	// implements the Uploader interface
	public boolean chatEnabled() {return _chatEnabled;}

	// implements the Uploader interface
	public String getChatHost() {return _chatHost;}

	// implements the Uploader interface
	public int getChatPort() {return _chatPort;}

    /**The number of bytes read. The way we calculate the number of bytes 
     * read is a little wierd if the range header begins from the middle of 
     * the file (say from byte x). Then we consider that bytes 0-x have 
     * already been read. 
     * <p>
     * This may lead to some wierd behaviour with chunking. For example if 
     * a host requests the last 10% of a file, the GUI will display 90%
     * downloaded. Later if the same host requests from 20% to 30% the 
     * progress will reduce to 20% onwards. 
	 * 
	 * Implements the Uploader interface.
     */
	public int amountUploaded() {return _amountRead;}

	/**
	 * Returns the <tt>FileDesc</tt> instance for this uploader.
	 *
	 * @return the <tt>FileDesc</tt> instance for this uploader, or
	 *  <tt>null</tt> if the <tt>FileDesc</tt> could not be assigned
	 *  from the shared files
	 */
	FileDesc getFileDesc() {return _fileDesc;}

    boolean getClientAcceptsXGnutellaQueryreplies() {
        return _clientAcceptsXGnutellaQueryreplies;
    }    

	/**
	 * Reads the HTTP header sent by the requesting client -- note that the
	 * 'GET' portion of the request header has already been read.
	 *
	 * @throws <tt>IOException</tt> if there are any io issues while reading
	 *  the header
	 */
	private void readHeader() throws IOException {
        _uploadBegin = 0;
        _uploadEnd = 0;
		String userAgent;
		_clientAcceptsXGnutellaQueryreplies = false;
        
		ByteReader br = new ByteReader(_socket.getInputStream());
        
		// NOTE: it might improve readability to move
		// the try and catches around the big loops.

		while (true) {
			// read the line in from the socket.
            String str = br.readLine();
            debug("HTTPUploader.readHeader(): str = " +  str);
			// break out of the loop if it is null or blank
            if ( (str==null) || (str.equals("")) )
                break;
			else if (str.toUpperCase().indexOf("CHAT:") != -1) {
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
            else if (indexOfIgnoreCase(str, "Range:") == 0) {
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
			else if (indexOfIgnoreCase(str, "User-Agent:") != -1) {
				// check for netscape, internet explorer,
				// or other free riding downoaders
                //Allow them to browse the host though
				if (SettingsManager.instance().getAllowBrowser() == false
                    && !(_stateNum == BROWSE_HOST)  
					&& !(_fileName.toUpperCase().startsWith("LIMEWIRE"))) {
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
			else if(HTTPHeaderName.CONTENT_URN.matchesStartOfString(str)) {
				URN requestedURN = HTTPUploader.readContentUrn(str);
				if(requestedURN == null) {
					setState(FILE_NOT_FOUND);
				} 		
				if(_fileDesc != null) {
					if(!_fileDesc.equals(_fileManager.getFileDescForUrn(requestedURN))) {
						setState(FILE_NOT_FOUND);
					}
				}
			}
			else if(HTTPHeaderName.ALT_LOCATION.matchesStartOfString(str)) {
				if(_alternateLocationCollection == null) {
					_alternateLocationCollection = new AlternateLocationCollection();
				}
				HTTPUploader.readAlternateLocations(str, _alternateLocationCollection);
			}
            //check the "accept:" header
            if (indexOfIgnoreCase(str, "accept:") != -1) {
                if(indexOfIgnoreCase(str, Constants.QUERYREPLY_MIME_TYPE)
                    != -1) {
                    _clientAcceptsXGnutellaQueryreplies = true;
                }
            }
		}

		if (_uploadEnd == 0)
			_uploadEnd = _fileSize;
	}

	/**
	 * This method parses the "X-Gnutella-Content-URN" header, as specified
	 * in HUGE v0.93.  This assigns the requested urn value for this 
	 * upload, which otherwise remains null.
	 *
	 * @param contentUrnStr the string containing the header
	 * @return a new <tt>URN</tt> instance for the request line, or 
	 *  <tt>null</tt> if there was any problem creating it
	 */
	private static URN readContentUrn(final String contentUrnStr) {
		int offset = contentUrnStr.indexOf(":");
		int spaceIndex = contentUrnStr.indexOf(" ");
		if(offset == -1) {
			return null;
		}
		if(spaceIndex == -1) {
			// this means that there's no space after the colon
			offset++;
		}
		else if((spaceIndex - offset) == 1) {
			// this means that there is a space after the colon,
			// so the urn is offset by one more index
			offset += 2;
		}
		else {
			// otherwise, the request is of an unknown form, so just 
			// return null
			return null;
		}
		
		String urnStr = contentUrnStr.substring(offset);
		try {
			return URNFactory.createSHA1Urn(urnStr);
		} catch(IOException e) {
			// this will be thrown if the URN string was invalid for any
			// reason -- just return null
			return null;
		}		
	}
	
	/**
	 * Reads alternate location header.  The header can contain only one
	 * alternate location, or it can contain many in the same header.
	 * This method adds them all to the <tt>FileDesc</tt> for this
	 * uploader.  This will not allow more than 20 alternate locations
	 * for a single file.
	 *
	 * @param altHeader the full alternate locations header
	 * @param alc the <tt>AlternateLocationCollector</tt> that read alternate
	 *  locations should be added to
	 */
	private static void readAlternateLocations(final String altHeader,
											   final AlternateLocationCollector alc) {
		int colonIndex = altHeader.indexOf(":");
		if(colonIndex == -1) {
			return;
		}
		final String alternateLocations = 
		    altHeader.substring(colonIndex+1).trim();
		StringTokenizer st = new StringTokenizer(alternateLocations, ",");

		while(st.hasMoreTokens()) {
			try {
				AlternateLocation al = new AlternateLocation(st.nextToken().trim());
				alc.addAlternateLocation(al);
			} catch(IOException e) {
				e.printStackTrace();
				// just return without adding it.
				continue;
			}
		}
	}

	/**
	 * Helper method to compare two stings, ignoring their case.
	 */ 
	private int indexOfIgnoreCase(String str, String section) {
		// convert both strings to lower case -- this is expensive
		String aaa = str.toLowerCase();
		String bbb = section.toLowerCase();
		// then look for the index...
		return aaa.indexOf(bbb);
	}
  
    public void measureBandwidth() {
        bandwidthTracker.measureBandwidth(amountUploaded());
    }

    public float getMeasuredBandwidth() {
        float retVal = 0;
        try {
            retVal = bandwidthTracker.getMeasuredBandwidth();
        } catch (InsufficientDataException ide) {
            retVal = 0;
        }
        return retVal;
    }
    
    //inherit doc comment
    public boolean getCloseConnection() {
        return _state.getCloseConnection();
    }

    private final boolean debugOn = false;
    private void debug(String out) {
        if (debugOn)
            System.out.println(out);
    }

	// overrides Object.toString
	public String toString() {
		return "HTTPUploader:\r\n"+
		       "File Name: "+_fileName+"\r\n"+
		       "Host Name: "+_hostName+"\r\n"+
		       "Port:      "+_port+"\r\n"+
		       "File Size: "+_fileSize+"\r\n"+
		       "State:     "+_state;
		
	}
}










