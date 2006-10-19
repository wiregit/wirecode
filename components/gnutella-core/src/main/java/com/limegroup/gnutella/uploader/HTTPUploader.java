package com.limegroup.gnutella.uploader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.ByteReader;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UploadManager;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationCollection;
import com.limegroup.gnutella.altlocs.DirectAltLoc;
import com.limegroup.gnutella.altlocs.PushAltLoc;
import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HTTPMessage;
import com.limegroup.gnutella.http.HTTPRequestMethod;
import com.limegroup.gnutella.http.HTTPUtils;
import com.limegroup.gnutella.http.ProblemReadingHeaderException;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.statistics.BandwidthStat;
import com.limegroup.gnutella.udpconnect.UDPConnection;
import com.limegroup.gnutella.util.CountingOutputStream;
import com.limegroup.gnutella.util.MultiRRIterator;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.StringUtils;

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
    
	private static final Log LOG = LogFactory.getLog(HTTPUploader.class);
    /**
     * The outputstream -- a CountingOutputStream so that we can
     * keep track of the amount of bytes written.
     * Currently track is only kept for writing a THEX tree, so that
     * progress of the tree and bandwidth measurement may be done.
     */
	private CountingOutputStream _ostream;
	private InputStream _fis;
	private final HTTPSession session;
	private final Socket _socket;
	private int _totalAmountReadBefore;
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
	private int _stateNum = CONNECTING;
	private int _lastTransferStateNum;
	private HTTPMessage _state;
	private boolean _firstReply = true;
	private boolean _containedRangeRequest = false;
	
	private boolean _chatEnabled;
	private boolean _browseEnabled;
    private boolean _supportsQueueing = false;
    
    /**
     * True if this is a forcibly shared network file.
     */
    private boolean _isForcedShare = false;
    
    /**
     * whether the remote side indicated they want to receive
     * firewalled altlocs.
     */
    private boolean _wantsFalts = false;
    
    /**
     * the version of the FWT protocol the remote supports.  
     * Non-firewalled hosts should not send this feature.
     * INVARIANT: if this is greater than 0, _wantsFalts is set.
     */
    private int _FWTVersion = 0;

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
    private Map<String, Object> _parameters = null;

    /**
     * The alternate locations that have been written out (as good) locations.
     */
    private Set<DirectAltLoc> _writtenLocs;
    
    /**
     * The firewalled alternate locations that have been written out as good locations.
     */
    private Set<PushAltLoc> _writtenPushLocs;
    
    /**
     * The maximum number of alts to write per http transfer.
     */
    private static final int MAX_LOCATIONS = 10;
    
    /**
     * The maximum number of firewalled alts to write per http transfer.
     */
    private static final int MAX_PUSH_LOCATIONS = 5;

	/**
	 * The <tt>HTTPRequestMethod</tt> to use for the upload.
	 */
	private HTTPRequestMethod _method;

	/**
	 * Consructor for a "normal" non-push upload.  Note that this can
	 * be a URN get request.
	 *
	 * @param method the <tt>HTTPRequestMethod</tt> for the request
	 * @param fileName the name of the file
	 * @param session the <tt>HTTPSession</tt> associated with this upload
	 * @param index the index of the file in the set of shared files
	 * @param params the map of parameters in the http request.
     * @param dog the StalledUploadWatchdog to use for monitor stalls.
     * @param hadPassword the get line had a matching password.
     * to initialize this' bandwidth tracker so we have history
	 */
	public HTTPUploader(HTTPRequestMethod method,
	                    String fileName, 
                        HTTPSession session,
                        int index,
                        Map<String, Object> params,
                        StalledUploadWatchdog dog) {

        STALLED_WATCHDOG = dog;
        this.session = session;
		_socket = session.getSocket();
		_fileName = fileName;
		_index = index;
		_writtenLocs = null;
        _totalAmountRead = 0;
        _amountRead = 0;
		reinitialize(method, params);
    }
    
    /**
     * Reinitializes this uploader for a new request method.
     *
     * @param method the HTTPRequestMethod to change to.
     * @param params the parameter list to change to.
     */
    public void reinitialize(HTTPRequestMethod method, Map<String, Object> params) {
        _method = method;
        _amountRequested = 0;
        _uploadBegin = 0;
        _uploadEnd = 0;
        _headersParsed = false;
        _stateNum = CONNECTING;
        _state = null;
        _nodePort = 0;
        _supportsQueueing = false;
        _requestedURN = null;
        _clientAcceptsXGnutellaQueryreplies = false;
        _parameters = params;
        _totalAmountReadBefore = 0;
        _totalAmountRead += _amountRead;
        _amountRead = 0;
        
	}
	
	/**
	 * Sets the FileDesc for this HTTPUploader to use.
	 * 
	 * @param fd the <tt>FileDesc</tt> to use
	 * @throws IOException if the file cannot be read from the disk.
	 */
	public void setFileDesc(FileDesc fd) throws IOException {
		if (LOG.isDebugEnabled())
			LOG.debug("trying to set the fd for uploader "+this+ " with "+fd);
	    _fileDesc = fd;
	    _fileSize = (int)fd.getFileSize();
	    // initializd here because we'll only write locs if a FileDesc exists
	    // only initialize once, so we don't write out previously written locs
	    if( _writtenLocs == null )
	        _writtenLocs = new HashSet<DirectAltLoc>();
	    
	    if( _writtenPushLocs == null )
	        _writtenPushLocs = new HashSet<PushAltLoc>(); 
	    
        // if there already was an input stream, close it.
        if( _fis != null ) {
        	if (LOG.isDebugEnabled())
        		LOG.debug(this+ " had an existing stream");
            try {
                _fis.close();
            } catch(IOException ignored) {}
        }
        _fis = _fileDesc.createInputStream();
        _isForcedShare = FileManager.isForcedShare(_fileDesc);
	}

	/**
	 * Initializes the OutputStream for this HTTPUploader to use.
	 * 
	 * @throws IOException if the connection was closed.
	 */
	public void initializeStreams() throws IOException {
	    _ostream = new CountingOutputStream(_socket.getOutputStream());
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
        _ostream.setIsCounting(_stateNum == THEX_REQUEST);
		try {
			_method.writeHttpResponse(_state, _ostream);
		} catch (IOException e) {
            // Only propogate the exception if they did not read
            // as much as they wanted to.
            if ( amountUploaded() < getAmountRequested() )
                throw e;
		}
		_firstReply = false;
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
            int pos = session.positionInQueue();
            _state = new QueuedUploadState(pos,this);
            break;
        case NOT_VALIDATED:
            _state = new LimitReachedUploadState(this, true);
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
        case BROWSER_CONTROL:
            _state = new BrowserControlUploadState(this);
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
        case BANNED_GREEDY:
        	_state = new BannedUploadState();
        	break;
        case THEX_REQUEST:
        	_state = new THEXUploadState(this, STALLED_WATCHDOG);
        	break;
		case COMPLETE:
		case INTERRUPTED:
		case CONNECTING:
		    _state = null;
			break;
        default:
            Assert.that(false, "Invalid state: " + state);
		}
		
		if(_state != null)
		    _lastTransferStateNum = state;
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
     * Returns the InetAddress of the socket we're connected to.
     */
    public InetAddress getConnectedHost() {
        if(_socket == null)
            return null;
        else
            return _socket.getInetAddress();
    }
    
    /**
      * Determines if this is uploading to via a UDP transfer.
      */
    boolean isUDPTransfer() {
        return (_socket instanceof UDPConnection);
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
            return session.positionInQueue();
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
		if(newData > 0) {
            if (isForcedShare())
                BandwidthStat.HTTP_BODY_UPSTREAM_INNETWORK_BANDWIDTH.addData(newData);
            else
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
	Map<String, Object> getParameters() {
	    return _parameters;
	}
	 
    /** The byte offset where we should start the upload. */
	public int getUploadBegin() {return _uploadBegin;}
    /** Returns the offset of the last byte to send <b>PLUS ONE</b>. */
    public int getUploadEnd() {return _uploadEnd;}
    
    /**
     * Set new upload begin & end values, modifying the amount requested.
     */
    public void setUploadBeginAndEnd(int begin, int end) {
        _uploadBegin = begin;
        _uploadEnd = end;
        _amountRequested = _uploadEnd - _uploadBegin;
    }
    
    /**
     * Whether or not the last request to this HTTPUploader contained
     * a 'Range: ' header, so we can truncate the requested range.
     */
    public boolean containedRangeRequest() {
        return _containedRangeRequest;
    }

	// implements the Uploader interface
	public long getFileSize() {
	    if(_stateNum == THEX_REQUEST)
	        return _fileDesc.getHashTree().getOutputLength();
	    else
	        return _fileSize;
    }
	
	// implements the Uploader interface
	public long getAmountRequested() {
	    if(_stateNum == THEX_REQUEST)
	        return _fileDesc.getHashTree().getOutputLength();
	    else
	        return _amountRequested;
    }

	// implements the Uploader interface
	public int getIndex() {return _index;}

	// implements the Uploader interface
	public String getFileName() {return _fileName;}

	// implements the Uploader interface
	public int getState() {return _stateNum;}
	
	// implements the Uploader interface
	public int getLastTransferState() { return _lastTransferStateNum; }

	// implements the Uploader interface
	public String getHost() {return session.getHost();}

	// implements the Uploader interface
	public boolean isChatEnabled() {return _chatEnabled;}
	
	// implements the Uploader interface
	public boolean isBrowseHostEnabled() { return _browseEnabled; }

	// implements the Uploader interface
	public int getGnutellaPort() {return _nodePort;}
	
	//implements the Uploader interface
	public String getUserAgent() { return _userAgent; }
	
	//implements the Uploader interface
	public boolean isHeaderParsed() { return _headersParsed; }
	
	// is a forced network share?
	public boolean isForcedShare() { return _isForcedShare; }

    public boolean supportsQueueing() {
        return _supportsQueueing && isValidQueueingAgent();
	}
	
	public boolean isTHEXRequest() {
		return HTTPConstants.NAME_TO_THEX.equals(
				_parameters.get(UploadManager.SERVICE_ID));
	}
    	
    
    /**
     * Returns an AlternateLocationCollection of alternates that
     * have not been sent out already.
     */
    Set<DirectAltLoc> getNextSetOfAltsToSend() {
        AlternateLocationCollection<DirectAltLoc> coll =
            RouterService.getAltlocManager().getDirect(_fileDesc.getSHA1Urn());
        Set<DirectAltLoc> ret = null;
        long now = System.currentTimeMillis();
        synchronized(coll) {
            Iterator<DirectAltLoc> iter  = coll.iterator();
            for(int i = 0; iter.hasNext() && i < MAX_LOCATIONS;) {
                DirectAltLoc al = iter.next();
                if(_writtenLocs.contains(al))
                    continue;
                
                if (al.canBeSent(AlternateLocation.MESH_LEGACY)) {
                    _writtenLocs.add(al);
                    if(ret == null)
                        ret = new HashSet<DirectAltLoc>();
                    ret.add(al);
                    i++;
                    al.send(now,AlternateLocation.MESH_LEGACY);
                } else if (!al.canBeSentAny()) 
                    iter.remove();
            }
        }
        if(ret == null)
            return Collections.emptySet();
        else
            return ret;
     
    }
    
    Set<PushAltLoc> getNextSetOfPushAltsToSend() {
        if (!_wantsFalts)
            return Collections.emptySet();
        
    	AlternateLocationCollection<PushAltLoc> fwt = 
            RouterService.getAltlocManager().getPush(_fileDesc.getSHA1Urn(), true);
        
        AlternateLocationCollection<PushAltLoc> push;
        if(_FWTVersion > 0)
            push = AlternateLocationCollection.getEmptyCollection();
        else
            push = RouterService.getAltlocManager().getPush(_fileDesc.getSHA1Urn(), false);
    	
    	Set<PushAltLoc> ret = null;
    	long now = System.currentTimeMillis();
    	synchronized(push) {
    	    synchronized (fwt) {
    	        Iterator<PushAltLoc> iter  = 
    	        	new MultiRRIterator<PushAltLoc>(fwt.iterator(),push.iterator());
    	        for(int i = 0; iter.hasNext() && i < MAX_PUSH_LOCATIONS;) {
    	            PushAltLoc al = iter.next();
    	            
    	            if(_writtenPushLocs.contains(al))
    	                continue;
    	            
    	            // it is possible to end up having a PE with all
    	            // proxies removed.  In that case we remove it explicitly
    	            if(al.getPushAddress().getProxies().isEmpty()) {
    	                iter.remove();
    	                continue;
    	            }
    	            
                    if (al.canBeSent(AlternateLocation.MESH_LEGACY)) {
                        al.send(now,AlternateLocation.MESH_LEGACY);
                        _writtenPushLocs.add(al);
                        
                        if(ret == null)
                            ret = new HashSet<PushAltLoc>();
                        ret.add(al);
                        i++;
                    } else if (!al.canBeSentAny())
                        iter.remove();
    	        }
    	    }
    	}
        
        if(ret == null)
            return Collections.emptySet();
        else
            return ret;
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
    
    protected boolean isFirstReply () {
    	return _firstReply;
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
	public long amountUploaded() {
	    if(_stateNum == THEX_REQUEST) {
	        if(_ostream == null)
	            return 0;
	        else
	            return _ostream.getAmountWritten();
	    } else
	        return _amountRead;
    }
	
	/**
	 * The total amount of bytes that this upload and all previous
	 * uploaders have transferred on this socket in this file-exchange.
	 *
	 * Implements the Uploader interface.
	 */
	public long getTotalAmountUploaded() {
	    if(_stateNum == THEX_REQUEST) {
	        if(_ostream == null)
	            return 0;
	        else
	            return _ostream.getAmountWritten();
	    } else {
			if ( _totalAmountReadBefore > 0 )
				return _totalAmountReadBefore + _amountRead;
			else
	        return _totalAmountRead + _amountRead;
    }
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
        _containedRangeRequest = false;
		_clientAcceptsXGnutellaQueryreplies = false;
		_totalAmountReadBefore = 0;
        
		ByteReader br = new ByteReader(iStream);
        
        try {
        	while (true) {
        		// read the line in from the socket.
                String str = br.readLine();

                if ( (str==null) || (str.equals("")) ) 
                    break;


                if (isForcedShare())
                    BandwidthStat.HTTP_HEADER_DOWNSTREAM_INNETWORK_BANDWIDTH.addData(str.length());
                else 
                    BandwidthStat.
                        HTTP_HEADER_DOWNSTREAM_BANDWIDTH.addData(str.length());
                if (LOG.isDebugEnabled())
                	LOG.debug("HTTPUploader.readHeader(): str = " +  str);

                
        		// break out of the loop if it is null or blank

                if      ( readChatHeader(str)        ) ;
                else if ( readRangeHeader(str)       ) ;
                else if ( readUserAgentHeader(str)   ) ;
                else if ( readContentURNHeader(str)  ) ;
                else if ( readAltLocationHeader(str) ) ;
                else if ( readNAltLocationHeader(str)) ;
                else if ( readFAltLocationHeader(str)) ;
                else if ( readNFAltLocationHeader(str));
                else if ( readAcceptHeader(str)      ) ;
                else if ( readQueueVersion(str)      ) ;
                else if ( readNodeHeader(str)        ) ;
                else if ( readFeatureHeader(str)     ) ;
                else if ( readXDownloadedHeader(str) ) ;
        	}
        } catch(ProblemReadingHeaderException prhe) {
            // there was a problem reading the header.. gobble up
            // the rest of the input and rethrow the exception
            while(true) {
                String str = br.readLine();
                if( str == null || str.equals("") )
                 break;
            }
            
            // TODO: record stats for this
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
		_nodePort = port;
        
        return true;
    }
    
    /**
	 * Look for X-Downloaded header which represents number 
	 * of bytes for this file already downloaded by peer
	 *
     * @return true if it had a X-Downloaded header
     */
    private boolean readXDownloadedHeader(String str) {
        
        if ( !HTTPHeaderName.DOWNLOADED.matchesStartOfString(str) )
            return false;
            
		try {
			str = HTTPUtils.extractHeaderValue(str);
			if ( str != null ) {
				_totalAmountReadBefore = Integer.parseInt(str);
			}
		} 
		catch (NumberFormatException e) {}

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
        if ( StringUtils.indexOfIgnoreCase(str, "Range:") == -1 )
            return false;
            
        _containedRangeRequest = true;
            
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
            
            // If the "-" does not exist, the head is incorrectly formatted.
            if(dash == -1) {
                throw new ProblemReadingHeaderException();
            }
			String first = sub.substring(0, dash).trim();
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
        if ( StringUtils.indexOfIgnoreCase(str, "User-Agent:") == -1 )
            return false;
        
		// check for netscape, internet explorer,
		// or other free riding downoaders
        //Allow them to browse the host though
		if (SharingSettings.ALLOW_BROWSER.getValue() == false
            && !(_stateNum == BROWSE_HOST)  
            && !(_stateNum == BROWSER_CONTROL)  
            && !(_stateNum == PUSH_PROXY)  
			&& !(_fileName.toUpperCase().startsWith("LIMEWIRE"))) {
			// if we are not supposed to read from them
			// throw an exception
			if( (str.indexOf("Mozilla") != -1) ||
			    (str.indexOf("Morpheus") != -1) ||
				(str.indexOf("DA") != -1) ||
				(str.indexOf("Download") != -1) ||
				(str.indexOf("FlashGet") != -1) ||
				(str.indexOf("GetRight") != -1) ||
				(str.indexOf("Go!Zilla") != -1) ||
				(str.indexOf("Inet") != -1) ||
				(str.indexOf("MIIxpc") != -1) ||
				(str.indexOf("MSProxy") != -1) ||
				(str.indexOf("Mass") != -1) ||
				(str.indexOf("MLdonkey") != -1) ||
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
                
        if(_fileDesc != null) 
            parseAlternateLocations(str, true);
        return true;
    }

    private boolean readNAltLocationHeader(String str) {
        if (!HTTPHeaderName.NALTS.matchesStartOfString(str))
            return false;
        
        if(_fileDesc != null)
            parseAlternateLocations(str, false);
        return true;
    }
    
	private boolean readFAltLocationHeader(String str) {
        if ( ! HTTPHeaderName.FALT_LOCATION.matchesStartOfString(str) )
            return false;
        
        //also set the interested flag
        _wantsFalts=true;
        
        if(_fileDesc != null) 
            parseAlternateLocations(str, true);
        return true;
    }

    private boolean readNFAltLocationHeader(String str) {
        if (!HTTPHeaderName.BFALT_LOCATION.matchesStartOfString(str))
            return false;

        //also set the interested flag
        _wantsFalts=true;
        
        if(_fileDesc != null)
            parseAlternateLocations(str, false);
        return true;
    }
    
    
    

    /** 
     * Reads the Accept heder
     *
     * @return true if the header had an accept field
     */
    private boolean readAcceptHeader(String str) {
        if ( StringUtils.indexOfIgnoreCase(str, "accept:") == -1 )
            return false;
           
        if(StringUtils.indexOfIgnoreCase(str, Constants.QUERYREPLY_MIME_TYPE) != -1)
            _clientAcceptsXGnutellaQueryreplies = true;
            
        return true;
    }	

    private boolean readQueueVersion(String str) {
        if (! HTTPHeaderName.QUEUE.matchesStartOfString(str))
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
        if ( !HTTPHeaderName.NODE.matchesStartOfString(str) )
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
	 * Reads the X-Features header
	 *
	 * @return true if the header had an node description value
	 */
	private boolean readFeatureHeader(String str) {
		if ( !HTTPHeaderName.FEATURES.matchesStartOfString(str) )
			return false;
        str = HTTPUtils.extractHeaderValue(str);
        if (LOG.isDebugEnabled())
        	LOG.debug("reading feature header: "+str);
        StringTokenizer tok = new StringTokenizer(str, ",");
        while (tok.hasMoreTokens()) {
            String feature = tok.nextToken();
            String protocol = "";
            int slash = feature.indexOf("/");
            if(slash == -1) {
                protocol = feature.toLowerCase().trim();
            } else {
                protocol = feature.substring(0, slash).toLowerCase().trim();
            }
            // not interested in the version ...

			if (protocol.equals(HTTPConstants.CHAT_PROTOCOL))
				_chatEnabled = true;
			else if (protocol.equals(HTTPConstants.BROWSE_PROTOCOL))
				_browseEnabled = true;
			else if (protocol.equals(HTTPConstants.QUEUE_PROTOCOL))
				_supportsQueueing = true;
			else if (protocol.equals(HTTPConstants.PUSH_LOCS))
            	_wantsFalts=true;
            else if (protocol.equals(HTTPConstants.FW_TRANSFER)){
                // for this header we care about the version
            	try {
            	    _FWTVersion = (int)HTTPUtils.parseFeatureToken(feature);
            	    _wantsFalts=true;
            	}catch(ProblemReadingHeaderException prhe){
            	    continue;
            	}
            }
			
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
	private void parseAlternateLocations(final String altHeader, boolean isGood) {

		final String alternateLocations=HTTPUtils.extractHeaderValue(altHeader);

		URN sha1 =_fileDesc.getSHA1Urn(); 
		
		// return if the alternate locations could not be properly extracted
		if(alternateLocations == null) return;
		StringTokenizer st = new StringTokenizer(alternateLocations, ",");
        while(st.hasMoreTokens()) {
            try {
                // note that the trim method removes any CRLF character
                // sequences that may be used if the sender is using
                // continuations.
                AlternateLocation al = 
                AlternateLocation.create(st.nextToken().trim(),
                                         _fileDesc.getSHA1Urn());
                
                Assert.that(al.getSHA1Urn().equals(sha1));
                
                if (al.isMe())
                    continue;
                
                if(al instanceof PushAltLoc) 
                    ((PushAltLoc)al).updateProxies(isGood);
                // Note: if this thread gets preempted at this point,
                // the AlternateLocationCollectioin may contain a PE
                // without any proxies.
                if(isGood) 
                    RouterService.getAltlocManager().add(al, null);
                else
                    RouterService.getAltlocManager().remove(al, null);
                        
                if (al instanceof DirectAltLoc)
                 	_writtenLocs.add((DirectAltLoc)al);
                else 
                 	_writtenPushLocs.add((PushAltLoc)al); // no problem if we add an existing pushloc
            } catch(IOException e) {
                // just return without adding it.
                continue;
            }
        }
	}

	public void measureBandwidth() {
	    int written = _totalAmountRead + _amountRead;
	    if(_ostream != null)
	        written += _ostream.getAmountWritten();
        session.measureBandwidth(written);
    }

    public float getMeasuredBandwidth() {
        float retVal = 0;
        try {
            retVal = session.getMeasuredBandwidth();
        } catch (InsufficientDataException ide) {
            retVal = 0;
        }
        return retVal;
    }
    
    public float getAverageBandwidth() {
        return session.getAverageBandwidth();
    }
    
    public boolean wantsFAlts() {
    	return _wantsFalts;
    }
    
    public int wantsFWTAlts() {
    	return _FWTVersion;
    }
    
    public String getCustomIconDescriptor() {
    	return null;
    }

	// overrides Object.toString
	public String toString() {
        return "<"+getHost()+":"+ _index +">";
//  		return "HTTPUploader:\r\n"+
//  		       "File Name: "+_fileName+"\r\n"+
//  		       "Host Name: "+_hostName+"\r\n"+
//  		       "Port:      "+_port+"\r\n"+
//  		       "File Size: "+_fileSize+"\r\n"+
//  		       "State:     "+_state;
		
	}
}










