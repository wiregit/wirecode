package com.limegroup.gnutella.downloader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.BandwidthTracker;
import com.limegroup.gnutella.BandwidthTrackerImpl;
import com.limegroup.gnutella.ByteReader;
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
import com.limegroup.gnutella.http.HTTPHeaderValueCollection;
import com.limegroup.gnutella.http.HTTPUtils;
import com.limegroup.gnutella.http.ProblemReadingHeaderException;
import com.limegroup.gnutella.settings.ChatSettings;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.DownloadSettings;
import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.gnutella.statistics.BandwidthStat;
import com.limegroup.gnutella.statistics.DownloadStat;
import com.limegroup.gnutella.statistics.NumericalDownloadStat;
import com.limegroup.gnutella.tigertree.HashTree;
import com.limegroup.gnutella.udpconnect.UDPConnection;
import com.limegroup.gnutella.util.BandwidthThrottle;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.CountingInputStream;
import com.limegroup.gnutella.util.IntervalSet;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.IpPortImpl;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.NPECatchingInputStream;
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
    public static final int BUF_LENGTH=1024;
    
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
    
    /**
     * The throttle to use for all TCP downloads.
     */
    private static final BandwidthThrottle THROTTLE =
        new BandwidthThrottle(Float.MAX_VALUE, false);
        
    /**
     * The throttle to use for UDP downloads.
     */
    private static final BandwidthThrottle UDP_THROTTLE =
        new BandwidthThrottle(Float.MAX_VALUE, false);

    private RemoteFileDesc _rfd;
    private boolean _isPush;
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
	private int _contentLength;
	
	/**
	 * Whether or not the body has been consumed.
	 */
	private boolean _bodyConsumed = true;

	private ByteReader _byteReader;
	private Socket _socket;  //initialized in HTTPDownloader(Socket) or connect
	private OutputStream _output;
	private InputStream _input;
    private final VerifyingFile _incompleteFile;
    
	/**
	 * The new alternate locations we've received for this file.
	 */
	private HashSet _locationsReceived;

    /**
     *  The good locations to send the uploaders as in the alts list
     */
    private Set _goodLocs;
    
    /**
     * The firewalled locations to send to uploaders that are interested
     */
    private Set _goodPushLocs;
    
    /**
     * The bad firewalled locations to send to uploaders that are interested
     */
    private Set _badPushLocs;
    
    /** 
     * The list to send in the n-alts list
     */
    private Set _badLocs;
    
    /**
     * The list of already written alts, used to stop duplicates
     */
    private Set _writtenGoodLocs;
    
    /**
     * The list of already written n-alts, used to stop duplicates
     */ 
    private Set _writtenBadLocs;
    
    /**
     * The list of already written push alts, used to stop duplicates
     */
    private Set _writtenPushLocs;
    
    /**
     * The list of already written bad push alts, used to stop duplicates
     */
    private Set _writtenBadPushLocs;

    
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
        _isPush=false;
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
	public HTTPDownloader(Socket socket, RemoteFileDesc rfd, 
	        VerifyingFile incompleteFile, boolean inNetwork) {
        if(rfd == null) {
            throw new NullPointerException("null rfd");
        }
        _isPush=true;
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
        URN urn = rfd.getSHA1Urn();
        _locationsReceived = new HashSet();
        _goodLocs = new HashSet();
        _badLocs = new HashSet();
        _goodPushLocs = new HashSet();
        _badPushLocs = new HashSet();
        _writtenGoodLocs = new HashSet();
        _writtenBadLocs = new HashSet();
        _writtenPushLocs = new HashSet();
        _writtenBadPushLocs = new HashSet();
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
    Collection getLocationsReceived() { 
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
    				_goodLocs.add(loc); //duplicates make no difference
    		}
    	}
    	else {
    		synchronized(_badPushLocs) {
    			//If we ever thought loc was bad, forget that we did, so that we can
    			//add it to the n-alts list again, if it fails -- remove from
    			//writtenBadlocs
    			_writtenBadPushLocs.remove(loc);           
    			_badPushLocs.remove(loc);
    		}
    		synchronized(_goodPushLocs) {
    			if(!_writtenPushLocs.contains(loc)) //not written earlier
    				_goodPushLocs.add(loc); //duplicates make no difference
    				
    		}
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
    				_badLocs.add(loc); //duplicates make no difference
    		}
    	}
    	else {
    		synchronized(_goodPushLocs) {
    			_writtenPushLocs.remove(loc);
    			_goodPushLocs.remove(loc);
    		}
        
    		synchronized(_badPushLocs) {
    			if(!_writtenBadPushLocs.contains(loc))//no need to repeat to uploader
    				_badPushLocs.add(loc); //duplicates make no difference
    		}
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
     * @exception CantConnectException could not establish a TCP connection
     */
	public void connectTCP(int timeout) throws IOException {
        //Connect, if not already done.  Ignore 
        //The try-catch below is a work-around for JDK bug 4091706.
        try {            
            if (_socket==null) {
                long curTime = System.currentTimeMillis();
                _socket = Sockets.connect(_host, _port, timeout);
                NumericalDownloadStat.TCP_CONNECT_TIME.
                    addData((int)(System.currentTimeMillis() -  curTime));
                
            }
            //If platform supports it, set SO_KEEPALIVE option.  This helps
            //detect a crashed uploader.
            Sockets.setKeepAlive(_socket, true);
            _input = new NPECatchingInputStream(new BufferedInputStream(_socket.getInputStream()));
            _output = new BufferedOutputStream(_socket.getOutputStream());
            
        } catch (IOException e) {
            throw new CantConnectException();
        }
        //Note : once we have established the TCP connection with the host we
        //want to download from we set the soTimeout. Its reset in doDownload
        //Note2 : this may throw an IOException.  
        _socket.setSoTimeout(Constants.TIMEOUT);
        _byteReader = new ByteReader(_input);
    }
    
    /**
     * Same as connectHTTP(start, stop, supportQueueing, -1)
     */
    public void connectHTTP(int start, int stop, boolean supportQueueing) 
        throws IOException, TryAgainLaterException, FileNotFoundException, 
             NotSharingException, QueuedException, RangeNotAvailableException,
             ProblemReadingHeaderException, UnknownCodeException {
        connectHTTP(start, stop, supportQueueing, -1);
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
    public void connectHTTP(int start, int stop, boolean supportQueueing,
    						int amountDownloaded) 
        throws IOException, TryAgainLaterException, FileNotFoundException, 
             NotSharingException, QueuedException, RangeNotAvailableException,
             ProblemReadingHeaderException, UnknownCodeException {
        if(start < 0)
            throw new IllegalArgumentException("invalid start: " + start);
        if(stop <= start)
            throw new IllegalArgumentException("stop(" + stop +
                                               ") <= start(" + start +")");

        synchronized(this) {
            _isActive = true;
            _amountToRead = stop-start;
            _amountRead = 0;
            _initialReadingPoint = start;
            _initialWritingPoint = start;
            _bodyConsumed = false;
            _contentLength = 0;
        }
		
        
		// features to be sent with the X-Features header
        Set features = new HashSet();
		
        //Write GET request and headers.  We request HTTP/1.1 since we need
        //persistence for queuing & chunked downloads.
        //(So we can't write "Connection: close".)
        OutputStreamWriter osw = new OutputStreamWriter(_output);
        BufferedWriter out=new BufferedWriter(osw);
        String startRange = java.lang.String.valueOf(_initialReadingPoint);
        out.write("GET "+_rfd.getUrl().getFile()+" HTTP/1.1\r\n");
        out.write("HOST: "+_host+":"+_port+"\r\n");
        out.write("User-Agent: "+CommonUtils.getHttpServer()+"\r\n");

        if (supportQueueing) {
            // legacy QUEUE header, - to be replaced by X-Features header
            // as already implemented by BearShare
            out.write("X-Queue: 0.1\r\n"); //we support remote queueing
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
        	 (RouterService.acceptedIncomingConnection() ||
        			_wantsFalts)) {
        		AlternateLocation me = AlternateLocation.create(_rfd.getSHA1Urn());
        		if (me != null)
        			addSuccessfulAltLoc(me);
        }

        URN sha1 = _rfd.getSHA1Urn();
		if ( sha1 != null )
		    HTTPUtils.writeHeader(HTTPHeaderName.GNUTELLA_CONTENT_URN,sha1,out);

        //We don't want to hold locks while doing network operations, so we use
        //this variable to clone _goodLocs and _badLocs and write to network
        //while iterating over the clone
        Set writeClone = null;
        
        //write altLocs 
        synchronized(_goodLocs) {
            if(_goodLocs.size() > 0) {
                writeClone = new HashSet();
                Iterator iter = _goodLocs.iterator();
                while(iter.hasNext()) {
                    Object next = iter.next();
                    writeClone.add(next);
                    _writtenGoodLocs.add(next);
                }
                _goodLocs.clear();
            }
        }
        if(writeClone != null) //have something to write?
            HTTPUtils.writeHeader(HTTPHeaderName.ALT_LOCATION,
                                 new HTTPHeaderValueCollection(writeClone),out);
        
        writeClone = null;
        //write-nalts        
        synchronized(_badLocs) {
            if(_badLocs.size() > 0) {
                writeClone = new HashSet();
                Iterator iter = _badLocs.iterator();
                while(iter.hasNext()) {
                    Object next = iter.next();
                    writeClone.add(next);
                    _writtenBadLocs.add(next);
                }
                _badLocs.clear();
            }
        }

        if(writeClone != null) //have something to write?
            HTTPUtils.writeHeader(HTTPHeaderName.NALTS,
                                new HTTPHeaderValueCollection(writeClone),out);
        
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
        			writeClone = new HashSet();
        			Iterator iter = _goodPushLocs.iterator();
        			while(iter.hasNext()) {
        				PushAltLoc next = (PushAltLoc)iter.next();
        				
        				// we should not have empty proxies unless this is ourselves
        				if (next.getPushAddress().getProxies().isEmpty()) {
        				    if (next.getPushAddress() instanceof PushEndpointForSelf)
        				        continue;
        				    else
        				        Assert.that(false,"empty pushloc in downloader");
        				}
        				
        				writeClone.add(next);
        				_writtenPushLocs.add(next);
        			}
        			_goodPushLocs.clear();
        		}
        	}
        	if (writeClone!=null) 
        		HTTPUtils.writeHeader(HTTPHeaderName.FALT_LOCATION,
        			new HTTPHeaderValueCollection(writeClone),out);
        	
        	//do the same with bad push locs
        	writeClone = null;
        	synchronized(_badPushLocs) {
                if(_badPushLocs.size() > 0) {
                    writeClone = new HashSet();
                    Iterator iter = _badPushLocs.iterator();
                    while(iter.hasNext()) {
                        PushAltLoc next = (PushAltLoc)iter.next();
                        
                        // no empty proxies allowed here
        				Assert.that(!next.getPushAddress().getProxies().isEmpty());
        				
        				writeClone.add(next);
                        _writtenBadPushLocs.add(next);
                    }
                    _badPushLocs.clear();
                }
            }
        	
        	if (writeClone!=null) 
        		HTTPUtils.writeHeader(HTTPHeaderName.BFALT_LOCATION,
        				new HTTPHeaderValueCollection(writeClone),out);
        }
        
        
        

        
        out.write("Range: bytes=" + startRange + "-"+(stop-1)+"\r\n");
        synchronized(this) {
            _requestedInterval = new Interval(_initialReadingPoint, stop-1);
        }
		if (RouterService.acceptedIncomingConnection() &&
           !NetworkUtils.isPrivateAddress(RouterService.getAddress())) {
            int port = RouterService.getPort();
            String host = NetworkUtils.ip2string(RouterService.getAddress());
            out.write("X-Node: " + host + ":" + port + "\r\n");
            features.add(ConstantHTTPHeaderValue.BROWSE_FEATURE);
            // Legacy chat header. Replaced by X-Features header / X-Node
            // header
            if (ChatSettings.CHAT_ENABLED.getValue()) {
                out.write("Chat: " + host + ":" + port + "\r\n");
                features.add(ConstantHTTPHeaderValue.CHAT_FEATURE);
            }
        }	
		
		// Write X-Features header.
        if (features.size() > 0) {
            HTTPUtils.writeHeader(HTTPHeaderName.FEATURES,
                        new HTTPHeaderValueCollection(features),
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
        
        // if we got here, we connected fine
        if (LOG.isDebugEnabled())
            LOG.debug(this+" completed connectHTTP");
	}
	
	/**
	 * Consumes the body of the HTTP message that was previously exchanged,
	 * if necessary.
	 */
    public void consumeBodyIfNecessary() {
        LOG.trace("enter consumeBodyIfNecessary");
        try {
            if(!_bodyConsumed)
                consumeBody(_contentLength);
        } catch(IOException ignored) {}
        _bodyConsumed = true;
    }
	
    /**
     * Returns the ConnectionStatus from the request.
     * Can be one of:
     *   Connected -- means to immediately assignAndRequest.
     *   Queued -- means to sleep while queued.
     *   ThexResponse -- means the thex tree was received.
     */
    public ConnectionStatus requestHashTree(URN sha1) {
        if (LOG.isDebugEnabled())
            LOG.debug("requesting HashTree for " + _thexUri + 
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
        } catch (IOException ioe) {
            if (LOG.isDebugEnabled())
                LOG.debug("connection failed during sending hashtree request"); 
            return ConnectionStatus.getConnected();
        }
        try {
            String line = _byteReader.readLine();
            if(line == null)
                throw new IOException("disconnected");
            int code = parseHTTPCode(line, _rfd);
            if(code < 200 || code >= 300) {
                if(LOG.isDebugEnabled())
                    LOG.debug("invalid HTTP code: " + code);
                _rfd.setTHEXFailed();
                return consumeResponse(code);
            }
            
            // Code was 2xx, consume the headers
            int contentLength = consumeHeaders(null);
            // .. and read the body.
            // if it fails for any reason, try consuming however much we
            // have left to read
            InputStream in = _input;
            if(contentLength != -1)
                in = new CountingInputStream(_input);
            try {
                HashTree hashTree =
                    HashTree.createHashTree(in, sha1.toString(),
                                            _root32, _rfd.getFileSize());
                _thexSucceeded = true;
                return ConnectionStatus.getThexResponse(hashTree);
            } catch(IOException ioe) {
                if(in instanceof CountingInputStream) {
                    LOG.debug("failed with contentLength", ioe);
                    _rfd.setTHEXFailed();                    
                    int read = ((CountingInputStream)in).getAmountRead();
                    return consumeBody(contentLength - read);
                } else {
                    throw ioe;
                }
            }       
        } catch (IOException ioe) {
            LOG.debug("failed without contentLength", ioe);
            
            _rfd.setTHEXFailed();
            // any other replies that can possibly cause an exception
            // (404, 410) will cause the host to fall through in the
            // ManagedDownloader anyway.
            // if it was just a connection failure, we may retry.
            return ConnectionStatus.getConnected();
        }
    }
    
    /**
     * Consumes the headers of an HTTP message, returning the Content-Length.
     */
    private int consumeHeaders(int[] queueInfo) throws IOException {
        if(LOG.isDebugEnabled())
            LOG.debug(_rfd + " consuming headers");
            
        int contentLength = -1;
        String str;
        while(true) {
            str = _byteReader.readLine();
            if(str == null || str.equals(""))
                break;
            if(HTTPHeaderName.CONTENT_LENGTH.matchesStartOfString(str)) {
                String value = HTTPUtils.extractHeaderValue(str);
                if(value == null) continue;
                try {
                    contentLength = Integer.parseInt(value.trim());
                } catch(NumberFormatException nfe) {
                    contentLength = -1;
                }
            } else if(queueInfo != null && 
                      HTTPHeaderName.QUEUE.matchesStartOfString(str)) 
                parseQueueHeaders(str, queueInfo);
        }
        return contentLength;
    }   
    
    /**
     * Consumes the response of an HTTP message.
     */
    private ConnectionStatus consumeResponse(int code) throws IOException {
        if(LOG.isDebugEnabled())
            LOG.debug(_rfd + " consuming response, code: " + code);

        int[] queueInfo = { -1, -1, -1 };
        int contentLength = consumeHeaders(queueInfo);
        if(code == 503) {
            int min = queueInfo[0];
            int max = queueInfo[1];
            int pos = queueInfo[2];
            if(min != -1 && max != -1 && pos != -1)
                return ConnectionStatus.getQueued(pos, min);
        }
        return consumeBody(contentLength);
    }
    
    /**
     * Consumes the body portion of an HTTP Message.
     */
    private ConnectionStatus consumeBody(int contentLength)
      throws IOException {
        if(LOG.isTraceEnabled())
            LOG.trace("enter consumeBody(" + contentLength + ")");

        if(contentLength < 0)
            throw new IOException("unknown content-length, can't consume");

        byte[] buf = new byte[1024];
        // read & ignore all the content.
        while(contentLength > 0) {
            int toRead = Math.min(buf.length, contentLength);
            int read = _input.read(buf, 0, toRead);
            if(read == -1)
                break;
            contentLength -= read;
        }
        return ConnectionStatus.getConnected();
    }           

    /*
     * Reads the headers from this, setting _initialReadingPoint and
     * _amountToRead.  Throws any of the exceptions listed in connect().  
     */
	private void readHeaders() throws IOException {
		if (_byteReader == null) 
			throw new ReaderIsNullException();

		// Read the response code from the first line and check for any errors
		String str = _byteReader.readLine();  
		if (str==null || str.equals(""))
            throw new IOException();

        if (_inNetwork)
            BandwidthStat.HTTP_HEADER_DOWNSTREAM_INNETWORK_BANDWIDTH.addData(str.length());
        else 
            BandwidthStat.HTTP_HEADER_DOWNSTREAM_BANDWIDTH.addData(str.length());
        
        int code=parseHTTPCode(str, _rfd);	
        //Note: According to the specification there are 5 headers, LimeWire
        //ignores 2 of them - queue length, and maxUploadSlots.
        int[] refQueueInfo = {-1,-1,-1};
        //Now read each header...
		while (true) {            
			str = _byteReader.readLine();
            if (str==null || str.equals(""))
                break;
            
            if (_inNetwork)
                BandwidthStat.HTTP_HEADER_DOWNSTREAM_INNETWORK_BANDWIDTH.addData(str.length());
            else 
                BandwidthStat.HTTP_HEADER_DOWNSTREAM_BANDWIDTH.addData(str.length());
            //As of LimeWire 1.9, we ignore the "Content-length" header for
            //handling normal download flow.  The Content-Length is only
            //used for reading/discarding some HTTP body messages.
			
            //For "Content-Range" headers, we store what the remote side is
            //going to give us.  Users should examine the interval and
            //update external structures appropriately.
            if (str.toUpperCase().startsWith("CONTENT-RANGE:")) {
                Interval responseRange = parseContentRange(str);
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
            else if(HTTPHeaderName.CONTENT_LENGTH.matchesStartOfString(str))
                _contentLength = readContentLength(str);
            else if(HTTPHeaderName.CONTENT_URN.matchesStartOfString(str))
				checkContentUrnHeader(str, _rfd.getSHA1Urn());
            else if(HTTPHeaderName.GNUTELLA_CONTENT_URN.matchesStartOfString(str))
				checkContentUrnHeader(str, _rfd.getSHA1Urn());
			else if(HTTPHeaderName.ALT_LOCATION.matchesStartOfString(str))
                readAlternateLocations(str);
            else if(HTTPHeaderName.QUEUE.matchesStartOfString(str)) 
                parseQueueHeaders(str, refQueueInfo);
            else if (HTTPHeaderName.SERVER.matchesStartOfString(str)) 
                _server = readServer(str);
            else if (HTTPHeaderName.AVAILABLE_RANGES.matchesStartOfString(str))
                parseAvailableRangesHeader(str, _rfd);
            else if (HTTPHeaderName.RETRY_AFTER.matchesStartOfString(str)) 
                parseRetryAfterHeader(str, _rfd);
            else if (HTTPHeaderName.CREATION_TIME.matchesStartOfString(str))
                parseCreationTimeHeader(str, _rfd);
            else if (HTTPHeaderName.FEATURES.matchesStartOfString(str))
            	parseFeatureHeader(str);
            else if (HTTPHeaderName.THEX_URI.matchesStartOfString(str))
                parseTHEXHeader(str);
            else if (HTTPHeaderName.FALT_LOCATION.matchesStartOfString(str))
            	parseFALTHeader(str);
            else if (HTTPHeaderName.PROXIES.matchesStartOfString(str))
                parseProxiesHeader(str);
            
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
                if(min != -1 && max != -1 && pos != -1)
                    throw new QueuedException(min,max,pos);
                    
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
    private void checkContentUrnHeader(String str, URN sha1)
        throws ContentUrnMismatchException {
        String value = HTTPUtils.extractHeaderValue(str);
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
	private void readAlternateLocations(final String altHeader) {
		final String altStr = HTTPUtils.extractHeaderValue(altHeader);
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
	 * Reads the Server header.  All information after the ':' is considered
	 * to be the Server.
	 */
	public static String readServer(final String serverHeader) {
	    int colon = serverHeader.indexOf(':');
	    // if it existed & wasn't at the end...
	    if ( colon != -1 && colon < serverHeader.length()-1 )
	        return serverHeader.substring(colon+1).trim();
        else
            return "";
    }
    
    /**
     * Reads the Content-Length.  Invalid Content-Lengths are set to 0.
     */
    public static int readContentLength(final String header) {
        String value = HTTPUtils.extractHeaderValue(header);
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
        //Note: According to the specification there are 5 headers, LimeWire
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
            } catch(NumberFormatException nfx) {//bad headers drop connection
                //We could return at this point--basically does the same things.
                Arrays.fill(refQueueInfo,-1);
            } catch(NoSuchElementException nsex) {//bad headers drop connection
                //We could return at this point--basically does the same things.
                Arrays.fill(refQueueInfo,-1);
            }
        } //end of while - done parsing this line.
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
        str = HTTPUtils.extractHeaderValue(str);
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
        str = HTTPUtils.extractHeaderValue(str);
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
        str = HTTPUtils.extractHeaderValue(str);
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

        str = HTTPUtils.extractHeaderValue(str);
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
        str = HTTPUtils.extractHeaderValue(str);
        
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
     * file.  Blocking.  This MUST be initialized via connect() beforehand, and
     * doDownload MUST NOT have already been called. If there is
     * a mismatch in overlaps, the VerifyingFile triggers a callback to
     * the ManagedDownloader, which triggers a callback to the GUI to let us
     * know whether to continue or interrupt.
     *  
     * @exception IOException download was interrupted, typically (but not
     *  always) because the other end closed the connection.
     */
	public void doDownload() 
        throws DiskException, IOException {
        
        _socket.setSoTimeout(1*60*1000);//downloading, can stall upto 1 mins
        
        long currPos = _initialReadingPoint;
        try {
            
            int c = -1;
            byte[] buf = new byte[BUF_LENGTH];
            
            while (true) {
                //Read from network.  It's possible that we've read more than
                //requested because of a call to setAmountToRead() or stopAt() from another
                //thread.  We check for that before we write to disk.
                
                // first see how much we have left to read, if any
                int left;
                synchronized(this) {
                    if (_amountRead >= _amountToRead) {
                        _isActive = false;
                        break;
                    }
                    left = _amountToRead - _amountRead;
                }
                
                Assert.that(left>0);

                // do the actual read from the network using the appropriate bandwidth 
                // throttle
                BandwidthThrottle throttle = _socket instanceof UDPConnection ?
                    UDP_THROTTLE : THROTTLE;
                int toRead = throttle.request(Math.min(BUF_LENGTH, left));
                c = _byteReader.read(buf, 0, toRead);
                if (c == -1) 
                    break;
                
                if (_inNetwork)
                    BandwidthStat.HTTP_BODY_DOWNSTREAM_INNETWORK_BANDWIDTH.addData(c);
                else
                    BandwidthStat.HTTP_BODY_DOWNSTREAM_BANDWIDTH.addData(c);

				synchronized(this) {
				    if (_isActive) {
                        
                        // skip until we reach the initial writing point
                        int skipped = 0;
                        while (_initialWritingPoint > currPos && c > 0) {
                            skipped++;
                            currPos++;
                            c--;
                            _amountRead++;
                        }
                        
                        // if we're still not there, continue
                        if (_initialWritingPoint > currPos || c == 0) {
                            if (LOG.isDebugEnabled())
                                LOG.debug("skipped "+skipped+" bytes");
                            
                            continue;
                        }
                        
                        // if are past our initial writing point, but we had to skip some bytes 
                        // or were told to stop sooner, trim the buffer
                        if (skipped > 0 || _amountRead+c >= _amountToRead) {
                            c = Math.min(c,_amountToRead - _amountRead);
                            if (LOG.isDebugEnabled())
                                LOG.debug("trimming buffer by "+
                                        skipped +" to "+c+" bytes");
                            
                            byte [] temp = new byte[c];
                            System.arraycopy(buf,skipped,temp,0,c);
                            System.arraycopy(temp,0,buf,0,temp.length);
                        } 
                        
                        // write to disk
                        try {
                            _incompleteFile.writeBlock(currPos,c,buf);
                        } catch (InterruptedException killed) {
                            _isActive = false;
                            break;
                        }
				        
                        _amountRead+=c;
				        currPos += c;//update the currPos for next iteration
				        
				    }
				    else {
				        if (LOG.isDebugEnabled())
				            LOG.debug("WORKER:"+this+" stopping at "+(_initialReadingPoint+_amountRead));
				        break;
				    }
				} 
            }  // end of while loop

            synchronized(this) {
                _isActive = false;
                if ( _amountRead < _amountToRead ) { 
                    throw new FileIncompleteException();  
                }
            }
            
        } finally {
            _bodyConsumed = true;
            synchronized(this) {
                _isActive = false;
            }
            if(!isHTTP11() || _disconnect) 
                throw new IOException("stolen from");
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
        if (_byteReader != null)
            _byteReader.close();
        try {
            if (_socket != null)
                _socket.close();
        } catch (IOException e) { }
        try {
            if(_input != null)
                _input.close();
        } catch(IOException e) {}
        try {
            if(_output != null)
                _output.close();
        } catch(IOException e) {}
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
     * Returns true iff this is a push download.
     */
    public boolean isPush() {return _isPush;}
    
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
        UDP_THROTTLE.setRate(bytesPerSecond);
    }
    
    /**
     * Apply bandwidth limitation from settings.
     */
    public static void applyRate() {
        float downloadRate = Float.MAX_VALUE;
        int downloadThrottle = DownloadSettings.DOWNLOAD_SPEED.getValue();
        
        if ( downloadThrottle < 100 )
        {
            downloadRate = (((float)downloadThrottle/100.f)*
             ((float)ConnectionSettings.CONNECTION_SPEED.getValue()/8.f))*1024.f;
        }
        setRate( downloadRate );
    }
    
    
	////////////////////////////// Unit Test ////////////////////////////////

    public String toString() {
        return "<"+_host+":"+_port+", "+getFileName()+">";
    }
    
    public static void setThrottleSwitching(boolean on) {
        THROTTLE.setSwitching(on);
        // DO NOT PUT SWITCHING ON THE UDP SIDE.
    }
    
	private HTTPDownloader(String str) {
		ByteArrayInputStream stream = new ByteArrayInputStream(str.getBytes());
		_byteReader = new ByteReader(stream);
		_locationsReceived = null;
        _goodLocs = null;
        _badLocs = null;
        _writtenGoodLocs = null;
        _writtenBadLocs = null;
		_rfd =  new RemoteFileDesc("127.0.0.1", 1,
                                  0, "a", 0, new byte[16],
                                  0, false, 0, false, null, null,
                                  false, false, "", 0, null, -1, 0);
        _incompleteFile = null;
        _inNetwork = false;
	}    
}










