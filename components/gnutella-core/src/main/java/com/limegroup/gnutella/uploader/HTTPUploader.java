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
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.altlocs.*;

/**
 * Maintains state for an HTTP upload request.  This class follows the
 * State Pattern, delegating its writeResponse method to the appropriate
 * state.  All states except for CONNECTING, COMPLETE, and INTERRUPTED
 * have an associated state class that implements HTTPMessage.
 *
 * Care must be taken to call closeFileStreams whenever a chunk of the
 * transfer is finished, and to call stop when the entire HTTP/1.1
 * session is finished.
 *
 * A single HTTPUploader should be reused for multiple chunks of a single
 * file in an HTTP/1.1 session.  However, multiple HTTPUploaders
 * should be used for multiple files in a single HTTP/1.1 session.
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
	private int _stateNum = CONNECTING;
	private int _lastTransferStateNum;
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

    /**
     * The address as described by the "X-Node" header.
     */
    private InetAddress _nodeAddress = null;

    /**
     * The port as described by the "X-Node" header.
     */
    private int _nodePort = -1;
    
    /**
     * The parameters passed to the HTTP Request.
     */
    private Map _parameters = null;

    private BandwidthTrackerImpl bandwidthTracker=null;

	/**
	 * Stores good AlternateLocations read from the HTTP headers for this
	 * upload.  
     */
	private AlternateLocationCollection _goodLocs;

    /**
     * Stores bad AlternateLocations read from the HTTP Headers for this upload
     */
    private AlternateLocationCollection _badLocs;


    /**
     * The number of AltLocs that this uploader has written out. It is necessary
     * to rememeber this number beacuse uploader do not want to keep repeating
     * the same set of alternate locations to potential downloaders
     */
    private int _numAltsWritten;

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
	 * @param params the map of parameters in the http request.
     * @param dog the StalledUploadWatchdog to use for monitor stalls.
     * to initialize this' bandwidth tracker so we have history
	 */
	public HTTPUploader(HTTPRequestMethod method,
	                    String fileName, 
                        Socket socket,
                        int index,
                        Map params,
                        StalledUploadWatchdog dog) {
        STALLED_WATCHDOG = dog;
		_socket = socket;
		_hostName = _socket.getInetAddress().getHostAddress();
		_fileName = fileName;
		_index = index;
		reinitialize(method, params);
    }
    
    /**
     * Reinitializes this uploader for a new request method.
     *
     * @param method the HTTPRequestMethod to change to.
     * @param params the parameter list to change to.
     */
    public void reinitialize(HTTPRequestMethod method, Map params) {
        _method = method;
        _amountRequested = 0;
        _uploadBegin = 0;
        _uploadEnd = 0;
        _headersParsed = false;
        _stateNum = CONNECTING;
        _state = null;
        _gnutellaPort = 0;
        _supportsQueueing = false;
        _requestedURN = null;
        _clientAcceptsXGnutellaQueryreplies = false;
        _parameters = params;
        _goodLocs = null;
        _badLocs = null;
        
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
	}
	
	/**
	 * Sets the FileDesc for this HTTPUploader to use.
	 * Throws IOException if the file cannot be read from the disk.
	 */
	public void setFileDesc(FileDesc fd) throws IOException {
	    _fileDesc = fd;
	    _fileSize = (int)fd.getSize();
	    
        // if there already was an input stream, close it.
        if( _fis != null ) {
            try {
                _fis.close();
            } catch(IOException ignored) {}
        }
        _fis = _fileDesc.createInputStream();
	}
	
    /**
     * Creates the valid and failed AlternateLocationCollections with the
     * correct hash and initializes _goodLocs and _badLocs
     */
    public void initializeAltLocs() {
        Assert.that(_fileDesc!=null,"trying to upload a null file desc");
        _goodLocs = AlternateLocationCollection.create(_fileDesc.getSHA1Urn());
        _badLocs = AlternateLocationCollection.create(_fileDesc.getSHA1Urn());
    }
    

	/**
	 * Initializes the OutputStream for this HTTPUploader to use.
	 * Throws IOException if the connection was closed.
	 */
	public void initializeStreams() throws IOException {
	    _ostream = _socket.getOutputStream();
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
	public void writeResponse() throws IOException {
		try {
			_method.writeHttpResponse(_state, _ostream);
		} catch (IOException e) {
            // Only propogate the exception if they did not read
            // as much as they wanted to.
            if ( _amountRead < _amountRequested )
                throw e;
		} finally {    
    		if( _fileDesc != null) {
                //Synchronization note: the following two synchronization blocks
                //hold the locks of two AlternateLocationCollections
                //simultaneously. This may seem like a festering ground for
                //deadlocks, but this is not dangerous because the locks for
                //goodLoc and badLocs cannot be held by more than one thread.
                synchronized(_badLocs) {
                    Iterator iter = _badLocs.iterator();
                    while(iter.hasNext()) {
                        AlternateLocation loc =
                        ((AlternateLocation)iter.next()).createClone();
                        Assert.that(loc!=null,"problem cloning AltLoc");
                        _fileDesc.remove(loc);
                    }
                }
                synchronized(_goodLocs) {
                    Iterator iter = _goodLocs.iterator();
                    while(iter.hasNext()) {
                        AlternateLocation loc = 
                        ((AlternateLocation)iter.next()).createClone();
                        Assert.that(loc!=null,"problem cloning AltLoc");
                        _fileDesc.add(loc);
                    }
                }
            }
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
	 * Close the file input stream.
	 */
	public void closeFileStreams() {
        try {
            if( _fis != null )
                _fis.close();
        } catch(IOException e) {}
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
		case UPLOADING:
			_state = new NormalUploadState(this, STALLED_WATCHDOG);
			break;
        case QUEUED:
            int pos=RouterService.getUploadManager().positionInQueue(_socket);
            _state = new QueuedUploadState(pos,this);
            break;
		case LIMIT_REACHED:
			_state = new LimitReachedUploadState(this);
			break;
		case FREELOADER:     
			_state = new FreeloaderUploadState();
			break;
        case BROWSE_HOST:
            _state = new BrowseHostUploadState(this);
            break;
        case PUSH_PROXY:
            _state = new PushProxyUploadState(this);
            break;
        case UPDATE_FILE:
            _state = new UpdateFileState(this);
            break;
		case FILE_NOT_FOUND:
			_state = new FileNotFoundUploadState();
            break;
        case MALFORMED_REQUEST:
            _state = new MalformedRequestState();
            break;
        case UNAVAILABLE_RANGE:
            _state = new UnavailableRangeUploadState(this);
            break;
		case COMPLETE:
		case INTERRUPTED:
		case CONNECTING:
		    _state = null;
			break;
        default:
            Assert.that(false, "Invalid state: " + state);
		}
		
		// look again to set the last transfer state.
		switch(state) {
	    case COMPLETE:
	    case INTERRUPTED:
	    case CONNECTING:
	        break;
        default:
            _lastTransferStateNum = state;
        }
	}
	
	/**
	 * Returns the output stream this uploader is writing to.
	 */
	OutputStream getOutputStream() {
        return _ostream;
    }
    
    /**
     * Returns the FileInputStream this uploader is reading from.
     */
	InputStream getInputStream() {
	    return _fis;
    }
    
    /**
     * Returns whether or not the current state wants
     * to close the connection.
     */
    public boolean getCloseConnection() {
        Assert.that(_state != null);
        return _state.getCloseConnection();
    }    
    
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
        if( _lastTransferStateNum != QUEUED || _stateNum == INTERRUPTED)
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
	 * state, such as completed or aborted.
	 *
	 * @return <tt>true</tt> if this upload is in an inactive state,
	 *  <tt>false</tt> otherwise
	 */
	public boolean isInactive() {
        switch(_stateNum) {
        case COMPLETE:
        case INTERRUPTED:
            return true;
        default:
            return false;
        }
	}
	
	/**
	 * Returns the parameter list of this HTTPUploader.
	 */
	Map getParameters() {
	    return _parameters;
	}
	 
    /** The byte offset where we should start the upload. */
	public int getUploadBegin() {return _uploadBegin;}
    /** Returns the offset of the last byte to send <b>PLUS ONE</b>. */
    public int getUploadEnd() {return _uploadEnd;}

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
	public int getLastTransferState() { return _lastTransferStateNum; }

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
     * package access, generates an AlternateLocationCollection based on the
     * number of AltLocs that this Uploader has already sent out.
     */
    AlternateLocationCollection getAlternateLocationCollection() {
        AlternateLocationCollection coll =
                                    _fileDesc.getAlternateLocationCollection();
        AlternateLocationCollection ret=
                    AlternateLocationCollection.create(_fileDesc.getSHA1Urn());
        synchronized(coll) {
            Iterator iter  = coll.iterator();
            for(int i=0; iter.hasNext() && i < _numAltsWritten ; i++) 
                iter.next(); //skip the first _numAltsWritten
            int i;
            //Synchronization note: We hold the locks of two
            //AlternateLocationCollections concurrently, but one of them is a
            //local variable, so we are OK.
            for(i = 0; i< 10 && iter.hasNext();i++)
                ret.add((AlternateLocation)iter.next());
            _numAltsWritten += i;//add as many as we added now.
            return ret;
        }
    }
    
    /**
     * Blocks certain vendors from being queued, because of buggy
     * downloading implementations on their side.
     */
    private boolean isValidQueueingAgent() {
        if( _userAgent == null )
            return true;

        return !_userAgent.startsWith("Morpheus 3.0.2");
    }
    
    public InetAddress getNodeAddress() {return _nodeAddress; }
    
    public int getNodePort() {return _nodePort; }
    
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
     * Returns the content URN that the client asked for.
     */
    public URN getRequestedURN() {
        return _requestedURN;
    }

	/**
     * Reads the HTTP header sent by the requesting client -- note that the
	 * 'GET' portion of the request header has already been read.
	 *
	 * @param iStream the input stream to read the headers from.
	 * @throws <tt>IOException</tt> if the connection closes while reading
	 * @throws <tt>ProblemReadingHeaderException</tt> if any header is invalid
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
                else if ( readNAltLocationHeader(str)) ;
                else if ( readAcceptHeader(str)      ) ;
                else if ( readQueueVersion(str)      ) ;
                else if ( readNodeHeader(str)        ) ;
        	}
        } catch(ProblemReadingHeaderException prhe) {
            // there was a problem reading the header.. gobble up
            // the rest of the input and rethrow the exception
            while(true) {
                String str = br.readLine();
                if( str == null || str.equals("") )
                 break;
            }
            
            throw prhe;
        } finally {
            // we want to ensure these are always set, regardless
            // of if an exception was thrown.
            
			//if invalid end-index, then upload up to the end of file
			//or mark as unknown to bet when file size is set.
			if( _uploadEnd <= 0 ||
			  _uploadEnd <= _uploadBegin || 
			  _uploadEnd > _fileSize) {
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
			throw new ProblemReadingHeaderException();
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
			throw new ProblemReadingHeaderException();
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
                throw new ProblemReadingHeaderException(
                     "bytes not present in range");
            i+=6;                          //TODO: use constant
			sub = str.substring(i);
		} catch (IndexOutOfBoundsException e) {
			throw new ProblemReadingHeaderException();
		}
		// remove the white space
        sub = sub.trim();   
        char c;
		// get the first character
		try {
			c = sub.charAt(0);
		} catch (IndexOutOfBoundsException e) {
			throw new ProblemReadingHeaderException();
		}
		// - n  
        if (c == '-') {  
			// String second;
			try {
				second = sub.substring(1);
			} catch (IndexOutOfBoundsException e) {
				throw new ProblemReadingHeaderException();
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
				throw new ProblemReadingHeaderException();
			}
        }
        else {                
			// m - n or 0 -
            int dash = sub.indexOf("-");
			String first;
			try {
				first = sub.substring(0, dash);
			} catch (IndexOutOfBoundsException e) {
				throw new ProblemReadingHeaderException();
			}
            first = first.trim();
			try {
				_uploadBegin = java.lang.Integer.parseInt(first);
			} catch (NumberFormatException e) {
				throw new ProblemReadingHeaderException();
			}
			try {
				second = sub.substring(dash+1);
			} catch (IndexOutOfBoundsException e) {
				throw new ProblemReadingHeaderException();
			}
            second = second.trim();
            if (!second.equals("")) 
				try {
                    //HTTP range requests are inclusive.  So "1-3" means
                    //bytes 1, 2, and 3.  But _uploadEnd is an EXCLUSIVE
                    //index, so increment by 1.
					_uploadEnd = java.lang.Integer.parseInt(second)+1;
            } catch (NumberFormatException e) {
				throw new ProblemReadingHeaderException();
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
		if (SharingSettings.ALLOW_BROWSER.getValue() == false
            && !(_stateNum == BROWSE_HOST)  
            && !(_stateNum == PUSH_PROXY)  
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

        _requestedURN = HTTPUploader.parseContentUrn(str);
		
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
                
        if(_goodLocs != null) 
            HTTPUploader.parseAlternateLocations(str, _goodLocs);
        return true;
    }

    private boolean readNAltLocationHeader(String str) {
        if (!HTTPHeaderName.NALTS.matchesStartOfString(str))
            return false;
        
        if(_badLocs != null)
            HTTPUploader.parseAlternateLocations(str,_badLocs);
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
     * Reads the X-Node header
     *
     * @return true if the header had an node description value
     */
    private boolean readNodeHeader(final String str) {
        if ( indexOfIgnoreCase(str, "X-Node") == -1 )
            return false;
           
        StringTokenizer st = 
            new StringTokenizer(HTTPUtils.extractHeaderValue(str), ":");
        InetAddress tempAddr = null;
        int tempPort = -1;
        // we are expecting 2 tokens - only evalute if you see 2
        if (st.countTokens() == 2) {
            try {
                tempAddr = InetAddress.getByName(st.nextToken().trim());
                tempPort = Integer.parseInt(st.nextToken().trim());
                if (NetworkUtils.isValidPort(tempPort)) {
                    // everything checks out....
                    _nodeAddress = tempAddr;
                    _nodePort = tempPort;
                }
            }
            catch (UnknownHostException badHost) { // crappy host
            }
            catch (NumberFormatException nfe) {} // crappy port
        }
            
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
		
		if(urnStr == null)
		    return URN.INVALID;
		try {
			return URN.createSHA1Urn(urnStr);
		} catch(IOException e) {
		    return URN.INVALID;
		}		
	}
	
	/**
	 * Parses the alternate location header.  The header can contain only one
	 * alternate location, or it can contain many in the same header.
	 * This method will notify DownloadManager of new alternate locations
	 * if the FileDesc is an IncompleteFileDesc.
	 *
	 * @param altHeader the full alternate locations header
	 * @param alc the <tt>AlternateLocationCollector</tt> that reads alternate
	 *  locations should be added to
	 */
	private static void parseAlternateLocations(final String altHeader,
                                       final AlternateLocationCollector alc) {

		final String alternateLocations=HTTPUtils.extractHeaderValue(altHeader);

		// return if the alternate locations could not be properly extracted
		if(alternateLocations == null) return;
		StringTokenizer st = new StringTokenizer(alternateLocations, ",");
        while(st.hasMoreTokens()) {
            try {
                // note that the trim method removes any CRLF character
                // sequences that may be used if the sender is using
                // continuations.
                AlternateLocation al = 
                AlternateLocation.create(st.nextToken().trim());
                
                URN sha1 = al.getSHA1Urn();
                if(sha1.equals(alc.getSHA1Urn()))
                    alc.add(al);
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
    
    private final boolean debugOn = false;
    private void debug(String out) {
        if (debugOn)
            System.out.println(out);
    }

	// overrides Object.toString
	public String toString() {
        return "<"+_hostName+":"+ _index +">";
//  		return "HTTPUploader:\r\n"+
//  		       "File Name: "+_fileName+"\r\n"+
//  		       "Host Name: "+_hostName+"\r\n"+
//  		       "Port:      "+_port+"\r\n"+
//  		       "File Size: "+_fileSize+"\r\n"+
//  		       "State:     "+_state;
		
	}
}










