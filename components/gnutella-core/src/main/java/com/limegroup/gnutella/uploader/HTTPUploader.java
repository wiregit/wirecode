pbckage com.limegroup.gnutella.uploader;

import jbva.io.IOException;
import jbva.io.InputStream;
import jbva.io.OutputStream;
import jbva.io.File;
import jbva.net.InetAddress;
import jbva.net.Socket;
import jbva.net.UnknownHostException;
import jbva.util.Collections;
import jbva.util.HashSet;
import jbva.util.Iterator;
import jbva.util.Map;
import jbva.util.Set;
import jbva.util.StringTokenizer;

import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.limegroup.gnutellb.Assert;
import com.limegroup.gnutellb.BandwidthTrackerImpl;
import com.limegroup.gnutellb.ByteReader;
import com.limegroup.gnutellb.Constants;
import com.limegroup.gnutellb.FileDesc;
import com.limegroup.gnutellb.FileManager;
import com.limegroup.gnutellb.InsufficientDataException;
import com.limegroup.gnutellb.RouterService;
import com.limegroup.gnutellb.URN;
import com.limegroup.gnutellb.UploadManager;
import com.limegroup.gnutellb.Uploader;
import com.limegroup.gnutellb.altlocs.AlternateLocation;
import com.limegroup.gnutellb.altlocs.AlternateLocationCollection;
import com.limegroup.gnutellb.altlocs.DirectAltLoc;
import com.limegroup.gnutellb.altlocs.PushAltLoc;
import com.limegroup.gnutellb.http.HTTPConstants;
import com.limegroup.gnutellb.http.HTTPHeaderName;
import com.limegroup.gnutellb.http.HTTPMessage;
import com.limegroup.gnutellb.http.HTTPRequestMethod;
import com.limegroup.gnutellb.http.HTTPUtils;
import com.limegroup.gnutellb.http.ProblemReadingHeaderException;
import com.limegroup.gnutellb.settings.SharingSettings;
import com.limegroup.gnutellb.statistics.BandwidthStat;
import com.limegroup.gnutellb.udpconnect.UDPConnection;
import com.limegroup.gnutellb.util.CountingOutputStream;
import com.limegroup.gnutellb.util.MultiRRIterator;
import com.limegroup.gnutellb.util.NetworkUtils;
import com.limegroup.gnutellb.util.StringUtils;

/**
 * Mbintains state for an HTTP upload request.  This class follows the
 * Stbte Pattern, delegating its writeResponse method to the appropriate
 * stbte.  All states except for CONNECTING, COMPLETE, and INTERRUPTED
 * hbve an associated state class that implements HTTPMessage.
 *
 * Cbre must be taken to call closeFileStreams whenever a chunk of the
 * trbnsfer is finished, and to call stop when the entire HTTP/1.1
 * session is finished.
 *
 * A single HTTPUplobder should be reused for multiple chunks of a single
 * file in bn HTTP/1.1 session.  However, multiple HTTPUploaders
 * should be used for multiple files in b single HTTP/1.1 session.
 */
public finbl class HTTPUploader implements Uploader {
    
	privbte static final Log LOG = LogFactory.getLog(HTTPUploader.class);
    /**
     * The outputstrebm -- a CountingOutputStream so that we can
     * keep trbck of the amount of bytes written.
     * Currently trbck is only kept for writing a THEX tree, so that
     * progress of the tree bnd bandwidth measurement may be done.
     */
	privbte CountingOutputStream _ostream;
	privbte InputStream _fis;
	privbte Socket _socket;
	privbte int _totalAmountReadBefore;
	privbte int _totalAmountRead;
	privbte int _amountRead;
	// useful so we don't hbve to do _uploadEnd - _uploadBegin everywhere
    privbte int _amountRequested;
	privbte int _uploadBegin;
	privbte int _uploadEnd;
	privbte int _fileSize;
	privbte final int _index;
	privbte String _userAgent;
	privbte boolean _headersParsed;
	privbte final String _fileName;
	privbte final String _hostName;
	privbte int _stateNum = CONNECTING;
	privbte int _lastTransferStateNum;
	privbte HTTPMessage _state;
	privbte boolean _firstReply = true;
	privbte boolean _containedRangeRequest = false;
	
	privbte boolean _chatEnabled;
	privbte boolean _browseEnabled;
    privbte boolean _supportsQueueing = false;
    privbte final boolean _hadPassword;
    
    /**
     * True if this is b forcibly shared network file.
     */
    privbte boolean _isForcedShare = false;
    
    /**
     * whether the remote side indicbted they want to receive
     * firewblled altlocs.
     */
    privbte boolean _wantsFalts = false;
    
    /**
     * the version of the FWT protocol the remote supports.  
     * Non-firewblled hosts should not send this feature.
     * INVARIANT: if this is grebter than 0, _wantsFalts is set.
     */
    privbte int _FWTVersion = 0;

    /**
     * The Wbtchdog that will kill this uploader if it takes too long.
     */
    privbte final StalledUploadWatchdog STALLED_WATCHDOG;

	/**
	 * The URN specified in the X-Gnutellb-Content-URN header, if any.
	 */
	privbte URN _requestedURN;

	/**
	 * The descriptor for the file we're uplobding.
	 */
	privbte FileDesc _fileDesc;
    
    /**
     * Indicbtes that the client to which we are uploading is capable of
     * bccepting Queryreplies in the response.
     */
    privbte boolean _clientAcceptsXGnutellaQueryreplies = false;

    /**
     * The bddress as described by the "X-Node" header.
     */
    privbte InetAddress _nodeAddress = null;

    /**
     * The port bs described by the "X-Node" header.
     */
    privbte int _nodePort = -1;
    
    /**
     * The pbrameters passed to the HTTP Request.
     */
    privbte Map _parameters = null;

    privbte BandwidthTrackerImpl bandwidthTracker=null;
    
    /**
     * The blternate locations that have been written out (as good) locations.
     */
    privbte Set _writtenLocs;
    
    /**
     * The firewblled alternate locations that have been written out as good locations.
     */
    privbte Set _writtenPushLocs;
    
    /**
     * The mbximum number of alts to write per http transfer.
     */
    privbte static final int MAX_LOCATIONS = 10;
    
    /**
     * The mbximum number of firewalled alts to write per http transfer.
     */
    privbte static final int MAX_PUSH_LOCATIONS = 5;

	/**
	 * The <tt>HTTPRequestMethod</tt> to use for the uplobd.
	 */
	privbte HTTPRequestMethod _method;

	/**
	 * Consructor for b "normal" non-push upload.  Note that this can
	 * be b URN get request.
	 *
	 * @pbram method the <tt>HTTPRequestMethod</tt> for the request
	 * @pbram fileName the name of the file
	 * @pbram socket the <tt>Socket</tt> instance to serve the upload over
	 * @pbram index the index of the file in the set of shared files
	 * @pbram params the map of parameters in the http request.
     * @pbram dog the StalledUploadWatchdog to use for monitor stalls.
     * @pbram hadPassword the get line had a matching password.
     * to initiblize this' bandwidth tracker so we have history
	 */
	public HTTPUplobder(HTTPRequestMethod method,
	                    String fileNbme, 
                        Socket socket,
                        int index,
                        Mbp params,
                        StblledUploadWatchdog dog,
                        boolebn hadPassword) {
        STALLED_WATCHDOG = dog;
		_socket = socket;
		_hostNbme = _socket.getInetAddress().getHostAddress();
		_fileNbme = fileName;
		_index = index;
		_writtenLocs = null;
        _hbdPassword = hadPassword;
		reinitiblize(method, params);
    }
    
    /**
     * Reinitiblizes this uploader for a new request method.
     *
     * @pbram method the HTTPRequestMethod to change to.
     * @pbram params the parameter list to change to.
     */
    public void reinitiblize(HTTPRequestMethod method, Map params) {
        _method = method;
        _bmountRequested = 0;
        _uplobdBegin = 0;
        _uplobdEnd = 0;
        _hebdersParsed = false;
        _stbteNum = CONNECTING;
        _stbte = null;
        _nodePort = 0;
        _supportsQueueing = fblse;
        _requestedURN = null;
        _clientAcceptsXGnutellbQueryreplies = false;
        _pbrameters = params;
        _totblAmountReadBefore = 0;
        
        // If this is the first time we bre initializing it,
        // crebte a new bandwidth tracker and set a few more variables.
        if( bbndwidthTracker == null ) {
            bbndwidthTracker = new BandwidthTrackerImpl();
            _totblAmountRead = 0;
            _bmountRead = 0;
        }            
        // Otherwise, updbte the amount read.
        else {
            _totblAmountRead += _amountRead;
            _bmountRead = 0;
        }
	}
	
	/**
	 * Sets the FileDesc for this HTTPUplobder to use.
	 * 
	 * @pbram fd the <tt>FileDesc</tt> to use
	 * @throws IOException if the file cbnnot be read from the disk.
	 */
	public void setFileDesc(FileDesc fd) throws IOException {
		if (LOG.isDebugEnbbled())
			LOG.debug("trying to set the fd for uplobder "+this+ " with "+fd);
	    _fileDesc = fd;
	    _fileSize = (int)fd.getFileSize();
	    // initiblizd here because we'll only write locs if a FileDesc exists
	    // only initiblize once, so we don't write out previously written locs
	    if( _writtenLocs == null )
	        _writtenLocs = new HbshSet();
	    
	    if( _writtenPushLocs == null )
	        _writtenPushLocs = new HbshSet(); 
	    
        // if there blready was an input stream, close it.
        if( _fis != null ) {
        	if (LOG.isDebugEnbbled())
        		LOG.debug(this+ " hbd an existing stream");
            try {
                _fis.close();
            } cbtch(IOException ignored) {}
        }
        _fis = _fileDesc.crebteInputStream();
        _isForcedShbre = FileManager.isForcedShare(_fileDesc);
	}

	/**
	 * Initiblizes the OutputStream for this HTTPUploader to use.
	 * 
	 * @throws IOException if the connection wbs closed.
	 */
	public void initiblizeStreams() throws IOException {
	    _ostrebm = new CountingOutputStream(_socket.getOutputStream());
	}
	    
    
	/**
	 * Stbrts "uploading" the requested file.  The behavior of the upload,
	 * however, depends on the current uplobd state.  If the file was not
	 * found, for exbmple, the upload sends a 404 Not Found message, whereas
	 * in the cbse of a normal upload, the file is transferred as expected.<p>
	 *
	 * This method blso handles storing any newly discovered alternate 
	 * locbtions for this file in the corresponding <tt>FileDesc</tt>.  The
	 * new blternate locations are discovered through the requesting client's
	 * HTTP hebders.<p>
	 *
	 * Implements the <tt>Uplobder</tt> interface.
	 */
	public void writeResponse() throws IOException {
        _ostrebm.setIsCounting(_stateNum == THEX_REQUEST);
		try {
			_method.writeHttpResponse(_stbte, _ostream);
		} cbtch (IOException e) {
            // Only propogbte the exception if they did not read
            // bs much as they wanted to.
            if ( bmountUploaded() < getAmountRequested() )
                throw e;
		}
		_firstReply = fblse;
	}

    /**
	 * Closes the outputstrebm, inputstream, and socket for this upload 
	 * connection if they bre not null.
	 *
	 * Implements the <tt>Uplobder</tt> interface.
	 */
	public void stop() {
		try {
			if (_ostrebm != null)
				_ostrebm.close();
		} cbtch (IOException e) {}
		try {
			if (_fis != null)
				_fis.close();
		} cbtch (IOException e) {}
		try {
			if (_socket != null) 
				_socket.close();
		} cbtch (IOException e) {}
	}
	
	/**
	 * Close the file input strebm.
	 */
	public void closeFileStrebms() {
        try {
            if( _fis != null )
                _fis.close();
        } cbtch(IOException e) {}
    }
    
	/**
	 * This method chbnges the appropriate state class based on
	 * the integer representing the stbte.  I'm not sure if this
	 * is b good idea, since it results in a case statement, that
	 * i wbs trying to avoid with.
	 *
	 * Implements the <tt>Uplobder</tt> interface.
	 */
	public void setStbte(int state) {
		_stbteNum = state;
		switch (stbte) {
		cbse UPLOADING:
			_stbte = new NormalUploadState(this, STALLED_WATCHDOG);
			brebk;
        cbse QUEUED:
            int pos=RouterService.getUplobdManager().positionInQueue(_socket);
            _stbte = new QueuedUploadState(pos,this);
            brebk;
		cbse LIMIT_REACHED:
			_stbte = new LimitReachedUploadState(this);
			brebk;
		cbse FREELOADER:     
			_stbte = new FreeloaderUploadState();
			brebk;
        cbse BROWSE_HOST:
            _stbte = new BrowseHostUploadState(this);
            brebk;
        cbse BROWSER_CONTROL:
            _stbte = new BrowserControlUploadState(this);
            brebk;
        cbse PUSH_PROXY:
            _stbte = new PushProxyUploadState(this);
            brebk;
        cbse UPDATE_FILE:
            _stbte = new UpdateFileState(this);
            brebk;
		cbse FILE_NOT_FOUND:
			_stbte = new FileNotFoundUploadState();
            brebk;
        cbse MALFORMED_REQUEST:
            _stbte = new MalformedRequestState();
            brebk;
        cbse UNAVAILABLE_RANGE:
            _stbte = new UnavailableRangeUploadState(this);
            brebk;
        cbse BANNED_GREEDY:
        	_stbte = new BannedUploadState();
        	brebk;
        cbse THEX_REQUEST:
        	_stbte = new THEXUploadState(this, STALLED_WATCHDOG);
        	brebk;
		cbse COMPLETE:
		cbse INTERRUPTED:
		cbse CONNECTING:
		    _stbte = null;
			brebk;
        defbult:
            Assert.thbt(false, "Invalid state: " + state);
		}
		
		if(_stbte != null)
		    _lbstTransferStateNum = state;
	}
	
	/**
	 * Returns the output strebm this uploader is writing to.
	 */
	OutputStrebm getOutputStream() {
        return _ostrebm;
    }
    
    /**
     * Returns the FileInputStrebm this uploader is reading from.
     */
	InputStrebm getInputStream() {
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
      * Determines if this is uplobding to via a UDP transfer.
      */
    boolebn isUDPTransfer() {
        return (_socket instbnceof UDPConnection);
    }
    
    /**
     * Returns whether or not the current stbte wants
     * to close the connection.
     */
    public boolebn getCloseConnection() {
        Assert.thbt(_state != null);
        return _stbte.getCloseConnection();
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
        if( _lbstTransferStateNum != QUEUED || _stateNum == INTERRUPTED)
            return -1;
        else
            return RouterService.getUplobdManager().positionInQueue(_socket);
    }

	/**
	 * Sets the number of bytes thbt have been uploaded for this upload.
	 * This is expected to restbrt from 0 for each chunk of an HTTP/1.1
	 * trbnsfer.
	 *
	 * @pbram amount the number of bytes that have been uploaded
	 */
	void setAmountUplobded(int amount) {
		int newDbta = amount - _amountRead;
		if(newDbta > 0) {
            if (isForcedShbre())
                BbndwidthStat.HTTP_BODY_UPSTREAM_INNETWORK_BANDWIDTH.addData(newData);
            else
                BbndwidthStat.HTTP_BODY_UPSTREAM_BANDWIDTH.addData(newData);
        }
		_bmountRead = amount;
	}
    
	/**
	 * Returns whether or not this uplobd is in what is considered an "inactive"
	 * stbte, such as completed or aborted.
	 *
	 * @return <tt>true</tt> if this uplobd is in an inactive state,
	 *  <tt>fblse</tt> otherwise
	 */
	public boolebn isInactive() {
        switch(_stbteNum) {
        cbse COMPLETE:
        cbse INTERRUPTED:
            return true;
        defbult:
            return fblse;
        }
	}
	
	/**
	 * Returns the pbrameter list of this HTTPUploader.
	 */
	Mbp getParameters() {
	    return _pbrameters;
	}
	 
    /** The byte offset where we should stbrt the upload. */
	public int getUplobdBegin() {return _uploadBegin;}
    /** Returns the offset of the lbst byte to send <b>PLUS ONE</b>. */
    public int getUplobdEnd() {return _uploadEnd;}
    
    /**
     * Set new uplobd begin & end values, modifying the amount requested.
     */
    public void setUplobdBeginAndEnd(int begin, int end) {
        _uplobdBegin = begin;
        _uplobdEnd = end;
        _bmountRequested = _uploadEnd - _uploadBegin;
    }
    
    /**
     * Whether or not the lbst request to this HTTPUploader contained
     * b 'Range: ' header, so we can truncate the requested range.
     */
    public boolebn containedRangeRequest() {
        return _contbinedRangeRequest;
    }

	// implements the Uplobder interface
	public int getFileSize() {
	    if(_stbteNum == THEX_REQUEST)
	        return _fileDesc.getHbshTree().getOutputLength();
	    else
	        return _fileSize;
    }
	
	// implements the Uplobder interface
	public int getAmountRequested() {
	    if(_stbteNum == THEX_REQUEST)
	        return _fileDesc.getHbshTree().getOutputLength();
	    else
	        return _bmountRequested;
    }

	// implements the Uplobder interface
	public int getIndex() {return _index;}

	// implements the Uplobder interface
	public String getFileNbme() {return _fileName;}

	// implements the Uplobder interface
	public int getStbte() {return _stateNum;}
	
	// implements the Uplobder interface
	public int getLbstTransferState() { return _lastTransferStateNum; }

	// implements the Uplobder interface
	public String getHost() {return _hostNbme;}

	// implements the Uplobder interface
	public boolebn isChatEnabled() {return _chatEnabled;}
	
	// implements the Uplobder interface
	public boolebn isBrowseHostEnabled() { return _browseEnabled; }

	// implements the Uplobder interface
	public int getGnutellbPort() {return _nodePort;}
	
	//implements the Uplobder interface
	public String getUserAgent() { return _userAgent; }
	
	//implements the Uplobder interface
	public boolebn isHeaderParsed() { return _headersParsed; }
	
	// is b forced network share?
	public boolebn isForcedShare() { return _isForcedShare; }

    public boolebn supportsQueueing() {
        return _supportsQueueing && isVblidQueueingAgent();
	}
	
	public boolebn isTHEXRequest() {
		return HTTPConstbnts.NAME_TO_THEX.equals(
				_pbrameters.get(UploadManager.SERVICE_ID));
	}
    	
    
    /**
     * Returns bn AlternateLocationCollection of alternates that
     * hbve not been sent out already.
     */
    Set getNextSetOfAltsToSend() {
        AlternbteLocationCollection coll = RouterService.getAltlocManager().getDirect(_fileDesc.getSHA1Urn());
        Set ret = null;
        long now = System.currentTimeMillis();
        synchronized(coll) {
            Iterbtor iter  = coll.iterator();
            for(int i = 0; iter.hbsNext() && i < MAX_LOCATIONS;) {
                AlternbteLocation al = (AlternateLocation)iter.next();
                if(_writtenLocs.contbins(al))
                    continue;
                
                if (bl.canBeSent(AlternateLocation.MESH_LEGACY)) {
                    _writtenLocs.bdd(al);
                    if(ret == null) ret = new HbshSet();
                    ret.bdd(al);
                    i++;
                    bl.send(now,AlternateLocation.MESH_LEGACY);
                } else if (!bl.canBeSentAny()) 
                    iter.remove();
            }
        }
        return ret == null ? Collections.EMPTY_SET : ret;
     
    }
    
    Set getNextSetOfPushAltsToSend() {
        if (!_wbntsFalts)
            return Collections.EMPTY_SET;
        
    	AlternbteLocationCollection fwt = 
            RouterService.getAltlocMbnager().getPush(_fileDesc.getSHA1Urn(), true);
        
        AlternbteLocationCollection push = _FWTVersion > 0 ? AlternateLocationCollection.EMPTY : 
            RouterService.getAltlocMbnager().getPush(_fileDesc.getSHA1Urn(), false);
    	
    	Set ret = null;
    	long now = System.currentTimeMillis();
    	synchronized(push) {
    	    synchronized (fwt) {
    	        Iterbtor iter  = 
    	        	new MultiRRIterbtor(new Iterator[]{fwt.iterator(),push.iterator()});
    	        for(int i = 0; iter.hbsNext() && i < MAX_PUSH_LOCATIONS;) {
    	            PushAltLoc bl = (PushAltLoc)iter.next();
    	            
    	            if(_writtenPushLocs.contbins(al))
    	                continue;
    	            
    	            // it is possible to end up hbving a PE with all
    	            // proxies removed.  In thbt case we remove it explicitly
    	            if(bl.getPushAddress().getProxies().isEmpty()) {
    	                iter.remove();
    	                continue;
    	            }
    	            
                    if (bl.canBeSent(AlternateLocation.MESH_LEGACY)) {
                        bl.send(now,AlternateLocation.MESH_LEGACY);
                        _writtenPushLocs.bdd(al);
                        
                        if(ret == null) ret = new HbshSet();
                        ret.bdd(al);
                        i++;
                    } else if (!bl.canBeSentAny())
                        iter.remove();
    	        }
    	    }
    	}

        return ret == null ? Collections.EMPTY_SET : ret;
    }
    
    /**
     * Blocks certbin vendors from being queued, because of buggy
     * downlobding implementations on their side.
     */
    privbte boolean isValidQueueingAgent() {
        if( _userAgent == null )
            return true;

        return !_userAgent.stbrtsWith("Morpheus 3.0.2");
    }
    
    protected boolebn isFirstReply () {
    	return _firstReply;
    }
    
    public InetAddress getNodeAddress() {return _nodeAddress; }
    
    public int getNodePort() {return _nodePort; }
    
    /**
     * The bmount of bytes that this upload has transferred.
     * For HTTP/1.1 trbnsfers, this number is the amount uploaded
     * for this specific chunk only.  Uses getTotblAmountUploaded
     * for the entire bmount uploaded.
     *
	 * Implements the Uplobder interface.
     */
	public int bmountUploaded() {
	    if(_stbteNum == THEX_REQUEST) {
	        if(_ostrebm == null)
	            return 0;
	        else
	            return _ostrebm.getAmountWritten();
	    } else
	        return _bmountRead;
    }
	
	/**
	 * The totbl amount of bytes that this upload and all previous
	 * uplobders have transferred on this socket in this file-exchange.
	 *
	 * Implements the Uplobder interface.
	 */
	public int getTotblAmountUploaded() {
	    if(_stbteNum == THEX_REQUEST) {
	        if(_ostrebm == null)
	            return 0;
	        else
	            return _ostrebm.getAmountWritten();
	    } else {
			if ( _totblAmountReadBefore > 0 )
				return _totblAmountReadBefore + _amountRead;
			else
	        return _totblAmountRead + _amountRead;
    }
    }

	/**
	 * Returns the <tt>FileDesc</tt> instbnce for this uploader.
	 *
	 * @return the <tt>FileDesc</tt> instbnce for this uploader, or
	 *  <tt>null</tt> if the <tt>FileDesc</tt> could not be bssigned
	 *  from the shbred files
	 */
	public FileDesc getFileDesc() {return _fileDesc;}

    boolebn getClientAcceptsXGnutellaQueryreplies() {
        return _clientAcceptsXGnutellbQueryreplies;
    }
    
    /**
     * Returns the content URN thbt the client asked for.
     */
    public URN getRequestedURN() {
        return _requestedURN;
    }

	/**
     * Rebds the HTTP header sent by the requesting client -- note that the
	 * 'GET' portion of the request hebder has already been read.
	 *
	 * @pbram iStream the input stream to read the headers from.
	 * @throws <tt>IOException</tt> if the connection closes while rebding
	 * @throws <tt>ProblemRebdingHeaderException</tt> if any header is invalid
	 */
	public void rebdHeader(InputStream iStream) throws IOException {
        _uplobdBegin = 0;
        _uplobdEnd = 0;
        _contbinedRangeRequest = false;
		_clientAcceptsXGnutellbQueryreplies = false;
		_totblAmountReadBefore = 0;
        
		ByteRebder br = new ByteReader(iStream);
        
        try {
        	while (true) {
        		// rebd the line in from the socket.
                String str = br.rebdLine();

                if ( (str==null) || (str.equbls("")) ) 
                    brebk;


                if (isForcedShbre())
                    BbndwidthStat.HTTP_HEADER_DOWNSTREAM_INNETWORK_BANDWIDTH.addData(str.length());
                else 
                    BbndwidthStat.
                        HTTP_HEADER_DOWNSTREAM_BANDWIDTH.bddData(str.length());
                if (LOG.isDebugEnbbled())
                	LOG.debug("HTTPUplobder.readHeader(): str = " +  str);

                
        		// brebk out of the loop if it is null or blank

                if      ( rebdChatHeader(str)        ) ;
                else if ( rebdRangeHeader(str)       ) ;
                else if ( rebdUserAgentHeader(str)   ) ;
                else if ( rebdContentURNHeader(str)  ) ;
                else if ( rebdAltLocationHeader(str) ) ;
                else if ( rebdNAltLocationHeader(str)) ;
                else if ( rebdFAltLocationHeader(str)) ;
                else if ( rebdNFAltLocationHeader(str));
                else if ( rebdAcceptHeader(str)      ) ;
                else if ( rebdQueueVersion(str)      ) ;
                else if ( rebdNodeHeader(str)        ) ;
                else if ( rebdFeatureHeader(str)     ) ;
                else if ( rebdXDownloadedHeader(str) ) ;
        	}
        } cbtch(ProblemReadingHeaderException prhe) {
            // there wbs a problem reading the header.. gobble up
            // the rest of the input bnd rethrow the exception
            while(true) {
                String str = br.rebdLine();
                if( str == null || str.equbls("") )
                 brebk;
            }
            
            // TODO: record stbts for this
            throw prhe;
        } finblly {
            // we wbnt to ensure these are always set, regardless
            // of if bn exception was thrown.
            
			//if invblid end-index, then upload up to the end of file
			//or mbrk as unknown to bet when file size is set.
			if( _uplobdEnd <= 0 ||
			  _uplobdEnd <= _uploadBegin || 
			  _uplobdEnd > _fileSize) {
                _uplobdEnd = _fileSize;
            }

            _bmountRequested = _uploadEnd - _uploadBegin;
            
            _hebdersParsed = true;
        }
        
	}
	

    /**
     * Rebd the chat portion of a header.
     * @return true if it hbd a chat header.
     */
    privbte boolean readChatHeader(String str) throws IOException {
        if (str.toUpperCbse().indexOf("CHAT:") == -1)
            return fblse;
    
		String sub;
		try {
			sub = str.substring(5);
		} cbtch (IndexOutOfBoundsException e) {
			throw new ProblemRebdingHeaderException();
        }
		sub = sub.trim();
		int colon = sub.indexOf(":");
		String host  = sub.substring(0,colon);
		host = host.trim();
		String sport = sub.substring(colon+1);
		sport = sport.trim();

		int port; 
		try {
			port = jbva.lang.Integer.parseInt(sport);
		} cbtch (NumberFormatException e) {
			throw new ProblemRebdingHeaderException();
        }
		_chbtEnabled = true;
		_browseEnbbled = true;
		_nodePort = port;
        
        return true;
    }
    
    /**
	 * Look for X-Downlobded header which represents number 
	 * of bytes for this file blready downloaded by peer
	 *
     * @return true if it hbd a X-Downloaded header
     */
    privbte boolean readXDownloadedHeader(String str) throws IOException {
        
        if ( !HTTPHebderName.DOWNLOADED.matchesStartOfString(str) )
            return fblse;
            
		try {
			str = HTTPUtils.extrbctHeaderValue(str);
			if ( str != null ) {
				_totblAmountReadBefore = Integer.parseInt(str);
			}
		} 
		cbtch (NumberFormatException e) {}

		return true;
    }
    
    /**
	 * Look for rbnge header of form, "Range: bytes=", "Range:bytes=",
	 * "Rbnge: bytes ", etc.  Note that the "=" is required by HTTP, but
     * old versions of BebrShare do not send it.  The value following the
     * bytes unit will be in the form '-n', 'm-n', or 'm-'.
     *
     * @return true if it hbd a Range header
     */
    privbte boolean readRangeHeader(String str) throws IOException {
        // wbs: != 0, is == -1 (that okay?)
        if ( StringUtils.indexOfIgnoreCbse(str, "Range:") == -1 )
            return fblse;
            
        _contbinedRangeRequest = true;
            
        //Set 'sub' to the vblue after the "bytes=" or "bytes ".  Note
        //thbt we don't validate the data between "Range:" and the
        //bytes.
		String sub;
		String second;
		try {
            int i=str.indexOf("bytes");    //TODO: use constbnt
            if (i<0)
                throw new ProblemRebdingHeaderException(
                     "bytes not present in rbnge");
            i+=6;                          //TODO: use constbnt
			sub = str.substring(i);
		} cbtch (IndexOutOfBoundsException e) {
			throw new ProblemRebdingHeaderException();
		}
		// remove the white spbce
        sub = sub.trim();   
        chbr c;
		// get the first chbracter
		try {
			c = sub.chbrAt(0);
		} cbtch (IndexOutOfBoundsException e) {
			throw new ProblemRebdingHeaderException();
		}
		// - n  
        if (c == '-') {  
			// String second;
			try {
				second = sub.substring(1);
			} cbtch (IndexOutOfBoundsException e) {
				throw new ProblemRebdingHeaderException();
			}
            second = second.trim();
			try {
                //A rbnge request for "-3" means return the last 3 bytes
                //of the file.  (LW used to incorrectly return bytes
                //0-3.)  
                _uplobdBegin = Math.max(0,
                                    _fileSize-Integer.pbrseInt(second));
				_uplobdEnd = _fileSize;
			} cbtch (NumberFormatException e) {
				throw new ProblemRebdingHeaderException();
			}
        }
        else {                
			// m - n or 0 -
            int dbsh = sub.indexOf("-");
            
            // If the "-" does not exist, the hebd is incorrectly formatted.
            if(dbsh == -1) {
                throw new ProblemRebdingHeaderException();
            }
			String first = sub.substring(0, dbsh).trim();
			try {
				_uplobdBegin = java.lang.Integer.parseInt(first);
			} cbtch (NumberFormatException e) {
				throw new ProblemRebdingHeaderException();
			}
			try {
				second = sub.substring(dbsh+1);
			} cbtch (IndexOutOfBoundsException e) {
				throw new ProblemRebdingHeaderException();
			}
            second = second.trim();
            if (!second.equbls("")) 
				try {
                    //HTTP rbnge requests are inclusive.  So "1-3" means
                    //bytes 1, 2, bnd 3.  But _uploadEnd is an EXCLUSIVE
                    //index, so increment by 1.
					_uplobdEnd = java.lang.Integer.parseInt(second)+1;
            } cbtch (NumberFormatException e) {
				throw new ProblemRebdingHeaderException();
			}
        }
        
        return true;
    }
    
    /**
     * Rebd the User-Agent field of the header
     *
     * @return true if the hebder had a UserAgent field
     */
    privbte boolean readUserAgentHeader(String str)
		throws FreelobderUploadingException {
        if ( StringUtils.indexOfIgnoreCbse(str, "User-Agent:") == -1 )
            return fblse;
        
		// check for netscbpe, internet explorer,
		// or other free riding downobders
        //Allow them to browse the host though
		if (ShbringSettings.ALLOW_BROWSER.getValue() == false
            && !(_stbteNum == BROWSE_HOST)  
            && !(_stbteNum == BROWSER_CONTROL)  
            && !(_stbteNum == PUSH_PROXY)  
			&& !(_fileNbme.toUpperCase().startsWith("LIMEWIRE"))) {
			// if we bre not supposed to read from them
			// throw bn exception
			if( (str.indexOf("Mozillb") != -1) ||
			    (str.indexOf("Morpheus") != -1) ||
				(str.indexOf("DA") != -1) ||
				(str.indexOf("Downlobd") != -1) ||
				(str.indexOf("FlbshGet") != -1) ||
				(str.indexOf("GetRight") != -1) ||
				(str.indexOf("Go!Zillb") != -1) ||
				(str.indexOf("Inet") != -1) ||
				(str.indexOf("MIIxpc") != -1) ||
				(str.indexOf("MSProxy") != -1) ||
				(str.indexOf("Mbss") != -1) ||
				(str.indexOf("MLdonkey") != -1) ||
				(str.indexOf("MyGetRight") != -1) ||
				(str.indexOf("NetAnts") != -1) ||
				(str.indexOf("NetZip") != -1) ||
				(str.indexOf("ReblDownload") != -1) ||
				(str.indexOf("SmbrtDownload") != -1) ||
				(str.indexOf("Teleport") != -1) ||
				(str.indexOf("WebDownlobder") != -1) ) {
                if (!_hbdPassword)
                    throw new FreelobderUploadingException();
                
                    
			}
		}
		_userAgent = str.substring(11).trim();
		
		return true;
    }
    
    /**
	 * Rebd the content URN header
	 *
     * @return true if the hebder had a contentURN field
     */
    privbte boolean readContentURNHeader(String str) {
        if ( ! HTTPHebderName.GNUTELLA_CONTENT_URN.matchesStartOfString(str) )
            return fblse;

        _requestedURN = HTTPUplobder.parseContentUrn(str);
		
		return true;
	}
	
	/**
	 * Rebd the Alternate Locations header
	 *
	 * @return true if the hebder had an alternate locations field
	 */
	privbte boolean readAltLocationHeader(String str) {
        if ( ! HTTPHebderName.ALT_LOCATION.matchesStartOfString(str) )
            return fblse;
                
        if(_fileDesc != null) 
            pbrseAlternateLocations(str, true);
        return true;
    }

    privbte boolean readNAltLocationHeader(String str) {
        if (!HTTPHebderName.NALTS.matchesStartOfString(str))
            return fblse;
        
        if(_fileDesc != null)
            pbrseAlternateLocations(str, false);
        return true;
    }
    
	privbte boolean readFAltLocationHeader(String str) {
        if ( ! HTTPHebderName.FALT_LOCATION.matchesStartOfString(str) )
            return fblse;
        
        //blso set the interested flag
        _wbntsFalts=true;
        
        if(_fileDesc != null) 
            pbrseAlternateLocations(str, true);
        return true;
    }

    privbte boolean readNFAltLocationHeader(String str) {
        if (!HTTPHebderName.BFALT_LOCATION.matchesStartOfString(str))
            return fblse;

        //blso set the interested flag
        _wbntsFalts=true;
        
        if(_fileDesc != null)
            pbrseAlternateLocations(str, false);
        return true;
    }
    
    
    

    /** 
     * Rebds the Accept heder
     *
     * @return true if the hebder had an accept field
     */
    privbte boolean readAcceptHeader(String str) {
        if ( StringUtils.indexOfIgnoreCbse(str, "accept:") == -1 )
            return fblse;
           
        if(StringUtils.indexOfIgnoreCbse(str, Constants.QUERYREPLY_MIME_TYPE) != -1)
            _clientAcceptsXGnutellbQueryreplies = true;
            
        return true;
    }	

    privbte boolean readQueueVersion(String str) {
        if (! HTTPHebderName.QUEUE_HEADER.matchesStartOfString(str))
            return fblse;
        
        //String s = HTTPUtils.extrbctHeaderValue(str);
        //we bre not interested in the value at this point, the fact that the
        //hebder was sent implies that the uploader supports queueing. 
        _supportsQueueing = true;
        return true;
    }

    /** 
     * Rebds the X-Node header
     *
     * @return true if the hebder had an node description value
     */
    privbte boolean readNodeHeader(final String str) {
        if ( !HTTPHebderName.NODE.matchesStartOfString(str) )
            return fblse;
           
        StringTokenizer st = 
            new StringTokenizer(HTTPUtils.extrbctHeaderValue(str), ":");
        InetAddress tempAddr = null;
        int tempPort = -1;
        // we bre expecting 2 tokens - only evalute if you see 2
        if (st.countTokens() == 2) {
            try {
                tempAddr = InetAddress.getByNbme(st.nextToken().trim());
                tempPort = Integer.pbrseInt(st.nextToken().trim());
                if (NetworkUtils.isVblidPort(tempPort)) {
                    // everything checks out....
                    _nodeAddress = tempAddr;
                    _nodePort = tempPort;
                }
            }
            cbtch (UnknownHostException badHost) { // crappy host
            }
            cbtch (NumberFormatException nfe) {} // crappy port
        }
            
        return true;
    }	

	/**
	 * Rebds the X-Features header
	 *
	 * @return true if the hebder had an node description value
	 */
	privbte boolean readFeatureHeader(String str) {
		if ( !HTTPHebderName.FEATURES.matchesStartOfString(str) )
			return fblse;
        str = HTTPUtils.extrbctHeaderValue(str);
        if (LOG.isDebugEnbbled())
        	LOG.debug("rebding feature header: "+str);
        StringTokenizer tok = new StringTokenizer(str, ",");
        while (tok.hbsMoreTokens()) {
            String febture = tok.nextToken();
            String protocol = "";
            int slbsh = feature.indexOf("/");
            if(slbsh == -1) {
                protocol = febture.toLowerCase().trim();
            } else {
                protocol = febture.substring(0, slash).toLowerCase().trim();
            }
            // not interested in the version ...

			if (protocol.equbls(HTTPConstants.CHAT_PROTOCOL))
				_chbtEnabled = true;
			else if (protocol.equbls(HTTPConstants.BROWSE_PROTOCOL))
				_browseEnbbled = true;
			else if (protocol.equbls(HTTPConstants.QUEUE_PROTOCOL))
				_supportsQueueing = true;
			else if (protocol.equbls(HTTPConstants.PUSH_LOCS))
            	_wbntsFalts=true;
            else if (protocol.equbls(HTTPConstants.FW_TRANSFER)){
                // for this hebder we care about the version
            	try {
            	    _FWTVersion = (int)HTTPUtils.pbrseFeatureToken(feature);
            	    _wbntsFalts=true;
            	}cbtch(ProblemReadingHeaderException prhe){
            	    continue;
            	}
            }
			
		}
		return true;
	}
	
	/**
	 * This method pbrses the "X-Gnutella-Content-URN" header, as specified
	 * in HUGE v0.93.  This bssigns the requested urn value for this 
	 * uplobd, which otherwise remains null.
	 *
	 * @pbram contentUrnStr the string containing the header
	 * @return b new <tt>URN</tt> instance for the request line, or 
	 *  <tt>null</tt> if there wbs any problem creating it
	 */
	privbte static URN parseContentUrn(final String contentUrnStr) {
		String urnStr = HTTPUtils.extrbctHeaderValue(contentUrnStr);
		
		if(urnStr == null)
		    return URN.INVALID;
		try {
			return URN.crebteSHA1Urn(urnStr);
		} cbtch(IOException e) {
		    return URN.INVALID;
		}		
	}
	
	/**
	 * Pbrses the alternate location header.  The header can contain only one
	 * blternate location, or it can contain many in the same header.
	 * This method will notify DownlobdManager of new alternate locations
	 * if the FileDesc is bn IncompleteFileDesc.
	 *
	 * @pbram altHeader the full alternate locations header
	 * @pbram alc the <tt>AlternateLocationCollector</tt> that reads alternate
	 *  locbtions should be added to
	 */
	privbte void parseAlternateLocations(final String altHeader, boolean isGood) {

		finbl String alternateLocations=HTTPUtils.extractHeaderValue(altHeader);

		URN shb1 =_fileDesc.getSHA1Urn(); 
		
		// return if the blternate locations could not be properly extracted
		if(blternateLocations == null) return;
		StringTokenizer st = new StringTokenizer(blternateLocations, ",");
        while(st.hbsMoreTokens()) {
            try {
                // note thbt the trim method removes any CRLF character
                // sequences thbt may be used if the sender is using
                // continubtions.
                AlternbteLocation al = 
                AlternbteLocation.create(st.nextToken().trim(),
                                         _fileDesc.getSHA1Urn());
                
                Assert.thbt(al.getSHA1Urn().equals(sha1));
                
                if (bl.isMe())
                    continue;
                
                if(bl instanceof PushAltLoc) 
                    ((PushAltLoc)bl).updateProxies(isGood);
                // Note: if this threbd gets preempted at this point,
                // the AlternbteLocationCollectioin may contain a PE
                // without bny proxies.
                if(isGood) 
                    RouterService.getAltlocMbnager().add(al, null);
                else
                    RouterService.getAltlocMbnager().remove(al, null);
                        
                if (bl instanceof DirectAltLoc)
                 	_writtenLocs.bdd(al);
                else
                 	_writtenPushLocs.bdd(al); // no problem if we add an existing pushloc
            } cbtch(IOException e) {
                // just return without bdding it.
                continue;
            }
        }
	}

	public void mebsureBandwidth() {
	    int written = _totblAmountRead + _amountRead;
	    if(_ostrebm != null)
	        written += _ostrebm.getAmountWritten();
        bbndwidthTracker.measureBandwidth(written);
    }

    public flobt getMeasuredBandwidth() {
        flobt retVal = 0;
        try {
            retVbl = bandwidthTracker.getMeasuredBandwidth();
        } cbtch (InsufficientDataException ide) {
            retVbl = 0;
        }
        return retVbl;
    }
    
    public flobt getAverageBandwidth() {
        return bbndwidthTracker.getAverageBandwidth();
    }
    
    public boolebn wantsFAlts() {
    	return _wbntsFalts;
    }
    
    public int wbntsFWTAlts() {
    	return _FWTVersion;
    }
    
    privbte final boolean debugOn = false;
    privbte void debug(String out) {
        if (debugOn)
            System.out.println(out);
    }

	// overrides Object.toString
	public String toString() {
        return "<"+_hostNbme+":"+ _index +">";
//  		return "HTTPUplobder:\r\n"+
//  		       "File Nbme: "+_fileName+"\r\n"+
//  		       "Host Nbme: "+_hostName+"\r\n"+
//  		       "Port:      "+_port+"\r\n"+
//  		       "File Size: "+_fileSize+"\r\n"+
//  		       "Stbte:     "+_state;
		
	}
}










