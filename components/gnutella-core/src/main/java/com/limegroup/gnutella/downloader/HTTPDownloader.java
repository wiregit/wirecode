package com.limegroup.gnutella.downloader;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.AssertFailure;
import com.limegroup.gnutella.BandwidthTracker;
import com.limegroup.gnutella.BandwidthTrackerImpl;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.CreationTimeCache;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.PushEndpointForSelf;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.DirectAltLoc;
import com.limegroup.gnutella.altlocs.PushAltLoc;
import com.limegroup.gnutella.http.ConstantHTTPHeaderValue;
import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HTTPHeaderValue;
import com.limegroup.gnutella.http.HTTPHeaderValueCollection;
import com.limegroup.gnutella.http.HTTPUtils;
import com.limegroup.gnutella.http.ProblemReadingHeaderException;
import com.limegroup.gnutella.http.SimpleHTTPHeaderValue;
import com.limegroup.gnutella.http.SimpleReadHeaderState;
import com.limegroup.gnutella.http.SimpleWriteHeaderState;
import com.limegroup.gnutella.io.IOState;
import com.limegroup.gnutella.io.IOStateMachine;
import com.limegroup.gnutella.io.IOStateObserver;
import com.limegroup.gnutella.io.InterestReadChannel;
import com.limegroup.gnutella.io.NBThrottle;
import com.limegroup.gnutella.io.NIODispatcher;
import com.limegroup.gnutella.io.NIOMultiplexor;
import com.limegroup.gnutella.io.ReadSkipState;
import com.limegroup.gnutella.io.ReadState;
import com.limegroup.gnutella.io.Throttle;
import com.limegroup.gnutella.io.ThrottleReader;
import com.limegroup.gnutella.settings.ChatSettings;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.DownloadSettings;
import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.gnutella.statistics.BandwidthStat;
import com.limegroup.gnutella.statistics.DownloadStat;
import com.limegroup.gnutella.statistics.NumericalDownloadStat;
import com.limegroup.gnutella.tigertree.HashTree;
import com.limegroup.gnutella.tigertree.ThexReader;
import com.limegroup.gnutella.util.IOUtils;
import com.limegroup.gnutella.util.IntervalSet;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.IpPortImpl;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.Sockets;

/**
 * Downloads a file over an HTTP connection.  This class is as simple as
 * possible.  It does not deal with retries, prioritizing hosts, etc.  Nor does
 * it check whether a file already exists; it just writes over anything on
 * disk.<p>
 *
 * It is necessary to explicitly initialize an HTTPDownloader with the
 * connectTCP(..) followed by a connectHTTP(..) method.  (Hence HTTPDownloader
 * behaves much like Connection.)  Typical use is as follows:
 *
 * <pre>
 * HTTPDownloader dl=new HTTPDownloader(host, port);
 * dl.connectTCP(timeout);
 * dl.connectHTTP(startByte, stopByte);
 * dl.doDownload();
 * </pre> 
 * LOCKING: _writtenGoodLocs and _goodLocs are both synchronized on _goodLocs
 * LOCKING: _writtenBadLocs and _badLocs are both synchronized on _badLocs
 */

public class HTTPDownloader implements BandwidthTracker {
    
    private static final Log LOG = LogFactory.getLog(HTTPDownloader.class);
    
    /**
     * The length of the buffer used in downloading.
     */
    public static final int BUF_LENGTH=2048;
    
    /**
     * The smallest possible time in seconds to wait before retrying a busy
     * host. 
     */
    private static final int MIN_RETRY_AFTER = 60; // 60 seconds

    /**
     * The maximum possible time in seconds to wait before retrying a busy
     * host. 
     */
    private static final int MAX_RETRY_AFTER = 60 * 60; // 1 hour
    
    /**
     * The smallest possible file to be shared with partial file sharing.
     * Non final for testing purposes.
     */
    static int MIN_PARTIAL_FILE_BYTES = 1*1024*1024; // 1MB
    
    /** The throttle. */
    private static final Throttle THROTTLE = new NBThrottle(false, Float.MAX_VALUE);

    private RemoteFileDesc _rfd;
	private long _index;
	private String _filename; 
	private byte[] _guid;

	
    /**
     * The total amount we've downloaded, including all previous 
     * HTTP connections
     * LOCKING: this
     */
    private int _totalAmountRead;
    
    /**
     *  The amount we've downloaded.
     * LOCKING: this 
     */
	private int _amountRead;
	
    /** 
     * The amount we'll have downloaded if the download completes properly. 
     *  Note that the amount still left to download is 
     *  _amountToRead - _amountRead.
     * LOCKING: this 
     */
	private int _amountToRead;
    
    /**
     * Whether to disconnect after reading the amount we have wanted to read
     */
    private volatile boolean _disconnect;
    
    /** 
     *  The index to start reading from the server 
     * LOCKING: this 
     */
	private int _initialReadingPoint;
	
    /** 
     *  The index to actually start writing to the file.
     *  LOCKING:this
     */
    private int _initialWritingPoint;
    
	/**
	 * The content-length of the output, useful only for when we
	 * want to read & discard the body of the HTTP message.
	 */
	private long _contentLength;
	
	/**
	 * Whether or not the body has been consumed.
	 */
	private volatile boolean _bodyConsumed = true;

	private Socket _socket;  //initialized in HTTPDownloader(Socket) or connect
    private IOStateMachine _stateMachine;
    private Observer observerHandler;
    private SimpleReadHeaderState _headerReader;
    private boolean _requestingThex;
    private ThexReader _thexReader;
    private final VerifyingFile _incompleteFile;
    
	/**
	 * The new alternate locations we've received for this file.
	 */
	private Set<RemoteFileDesc> _locationsReceived;

    /**
     *  The good locations to send the uploaders as in the alts list
     */
    private Set<DirectAltLoc> _goodLocs;
    
    /**
     * The firewalled locations to send to uploaders that are interested
     */
    private Set<PushAltLoc> _goodPushLocs;
    
    /**
     * The bad firewalled locations to send to uploaders that are interested
     */
    private Set<PushAltLoc> _badPushLocs;
    
    /** 
     * The list to send in the n-alts list
     */
    private Set<DirectAltLoc> _badLocs;
    
    /**
     * The list of already written alts, used to stop duplicates
     */
    private Set<DirectAltLoc> _writtenGoodLocs;
    
    /**
     * The list of already written n-alts, used to stop duplicates
     */ 
    private Set<DirectAltLoc> _writtenBadLocs;
    
    /**
     * The list of already written push alts, used to stop duplicates
     */
    private Set<PushAltLoc> _writtenPushLocs;
    
    /**
     * The list of already written bad push alts, used to stop duplicates
     */
    private Set<PushAltLoc> _writtenBadPushLocs;

    
	private int _port;
	private String _host;
	
	private boolean _chatEnabled = false; // for now
    private boolean _browseEnabled = false; // also for now
    private String _server = "";
    
    private String _thexUri = null;
    private String _root32 = null;    
    /**
     * Whether or not the retrieval of THEX succeeded.
     * This is stored here, as opposed to the RemoteFileDesc,
     * because we may want to re-use the RemoteFileDesc to try
     * and get the THEX tree later on from this host, if
     * the first attempt failed from corruption.
     *
     * Failures are stored in the RemoteFileDesc because
     * if it failed we never want to try it again, ever.
     */
    private boolean _thexSucceeded = false;

    /** For implementing the BandwidthTracker interface. */
    private BandwidthTrackerImpl bandwidthTracker=new BandwidthTrackerImpl();
    
    /**
     * Whether or not this HTTPDownloader is currently attempting to read
     * information from the network.
     */
    private boolean _isActive = false;


    private Interval _requestedInterval = null;
    
    /**
     * whether the other side wants to receive firewalled altlocs
     */
    private boolean _wantsFalts = false;
    
    /** Whether to count the bandwidth used by this downloader */
    private final boolean _inNetwork;
    

    /**
     * Creates an uninitialized client-side normal download.  Call 
     * connectTCP and connectHTTP() on this before any other methods.  
     * Non-blocking.
     *
     * @param rfd complete information for the file to download, including
     *  host address and port
     * @param incompleteFile the temp file to use while downloading, which need
     *  not exist.
     * @param start the place to start reading from the network and writing to 
     *  the file
     * @param stop the last byte to read+1
     */
	public HTTPDownloader(RemoteFileDesc rfd, VerifyingFile incompleteFile, boolean inNetwork) {
        //Dirty secret: this is implemented with the push constructor!
        this(null, rfd, incompleteFile, inNetwork);
	}	

	/**
     * Creates an uninitialized server-side push download. connectTCP() and 
     * connectHTTP() on this before any other methods.  Non-blocking.
     * 
     * @param socket the socket to download from.  The "GIV..." line must
     *  have been read from socket.  HTTP headers may not have been read or 
     *  buffered -- this can be <tt>null</tt>
     * @param rfd complete information for the file to download, including
     *  host address and port
     * @param incompleteFile the temp file to use while downloading, which need
     *  not exist.
     */
	public HTTPDownloader(Socket socket, RemoteFileDesc rfd, VerifyingFile incompleteFile, boolean inNetwork) {
        if(rfd == null)
            throw new NullPointerException("null rfd");
        
        _rfd=rfd;
        _socket=socket;
        _incompleteFile=incompleteFile;
		_filename = rfd.getFileName();
		_index = rfd.getIndex();
		_guid = rfd.getClientGUID();
		_amountToRead = 0;
		_port = rfd.getPort();
		_host = rfd.getHost();
		_chatEnabled = rfd.chatEnabled();
        _browseEnabled = rfd.browseHostEnabled();
        _locationsReceived = new HashSet<RemoteFileDesc>();
        _goodLocs = new HashSet<DirectAltLoc>();
        _badLocs = new HashSet<DirectAltLoc>();
        _goodPushLocs = new HashSet<PushAltLoc>();
        _badPushLocs = new HashSet<PushAltLoc>();
        _writtenGoodLocs = new HashSet<DirectAltLoc>();
        _writtenBadLocs = new HashSet<DirectAltLoc>();
        _writtenPushLocs = new HashSet<PushAltLoc>();
        _writtenBadPushLocs = new HashSet<PushAltLoc>();
		_amountRead = 0;
		_totalAmountRead = 0;
        _inNetwork = inNetwork;
		applyRate();
    }

    ////////////////////////Alt Locs methods////////////////////////
    
    /**
     * Accessor for the alternate locations received from the server for 
     * this download attempt.  
     *
     * @return the <tt>AlternateLocationCollection</tt> containing the 
     *  received locations, can be <tt>null</tt> if we could not create
     *  a collection, or could be empty
     */
    Collection<RemoteFileDesc> getLocationsReceived() { 
	    return _locationsReceived;
    }
    
    void addSuccessfulAltLoc(AlternateLocation loc) {
    	if (loc instanceof DirectAltLoc) {
    		synchronized(_badLocs) {
    			//If we ever thought loc was bad, forget that we did, so that we can
    			//add it to the n-alts list again, if it fails -- remove from
    			//writtenBadlocs
    			_writtenBadLocs.remove(loc);           
    			_badLocs.remove(loc);
    		}
    		synchronized(_goodLocs) {
    			if(!_writtenGoodLocs.contains(loc)) //not written earlier
    				_goodLocs.add((DirectAltLoc)loc); //duplicates make no difference
    		}
    	} else if(loc instanceof PushAltLoc){
    		synchronized(_badPushLocs) {
    			//If we ever thought loc was bad, forget that we did, so that we can
    			//add it to the n-alts list again, if it fails -- remove from
    			//writtenBadlocs
    			_writtenBadPushLocs.remove(loc);           
    			_badPushLocs.remove(loc);
    		}
    		synchronized(_goodPushLocs) {
    			if(!_writtenPushLocs.contains(loc)) //not written earlier
    				_goodPushLocs.add((PushAltLoc)loc); //duplicates make no difference
    				
    		}
    	} else {
    	    throw new IllegalStateException("bad location of class: " + loc.getClass());
        }
    }
    
    void addFailedAltLoc(AlternateLocation loc) {
        //if we ever thought it was good, forget that we did, so we can write it
        //out as good again -- remove it from writtenGoodLocs if it was there
    	
    	if (loc instanceof DirectAltLoc){
    		synchronized(_goodLocs) {
    			_writtenGoodLocs.remove(loc);
    			_goodLocs.remove(loc);
    		}
        
    		synchronized(_badLocs) {
    			if(!_writtenBadLocs.contains(loc))//no need to repeat to uploader
    				_badLocs.add((DirectAltLoc)loc); //duplicates make no difference
    		}
    	} else if(loc instanceof PushAltLoc){
    		synchronized(_goodPushLocs) {
    			_writtenPushLocs.remove(loc);
    			_goodPushLocs.remove(loc);
    		}
        
    		synchronized(_badPushLocs) {
    			if(!_writtenBadPushLocs.contains(loc))//no need to repeat to uploader
    				_badPushLocs.add((PushAltLoc)loc); //duplicates make no difference
    		}
    	} else {
            throw new IllegalStateException("bad location of class: " + loc.getClass());
        }
    }
    
    ///////////////////////////////// Connection /////////////////////////////

    /** 
     * Initializes this by connecting to the remote host (in the case of a
     * normal client-side download). Blocks for up to timeout milliseconds 
     * trying to connect, unless timeout is zero, in which case there is 
     * no timeout.  This MUST be uninitialized, i.e., connectTCP may not be 
     * called more than once.
     * <p>
     * @param timeout the timeout to use for connecting, in milliseconds,
     *  or zero if no timeout
     * @exception IOException could not establish a TCP connection
     */
	public void connectTCP(int timeout) throws IOException {
        if (_socket == null) {
            long curTime = System.currentTimeMillis();
            _socket = Sockets.connect(_host, _port, timeout);
            NumericalDownloadStat.TCP_CONNECT_TIME.addData((int) (System.currentTimeMillis() - curTime));
        }
        
        _socket.setKeepAlive(true);
        observerHandler = new Observer();
        _stateMachine = new IOStateMachine(observerHandler, new LinkedList(), BUF_LENGTH);
        _stateMachine.setReadChannel(new ThrottleReader(THROTTLE));
        ((NIOMultiplexor)_socket).setReadObserver(_stateMachine);
        ((NIOMultiplexor)_socket).setWriteObserver(_stateMachine);
        
        // Note : once we have established the TCP connection with the host we
        // want to download from we set the soTimeout. Its reset in doDownload
        // Note2 : this may throw an IOException.
        _socket.setSoTimeout(Constants.TIMEOUT);
    }

    /**
     * Same as connectHTTP(start, stop, supportQueueing, -1)
     */
    public void connectHTTP(int start, int stop, boolean supportQueueing, IOStateObserver observer) {
        connectHTTP(start, stop, supportQueueing, -1, observer);
    }
    
    /** 
     * Sends a GET request using an already open socket, and reads all 
     * headers.  The actual ranges downloaded MAY NOT be the same
     * as the 'start' and 'stop' parameters, as HTTP allows the server
     * to respond with any satisfiable subrange of the request.
     *
     * Users of this class should examine getInitialReadingPoint()
     * and getAmountToRead() to determine what the effective start & stop
     * ranges are, and update external datastructures appropriately.
     *  int newStart = dloader.getInitialReadingPoint();
     *  int newStop = (dloader.getAmountToRead() - 1) + newStart; // INCLUSIVE
     * or
     *  int newStop = dloader.getAmountToRead() + newStart; // EXCLUSIVE
     *
     * <p>
     * @param start The byte at which the HTTPDownloader should begin
     * @param stop the index just past the last byte to read;
     *  stop-1 is the last byte the HTTPDownloader should download
     * <p>
     * @exception TryAgainLaterException the host is busy
     * @exception FileNotFoundException the host doesn't recognize the file
     * @exception NotSharingException the host isn't sharing files (BearShare)
     * @exception IOException miscellaneous  error 
     * @exception QueuedException uploader has queued us
     * @exception RangeNotAvailableException uploader has ranges 
     * other than requested
     * @exception ProblemReadingHeaderException could not parse headers
     * @exception UnknownCodeException unknown response code
     */
    public void connectHTTP(int start, int stop, boolean supportQueueing, int amountDownloaded, IOStateObserver observer) {
        if(start < 0)
            throw new IllegalArgumentException("invalid start: " + start);
        if(stop <= start)
            throw new IllegalArgumentException("stop(" + stop + ") <= start(" + start +")");

        synchronized(this) {
            _isActive = true;
            _amountToRead = stop-start;
            _amountRead = 0;
            _initialReadingPoint = start;
            _initialWritingPoint = start;
            _bodyConsumed = false;
            _contentLength = 0;
            _requestedInterval = new Interval(_initialReadingPoint, stop-1);
        }
		
        observerHandler.setDelegate(observer);
        
        Map<HTTPHeaderName, HTTPHeaderValue> headers = new LinkedHashMap<HTTPHeaderName, HTTPHeaderValue>();
        Set<HTTPHeaderValue> features = new HashSet<HTTPHeaderValue>();
        
        headers.put(HTTPHeaderName.HOST, new SimpleHTTPHeaderValue(_host + ":" + _port));
        headers.put(HTTPHeaderName.USER_AGENT, ConstantHTTPHeaderValue.USER_AGENT);

        if (supportQueueing) {
            headers.put(HTTPHeaderName.QUEUE, ConstantHTTPHeaderValue.QUEUE_VERSION);
            features.add(ConstantHTTPHeaderValue.QUEUE_FEATURE);
        }
        
        //if I'm not firewalled or I can do FWT, say that I want pushlocs.
        //if I am firewalled, send the version of the FWT protocol I support.
        // (which implies that I want only altlocs that support FWT)
        if (RouterService.acceptedIncomingConnection() || UDPService.instance().canDoFWT()) {
            features.add(ConstantHTTPHeaderValue.PUSH_LOCS_FEATURE);
            if (!RouterService.acceptedIncomingConnection())
                features.add(ConstantHTTPHeaderValue.FWT_PUSH_LOCS_FEATURE);
        }
        	

        // Add ourselves to the mesh if the partial file is valid
        //if I'm firewalled add myself only if the other guy wants falts
        if( isPartialFileValid() && 
        	 (RouterService.acceptedIncomingConnection() || _wantsFalts)) {
        		AlternateLocation me = AlternateLocation.create(_rfd.getSHA1Urn());
        		if (me != null)
        			addSuccessfulAltLoc(me);
        }

        URN sha1 = _rfd.getSHA1Urn();
		if ( sha1 != null )
            headers.put(HTTPHeaderName.GNUTELLA_CONTENT_URN, sha1);

        //We don't want to hold locks while doing network operations, so we use
        //this variable to clone _goodLocs and _badLocs and write to network
        //while iterating over the clone
        Set<AlternateLocation> writeClone = null;
        
        //write altLocs 
        synchronized(_goodLocs) {
            if(_goodLocs.size() > 0) {
                writeClone = new HashSet<AlternateLocation>();
                for(DirectAltLoc loc : _goodLocs) {
                    writeClone.add(loc);
                    _writtenGoodLocs.add(loc);
                }
                _goodLocs.clear();
            }
        }
        if(writeClone != null) //have something to write?
            headers.put(HTTPHeaderName.ALT_LOCATION, new HTTPHeaderValueCollection(writeClone));
        
        writeClone = null;
        //write-nalts        
        synchronized(_badLocs) {
            if(_badLocs.size() > 0) {
                writeClone = new HashSet<AlternateLocation>();
                for(DirectAltLoc loc : _badLocs) {
                    writeClone.add(loc);
                    _writtenBadLocs.add(loc);
                }
                _badLocs.clear();
            }
        }

        if(writeClone != null) //have something to write?
            headers.put(HTTPHeaderName.NALTS, new HTTPHeaderValueCollection(writeClone));
        
        // if the other side indicated they want firewalled altlocs, send some
        //
        // Note: we send both types of firewalled altlocs to the uploader since even if
        // it can't support FWT it can still spread them to other downloaders.
        //
        // Note2: we can't know whether the other side wants to receive pushlocs until
        // we read their headers. Therefore pushlocs will be sent from the second
        // http request on.
        
        if (_wantsFalts) {
        	writeClone = null;
        	synchronized(_goodPushLocs) {
        		if(_goodPushLocs.size() > 0) {
        			writeClone = new HashSet<AlternateLocation>();
                    for(PushAltLoc loc : _goodPushLocs) {
        				// we should not have empty proxies unless this is ourselves
        				if (loc.getPushAddress().getProxies().isEmpty()) {
        				    if (loc.getPushAddress() instanceof PushEndpointForSelf)
        				        continue;
        				    else
        				        Assert.that(false,"empty pushloc in downloader");
        				}
        				
        				writeClone.add(loc);
        				_writtenPushLocs.add(loc);
        			}
        			_goodPushLocs.clear();
        		}
        	}
        	if (writeClone!=null)
                headers.put(HTTPHeaderName.FALT_LOCATION, new HTTPHeaderValueCollection(writeClone));
        	
        	//do the same with bad push locs
        	writeClone = null;
        	synchronized(_badPushLocs) {
                if(_badPushLocs.size() > 0) {
                    writeClone = new HashSet<AlternateLocation>();
                    for(PushAltLoc loc : _badPushLocs) {
                        // no empty proxies allowed here
        				Assert.that(!loc.getPushAddress().getProxies().isEmpty());
        				
        				writeClone.add(loc);
                        _writtenBadPushLocs.add(loc);
                    }
                    _badPushLocs.clear();
                }
            }
        	
        	if (writeClone!=null) 
                headers.put(HTTPHeaderName.BFALT_LOCATION, new HTTPHeaderValueCollection(writeClone));
        }
        
        
        

        headers.put(HTTPHeaderName.RANGE, new SimpleHTTPHeaderValue("bytes=" + _initialReadingPoint + "-" + (stop-1)));
        
		if (RouterService.acceptedIncomingConnection() &&
           !NetworkUtils.isPrivateAddress(RouterService.getAddress())) {
            int port = RouterService.getPort();
            String host = NetworkUtils.ip2string(RouterService.getAddress());
            headers.put(HTTPHeaderName.NODE, new SimpleHTTPHeaderValue(host + ":" + port));
            features.add(ConstantHTTPHeaderValue.BROWSE_FEATURE);
            // Legacy chat header. Replaced by X-Features header / X-Node
            // header
            if (ChatSettings.CHAT_ENABLED.getValue()) {
                headers.put(HTTPHeaderName.CHAT, new SimpleHTTPHeaderValue(host + ":" + port));
                features.add(ConstantHTTPHeaderValue.CHAT_FEATURE);
            }
        }	
		
		// Write X-Features header.
        if (features.size() > 0)
            headers.put(HTTPHeaderName.FEATURES, new HTTPHeaderValueCollection(features));
		
        // Write X-Downloaded header to inform uploader about
        // how many bytes already transferred for this file
        if ( amountDownloaded > 0 ) {
            headers.put(HTTPHeaderName.DOWNLOADED, new SimpleHTTPHeaderValue("" + amountDownloaded));
        }
		
        SimpleWriteHeaderState writer = new SimpleWriteHeaderState(
                "GET " + _rfd.getUrl().getFile() + " HTTP/1.1",
                headers,
                _inNetwork ? BandwidthStat.HTTP_HEADER_UPSTREAM_INNETWORK_BANDWIDTH :
                             BandwidthStat.HTTP_HEADER_UPSTREAM_BANDWIDTH);
        SimpleReadHeaderState reader = new SimpleReadHeaderState(
                _inNetwork ? BandwidthStat.HTTP_HEADER_DOWNSTREAM_INNETWORK_BANDWIDTH :
                             BandwidthStat.HTTP_HEADER_DOWNSTREAM_BANDWIDTH,
                             DownloadSettings.MAX_HEADERS.getValue(),
                             DownloadSettings.MAX_HEADER_SIZE.getValue());
        
        _stateMachine.addStates(new IOState[] { writer, reader } );
        _headerReader = reader;
	}
	
	/**
	 * Consumes the body of the HTTP message that was previously exchanged,
	 * if necessary.
	 */
    void consumeBody(IOStateObserver observer) {
        if (!_bodyConsumed) {
            if(_contentLength != -1)
                consumeBody(_contentLength, observer);
            else
                observer.handleIOException(new IOException("no content length"));
        } else {
            observer.handleStatesFinished();
        }
        _bodyConsumed = true;
    }
    
    /** Determines if the body needs to be consumed. */
    boolean isBodyConsumed() {
        return _bodyConsumed;
    }
	
    /**
     * Returns the ConnectionStatus from the request.
     * Can be one of:
     *   Connected -- means to immediately assignAndRequest.
     *   Queued -- means to sleep while queued.
     *   ThexResponse -- means the thex tree was received.
     */
    public void requestHashTree(URN sha1, IOStateObserver observer) {
        if (LOG.isDebugEnabled())
            LOG.debug("requesting HashTree for " + _thexUri + 
                      " from " +_host + ":" + _port);
        
        observerHandler.setDelegate(observer);
        
        Map<HTTPHeaderName, HTTPHeaderValue> headers = new LinkedHashMap<HTTPHeaderName, HTTPHeaderValue>();
        headers.put(HTTPHeaderName.HOST, new SimpleHTTPHeaderValue(_host + ":" + _port));
        headers.put(HTTPHeaderName.USER_AGENT, ConstantHTTPHeaderValue.USER_AGENT);
        
        SimpleWriteHeaderState writer = new SimpleWriteHeaderState(
                "GET " + _thexUri + " HTTP/1.1",
                headers,
                BandwidthStat.GNUTELLA_UPSTREAM_BANDWIDTH);
        SimpleReadHeaderState reader = new SimpleReadHeaderState(
                BandwidthStat.GNUTELLA_DOWNSTREAM_BANDWIDTH,
                DownloadSettings.MAX_HEADERS.getValue(),
                DownloadSettings.MAX_HEADER_SIZE.getValue());
        
        _headerReader = reader;
        _requestingThex = true;
        _bodyConsumed = false;
        _stateMachine.addStates(new IOState[] { writer, reader });
    }
    
    boolean isRequestingThex() {
        return _requestingThex;
    }
    
    public ConnectionStatus parseThexResponseHeaders() {
        _requestingThex = false;
        try {
            int code = parseHTTPCode(_headerReader.getConnectLine(), _rfd);
            boolean failed = false;
            if(code < 200 || code >= 300)
                failed = true;
            return parseThexHeaders(code, failed);
        } catch(IOException failed) {
            return ConnectionStatus.getNoFile();
        }
        
    }
    
    public void downloadThexBody(URN sha1, IOStateObserver observer) {
        _thexReader = HashTree.createHashTreeReader(sha1.httpStringValue(), _root32, _rfd.getFileSize());
        observerHandler.setDelegate(observer);
        _stateMachine.addState(_thexReader);
    }
    
    public HashTree getHashTree() {
        //LOG.debug("Retrieving hash tree, expected length: " + _contentLength + ", read: " + _thexReader.getAmountProcessed());
        _contentLength -= _thexReader.getAmountProcessed();
        if(_contentLength == 0)
            _bodyConsumed = true;
        HashTree tree =  null;
        try {
            tree = _thexReader.getHashTree();
        } catch(IOException iox) {
            LOG.warn("Failed to create tree", iox);
        }
        if(tree == null)
            _rfd.setTHEXFailed();
        else
            _thexSucceeded = true;
        
        return tree;
    }
    
    /**
     * Parses the headers of a thex response.
     * Ensures a content-length is included,
     * and if queued returns a queued response.
     */
    private ConnectionStatus parseThexHeaders(int code, boolean failed) {
        if(LOG.isDebugEnabled())
            LOG.debug(_rfd + " consuming headers");
        
        _contentLength = -1;
        for(Map.Entry<String, String> entry : _headerReader.getHeaders().entrySet()) {
            String header = entry.getKey();
            if(HTTPHeaderName.CONTENT_LENGTH.is(header))
                _contentLength = readContentLength(entry.getValue());
            if(code == 503 && HTTPHeaderName.QUEUE.is(header)) {
                String value = entry.getValue();
                int queueInfo[] = { -1, -1, -1 };
                parseQueueHeaders(value, queueInfo);
                int min = queueInfo[0];
                int max = queueInfo[1];
                int pos = queueInfo[2];
                if(min != -1 && max != -1 && pos != -1) {
                    _bodyConsumed = true;
                    return ConnectionStatus.getQueued(pos, min);
                }
            }
        }
        
        if(_contentLength == 0)
            _bodyConsumed = true;
        
        if(failed || _contentLength == -1)
            return ConnectionStatus.getNoFile();
        else
            return ConnectionStatus.getConnected();
    }
    
    /**
     * Consumes the body portion of an HTTP Message.
     */
    private void consumeBody(long contentLength, IOStateObserver observer) {
        if(LOG.isTraceEnabled())
            LOG.trace("enter consumeBody(" + contentLength + ")");

        if(contentLength < 0)
            observer.handleIOException(new IOException("unknown content-length, can't consume"));
            
        observerHandler.setDelegate(observer);
        _stateMachine.addState(new ReadSkipState(contentLength));
    }

    /*
     * Reads the headers from this, setting _initialReadingPoint and
     * _amountToRead.  Throws any of the exceptions listed in connect().  
     */
	public void parseHeaders() throws IOException {
        String connectLine = _headerReader.getConnectLine();
        Map<String, String> headers = _headerReader.getHeaders();
        
        if (connectLine == null || connectLine.equals(""))
            throw new IOException();
        
        int code = parseHTTPCode(connectLine, _rfd);
        _contentLength = -1;
        //Note: According to the specification there are 5 headers, LimeWire
        //ignores 2 of them - queue length, and maxUploadSlots.
        int[] refQueueInfo = {-1,-1,-1};
        //Now read each header...
        for(Map.Entry<String, String> entry : headers.entrySet()) {
            String header = entry.getKey();
            String value = entry.getValue();
			
            //For "Content-Range" headers, we store what the remote side is
            //going to give us.  Users should examine the interval and
            //update external structures appropriately.
            if (HTTPHeaderName.CONTENT_RANGE.is(header))
                validateContentRange(parseContentRange(value));
            else if(HTTPHeaderName.CONTENT_LENGTH.is(header))
                _contentLength = readContentLength(value);
            else if(HTTPHeaderName.CONTENT_URN.is(header))
				checkContentUrnHeader(value, _rfd.getSHA1Urn());
            else if(HTTPHeaderName.GNUTELLA_CONTENT_URN.is(header))
				checkContentUrnHeader(value, _rfd.getSHA1Urn());
			else if(HTTPHeaderName.ALT_LOCATION.is(header))
                readAlternateLocations(value);
            else if(HTTPHeaderName.QUEUE.is(header)) 
                parseQueueHeaders(value, refQueueInfo);
            else if (HTTPHeaderName.SERVER.is(header)) 
                _server = value;
            else if (HTTPHeaderName.AVAILABLE_RANGES.is(header))
                parseAvailableRangesHeader(value, _rfd);
            else if (HTTPHeaderName.RETRY_AFTER.is(header)) 
                parseRetryAfterHeader(value, _rfd);
            else if (HTTPHeaderName.CREATION_TIME.is(header))
                parseCreationTimeHeader(value, _rfd);
            else if (HTTPHeaderName.FEATURES.is(header))
            	parseFeatureHeader(value);
            else if (HTTPHeaderName.THEX_URI.is(header))
                parseTHEXHeader(value);
            else if (HTTPHeaderName.FALT_LOCATION.is(header))
            	parseFALTHeader(value);
            else if (HTTPHeaderName.PROXIES.is(header))
                parseProxiesHeader(value);
            
        }

		//Accept any 2xx's, but reject other codes.
		if ( (code < 200) || (code >= 300) ) {
			if (code == 404) // file not found
				throw new 
				    com.limegroup.gnutella.downloader.FileNotFoundException();
			else if (code == 410) // not shared.
				throw new NotSharingException();
            else if (code == 416) {//requested range not available
                //See if the uploader is up to mischief
                if(_rfd.isPartialSource()) {
                    Iterator iter = _rfd.getAvailableRanges().getAllIntervals();
                    while(iter.hasNext()) {
                        Interval next = (Interval)iter.next();
                        if(_requestedInterval.isSubrange(next))
                            throw new 
                            ProblemReadingHeaderException("Bad ranges sent");
                    }
                }
                else {//Uploader sent 416 and no ranges
                    throw new ProblemReadingHeaderException("no ranges sent");
                }
                //OK. The uploader is not messing with us.
                throw new RangeNotAvailableException();
            }
			else if (code == 503) { // busy or queued, or range not available.
                int min = refQueueInfo[0];
                int max = refQueueInfo[1];
                int pos = refQueueInfo[2];
                if(min != -1 && max != -1 && pos != -1) {
                    _bodyConsumed = true;
                    throw new QueuedException(min,max,pos);
                }
                    
                // per the PFSP spec, a 503 should be returned. But if the
                // uploader returns a "Avaliable-Ranges" header regardless of
                // whether it is really busy or just does not have the requested
                // range, we cannot really distingush between the two cases on
                // the client side.
                
                //For the most part clients send 416 when they have other ranges
                //that may match the clients need. From LimeWire 4.0.6 onwards
                //LimeWire will treate 503s to mean either busy or queued BUT
                //NOT partial range available.

                //if( _rfd.isPartialSource() )
                //throw new RangeNotAvailableException();
                    
                //no QueuedException or RangeNotAvailableException? not queued.
                //throw a generic busy exception.
				throw new TryAgainLaterException();

                // a general catch for 4xx and 5xx's
                // should maybe be a different exception?
                // else if ( (code >= 400) && (code < 600) ) 
            }
			else // unknown or unimportant
				throw new UnknownCodeException(code);			
		}        
    }

	/**
     * Does nothing except for throwing an exception if the
     * X-Gnutella-Content-URN header does not match
     * 
     * @param str
     *            the header <tt>String</tt>
     * @param sha1
     *            the <tt>URN</tt> we expect
     * @throws ContentUrnMismatchException
     */
    private void checkContentUrnHeader(String value, URN sha1)
        throws ContentUrnMismatchException {
        if (_root32 == null && value.indexOf("urn:bitprint:") > -1) { 
            // If the root32 was not in the X-Thex-URI header
            // (the spec requires it be there), then steal it from
            // the content-urn if it was a bitprint.
            _root32 = value.substring(value.lastIndexOf(".")+1).trim();
        }

        if(sha1 == null)
            return;
        
        
        URN contentUrn = null;
        try {
            contentUrn = URN.createSHA1Urn(value);
        } catch (IOException ioe) {
            // could be an URN type we don't know. So ignore all
            return;
        }
        if (!sha1.equals(contentUrn))
            throw new ContentUrnMismatchException();
        // else do nothing at all.
    }
	
	/**
	 * Reads alternate location header.  The header can contain only one
	 * alternate location, or it can contain many in the same header.
	 * This method adds them all to the <tt>FileDesc</tt> for this
	 * uploader.  This will not allow more than 20 alternate locations
	 * for a single file.
	 * 
	 * Since uploaders send only good alternate locations, we add merge
	 * proxies to the existing sets.
	 *
	 * @param altHeader the full alternate locations header
	 */
	private void readAlternateLocations(final String altStr) {
		if(altStr == null)
		    return;

        final URN sha1 = _rfd.getSHA1Urn();
        if(sha1 == null)
            return;
            
		StringTokenizer st = new StringTokenizer(altStr, ",");
		while(st.hasMoreTokens()) {
			try {
				AlternateLocation al =
					AlternateLocation.create(st.nextToken().trim(), sha1);
                Assert.that(al.getSHA1Urn().equals(sha1));
                if (al.isMe())
                    continue;
                
                RemoteFileDesc rfd = al.createRemoteFileDesc(_rfd.getSize());
                
                if(_locationsReceived.add(rfd)) {
                    if (al instanceof DirectAltLoc)
                        DownloadStat.ALTERNATE_COLLECTED.incrementStat();
                    else
                        DownloadStat.PUSH_ALTERNATE_COLLECTED.incrementStat();
                }
			} catch(IOException e) {
				// continue without adding it.
				continue;
			}
		}
	}
	
	/**
	 * Determines whether or not the partial file is valid for us
	 * to add ourselves to the mesh.
	 *
	 * Checks the following:
	 *  - RFD has a SHA1.
	 *  - We are allowing partial sharing
	 *  - We have successfully verified at least certain size of the file
	 *  - Our port and IP address are valid 
	 */
	private boolean isPartialFileValid() {
	    return _rfd.getSHA1Urn() != null && 
               _incompleteFile.getVerifiedBlockSize() > MIN_PARTIAL_FILE_BYTES &&
               UploadSettings.ALLOW_PARTIAL_SHARING.getValue() &&
               NetworkUtils.isValidPort(RouterService.getPort()) &&
               NetworkUtils.isValidAddress(RouterService.getAddress()); 
    }
    
    /**
     * Reads the Content-Length.  Invalid Content-Lengths are set to 0.
     */
    public static int readContentLength(final String value) {
        if(value == null)
            return 0;
        else {
            try {
                return Integer.parseInt(value.trim());
            } catch(NumberFormatException nfe) {
                return 0;
            }
        }
    }

    /**
     * Returns the HTTP response code from the given string, throwing
     * an exception if it couldn't be parsed.
     *
     * @param str an HTTP response string, e.g., "HTTP/1.1 200 OK \r\n"
     * @exception NoHTTPOKException str didn't contain "HTTP"
     * @exception ProblemReadingHeaderException some other problem
     *  extracting result code
     */
    private static int parseHTTPCode(String str, RemoteFileDesc rfd) 
                                                         throws IOException {
		StringTokenizer tokenizer = new StringTokenizer(str, " ");		
		String token;

		// just a safety
		if (! tokenizer.hasMoreTokens() )
			throw new NoHTTPOKException();

		token = tokenizer.nextToken();
		
		// the first token should contain HTTP
		if (token.toUpperCase().indexOf("HTTP") < 0 )
			throw new NoHTTPOKException("got: " + str);
        // does the server support http 1.1?
        else 
            rfd.setHTTP11( token.indexOf("1.1") > 0 );
		
		// the next token should be a number
		// just a safety
		if (! tokenizer.hasMoreTokens() )
			throw new NoHTTPOKException();

		token = tokenizer.nextToken();
		
		String num = token.trim();
		try {
			return java.lang.Integer.parseInt(num);
		} catch (NumberFormatException e) {
			throw new ProblemReadingHeaderException(e);
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
        if(str==null)
            return;
        
        //Note: According to the specification there are 5 headers, LimeWire
        //ignores 2 of them - queue length, and maxUploadSlots.
        StringTokenizer tokenizer = new StringTokenizer(str," ,:=");
        while(tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
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
            } catch(NumberFormatException nfx) {
                Arrays.fill(refQueueInfo,-1);
                break;
            } catch(NoSuchElementException nsex) {
                Arrays.fill(refQueueInfo,-1);
                break;
            }
        }
    }
    
    private void validateContentRange(Interval responseRange) throws IOException {
        int low = responseRange.low;
        int high = responseRange.high + 1;
        synchronized(this) {
            // were we stolen from in the meantime?
            if (_disconnect)
                throw new IOException("stolen from");
            
            // Make sure that the range they gave us is a subrange
            // of what we wanted in the first place.
            if(low < _initialReadingPoint ||
                    high > _initialReadingPoint + _amountToRead)
                throw new ProblemReadingHeaderException(
                        "invalid subrange given.  wanted low: " + 
                        _initialReadingPoint + ", high: " + 
                        (_initialReadingPoint + _amountToRead - 1) +
                        "... given low: " + low + ", high: " + high);                
            _initialReadingPoint = low;
            _amountToRead = high - low;
        }
    }

    /**
     * Returns the interval of the responding content range.
     * If the full content range (start & stop interval) is not given,
     * we assume it to be the interval that we requested.
     * The returned interval's low & high ranges are both INCLUSIVE.
     *
     * Does not strictly enforce HTTP; allows minor errors like replacing the
     * space after "bytes" with an equals.  Also tries to interpret malformed 
     * LimeWire 0.5 headers.
     *
     * @param str a Content-range header line, e.g.,
     *      "Content-range: bytes 0-9/10" or
     *      "Content-range:bytes 0-9/10" or
     *      "Content-range:bytes 0-9/X" (replacing X with "*") or
     *      "Content-range:bytes X/10" (replacing X with "*") or
     *      "Content-range:bytes X/X" (replacing X with "*") or
     *  Will also accept the incorrect but common 
     *      "Content-range: bytes=0-9/10"
     * @exception ProblemReadingHeaderException some problem
     *  extracting the start offset.  
     */
    private Interval parseContentRange(String str) throws IOException {
        int numBeforeDash;
        int numBeforeSlash;
        int numAfterSlash;

        if (LOG.isDebugEnabled())
            LOG.debug("reading content range: "+str);
        
        //Try to parse all three numbers from header for verification.
        //Special case "*" before or after slash.
        try {
            int start=str.indexOf("bytes")+6;  //skip "bytes " or "bytes="
            int slash=str.indexOf('/');
            
            //"bytes */*" or "bytes */10"
            // We don't know what we're getting, but it'll start at 0.
            // Assume that we're going to get until the part we requested.
            // If we read more, good.  If we read less, it'll work out just
            // fine.
            if (str.substring(start, slash).equals("*")) {
                if(LOG.isDebugEnabled())
                    LOG.debug(_rfd + " Content-Range like */?, " + str);
                synchronized(this) {
                    return new Interval(0, _amountToRead - 1);
                }
            }

            int dash=str.lastIndexOf("-");     //skip past "Content-range"
            numBeforeDash=Integer.parseInt(str.substring(start, dash));
            numBeforeSlash=Integer.parseInt(str.substring(dash+1, slash));

            if(numBeforeSlash < numBeforeDash)
                throw new ProblemReadingHeaderException(
                    "invalid range, high (" + numBeforeSlash +
                    ") less than low (" + numBeforeDash + ")");

            //"bytes 0-9/*"
            if (str.substring(slash+1).equals("*")) {
                if(LOG.isDebugEnabled())
                    LOG.debug(_rfd + " Content-Range like #-#/*, " + str);

                return new Interval(numBeforeDash, numBeforeSlash);
            }

            numAfterSlash=Integer.parseInt(str.substring(slash+1));
        } catch (IndexOutOfBoundsException e) {
            throw new ProblemReadingHeaderException(str);
        } catch (NumberFormatException e) {
            throw new ProblemReadingHeaderException(str);
        }

        // In order to be backwards compatible with
        // LimeWire 0.5, which sent broken headers like:
        // Content-range: bytes=1-67818707/67818707
        //
        // If the number preceding the '/' is equal 
        // to the number after the '/', then we want
        // to decrement the first number and the number
        // before the '/'.
        if (numBeforeSlash == numAfterSlash) {
            numBeforeDash--;
            numBeforeSlash--;
        }
        
        if(LOG.isDebugEnabled())
            LOG.debug(_rfd + " Content-Range like #-#/#, " + str);
        return new Interval(numBeforeDash, numBeforeSlash);
    }

    /**
     * Parses X-Available-Ranges header and stores the available ranges as a
     * list.
     * 
     * @param line the X-Available-Ranges header line which should look like:
     *         "X-Available-Ranges: bytes A-B, C-D, E-F"
     *         "X-Available-Ranges:bytes A-B"
     * @param rfd the RemoteFileDesc2 for the location we are trying to download
     *         from. We need this to store the available Ranges. 
     * @exception ProblemReadingHeaderException when we could not parse the 
     *         header line.
     */
    private void parseAvailableRangesHeader(String line, RemoteFileDesc rfd) 
                                                            throws IOException {
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
                break; 

            // this is the interval to store the available 
            // range we are parsing in.
            Interval interval = null;
    
            try {
                // read number before dash
                // bytes A-B, C-D
                //       ^
                int low = Integer.parseInt(line.substring(start, stop).trim());
                
                // now moving the start index to the 
                // character after the dash:
                // bytes A-B, C-D
                //         ^
                start = stop + 1;
                // we are parsing the number before the comma
                stop = line.indexOf(',', start);
                
                // If we are at the end of the header line, there is no comma 
                // following.
                if ( stop == -1 )
                    stop = line.length();
                
                // read number after dash
                // bytes A-B, C-D
                //         ^
                int high = Integer.parseInt(line.substring(start, stop).trim());

                // start parsing after the next comma. If we are at the
                // end of the header line start will be set to 
                // line.length() +1
                start = stop + 1;
                
                if(high >= rfd.getSize())
                    high = rfd.getSize()-1;

                if(low > high)//interval read off network is bad, try next one
                    continue;

                // this interval should be inclusive at both ends
                interval = new Interval( low, high );
                
            } catch (NumberFormatException e) {
                throw new ProblemReadingHeaderException(e);
            }
            availableRanges.add(interval);
        }
        rfd.setAvailableRanges(availableRanges);
    }

    /**
     * Parses the Retry-After header.
     * @param str - expects a simple integer number specifying the
     * number of seconds to wait before retrying the host.
     * @exception ProblemReadingHeaderException if we could not read 
     * the header
     */
    private static void parseRetryAfterHeader(String str, RemoteFileDesc rfd) 
      throws IOException {
        int seconds = 0;
        try {
            seconds = Integer.parseInt(str);
        } catch (NumberFormatException e) {
            throw new ProblemReadingHeaderException(e);
        }
        // make sure the value is not smaller than MIN_RETRY_AFTER seconds
        seconds = Math.max(seconds, MIN_RETRY_AFTER);
        // make sure the value is not larger than MAX_RETRY_AFTER seconds
        seconds = Math.min(seconds, MAX_RETRY_AFTER);
        rfd.setRetryAfter(seconds);
    }
    
    /**
     * Parses the Creation Time header.
     * @param str - expects a long number specifying the age in milliseconds
     * of this file.
     * @exception ProblemReadingHeaderException if we could not read 
     * the header
     */
    private static void parseCreationTimeHeader(String str, RemoteFileDesc rfd) 
      throws IOException {
        long milliSeconds = 0;
        try {
            milliSeconds = Long.parseLong(str);
        } catch (NumberFormatException e) {
            throw new ProblemReadingHeaderException(e);
        }
        if (rfd.getSHA1Urn() != null) {
            CreationTimeCache ctCache = CreationTimeCache.instance();
            synchronized (ctCache) {
                Long cTime = ctCache.getCreationTime(rfd.getSHA1Urn());
                // prefer older times....
                if ((cTime == null) || (cTime.longValue() > milliSeconds))
                    ctCache.addTime(rfd.getSHA1Urn(), milliSeconds);
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
            // ignore the version for now.

            if (protocol.equals(HTTPConstants.CHAT_PROTOCOL))
                _chatEnabled = true;
            else if (protocol.equals(HTTPConstants.BROWSE_PROTOCOL))
                _browseEnabled = true;
            else if (protocol.equals(HTTPConstants.PUSH_LOCS))
            	_wantsFalts=true;
            else if (protocol.equals(HTTPConstants.FW_TRANSFER)) {
                //for this header we care about the version
                int FWTVersion=0;
                try{
                    FWTVersion = (int)HTTPUtils.parseFeatureToken(feature);
                    _wantsFalts=true;
                }catch(ProblemReadingHeaderException prhe) {
                    //ignore this header
                    continue;
                }

                // try to update the FWT version and external address we know for this host
            	try {
            	    updatePEAddress();
            	    PushEndpoint.setFWTVersionSupported(_rfd.getClientGUID(),FWTVersion);
                } catch (IOException ignored) {}
            }
        }
    }

    /**
     * Method for reading the X-Thex-Uri header.
     */
    private void parseTHEXHeader (String str) {
        if(LOG.isDebugEnabled())
            LOG.debug(_host + ":" + _port +">" + str);
        
        if (str.indexOf(";") > 0) {
            StringTokenizer tok = new StringTokenizer(str, ";");
            _thexUri = tok.nextToken();
            _root32 = tok.nextToken();
        } else
            _thexUri = str;
    }    
    
    /**
     * 
     * Method for parsing the header containing firewalled alternate
     * locations.  The format is a modified version of the one described
     * in the push proxy spec at the_gdf
     * 
     */
    private void parseFALTHeader(String str) {
    	//if we entered this method means the other side is interested
    	//in receiving firewalled locations.
    	_wantsFalts=true;
    	
    	//this just delegates to readAlternateLocationHeader
    	readAlternateLocations(str);
    }
      
    
    /**
     * parses the header containing the current set of push proxies for 
     * the given host, and updates the rfd
     */
    private void parseProxiesHeader(String str) {
        if (_rfd.getPushAddr()==null || str==null || str.length()<12) 
            return;
        
        try {
            PushEndpoint.overwriteProxies(_rfd.getClientGUID(),str);
            updatePEAddress();
        }catch(IOException tooBad) {
            // invalid header - ignore it.
        }
        
    }
    
    private void updatePEAddress() throws IOException {
        IpPort newAddr = new IpPortImpl(_socket.getInetAddress().getHostAddress(),_socket.getPort()); 
        if (NetworkUtils.isValidExternalIpPort(newAddr))
            PushEndpoint.setAddr(_rfd.getClientGUID(),newAddr);
    }
    
    /////////////////////////////// Download ////////////////////////////////

    /*
     * Downloads the content from the server and writes it to a temporary
     * file.  Non-blocking.  This MUST be initialized via connect() beforehand, and
     * doDownload MUST NOT have already been called.
     *  
     * @exception IOException download was interrupted, typically (but not
     *  always) because the other end closed the connection.
     */
	public void doDownload(IOStateObserver observer) throws SocketException {
        _socket.setSoTimeout(60 * 1000); // downloading, can stall upto 1 minute
        observerHandler.setDelegate(observer);
        _stateMachine.addState(new DownloadState());
    }
    
    private class DownloadState extends ReadState {
        private long currPos = _initialReadingPoint;
        private volatile boolean doingWrite;
        
        void writeDone() {
            doingWrite = false;
        }

        protected boolean processRead(ReadableByteChannel channel, ByteBuffer buffer) throws IOException {
            if(doingWrite)
                return true;
            
            boolean dataLeft = false;
            try {
                //LOG.debug("Doing read");
                dataLeft = readImpl(channel, buffer);
            } catch (IOException error) {
                LOG.debug("Error while reading", error);
                chunkCompleted();
                throw error;
            }

            if (!dataLeft) {
                chunkCompleted();
                if (!isHTTP11() || _disconnect)
                    throw new IOException("stolen from");
            }
            
            return dataLeft;
        }
        
        private void chunkCompleted() {
            _bodyConsumed = true;
            synchronized (HTTPDownloader.this) {
                _isActive = false;
            }
        }

        private boolean readImpl(ReadableByteChannel rc, ByteBuffer buffer) throws IOException {
            while(true) {
                int read = 0;
                    
                // first see how much we have left to read, if any
                int left;
                synchronized(HTTPDownloader.this) {
                    if (_amountRead >= _amountToRead) {
                        LOG.debug("Read >= to needed, done.");
                        _isActive = false;
                        return false;
                    }
                    left = _amountToRead - _amountRead;
                }
                
                // Account for data already in the buffer.
                int preread = Math.min(left, buffer.position());
                if(preread != 0 && LOG.isDebugEnabled())
                    LOG.debug("Using preread data of: " + preread);
                
                if(left - preread > 0) {
                    // ensure we don't read more into the buffer than we want.
                    if(buffer.limit() > left)
                        buffer.limit(left);
                   
                    while(buffer.hasRemaining() && (read = rc.read(buffer)) > 0);
                
                    // ensure the limit is set back to normal.
                    buffer.limit(buffer.capacity());
                }
                
                int totalRead = buffer.position();
                if (_inNetwork)
                    BandwidthStat.HTTP_BODY_DOWNSTREAM_INNETWORK_BANDWIDTH.addData(totalRead);
                else
                    BandwidthStat.HTTP_BODY_DOWNSTREAM_BANDWIDTH.addData(totalRead);
                
                // If nothing could be read at all, leave.
                if(totalRead == 0) {
                    if(read == -1) {
                        LOG.debug("EOF while reading");
                        throw new IOException("EOF");
                    } else if(read == 0) {
                        return true;
                    }
                }
                
                long filePosition;
                int dataLength;
                int dataStart;
    			synchronized(this) {
                    if (_isActive) {
                        // see if we were stolen from while reading
                        totalRead = Math.min(totalRead, _amountToRead - _amountRead);
                        if (totalRead <=0 ) {
                            LOG.debug("Someone stole completely from us while reading");
                            // if were told to not read anything more, finish
                            _isActive = false;
                            buffer.clear();
                            return false;
                        }
                        
                        int skipped = Math.min(totalRead, Math.max(0, (int)(_initialWritingPoint - currPos)));
                        if(skipped > 0)
                            LOG.debug("Amount we should skip: " + skipped);

                        
                        // setup data for writing.
                        dataLength = totalRead - skipped;
                        dataStart = skipped;
                        filePosition = currPos + skipped;
                        // maintain data for next read.
                        _amountRead += totalRead;
                        currPos += totalRead;
                        
                        if(skipped >= totalRead) {       
                            if(LOG.isDebugEnabled())
                                LOG.debug("skipped full read of: " + skipped + " bytes");
                            buffer.clear();
                            continue;
                        }                 
                    } else {
    			        if (LOG.isDebugEnabled())
    			            LOG.debug("WORKER:"+this+" stopping at "+(_initialReadingPoint+_amountRead));
                        buffer.clear();
    			       return false;
    			    }
    			}                
                
                // TODO: Write to disk only when buffer is full.
    			try {
    				// write to disk outside of lock.
    				//LOG.debug("WORKER: " + this + ", left: " + (left-totalRead) +",  writing fp: " + filePosition +", ds: " + dataStart + ", dL: " + dataLength);
    				if(!_incompleteFile.writeBlock(filePosition, dataStart, dataLength, buffer.array())) {
    					LOG.debug("Scheduling callback for write.");
    					InterestReadChannel irc = (InterestReadChannel)rc;
    					irc.interest(false);
    					doingWrite = true;
    					_incompleteFile.writeBlockWithCallback(filePosition, dataStart, dataLength, buffer.array(),
    							new DownloadRestarter(irc, buffer, this));
    					return true;
    				}
    			} catch (AssertFailure bad) {
    				createAssertionReport(bad);
    			}
                buffer.clear();
            }
        }

        public long getAmountProcessed() {
            return -1;
        }
	}
    
    void createAssertionReport(AssertFailure bad) {
		String currentWorker = "current worker "+System.identityHashCode(this);
		String allWorkers = null;
		URN urn = _rfd.getSHA1Urn();
		if (urn != null) {
			ManagedDownloader myDownloader = RouterService.getDownloadManager().getDownloaderForURN(urn);
			if (myDownloader == null)
				allWorkers = "couldn't find my downloader???";
			else
				allWorkers = myDownloader.getWorkersInfo();
		}else
			allWorkers = " sha1 not available ";
		
		String errorReport = bad.getMessage() + "\n\n"+currentWorker+"\n\n"+allWorkers;
		AssertFailure failure = new AssertFailure(errorReport);
		failure.setStackTrace(bad.getStackTrace()); // so we see the VF dump only once.
		throw failure;

    }
    
    private static class DownloadRestarter implements VerifyingFile.WriteCallback {
        private final DownloadState downloader;
        private final InterestReadChannel irc;
        private final ByteBuffer buffer;
        
        DownloadRestarter(InterestReadChannel irc, ByteBuffer buffer, DownloadState downloader) {
            this.irc = irc;
            this.buffer = buffer;
            this.downloader = downloader;
        }
        
        public void writeScheduled() {
            LOG.debug("Delayed write scheduled");
            buffer.clear();
            downloader.writeDone();
            irc.interest(true);
        }
    }

    /** 
     * Stops this immediately.  This method is always safe to call.
     *     @modifies this
     */
	public void stop() {
	    synchronized(this) {
	        if (LOG.isDebugEnabled())
	            LOG.debug("WORKER:"+this+" signaled to stop at "+(_initialReadingPoint+_amountRead));
	        _isActive = false;
	    }
        
        // Close in the NIO thread, so everything stays there.
        NIODispatcher.instance().invokeLater(new Runnable() {
            public void run() {
                IOUtils.close(_socket);
            }
        });
	}

    /**
     * Instructs this stop just before reading the given byte.
     * This cannot be used to increase the initial range.
     * @param stop the index just past the last byte to read;
     *  stop-1 is the index of the last byte to be downloaded
     */
    public synchronized void stopAt(int stop) {
        _disconnect = true;
        _amountToRead = Math.min(_amountToRead,stop-_initialReadingPoint);
    }
    
    public synchronized void startAt(int start) {
        _initialWritingPoint = start;
    }
    
    synchronized void forgetRanges() {
    	_initialWritingPoint = 0;
    	_initialReadingPoint = 0;
    	_amountToRead = 0;
    	_totalAmountRead += _amountRead;
    	_amountRead = 0;
    }
    
    ///////////////////////////// Accessors ///////////////////////////////////

    public synchronized int getInitialReadingPoint() {return _initialReadingPoint;}
    public synchronized int getInitialWritingPoint() {return _initialWritingPoint;}
	public synchronized int getAmountRead() {return _amountRead;}
	public synchronized int getTotalAmountRead() {return _totalAmountRead + _amountRead;}
	public synchronized int getAmountToRead() {return _amountToRead;}
	public synchronized boolean isActive() { return _isActive; }
    synchronized boolean isVictim() {return _disconnect; }

    /** 
     * Forces this to not write past the given byte of the file, if it has not
     * already done so. Typically this is called to reduce the download window;
     * doing otherwise will typically result in incomplete downloads.
     * 
     * @param stop a byte index into the file, using 0 to N-1 notation.  */
    public InetAddress getInetAddress() {return _socket.getInetAddress();}
	public boolean chatEnabled() {
		return _chatEnabled;
	}

	public boolean browseEnabled() {
		return _browseEnabled;
	}
	
	/**
	 * @return whether the remote host is interested in receiving
	 * firewalled alternate locations.
	 */
	public boolean wantsFalts() {
		return _wantsFalts;
	}
	
	public String getVendor() { return _server; }

	public long getIndex() {return _index;}
  	public String getFileName() {return _filename;}
  	public byte[] getGUID() {return _guid;}
	public int getPort() {return _port;}
	
    /**
     * Returns the RemoteFileDesc passed to this' constructor.
     */
    public RemoteFileDesc getRemoteFileDesc() {return _rfd;}
    
    /**
     *  returns true if we have think that the server 
     *  supports HTTP1.1 
     */
    public boolean isHTTP11() {
        return _rfd.isHTTP11();
    }
    
    /**
     * Returns TRUE if this downloader has a THEX tree that we have not yet
     * retrieved.
     */
    public boolean hasHashTree() {
        return _thexUri != null 
            && _root32 != null
            && !_rfd.hasTHEXFailed()
            && !_thexSucceeded;
    }

    /////////////////////Bandwidth tracker interface methods//////////////
    public void measureBandwidth() {
        int totalAmountRead = 0;
        synchronized(this) {
            if (!_isActive)
                return;
            totalAmountRead = getTotalAmountRead();
        }
        
        bandwidthTracker.measureBandwidth(totalAmountRead);
    }

    public float getMeasuredBandwidth() throws InsufficientDataException {
        return bandwidthTracker.getMeasuredBandwidth();
    }
    
    public float getAverageBandwidth() {
        return bandwidthTracker.getAverageBandwidth();
    }
            
    /**
     * Set bandwidth limitation for downloads.
     */
    public static void setRate(float bytesPerSecond) {
        THROTTLE.setRate(bytesPerSecond);
    }
    
    /**
     * Apply bandwidth limitation from settings.
     */
    public static void applyRate() {
        float downloadRate = Float.MAX_VALUE;
        int downloadThrottle = DownloadSettings.DOWNLOAD_SPEED.getValue();
        
        if ( downloadThrottle < 100 )
        {
            downloadRate = ((downloadThrottle/100.f)*
             (ConnectionSettings.CONNECTION_SPEED.getValue()/8.f))*1024.f;
        }
        setRate( downloadRate );
    }
    
    
	////////////////////////////// Unit Test ////////////////////////////////

    public String toString() {
        return "<"+_host+":"+_port+", "+getFileName()+">";
    }
    
    public static void setThrottleSwitching(boolean on) {
        //THROTTLE.setSwitching(on);
        // DO NOT PUT SWITCHING ON THE UDP SIDE.
    }
    
    private static class Observer implements IOStateObserver {
        private IOStateObserver delegate;
        private boolean handled = false;
        private boolean error = false;
        
        public void handleIOException(IOException iox) {
            IOStateObserver del;
            synchronized(this) {
                error = true;
                if(handled) {
                    LOG.warn("Ignoring iox", iox);
                    return;
                }
                
                handled = true;
                del = delegate;
            }
            if(del != null)
             del.handleIOException(iox);
        }
    
        public void handleStatesFinished() {
            IOStateObserver del;
            synchronized(this) {
                if(handled) {
                    if(LOG.isWarnEnabled())
                        LOG.warn("Ignoring states finished", new Exception());
                    return;
                }
                handled = true;
                del = delegate;
            }
            if(del != null)
                del.handleStatesFinished();
        }
    
        public void shutdown() {
            IOStateObserver del;
            synchronized(this) {
                error = true;
                if(handled) {
                    if(LOG.isWarnEnabled())
                        LOG.warn("Ignoring shutdown.");
                    return;
                }
                handled = true;
                del = delegate;
            }
            if(del != null)
                del.shutdown();
        }
        
        void setDelegate(IOStateObserver observer) {
            boolean hadError = false;
            synchronized(this) {
                handled = false;
                hadError = error;
                delegate = observer;
            }
            
            if(hadError) {
                observer.shutdown();
            }
        }
    }
}










