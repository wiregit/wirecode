package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.http.*;
import com.limegroup.gnutella.statistics.*;
import com.limegroup.gnutella.settings.ChatSettings;
import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.gnutella.util.Sockets;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.http.HTTPHeaderValueCollection;
import java.io.*;
import java.net.*;
import java.util.StringTokenizer;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.altlocs.*;

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
    /**
     * The length of the buffer used in downloading.
     */
    public static final int BUF_LENGTH=1024;
    
    
    /**
     * The smallest possible file to be shared with partial file sharing.
     * Non final for testing purposes.
     */
    static int MIN_PARTIAL_FILE_BYTES = 1*1024*1024; // 1MB
    
    private RemoteFileDesc _rfd;
    private boolean _isPush;
	private long _index;
	private String _filename; 
	private byte[] _guid;

    /**
     * The total amount we've downloaded, including all previous 
     * HTTP connections
     */
    private volatile int _totalAmountRead;
    /** The amount we've downloaded. */
	private volatile int _amountRead;
    /** The amount we'll have downloaded if the download completes properly. 
     *  Note that the amount still left to download is 
     *  _amountToRead - _amountRead. */
	private volatile int _amountToRead;
    /** The index to start reading from the server and start writing to the
     *  file. */
	private int _initialReadingPoint;

	private ByteReader _byteReader;
	private Socket _socket;  //initialized in HTTPDownloader(Socket) or connect
    private File _incompleteFile;
    
    /**
     * The last state of commonOutFile.isCorrupted.
     * Used to know whether or not to add ourselves to the mesh.
     */
    private boolean _outIsCorrupted;

	/**
	 * The new alternate locations we've received for this file.
	 */
	private AlternateLocationCollection _altLocsReceived;

    /**
     *  The good locations to send the uploaders as in the alts list
     */
    private Set _goodLocs;
    
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

    
	private int _port;
	private String _host;
	
	private boolean _chatEnabled = false; // for now
    private boolean _browseEnabled = false; // also for now
    private String _server = "";

    /** For implementing the BandwidthTracker interface. */
    private BandwidthTrackerImpl bandwidthTracker=new BandwidthTrackerImpl();

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
	public HTTPDownloader(RemoteFileDesc rfd, File incompleteFile) {
        //Dirty secret: this is implemented with the push constructor!
        this(null, rfd, incompleteFile);
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
                                                          File incompleteFile) {
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
		_amountToRead = rfd.getSize();
		_port = rfd.getPort();
		_host = rfd.getHost();
		_chatEnabled = rfd.chatEnabled();
        _browseEnabled = rfd.browseHostEnabled();
        URN urn = rfd.getSHA1Urn();
        _altLocsReceived = urn==null ? null:
            AlternateLocationCollection.create(urn);
        AlternateLocationCollection s = null;
        AlternateLocationCollection f = null;
        _goodLocs = new HashSet();
        _badLocs = new HashSet();
        _writtenGoodLocs = new HashSet();
        _writtenBadLocs = new HashSet();
		_amountRead = 0;
		_totalAmountRead = 0;
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
    AlternateLocationCollection getAltLocsReceived() { 
	    return _altLocsReceived;
    }
        
    void addSuccessfulAltLoc(AlternateLocation loc) {
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
    
    void addFailedAltLoc(AlternateLocation loc) {
        //if we ever thought it was good, forget that we did, so we can write it
        //out as good again -- remove it from writtenGoodLocs if it was there
        synchronized(_goodLocs) {
            _writtenGoodLocs.remove(loc);
            _goodLocs.remove(loc);
        }
        
        synchronized(_badLocs) {
            if(!_writtenBadLocs.contains(loc))//no need to repeat to uploader
                _badLocs.add(loc); //duplicates make no difference
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
        InputStream istream=null;
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
            istream = _socket.getInputStream(); 
        } catch (IOException e) {
            throw new CantConnectException();
        }
        //Note : once we have established the TCP connection with the host we
        //want to download from we set the soTimeout. Its reset in doDownload
        //Note2 : this may throw an IOException.  
        _socket.setSoTimeout(Constants.TIMEOUT);
        _byteReader = new ByteReader(istream);
    }
    
    /** Sends a GET request using an already open socket, and reads all 
     * headers. 
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
    public void connectHTTP(int start, int stop, boolean supportQueueing) 
        throws IOException, TryAgainLaterException, FileNotFoundException, 
             NotSharingException, QueuedException, RangeNotAvailableException,
             ProblemReadingHeaderException, UnknownCodeException {

        _amountToRead = stop-start;
        _totalAmountRead += _amountRead;
        _amountRead = 0;
		_initialReadingPoint = start;
        //Write GET request and headers.  We request HTTP/1.1 since we need
        //persistence for queuing & chunked downloads.
        //(So we can't write "Connection: close".)
        OutputStream os = _socket.getOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(os);
        BufferedWriter out=new BufferedWriter(osw);
        String startRange = java.lang.String.valueOf(_initialReadingPoint);
        out.write("GET "+_rfd.getUrl().getFile()+" HTTP/1.1\r\n");
        out.write("HOST: "+_host+":"+_port+"\r\n");
        out.write("User-Agent: "+CommonUtils.getHttpServer()+"\r\n");

        if(supportQueueing) 
            out.write("X-Queue: 0.1\r\n");//we support remote queueing

        // Add ourselves to the mesh if the partial file is valid
        if( isPartialFileValid() ) {
            AlternateLocation me = AlternateLocation.create(_rfd.getSHA1Urn());
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

        out.write("Range: bytes=" + startRange + "-"+(stop-1)+"\r\n");
		if (ChatSettings.CHAT_ENABLED.getValue() &&
		    RouterService.acceptedIncomingConnection() &&
		    !NetworkUtils.isPrivateAddress(RouterService.getAddress())) {
            int port = RouterService.getPort();
            String host = NetworkUtils.ip2string(RouterService.getAddress());
            out.write("Chat: " + host + ":" + port + "\r\n");            
		}
        out.write("\r\n");
        out.flush();

        //Read response.
        readHeaders();
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
        //System.out.println("\t\t\t"+str);
		if (str==null || str.equals(""))
            throw new IOException();
		if(!CommonUtils.isJava118()) 
			BandwidthStat.HTTP_HEADER_DOWNSTREAM_BANDWIDTH.addData(str.length());
        int code=parseHTTPCode(str, _rfd);	
        //Note: According to the specification there are 5 headers, LimeWire
        //ignores 2 of them - queue length, and maxUploadSlots.
        int[] refQueueInfo = {-1,-1,-1};
        //Now read each header...
		while (true) {            
			str = _byteReader.readLine();
            //System.out.println("\t\t\t"+str);
            if (str==null || str.equals(""))
                break;
			if(!CommonUtils.isJava118()) 
				BandwidthStat.HTTP_HEADER_DOWNSTREAM_BANDWIDTH.addData(str.length());
            //As of LimeWire 1.9, we ignore the "Content-length" header;
            //handling an unexpectedly low Content-length value is no different
            //from handling premature connection termination.  Look at LimeWire
            //1.8 and earlier for parsing code.
			
            //For "Content-range" headers, we only look at the start of the
            //range, terminating the download if it's not what we expected.  We
            //ignore the ending value and length for the same reasons as
            //Content-length.  TODO3: it's possible to recover from a starting
            //range that's too small, though this will rarely come up.
            if (str.toUpperCase().startsWith("CONTENT-RANGE:")) {
				int startOffset=parseContentRangeStart(str);
                if (startOffset!=_initialReadingPoint)
                    throw new ProblemReadingHeaderException(
                        "Unexpected start offset; too dumb to recover");
            }

            // TODO: we should read the X-Gnutella-Content-URN header here

			// Read any alternate locations
			else if(HTTPHeaderName.ALT_LOCATION.matchesStartOfString(str))
                readAlternateLocations(str, false);
            else if(HTTPHeaderName.OLD_ALT_LOCS.matchesStartOfString(str))
                readAlternateLocations(str, true);
            else if(HTTPHeaderName.QUEUE.matchesStartOfString(str)) 
                parseQueueHeaders(str, refQueueInfo);
            else if (HTTPHeaderName.SERVER.matchesStartOfString(str)) 
                _server = readServer(str);
            else if (HTTPHeaderName.AVAILABLE_RANGES.matchesStartOfString(str))
                parseAvailableRangesHeader(str, _rfd);
        }


		//Accept any 2xx's, but reject other codes.
		if ( (code < 200) || (code >= 300) ) {
			if (code == 404) // file not found
				throw new 
				    com.limegroup.gnutella.downloader.FileNotFoundException();
			else if (code == 410) // not shared.
				throw new NotSharingException();
            else if (code == 416) //Shareaza's requested range not available
                throw new RangeNotAvailableException();
			else if (code == 503) { // busy or queued, or range not available.
                int min = refQueueInfo[0];
                int max = refQueueInfo[1];
                int pos = refQueueInfo[2];
                if(min != -1 && max != -1 && pos != -1)
                    throw new QueuedException(min,max,pos);
                    
                // per the PFSP spec, a 503 should be returned.
                // but, to let us distinguish between a busy &
                // a range not available, check if the partial
                // sources are filled up ...
                if( _rfd.isPartialSource() )
                    throw new RangeNotAvailableException();
                    
                //no QueuedException or RangeNotAvailableException? not queued.
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
	 * Reads alternate location header.  The header can contain only one
	 * alternate location, or it can contain many in the same header.
	 * This method adds them all to the <tt>FileDesc</tt> for this
	 * uploader.  This will not allow more than 20 alternate locations
	 * for a single file.
	 *
	 * @param altHeader the full alternate locations header
	 */
	private void readAlternateLocations(final String altHeader, boolean old) {
		final String altStr = HTTPUtils.extractHeaderValue(altHeader);
		if(altStr == null) return;
		StringTokenizer st = new StringTokenizer(altStr, ",");

		while(st.hasMoreTokens()) {
			try {
				AlternateLocation al=AlternateLocation.create(
				    st.nextToken().trim(), _rfd.getSHA1Urn());
                URN alSha1 = al.getSHA1Urn();
                if(alSha1 == null) {
                    continue;
                }
                if(_altLocsReceived == null)
                    _altLocsReceived = 
                    AlternateLocationCollection.create(alSha1);
                
                if(old)
                    al.setOld();

                boolean added = false;
                
                if(alSha1.equals(_altLocsReceived.getSHA1Urn())) {
                    synchronized(_altLocsReceived) {
                        added = _altLocsReceived.add(al);
                    }
                    if(ManagedDownloader.RECORD_STATS && added) 
                        DownloadStat.ALTERNATE_COLLECTED.incrementStat();
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
	 *  - The VerifyingFile is not corrupted
	 *  - We've accepted an incoming connection this session
	 *  - Our port and IP address are valid and not private.
	 */
	private boolean isPartialFileValid() {
	    return _rfd.getSHA1Urn() != null && 
               UploadSettings.ALLOW_PARTIAL_SHARING.getValue() &&
               !_outIsCorrupted &&
               RouterService.acceptedIncomingConnection() &&
               _incompleteFile.length() > MIN_PARTIAL_FILE_BYTES &&
               NetworkUtils.isValidPort(RouterService.getPort()) &&
               NetworkUtils.isValidAddress(RouterService.getAddress()) &&
               !NetworkUtils.isPrivateAddress(RouterService.getAddress());
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
			throw new NoHTTPOKException();
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
			throw new ProblemReadingHeaderException();
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
     * Returns the start byte offset in the given "Content-range" header,
     * throwing an exception if it couldn't be parsed.  Does not strictly
     * enforce HTTP; allows minor errors like replacing the space after "bytes"
     * with an equals.  Also tries to interpret malformed LimeWire 0.5 headers.
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
    private static int parseContentRangeStart(String str) throws IOException {
        int numBeforeDash;
        int numBeforeSlash;
        int numAfterSlash;

        //Try to parse all three numbers from header for verification.
        //Special case "*" before or after slash.
        try {
            int start=str.indexOf("bytes")+6;  //skip "bytes " or "bytes="
            int slash=str.indexOf('/');
            
            if (str.substring(start, slash).equals("*"))
                return 0;                      //"bytes */*" or "bytes */10"

            int dash=str.lastIndexOf("-");     //skip past "Content-range"
            numBeforeDash=Integer.parseInt(str.substring(start, dash));
            numBeforeSlash=Integer.parseInt(str.substring(dash+1, slash));

            if (str.substring(slash+1).equals("*"))
                return numBeforeDash; //bytes 0-9/*

            numAfterSlash=Integer.parseInt(str.substring(slash+1));
        } catch (IndexOutOfBoundsException e) {
            throw new ProblemReadingHeaderException();
        } catch (NumberFormatException e) {
            throw new ProblemReadingHeaderException();
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

        return numBeforeDash;
    }

    /**
     * Parses X-Available-Ranges header and stores the available ranges as a
     * list.
     * 
     * @param str the X-Available-Ranges header line which should look like:
     *         "X-Available-Ranges: bytes A-B, C-D, E-F"
     *         "X-Available-Ranges:bytes A-B"
     * @param rfd the RemoteFileDesc2 for the location we are trying to download
     *         from. We need this to store the available Ranges. 
     * @exception ProblemReadingHeaderException when we could not parse the 
     *         header line.
     */
    private static void parseAvailableRangesHeader(String line, 
                                                   RemoteFileDesc rfd) 
        throws IOException {
        List availableRanges = new ArrayList();
        
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
                
                // this interval should be inclusive at both ends
                interval = new Interval( low, high );
                
                // start parsing after the next comma. If we are at the
                // end of the header line start will be set to 
                // line.length() +1
                start = stop + 1;
                
            } catch (NumberFormatException e) {
                throw new ProblemReadingHeaderException();
            }
            availableRanges.add(interval);
        }
        rfd.setAvailableRanges(availableRanges);
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
	public void doDownload(VerifyingFile commonOutFile) 
        throws IOException {
        _socket.setSoTimeout(10*60*1000);//downloading, can stall upto 10 mins
        long currPos = _initialReadingPoint;
        try {
            int c = -1;
            byte[] buf = new byte[BUF_LENGTH];
            
            while (true) {
                //1. Read from network.  It's possible that we've read more than
                //requested because of a call to setAmountToRead from another
                //thread.  This used to be an error resulting in
                //FileTooLargeException.  Now we just return silently.  Note that
                //we capture _amountToRead in a local variable to prevent race
                //conditions; presumably Java can't de-optimize this.
                int atr=_amountToRead;
                if (_amountRead >= atr) 
                    break;                
                int left=atr - _amountRead;
                Assert.that(left>0);

                c = _byteReader.read(buf, 0, Math.min(BUF_LENGTH, left));
                if (c == -1) 
                    break;
                           
				if(!CommonUtils.isJava118()) 
					BandwidthStat.HTTP_BODY_DOWNSTREAM_BANDWIDTH.addData(c);
                //2. Check that data read matches any non-zero bytes already on
                //disk, i.e., from previous downloads.  Assumption: "holes" in
                //file are zeroed.  Be careful not read beyond end of file,
                //which is easy in the case of resuming.  Also note that
                //amountToCheck can be negative; the file length isn't extended
                //until the first write after a seek.
                commonOutFile.writeBlock(currPos,c, buf);
                _outIsCorrupted = commonOutFile.isCorrupted();

                currPos += c;//update the currPos for next iteration
                _amountRead += c;
            }  // end of while loop

            //It's OK to have read too much; see comment (1) above.
            if ( _amountRead < _amountToRead ) { 
                throw new FileIncompleteException();  
            }
        } finally {
            if(!isHTTP11())
                _byteReader.close();
        }
	}


    /** 
     * Stops this immediately.  This method is always safe to call.
     *     @modifies this
     */
	public void stop() {
        if (_byteReader != null)
            _byteReader.close();
        try {
            if (_socket != null)
                _socket.close();
        } catch (IOException e) { }
	}

    /**
     * Instructs this stop just before reading the given byte.
     * @param stop the index just past the last byte to read;
     *  stop-1 is the index of the last byte to be downloaded
     */
    public void stopAt(int stop) {
        _amountToRead=(stop-_initialReadingPoint);
    }


    ///////////////////////////// Accessors ///////////////////////////////////

    public int getInitialReadingPoint() {return _initialReadingPoint;}
	public int getAmountRead() {return _amountRead;}
	public int getTotalAmountRead() {return _totalAmountRead + _amountRead;}
	public int getAmountToRead() {return _amountToRead;}

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

    /////////////////////Bandwidth tracker interface methods//////////////
    public void measureBandwidth() {
        bandwidthTracker.measureBandwidth(getTotalAmountRead());
    }

    public float getMeasuredBandwidth() throws InsufficientDataException {
        try {
            return bandwidthTracker.getMeasuredBandwidth();
        } catch(InsufficientDataException ide) {
            throw ide;
        }
    }
    
    public float getAverageBandwidth() {
        return bandwidthTracker.getAverageBandwidth();
    }
            
	////////////////////////////// Unit Test ////////////////////////////////

    public String toString() {
        return "<"+_host+":"+_port+", "+getFileName()+">";
    }
    
	private HTTPDownloader(String str) {
		ByteArrayInputStream stream = new ByteArrayInputStream(str.getBytes());
		_byteReader = new ByteReader(stream);
		_altLocsReceived = null;
        _goodLocs = null;
        _badLocs = null;
        _writtenGoodLocs = null;
        _writtenBadLocs = null;
		_rfd =  new RemoteFileDesc("127.0.0.1", 1,
                                  0, "a", 0, new byte[16],
                                  0, false, 0, false, null, null,
                                  false, false, "", 0, null);
	}    
}










