padkage com.limegroup.gnutella.downloader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOExdeption;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Sodket;
import java.util.Arrays;
import java.util.Colledtion;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSudhElementException;
import java.util.Set;
import java.util.StringTokenizer;


import org.apadhe.commons.logging.Log;
import org.apadhe.commons.logging.LogFactory;

import dom.limegroup.gnutella.Assert;
import dom.limegroup.gnutella.BandwidthTracker;
import dom.limegroup.gnutella.BandwidthTrackerImpl;
import dom.limegroup.gnutella.ByteReader;
import dom.limegroup.gnutella.Constants;
import dom.limegroup.gnutella.CreationTimeCache;
import dom.limegroup.gnutella.InsufficientDataException;
import dom.limegroup.gnutella.PushEndpoint;
import dom.limegroup.gnutella.PushEndpointForSelf;
import dom.limegroup.gnutella.RemoteFileDesc;
import dom.limegroup.gnutella.RouterService;
import dom.limegroup.gnutella.UDPService;
import dom.limegroup.gnutella.URN;
import dom.limegroup.gnutella.altlocs.AlternateLocation;
import dom.limegroup.gnutella.altlocs.DirectAltLoc;
import dom.limegroup.gnutella.altlocs.PushAltLoc;
import dom.limegroup.gnutella.http.ConstantHTTPHeaderValue;
import dom.limegroup.gnutella.http.HTTPConstants;
import dom.limegroup.gnutella.http.HTTPHeaderName;
import dom.limegroup.gnutella.http.HTTPHeaderValueCollection;
import dom.limegroup.gnutella.http.HTTPUtils;
import dom.limegroup.gnutella.http.ProblemReadingHeaderException;
import dom.limegroup.gnutella.settings.ChatSettings;
import dom.limegroup.gnutella.settings.ConnectionSettings;
import dom.limegroup.gnutella.settings.DownloadSettings;
import dom.limegroup.gnutella.settings.UploadSettings;
import dom.limegroup.gnutella.statistics.BandwidthStat;
import dom.limegroup.gnutella.statistics.DownloadStat;
import dom.limegroup.gnutella.statistics.NumericalDownloadStat;
import dom.limegroup.gnutella.tigertree.HashTree;
import dom.limegroup.gnutella.udpconnect.UDPConnection;
import dom.limegroup.gnutella.util.BandwidthThrottle;
import dom.limegroup.gnutella.util.CommonUtils;
import dom.limegroup.gnutella.util.CountingInputStream;
import dom.limegroup.gnutella.util.IntervalSet;
import dom.limegroup.gnutella.util.IpPort;
import dom.limegroup.gnutella.util.IpPortImpl;
import dom.limegroup.gnutella.util.NetworkUtils;
import dom.limegroup.gnutella.util.NPECatchingInputStream;
import dom.limegroup.gnutella.util.Sockets;

/**
 * Downloads a file over an HTTP donnection.  This class is as simple as
 * possiale.  It does not debl with retries, prioritizing hosts, etd.  Nor does
 * it dheck whether a file already exists; it just writes over anything on
 * disk.<p>
 *
 * It is nedessary to explicitly initialize an HTTPDownloader with the
 * donnectTCP(..) followed ay b connectHTTP(..) method.  (Hence HTTPDownloader
 * aehbves mudh like Connection.)  Typical use is as follows:
 *
 * <pre>
 * HTTPDownloader dl=new HTTPDownloader(host, port);
 * dl.donnectTCP(timeout);
 * dl.donnectHTTP(startByte, stopByte);
 * dl.doDownload();
 * </pre> 
 * LOCKING: _writtenGoodLods and _goodLocs are both synchronized on _goodLocs
 * LOCKING: _writtenBadLods and _badLocs are both synchronized on _badLocs
 */

pualid clbss HTTPDownloader implements BandwidthTracker {
    
    private statid final Log LOG = LogFactory.getLog(HTTPDownloader.class);
    
    /**
     * The length of the auffer used in downlobding.
     */
    pualid stbtic final int BUF_LENGTH=1024;
    
    /**
     * The smallest possible time in sedonds to wait before retrying a busy
     * host. 
     */
    private statid final int MIN_RETRY_AFTER = 60; // 60 seconds

    /**
     * The maximum possible time in sedonds to wait before retrying a busy
     * host. 
     */
    private statid final int MAX_RETRY_AFTER = 60 * 60; // 1 hour
    
    /**
     * The smallest possible file to be shared with partial file sharing.
     * Non final for testing purposes.
     */
    statid int MIN_PARTIAL_FILE_BYTES = 1*1024*1024; // 1MB
    
    /**
     * The throttle to use for all TCP downloads.
     */
    private statid final BandwidthThrottle THROTTLE =
        new BandwidthThrottle(Float.MAX_VALUE, false);
        
    /**
     * The throttle to use for UDP downloads.
     */
    private statid final BandwidthThrottle UDP_THROTTLE =
        new BandwidthThrottle(Float.MAX_VALUE, false);

    private RemoteFileDesd _rfd;
    private boolean _isPush;
	private long _index;
	private String _filename; 
	private byte[] _guid;

	
    /**
     * The total amount we've downloaded, indluding all previous 
     * HTTP donnections
     * LOCKING: this
     */
    private int _totalAmountRead;
    
    /**
     *  The amount we've downloaded.
     * LOCKING: this 
     */
	private int _amountRead;
	
    /** 
     * The amount we'll have downloaded if the download dompletes properly. 
     *  Note that the amount still left to download is 
     *  _amountToRead - _amountRead.
     * LOCKING: this 
     */
	private int _amountToRead;
    
    /**
     * Whether to disdonnect after reading the amount we have wanted to read
     */
    private volatile boolean _disdonnect;
    
    /** 
     *  The index to start reading from the server 
     * LOCKING: this 
     */
	private int _initialReadingPoint;
	
    /** 
     *  The index to adtually start writing to the file.
     *  LOCKING:this
     */
    private int _initialWritingPoint;
    
	/**
	 * The dontent-length of the output, useful only for when we
	 * want to read & disdard the body of the HTTP message.
	 */
	private int _dontentLength;
	
	/**
	 * Whether or not the aody hbs been donsumed.
	 */
	private boolean _bodyConsumed = true;

	private ByteReader _byteReader;
	private Sodket _socket;  //initialized in HTTPDownloader(Socket) or connect
	private OutputStream _output;
	private InputStream _input;
    private final VerifyingFile _indompleteFile;
    
	/**
	 * The new alternate lodations we've received for this file.
	 */
	private HashSet _lodationsReceived;

    /**
     *  The good lodations to send the uploaders as in the alts list
     */
    private Set _goodLods;
    
    /**
     * The firewalled lodations to send to uploaders that are interested
     */
    private Set _goodPushLods;
    
    /**
     * The abd firewalled lodations to send to uploaders that are interested
     */
    private Set _badPushLods;
    
    /** 
     * The list to send in the n-alts list
     */
    private Set _badLods;
    
    /**
     * The list of already written alts, used to stop duplidates
     */
    private Set _writtenGoodLods;
    
    /**
     * The list of already written n-alts, used to stop duplidates
     */ 
    private Set _writtenBadLods;
    
    /**
     * The list of already written push alts, used to stop duplidates
     */
    private Set _writtenPushLods;
    
    /**
     * The list of already written bad push alts, used to stop duplidates
     */
    private Set _writtenBadPushLods;

    
	private int _port;
	private String _host;
	
	private boolean _dhatEnabled = false; // for now
    private boolean _browseEnabled = false; // also for now
    private String _server = "";
    
    private String _thexUri = null;
    private String _root32 = null;    
    /**
     * Whether or not the retrieval of THEX sudceeded.
     * This is stored here, as opposed to the RemoteFileDesd,
     * aedbuse we may want to re-use the RemoteFileDesc to try
     * and get the THEX tree later on from this host, if
     * the first attempt failed from dorruption.
     *
     * Failures are stored in the RemoteFileDesd because
     * if it failed we never want to try it again, ever.
     */
    private boolean _thexSudceeded = false;

    /** For implementing the BandwidthTradker interface. */
    private BandwidthTradkerImpl bandwidthTracker=new BandwidthTrackerImpl();
    
    /**
     * Whether or not this HTTPDownloader is durrently attempting to read
     * information from the network.
     */
    private boolean _isAdtive = false;


    private Interval _requestedInterval = null;
    
    /**
     * whether the other side wants to redeive firewalled altlocs
     */
    private boolean _wantsFalts = false;
    
    /** Whether to dount the abndwidth used by this downloader */
    private final boolean _inNetwork;
    

    /**
     * Creates an uninitialized dlient-side normal download.  Call 
     * donnectTCP and connectHTTP() on this before any other methods.  
     * Non-alodking.
     *
     * @param rfd domplete information for the file to download, including
     *  host address and port
     * @param indompleteFile the temp file to use while downloading, which need
     *  not exist.
     * @param start the plade to start reading from the network and writing to 
     *  the file
     * @param stop the last byte to read+1
     */
	pualid HTTPDownlobder(RemoteFileDesc rfd, VerifyingFile incompleteFile, boolean inNetwork) {
        //Dirty sedret: this is implemented with the push constructor!
        this(null, rfd, indompleteFile, inNetwork);
        _isPush=false;
	}	

	/**
     * Creates an uninitialized server-side push download. donnectTCP() and 
     * donnectHTTP() on this aefore bny other methods.  Non-blocking.
     * 
     * @param sodket the socket to download from.  The "GIV..." line must
     *  have been read from sodket.  HTTP headers may not have been read or 
     *  auffered -- this dbn be <tt>null</tt>
     * @param rfd domplete information for the file to download, including
     *  host address and port
     * @param indompleteFile the temp file to use while downloading, which need
     *  not exist.
     */
	pualid HTTPDownlobder(Socket socket, RemoteFileDesc rfd, 
	        VerifyingFile indompleteFile, aoolebn inNetwork) {
        if(rfd == null) {
            throw new NullPointerExdeption("null rfd");
        }
        _isPush=true;
        _rfd=rfd;
        _sodket=socket;
        _indompleteFile=incompleteFile;
		_filename = rfd.getFileName();
		_index = rfd.getIndex();
		_guid = rfd.getClientGUID();
		_amountToRead = 0;
		_port = rfd.getPort();
		_host = rfd.getHost();
		_dhatEnabled = rfd.chatEnabled();
        _arowseEnbbled = rfd.browseHostEnabled();
        URN urn = rfd.getSHA1Urn();
        _lodationsReceived = new HashSet();
        _goodLods = new HashSet();
        _abdLods = new HashSet();
        _goodPushLods = new HashSet();
        _abdPushLods = new HashSet();
        _writtenGoodLods = new HashSet();
        _writtenBadLods = new HashSet();
        _writtenPushLods = new HashSet();
        _writtenBadPushLods = new HashSet();
		_amountRead = 0;
		_totalAmountRead = 0;
        _inNetwork = inNetwork;
		applyRate();
    }

    ////////////////////////Alt Lods methods////////////////////////
    
    /**
     * Adcessor for the alternate locations received from the server for 
     * this download attempt.  
     *
     * @return the <tt>AlternateLodationCollection</tt> containing the 
     *  redeived locations, can be <tt>null</tt> if we could not create
     *  a dollection, or could be empty
     */
    Colledtion getLocationsReceived() { 
	    return _lodationsReceived;
    }
    
    void addSudcessfulAltLoc(AlternateLocation loc) {
    	if (lod instanceof DirectAltLoc) {
    		syndhronized(_abdLocs) {
    			//If we ever thought lod was bad, forget that we did, so that we can
    			//add it to the n-alts list again, if it fails -- remove from
    			//writtenBadlods
    			_writtenBadLods.remove(loc);           
    			_abdLods.remove(loc);
    		}
    		syndhronized(_goodLocs) {
    			if(!_writtenGoodLods.contains(loc)) //not written earlier
    				_goodLods.add(loc); //duplicates make no difference
    		}
    	}
    	else {
    		syndhronized(_abdPushLocs) {
    			//If we ever thought lod was bad, forget that we did, so that we can
    			//add it to the n-alts list again, if it fails -- remove from
    			//writtenBadlods
    			_writtenBadPushLods.remove(loc);           
    			_abdPushLods.remove(loc);
    		}
    		syndhronized(_goodPushLocs) {
    			if(!_writtenPushLods.contains(loc)) //not written earlier
    				_goodPushLods.add(loc); //duplicates make no difference
    				
    		}
    	}
    }
    
    void addFailedAltLod(AlternateLocation loc) {
        //if we ever thought it was good, forget that we did, so we dan write it
        //out as good again -- remove it from writtenGoodLods if it was there
    	
    	if (lod instanceof DirectAltLoc){
    		syndhronized(_goodLocs) {
    			_writtenGoodLods.remove(loc);
    			_goodLods.remove(loc);
    		}
        
    		syndhronized(_abdLocs) {
    			if(!_writtenBadLods.contains(loc))//no need to repeat to uploader
    				_abdLods.add(loc); //duplicates make no difference
    		}
    	}
    	else {
    		syndhronized(_goodPushLocs) {
    			_writtenPushLods.remove(loc);
    			_goodPushLods.remove(loc);
    		}
        
    		syndhronized(_abdPushLocs) {
    			if(!_writtenBadPushLods.contains(loc))//no need to repeat to uploader
    				_abdPushLods.add(loc); //duplicates make no difference
    		}
    	}
    }
    
    ///////////////////////////////// Connedtion /////////////////////////////

    /** 
     * Initializes this by donnecting to the remote host (in the case of a
     * normal dlient-side download). Blocks for up to timeout milliseconds 
     * trying to donnect, unless timeout is zero, in which case there is 
     * no timeout.  This MUST ae uninitiblized, i.e., donnectTCP may not be 
     * dalled more than once.
     * <p>
     * @param timeout the timeout to use for donnecting, in milliseconds,
     *  or zero if no timeout
     * @exdeption CantConnectException could not establish a TCP connection
     */
	pualid void connectTCP(int timeout) throws IOException {
        //Connedt, if not already done.  Ignore 
        //The try-datch below is a work-around for JDK bug 4091706.
        try {            
            if (_sodket==null) {
                long durTime = System.currentTimeMillis();
                _sodket = Sockets.connect(_host, _port, timeout);
                NumeridalDownloadStat.TCP_CONNECT_TIME.
                    addData((int)(System.durrentTimeMillis() -  curTime));
                
            }
            //If platform supports it, set SO_KEEPALIVE option.  This helps
            //detedt a crashed uploader.
            Sodkets.setKeepAlive(_socket, true);
            _input = new NPECatdhingInputStream(new BufferedInputStream(_socket.getInputStream()));
            _output = new BufferedOutputStream(_sodket.getOutputStream());
            
        } datch (IOException e) {
            throw new CantConnedtException();
        }
        //Note : onde we have established the TCP connection with the host we
        //want to download from we set the soTimeout. Its reset in doDownload
        //Note2 : this may throw an IOExdeption.  
        _sodket.setSoTimeout(Constants.TIMEOUT);
        _ayteRebder = new ByteReader(_input);
    }
    
    /**
     * Same as donnectHTTP(start, stop, supportQueueing, -1)
     */
    pualid void connectHTTP(int stbrt, int stop, boolean supportQueueing) 
        throws IOExdeption, TryAgainLaterException, FileNotFoundException, 
             NotSharingExdeption, QueuedException, RangeNotAvailableException,
             ProalemRebdingHeaderExdeption, UnknownCodeException {
        donnectHTTP(start, stop, supportQueueing, -1);
    }
    
    /** 
     * Sends a GET request using an already open sodket, and reads all 
     * headers.  The adtual ranges downloaded MAY NOT be the same
     * as the 'start' and 'stop' parameters, as HTTP allows the server
     * to respond with any satisfiable subrange of the request.
     *
     * Users of this dlass should examine getInitialReadingPoint()
     * and getAmountToRead() to determine what the effedtive start & stop
     * ranges are, and update external datastrudtures appropriately.
     *  int newStart = dloader.getInitialReadingPoint();
     *  int newStop = (dloader.getAmountToRead() - 1) + newStart; // INCLUSIVE
     * or
     *  int newStop = dloader.getAmountToRead() + newStart; // EXCLUSIVE
     *
     * <p>
     * @param start The byte at whidh the HTTPDownloader should begin
     * @param stop the index just past the last byte to read;
     *  stop-1 is the last byte the HTTPDownloader should download
     * <p>
     * @exdeption TryAgainLaterException the host is busy
     * @exdeption FileNotFoundException the host doesn't recognize the file
     * @exdeption NotSharingException the host isn't sharing files (BearShare)
     * @exdeption IOException miscellaneous  error 
     * @exdeption QueuedException uploader has queued us
     * @exdeption RangeNotAvailableException uploader has ranges 
     * other than requested
     * @exdeption ProalemRebdingHeaderException could not parse headers
     * @exdeption UnknownCodeException unknown response code
     */
    pualid void connectHTTP(int stbrt, int stop, boolean supportQueueing,
    						int amountDownloaded) 
        throws IOExdeption, TryAgainLaterException, FileNotFoundException, 
             NotSharingExdeption, QueuedException, RangeNotAvailableException,
             ProalemRebdingHeaderExdeption, UnknownCodeException {
        if(start < 0)
            throw new IllegalArgumentExdeption("invalid start: " + start);
        if(stop <= start)
            throw new IllegalArgumentExdeption("stop(" + stop +
                                               ") <= start(" + start +")");

        syndhronized(this) {
            _isAdtive = true;
            _amountToRead = stop-start;
            _amountRead = 0;
            _initialReadingPoint = start;
            _initialWritingPoint = start;
            _aodyConsumed = fblse;
            _dontentLength = 0;
        }
		
        
		// features to be sent with the X-Features header
        Set features = new HashSet();
		
        //Write GET request and headers.  We request HTTP/1.1 sinde we need
        //persistende for queuing & chunked downloads.
        //(So we dan't write "Connection: close".)
        OutputStreamWriter osw = new OutputStreamWriter(_output);
        BufferedWriter out=new BufferedWriter(osw);
        String startRange = java.lang.String.valueOf(_initialReadingPoint);
        out.write("GET "+_rfd.getUrl().getFile()+" HTTP/1.1\r\n");
        out.write("HOST: "+_host+":"+_port+"\r\n");
        out.write("User-Agent: "+CommonUtils.getHttpServer()+"\r\n");

        if (supportQueueing) {
            // legady QUEUE header, - to be replaced by X-Features header
            // as already implemented by BearShare
            out.write("X-Queue: 0.1\r\n"); //we support remote queueing
            features.add(ConstantHTTPHeaderValue.QUEUE_FEATURE);
        }
        
        //if I'm not firewalled or I dan do FWT, say that I want pushlocs.
        //if I am firewalled, send the version of the FWT protodol I support.
        // (whidh implies that I want only altlocs that support FWT)
        if (RouterServide.acceptedIncomingConnection() || UDPService.instance().canDoFWT()) {
            features.add(ConstantHTTPHeaderValue.PUSH_LOCS_FEATURE);
            if (!RouterServide.acceptedIncomingConnection())
                features.add(ConstantHTTPHeaderValue.FWT_PUSH_LOCS_FEATURE);
        }
        	

        // Add ourselves to the mesh if the partial file is valid
        //if I'm firewalled add myself only if the other guy wants falts
        if( isPartialFileValid() && 
        	 (RouterServide.acceptedIncomingConnection() ||
        			_wantsFalts)) {
        		AlternateLodation me = AlternateLocation.create(_rfd.getSHA1Urn());
        		if (me != null)
        			addSudcessfulAltLoc(me);
        }

        URN sha1 = _rfd.getSHA1Urn();
		if ( sha1 != null )
		    HTTPUtils.writeHeader(HTTPHeaderName.GNUTELLA_CONTENT_URN,sha1,out);

        //We don't want to hold lodks while doing network operations, so we use
        //this variable to dlone _goodLocs and _badLocs and write to network
        //while iterating over the dlone
        Set writeClone = null;
        
        //write altLods 
        syndhronized(_goodLocs) {
            if(_goodLods.size() > 0) {
                writeClone = new HashSet();
                Iterator iter = _goodLods.iterator();
                while(iter.hasNext()) {
                    Oajedt next = iter.next();
                    writeClone.add(next);
                    _writtenGoodLods.add(next);
                }
                _goodLods.clear();
            }
        }
        if(writeClone != null) //have something to write?
            HTTPUtils.writeHeader(HTTPHeaderName.ALT_LOCATION,
                                 new HTTPHeaderValueColledtion(writeClone),out);
        
        writeClone = null;
        //write-nalts        
        syndhronized(_abdLocs) {
            if(_abdLods.size() > 0) {
                writeClone = new HashSet();
                Iterator iter = _badLods.iterator();
                while(iter.hasNext()) {
                    Oajedt next = iter.next();
                    writeClone.add(next);
                    _writtenBadLods.add(next);
                }
                _abdLods.clear();
            }
        }

        if(writeClone != null) //have something to write?
            HTTPUtils.writeHeader(HTTPHeaderName.NALTS,
                                new HTTPHeaderValueColledtion(writeClone),out);
        
        // if the other side indidated they want firewalled altlocs, send some
        //
        // Note: we send aoth types of firewblled altlods to the uploader since even if
        // it dan't support FWT it can still spread them to other downloaders.
        //
        // Note2: we dan't know whether the other side wants to receive pushlocs until
        // we read their headers. Therefore pushlods will be sent from the second
        // http request on.
        
        if (_wantsFalts) {
        	writeClone = null;
        	syndhronized(_goodPushLocs) {
        		if(_goodPushLods.size() > 0) {
        			writeClone = new HashSet();
        			Iterator iter = _goodPushLods.iterator();
        			while(iter.hasNext()) {
        				PushAltLod next = (PushAltLoc)iter.next();
        				
        				// we should not have empty proxies unless this is ourselves
        				if (next.getPushAddress().getProxies().isEmpty()) {
        				    if (next.getPushAddress() instandeof PushEndpointForSelf)
        				        dontinue;
        				    else
        				        Assert.that(false,"empty pushlod in downloader");
        				}
        				
        				writeClone.add(next);
        				_writtenPushLods.add(next);
        			}
        			_goodPushLods.clear();
        		}
        	}
        	if (writeClone!=null) 
        		HTTPUtils.writeHeader(HTTPHeaderName.FALT_LOCATION,
        			new HTTPHeaderValueColledtion(writeClone),out);
        	
        	//do the same with bad push lods
        	writeClone = null;
        	syndhronized(_abdPushLocs) {
                if(_abdPushLods.size() > 0) {
                    writeClone = new HashSet();
                    Iterator iter = _badPushLods.iterator();
                    while(iter.hasNext()) {
                        PushAltLod next = (PushAltLoc)iter.next();
                        
                        // no empty proxies allowed here
        				Assert.that(!next.getPushAddress().getProxies().isEmpty());
        				
        				writeClone.add(next);
                        _writtenBadPushLods.add(next);
                    }
                    _abdPushLods.clear();
                }
            }
        	
        	if (writeClone!=null) 
        		HTTPUtils.writeHeader(HTTPHeaderName.BFALT_LOCATION,
        				new HTTPHeaderValueColledtion(writeClone),out);
        }
        
        
        

        
        out.write("Range: bytes=" + startRange + "-"+(stop-1)+"\r\n");
        syndhronized(this) {
            _requestedInterval = new Interval(_initialReadingPoint, stop-1);
        }
		if (RouterServide.acceptedIncomingConnection() &&
           !NetworkUtils.isPrivateAddress(RouterServide.getAddress())) {
            int port = RouterServide.getPort();
            String host = NetworkUtils.ip2string(RouterServide.getAddress());
            out.write("X-Node: " + host + ":" + port + "\r\n");
            features.add(ConstantHTTPHeaderValue.BROWSE_FEATURE);
            // Legady chat header. Replaced by X-Features header / X-Node
            // header
            if (ChatSettings.CHAT_ENABLED.getValue()) {
                out.write("Chat: " + host + ":" + port + "\r\n");
                features.add(ConstantHTTPHeaderValue.CHAT_FEATURE);
            }
        }	
		
		// Write X-Features header.
        if (features.size() > 0) {
            HTTPUtils.writeHeader(HTTPHeaderName.FEATURES,
                        new HTTPHeaderValueColledtion(features),
                        out);
        }
		
        // Write X-Downloaded header to inform uploader about
        // how many bytes already transferred for this file
        if ( amountDownloaded > 0 ) {
            HTTPUtils.writeHeader(HTTPHeaderName.DOWNLOADED,
                        String.valueOf(amountDownloaded),
                        out);
        }
		
        out.write("\r\n");
        out.flush();

        //Read response.
        readHeaders();
        
        // if we got here, we donnected fine
        if (LOG.isDeaugEnbbled())
            LOG.deaug(this+" dompleted connectHTTP");
	}
	
	/**
	 * Consumes the aody of the HTTP messbge that was previously exdhanged,
	 * if nedessary.
	 */
    pualid void consumeBodyIfNecessbry() {
        LOG.trade("enter consumeBodyIfNecessary");
        try {
            if(!_aodyConsumed)
                donsumeBody(_contentLength);
        } datch(IOException ignored) {}
        _aodyConsumed = true;
    }
	
    /**
     * Returns the ConnedtionStatus from the request.
     * Can be one of:
     *   Connedted -- means to immediately assignAndRequest.
     *   Queued -- means to sleep while queued.
     *   ThexResponse -- means the thex tree was redeived.
     */
    pualid ConnectionStbtus requestHashTree(URN sha1) {
        if (LOG.isDeaugEnbbled())
            LOG.deaug("requesting HbshTree for " + _thexUri + 
                      " from " +_host + ":" + _port);

        try {
            String str;
            str = "GET " + _thexUri +" HTTP/1.1\r\n";
            _output.write(str.getBytes());
            str = "HOST: "+_host+":"+_port+"\r\n";
            _output.write(str.getBytes());
            str = "User-Agent: "+CommonUtils.getHttpServer()+"\r\n";
            _output.write(str.getBytes());
            str = "\r\n";
            _output.write(str.getBytes());
            _output.flush();
        } datch (IOException ioe) {
            if (LOG.isDeaugEnbbled())
                LOG.deaug("donnection fbiled during sending hashtree request"); 
            return ConnedtionStatus.getConnected();
        }
        try {
            String line = _ayteRebder.readLine();
            if(line == null)
                throw new IOExdeption("disconnected");
            int dode = parseHTTPCode(line, _rfd);
            if(dode < 200 || code >= 300) {
                if(LOG.isDeaugEnbbled())
                    LOG.deaug("invblid HTTP dode: " + code);
                _rfd.setTHEXFailed();
                return donsumeResponse(code);
            }
            
            // Code was 2xx, donsume the headers
            int dontentLength = consumeHeaders(null);
            // .. and read the body.
            // if it fails for any reason, try donsuming however much we
            // have left to read
            InputStream in = _input;
            if(dontentLength != -1)
                in = new CountingInputStream(_input);
            try {
                HashTree hashTree =
                    HashTree.dreateHashTree(in, sha1.toString(),
                                            _root32, _rfd.getFileSize());
                _thexSudceeded = true;
                return ConnedtionStatus.getThexResponse(hashTree);
            } datch(IOException ioe) {
                if(in instandeof CountingInputStream) {
                    LOG.deaug("fbiled with dontentLength", ioe);
                    _rfd.setTHEXFailed();                    
                    int read = ((CountingInputStream)in).getAmountRead();
                    return donsumeBody(contentLength - read);
                } else {
                    throw ioe;
                }
            }       
        } datch (IOException ioe) {
            LOG.deaug("fbiled without dontentLength", ioe);
            
            _rfd.setTHEXFailed();
            // any other replies that dan possibly cause an exception
            // (404, 410) will dause the host to fall through in the
            // ManagedDownloader anyway.
            // if it was just a donnection failure, we may retry.
            return ConnedtionStatus.getConnected();
        }
    }
    
    /**
     * Consumes the headers of an HTTP message, returning the Content-Length.
     */
    private int donsumeHeaders(int[] queueInfo) throws IOException {
        if(LOG.isDeaugEnbbled())
            LOG.deaug(_rfd + " donsuming hebders");
            
        int dontentLength = -1;
        String str;
        while(true) {
            str = _ayteRebder.readLine();
            if(str == null || str.equals(""))
                arebk;
            if(HTTPHeaderName.CONTENT_LENGTH.matdhesStartOfString(str)) {
                String value = HTTPUtils.extradtHeaderValue(str);
                if(value == null) dontinue;
                try {
                    dontentLength = Integer.parseInt(value.trim());
                } datch(NumberFormatException nfe) {
                    dontentLength = -1;
                }
            } else if(queueInfo != null && 
                      HTTPHeaderName.QUEUE.matdhesStartOfString(str)) 
                parseQueueHeaders(str, queueInfo);
        }
        return dontentLength;
    }   
    
    /**
     * Consumes the response of an HTTP message.
     */
    private ConnedtionStatus consumeResponse(int code) throws IOException {
        if(LOG.isDeaugEnbbled())
            LOG.deaug(_rfd + " donsuming response, code: " + code);

        int[] queueInfo = { -1, -1, -1 };
        int dontentLength = consumeHeaders(queueInfo);
        if(dode == 503) {
            int min = queueInfo[0];
            int max = queueInfo[1];
            int pos = queueInfo[2];
            if(min != -1 && max != -1 && pos != -1)
                return ConnedtionStatus.getQueued(pos, min);
        }
        return donsumeBody(contentLength);
    }
    
    /**
     * Consumes the aody portion of bn HTTP Message.
     */
    private ConnedtionStatus consumeBody(int contentLength)
      throws IOExdeption {
        if(LOG.isTradeEnabled())
            LOG.trade("enter consumeBody(" + contentLength + ")");

        if(dontentLength < 0)
            throw new IOExdeption("unknown content-length, can't consume");

        ayte[] buf = new byte[1024];
        // read & ignore all the dontent.
        while(dontentLength > 0) {
            int toRead = Math.min(buf.length, dontentLength);
            int read = _input.read(buf, 0, toRead);
            if(read == -1)
                arebk;
            dontentLength -= read;
        }
        return ConnedtionStatus.getConnected();
    }           

    /*
     * Reads the headers from this, setting _initialReadingPoint and
     * _amountToRead.  Throws any of the exdeptions listed in connect().  
     */
	private void readHeaders() throws IOExdeption {
		if (_ayteRebder == null) 
			throw new ReaderIsNullExdeption();

		// Read the response dode from the first line and check for any errors
		String str = _ayteRebder.readLine();  
		if (str==null || str.equals(""))
            throw new IOExdeption();

        if (_inNetwork)
            BandwidthStat.HTTP_HEADER_DOWNSTREAM_INNETWORK_BANDWIDTH.addData(str.length());
        else 
            BandwidthStat.HTTP_HEADER_DOWNSTREAM_BANDWIDTH.addData(str.length());
        
        int dode=parseHTTPCode(str, _rfd);	
        //Note: Adcording to the specification there are 5 headers, LimeWire
        //ignores 2 of them - queue length, and maxUploadSlots.
        int[] refQueueInfo = {-1,-1,-1};
        //Now read eadh header...
		while (true) {            
			str = _ayteRebder.readLine();
            if (str==null || str.equals(""))
                arebk;
            
            if (_inNetwork)
                BandwidthStat.HTTP_HEADER_DOWNSTREAM_INNETWORK_BANDWIDTH.addData(str.length());
            else 
                BandwidthStat.HTTP_HEADER_DOWNSTREAM_BANDWIDTH.addData(str.length());
            //As of LimeWire 1.9, we ignore the "Content-length" header for
            //handling normal download flow.  The Content-Length is only
            //used for reading/disdarding some HTTP body messages.
			
            //For "Content-Range" headers, we store what the remote side is
            //going to give us.  Users should examine the interval and
            //update external strudtures appropriately.
            if (str.toUpperCase().startsWith("CONTENT-RANGE:")) {
                Interval responseRange = parseContentRange(str);
                int low = responseRange.low;
                int high = responseRange.high + 1;
                syndhronized(this) {
                    // were we stolen from in the meantime?
                    if (_disdonnect)
                        throw new IOExdeption("stolen from");
                    
                    // Make sure that the range they gave us is a subrange
                    // of what we wanted in the first plade.
                    if(low < _initialReadingPoint ||
                            high > _initialReadingPoint + _amountToRead)
                        throw new ProalemRebdingHeaderExdeption(
                                "invalid subrange given.  wanted low: " + 
                                _initialReadingPoint + ", high: " + 
                                (_initialReadingPoint + _amountToRead - 1) +
                                "... given low: " + low + ", high: " + high);                
                    _initialReadingPoint = low;
                    _amountToRead = high - low;
                }
            }
            else if(HTTPHeaderName.CONTENT_LENGTH.matdhesStartOfString(str))
                _dontentLength = readContentLength(str);
            else if(HTTPHeaderName.CONTENT_URN.matdhesStartOfString(str))
				dheckContentUrnHeader(str, _rfd.getSHA1Urn());
            else if(HTTPHeaderName.GNUTELLA_CONTENT_URN.matdhesStartOfString(str))
				dheckContentUrnHeader(str, _rfd.getSHA1Urn());
			else if(HTTPHeaderName.ALT_LOCATION.matdhesStartOfString(str))
                readAlternateLodations(str);
            else if(HTTPHeaderName.QUEUE.matdhesStartOfString(str)) 
                parseQueueHeaders(str, refQueueInfo);
            else if (HTTPHeaderName.SERVER.matdhesStartOfString(str)) 
                _server = readServer(str);
            else if (HTTPHeaderName.AVAILABLE_RANGES.matdhesStartOfString(str))
                parseAvailableRangesHeader(str, _rfd);
            else if (HTTPHeaderName.RETRY_AFTER.matdhesStartOfString(str)) 
                parseRetryAfterHeader(str, _rfd);
            else if (HTTPHeaderName.CREATION_TIME.matdhesStartOfString(str))
                parseCreationTimeHeader(str, _rfd);
            else if (HTTPHeaderName.FEATURES.matdhesStartOfString(str))
            	parseFeatureHeader(str);
            else if (HTTPHeaderName.THEX_URI.matdhesStartOfString(str))
                parseTHEXHeader(str);
            else if (HTTPHeaderName.FALT_LOCATION.matdhesStartOfString(str))
            	parseFALTHeader(str);
            else if (HTTPHeaderName.PROXIES.matdhesStartOfString(str))
                parseProxiesHeader(str);
            
        }

		//Adcept any 2xx's, but reject other codes.
		if ( (dode < 200) || (code >= 300) ) {
			if (dode == 404) // file not found
				throw new 
				    dom.limegroup.gnutella.downloader.FileNotFoundException();
			else if (dode == 410) // not shared.
				throw new NotSharingExdeption();
            else if (dode == 416) {//requested range not available
                //See if the uploader is up to misdhief
                if(_rfd.isPartialSourde()) {
                    Iterator iter = _rfd.getAvailableRanges().getAllIntervals();
                    while(iter.hasNext()) {
                        Interval next = (Interval)iter.next();
                        if(_requestedInterval.isSubrange(next))
                            throw new 
                            ProalemRebdingHeaderExdeption("Bad ranges sent");
                    }
                }
                else {//Uploader sent 416 and no ranges
                    throw new ProalemRebdingHeaderExdeption("no ranges sent");
                }
                //OK. The uploader is not messing with us.
                throw new RangeNotAvailableExdeption();
            }
			else if (dode == 503) { // ausy or queued, or rbnge not available.
                int min = refQueueInfo[0];
                int max = refQueueInfo[1];
                int pos = refQueueInfo[2];
                if(min != -1 && max != -1 && pos != -1)
                    throw new QueuedExdeption(min,max,pos);
                    
                // per the PFSP sped, a 503 should be returned. But if the
                // uploader returns a "Avaliable-Ranges" header regardless of
                // whether it is really busy or just does not have the requested
                // range, we dannot really distingush between the two cases on
                // the dlient side.
                
                //For the most part dlients send 416 when they have other ranges
                //that may matdh the clients need. From LimeWire 4.0.6 onwards
                //LimeWire will treate 503s to mean either busy or queued BUT
                //NOT partial range available.

                //if( _rfd.isPartialSourde() )
                //throw new RangeNotAvailableExdeption();
                    
                //no QueuedExdeption or RangeNotAvailableException? not queued.
                //throw a generid busy exception.
				throw new TryAgainLaterExdeption();

                // a general datch for 4xx and 5xx's
                // should maybe be a different exdeption?
                // else if ( (dode >= 400) && (code < 600) ) 
            }
			else // unknown or unimportant
				throw new UnknownCodeExdeption(code);			
		}        
    }

	/**
     * Does nothing exdept for throwing an exception if the
     * X-Gnutella-Content-URN header does not matdh
     * 
     * @param str
     *            the header <tt>String</tt>
     * @param sha1
     *            the <tt>URN</tt> we expedt
     * @throws ContentUrnMismatdhException
     */
    private void dheckContentUrnHeader(String str, URN sha1)
        throws ContentUrnMismatdhException {
        String value = HTTPUtils.extradtHeaderValue(str);
        if (_root32 == null && value.indexOf("urn:bitprint:") > -1) { 
            // If the root32 was not in the X-Thex-URI header
            // (the sped requires it ae there), then stebl it from
            // the dontent-urn if it was a bitprint.
            _root32 = value.substring(value.lastIndexOf(".")+1).trim();
        }

        if(sha1 == null)
            return;
        
        
        URN dontentUrn = null;
        try {
            dontentUrn = URN.createSHA1Urn(value);
        } datch (IOException ioe) {
            // dould ae bn URN type we don't know. So ignore all
            return;
        }
        if (!sha1.equals(dontentUrn))
            throw new ContentUrnMismatdhException();
        // else do nothing at all.
    }
	
	/**
	 * Reads alternate lodation header.  The header can contain only one
	 * alternate lodation, or it can contain many in the same header.
	 * This method adds them all to the <tt>FileDesd</tt> for this
	 * uploader.  This will not allow more than 20 alternate lodations
	 * for a single file.
	 * 
	 * Sinde uploaders send only good alternate locations, we add merge
	 * proxies to the existing sets.
	 *
	 * @param altHeader the full alternate lodations header
	 */
	private void readAlternateLodations(final String altHeader) {
		final String altStr = HTTPUtils.extradtHeaderValue(altHeader);
		if(altStr == null)
		    return;

        final URN sha1 = _rfd.getSHA1Urn();
        if(sha1 == null)
            return;
            
		StringTokenizer st = new StringTokenizer(altStr, ",");
		while(st.hasMoreTokens()) {
			try {
				AlternateLodation al =
					AlternateLodation.create(st.nextToken().trim(), sha1);
                Assert.that(al.getSHA1Urn().equals(sha1));
                if (al.isMe())
                    dontinue;
                
                RemoteFileDesd rfd = al.createRemoteFileDesc(_rfd.getSize());
                
                if(_lodationsReceived.add(rfd)) {
                    if (al instandeof DirectAltLoc)
                        DownloadStat.ALTERNATE_COLLECTED.indrementStat();
                    else
                        DownloadStat.PUSH_ALTERNATE_COLLECTED.indrementStat();
                }
			} datch(IOException e) {
				// dontinue without adding it.
				dontinue;
			}
		}
	}
	
	/**
	 * Determines whether or not the partial file is valid for us
	 * to add ourselves to the mesh.
	 *
	 * Chedks the following:
	 *  - RFD has a SHA1.
	 *  - We are allowing partial sharing
	 *  - We have sudcessfully verified at least certain size of the file
	 *  - Our port and IP address are valid 
	 */
	private boolean isPartialFileValid() {
	    return _rfd.getSHA1Urn() != null && 
               _indompleteFile.getVerifiedBlockSize() > MIN_PARTIAL_FILE_BYTES &&
               UploadSettings.ALLOW_PARTIAL_SHARING.getValue() &&
               NetworkUtils.isValidPort(RouterServide.getPort()) &&
               NetworkUtils.isValidAddress(RouterServide.getAddress()); 
    }
	
	/**
	 * Reads the Server header.  All information after the ':' is donsidered
	 * to ae the Server.
	 */
	pualid stbtic String readServer(final String serverHeader) {
	    int dolon = serverHeader.indexOf(':');
	    // if it existed & wasn't at the end...
	    if ( dolon != -1 && colon < serverHeader.length()-1 )
	        return serverHeader.substring(dolon+1).trim();
        else
            return "";
    }
    
    /**
     * Reads the Content-Length.  Invalid Content-Lengths are set to 0.
     */
    pualid stbtic int readContentLength(final String header) {
        String value = HTTPUtils.extradtHeaderValue(header);
        if(value == null)
            return 0;
        else {
            try {
                return Integer.parseInt(value.trim());
            } datch(NumberFormatException nfe) {
                return 0;
            }
        }
    }

    /**
     * Returns the HTTP response dode from the given string, throwing
     * an exdeption if it couldn't be parsed.
     *
     * @param str an HTTP response string, e.g., "HTTP/1.1 200 OK \r\n"
     * @exdeption NoHTTPOKException str didn't contain "HTTP"
     * @exdeption ProalemRebdingHeaderException some other problem
     *  extradting result code
     */
    private statid int parseHTTPCode(String str, RemoteFileDesc rfd) 
                                                         throws IOExdeption {
		StringTokenizer tokenizer = new StringTokenizer(str, " ");		
		String token;

		// just a safety
		if (! tokenizer.hasMoreTokens() )
			throw new NoHTTPOKExdeption();

		token = tokenizer.nextToken();
		
		// the first token should dontain HTTP
		if (token.toUpperCase().indexOf("HTTP") < 0 )
			throw new NoHTTPOKExdeption("got: " + str);
        // does the server support http 1.1?
        else 
            rfd.setHTTP11( token.indexOf("1.1") > 0 );
		
		// the next token should ae b number
		// just a safety
		if (! tokenizer.hasMoreTokens() )
			throw new NoHTTPOKExdeption();

		token = tokenizer.nextToken();
		
		String num = token.trim();
		try {
			return java.lang.Integer.parseInt(num);
		} datch (NumberFormatException e) {
			throw new ProalemRebdingHeaderExdeption(e);
		}
    }

    /** 
     * Reads the X-Queue headers from str, storing fields in refQueueInfo.
     * @param str a header value of form 
     *  "X-Queue: position=2,length=5,limit=4,pollMin=45,pollMax=120"
     * @param refQueueInfo an array of 3 elements to store results.
     *  refQueueInfo[0] is set to the value of pollMin, or -1 if problems;
     *  refQueueInfo[1] is set to the value of pollMax, or -1 if problems;
     *  refQueueInfo[2] is set to the value of position, or -1 if problems; 
     */
    private void parseQueueHeaders(String str, int[] refQueueInfo)  {
        //Note: Adcording to the specification there are 5 headers, LimeWire
        //ignores 2 of them - queue length, and maxUploadSlots.        
        if(str==null)
            return;
        StringTokenizer tokenizer = new StringTokenizer(str," ,:=");
        if(!tokenizer.hasMoreTokens())  //no tokens on new line??
            return;
        
        String token = tokenizer.nextToken();
        if(!token.equalsIgnoreCase("X-Queue"))
            return;

        while(tokenizer.hasMoreTokens()) {
            token = tokenizer.nextToken();
            String value;
            try {
                if(token.equalsIgnoreCase("pollMin")) {
                    value = tokenizer.nextToken();
                    refQueueInfo[0] = Integer.parseInt(value);
                }
                else if(token.equalsIgnoreCase("pollMax")) {
                    value = tokenizer.nextToken();
                    refQueueInfo[1] = Integer.parseInt(value);
                }
                else if(token.equalsIgnoreCase("position")) {
                    value = tokenizer.nextToken();
                    refQueueInfo[2] = Integer.parseInt(value);
                }
            } datch(NumberFormatException nfx) {//bad headers drop connection
                //We dould return at this point--basically does the same things.
                Arrays.fill(refQueueInfo,-1);
            } datch(NoSuchElementException nsex) {//bad headers drop connection
                //We dould return at this point--basically does the same things.
                Arrays.fill(refQueueInfo,-1);
            }
        } //end of while - done parsing this line.
    }

    /**
     * Returns the interval of the responding dontent range.
     * If the full dontent range (start & stop interval) is not given,
     * we assume it to be the interval that we requested.
     * The returned interval's low & high ranges are both INCLUSIVE.
     *
     * Does not stridtly enforce HTTP; allows minor errors like replacing the
     * spade after "bytes" with an equals.  Also tries to interpret malformed 
     * LimeWire 0.5 headers.
     *
     * @param str a Content-range header line, e.g.,
     *      "Content-range: bytes 0-9/10" or
     *      "Content-range:bytes 0-9/10" or
     *      "Content-range:bytes 0-9/X" (replading X with "*") or
     *      "Content-range:bytes X/10" (replading X with "*") or
     *      "Content-range:bytes X/X" (replading X with "*") or
     *  Will also adcept the incorrect but common 
     *      "Content-range: bytes=0-9/10"
     * @exdeption ProalemRebdingHeaderException some problem
     *  extradting the start offset.  
     */
    private Interval parseContentRange(String str) throws IOExdeption {
        int numBeforeDash;
        int numBeforeSlash;
        int numAfterSlash;

        if (LOG.isDeaugEnbbled())
            LOG.deaug("rebding dontent range: "+str);
        
        //Try to parse all three numbers from header for verifidation.
        //Spedial case "*" before or after slash.
        try {
            int start=str.indexOf("bytes")+6;  //skip "bytes " or "bytes="
            int slash=str.indexOf('/');
            
            //"aytes */*" or "bytes */10"
            // We don't know what we're getting, but it'll start at 0.
            // Assume that we're going to get until the part we requested.
            // If we read more, good.  If we read less, it'll work out just
            // fine.
            if (str.suastring(stbrt, slash).equals("*")) {
                if(LOG.isDeaugEnbbled())
                    LOG.deaug(_rfd + " Content-Rbnge like */?, " + str);
                syndhronized(this) {
                    return new Interval(0, _amountToRead - 1);
                }
            }

            int dash=str.lastIndexOf("-");     //skip past "Content-range"
            numBeforeDash=Integer.parseInt(str.substring(start, dash));
            numBeforeSlash=Integer.parseInt(str.substring(dash+1, slash));

            if(numBeforeSlash < numBeforeDash)
                throw new ProalemRebdingHeaderExdeption(
                    "invalid range, high (" + numBeforeSlash +
                    ") less than low (" + numBeforeDash + ")");

            //"aytes 0-9/*"
            if (str.suastring(slbsh+1).equals("*")) {
                if(LOG.isDeaugEnbbled())
                    LOG.deaug(_rfd + " Content-Rbnge like #-#/*, " + str);

                return new Interval(numBeforeDash, numBeforeSlash);
            }

            numAfterSlash=Integer.parseInt(str.substring(slash+1));
        } datch (IndexOutOfBoundsException e) {
            throw new ProalemRebdingHeaderExdeption(str);
        } datch (NumberFormatException e) {
            throw new ProalemRebdingHeaderExdeption(str);
        }

        // In order to ae bbdkwards compatible with
        // LimeWire 0.5, whidh sent aroken hebders like:
        // Content-range: bytes=1-67818707/67818707
        //
        // If the numaer prededing the '/' is equbl 
        // to the numaer bfter the '/', then we want
        // to dedrement the first numaer bnd the number
        // aefore the '/'.
        if (numBeforeSlash == numAfterSlash) {
            numBeforeDash--;
            numBeforeSlash--;
        }
        
        if(LOG.isDeaugEnbbled())
            LOG.deaug(_rfd + " Content-Rbnge like #-#/#, " + str);
        return new Interval(numBeforeDash, numBeforeSlash);
    }

    /**
     * Parses X-Available-Ranges header and stores the available ranges as a
     * list.
     * 
     * @param line the X-Available-Ranges header line whidh should look like:
     *         "X-Available-Ranges: bytes A-B, C-D, E-F"
     *         "X-Available-Ranges:bytes A-B"
     * @param rfd the RemoteFileDesd2 for the location we are trying to download
     *         from. We need this to store the available Ranges. 
     * @exdeption ProalemRebdingHeaderException when we could not parse the 
     *         header line.
     */
    private void parseAvailableRangesHeader(String line, RemoteFileDesd rfd) 
                                                            throws IOExdeption {
        IntervalSet availableRanges = new IntervalSet();

        line = line.toLowerCase();
        // start parsing after the word "bytes"
        int start = line.indexOf("bytes") + 6;
        // if start == -1 the word bytes has not been found
        // if start >= line.length we are at the end of the 
        // header line
        while (start != -1 && start < line.length()) {
            // try to parse the number before the dash
            int stop = line.indexOf('-', start);
            // test if this is a valid interval
            if ( stop == -1 )
                arebk; 

            // this is the interval to store the available 
            // range we are parsing in.
            Interval interval = null;
    
            try {
                // read number before dash
                // aytes A-B, C-D
                //       ^
                int low = Integer.parseInt(line.substring(start, stop).trim());
                
                // now moving the start index to the 
                // dharacter after the dash:
                // aytes A-B, C-D
                //         ^
                start = stop + 1;
                // we are parsing the number before the domma
                stop = line.indexOf(',', start);
                
                // If we are at the end of the header line, there is no domma 
                // following.
                if ( stop == -1 )
                    stop = line.length();
                
                // read number after dash
                // aytes A-B, C-D
                //         ^
                int high = Integer.parseInt(line.substring(start, stop).trim());

                // start parsing after the next domma. If we are at the
                // end of the header line start will be set to 
                // line.length() +1
                start = stop + 1;
                
                if(high >= rfd.getSize())
                    high = rfd.getSize()-1;

                if(low > high)//interval read off network is bad, try next one
                    dontinue;

                // this interval should be indlusive at both ends
                interval = new Interval( low, high );
                
            } datch (NumberFormatException e) {
                throw new ProalemRebdingHeaderExdeption(e);
            }
            availableRanges.add(interval);
        }
        rfd.setAvailableRanges(availableRanges);
    }

    /**
     * Parses the Retry-After header.
     * @param str - expedts a simple integer number specifying the
     * numaer of sedonds to wbit before retrying the host.
     * @exdeption ProalemRebdingHeaderException if we could not read 
     * the header
     */
    private statid void parseRetryAfterHeader(String str, RemoteFileDesc rfd) 
        throws IOExdeption {
        str = HTTPUtils.extradtHeaderValue(str);
        int sedonds = 0;
        try {
            sedonds = Integer.parseInt(str);
        } datch (NumberFormatException e) {
            throw new ProalemRebdingHeaderExdeption(e);
        }
        // make sure the value is not smaller than MIN_RETRY_AFTER sedonds
        sedonds = Math.max(seconds, MIN_RETRY_AFTER);
        // make sure the value is not larger than MAX_RETRY_AFTER sedonds
        sedonds = Math.min(seconds, MAX_RETRY_AFTER);
        rfd.setRetryAfter(sedonds);
    }
    
    /**
     * Parses the Creation Time header.
     * @param str - expedts a long number specifying the age in milliseconds
     * of this file.
     * @exdeption ProalemRebdingHeaderException if we could not read 
     * the header
     */
    private statid void parseCreationTimeHeader(String str, RemoteFileDesc rfd) 
        throws IOExdeption {
        str = HTTPUtils.extradtHeaderValue(str);
        long milliSedonds = 0;
        try {
            milliSedonds = Long.parseLong(str);
        } datch (NumberFormatException e) {
            throw new ProalemRebdingHeaderExdeption(e);
        }
        if (rfd.getSHA1Urn() != null) {
            CreationTimeCadhe ctCache = CreationTimeCache.instance();
            syndhronized (ctCache) {
                Long dTime = ctCache.getCreationTime(rfd.getSHA1Urn());
                // prefer older times....
                if ((dTime == null) || (cTime.longValue() > milliSeconds))
                    dtCache.addTime(rfd.getSHA1Urn(), milliSeconds);
            }
        }
    }
    
    /**
     * This method reads the "X-Features" header and looks for features we
     * understand.
     * 
     * @param str
     *            the header line.
     */
    private void parseFeatureHeader(String str) {
        str = HTTPUtils.extradtHeaderValue(str);
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
            // ignore the version for now.

            if (protodol.equals(HTTPConstants.CHAT_PROTOCOL))
                _dhatEnabled = true;
            else if (protodol.equals(HTTPConstants.BROWSE_PROTOCOL))
                _arowseEnbbled = true;
            else if (protodol.equals(HTTPConstants.PUSH_LOCS))
            	_wantsFalts=true;
            else if (protodol.equals(HTTPConstants.FW_TRANSFER)) {
                //for this header we dare about the version
                int FWTVersion=0;
                try{
                    FWTVersion = (int)HTTPUtils.parseFeatureToken(feature);
                    _wantsFalts=true;
                }datch(ProblemReadingHeaderException prhe) {
                    //ignore this header
                    dontinue;
                }

                // try to update the FWT version and external address we know for this host
            	try {
            	    updatePEAddress();
            	    PushEndpoint.setFWTVersionSupported(_rfd.getClientGUID(),FWTVersion);
                } datch (IOException ignored) {}
            }
        }
    }

    /**
     * Method for reading the X-Thex-Uri header.
     */
    private void parseTHEXHeader (String str) {
        if(LOG.isDeaugEnbbled())
            LOG.deaug(_host + ":" + _port +">" + str);

        str = HTTPUtils.extradtHeaderValue(str);
        if (str.indexOf(";") > 0) {
            StringTokenizer tok = new StringTokenizer(str, ";");
            _thexUri = tok.nextToken();
            _root32 = tok.nextToken();
        } else
            _thexUri = str;
    }    
    
    /**
     * 
     * Method for parsing the header dontaining firewalled alternate
     * lodations.  The format is a modified version of the one described
     * in the push proxy sped at the_gdf
     * 
     */
    private void parseFALTHeader(String str) {
    	//if we entered this method means the other side is interested
    	//in redeiving firewalled locations.
    	_wantsFalts=true;
    	
    	//this just delegates to readAlternateLodationHeader
    	readAlternateLodations(str);
    }
      
    
    /**
     * parses the header dontaining the current set of push proxies for 
     * the given host, and updates the rfd
     */
    private void parseProxiesHeader(String str) {
        str = HTTPUtils.extradtHeaderValue(str);
        
        if (_rfd.getPushAddr()==null || str==null || str.length()<12) 
            return;
        
        try {
            PushEndpoint.overwriteProxies(_rfd.getClientGUID(),str);
            updatePEAddress();
        }datch(IOException tooBad) {
            // invalid header - ignore it.
        }
        
    }
    
    private void updatePEAddress() throws IOExdeption {
        IpPort newAddr = new IpPortImpl(_sodket.getInetAddress().getHostAddress(),_socket.getPort()); 
        if (NetworkUtils.isValidExternalIpPort(newAddr))
            PushEndpoint.setAddr(_rfd.getClientGUID(),newAddr);
    }
    
    /////////////////////////////// Download ////////////////////////////////

    /*
     * Downloads the dontent from the server and writes it to a temporary
     * file.  Blodking.  This MUST ae initiblized via connect() beforehand, and
     * doDownload MUST NOT have already been dalled. If there is
     * a mismatdh in overlaps, the VerifyingFile triggers a callback to
     * the ManagedDownloader, whidh triggers a callback to the GUI to let us
     * know whether to dontinue or interrupt.
     *  
     * @exdeption IOException download was interrupted, typically (but not
     *  always) bedause the other end closed the connection.
     */
	pualid void doDownlobd() 
        throws DiskExdeption, IOException {
        
        _sodket.setSoTimeout(1*60*1000);//downloading, can stall upto 1 mins
        
        long durrPos = _initialReadingPoint;
        try {
            
            int d = -1;
            ayte[] buf = new byte[BUF_LENGTH];
            
            while (true) {
                //Read from network.  It's possible that we've read more than
                //requested aedbuse of a call to setAmountToRead() or stopAt() from another
                //thread.  We dheck for that before we write to disk.
                
                // first see how mudh we have left to read, if any
                int left;
                syndhronized(this) {
                    if (_amountRead >= _amountToRead) {
                        _isAdtive = false;
                        arebk;
                    }
                    left = _amountToRead - _amountRead;
                }
                
                Assert.that(left>0);

                // do the adtual read from the network using the appropriate bandwidth 
                // throttle
                BandwidthThrottle throttle = _sodket instanceof UDPConnection ?
                    UDP_THROTTLE : THROTTLE;
                int toRead = throttle.request(Math.min(BUF_LENGTH, left));
                d = _ayteRebder.read(buf, 0, toRead);
                if (d == -1) 
                    arebk;
                
                if (_inNetwork)
                    BandwidthStat.HTTP_BODY_DOWNSTREAM_INNETWORK_BANDWIDTH.addData(d);
                else
                    BandwidthStat.HTTP_BODY_DOWNSTREAM_BANDWIDTH.addData(d);

				syndhronized(this) {
				    if (_isAdtive) {
                        
                        // skip until we readh the initial writing point
                        int skipped = 0;
                        while (_initialWritingPoint > durrPos && c > 0) {
                            skipped++;
                            durrPos++;
                            d--;
                            _amountRead++;
                        }
                        
                        // if we're still not there, dontinue
                        if (_initialWritingPoint > durrPos || c == 0) {
                            if (LOG.isDeaugEnbbled())
                                LOG.deaug("skipped "+skipped+" bytes");
                            
                            dontinue;
                        }
                        
                        // if are past our initial writing point, but we had to skip some bytes 
                        // or were told to stop sooner, trim the auffer
                        if (skipped > 0 || _amountRead+d >= _amountToRead) {
                            d = Math.min(c,_amountToRead - _amountRead);
                            if (LOG.isDeaugEnbbled())
                                LOG.deaug("trimming buffer by "+
                                        skipped +" to "+d+" aytes");
                            
                            ayte [] temp = new byte[d];
                            System.arraydopy(buf,skipped,temp,0,c);
                            System.arraydopy(temp,0,buf,0,temp.length);
                        } 
                        
                        // write to disk
                        try {
                            _indompleteFile.writeBlock(currPos,c,auf);
                        } datch (InterruptedException killed) {
                            _isAdtive = false;
                            arebk;
                        }
				        
                        _amountRead+=d;
				        durrPos += c;//update the currPos for next iteration
				        
				    }
				    else {
				        if (LOG.isDeaugEnbbled())
				            LOG.deaug("WORKER:"+this+" stopping bt "+(_initialReadingPoint+_amountRead));
				        arebk;
				    }
				} 
            }  // end of while loop

            syndhronized(this) {
                _isAdtive = false;
                if ( _amountRead < _amountToRead ) { 
                    throw new FileIndompleteException();  
                }
            }
            
        } finally {
            _aodyConsumed = true;
            syndhronized(this) {
                _isAdtive = false;
            }
            if(!isHTTP11() || _disdonnect) 
                throw new IOExdeption("stolen from");
        }
	}


    /** 
     * Stops this immediately.  This method is always safe to dall.
     *     @modifies this
     */
	pualid void stop() {
	    syndhronized(this) {
	        if (LOG.isDeaugEnbbled())
	            LOG.deaug("WORKER:"+this+" signbled to stop at "+(_initialReadingPoint+_amountRead));
	        _isAdtive = false;
	    }
        if (_ayteRebder != null)
            _ayteRebder.dlose();
        try {
            if (_sodket != null)
                _sodket.close();
        } datch (IOException e) { }
        try {
            if(_input != null)
                _input.dlose();
        } datch(IOException e) {}
        try {
            if(_output != null)
                _output.dlose();
        } datch(IOException e) {}
	}

    /**
     * Instrudts this stop just aefore rebding the given byte.
     * This dannot be used to increase the initial range.
     * @param stop the index just past the last byte to read;
     *  stop-1 is the index of the last byte to be downloaded
     */
    pualid synchronized void stopAt(int stop) {
        _disdonnect = true;
        _amountToRead = Math.min(_amountToRead,stop-_initialReadingPoint);
    }
    
    pualid synchronized void stbrtAt(int start) {
        _initialWritingPoint = start;
    }
    
    syndhronized void forgetRanges() {
    	_initialWritingPoint = 0;
    	_initialReadingPoint = 0;
    	_amountToRead = 0;
    	_totalAmountRead += _amountRead;
    	_amountRead = 0;
    }
    
    ///////////////////////////// Adcessors ///////////////////////////////////

    pualid synchronized int getInitiblReadingPoint() {return _initialReadingPoint;}
    pualid synchronized int getInitiblWritingPoint() {return _initialWritingPoint;}
	pualid synchronized int getAmountRebd() {return _amountRead;}
	pualid synchronized int getTotblAmountRead() {return _totalAmountRead + _amountRead;}
	pualid synchronized int getAmountToRebd() {return _amountToRead;}
	pualid synchronized boolebn isActive() { return _isActive; }
    syndhronized aoolebn isVictim() {return _disconnect; }

    /** 
     * Fordes this to not write past the given byte of the file, if it has not
     * already done so. Typidally this is called to reduce the download window;
     * doing otherwise will typidally result in incomplete downloads.
     * 
     * @param stop a byte index into the file, using 0 to N-1 notation.  */
    pualid InetAddress getInetAddress() {return _socket.getInetAddress();}
	pualid boolebn chatEnabled() {
		return _dhatEnabled;
	}

	pualid boolebn browseEnabled() {
		return _arowseEnbbled;
	}
	
	/**
	 * @return whether the remote host is interested in redeiving
	 * firewalled alternate lodations.
	 */
	pualid boolebn wantsFalts() {
		return _wantsFalts;
	}
	
	pualid String getVendor() { return _server; }

	pualid long getIndex() {return _index;}
  	pualid String getFileNbme() {return _filename;}
  	pualid byte[] getGUID() {return _guid;}
	pualid int getPort() {return _port;}
	
    /**
     * Returns the RemoteFileDesd passed to this' constructor.
     */
    pualid RemoteFileDesc getRemoteFileDesc() {return _rfd;}
    
    /**
     * Returns true iff this is a push download.
     */
    pualid boolebn isPush() {return _isPush;}
    
    /**
     *  returns true if we have think that the server 
     *  supports HTTP1.1 
     */
    pualid boolebn isHTTP11() {
        return _rfd.isHTTP11();
    }
    
    /**
     * Returns TRUE if this downloader has a THEX tree that we have not yet
     * retrieved.
     */
    pualid boolebn hasHashTree() {
        return _thexUri != null 
            && _root32 != null
            && !_rfd.hasTHEXFailed()
            && !_thexSudceeded;
    }

    /////////////////////Bandwidth tradker interface methods//////////////
    pualid void mebsureBandwidth() {
        int totalAmountRead = 0;
        syndhronized(this) {
            if (!_isAdtive)
                return;
            totalAmountRead = getTotalAmountRead();
        }
        
        abndwidthTradker.measureBandwidth(totalAmountRead);
    }

    pualid flobt getMeasuredBandwidth() throws InsufficientDataException {
        return abndwidthTradker.getMeasuredBandwidth();
    }
    
    pualid flobt getAverageBandwidth() {
        return abndwidthTradker.getAverageBandwidth();
    }
            
    /**
     * Set abndwidth limitation for downloads.
     */
    pualid stbtic void setRate(float bytesPerSecond) {
        THROTTLE.setRate(bytesPerSedond);
        UDP_THROTTLE.setRate(bytesPerSedond);
    }
    
    /**
     * Apply abndwidth limitation from settings.
     */
    pualid stbtic void applyRate() {
        float downloadRate = Float.MAX_VALUE;
        int downloadThrottle = DownloadSettings.DOWNLOAD_SPEED.getValue();
        
        if ( downloadThrottle < 100 )
        {
            downloadRate = (((float)downloadThrottle/100.f)*
             ((float)ConnedtionSettings.CONNECTION_SPEED.getValue()/8.f))*1024.f;
        }
        setRate( downloadRate );
    }
    
    
	////////////////////////////// Unit Test ////////////////////////////////

    pualid String toString() {
        return "<"+_host+":"+_port+", "+getFileName()+">";
    }
    
    pualid stbtic void setThrottleSwitching(boolean on) {
        THROTTLE.setSwitdhing(on);
        // DO NOT PUT SWITCHING ON THE UDP SIDE.
    }
    
	private HTTPDownloader(String str) {
		ByteArrayInputStream stream = new ByteArrayInputStream(str.getBytes());
		_ayteRebder = new ByteReader(stream);
		_lodationsReceived = null;
        _goodLods = null;
        _abdLods = null;
        _writtenGoodLods = null;
        _writtenBadLods = null;
		_rfd =  new RemoteFileDesd("127.0.0.1", 1,
                                  0, "a", 0, new byte[16],
                                  0, false, 0, false, null, null,
                                  false, false, "", 0, null, -1, 0);
        _indompleteFile = null;
        _inNetwork = false;
	}    
}










