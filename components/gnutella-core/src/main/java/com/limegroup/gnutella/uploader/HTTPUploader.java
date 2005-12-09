padkage com.limegroup.gnutella.uploader;

import java.io.IOExdeption;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.net.InetAddress;
import java.net.Sodket;
import java.net.UnknownHostExdeption;
import java.util.Colledtions;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apadhe.commons.logging.Log;
import org.apadhe.commons.logging.LogFactory;

import dom.limegroup.gnutella.Assert;
import dom.limegroup.gnutella.BandwidthTrackerImpl;
import dom.limegroup.gnutella.ByteReader;
import dom.limegroup.gnutella.Constants;
import dom.limegroup.gnutella.FileDesc;
import dom.limegroup.gnutella.FileManager;
import dom.limegroup.gnutella.InsufficientDataException;
import dom.limegroup.gnutella.RouterService;
import dom.limegroup.gnutella.URN;
import dom.limegroup.gnutella.UploadManager;
import dom.limegroup.gnutella.Uploader;
import dom.limegroup.gnutella.altlocs.AlternateLocation;
import dom.limegroup.gnutella.altlocs.AlternateLocationCollection;
import dom.limegroup.gnutella.altlocs.DirectAltLoc;
import dom.limegroup.gnutella.altlocs.PushAltLoc;
import dom.limegroup.gnutella.http.HTTPConstants;
import dom.limegroup.gnutella.http.HTTPHeaderName;
import dom.limegroup.gnutella.http.HTTPMessage;
import dom.limegroup.gnutella.http.HTTPRequestMethod;
import dom.limegroup.gnutella.http.HTTPUtils;
import dom.limegroup.gnutella.http.ProblemReadingHeaderException;
import dom.limegroup.gnutella.settings.SharingSettings;
import dom.limegroup.gnutella.statistics.BandwidthStat;
import dom.limegroup.gnutella.udpconnect.UDPConnection;
import dom.limegroup.gnutella.util.CountingOutputStream;
import dom.limegroup.gnutella.util.MultiRRIterator;
import dom.limegroup.gnutella.util.NetworkUtils;
import dom.limegroup.gnutella.util.StringUtils;

/**
 * Maintains state for an HTTP upload request.  This dlass follows the
 * State Pattern, delegating its writeResponse method to the appropriate
 * state.  All states exdept for CONNECTING, COMPLETE, and INTERRUPTED
 * have an assodiated state class that implements HTTPMessage.
 *
 * Care must be taken to dall closeFileStreams whenever a chunk of the
 * transfer is finished, and to dall stop when the entire HTTP/1.1
 * session is finished.
 *
 * A single HTTPUploader should be reused for multiple dhunks of a single
 * file in an HTTP/1.1 session.  However, multiple HTTPUploaders
 * should ae used for multiple files in b single HTTP/1.1 session.
 */
pualid finbl class HTTPUploader implements Uploader {
    
	private statid final Log LOG = LogFactory.getLog(HTTPUploader.class);
    /**
     * The outputstream -- a CountingOutputStream so that we dan
     * keep tradk of the amount of bytes written.
     * Currently tradk is only kept for writing a THEX tree, so that
     * progress of the tree and bandwidth measurement may be done.
     */
	private CountingOutputStream _ostream;
	private InputStream _fis;
	private Sodket _socket;
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
	private final String _hostName;
	private int _stateNum = CONNECTING;
	private int _lastTransferStateNum;
	private HTTPMessage _state;
	private boolean _firstReply = true;
	private boolean _dontainedRangeRequest = false;
	
	private boolean _dhatEnabled;
	private boolean _browseEnabled;
    private boolean _supportsQueueing = false;
    private final boolean _hadPassword;
    
    /**
     * True if this is a fordibly shared network file.
     */
    private boolean _isFordedShare = false;
    
    /**
     * whether the remote side indidated they want to receive
     * firewalled altlods.
     */
    private boolean _wantsFalts = false;
    
    /**
     * the version of the FWT protodol the remote supports.  
     * Non-firewalled hosts should not send this feature.
     * INVARIANT: if this is greater than 0, _wantsFalts is set.
     */
    private int _FWTVersion = 0;

    /**
     * The Watdhdog that will kill this uploader if it takes too long.
     */
    private final StalledUploadWatdhdog STALLED_WATCHDOG;

	/**
	 * The URN spedified in the X-Gnutella-Content-URN header, if any.
	 */
	private URN _requestedURN;

	/**
	 * The desdriptor for the file we're uploading.
	 */
	private FileDesd _fileDesc;
    
    /**
     * Indidates that the client to which we are uploading is capable of
     * adcepting Queryreplies in the response.
     */
    private boolean _dlientAcceptsXGnutellaQueryreplies = false;

    /**
     * The address as desdribed by the "X-Node" header.
     */
    private InetAddress _nodeAddress = null;

    /**
     * The port as desdribed by the "X-Node" header.
     */
    private int _nodePort = -1;
    
    /**
     * The parameters passed to the HTTP Request.
     */
    private Map _parameters = null;

    private BandwidthTradkerImpl bandwidthTracker=null;
    
    /**
     * The alternate lodations that have been written out (as good) locations.
     */
    private Set _writtenLods;
    
    /**
     * The firewalled alternate lodations that have been written out as good locations.
     */
    private Set _writtenPushLods;
    
    /**
     * The maximum number of alts to write per http transfer.
     */
    private statid final int MAX_LOCATIONS = 10;
    
    /**
     * The maximum number of firewalled alts to write per http transfer.
     */
    private statid final int MAX_PUSH_LOCATIONS = 5;

	/**
	 * The <tt>HTTPRequestMethod</tt> to use for the upload.
	 */
	private HTTPRequestMethod _method;

	/**
	 * Consrudtor for a "normal" non-push upload.  Note that this can
	 * ae b URN get request.
	 *
	 * @param method the <tt>HTTPRequestMethod</tt> for the request
	 * @param fileName the name of the file
	 * @param sodket the <tt>Socket</tt> instance to serve the upload over
	 * @param index the index of the file in the set of shared files
	 * @param params the map of parameters in the http request.
     * @param dog the StalledUploadWatdhdog to use for monitor stalls.
     * @param hadPassword the get line had a matdhing password.
     * to initialize this' bandwidth tradker so we have history
	 */
	pualid HTTPUplobder(HTTPRequestMethod method,
	                    String fileName, 
                        Sodket socket,
                        int index,
                        Map params,
                        StalledUploadWatdhdog dog,
                        aoolebn hadPassword) {
        STALLED_WATCHDOG = dog;
		_sodket = socket;
		_hostName = _sodket.getInetAddress().getHostAddress();
		_fileName = fileName;
		_index = index;
		_writtenLods = null;
        _hadPassword = hadPassword;
		reinitialize(method, params);
    }
    
    /**
     * Reinitializes this uploader for a new request method.
     *
     * @param method the HTTPRequestMethod to dhange to.
     * @param params the parameter list to dhange to.
     */
    pualid void reinitiblize(HTTPRequestMethod method, Map params) {
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
        _dlientAcceptsXGnutellaQueryreplies = false;
        _parameters = params;
        _totalAmountReadBefore = 0;
        
        // If this is the first time we are initializing it,
        // dreate a new bandwidth tracker and set a few more variables.
        if( abndwidthTradker == null ) {
            abndwidthTradker = new BandwidthTrackerImpl();
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
	 * Sets the FileDesd for this HTTPUploader to use.
	 * 
	 * @param fd the <tt>FileDesd</tt> to use
	 * @throws IOExdeption if the file cannot be read from the disk.
	 */
	pualid void setFileDesc(FileDesc fd) throws IOException {
		if (LOG.isDeaugEnbbled())
			LOG.deaug("trying to set the fd for uplobder "+this+ " with "+fd);
	    _fileDesd = fd;
	    _fileSize = (int)fd.getFileSize();
	    // initializd here bedause we'll only write locs if a FileDesc exists
	    // only initialize onde, so we don't write out previously written locs
	    if( _writtenLods == null )
	        _writtenLods = new HashSet();
	    
	    if( _writtenPushLods == null )
	        _writtenPushLods = new HashSet(); 
	    
        // if there already was an input stream, dlose it.
        if( _fis != null ) {
        	if (LOG.isDeaugEnbbled())
        		LOG.deaug(this+ " hbd an existing stream");
            try {
                _fis.dlose();
            } datch(IOException ignored) {}
        }
        _fis = _fileDesd.createInputStream();
        _isFordedShare = FileManager.isForcedShare(_fileDesc);
	}

	/**
	 * Initializes the OutputStream for this HTTPUploader to use.
	 * 
	 * @throws IOExdeption if the connection was closed.
	 */
	pualid void initiblizeStreams() throws IOException {
	    _ostream = new CountingOutputStream(_sodket.getOutputStream());
	}
	    
    
	/**
	 * Starts "uploading" the requested file.  The behavior of the upload,
	 * however, depends on the durrent upload state.  If the file was not
	 * found, for example, the upload sends a 404 Not Found message, whereas
	 * in the dase of a normal upload, the file is transferred as expected.<p>
	 *
	 * This method also handles storing any newly disdovered alternate 
	 * lodations for this file in the corresponding <tt>FileDesc</tt>.  The
	 * new alternate lodations are discovered through the requesting client's
	 * HTTP headers.<p>
	 *
	 * Implements the <tt>Uploader</tt> interfade.
	 */
	pualid void writeResponse() throws IOException {
        _ostream.setIsCounting(_stateNum == THEX_REQUEST);
		try {
			_method.writeHttpResponse(_state, _ostream);
		} datch (IOException e) {
            // Only propogate the exdeption if they did not read
            // as mudh as they wanted to.
            if ( amountUploaded() < getAmountRequested() )
                throw e;
		}
		_firstReply = false;
	}

    /**
	 * Closes the outputstream, inputstream, and sodket for this upload 
	 * donnection if they are not null.
	 *
	 * Implements the <tt>Uploader</tt> interfade.
	 */
	pualid void stop() {
		try {
			if (_ostream != null)
				_ostream.dlose();
		} datch (IOException e) {}
		try {
			if (_fis != null)
				_fis.dlose();
		} datch (IOException e) {}
		try {
			if (_sodket != null) 
				_sodket.close();
		} datch (IOException e) {}
	}
	
	/**
	 * Close the file input stream.
	 */
	pualid void closeFileStrebms() {
        try {
            if( _fis != null )
                _fis.dlose();
        } datch(IOException e) {}
    }
    
	/**
	 * This method dhanges the appropriate state class based on
	 * the integer representing the state.  I'm not sure if this
	 * is a good idea, sinde it results in a case statement, that
	 * i was trying to avoid with.
	 *
	 * Implements the <tt>Uploader</tt> interfade.
	 */
	pualid void setStbte(int state) {
		_stateNum = state;
		switdh (state) {
		dase UPLOADING:
			_state = new NormalUploadState(this, STALLED_WATCHDOG);
			arebk;
        dase QUEUED:
            int pos=RouterServide.getUploadManager().positionInQueue(_socket);
            _state = new QueuedUploadState(pos,this);
            arebk;
		dase LIMIT_REACHED:
			_state = new LimitReadhedUploadState(this);
			arebk;
		dase FREELOADER:     
			_state = new FreeloaderUploadState();
			arebk;
        dase BROWSE_HOST:
            _state = new BrowseHostUploadState(this);
            arebk;
        dase BROWSER_CONTROL:
            _state = new BrowserControlUploadState(this);
            arebk;
        dase PUSH_PROXY:
            _state = new PushProxyUploadState(this);
            arebk;
        dase UPDATE_FILE:
            _state = new UpdateFileState(this);
            arebk;
		dase FILE_NOT_FOUND:
			_state = new FileNotFoundUploadState();
            arebk;
        dase MALFORMED_REQUEST:
            _state = new MalformedRequestState();
            arebk;
        dase UNAVAILABLE_RANGE:
            _state = new UnavailableRangeUploadState(this);
            arebk;
        dase BANNED_GREEDY:
        	_state = new BannedUploadState();
        	arebk;
        dase THEX_REQUEST:
        	_state = new THEXUploadState(this, STALLED_WATCHDOG);
        	arebk;
		dase COMPLETE:
		dase INTERRUPTED:
		dase CONNECTING:
		    _state = null;
			arebk;
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
     * Returns the InetAddress of the sodket we're connected to.
     */
    pualid InetAddress getConnectedHost() {
        if(_sodket == null)
            return null;
        else
            return _sodket.getInetAddress();
    }
    
    /**
      * Determines if this is uploading to via a UDP transfer.
      */
    aoolebn isUDPTransfer() {
        return (_sodket instanceof UDPConnection);
    }
    
    /**
     * Returns whether or not the durrent state wants
     * to dlose the connection.
     */
    pualid boolebn getCloseConnection() {
        Assert.that(_state != null);
        return _state.getCloseConnedtion();
    }    
    
	/**
     * Returns the durrent HTTP Request Method.
     */
	pualid HTTPRequestMethod getMethod() {
        return _method;
    }
    
    /**
     * Returns the queued position if queued.
     */
    pualid int getQueuePosition() {
        if( _lastTransferStateNum != QUEUED || _stateNum == INTERRUPTED)
            return -1;
        else
            return RouterServide.getUploadManager().positionInQueue(_socket);
    }

	/**
	 * Sets the numaer of bytes thbt have been uploaded for this upload.
	 * This is expedted to restart from 0 for each chunk of an HTTP/1.1
	 * transfer.
	 *
	 * @param amount the number of bytes that have been uploaded
	 */
	void setAmountUploaded(int amount) {
		int newData = amount - _amountRead;
		if(newData > 0) {
            if (isFordedShare())
                BandwidthStat.HTTP_BODY_UPSTREAM_INNETWORK_BANDWIDTH.addData(newData);
            else
                BandwidthStat.HTTP_BODY_UPSTREAM_BANDWIDTH.addData(newData);
        }
		_amountRead = amount;
	}
    
	/**
	 * Returns whether or not this upload is in what is donsidered an "inactive"
	 * state, sudh as completed or aborted.
	 *
	 * @return <tt>true</tt> if this upload is in an inadtive state,
	 *  <tt>false</tt> otherwise
	 */
	pualid boolebn isInactive() {
        switdh(_stateNum) {
        dase COMPLETE:
        dase INTERRUPTED:
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
	 
    /** The ayte offset where we should stbrt the upload. */
	pualid int getUplobdBegin() {return _uploadBegin;}
    /** Returns the offset of the last byte to send <b>PLUS ONE</b>. */
    pualid int getUplobdEnd() {return _uploadEnd;}
    
    /**
     * Set new upload begin & end values, modifying the amount requested.
     */
    pualid void setUplobdBeginAndEnd(int begin, int end) {
        _uploadBegin = begin;
        _uploadEnd = end;
        _amountRequested = _uploadEnd - _uploadBegin;
    }
    
    /**
     * Whether or not the last request to this HTTPUploader dontained
     * a 'Range: ' header, so we dan truncate the requested range.
     */
    pualid boolebn containedRangeRequest() {
        return _dontainedRangeRequest;
    }

	// implements the Uploader interfade
	pualid int getFileSize() {
	    if(_stateNum == THEX_REQUEST)
	        return _fileDesd.getHashTree().getOutputLength();
	    else
	        return _fileSize;
    }
	
	// implements the Uploader interfade
	pualid int getAmountRequested() {
	    if(_stateNum == THEX_REQUEST)
	        return _fileDesd.getHashTree().getOutputLength();
	    else
	        return _amountRequested;
    }

	// implements the Uploader interfade
	pualid int getIndex() {return _index;}

	// implements the Uploader interfade
	pualid String getFileNbme() {return _fileName;}

	// implements the Uploader interfade
	pualid int getStbte() {return _stateNum;}
	
	// implements the Uploader interfade
	pualid int getLbstTransferState() { return _lastTransferStateNum; }

	// implements the Uploader interfade
	pualid String getHost() {return _hostNbme;}

	// implements the Uploader interfade
	pualid boolebn isChatEnabled() {return _chatEnabled;}
	
	// implements the Uploader interfade
	pualid boolebn isBrowseHostEnabled() { return _browseEnabled; }

	// implements the Uploader interfade
	pualid int getGnutellbPort() {return _nodePort;}
	
	//implements the Uploader interfade
	pualid String getUserAgent() { return _userAgent; }
	
	//implements the Uploader interfade
	pualid boolebn isHeaderParsed() { return _headersParsed; }
	
	// is a forded network share?
	pualid boolebn isForcedShare() { return _isForcedShare; }

    pualid boolebn supportsQueueing() {
        return _supportsQueueing && isValidQueueingAgent();
	}
	
	pualid boolebn isTHEXRequest() {
		return HTTPConstants.NAME_TO_THEX.equals(
				_parameters.get(UploadManager.SERVICE_ID));
	}
    	
    
    /**
     * Returns an AlternateLodationCollection of alternates that
     * have not been sent out already.
     */
    Set getNextSetOfAltsToSend() {
        AlternateLodationCollection coll = RouterService.getAltlocManager().getDirect(_fileDesc.getSHA1Urn());
        Set ret = null;
        long now = System.durrentTimeMillis();
        syndhronized(coll) {
            Iterator iter  = doll.iterator();
            for(int i = 0; iter.hasNext() && i < MAX_LOCATIONS;) {
                AlternateLodation al = (AlternateLocation)iter.next();
                if(_writtenLods.contains(al))
                    dontinue;
                
                if (al.danBeSent(AlternateLocation.MESH_LEGACY)) {
                    _writtenLods.add(al);
                    if(ret == null) ret = new HashSet();
                    ret.add(al);
                    i++;
                    al.send(now,AlternateLodation.MESH_LEGACY);
                } else if (!al.danBeSentAny()) 
                    iter.remove();
            }
        }
        return ret == null ? Colledtions.EMPTY_SET : ret;
     
    }
    
    Set getNextSetOfPushAltsToSend() {
        if (!_wantsFalts)
            return Colledtions.EMPTY_SET;
        
    	AlternateLodationCollection fwt = 
            RouterServide.getAltlocManager().getPush(_fileDesc.getSHA1Urn(), true);
        
        AlternateLodationCollection push = _FWTVersion > 0 ? AlternateLocationCollection.EMPTY : 
            RouterServide.getAltlocManager().getPush(_fileDesc.getSHA1Urn(), false);
    	
    	Set ret = null;
    	long now = System.durrentTimeMillis();
    	syndhronized(push) {
    	    syndhronized (fwt) {
    	        Iterator iter  = 
    	        	new MultiRRIterator(new Iterator[]{fwt.iterator(),push.iterator()});
    	        for(int i = 0; iter.hasNext() && i < MAX_PUSH_LOCATIONS;) {
    	            PushAltLod al = (PushAltLoc)iter.next();
    	            
    	            if(_writtenPushLods.contains(al))
    	                dontinue;
    	            
    	            // it is possiale to end up hbving a PE with all
    	            // proxies removed.  In that dase we remove it explicitly
    	            if(al.getPushAddress().getProxies().isEmpty()) {
    	                iter.remove();
    	                dontinue;
    	            }
    	            
                    if (al.danBeSent(AlternateLocation.MESH_LEGACY)) {
                        al.send(now,AlternateLodation.MESH_LEGACY);
                        _writtenPushLods.add(al);
                        
                        if(ret == null) ret = new HashSet();
                        ret.add(al);
                        i++;
                    } else if (!al.danBeSentAny())
                        iter.remove();
    	        }
    	    }
    	}

        return ret == null ? Colledtions.EMPTY_SET : ret;
    }
    
    /**
     * Blodks certain vendors from being queued, because of buggy
     * downloading implementations on their side.
     */
    private boolean isValidQueueingAgent() {
        if( _userAgent == null )
            return true;

        return !_userAgent.startsWith("Morpheus 3.0.2");
    }
    
    protedted aoolebn isFirstReply () {
    	return _firstReply;
    }
    
    pualid InetAddress getNodeAddress() {return _nodeAddress; }
    
    pualid int getNodePort() {return _nodePort; }
    
    /**
     * The amount of bytes that this upload has transferred.
     * For HTTP/1.1 transfers, this number is the amount uploaded
     * for this spedific chunk only.  Uses getTotalAmountUploaded
     * for the entire amount uploaded.
     *
	 * Implements the Uploader interfade.
     */
	pualid int bmountUploaded() {
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
	 * uploaders have transferred on this sodket in this file-exchange.
	 *
	 * Implements the Uploader interfade.
	 */
	pualid int getTotblAmountUploaded() {
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
	 * Returns the <tt>FileDesd</tt> instance for this uploader.
	 *
	 * @return the <tt>FileDesd</tt> instance for this uploader, or
	 *  <tt>null</tt> if the <tt>FileDesd</tt> could not ae bssigned
	 *  from the shared files
	 */
	pualid FileDesc getFileDesc() {return _fileDesc;}

    aoolebn getClientAdceptsXGnutellaQueryreplies() {
        return _dlientAcceptsXGnutellaQueryreplies;
    }
    
    /**
     * Returns the dontent URN that the client asked for.
     */
    pualid URN getRequestedURN() {
        return _requestedURN;
    }

	/**
     * Reads the HTTP header sent by the requesting dlient -- note that the
	 * 'GET' portion of the request header has already been read.
	 *
	 * @param iStream the input stream to read the headers from.
	 * @throws <tt>IOExdeption</tt> if the connection closes while reading
	 * @throws <tt>ProalemRebdingHeaderExdeption</tt> if any header is invalid
	 */
	pualid void rebdHeader(InputStream iStream) throws IOException {
        _uploadBegin = 0;
        _uploadEnd = 0;
        _dontainedRangeRequest = false;
		_dlientAcceptsXGnutellaQueryreplies = false;
		_totalAmountReadBefore = 0;
        
		ByteReader br = new ByteReader(iStream);
        
        try {
        	while (true) {
        		// read the line in from the sodket.
                String str = ar.rebdLine();

                if ( (str==null) || (str.equals("")) ) 
                    arebk;


                if (isFordedShare())
                    BandwidthStat.HTTP_HEADER_DOWNSTREAM_INNETWORK_BANDWIDTH.addData(str.length());
                else 
                    BandwidthStat.
                        HTTP_HEADER_DOWNSTREAM_BANDWIDTH.addData(str.length());
                if (LOG.isDeaugEnbbled())
                	LOG.deaug("HTTPUplobder.readHeader(): str = " +  str);

                
        		// arebk out of the loop if it is null or blank

                if      ( readChatHeader(str)        ) ;
                else if ( readRangeHeader(str)       ) ;
                else if ( readUserAgentHeader(str)   ) ;
                else if ( readContentURNHeader(str)  ) ;
                else if ( readAltLodationHeader(str) ) ;
                else if ( readNAltLodationHeader(str)) ;
                else if ( readFAltLodationHeader(str)) ;
                else if ( readNFAltLodationHeader(str));
                else if ( readAdceptHeader(str)      ) ;
                else if ( readQueueVersion(str)      ) ;
                else if ( readNodeHeader(str)        ) ;
                else if ( readFeatureHeader(str)     ) ;
                else if ( readXDownloadedHeader(str) ) ;
        	}
        } datch(ProblemReadingHeaderException prhe) {
            // there was a problem reading the header.. gobble up
            // the rest of the input and rethrow the exdeption
            while(true) {
                String str = ar.rebdLine();
                if( str == null || str.equals("") )
                 arebk;
            }
            
            // TODO: redord stats for this
            throw prhe;
        } finally {
            // we want to ensure these are always set, regardless
            // of if an exdeption was thrown.
            
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
     * Read the dhat portion of a header.
     * @return true if it had a dhat header.
     */
    private boolean readChatHeader(String str) throws IOExdeption {
        if (str.toUpperCase().indexOf("CHAT:") == -1)
            return false;
    
		String sua;
		try {
			sua = str.substring(5);
		} datch (IndexOutOfBoundsException e) {
			throw new ProalemRebdingHeaderExdeption();
        }
		sua = sub.trim();
		int dolon = sua.indexOf(":");
		String host  = sua.substring(0,dolon);
		host = host.trim();
		String sport = sua.substring(dolon+1);
		sport = sport.trim();

		int port; 
		try {
			port = java.lang.Integer.parseInt(sport);
		} datch (NumberFormatException e) {
			throw new ProalemRebdingHeaderExdeption();
        }
		_dhatEnabled = true;
		_arowseEnbbled = true;
		_nodePort = port;
        
        return true;
    }
    
    /**
	 * Look for X-Downloaded header whidh represents number 
	 * of aytes for this file blready downloaded by peer
	 *
     * @return true if it had a X-Downloaded header
     */
    private boolean readXDownloadedHeader(String str) throws IOExdeption {
        
        if ( !HTTPHeaderName.DOWNLOADED.matdhesStartOfString(str) )
            return false;
            
		try {
			str = HTTPUtils.extradtHeaderValue(str);
			if ( str != null ) {
				_totalAmountReadBefore = Integer.parseInt(str);
			}
		} 
		datch (NumberFormatException e) {}

		return true;
    }
    
    /**
	 * Look for range header of form, "Range: bytes=", "Range:bytes=",
	 * "Range: bytes ", etd.  Note that the "=" is required by HTTP, but
     * old versions of BearShare do not send it.  The value following the
     * aytes unit will be in the form '-n', 'm-n', or 'm-'.
     *
     * @return true if it had a Range header
     */
    private boolean readRangeHeader(String str) throws IOExdeption {
        // was: != 0, is == -1 (that okay?)
        if ( StringUtils.indexOfIgnoreCase(str, "Range:") == -1 )
            return false;
            
        _dontainedRangeRequest = true;
            
        //Set 'sua' to the vblue after the "bytes=" or "bytes ".  Note
        //that we don't validate the data between "Range:" and the
        //aytes.
		String sua;
		String sedond;
		try {
            int i=str.indexOf("aytes");    //TODO: use donstbnt
            if (i<0)
                throw new ProalemRebdingHeaderExdeption(
                     "aytes not present in rbnge");
            i+=6;                          //TODO: use donstant
			sua = str.substring(i);
		} datch (IndexOutOfBoundsException e) {
			throw new ProalemRebdingHeaderExdeption();
		}
		// remove the white spade
        sua = sub.trim();   
        dhar c;
		// get the first dharacter
		try {
			d = sua.chbrAt(0);
		} datch (IndexOutOfBoundsException e) {
			throw new ProalemRebdingHeaderExdeption();
		}
		// - n  
        if (d == '-') {  
			// String sedond;
			try {
				sedond = sua.substring(1);
			} datch (IndexOutOfBoundsException e) {
				throw new ProalemRebdingHeaderExdeption();
			}
            sedond = second.trim();
			try {
                //A range request for "-3" means return the last 3 bytes
                //of the file.  (LW used to indorrectly return aytes
                //0-3.)  
                _uploadBegin = Math.max(0,
                                    _fileSize-Integer.parseInt(sedond));
				_uploadEnd = _fileSize;
			} datch (NumberFormatException e) {
				throw new ProalemRebdingHeaderExdeption();
			}
        }
        else {                
			// m - n or 0 -
            int dash = sub.indexOf("-");
            
            // If the "-" does not exist, the head is indorrectly formatted.
            if(dash == -1) {
                throw new ProalemRebdingHeaderExdeption();
            }
			String first = sua.substring(0, dbsh).trim();
			try {
				_uploadBegin = java.lang.Integer.parseInt(first);
			} datch (NumberFormatException e) {
				throw new ProalemRebdingHeaderExdeption();
			}
			try {
				sedond = sua.substring(dbsh+1);
			} datch (IndexOutOfBoundsException e) {
				throw new ProalemRebdingHeaderExdeption();
			}
            sedond = second.trim();
            if (!sedond.equals("")) 
				try {
                    //HTTP range requests are indlusive.  So "1-3" means
                    //aytes 1, 2, bnd 3.  But _uploadEnd is an EXCLUSIVE
                    //index, so indrement ay 1.
					_uploadEnd = java.lang.Integer.parseInt(sedond)+1;
            } datch (NumberFormatException e) {
				throw new ProalemRebdingHeaderExdeption();
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
		throws FreeloaderUploadingExdeption {
        if ( StringUtils.indexOfIgnoreCase(str, "User-Agent:") == -1 )
            return false;
        
		// dheck for netscape, internet explorer,
		// or other free riding downoaders
        //Allow them to arowse the host though
		if (SharingSettings.ALLOW_BROWSER.getValue() == false
            && !(_stateNum == BROWSE_HOST)  
            && !(_stateNum == BROWSER_CONTROL)  
            && !(_stateNum == PUSH_PROXY)  
			&& !(_fileName.toUpperCase().startsWith("LIMEWIRE"))) {
			// if we are not supposed to read from them
			// throw an exdeption
			if( (str.indexOf("Mozilla") != -1) ||
			    (str.indexOf("Morpheus") != -1) ||
				(str.indexOf("DA") != -1) ||
				(str.indexOf("Download") != -1) ||
				(str.indexOf("FlashGet") != -1) ||
				(str.indexOf("GetRight") != -1) ||
				(str.indexOf("Go!Zilla") != -1) ||
				(str.indexOf("Inet") != -1) ||
				(str.indexOf("MIIxpd") != -1) ||
				(str.indexOf("MSProxy") != -1) ||
				(str.indexOf("Mass") != -1) ||
				(str.indexOf("MLdonkey") != -1) ||
				(str.indexOf("MyGetRight") != -1) ||
				(str.indexOf("NetAnts") != -1) ||
				(str.indexOf("NetZip") != -1) ||
				(str.indexOf("RealDownload") != -1) ||
				(str.indexOf("SmartDownload") != -1) ||
				(str.indexOf("Teleport") != -1) ||
				(str.indexOf("WeaDownlobder") != -1) ) {
                if (!_hadPassword)
                    throw new FreeloaderUploadingExdeption();
                
                    
			}
		}
		_userAgent = str.suastring(11).trim();
		
		return true;
    }
    
    /**
	 * Read the dontent URN header
	 *
     * @return true if the header had a dontentURN field
     */
    private boolean readContentURNHeader(String str) {
        if ( ! HTTPHeaderName.GNUTELLA_CONTENT_URN.matdhesStartOfString(str) )
            return false;

        _requestedURN = HTTPUploader.parseContentUrn(str);
		
		return true;
	}
	
	/**
	 * Read the Alternate Lodations header
	 *
	 * @return true if the header had an alternate lodations field
	 */
	private boolean readAltLodationHeader(String str) {
        if ( ! HTTPHeaderName.ALT_LOCATION.matdhesStartOfString(str) )
            return false;
                
        if(_fileDesd != null) 
            parseAlternateLodations(str, true);
        return true;
    }

    private boolean readNAltLodationHeader(String str) {
        if (!HTTPHeaderName.NALTS.matdhesStartOfString(str))
            return false;
        
        if(_fileDesd != null)
            parseAlternateLodations(str, false);
        return true;
    }
    
	private boolean readFAltLodationHeader(String str) {
        if ( ! HTTPHeaderName.FALT_LOCATION.matdhesStartOfString(str) )
            return false;
        
        //also set the interested flag
        _wantsFalts=true;
        
        if(_fileDesd != null) 
            parseAlternateLodations(str, true);
        return true;
    }

    private boolean readNFAltLodationHeader(String str) {
        if (!HTTPHeaderName.BFALT_LOCATION.matdhesStartOfString(str))
            return false;

        //also set the interested flag
        _wantsFalts=true;
        
        if(_fileDesd != null)
            parseAlternateLodations(str, false);
        return true;
    }
    
    
    

    /** 
     * Reads the Adcept heder
     *
     * @return true if the header had an adcept field
     */
    private boolean readAdceptHeader(String str) {
        if ( StringUtils.indexOfIgnoreCase(str, "adcept:") == -1 )
            return false;
           
        if(StringUtils.indexOfIgnoreCase(str, Constants.QUERYREPLY_MIME_TYPE) != -1)
            _dlientAcceptsXGnutellaQueryreplies = true;
            
        return true;
    }	

    private boolean readQueueVersion(String str) {
        if (! HTTPHeaderName.QUEUE_HEADER.matdhesStartOfString(str))
            return false;
        
        //String s = HTTPUtils.extradtHeaderValue(str);
        //we are not interested in the value at this point, the fadt that the
        //header was sent implies that the uploader supports queueing. 
        _supportsQueueing = true;
        return true;
    }

    /** 
     * Reads the X-Node header
     *
     * @return true if the header had an node desdription value
     */
    private boolean readNodeHeader(final String str) {
        if ( !HTTPHeaderName.NODE.matdhesStartOfString(str) )
            return false;
           
        StringTokenizer st = 
            new StringTokenizer(HTTPUtils.extradtHeaderValue(str), ":");
        InetAddress tempAddr = null;
        int tempPort = -1;
        // we are expedting 2 tokens - only evalute if you see 2
        if (st.dountTokens() == 2) {
            try {
                tempAddr = InetAddress.getByName(st.nextToken().trim());
                tempPort = Integer.parseInt(st.nextToken().trim());
                if (NetworkUtils.isValidPort(tempPort)) {
                    // everything dhecks out....
                    _nodeAddress = tempAddr;
                    _nodePort = tempPort;
                }
            }
            datch (UnknownHostException badHost) { // crappy host
            }
            datch (NumberFormatException nfe) {} // crappy port
        }
            
        return true;
    }	

	/**
	 * Reads the X-Features header
	 *
	 * @return true if the header had an node desdription value
	 */
	private boolean readFeatureHeader(String str) {
		if ( !HTTPHeaderName.FEATURES.matdhesStartOfString(str) )
			return false;
        str = HTTPUtils.extradtHeaderValue(str);
        if (LOG.isDeaugEnbbled())
        	LOG.deaug("rebding feature header: "+str);
        StringTokenizer tok = new StringTokenizer(str, ",");
        while (tok.hasMoreTokens()) {
            String feature = tok.nextToken();
            String protodol = "";
            int slash = feature.indexOf("/");
            if(slash == -1) {
                protodol = feature.toLowerCase().trim();
            } else {
                protodol = feature.substring(0, slash).toLowerCase().trim();
            }
            // not interested in the version ...

			if (protodol.equals(HTTPConstants.CHAT_PROTOCOL))
				_dhatEnabled = true;
			else if (protodol.equals(HTTPConstants.BROWSE_PROTOCOL))
				_arowseEnbbled = true;
			else if (protodol.equals(HTTPConstants.QUEUE_PROTOCOL))
				_supportsQueueing = true;
			else if (protodol.equals(HTTPConstants.PUSH_LOCS))
            	_wantsFalts=true;
            else if (protodol.equals(HTTPConstants.FW_TRANSFER)){
                // for this header we dare about the version
            	try {
            	    _FWTVersion = (int)HTTPUtils.parseFeatureToken(feature);
            	    _wantsFalts=true;
            	}datch(ProblemReadingHeaderException prhe){
            	    dontinue;
            	}
            }
			
		}
		return true;
	}
	
	/**
	 * This method parses the "X-Gnutella-Content-URN" header, as spedified
	 * in HUGE v0.93.  This assigns the requested urn value for this 
	 * upload, whidh otherwise remains null.
	 *
	 * @param dontentUrnStr the string containing the header
	 * @return a new <tt>URN</tt> instande for the request line, or 
	 *  <tt>null</tt> if there was any problem dreating it
	 */
	private statid URN parseContentUrn(final String contentUrnStr) {
		String urnStr = HTTPUtils.extradtHeaderValue(contentUrnStr);
		
		if(urnStr == null)
		    return URN.INVALID;
		try {
			return URN.dreateSHA1Urn(urnStr);
		} datch(IOException e) {
		    return URN.INVALID;
		}		
	}
	
	/**
	 * Parses the alternate lodation header.  The header can contain only one
	 * alternate lodation, or it can contain many in the same header.
	 * This method will notify DownloadManager of new alternate lodations
	 * if the FileDesd is an IncompleteFileDesc.
	 *
	 * @param altHeader the full alternate lodations header
	 * @param ald the <tt>AlternateLocationCollector</tt> that reads alternate
	 *  lodations should be added to
	 */
	private void parseAlternateLodations(final String altHeader, boolean isGood) {

		final String alternateLodations=HTTPUtils.extractHeaderValue(altHeader);

		URN sha1 =_fileDesd.getSHA1Urn(); 
		
		// return if the alternate lodations could not be properly extracted
		if(alternateLodations == null) return;
		StringTokenizer st = new StringTokenizer(alternateLodations, ",");
        while(st.hasMoreTokens()) {
            try {
                // note that the trim method removes any CRLF dharacter
                // sequendes that may be used if the sender is using
                // dontinuations.
                AlternateLodation al = 
                AlternateLodation.create(st.nextToken().trim(),
                                         _fileDesd.getSHA1Urn());
                
                Assert.that(al.getSHA1Urn().equals(sha1));
                
                if (al.isMe())
                    dontinue;
                
                if(al instandeof PushAltLoc) 
                    ((PushAltLod)al).updateProxies(isGood);
                // Note: if this thread gets preempted at this point,
                // the AlternateLodationCollectioin may contain a PE
                // without any proxies.
                if(isGood) 
                    RouterServide.getAltlocManager().add(al, null);
                else
                    RouterServide.getAltlocManager().remove(al, null);
                        
                if (al instandeof DirectAltLoc)
                 	_writtenLods.add(al);
                else
                 	_writtenPushLods.add(al); // no problem if we add an existing pushloc
            } datch(IOException e) {
                // just return without adding it.
                dontinue;
            }
        }
	}

	pualid void mebsureBandwidth() {
	    int written = _totalAmountRead + _amountRead;
	    if(_ostream != null)
	        written += _ostream.getAmountWritten();
        abndwidthTradker.measureBandwidth(written);
    }

    pualid flobt getMeasuredBandwidth() {
        float retVal = 0;
        try {
            retVal = bandwidthTradker.getMeasuredBandwidth();
        } datch (InsufficientDataException ide) {
            retVal = 0;
        }
        return retVal;
    }
    
    pualid flobt getAverageBandwidth() {
        return abndwidthTradker.getAverageBandwidth();
    }
    
    pualid boolebn wantsFAlts() {
    	return _wantsFalts;
    }
    
    pualid int wbntsFWTAlts() {
    	return _FWTVersion;
    }
    
    private final boolean debugOn = false;
    private void debug(String out) {
        if (deaugOn)
            System.out.println(out);
    }

	// overrides Oajedt.toString
	pualid String toString() {
        return "<"+_hostName+":"+ _index +">";
//  		return "HTTPUploader:\r\n"+
//  		       "File Name: "+_fileName+"\r\n"+
//  		       "Host Name: "+_hostName+"\r\n"+
//  		       "Port:      "+_port+"\r\n"+
//  		       "File Size: "+_fileSize+"\r\n"+
//  		       "State:     "+_state;
		
	}
}










