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
	private int _index;
	private String _fileName;
	private String _hostName;
	private String _guid;
	private int _port;
	private int _stateNum = CONNECTING;

	private UploadState _state;
	private final UploadManager _manager;
    private MessageRouter _router;
	
	private boolean  _chatEnabled;
	private String  _chatHost;
	private int _chatPort;
    private FileManager _fileManager;
	private URN _urn = null;

	/**
	 * The URN specified in the X-Gnutella-Content-URN header, if any.
	 */
	private URN _requestedURN = null;

	/**
	 * <tt>Map</tt> instance for storing unique alternate locations sent in 
	 * the upload header.
	 */
	private Map _alternateLocations = null;

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
	 * still be a URN get request.
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
		FileDesc desc;
		boolean indexOut = false;
		boolean ioexcept = false;
        
		try {
			// This line can't be moved, or FileNotFoundUploadState
			// will have a null pointer exception.
			_ostream = _socket.getOutputStream();
			_fileDesc = _fileManager.get(_index);
			_fileSize = (int)_fileDesc.getSize();
			_urn = _fileDesc.getSHA1Urn();
            
            //special case for browse host
            if(index == UploadManager.BROWSE_HOST_FILE_INDEX) {
                setState(BROWSE_HOST);
            } 
			else {
				setState(CONNECTING);
			}
		} catch (IndexOutOfBoundsException e) {
			// this is an unlikely case, but if for
			// some reason the index is no longer valid.
			setState(FILE_NOT_FOUND);
		} catch (IOException e) {
			// FileManager.get() throws an IOException if
			// the file has been deleted
			setState(INTERRUPTED);
		}
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
						String guid, UploadManager m, FileManager fm,
                        MessageRouter router) {
		_fileName = file;
		_manager = m;
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
	 * Is called by the thread.  makes the
	 * actual call upload the file or appropriate
	 * error information.
	 */
	public void start() {
		try {
            if(_stateNum != BROWSE_HOST)
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

		if(_alternateLocationCollection != null) {
			// making this call now is necessary to avoid writing the 
			// same alternate locations back to the requester as were 
			// sent in the request headers
			_fileDesc.addAlternateLocationCollection(_alternateLocationCollection);
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
	
	/**
	 * Stores the alternate location for this upload in the associated 
	 * <tt>FileDesc</tt> instance.
	 */
	public void storeAlternateLocation() {
		// ignore if this is a push upload
		if(_socket == null) return;
		try {
			URL url = 
			    new URL("http", _hostName, _port, 
						URNFactory.createHttpUrnServiceRequest(_fileDesc.getSHA1Urn()));
			AlternateLocation al = new AlternateLocation(url);
			_fileDesc.addAlternateLocation(al);
		} catch(MalformedURLException e) {
			// if the url is invalid, it simply will not be added to the list
			// of alternate locations
		}		
	}

	/****************** accessor methods *****************/


	public OutputStream getOutputStream() {return _ostream;}
	public int getIndex() {return _index;}
	public String getFileName() {return _fileName;}
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

	/**
	 * Accessor for the <tt>URN</tt> instance for the file requested, which
	 * is <tt>null</tt> if there is no URN for the file.
	 *
	 * @return the <tt>URN</tt> instance for the file being uploaded, which
	 *  can be <tt>null</tt> if no URN has been assigned
	 */
	public URN getUrn() {return _urn;}

	/**
	 * Returns the requested <tt>URN</tt> as specified in the 
	 * "X-Gnutella-Content-URN" extension header, defined in HUGE v0.93.
	 *
	 * @return the requested <tt>URN</tt>
	 */
	public URN getRequestedUrn() {return _requestedURN;}

	/**
	 * Returns the <tt>FileDesc</tt> instance for this uploader.
	 *
	 * @return the <tt>FileDesc</tt> instance for this uploader, or
	 *  <tt>null</tt> if the <tt>FileDesc</tt> could not be assigned
	 *  from the shared files
	 */
	public FileDesc getFileDesc() {return _fileDesc;}

    public boolean getClientAcceptsXGnutellaQueryreplies() {
        return _clientAcceptsXGnutellaQueryreplies;
    }    
	/****************** private methods *******************/


	/**
	 * Reads the HTTP header sent by the requesting client -- note that the
	 * 'GET' portion of the request header has already been read.
	 *
	 * @throws <tt>IOException</tt> if there are any io issues while reading
	 *  the header
	 */
	private void readHeader() throws IOException {
        String str = "";
        _uploadBegin = 0;
        _uploadEnd = 0;
		String userAgent;
		_clientAcceptsXGnutellaQueryreplies = false;
        
		InputStream istream = _socket.getInputStream();
		ByteReader br = new ByteReader(istream);
        
		// NOTE: it might improve readability to move
		// the try and catches around the big loops.

		while (true) {
			// read the line in from the socket.
            str = br.readLine();
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
				_requestedURN = HTTPUploader.readContentUrn(str);
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
			return URNFactory.createUrn(urnStr);
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
		if (_fileName == null) {
            _fileName = name;
        } else {
			/* matches the name */
			if ( !name.equals(_fileName) ) {
				throw new IOException();
			}
        }

		// set the file size
        _fileSize = fdesc._size;

		// get the fileInputStream
		_fis = fdesc.createInputStream();

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










