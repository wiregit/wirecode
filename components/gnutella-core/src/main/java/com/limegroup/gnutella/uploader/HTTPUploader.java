package com.limegroup.gnutella.uploader;

import java.io.*;
import java.net.*;
import java.util.Date;
import java.util.StringTokenizer;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.http.*;
import com.limegroup.gnutella.statistics.*;
import com.limegroup.gnutella.util.*;

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
public final class HTTPUploader implements Uploader {

	private OutputStream _ostream;
	private InputStream _fis;
	private Socket _socket;
	private int _totalAmountRead;
	private int _amountRead;
	// useful so we don't have to do _uploadEnd - _uploadBegin everywhere
    private int _amountRequested;
	private int _uploadBegin;
	private int _uploadEnd;
	private int _fileSize;
	private final int _index;
	private String _userAgent;
	private boolean _headersParsed;
	private final String _fileName;
	private final String _hostName;
	private final String _guid;
	private final int _port;
	private int _stateNum = CONNECTING;

	private HTTPMessage _state;
	
	private boolean _chatEnabled;
	private boolean _browseEnabled;
	private int _gnutellaPort;
    private boolean _supportsQueueing = false;

    /**
     * The Watchdog that will kill this uploader if it takes too long.
     */
    private final StalledUploadWatchdog STALLED_WATCHDOG;

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

    private BandwidthTrackerImpl bandwidthTracker=null;

	/**
	 * Stores any alternate locations specified in the HTTP headers for 
	 * this upload.
	 */
	private AlternateLocationCollection _alternateLocationCollection;

	/**
	 * The <tt>HTTPRequestMethod</tt> to use for the upload.
	 */
	private HTTPRequestMethod _method;
	
	/**
	 * Whether or not to record stats.
	 */
	private static final boolean RECORD_STATS = !CommonUtils.isJava118();

	/**
	 * Consructor for a "normal" non-push upload.  Note that this can
	 * be a URN get request.
	 *
	 * @param method the <tt>HTTPRequestMethod</tt> for the request
	 * @param fileName the name of the file
	 * @param socket the <tt>Socket</tt> instance to serve the upload over
	 * @param index the index of the file in the set of shared files
     * @param old the old HTTPDownloader which we use for bandwidth 
     * to initialize this' bandwidth tracker so we have history
	 */
	public HTTPUploader(HTTPRequestMethod method,
	                    String fileName, 
                        Socket socket,
                        int index,
                        StalledUploadWatchdog dog) {
        STALLED_WATCHDOG = dog;
		_socket = socket;
		_hostName = _socket.getInetAddress().getHostAddress();
		_fileName = fileName;
		_index = index;;
		_guid = null;
		_port = 0;
		reinitialize(method);
        }
    
    /**
     * Reinitializes this uploader for a new request method.
     */
    public void reinitialize(HTTPRequestMethod method) {
        _method = method;
        _amountRequested = 0;
        _uploadBegin = 0;
        _uploadEnd = 0;
        _fileSize = 0;
        _userAgent = null;
        _headersParsed = false;
        _stateNum = CONNECTING;
        _state = null;
        _chatEnabled = false;
        _browseEnabled = false;
        _gnutellaPort = 0;
        _supportsQueueing = false;
        _requestedURN = null;
        _fileDesc = null;
        _clientAcceptsXGnutellaQueryreplies = false;
        _alternateLocationCollection = null;
        
        // If this is the first time we are initializing it,
        // create a new bandwidth tracker and set a few more variables.
        if( bandwidthTracker == null ) {
            bandwidthTracker = new BandwidthTrackerImpl();
            _totalAmountRead = 0;
            _amountRead = 0;
        }            
        // Otherwise, update the amount read.
        else {
            _totalAmountRead += _amountRead;
            _amountRead = 0;
        }
        
		try {			
			_ostream = _socket.getOutputStream();
        } catch(IOException e) {
            // the connection was broken
			setState(FILE_NOT_FOUND);            
            return;
        }

        //special case for certain states
        switch(_index) {
        case UploadManager.BROWSE_HOST_FILE_INDEX:
            setState(BROWSE_HOST);
            return;
        case UploadManager.UPDATE_FILE_INDEX:
            setState(UPDATE_FILE);
            return;
        case UploadManager.BAD_URN_QUERY_INDEX:
            setState(FILE_NOT_FOUND);
            return;
        case UploadManager.MALFORMED_REQUEST_INDEX:
            setState(MALFORMED_REQUEST);
            return;
        }
            
        FileManager fm = RouterService.getFileManager();
        if(fm.isValidIndex(_index)) {
            _fileDesc = fm.get(_index);
        } 
        if(_fileDesc == null) {
            setState(FILE_NOT_FOUND);
            return;
        }
        
        _fileSize = (int)_fileDesc.getSize();        
        // if the requested name does not match our name on disk,
        // report File Not Found
        if(!_fileName.equals(_fileDesc.getName())) {
            setState(FILE_NOT_FOUND);
        }
        else {
            try {
                // if there already was an input stream, close it.
                if( _fis != null ) {
                    try {
                        _fis.close();
                    } catch(IOException ignored) {}
                }
                _fis = _fileDesc.createInputStream();
                setState(CONNECTING);
			} catch(FileNotFoundException e) {
                // could not access the file on disk
                setState(FILE_NOT_FOUND);
            }
		}
	}
		

    /**
     * The other constructor that is used for push uploads. This constructor *
	 *  does not take a socket. An uploader created with this constructor * must
	 *  eventually connect to the downloader using the connect method of * this
	 *  class.  
     *  Note: This uploader dies after receiving the GIV. So this constructor 
     * does not have all the critical features the other constructr has.
	 *  need to change
     * @param fileName The name of the file to be uploaded
     * @param host The downloaders ip address
     * @param port The port at which the downloader is listneing 
     * @param index index of file to be uploaded
	 * @param guid the GUID of the request */
	public HTTPUploader(String fileName, String host, int port, 
                                            int index, String guid) {
        STALLED_WATCHDOG = null; // not used to transfer.
		_fileName = fileName;
		_index = index;
		_uploadBegin = 0;
		_amountRead = 0;
		_totalAmountRead = 0;
		_hostName = host;
		_guid = guid;
		_port = port;

        FileManager fm = RouterService.getFileManager();
        if(!fm.isValidIndex(_index)) {
            setState(PUSH_FAILED);
            return;
        }
        _fileDesc = fm.get(_index);
        if(_fileDesc == null) {
            setState(PUSH_FAILED);
            return;                
        }
        
        _fileSize = (int)_fileDesc.getSize();
        
            
        // if the requested name does not match our name on disk,
        // report File Not Found
        if(!_fileName.equals(_fileDesc.getName())) {
            setState(FILE_NOT_FOUND);
        }
        else {
            try {
                _fis = _fileDesc.createInputStream();
                setState(CONNECTING);
            } catch(IOException e) {
                setState(FILE_NOT_FOUND);                    
            }
        }
	}

    /**
     * Establishes a push upload.  If this is already connected, returns
     * immediately.  Otherwise attempts to establish a TCP connection to the
     * remote host and send a GIV request.
     *
     * @return this' underlying socket, for which a GIV has been sent but
     *  nothing has been read.  Typically the caller will read the GET or HEAD
     *  request from this socket.  The socket is needed for persistence and
     *  queueing purposes; caller must be careful when modifying it.
     * @throws IOException couldn't establish the connection
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

            //OK. We connected, sent the GIV.  Now just return the socket.
            return _socket;
		} catch (SecurityException e) {
			this.setState(Uploader.PUSH_FAILED);
			throw new IOException("security exception");
        } catch (IOException e) {
			this.setState(Uploader.PUSH_FAILED);
			throw e;
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
	 * HTTP headers.<p>
	 *
	 * Implements the <tt>Uploader</tt> interface.
	 */
	public void writeResponse() {
		try {
			if(_ostream != null) {
				_method.writeHttpResponse(_state, _ostream);
			}
		} catch (IOException e) {
            // set it to be completed if they read what they wanted
            // regardless of interruption
            if ( _amountRead >= _amountRequested ) {
                setState(COMPLETE);
            }
            else {
			    // really what we want to be doing???
			    setState(INTERRUPTED);
			}
		}

		if(_alternateLocationCollection != null && _fileDesc != null) {
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
			_state = new NormalUploadState(this, STALLED_WATCHDOG);
			break;
        case QUEUED:
            int pos=RouterService.getUploadManager().positionInQueue(_socket);
            _state = new QueuedUploadState(pos,_fileDesc);
            break;
		case LIMIT_REACHED:
			_state = new LimitReachedUploadState(_fileDesc);
			break;
		case PUSH_FAILED:
			_state = new PushFailedUploadState();
			break;
		case FREELOADER:     
			_state = new FreeloaderUploadState();
			break;
        case BROWSE_HOST:
            _state = new BrowseHostUploadState(this);
            break;
        case UPDATE_FILE:
            _state = new UpdateFileState(this);
            break;
		case FILE_NOT_FOUND:
			_state = new FileNotFoundUploadState();
            break;
        case MALFORMED_REQUEST:
            _state = new MalformedRequestState();
		case COMPLETE:
		case INTERRUPTED:		   
			break;
		}
	}
	
	OutputStream getOutputStream() {return _ostream;}
	InputStream getInputStream() {return _fis;}
    
    
	/**
     * Returns the current HTTP Request Method.
     */
	public HTTPRequestMethod getMethod() {
        return _method;
    }
    
    /**
     * Returns the queued position if queued.
     */
    public int getQueuePosition() {
        if( _stateNum != QUEUED )
            return -1;
        else
            return RouterService.getUploadManager().positionInQueue(_socket);
    }

	/**
	 * Sets the number of bytes that have been uploaded for this upload.
	 * This is expected to restart from 0 for each chunk of an HTTP/1.1
	 * transfer.
	 *
	 * @param amount the number of bytes that have been uploaded
	 */
	void setAmountUploaded(int amount) {
		int newData = amount - _amountRead;
		if(RECORD_STATS && newData > 0) {
            BandwidthStat.HTTP_BODY_UPSTREAM_BANDWIDTH.addData(newData);
		}
		_amountRead = amount;
	}
    
	/**
	 * Returns whether or not this upload is in what is considered an "inactive"
	 * state, such as completeed, aborted, failed, etc.
	 *
	 * @return <tt>true</tt> if this upload is in an inactive state,
	 *  <tt>false</tt> otherwise
	 */
	public boolean isInactive() {
        switch(_stateNum) {
        case COMPLETE:
        case PUSH_FAILED:
        case LIMIT_REACHED:
        case INTERRUPTED:
        case FILE_NOT_FOUND:
        case FREELOADER:
            return true;
        default:
            return false;
        }
	}	 
	 
    /** The byte offset where we should start the upload. */
	int getUploadBegin() {return _uploadBegin;}
    /** Returns the offset of the last byte to send <b>PLUS ONE</b>. */
    int getUploadEnd() {return _uploadEnd;}

	// implements the Uploader interface
	public int getFileSize() {return _fileSize;}
	
	// implements the Uploader interface
	public int getAmountRequested() { return _amountRequested; }

	// implements the Uploader interface
	public int getIndex() {return _index;}

	// implements the Uploader interface
	public String getFileName() {return _fileName;}

	// implements the Uploader interface
	public int getState() {return _stateNum;}

	// implements the Uploader interface
	public String getHost() {return _hostName;}

	// implements the Uploader interface
	public boolean isChatEnabled() {return _chatEnabled;}
	
	// implements the Uploader interface
	public boolean isBrowseHostEnabled() { return _browseEnabled; }

	// implements the Uploader interface
	public int getGnutellaPort() {return _gnutellaPort;}
	
	//implements the Uploader interface
	public String getUserAgent() { return _userAgent; }
	
	//implements the Uploader interface
	public boolean isHeaderParsed() { return _headersParsed; }

    public boolean supportsQueueing() {
        return _supportsQueueing && isValidQueueingAgent();
	}
    
    /**
     * Blocks certain vendors from being queued, because of buggy
     * downloading implementations on their side.
     */
    private boolean isValidQueueingAgent() {
        return !_userAgent.startsWith("Morpheus 3.0.2");
    }
    
    /**
     * The amount of bytes that this upload has transferred.
     * For HTTP/1.1 transfers, this number is the amount uploaded
     * for this specific chunk only.  Uses getTotalAmountUploaded
     * for the entire amount uploaded.
     *
	 * Implements the Uploader interface.
     */
	public int amountUploaded() {return _amountRead;}
	
	/**
	 * The total amount of bytes that this upload and all previous
	 * uploaders have transferred on this socket in this file-exchange.
	 *
	 * Implements the Uploader interface.
	 */
	public int getTotalAmountUploaded() { 
	    return _totalAmountRead + _amountRead;
    }

	/**
	 * Returns the <tt>FileDesc</tt> instance for this uploader.
	 *
	 * @return the <tt>FileDesc</tt> instance for this uploader, or
	 *  <tt>null</tt> if the <tt>FileDesc</tt> could not be assigned
	 *  from the shared files
	 */
	public FileDesc getFileDesc() {return _fileDesc;}

    boolean getClientAcceptsXGnutellaQueryreplies() {
        return _clientAcceptsXGnutellaQueryreplies;
    }

	/**
     * Reads the HTTP header sent by the requesting client -- note that the
	 * 'GET' portion of the request header has already been read.
	 *
	 * @param iStream the input stream to read the headers from.
	 * @throws <tt>IOException</tt> if there are any io issues while reading
	 *  the header
	 */
	public void readHeader(InputStream iStream) throws IOException {
        _uploadBegin = 0;
        _uploadEnd = 0;
		_clientAcceptsXGnutellaQueryreplies = false;
        
		ByteReader br = new ByteReader(iStream);
        
        try {
        	while (true) {
        		// read the line in from the socket.
                String str = br.readLine();

                if ( (str==null) || (str.equals("")) ) 
                    break;

                if(RECORD_STATS) 
					BandwidthStat.
                        HTTP_HEADER_DOWNSTREAM_BANDWIDTH.addData(str.length());
                debug("HTTPUploader.readHeader(): str = " +  str);
                
        		// break out of the loop if it is null or blank

                if      ( readChatHeader(str)        ) ;
                else if ( readRangeHeader(str)       ) ;
                else if ( readUserAgentHeader(str)   ) ;
                else if ( readContentURNHeader(str)  ) ;
                else if ( readAltLocationHeader(str) ) ;
                else if ( readAcceptHeader(str)      ) ;
                else if ( readQueueVersion(str)      ) ;
        	}
        } finally {
            // we want to ensure these are always set, regardless
            // of if an exception was thrown.
            
			//if invalid end-index, then upload up to the end of file
			if( _uploadEnd <= 0
			   || _uploadEnd <= _uploadBegin 
			   || _uploadEnd > _fileSize ) {
			    _uploadEnd = _fileSize;      
		    }		
            _amountRequested = _uploadEnd - _uploadBegin;		
            _headersParsed = true;
        }
	}
	

    /**
     * Read the chat portion of a header.
     * @return true if it had a chat header.
     */
    private boolean readChatHeader(String str) throws IOException {
        if (str.toUpperCase().indexOf("CHAT:") == -1)
            return false;
    
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
		_browseEnabled = true;
		_gnutellaPort = port;
        
        return true;
    }
    
    /**
	 * Look for range header of form, "Range: bytes=", "Range:bytes=",
	 * "Range: bytes ", etc.  Note that the "=" is required by HTTP, but
     * old versions of BearShare do not send it.  The value following the
     * bytes unit will be in the form '-n', 'm-n', or 'm-'.
     *
     * @return true if it had a Range header
     */
    private boolean readRangeHeader(String str) throws IOException {
        // was: != 0, is == -1 (that okay?)
        if ( indexOfIgnoreCase(str, "Range:") == -1 )
            return false;
            
        //Set 'sub' to the value after the "bytes=" or "bytes ".  Note
        //that we don't validate the data between "Range:" and the
        //bytes.
		String sub;
		String second;
		try {
            int i=str.indexOf("bytes");    //TODO: use constant
            if (i<0)
                throw new IOException("bytes not present in range");
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
        
        return true;
    }
    
    /**
     * Read the User-Agent field of the header
     *
     * @return true if the header had a UserAgent field
     */
    private boolean readUserAgentHeader(String str)
		throws FreeloaderUploadingException {
        if ( indexOfIgnoreCase(str, "User-Agent:") == -1 )
            return false;
        
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
		_userAgent = str.substring(11).trim();
		
		return true;
    }
    
    /**
	 * Read the content URN header
	 *
     * @return true if the header had a contentURN field
     */
    private boolean readContentURNHeader(String str) {
        if ( ! HTTPHeaderName.GNUTELLA_CONTENT_URN.matchesStartOfString(str) )
            return false;

        // Do we actually set the uploading file to be the URN anywhere?
        // Or do we expect that we recieved the right index/filename ?
        // IE: is it possible to a client to send a GET request with 
        // only the URN as the specifier?        
        URN requestedURN = HTTPUploader.parseContentUrn(str);
		if(requestedURN == null) {
            // TODO: do we really want to set the state to not found?
            // what if keywords match?
			setState(FILE_NOT_FOUND);
		} else if(_fileDesc != null) {
			if(!_fileDesc.containsUrn(requestedURN)) {
				setState(FILE_NOT_FOUND);
			}
		}
		
		return true;
	}
	
	/**
	 * Read the Alternate Locations header
	 *
	 * @return true if the header had an alternate locations field
	 */
	private boolean readAltLocationHeader(String str) {
        if ( ! HTTPHeaderName.ALT_LOCATION.matchesStartOfString(str) )
            return false;
        
   		if(_alternateLocationCollection == null && _fileDesc != null) 
   			_alternateLocationCollection = 
				AlternateLocationCollection.createCollection(_fileDesc.getSHA1Urn());
        if(_alternateLocationCollection != null)
            HTTPUploader.parseAlternateLocations(str, _alternateLocationCollection);

        return true;
    }

    /** 
     * Reads the Accept heder
     *
     * @return true if the header had an accept field
     */
    private boolean readAcceptHeader(String str) {
        if ( indexOfIgnoreCase(str, "accept:") == -1 )
            return false;
           
        if(indexOfIgnoreCase(str, Constants.QUERYREPLY_MIME_TYPE) != -1)
            _clientAcceptsXGnutellaQueryreplies = true;
            
        return true;
    }	

    private boolean readQueueVersion(String str) {
        if (! HTTPHeaderName.QUEUE_HEADER.matchesStartOfString(str))
            return false;
        
        //String s = HTTPUtils.extractHeaderValue(str);
        //we are not interested in the value at this point, the fact that the
        //header was sent implies that the uploader supports queueing. 
        _supportsQueueing = true;
        return true;
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
	private static URN parseContentUrn(final String contentUrnStr) {
		String urnStr = HTTPUtils.extractHeaderValue(contentUrnStr);
		
		// return null if the header value could not be extracted
		if(urnStr == null) return null;
		try {
			return URN.createSHA1Urn(urnStr);
		} catch(IOException e) {
			// this will be thrown if the URN string was invalid for any
			// reason -- just return null
			return null;
		}		
	}
	
	/**
	 * Parses the alternate location header.  The header can contain only one
	 * alternate location, or it can contain many in the same header.
	 * This method adds them all to the <tt>FileDesc</tt> for this
	 * uploader.  This will not allow more than 20 alternate locations
	 * for a single file.
	 *
	 * @param altHeader the full alternate locations header
	 * @param alc the <tt>AlternateLocationCollector</tt> that reads alternate
	 *  locations should be added to
	 */
	private static void parseAlternateLocations(final String altHeader,
												final AlternateLocationCollector alc) {
		final String alternateLocations = HTTPUtils.extractHeaderValue(altHeader);

		// return if the alternate locations could not be properly extracted
		if(alternateLocations == null) return;
		StringTokenizer st = new StringTokenizer(alternateLocations, ",");

		while(st.hasMoreTokens()) {
			try {
				// note that the trim method removes any CRLF character
				// sequences that may be used if the sender is using
				// continuations.
				AlternateLocation al = 
				    AlternateLocation.createAlternateLocation(st.nextToken().trim());
				
                URN sha1 = al.getSHA1Urn();
                if(sha1.equals(alc.getSHA1Urn())) {
                    alc.addAlternateLocation(al);
                }
			} catch(IOException e) {
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
        bandwidthTracker.measureBandwidth(getTotalAmountUploaded());
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
    
    public float getAverageBandwidth() {
        return bandwidthTracker.getAverageBandwidth();
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
        return "<"+_hostName+":"+_port+", "+ _index +">";
//  		return "HTTPUploader:\r\n"+
//  		       "File Name: "+_fileName+"\r\n"+
//  		       "Host Name: "+_hostName+"\r\n"+
//  		       "Port:      "+_port+"\r\n"+
//  		       "File Size: "+_fileSize+"\r\n"+
//  		       "State:     "+_state;
		
	}
}










