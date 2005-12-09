pbckage com.limegroup.gnutella.downloader;

import jbva.io.BufferedInputStream;
import jbva.io.BufferedOutputStream;
import jbva.io.BufferedWriter;
import jbva.io.ByteArrayInputStream;
import jbva.io.IOException;
import jbva.io.InputStream;
import jbva.io.OutputStream;
import jbva.io.OutputStreamWriter;
import jbva.net.InetAddress;
import jbva.net.Socket;
import jbva.util.Arrays;
import jbva.util.Collection;
import jbva.util.HashSet;
import jbva.util.Iterator;
import jbva.util.NoSuchElementException;
import jbva.util.Set;
import jbva.util.StringTokenizer;


import org.bpache.commons.logging.Log;
import org.bpache.commons.logging.LogFactory;

import com.limegroup.gnutellb.Assert;
import com.limegroup.gnutellb.BandwidthTracker;
import com.limegroup.gnutellb.BandwidthTrackerImpl;
import com.limegroup.gnutellb.ByteReader;
import com.limegroup.gnutellb.Constants;
import com.limegroup.gnutellb.CreationTimeCache;
import com.limegroup.gnutellb.InsufficientDataException;
import com.limegroup.gnutellb.PushEndpoint;
import com.limegroup.gnutellb.PushEndpointForSelf;
import com.limegroup.gnutellb.RemoteFileDesc;
import com.limegroup.gnutellb.RouterService;
import com.limegroup.gnutellb.UDPService;
import com.limegroup.gnutellb.URN;
import com.limegroup.gnutellb.altlocs.AlternateLocation;
import com.limegroup.gnutellb.altlocs.DirectAltLoc;
import com.limegroup.gnutellb.altlocs.PushAltLoc;
import com.limegroup.gnutellb.http.ConstantHTTPHeaderValue;
import com.limegroup.gnutellb.http.HTTPConstants;
import com.limegroup.gnutellb.http.HTTPHeaderName;
import com.limegroup.gnutellb.http.HTTPHeaderValueCollection;
import com.limegroup.gnutellb.http.HTTPUtils;
import com.limegroup.gnutellb.http.ProblemReadingHeaderException;
import com.limegroup.gnutellb.settings.ChatSettings;
import com.limegroup.gnutellb.settings.ConnectionSettings;
import com.limegroup.gnutellb.settings.DownloadSettings;
import com.limegroup.gnutellb.settings.UploadSettings;
import com.limegroup.gnutellb.statistics.BandwidthStat;
import com.limegroup.gnutellb.statistics.DownloadStat;
import com.limegroup.gnutellb.statistics.NumericalDownloadStat;
import com.limegroup.gnutellb.tigertree.HashTree;
import com.limegroup.gnutellb.udpconnect.UDPConnection;
import com.limegroup.gnutellb.util.BandwidthThrottle;
import com.limegroup.gnutellb.util.CommonUtils;
import com.limegroup.gnutellb.util.CountingInputStream;
import com.limegroup.gnutellb.util.IntervalSet;
import com.limegroup.gnutellb.util.IpPort;
import com.limegroup.gnutellb.util.IpPortImpl;
import com.limegroup.gnutellb.util.NetworkUtils;
import com.limegroup.gnutellb.util.NPECatchingInputStream;
import com.limegroup.gnutellb.util.Sockets;

/**
 * Downlobds a file over an HTTP connection.  This class is as simple as
 * possible.  It does not debl with retries, prioritizing hosts, etc.  Nor does
 * it check whether b file already exists; it just writes over anything on
 * disk.<p>
 *
 * It is necessbry to explicitly initialize an HTTPDownloader with the
 * connectTCP(..) followed by b connectHTTP(..) method.  (Hence HTTPDownloader
 * behbves much like Connection.)  Typical use is as follows:
 *
 * <pre>
 * HTTPDownlobder dl=new HTTPDownloader(host, port);
 * dl.connectTCP(timeout);
 * dl.connectHTTP(stbrtByte, stopByte);
 * dl.doDownlobd();
 * </pre> 
 * LOCKING: _writtenGoodLocs bnd _goodLocs are both synchronized on _goodLocs
 * LOCKING: _writtenBbdLocs and _badLocs are both synchronized on _badLocs
 */

public clbss HTTPDownloader implements BandwidthTracker {
    
    privbte static final Log LOG = LogFactory.getLog(HTTPDownloader.class);
    
    /**
     * The length of the buffer used in downlobding.
     */
    public stbtic final int BUF_LENGTH=1024;
    
    /**
     * The smbllest possible time in seconds to wait before retrying a busy
     * host. 
     */
    privbte static final int MIN_RETRY_AFTER = 60; // 60 seconds

    /**
     * The mbximum possible time in seconds to wait before retrying a busy
     * host. 
     */
    privbte static final int MAX_RETRY_AFTER = 60 * 60; // 1 hour
    
    /**
     * The smbllest possible file to be shared with partial file sharing.
     * Non finbl for testing purposes.
     */
    stbtic int MIN_PARTIAL_FILE_BYTES = 1*1024*1024; // 1MB
    
    /**
     * The throttle to use for bll TCP downloads.
     */
    privbte static final BandwidthThrottle THROTTLE =
        new BbndwidthThrottle(Float.MAX_VALUE, false);
        
    /**
     * The throttle to use for UDP downlobds.
     */
    privbte static final BandwidthThrottle UDP_THROTTLE =
        new BbndwidthThrottle(Float.MAX_VALUE, false);

    privbte RemoteFileDesc _rfd;
    privbte boolean _isPush;
	privbte long _index;
	privbte String _filename; 
	privbte byte[] _guid;

	
    /**
     * The totbl amount we've downloaded, including all previous 
     * HTTP connections
     * LOCKING: this
     */
    privbte int _totalAmountRead;
    
    /**
     *  The bmount we've downloaded.
     * LOCKING: this 
     */
	privbte int _amountRead;
	
    /** 
     * The bmount we'll have downloaded if the download completes properly. 
     *  Note thbt the amount still left to download is 
     *  _bmountToRead - _amountRead.
     * LOCKING: this 
     */
	privbte int _amountToRead;
    
    /**
     * Whether to disconnect bfter reading the amount we have wanted to read
     */
    privbte volatile boolean _disconnect;
    
    /** 
     *  The index to stbrt reading from the server 
     * LOCKING: this 
     */
	privbte int _initialReadingPoint;
	
    /** 
     *  The index to bctually start writing to the file.
     *  LOCKING:this
     */
    privbte int _initialWritingPoint;
    
	/**
	 * The content-length of the output, useful only for when we
	 * wbnt to read & discard the body of the HTTP message.
	 */
	privbte int _contentLength;
	
	/**
	 * Whether or not the body hbs been consumed.
	 */
	privbte boolean _bodyConsumed = true;

	privbte ByteReader _byteReader;
	privbte Socket _socket;  //initialized in HTTPDownloader(Socket) or connect
	privbte OutputStream _output;
	privbte InputStream _input;
    privbte final VerifyingFile _incompleteFile;
    
	/**
	 * The new blternate locations we've received for this file.
	 */
	privbte HashSet _locationsReceived;

    /**
     *  The good locbtions to send the uploaders as in the alts list
     */
    privbte Set _goodLocs;
    
    /**
     * The firewblled locations to send to uploaders that are interested
     */
    privbte Set _goodPushLocs;
    
    /**
     * The bbd firewalled locations to send to uploaders that are interested
     */
    privbte Set _badPushLocs;
    
    /** 
     * The list to send in the n-blts list
     */
    privbte Set _badLocs;
    
    /**
     * The list of blready written alts, used to stop duplicates
     */
    privbte Set _writtenGoodLocs;
    
    /**
     * The list of blready written n-alts, used to stop duplicates
     */ 
    privbte Set _writtenBadLocs;
    
    /**
     * The list of blready written push alts, used to stop duplicates
     */
    privbte Set _writtenPushLocs;
    
    /**
     * The list of blready written bad push alts, used to stop duplicates
     */
    privbte Set _writtenBadPushLocs;

    
	privbte int _port;
	privbte String _host;
	
	privbte boolean _chatEnabled = false; // for now
    privbte boolean _browseEnabled = false; // also for now
    privbte String _server = "";
    
    privbte String _thexUri = null;
    privbte String _root32 = null;    
    /**
     * Whether or not the retrievbl of THEX succeeded.
     * This is stored here, bs opposed to the RemoteFileDesc,
     * becbuse we may want to re-use the RemoteFileDesc to try
     * bnd get the THEX tree later on from this host, if
     * the first bttempt failed from corruption.
     *
     * Fbilures are stored in the RemoteFileDesc because
     * if it fbiled we never want to try it again, ever.
     */
    privbte boolean _thexSucceeded = false;

    /** For implementing the BbndwidthTracker interface. */
    privbte BandwidthTrackerImpl bandwidthTracker=new BandwidthTrackerImpl();
    
    /**
     * Whether or not this HTTPDownlobder is currently attempting to read
     * informbtion from the network.
     */
    privbte boolean _isActive = false;


    privbte Interval _requestedInterval = null;
    
    /**
     * whether the other side wbnts to receive firewalled altlocs
     */
    privbte boolean _wantsFalts = false;
    
    /** Whether to count the bbndwidth used by this downloader */
    privbte final boolean _inNetwork;
    

    /**
     * Crebtes an uninitialized client-side normal download.  Call 
     * connectTCP bnd connectHTTP() on this before any other methods.  
     * Non-blocking.
     *
     * @pbram rfd complete information for the file to download, including
     *  host bddress and port
     * @pbram incompleteFile the temp file to use while downloading, which need
     *  not exist.
     * @pbram start the place to start reading from the network and writing to 
     *  the file
     * @pbram stop the last byte to read+1
     */
	public HTTPDownlobder(RemoteFileDesc rfd, VerifyingFile incompleteFile, boolean inNetwork) {
        //Dirty secret: this is implemented with the push constructor!
        this(null, rfd, incompleteFile, inNetwork);
        _isPush=fblse;
	}	

	/**
     * Crebtes an uninitialized server-side push download. connectTCP() and 
     * connectHTTP() on this before bny other methods.  Non-blocking.
     * 
     * @pbram socket the socket to download from.  The "GIV..." line must
     *  hbve been read from socket.  HTTP headers may not have been read or 
     *  buffered -- this cbn be <tt>null</tt>
     * @pbram rfd complete information for the file to download, including
     *  host bddress and port
     * @pbram incompleteFile the temp file to use while downloading, which need
     *  not exist.
     */
	public HTTPDownlobder(Socket socket, RemoteFileDesc rfd, 
	        VerifyingFile incompleteFile, boolebn inNetwork) {
        if(rfd == null) {
            throw new NullPointerException("null rfd");
        }
        _isPush=true;
        _rfd=rfd;
        _socket=socket;
        _incompleteFile=incompleteFile;
		_filenbme = rfd.getFileName();
		_index = rfd.getIndex();
		_guid = rfd.getClientGUID();
		_bmountToRead = 0;
		_port = rfd.getPort();
		_host = rfd.getHost();
		_chbtEnabled = rfd.chatEnabled();
        _browseEnbbled = rfd.browseHostEnabled();
        URN urn = rfd.getSHA1Urn();
        _locbtionsReceived = new HashSet();
        _goodLocs = new HbshSet();
        _bbdLocs = new HashSet();
        _goodPushLocs = new HbshSet();
        _bbdPushLocs = new HashSet();
        _writtenGoodLocs = new HbshSet();
        _writtenBbdLocs = new HashSet();
        _writtenPushLocs = new HbshSet();
        _writtenBbdPushLocs = new HashSet();
		_bmountRead = 0;
		_totblAmountRead = 0;
        _inNetwork = inNetwork;
		bpplyRate();
    }

    ////////////////////////Alt Locs methods////////////////////////
    
    /**
     * Accessor for the blternate locations received from the server for 
     * this downlobd attempt.  
     *
     * @return the <tt>AlternbteLocationCollection</tt> containing the 
     *  received locbtions, can be <tt>null</tt> if we could not create
     *  b collection, or could be empty
     */
    Collection getLocbtionsReceived() { 
	    return _locbtionsReceived;
    }
    
    void bddSuccessfulAltLoc(AlternateLocation loc) {
    	if (loc instbnceof DirectAltLoc) {
    		synchronized(_bbdLocs) {
    			//If we ever thought loc wbs bad, forget that we did, so that we can
    			//bdd it to the n-alts list again, if it fails -- remove from
    			//writtenBbdlocs
    			_writtenBbdLocs.remove(loc);           
    			_bbdLocs.remove(loc);
    		}
    		synchronized(_goodLocs) {
    			if(!_writtenGoodLocs.contbins(loc)) //not written earlier
    				_goodLocs.bdd(loc); //duplicates make no difference
    		}
    	}
    	else {
    		synchronized(_bbdPushLocs) {
    			//If we ever thought loc wbs bad, forget that we did, so that we can
    			//bdd it to the n-alts list again, if it fails -- remove from
    			//writtenBbdlocs
    			_writtenBbdPushLocs.remove(loc);           
    			_bbdPushLocs.remove(loc);
    		}
    		synchronized(_goodPushLocs) {
    			if(!_writtenPushLocs.contbins(loc)) //not written earlier
    				_goodPushLocs.bdd(loc); //duplicates make no difference
    				
    		}
    	}
    }
    
    void bddFailedAltLoc(AlternateLocation loc) {
        //if we ever thought it wbs good, forget that we did, so we can write it
        //out bs good again -- remove it from writtenGoodLocs if it was there
    	
    	if (loc instbnceof DirectAltLoc){
    		synchronized(_goodLocs) {
    			_writtenGoodLocs.remove(loc);
    			_goodLocs.remove(loc);
    		}
        
    		synchronized(_bbdLocs) {
    			if(!_writtenBbdLocs.contains(loc))//no need to repeat to uploader
    				_bbdLocs.add(loc); //duplicates make no difference
    		}
    	}
    	else {
    		synchronized(_goodPushLocs) {
    			_writtenPushLocs.remove(loc);
    			_goodPushLocs.remove(loc);
    		}
        
    		synchronized(_bbdPushLocs) {
    			if(!_writtenBbdPushLocs.contains(loc))//no need to repeat to uploader
    				_bbdPushLocs.add(loc); //duplicates make no difference
    		}
    	}
    }
    
    ///////////////////////////////// Connection /////////////////////////////

    /** 
     * Initiblizes this by connecting to the remote host (in the case of a
     * normbl client-side download). Blocks for up to timeout milliseconds 
     * trying to connect, unless timeout is zero, in which cbse there is 
     * no timeout.  This MUST be uninitiblized, i.e., connectTCP may not be 
     * cblled more than once.
     * <p>
     * @pbram timeout the timeout to use for connecting, in milliseconds,
     *  or zero if no timeout
     * @exception CbntConnectException could not establish a TCP connection
     */
	public void connectTCP(int timeout) throws IOException {
        //Connect, if not blready done.  Ignore 
        //The try-cbtch below is a work-around for JDK bug 4091706.
        try {            
            if (_socket==null) {
                long curTime = System.currentTimeMillis();
                _socket = Sockets.connect(_host, _port, timeout);
                NumericblDownloadStat.TCP_CONNECT_TIME.
                    bddData((int)(System.currentTimeMillis() -  curTime));
                
            }
            //If plbtform supports it, set SO_KEEPALIVE option.  This helps
            //detect b crashed uploader.
            Sockets.setKeepAlive(_socket, true);
            _input = new NPECbtchingInputStream(new BufferedInputStream(_socket.getInputStream()));
            _output = new BufferedOutputStrebm(_socket.getOutputStream());
            
        } cbtch (IOException e) {
            throw new CbntConnectException();
        }
        //Note : once we hbve established the TCP connection with the host we
        //wbnt to download from we set the soTimeout. Its reset in doDownload
        //Note2 : this mby throw an IOException.  
        _socket.setSoTimeout(Constbnts.TIMEOUT);
        _byteRebder = new ByteReader(_input);
    }
    
    /**
     * Sbme as connectHTTP(start, stop, supportQueueing, -1)
     */
    public void connectHTTP(int stbrt, int stop, boolean supportQueueing) 
        throws IOException, TryAgbinLaterException, FileNotFoundException, 
             NotShbringException, QueuedException, RangeNotAvailableException,
             ProblemRebdingHeaderException, UnknownCodeException {
        connectHTTP(stbrt, stop, supportQueueing, -1);
    }
    
    /** 
     * Sends b GET request using an already open socket, and reads all 
     * hebders.  The actual ranges downloaded MAY NOT be the same
     * bs the 'start' and 'stop' parameters, as HTTP allows the server
     * to respond with bny satisfiable subrange of the request.
     *
     * Users of this clbss should examine getInitialReadingPoint()
     * bnd getAmountToRead() to determine what the effective start & stop
     * rbnges are, and update external datastructures appropriately.
     *  int newStbrt = dloader.getInitialReadingPoint();
     *  int newStop = (dlobder.getAmountToRead() - 1) + newStart; // INCLUSIVE
     * or
     *  int newStop = dlobder.getAmountToRead() + newStart; // EXCLUSIVE
     *
     * <p>
     * @pbram start The byte at which the HTTPDownloader should begin
     * @pbram stop the index just past the last byte to read;
     *  stop-1 is the lbst byte the HTTPDownloader should download
     * <p>
     * @exception TryAgbinLaterException the host is busy
     * @exception FileNotFoundException the host doesn't recognize the file
     * @exception NotShbringException the host isn't sharing files (BearShare)
     * @exception IOException miscellbneous  error 
     * @exception QueuedException uplobder has queued us
     * @exception RbngeNotAvailableException uploader has ranges 
     * other thbn requested
     * @exception ProblemRebdingHeaderException could not parse headers
     * @exception UnknownCodeException unknown response code
     */
    public void connectHTTP(int stbrt, int stop, boolean supportQueueing,
    						int bmountDownloaded) 
        throws IOException, TryAgbinLaterException, FileNotFoundException, 
             NotShbringException, QueuedException, RangeNotAvailableException,
             ProblemRebdingHeaderException, UnknownCodeException {
        if(stbrt < 0)
            throw new IllegblArgumentException("invalid start: " + start);
        if(stop <= stbrt)
            throw new IllegblArgumentException("stop(" + stop +
                                               ") <= stbrt(" + start +")");

        synchronized(this) {
            _isActive = true;
            _bmountToRead = stop-start;
            _bmountRead = 0;
            _initiblReadingPoint = start;
            _initiblWritingPoint = start;
            _bodyConsumed = fblse;
            _contentLength = 0;
        }
		
        
		// febtures to be sent with the X-Features header
        Set febtures = new HashSet();
		
        //Write GET request bnd headers.  We request HTTP/1.1 since we need
        //persistence for queuing & chunked downlobds.
        //(So we cbn't write "Connection: close".)
        OutputStrebmWriter osw = new OutputStreamWriter(_output);
        BufferedWriter out=new BufferedWriter(osw);
        String stbrtRange = java.lang.String.valueOf(_initialReadingPoint);
        out.write("GET "+_rfd.getUrl().getFile()+" HTTP/1.1\r\n");
        out.write("HOST: "+_host+":"+_port+"\r\n");
        out.write("User-Agent: "+CommonUtils.getHttpServer()+"\r\n");

        if (supportQueueing) {
            // legbcy QUEUE header, - to be replaced by X-Features header
            // bs already implemented by BearShare
            out.write("X-Queue: 0.1\r\n"); //we support remote queueing
            febtures.add(ConstantHTTPHeaderValue.QUEUE_FEATURE);
        }
        
        //if I'm not firewblled or I can do FWT, say that I want pushlocs.
        //if I bm firewalled, send the version of the FWT protocol I support.
        // (which implies thbt I want only altlocs that support FWT)
        if (RouterService.bcceptedIncomingConnection() || UDPService.instance().canDoFWT()) {
            febtures.add(ConstantHTTPHeaderValue.PUSH_LOCS_FEATURE);
            if (!RouterService.bcceptedIncomingConnection())
                febtures.add(ConstantHTTPHeaderValue.FWT_PUSH_LOCS_FEATURE);
        }
        	

        // Add ourselves to the mesh if the pbrtial file is valid
        //if I'm firewblled add myself only if the other guy wants falts
        if( isPbrtialFileValid() && 
        	 (RouterService.bcceptedIncomingConnection() ||
        			_wbntsFalts)) {
        		AlternbteLocation me = AlternateLocation.create(_rfd.getSHA1Urn());
        		if (me != null)
        			bddSuccessfulAltLoc(me);
        }

        URN shb1 = _rfd.getSHA1Urn();
		if ( shb1 != null )
		    HTTPUtils.writeHebder(HTTPHeaderName.GNUTELLA_CONTENT_URN,sha1,out);

        //We don't wbnt to hold locks while doing network operations, so we use
        //this vbriable to clone _goodLocs and _badLocs and write to network
        //while iterbting over the clone
        Set writeClone = null;
        
        //write bltLocs 
        synchronized(_goodLocs) {
            if(_goodLocs.size() > 0) {
                writeClone = new HbshSet();
                Iterbtor iter = _goodLocs.iterator();
                while(iter.hbsNext()) {
                    Object next = iter.next();
                    writeClone.bdd(next);
                    _writtenGoodLocs.bdd(next);
                }
                _goodLocs.clebr();
            }
        }
        if(writeClone != null) //hbve something to write?
            HTTPUtils.writeHebder(HTTPHeaderName.ALT_LOCATION,
                                 new HTTPHebderValueCollection(writeClone),out);
        
        writeClone = null;
        //write-nblts        
        synchronized(_bbdLocs) {
            if(_bbdLocs.size() > 0) {
                writeClone = new HbshSet();
                Iterbtor iter = _badLocs.iterator();
                while(iter.hbsNext()) {
                    Object next = iter.next();
                    writeClone.bdd(next);
                    _writtenBbdLocs.add(next);
                }
                _bbdLocs.clear();
            }
        }

        if(writeClone != null) //hbve something to write?
            HTTPUtils.writeHebder(HTTPHeaderName.NALTS,
                                new HTTPHebderValueCollection(writeClone),out);
        
        // if the other side indicbted they want firewalled altlocs, send some
        //
        // Note: we send both types of firewblled altlocs to the uploader since even if
        // it cbn't support FWT it can still spread them to other downloaders.
        //
        // Note2: we cbn't know whether the other side wants to receive pushlocs until
        // we rebd their headers. Therefore pushlocs will be sent from the second
        // http request on.
        
        if (_wbntsFalts) {
        	writeClone = null;
        	synchronized(_goodPushLocs) {
        		if(_goodPushLocs.size() > 0) {
        			writeClone = new HbshSet();
        			Iterbtor iter = _goodPushLocs.iterator();
        			while(iter.hbsNext()) {
        				PushAltLoc next = (PushAltLoc)iter.next();
        				
        				// we should not hbve empty proxies unless this is ourselves
        				if (next.getPushAddress().getProxies().isEmpty()) {
        				    if (next.getPushAddress() instbnceof PushEndpointForSelf)
        				        continue;
        				    else
        				        Assert.thbt(false,"empty pushloc in downloader");
        				}
        				
        				writeClone.bdd(next);
        				_writtenPushLocs.bdd(next);
        			}
        			_goodPushLocs.clebr();
        		}
        	}
        	if (writeClone!=null) 
        		HTTPUtils.writeHebder(HTTPHeaderName.FALT_LOCATION,
        			new HTTPHebderValueCollection(writeClone),out);
        	
        	//do the sbme with bad push locs
        	writeClone = null;
        	synchronized(_bbdPushLocs) {
                if(_bbdPushLocs.size() > 0) {
                    writeClone = new HbshSet();
                    Iterbtor iter = _badPushLocs.iterator();
                    while(iter.hbsNext()) {
                        PushAltLoc next = (PushAltLoc)iter.next();
                        
                        // no empty proxies bllowed here
        				Assert.thbt(!next.getPushAddress().getProxies().isEmpty());
        				
        				writeClone.bdd(next);
                        _writtenBbdPushLocs.add(next);
                    }
                    _bbdPushLocs.clear();
                }
            }
        	
        	if (writeClone!=null) 
        		HTTPUtils.writeHebder(HTTPHeaderName.BFALT_LOCATION,
        				new HTTPHebderValueCollection(writeClone),out);
        }
        
        
        

        
        out.write("Rbnge: bytes=" + startRange + "-"+(stop-1)+"\r\n");
        synchronized(this) {
            _requestedIntervbl = new Interval(_initialReadingPoint, stop-1);
        }
		if (RouterService.bcceptedIncomingConnection() &&
           !NetworkUtils.isPrivbteAddress(RouterService.getAddress())) {
            int port = RouterService.getPort();
            String host = NetworkUtils.ip2string(RouterService.getAddress());
            out.write("X-Node: " + host + ":" + port + "\r\n");
            febtures.add(ConstantHTTPHeaderValue.BROWSE_FEATURE);
            // Legbcy chat header. Replaced by X-Features header / X-Node
            // hebder
            if (ChbtSettings.CHAT_ENABLED.getValue()) {
                out.write("Chbt: " + host + ":" + port + "\r\n");
                febtures.add(ConstantHTTPHeaderValue.CHAT_FEATURE);
            }
        }	
		
		// Write X-Febtures header.
        if (febtures.size() > 0) {
            HTTPUtils.writeHebder(HTTPHeaderName.FEATURES,
                        new HTTPHebderValueCollection(features),
                        out);
        }
		
        // Write X-Downlobded header to inform uploader about
        // how mbny bytes already transferred for this file
        if ( bmountDownloaded > 0 ) {
            HTTPUtils.writeHebder(HTTPHeaderName.DOWNLOADED,
                        String.vblueOf(amountDownloaded),
                        out);
        }
		
        out.write("\r\n");
        out.flush();

        //Rebd response.
        rebdHeaders();
        
        // if we got here, we connected fine
        if (LOG.isDebugEnbbled())
            LOG.debug(this+" completed connectHTTP");
	}
	
	/**
	 * Consumes the body of the HTTP messbge that was previously exchanged,
	 * if necessbry.
	 */
    public void consumeBodyIfNecessbry() {
        LOG.trbce("enter consumeBodyIfNecessary");
        try {
            if(!_bodyConsumed)
                consumeBody(_contentLength);
        } cbtch(IOException ignored) {}
        _bodyConsumed = true;
    }
	
    /**
     * Returns the ConnectionStbtus from the request.
     * Cbn be one of:
     *   Connected -- mebns to immediately assignAndRequest.
     *   Queued -- mebns to sleep while queued.
     *   ThexResponse -- mebns the thex tree was received.
     */
    public ConnectionStbtus requestHashTree(URN sha1) {
        if (LOG.isDebugEnbbled())
            LOG.debug("requesting HbshTree for " + _thexUri + 
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
        } cbtch (IOException ioe) {
            if (LOG.isDebugEnbbled())
                LOG.debug("connection fbiled during sending hashtree request"); 
            return ConnectionStbtus.getConnected();
        }
        try {
            String line = _byteRebder.readLine();
            if(line == null)
                throw new IOException("disconnected");
            int code = pbrseHTTPCode(line, _rfd);
            if(code < 200 || code >= 300) {
                if(LOG.isDebugEnbbled())
                    LOG.debug("invblid HTTP code: " + code);
                _rfd.setTHEXFbiled();
                return consumeResponse(code);
            }
            
            // Code wbs 2xx, consume the headers
            int contentLength = consumeHebders(null);
            // .. bnd read the body.
            // if it fbils for any reason, try consuming however much we
            // hbve left to read
            InputStrebm in = _input;
            if(contentLength != -1)
                in = new CountingInputStrebm(_input);
            try {
                HbshTree hashTree =
                    HbshTree.createHashTree(in, sha1.toString(),
                                            _root32, _rfd.getFileSize());
                _thexSucceeded = true;
                return ConnectionStbtus.getThexResponse(hashTree);
            } cbtch(IOException ioe) {
                if(in instbnceof CountingInputStream) {
                    LOG.debug("fbiled with contentLength", ioe);
                    _rfd.setTHEXFbiled();                    
                    int rebd = ((CountingInputStream)in).getAmountRead();
                    return consumeBody(contentLength - rebd);
                } else {
                    throw ioe;
                }
            }       
        } cbtch (IOException ioe) {
            LOG.debug("fbiled without contentLength", ioe);
            
            _rfd.setTHEXFbiled();
            // bny other replies that can possibly cause an exception
            // (404, 410) will cbuse the host to fall through in the
            // MbnagedDownloader anyway.
            // if it wbs just a connection failure, we may retry.
            return ConnectionStbtus.getConnected();
        }
    }
    
    /**
     * Consumes the hebders of an HTTP message, returning the Content-Length.
     */
    privbte int consumeHeaders(int[] queueInfo) throws IOException {
        if(LOG.isDebugEnbbled())
            LOG.debug(_rfd + " consuming hebders");
            
        int contentLength = -1;
        String str;
        while(true) {
            str = _byteRebder.readLine();
            if(str == null || str.equbls(""))
                brebk;
            if(HTTPHebderName.CONTENT_LENGTH.matchesStartOfString(str)) {
                String vblue = HTTPUtils.extractHeaderValue(str);
                if(vblue == null) continue;
                try {
                    contentLength = Integer.pbrseInt(value.trim());
                } cbtch(NumberFormatException nfe) {
                    contentLength = -1;
                }
            } else if(queueInfo != null && 
                      HTTPHebderName.QUEUE.matchesStartOfString(str)) 
                pbrseQueueHeaders(str, queueInfo);
        }
        return contentLength;
    }   
    
    /**
     * Consumes the response of bn HTTP message.
     */
    privbte ConnectionStatus consumeResponse(int code) throws IOException {
        if(LOG.isDebugEnbbled())
            LOG.debug(_rfd + " consuming response, code: " + code);

        int[] queueInfo = { -1, -1, -1 };
        int contentLength = consumeHebders(queueInfo);
        if(code == 503) {
            int min = queueInfo[0];
            int mbx = queueInfo[1];
            int pos = queueInfo[2];
            if(min != -1 && mbx != -1 && pos != -1)
                return ConnectionStbtus.getQueued(pos, min);
        }
        return consumeBody(contentLength);
    }
    
    /**
     * Consumes the body portion of bn HTTP Message.
     */
    privbte ConnectionStatus consumeBody(int contentLength)
      throws IOException {
        if(LOG.isTrbceEnabled())
            LOG.trbce("enter consumeBody(" + contentLength + ")");

        if(contentLength < 0)
            throw new IOException("unknown content-length, cbn't consume");

        byte[] buf = new byte[1024];
        // rebd & ignore all the content.
        while(contentLength > 0) {
            int toRebd = Math.min(buf.length, contentLength);
            int rebd = _input.read(buf, 0, toRead);
            if(rebd == -1)
                brebk;
            contentLength -= rebd;
        }
        return ConnectionStbtus.getConnected();
    }           

    /*
     * Rebds the headers from this, setting _initialReadingPoint and
     * _bmountToRead.  Throws any of the exceptions listed in connect().  
     */
	privbte void readHeaders() throws IOException {
		if (_byteRebder == null) 
			throw new RebderIsNullException();

		// Rebd the response code from the first line and check for any errors
		String str = _byteRebder.readLine();  
		if (str==null || str.equbls(""))
            throw new IOException();

        if (_inNetwork)
            BbndwidthStat.HTTP_HEADER_DOWNSTREAM_INNETWORK_BANDWIDTH.addData(str.length());
        else 
            BbndwidthStat.HTTP_HEADER_DOWNSTREAM_BANDWIDTH.addData(str.length());
        
        int code=pbrseHTTPCode(str, _rfd);	
        //Note: According to the specificbtion there are 5 headers, LimeWire
        //ignores 2 of them - queue length, bnd maxUploadSlots.
        int[] refQueueInfo = {-1,-1,-1};
        //Now rebd each header...
		while (true) {            
			str = _byteRebder.readLine();
            if (str==null || str.equbls(""))
                brebk;
            
            if (_inNetwork)
                BbndwidthStat.HTTP_HEADER_DOWNSTREAM_INNETWORK_BANDWIDTH.addData(str.length());
            else 
                BbndwidthStat.HTTP_HEADER_DOWNSTREAM_BANDWIDTH.addData(str.length());
            //As of LimeWire 1.9, we ignore the "Content-length" hebder for
            //hbndling normal download flow.  The Content-Length is only
            //used for rebding/discarding some HTTP body messages.
			
            //For "Content-Rbnge" headers, we store what the remote side is
            //going to give us.  Users should exbmine the interval and
            //updbte external structures appropriately.
            if (str.toUpperCbse().startsWith("CONTENT-RANGE:")) {
                Intervbl responseRange = parseContentRange(str);
                int low = responseRbnge.low;
                int high = responseRbnge.high + 1;
                synchronized(this) {
                    // were we stolen from in the mebntime?
                    if (_disconnect)
                        throw new IOException("stolen from");
                    
                    // Mbke sure that the range they gave us is a subrange
                    // of whbt we wanted in the first place.
                    if(low < _initiblReadingPoint ||
                            high > _initiblReadingPoint + _amountToRead)
                        throw new ProblemRebdingHeaderException(
                                "invblid subrange given.  wanted low: " + 
                                _initiblReadingPoint + ", high: " + 
                                (_initiblReadingPoint + _amountToRead - 1) +
                                "... given low: " + low + ", high: " + high);                
                    _initiblReadingPoint = low;
                    _bmountToRead = high - low;
                }
            }
            else if(HTTPHebderName.CONTENT_LENGTH.matchesStartOfString(str))
                _contentLength = rebdContentLength(str);
            else if(HTTPHebderName.CONTENT_URN.matchesStartOfString(str))
				checkContentUrnHebder(str, _rfd.getSHA1Urn());
            else if(HTTPHebderName.GNUTELLA_CONTENT_URN.matchesStartOfString(str))
				checkContentUrnHebder(str, _rfd.getSHA1Urn());
			else if(HTTPHebderName.ALT_LOCATION.matchesStartOfString(str))
                rebdAlternateLocations(str);
            else if(HTTPHebderName.QUEUE.matchesStartOfString(str)) 
                pbrseQueueHeaders(str, refQueueInfo);
            else if (HTTPHebderName.SERVER.matchesStartOfString(str)) 
                _server = rebdServer(str);
            else if (HTTPHebderName.AVAILABLE_RANGES.matchesStartOfString(str))
                pbrseAvailableRangesHeader(str, _rfd);
            else if (HTTPHebderName.RETRY_AFTER.matchesStartOfString(str)) 
                pbrseRetryAfterHeader(str, _rfd);
            else if (HTTPHebderName.CREATION_TIME.matchesStartOfString(str))
                pbrseCreationTimeHeader(str, _rfd);
            else if (HTTPHebderName.FEATURES.matchesStartOfString(str))
            	pbrseFeatureHeader(str);
            else if (HTTPHebderName.THEX_URI.matchesStartOfString(str))
                pbrseTHEXHeader(str);
            else if (HTTPHebderName.FALT_LOCATION.matchesStartOfString(str))
            	pbrseFALTHeader(str);
            else if (HTTPHebderName.PROXIES.matchesStartOfString(str))
                pbrseProxiesHeader(str);
            
        }

		//Accept bny 2xx's, but reject other codes.
		if ( (code < 200) || (code >= 300) ) {
			if (code == 404) // file not found
				throw new 
				    com.limegroup.gnutellb.downloader.FileNotFoundException();
			else if (code == 410) // not shbred.
				throw new NotShbringException();
            else if (code == 416) {//requested rbnge not available
                //See if the uplobder is up to mischief
                if(_rfd.isPbrtialSource()) {
                    Iterbtor iter = _rfd.getAvailableRanges().getAllIntervals();
                    while(iter.hbsNext()) {
                        Intervbl next = (Interval)iter.next();
                        if(_requestedIntervbl.isSubrange(next))
                            throw new 
                            ProblemRebdingHeaderException("Bad ranges sent");
                    }
                }
                else {//Uplobder sent 416 and no ranges
                    throw new ProblemRebdingHeaderException("no ranges sent");
                }
                //OK. The uplobder is not messing with us.
                throw new RbngeNotAvailableException();
            }
			else if (code == 503) { // busy or queued, or rbnge not available.
                int min = refQueueInfo[0];
                int mbx = refQueueInfo[1];
                int pos = refQueueInfo[2];
                if(min != -1 && mbx != -1 && pos != -1)
                    throw new QueuedException(min,mbx,pos);
                    
                // per the PFSP spec, b 503 should be returned. But if the
                // uplobder returns a "Avaliable-Ranges" header regardless of
                // whether it is reblly busy or just does not have the requested
                // rbnge, we cannot really distingush between the two cases on
                // the client side.
                
                //For the most pbrt clients send 416 when they have other ranges
                //thbt may match the clients need. From LimeWire 4.0.6 onwards
                //LimeWire will trebte 503s to mean either busy or queued BUT
                //NOT pbrtial range available.

                //if( _rfd.isPbrtialSource() )
                //throw new RbngeNotAvailableException();
                    
                //no QueuedException or RbngeNotAvailableException? not queued.
                //throw b generic busy exception.
				throw new TryAgbinLaterException();

                // b general catch for 4xx and 5xx's
                // should mbybe be a different exception?
                // else if ( (code >= 400) && (code < 600) ) 
            }
			else // unknown or unimportbnt
				throw new UnknownCodeException(code);			
		}        
    }

	/**
     * Does nothing except for throwing bn exception if the
     * X-Gnutellb-Content-URN header does not match
     * 
     * @pbram str
     *            the hebder <tt>String</tt>
     * @pbram sha1
     *            the <tt>URN</tt> we expect
     * @throws ContentUrnMismbtchException
     */
    privbte void checkContentUrnHeader(String str, URN sha1)
        throws ContentUrnMismbtchException {
        String vblue = HTTPUtils.extractHeaderValue(str);
        if (_root32 == null && vblue.indexOf("urn:bitprint:") > -1) { 
            // If the root32 wbs not in the X-Thex-URI header
            // (the spec requires it be there), then stebl it from
            // the content-urn if it wbs a bitprint.
            _root32 = vblue.substring(value.lastIndexOf(".")+1).trim();
        }

        if(shb1 == null)
            return;
        
        
        URN contentUrn = null;
        try {
            contentUrn = URN.crebteSHA1Urn(value);
        } cbtch (IOException ioe) {
            // could be bn URN type we don't know. So ignore all
            return;
        }
        if (!shb1.equals(contentUrn))
            throw new ContentUrnMismbtchException();
        // else do nothing bt all.
    }
	
	/**
	 * Rebds alternate location header.  The header can contain only one
	 * blternate location, or it can contain many in the same header.
	 * This method bdds them all to the <tt>FileDesc</tt> for this
	 * uplobder.  This will not allow more than 20 alternate locations
	 * for b single file.
	 * 
	 * Since uplobders send only good alternate locations, we add merge
	 * proxies to the existing sets.
	 *
	 * @pbram altHeader the full alternate locations header
	 */
	privbte void readAlternateLocations(final String altHeader) {
		finbl String altStr = HTTPUtils.extractHeaderValue(altHeader);
		if(bltStr == null)
		    return;

        finbl URN sha1 = _rfd.getSHA1Urn();
        if(shb1 == null)
            return;
            
		StringTokenizer st = new StringTokenizer(bltStr, ",");
		while(st.hbsMoreTokens()) {
			try {
				AlternbteLocation al =
					AlternbteLocation.create(st.nextToken().trim(), sha1);
                Assert.thbt(al.getSHA1Urn().equals(sha1));
                if (bl.isMe())
                    continue;
                
                RemoteFileDesc rfd = bl.createRemoteFileDesc(_rfd.getSize());
                
                if(_locbtionsReceived.add(rfd)) {
                    if (bl instanceof DirectAltLoc)
                        DownlobdStat.ALTERNATE_COLLECTED.incrementStat();
                    else
                        DownlobdStat.PUSH_ALTERNATE_COLLECTED.incrementStat();
                }
			} cbtch(IOException e) {
				// continue without bdding it.
				continue;
			}
		}
	}
	
	/**
	 * Determines whether or not the pbrtial file is valid for us
	 * to bdd ourselves to the mesh.
	 *
	 * Checks the following:
	 *  - RFD hbs a SHA1.
	 *  - We bre allowing partial sharing
	 *  - We hbve successfully verified at least certain size of the file
	 *  - Our port bnd IP address are valid 
	 */
	privbte boolean isPartialFileValid() {
	    return _rfd.getSHA1Urn() != null && 
               _incompleteFile.getVerifiedBlockSize() > MIN_PARTIAL_FILE_BYTES &&
               UplobdSettings.ALLOW_PARTIAL_SHARING.getValue() &&
               NetworkUtils.isVblidPort(RouterService.getPort()) &&
               NetworkUtils.isVblidAddress(RouterService.getAddress()); 
    }
	
	/**
	 * Rebds the Server header.  All information after the ':' is considered
	 * to be the Server.
	 */
	public stbtic String readServer(final String serverHeader) {
	    int colon = serverHebder.indexOf(':');
	    // if it existed & wbsn't at the end...
	    if ( colon != -1 && colon < serverHebder.length()-1 )
	        return serverHebder.substring(colon+1).trim();
        else
            return "";
    }
    
    /**
     * Rebds the Content-Length.  Invalid Content-Lengths are set to 0.
     */
    public stbtic int readContentLength(final String header) {
        String vblue = HTTPUtils.extractHeaderValue(header);
        if(vblue == null)
            return 0;
        else {
            try {
                return Integer.pbrseInt(value.trim());
            } cbtch(NumberFormatException nfe) {
                return 0;
            }
        }
    }

    /**
     * Returns the HTTP response code from the given string, throwing
     * bn exception if it couldn't be parsed.
     *
     * @pbram str an HTTP response string, e.g., "HTTP/1.1 200 OK \r\n"
     * @exception NoHTTPOKException str didn't contbin "HTTP"
     * @exception ProblemRebdingHeaderException some other problem
     *  extrbcting result code
     */
    privbte static int parseHTTPCode(String str, RemoteFileDesc rfd) 
                                                         throws IOException {
		StringTokenizer tokenizer = new StringTokenizer(str, " ");		
		String token;

		// just b safety
		if (! tokenizer.hbsMoreTokens() )
			throw new NoHTTPOKException();

		token = tokenizer.nextToken();
		
		// the first token should contbin HTTP
		if (token.toUpperCbse().indexOf("HTTP") < 0 )
			throw new NoHTTPOKException("got: " + str);
        // does the server support http 1.1?
        else 
            rfd.setHTTP11( token.indexOf("1.1") > 0 );
		
		// the next token should be b number
		// just b safety
		if (! tokenizer.hbsMoreTokens() )
			throw new NoHTTPOKException();

		token = tokenizer.nextToken();
		
		String num = token.trim();
		try {
			return jbva.lang.Integer.parseInt(num);
		} cbtch (NumberFormatException e) {
			throw new ProblemRebdingHeaderException(e);
		}
    }

    /** 
     * Rebds the X-Queue headers from str, storing fields in refQueueInfo.
     * @pbram str a header value of form 
     *  "X-Queue: position=2,length=5,limit=4,pollMin=45,pollMbx=120"
     * @pbram refQueueInfo an array of 3 elements to store results.
     *  refQueueInfo[0] is set to the vblue of pollMin, or -1 if problems;
     *  refQueueInfo[1] is set to the vblue of pollMax, or -1 if problems;
     *  refQueueInfo[2] is set to the vblue of position, or -1 if problems; 
     */
    privbte void parseQueueHeaders(String str, int[] refQueueInfo)  {
        //Note: According to the specificbtion there are 5 headers, LimeWire
        //ignores 2 of them - queue length, bnd maxUploadSlots.        
        if(str==null)
            return;
        StringTokenizer tokenizer = new StringTokenizer(str," ,:=");
        if(!tokenizer.hbsMoreTokens())  //no tokens on new line??
            return;
        
        String token = tokenizer.nextToken();
        if(!token.equblsIgnoreCase("X-Queue"))
            return;

        while(tokenizer.hbsMoreTokens()) {
            token = tokenizer.nextToken();
            String vblue;
            try {
                if(token.equblsIgnoreCase("pollMin")) {
                    vblue = tokenizer.nextToken();
                    refQueueInfo[0] = Integer.pbrseInt(value);
                }
                else if(token.equblsIgnoreCase("pollMax")) {
                    vblue = tokenizer.nextToken();
                    refQueueInfo[1] = Integer.pbrseInt(value);
                }
                else if(token.equblsIgnoreCase("position")) {
                    vblue = tokenizer.nextToken();
                    refQueueInfo[2] = Integer.pbrseInt(value);
                }
            } cbtch(NumberFormatException nfx) {//bad headers drop connection
                //We could return bt this point--basically does the same things.
                Arrbys.fill(refQueueInfo,-1);
            } cbtch(NoSuchElementException nsex) {//bad headers drop connection
                //We could return bt this point--basically does the same things.
                Arrbys.fill(refQueueInfo,-1);
            }
        } //end of while - done pbrsing this line.
    }

    /**
     * Returns the intervbl of the responding content range.
     * If the full content rbnge (start & stop interval) is not given,
     * we bssume it to be the interval that we requested.
     * The returned intervbl's low & high ranges are both INCLUSIVE.
     *
     * Does not strictly enforce HTTP; bllows minor errors like replacing the
     * spbce after "bytes" with an equals.  Also tries to interpret malformed 
     * LimeWire 0.5 hebders.
     *
     * @pbram str a Content-range header line, e.g.,
     *      "Content-rbnge: bytes 0-9/10" or
     *      "Content-rbnge:bytes 0-9/10" or
     *      "Content-rbnge:bytes 0-9/X" (replacing X with "*") or
     *      "Content-rbnge:bytes X/10" (replacing X with "*") or
     *      "Content-rbnge:bytes X/X" (replacing X with "*") or
     *  Will blso accept the incorrect but common 
     *      "Content-rbnge: bytes=0-9/10"
     * @exception ProblemRebdingHeaderException some problem
     *  extrbcting the start offset.  
     */
    privbte Interval parseContentRange(String str) throws IOException {
        int numBeforeDbsh;
        int numBeforeSlbsh;
        int numAfterSlbsh;

        if (LOG.isDebugEnbbled())
            LOG.debug("rebding content range: "+str);
        
        //Try to pbrse all three numbers from header for verification.
        //Specibl case "*" before or after slash.
        try {
            int stbrt=str.indexOf("bytes")+6;  //skip "bytes " or "bytes="
            int slbsh=str.indexOf('/');
            
            //"bytes */*" or "bytes */10"
            // We don't know whbt we're getting, but it'll start at 0.
            // Assume thbt we're going to get until the part we requested.
            // If we rebd more, good.  If we read less, it'll work out just
            // fine.
            if (str.substring(stbrt, slash).equals("*")) {
                if(LOG.isDebugEnbbled())
                    LOG.debug(_rfd + " Content-Rbnge like */?, " + str);
                synchronized(this) {
                    return new Intervbl(0, _amountToRead - 1);
                }
            }

            int dbsh=str.lastIndexOf("-");     //skip past "Content-range"
            numBeforeDbsh=Integer.parseInt(str.substring(start, dash));
            numBeforeSlbsh=Integer.parseInt(str.substring(dash+1, slash));

            if(numBeforeSlbsh < numBeforeDash)
                throw new ProblemRebdingHeaderException(
                    "invblid range, high (" + numBeforeSlash +
                    ") less thbn low (" + numBeforeDash + ")");

            //"bytes 0-9/*"
            if (str.substring(slbsh+1).equals("*")) {
                if(LOG.isDebugEnbbled())
                    LOG.debug(_rfd + " Content-Rbnge like #-#/*, " + str);

                return new Intervbl(numBeforeDash, numBeforeSlash);
            }

            numAfterSlbsh=Integer.parseInt(str.substring(slash+1));
        } cbtch (IndexOutOfBoundsException e) {
            throw new ProblemRebdingHeaderException(str);
        } cbtch (NumberFormatException e) {
            throw new ProblemRebdingHeaderException(str);
        }

        // In order to be bbckwards compatible with
        // LimeWire 0.5, which sent broken hebders like:
        // Content-rbnge: bytes=1-67818707/67818707
        //
        // If the number preceding the '/' is equbl 
        // to the number bfter the '/', then we want
        // to decrement the first number bnd the number
        // before the '/'.
        if (numBeforeSlbsh == numAfterSlash) {
            numBeforeDbsh--;
            numBeforeSlbsh--;
        }
        
        if(LOG.isDebugEnbbled())
            LOG.debug(_rfd + " Content-Rbnge like #-#/#, " + str);
        return new Intervbl(numBeforeDash, numBeforeSlash);
    }

    /**
     * Pbrses X-Available-Ranges header and stores the available ranges as a
     * list.
     * 
     * @pbram line the X-Available-Ranges header line which should look like:
     *         "X-Avbilable-Ranges: bytes A-B, C-D, E-F"
     *         "X-Avbilable-Ranges:bytes A-B"
     * @pbram rfd the RemoteFileDesc2 for the location we are trying to download
     *         from. We need this to store the bvailable Ranges. 
     * @exception ProblemRebdingHeaderException when we could not parse the 
     *         hebder line.
     */
    privbte void parseAvailableRangesHeader(String line, RemoteFileDesc rfd) 
                                                            throws IOException {
        IntervblSet availableRanges = new IntervalSet();

        line = line.toLowerCbse();
        // stbrt parsing after the word "bytes"
        int stbrt = line.indexOf("bytes") + 6;
        // if stbrt == -1 the word bytes has not been found
        // if stbrt >= line.length we are at the end of the 
        // hebder line
        while (stbrt != -1 && start < line.length()) {
            // try to pbrse the number before the dash
            int stop = line.indexOf('-', stbrt);
            // test if this is b valid interval
            if ( stop == -1 )
                brebk; 

            // this is the intervbl to store the available 
            // rbnge we are parsing in.
            Intervbl interval = null;
    
            try {
                // rebd number before dash
                // bytes A-B, C-D
                //       ^
                int low = Integer.pbrseInt(line.substring(start, stop).trim());
                
                // now moving the stbrt index to the 
                // chbracter after the dash:
                // bytes A-B, C-D
                //         ^
                stbrt = stop + 1;
                // we bre parsing the number before the comma
                stop = line.indexOf(',', stbrt);
                
                // If we bre at the end of the header line, there is no comma 
                // following.
                if ( stop == -1 )
                    stop = line.length();
                
                // rebd number after dash
                // bytes A-B, C-D
                //         ^
                int high = Integer.pbrseInt(line.substring(start, stop).trim());

                // stbrt parsing after the next comma. If we are at the
                // end of the hebder line start will be set to 
                // line.length() +1
                stbrt = stop + 1;
                
                if(high >= rfd.getSize())
                    high = rfd.getSize()-1;

                if(low > high)//intervbl read off network is bad, try next one
                    continue;

                // this intervbl should be inclusive at both ends
                intervbl = new Interval( low, high );
                
            } cbtch (NumberFormatException e) {
                throw new ProblemRebdingHeaderException(e);
            }
            bvailableRanges.add(interval);
        }
        rfd.setAvbilableRanges(availableRanges);
    }

    /**
     * Pbrses the Retry-After header.
     * @pbram str - expects a simple integer number specifying the
     * number of seconds to wbit before retrying the host.
     * @exception ProblemRebdingHeaderException if we could not read 
     * the hebder
     */
    privbte static void parseRetryAfterHeader(String str, RemoteFileDesc rfd) 
        throws IOException {
        str = HTTPUtils.extrbctHeaderValue(str);
        int seconds = 0;
        try {
            seconds = Integer.pbrseInt(str);
        } cbtch (NumberFormatException e) {
            throw new ProblemRebdingHeaderException(e);
        }
        // mbke sure the value is not smaller than MIN_RETRY_AFTER seconds
        seconds = Mbth.max(seconds, MIN_RETRY_AFTER);
        // mbke sure the value is not larger than MAX_RETRY_AFTER seconds
        seconds = Mbth.min(seconds, MAX_RETRY_AFTER);
        rfd.setRetryAfter(seconds);
    }
    
    /**
     * Pbrses the Creation Time header.
     * @pbram str - expects a long number specifying the age in milliseconds
     * of this file.
     * @exception ProblemRebdingHeaderException if we could not read 
     * the hebder
     */
    privbte static void parseCreationTimeHeader(String str, RemoteFileDesc rfd) 
        throws IOException {
        str = HTTPUtils.extrbctHeaderValue(str);
        long milliSeconds = 0;
        try {
            milliSeconds = Long.pbrseLong(str);
        } cbtch (NumberFormatException e) {
            throw new ProblemRebdingHeaderException(e);
        }
        if (rfd.getSHA1Urn() != null) {
            CrebtionTimeCache ctCache = CreationTimeCache.instance();
            synchronized (ctCbche) {
                Long cTime = ctCbche.getCreationTime(rfd.getSHA1Urn());
                // prefer older times....
                if ((cTime == null) || (cTime.longVblue() > milliSeconds))
                    ctCbche.addTime(rfd.getSHA1Urn(), milliSeconds);
            }
        }
    }
    
    /**
     * This method rebds the "X-Features" header and looks for features we
     * understbnd.
     * 
     * @pbram str
     *            the hebder line.
     */
    privbte void parseFeatureHeader(String str) {
        str = HTTPUtils.extrbctHeaderValue(str);
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
            // ignore the version for now.

            if (protocol.equbls(HTTPConstants.CHAT_PROTOCOL))
                _chbtEnabled = true;
            else if (protocol.equbls(HTTPConstants.BROWSE_PROTOCOL))
                _browseEnbbled = true;
            else if (protocol.equbls(HTTPConstants.PUSH_LOCS))
            	_wbntsFalts=true;
            else if (protocol.equbls(HTTPConstants.FW_TRANSFER)) {
                //for this hebder we care about the version
                int FWTVersion=0;
                try{
                    FWTVersion = (int)HTTPUtils.pbrseFeatureToken(feature);
                    _wbntsFalts=true;
                }cbtch(ProblemReadingHeaderException prhe) {
                    //ignore this hebder
                    continue;
                }

                // try to updbte the FWT version and external address we know for this host
            	try {
            	    updbtePEAddress();
            	    PushEndpoint.setFWTVersionSupported(_rfd.getClientGUID(),FWTVersion);
                } cbtch (IOException ignored) {}
            }
        }
    }

    /**
     * Method for rebding the X-Thex-Uri header.
     */
    privbte void parseTHEXHeader (String str) {
        if(LOG.isDebugEnbbled())
            LOG.debug(_host + ":" + _port +">" + str);

        str = HTTPUtils.extrbctHeaderValue(str);
        if (str.indexOf(";") > 0) {
            StringTokenizer tok = new StringTokenizer(str, ";");
            _thexUri = tok.nextToken();
            _root32 = tok.nextToken();
        } else
            _thexUri = str;
    }    
    
    /**
     * 
     * Method for pbrsing the header containing firewalled alternate
     * locbtions.  The format is a modified version of the one described
     * in the push proxy spec bt the_gdf
     * 
     */
    privbte void parseFALTHeader(String str) {
    	//if we entered this method mebns the other side is interested
    	//in receiving firewblled locations.
    	_wbntsFalts=true;
    	
    	//this just delegbtes to readAlternateLocationHeader
    	rebdAlternateLocations(str);
    }
      
    
    /**
     * pbrses the header containing the current set of push proxies for 
     * the given host, bnd updates the rfd
     */
    privbte void parseProxiesHeader(String str) {
        str = HTTPUtils.extrbctHeaderValue(str);
        
        if (_rfd.getPushAddr()==null || str==null || str.length()<12) 
            return;
        
        try {
            PushEndpoint.overwriteProxies(_rfd.getClientGUID(),str);
            updbtePEAddress();
        }cbtch(IOException tooBad) {
            // invblid header - ignore it.
        }
        
    }
    
    privbte void updatePEAddress() throws IOException {
        IpPort newAddr = new IpPortImpl(_socket.getInetAddress().getHostAddress(),_socket.getPort()); 
        if (NetworkUtils.isVblidExternalIpPort(newAddr))
            PushEndpoint.setAddr(_rfd.getClientGUID(),newAddr);
    }
    
    /////////////////////////////// Downlobd ////////////////////////////////

    /*
     * Downlobds the content from the server and writes it to a temporary
     * file.  Blocking.  This MUST be initiblized via connect() beforehand, and
     * doDownlobd MUST NOT have already been called. If there is
     * b mismatch in overlaps, the VerifyingFile triggers a callback to
     * the MbnagedDownloader, which triggers a callback to the GUI to let us
     * know whether to continue or interrupt.
     *  
     * @exception IOException downlobd was interrupted, typically (but not
     *  blways) because the other end closed the connection.
     */
	public void doDownlobd() 
        throws DiskException, IOException {
        
        _socket.setSoTimeout(1*60*1000);//downlobding, can stall upto 1 mins
        
        long currPos = _initiblReadingPoint;
        try {
            
            int c = -1;
            byte[] buf = new byte[BUF_LENGTH];
            
            while (true) {
                //Rebd from network.  It's possible that we've read more than
                //requested becbuse of a call to setAmountToRead() or stopAt() from another
                //threbd.  We check for that before we write to disk.
                
                // first see how much we hbve left to read, if any
                int left;
                synchronized(this) {
                    if (_bmountRead >= _amountToRead) {
                        _isActive = fblse;
                        brebk;
                    }
                    left = _bmountToRead - _amountRead;
                }
                
                Assert.thbt(left>0);

                // do the bctual read from the network using the appropriate bandwidth 
                // throttle
                BbndwidthThrottle throttle = _socket instanceof UDPConnection ?
                    UDP_THROTTLE : THROTTLE;
                int toRebd = throttle.request(Math.min(BUF_LENGTH, left));
                c = _byteRebder.read(buf, 0, toRead);
                if (c == -1) 
                    brebk;
                
                if (_inNetwork)
                    BbndwidthStat.HTTP_BODY_DOWNSTREAM_INNETWORK_BANDWIDTH.addData(c);
                else
                    BbndwidthStat.HTTP_BODY_DOWNSTREAM_BANDWIDTH.addData(c);

				synchronized(this) {
				    if (_isActive) {
                        
                        // skip until we rebch the initial writing point
                        int skipped = 0;
                        while (_initiblWritingPoint > currPos && c > 0) {
                            skipped++;
                            currPos++;
                            c--;
                            _bmountRead++;
                        }
                        
                        // if we're still not there, continue
                        if (_initiblWritingPoint > currPos || c == 0) {
                            if (LOG.isDebugEnbbled())
                                LOG.debug("skipped "+skipped+" bytes");
                            
                            continue;
                        }
                        
                        // if bre past our initial writing point, but we had to skip some bytes 
                        // or were told to stop sooner, trim the buffer
                        if (skipped > 0 || _bmountRead+c >= _amountToRead) {
                            c = Mbth.min(c,_amountToRead - _amountRead);
                            if (LOG.isDebugEnbbled())
                                LOG.debug("trimming buffer by "+
                                        skipped +" to "+c+" bytes");
                            
                            byte [] temp = new byte[c];
                            System.brraycopy(buf,skipped,temp,0,c);
                            System.brraycopy(temp,0,buf,0,temp.length);
                        } 
                        
                        // write to disk
                        try {
                            _incompleteFile.writeBlock(currPos,c,buf);
                        } cbtch (InterruptedException killed) {
                            _isActive = fblse;
                            brebk;
                        }
				        
                        _bmountRead+=c;
				        currPos += c;//updbte the currPos for next iteration
				        
				    }
				    else {
				        if (LOG.isDebugEnbbled())
				            LOG.debug("WORKER:"+this+" stopping bt "+(_initialReadingPoint+_amountRead));
				        brebk;
				    }
				} 
            }  // end of while loop

            synchronized(this) {
                _isActive = fblse;
                if ( _bmountRead < _amountToRead ) { 
                    throw new FileIncompleteException();  
                }
            }
            
        } finblly {
            _bodyConsumed = true;
            synchronized(this) {
                _isActive = fblse;
            }
            if(!isHTTP11() || _disconnect) 
                throw new IOException("stolen from");
        }
	}


    /** 
     * Stops this immedibtely.  This method is always safe to call.
     *     @modifies this
     */
	public void stop() {
	    synchronized(this) {
	        if (LOG.isDebugEnbbled())
	            LOG.debug("WORKER:"+this+" signbled to stop at "+(_initialReadingPoint+_amountRead));
	        _isActive = fblse;
	    }
        if (_byteRebder != null)
            _byteRebder.close();
        try {
            if (_socket != null)
                _socket.close();
        } cbtch (IOException e) { }
        try {
            if(_input != null)
                _input.close();
        } cbtch(IOException e) {}
        try {
            if(_output != null)
                _output.close();
        } cbtch(IOException e) {}
	}

    /**
     * Instructs this stop just before rebding the given byte.
     * This cbnnot be used to increase the initial range.
     * @pbram stop the index just past the last byte to read;
     *  stop-1 is the index of the lbst byte to be downloaded
     */
    public synchronized void stopAt(int stop) {
        _disconnect = true;
        _bmountToRead = Math.min(_amountToRead,stop-_initialReadingPoint);
    }
    
    public synchronized void stbrtAt(int start) {
        _initiblWritingPoint = start;
    }
    
    synchronized void forgetRbnges() {
    	_initiblWritingPoint = 0;
    	_initiblReadingPoint = 0;
    	_bmountToRead = 0;
    	_totblAmountRead += _amountRead;
    	_bmountRead = 0;
    }
    
    ///////////////////////////// Accessors ///////////////////////////////////

    public synchronized int getInitiblReadingPoint() {return _initialReadingPoint;}
    public synchronized int getInitiblWritingPoint() {return _initialWritingPoint;}
	public synchronized int getAmountRebd() {return _amountRead;}
	public synchronized int getTotblAmountRead() {return _totalAmountRead + _amountRead;}
	public synchronized int getAmountToRebd() {return _amountToRead;}
	public synchronized boolebn isActive() { return _isActive; }
    synchronized boolebn isVictim() {return _disconnect; }

    /** 
     * Forces this to not write pbst the given byte of the file, if it has not
     * blready done so. Typically this is called to reduce the download window;
     * doing otherwise will typicblly result in incomplete downloads.
     * 
     * @pbram stop a byte index into the file, using 0 to N-1 notation.  */
    public InetAddress getInetAddress() {return _socket.getInetAddress();}
	public boolebn chatEnabled() {
		return _chbtEnabled;
	}

	public boolebn browseEnabled() {
		return _browseEnbbled;
	}
	
	/**
	 * @return whether the remote host is interested in receiving
	 * firewblled alternate locations.
	 */
	public boolebn wantsFalts() {
		return _wbntsFalts;
	}
	
	public String getVendor() { return _server; }

	public long getIndex() {return _index;}
  	public String getFileNbme() {return _filename;}
  	public byte[] getGUID() {return _guid;}
	public int getPort() {return _port;}
	
    /**
     * Returns the RemoteFileDesc pbssed to this' constructor.
     */
    public RemoteFileDesc getRemoteFileDesc() {return _rfd;}
    
    /**
     * Returns true iff this is b push download.
     */
    public boolebn isPush() {return _isPush;}
    
    /**
     *  returns true if we hbve think that the server 
     *  supports HTTP1.1 
     */
    public boolebn isHTTP11() {
        return _rfd.isHTTP11();
    }
    
    /**
     * Returns TRUE if this downlobder has a THEX tree that we have not yet
     * retrieved.
     */
    public boolebn hasHashTree() {
        return _thexUri != null 
            && _root32 != null
            && !_rfd.hbsTHEXFailed()
            && !_thexSucceeded;
    }

    /////////////////////Bbndwidth tracker interface methods//////////////
    public void mebsureBandwidth() {
        int totblAmountRead = 0;
        synchronized(this) {
            if (!_isActive)
                return;
            totblAmountRead = getTotalAmountRead();
        }
        
        bbndwidthTracker.measureBandwidth(totalAmountRead);
    }

    public flobt getMeasuredBandwidth() throws InsufficientDataException {
        return bbndwidthTracker.getMeasuredBandwidth();
    }
    
    public flobt getAverageBandwidth() {
        return bbndwidthTracker.getAverageBandwidth();
    }
            
    /**
     * Set bbndwidth limitation for downloads.
     */
    public stbtic void setRate(float bytesPerSecond) {
        THROTTLE.setRbte(bytesPerSecond);
        UDP_THROTTLE.setRbte(bytesPerSecond);
    }
    
    /**
     * Apply bbndwidth limitation from settings.
     */
    public stbtic void applyRate() {
        flobt downloadRate = Float.MAX_VALUE;
        int downlobdThrottle = DownloadSettings.DOWNLOAD_SPEED.getValue();
        
        if ( downlobdThrottle < 100 )
        {
            downlobdRate = (((float)downloadThrottle/100.f)*
             ((flobt)ConnectionSettings.CONNECTION_SPEED.getValue()/8.f))*1024.f;
        }
        setRbte( downloadRate );
    }
    
    
	////////////////////////////// Unit Test ////////////////////////////////

    public String toString() {
        return "<"+_host+":"+_port+", "+getFileNbme()+">";
    }
    
    public stbtic void setThrottleSwitching(boolean on) {
        THROTTLE.setSwitching(on);
        // DO NOT PUT SWITCHING ON THE UDP SIDE.
    }
    
	privbte HTTPDownloader(String str) {
		ByteArrbyInputStream stream = new ByteArrayInputStream(str.getBytes());
		_byteRebder = new ByteReader(stream);
		_locbtionsReceived = null;
        _goodLocs = null;
        _bbdLocs = null;
        _writtenGoodLocs = null;
        _writtenBbdLocs = null;
		_rfd =  new RemoteFileDesc("127.0.0.1", 1,
                                  0, "b", 0, new byte[16],
                                  0, fblse, 0, false, null, null,
                                  fblse, false, "", 0, null, -1, 0);
        _incompleteFile = null;
        _inNetwork = fblse;
	}    
}










