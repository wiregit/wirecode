package com.limegroup.gnutella;

import com.limegroup.gnutella.uploader.*;
import com.limegroup.gnutella.util.Buffer;
import java.net.*;
import java.io.*;
import com.sun.java.util.collections.*;
import java.util.Date;
import com.limegroup.gnutella.util.URLDecoder;

/**
 * The list of all the uploads in progress.
 *
 *
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|

public class UploadManager implements BandwidthTracker {
	/** The callback for notifying the GUI of major changes. */
    private ActivityCallback _callback;
    /** The message router to use for pushes. */
    private MessageRouter _router;
    /** Used for get addresses in pushes. */
    private Acceptor _acceptor;

	/**
	 * LOCKING: obtain this' monitor before modifying any 
	 * of the data structures
	 */

	/** The maximum time in SECONDS after an unsuccessful push until we will
     *  try the push again.  This should be larger than the 15+4*30=135 sec
     *  window in which Gnotella resends pushes by default */
    private static final int PUSH_INVALIDATE_TIME=60*5;  //5 minutes
	  /**
     * The list of all files that we've tried unsuccessfully to upload
     * via pushes.  (Here successful means we were able to connect.
     * It does not mean the file was actually transferred.) If we get
     * another push request from one of these hosts (e.g., because the
     * host is firewalled and sends multiple push packets) we will not
     * try again.
     *
     * INVARIANT: for all i>j, ! failedPushes[i].before(failedPushes[j])
	 */
	private List /* of PushRequestedFile */ _failedPushes=
        new LinkedList();
    private List /* of PushRequestedFile */ _attemptingPushes=
        new LinkedList();

	/**
	 * This is a <tt>List</tt> of all of the current <tt>Uploader</tt>
	 * instances (all of the uploads in progress).  
	 */
	private List /* of Uploaders */ _activeUploadList =
		new LinkedList();

	/**
	 * The number of uploads in progress from each host. If the number
	 * of uploads by a single user exceeds the SettingsManager's
	 * uploadsPerPerson_ variable, then the upload is denied, 
	 * and the used gets a Try Again Later message.
	 */
	private static Map /* String -> Integer */ _uploadsInProgress =
		new HashMap();
    /**
     * The number of uploads that are actually transferring data.
     *
     * INVARIANT: _activeUploads is always less than or equal to the
     * summation of the values of _uploadsInProgress
     */
    private volatile int _activeUploads= 0;

	/** set to true when an upload has been succesfully completed. */
	private volatile boolean _hadSuccesfulUpload=false;


    /** The number of uploads considered when calculating capacity, if possible.
     *  BearShare uses 10.  Settings it too low causes you to be fooled be a
     *  streak of slow downloaders.  Setting it too high causes you to be fooled
     *  by a number of quick downloads before your slots become filled.  */
    private static final int MAX_SPEED_SAMPLE_SIZE=5;
    /** The min number of uploads considered to give out your speed.  Same 
     *  criteria needed as for MAX_SPEED_SAMPLE_SIZE. */
    private static final int MIN_SPEED_SAMPLE_SIZE=5;
    /** The minimum number of bytes transferred by an uploadeder to count. */
    private static final int MIN_SAMPLE_BYTES=200000;  //200KB
    /** The average speed in kiloBITs/second of the last few uploads. */
    private Buffer /* of Integer */ speeds=new Buffer(MAX_SPEED_SAMPLE_SIZE);
    /** The highestSpeed of the last few downloads, or -1 if not enough
     *  downloads have been down for an accurate sample.
     *  INVARIANT: highestSpeed>=0 ==> highestSpeed==max({i | i in speeds}) 
     *  INVARIANT: speeds.size()<MIN_SPEED_SAMPLE_SIZE <==> highestSpeed==-1
     */
    private volatile int highestSpeed=-1;

    /** The desired minimum quality of service to provide for uploads, in
     *  KB/s.  See testTotalUploadLimit. */
    private static final float MINIMUM_UPLOAD_SPEED=3.0f;
    
    private FileManager _fileManager;


   //////////////////////// Main Public Interface /////////////////////////

    /** 
     * Initializes this manager. <b>This method must be called before any other
     * methods are used.</b> 
     *     @param callback the UI callback to notify of download changes
     *     @param router the message router to use for sending push requests
     *     @param acceptor used to get my IP address and port for pushes
     */


    public void initialize(ActivityCallback callback,
                           MessageRouter router,
                           Acceptor acceptor,
                           FileManager fileManager) {
        _fileManager = fileManager;
        _callback = callback;
        _router = router;
        _acceptor = acceptor;

    }
                
	/**
	 * Accepts a new upload, creating a new <tt>HTTPUploader</tt>
	 * if it successfully parses the HTTP 'get' header.  BLOCKING.
	 *
	 * @param socket the <tt>Socket</tt> that will be used for the new upload
	 */
    public void acceptUpload(Socket socket) {

		HTTPUploader uploader;
		GETLine line;
		try {
            //increment the download count
            synchronized(this) { _activeUploads++; }
            
            //do uploads
            while(true) {
                try {
                //parse the get line
                line = parseGET(socket);
                } catch (IOException e) {
                    // the GET line was wrong, just exit.
                    return;
                }
                //create an uploader
                uploader = new HTTPUploader(line._fileName, socket, line._index, 
                    this, _fileManager);
                //do the upload
                doSingleUpload(uploader, 
                    socket.getInetAddress().getHostAddress(), line._index);
                
                //if not to keep the connection open (either due to protocol
                //not being HTTP 1.1, or due to error state, then return
                if (!line.isHTTP11() || uploader.getCloseConnection())
                    return;
                
                //read the first word of the next request
                //and proceed only if "GET" request
                try {
                    int oldTimeout = socket.getSoTimeout();
                    socket.setSoTimeout(SettingsManager.instance(
                        ).getPersistentHTTPConnectionTimeout());
                    //dont read a word of size more than 3 
                    //as we will handle only the next "GET" request
                    String word=IOUtils.readWord(socket.getInputStream(), 3);
                    socket.setSoTimeout(oldTimeout);
                    if(!word.equalsIgnoreCase("GET"))
                        return;
                } catch (IOException ioe) {
                    return;
                }
            }//end of while
        } finally {
            //decrement the download count
            synchronized(this) { _activeUploads--; }
            //close the socket
            close(socket);
        }
	}
    
    private void doSingleUpload(Uploader uploader, String host,
        int index) {
        long startTime=-1;
        try {
            // check if it complies with the restrictions.
            //and set the uploader state accordingly
            insertAndTest(uploader, host);
            // connect is always safe to call.  it should be
            // if it is a non-push upload, then connect should
            // just return.  if there is an error, it should
            // be because the connection failed.
            uploader.connect();
            // start doesn't throw an exception.  rather, it
            // handles it internally.  is this the correct
            // way to handle it?
            startTime=System.currentTimeMillis();
            uploader.start();
            // check the state of the upload once the
            // start method has finished.  if it is complete...
            if (uploader.getState() == Uploader.COMPLETE)
                // then set a flag in the upload manager...
                _hadSuccesfulUpload = true;
        } catch (IOException e) {
            // if it fails, insert it into the push failed list
            synchronized(UploadManager.this) { insertFailedPush(host, index); }
        } finally {			    
            long finishTime=System.currentTimeMillis();
            synchronized(UploadManager.this) {
                //Report how quickly we uploaded the data, regardless of
                //whether the transfer was interrupted, unless we couldn't
                //connect.  The client will ignore small amounts of data.
                if (startTime>0)
                    reportUploadSpeed(finishTime-startTime,
                                      uploader.amountUploaded());
                removeFromMapAndList(uploader, host);
                removeAttemptingPush(host, index);
                _callback.removeUpload(uploader);		
            }
        }
    }

    /**
     * closes the passed socket and its corresponding I/O streams
     */
    public void close(Socket socket) {
        //close the output streams, input streams and the socket
        try {
            if (socket != null)
                socket.getOutputStream().close();
        } catch (Exception e) {}
        try {
            if (socket != null)
                socket.getInputStream().close();
        } catch (Exception e) {}
        try {
            if (socket != null) 
                socket.close();
        } catch (Exception e) {}
    }
    
	/**
	 * Accepts a new push upload, creating a new <tt>HTTPUploader</tt>.
     * NON-BLOCKING: creates a new thread to transfer the file.
	 *
	 * @param file the fully qualified pathname of the file to upload
	 * @param host the ip address of the host to upload to
	 * @param port the port over which the transfer will occur
	 * @param index the index of the file in <tt>FileManager</tt>
	 * @param guid the unique identifying client guid of the uploading client
	 */
	public synchronized void acceptPushUpload(final String file, 
											  final String host, 
                                              final int port, 
											  final int index, 
                                              final String guid) { 
		final Uploader uploader = new HTTPUploader(file, host, port, index, 
                                                   guid, this, _fileManager);

        // Test if we are either currently attempting a push, or we have
        // unsuccessfully attempted a push with this host in the past.
		clearFailedPushes();
        if ( (! testAttemptingPush(host, index) )  ||
             (! testFailedPush(host, index) ) )
            return;
        insertAttemptingPush(host, index);        

        Thread runner=new Thread() {
            public void run() {
                try {
                    synchronized(UploadManager.this) { _activeUploads++; }
                    doSingleUpload(uploader, host, index);
                } finally {
                    //decrement the download count
                    synchronized(UploadManager.this) { _activeUploads--; }
                    //close the socket
                    uploader.stop();
                }
            }
        };
        runner.start();
	}

	/** 
     * This method was added to adopt BearShare's busy bit
     * in the Query Hit Descriptor.  It takes no parameters
	 * and returns 'true' if there are no slots available
	 * for uploading.  It returns 'false' if there are slots
	 * available.  
     */
	public synchronized boolean isBusy() {
		// testTotalUploadLimit returns true is there are
		// slots available, false otherwise.
		return (! testTotalUploadLimit());
	}

	public int uploadsInProgress() {
		return _activeUploads;
	}


	/**
	 * Returns true if this has ever successfully uploaded a file
     * during this session.<p>
     * 
     * This method was added to adopt more of the BearShare QHD
	 * standard.
	 */
	public boolean hadSuccesfulUpload() {
		return _hadSuccesfulUpload;
	} 


	/////////////////// Private Interface for Testing Limits /////////////////

    /** Increments the count of uploads in progress for host.
     *  If uploader has exceeded its limits, places it in LIMIT_REACHED state.
     *  Notifies callback of this.
     *      @modifies _uploadsInProgress, uploader, _callback */
	private synchronized void insertAndTest(Uploader uploader, String host) {
		// add to the Map
		insertIntoMapAndList(uploader, host);

		if ( (! testPerHostLimit(host) ) ||
			 ( ! testTotalUploadLimit() ) )
			 uploader.setState(Uploader.LIMIT_REACHED);
		
		_callback.addUpload(uploader);		

	}

    /** Increments the count of uploads in progress for host. 
     *      @modifies _uploadsInProgress */
	private void insertIntoMapAndList(Uploader uploader, String host) {
		int numUploads = 1;
		// check to see if the map aleady contains
		// a reference to this host.  if so, get its
		// value.
		if ( _uploadsInProgress.containsKey(host) ) {
			Integer myInteger = (Integer)_uploadsInProgress.get(host);
			numUploads += myInteger.intValue();
		}
		_uploadsInProgress.put(host, new Integer(numUploads));	
		_activeUploadList.add(uploader);
	}

	/**
	 * Decrements the number of active uploads for the host specified in
	 * the <tt>host</tt> argument, removing that host from the <tt>Map</tt>
	 * if this was the only upload allocated to that host.<p>
	 *
	 * This method also removes the <tt>Uploader</tt> from the <tt>List</tt>
	 * of active uploads.
	 */
  	private void removeFromMapAndList(Uploader uploader, String host) {
  		if ( _uploadsInProgress.containsKey(host) ) {
  			Integer myInteger = (Integer)_uploadsInProgress.get(host);
  			int numUploads = myInteger.intValue();
  			if (numUploads == 1) 
  				_uploadsInProgress.remove(host);
  			else {
  				--numUploads;
				_uploadsInProgress.put(host, new Integer(numUploads));
  			}
  		}
		_activeUploadList.remove(uploader);

		// Enable auto shutdown
		if(_activeUploads == 0)
			_callback.uploadsComplete();
  	}
	
	private boolean testPerHostLimit(String host) {
		if ( _uploadsInProgress.containsKey(host) ) {
			Integer value = (Integer)_uploadsInProgress.get(host);
			int current = value.intValue();
			int max = SettingsManager.instance().getUploadsPerPerson();
			if (current > max)
				return false;
		}
		return true;
	}
		

	/**
	 * Returns true iff another upload is allowed.  Note that because this test
	 * relies on the uploadsInProgress() method, it may sometimes be incorrect
	 * if a push request takes a long time to respond.  REQUIRES: this'
     * monitor is held.
     */
	private boolean testTotalUploadLimit() {
        //Allow another upload if (a) we currently have fewer than
        //SOFT_MAX_UPLOADS uploads or (b) some upload has more than
        //MINIMUM_UPLOAD_SPEED KB/s.  But never allow more than MAX_UPLOADS.
        //
        //In other words, we continue to allow uploads until everyone's
        //bandwidth is diluted.  The assumption is that with MAX_UPLOADS
        //uploads, the probability that all just happen to have low capacity
        //(e.g., modems) is small.  This reduces "Try Again Later"'s at the
        //expensive of quality, making swarmed downloads work better.        

		int current = uploadsInProgress();
        SettingsManager settings=SettingsManager.instance();
		if (current >= settings.getMaxUploads()) {
            return false;
        } else if (current < settings.getSoftMaxUploads()) {
            return true;
        } else {
            float fastest=0.0f;
            for (Iterator iter=_activeUploadList.iterator(); iter.hasNext(); ) {
                BandwidthTracker upload=(BandwidthTracker)iter.next();
                fastest=Math.max(fastest, upload.getMeasuredBandwidth());
            }
            return fastest>MINIMUM_UPLOAD_SPEED;
        }
	}

    /** @requires caller has this' monitor */
	private void insertFailedPush(String host, int index) {
		_failedPushes.add(new PushedFile(host, index));
	}
	
	private boolean testFailedPush(String host, int index) {
		PushedFile pf = new PushedFile(host, index);
		PushedFile pfile;
		Iterator iter = _failedPushes.iterator();
		while ( iter.hasNext() ) {
			pfile = (PushedFile)iter.next();
			if ( pf.equals(pfile) ) 
				return false;
		}
		return true;

	}

	private void insertAttemptingPush(String host, int index) {
		_attemptingPushes.add(new PushedFile(host, index));
	}

	private boolean testAttemptingPush(String host, int index) {
		PushedFile pf = new PushedFile(host, index);
		PushedFile pfile;
		Iterator iter = _attemptingPushes.iterator();
		while ( iter.hasNext() ) {
			pfile = (PushedFile)iter.next();
			if ( pf.equals(pfile) )
				return false;
		}
		return true;
	}
	
	private void removeAttemptingPush(String host, int index) {
		PushedFile pf = new PushedFile(host, index);
		PushedFile pfile;
		Iterator iter = _attemptingPushes.iterator();
		while ( iter.hasNext() ) {
			pfile = (PushedFile)iter.next();
			if ( pf.equals(pfile) )
				// calling iter.remove() rather than
				// remove on the list, since this will be
				// safer while iterating through the list.
				iter.remove();
		}
	}

    /** @requires caller has this' monitor */
	private void clearFailedPushes() {
		// First remove all files that were pushed more than a few minutes ago
		Date time = new Date();
		time.setTime(time.getTime()-(PUSH_INVALIDATE_TIME*1000));
		Iterator iter = _failedPushes.iterator();
		while (iter.hasNext()) {
			PushedFile pf=(PushedFile)iter.next();
			if (pf.before(time))
				iter.remove();
		}
		
	}


	////////////////// Bandwith Allocation and Measurement///////////////

	/**
	 * calculates the appropriate burst size for the allocating
	 * bandwith on the upload.
	 * @return burstSize.  if it is the special case, in which 
	 *         we want to upload as quickly as possible.
	 */
	public int calculateBandwidth() {
		// public int calculateBurstSize() {
		float totalBandwith = getTotalBandwith();
		float burstSize = totalBandwith/uploadsInProgress();
		return (int)burstSize;
	}
	
	/**
	 * @return the total bandwith available for uploads
	 */
	private float getTotalBandwith() {

		SettingsManager manager = SettingsManager.instance();
		// To calculate the total bandwith available for
		// uploads, there are two properties.  The first
		// is what the user *thinks* their connection
		// speed is.  Note, that they may have set this
		// wrong, but we have no way to tell.
		float connectionSpeed  
		= ((float)manager.getConnectionSpeed())/8.f;
		// the second number is the speed that they have 
		// allocated to uploads.  This is really a percentage
		// that the user is willing to allocate.
		float speed = manager.getUploadSpeed();
		// the total bandwith available then, is the percentage
		// allocated of the total bandwith.
		float totalBandwith = ((connectionSpeed*((float)speed/100.F)));
		return totalBandwith;
	}


	/**
	 * returns in string format the ip address of the machine
	 * that this limewire is running on.
	 */
	public String getThisHost() {
		byte[] address = _acceptor.getAddress() ;
		String host = Message.ip2string(address);
		return host;
	}
	/**
	 * returns the port of the machine that this limewire is
	 * running on
	 */
	public int getThisPort() {return _acceptor.getPort(); }

    /** Returns the estimated upload speed in <b>KILOBITS/s</b> [sic] of the
     *  next transfer, assuming the client (i.e., downloader) has infinite
     *  bandwidth.  Returns -1 if not enough data is available for an 
     *  accurate estimate. */
    public int measuredUploadSpeed() {
        //Note that no lock is needed.
        return highestSpeed;
    }

    /**
     * Notes that some uploader has uploaded the given number of BYTES in the
     * given number of milliseconds.  If bytes is too small, the data may be
     * ignored.  
     *     @requires this' lock held 
     *     @modifies this.speed, this.speeds
     */
    private void reportUploadSpeed(long milliseconds, long bytes) {
        //This is critical for ignoring 404's messages, etc.
        if (bytes<MIN_SAMPLE_BYTES)
            return;

        //Calculate the bandwidth in kiloBITS/s.  We just assume that 1 kilobyte
        //is 1000 (not 1024) bytes for simplicity.
        int bandwidth=8*(int)((float)bytes/(float)milliseconds);
        speeds.add(new Integer(bandwidth));

        //Update maximum speed if possible.  This should be atomic.  TODO: can
        //the compiler replace the temporary variable max with highestSpeed?
        if (speeds.size()>=MIN_SPEED_SAMPLE_SIZE) {
            int max=0;
            for (int i=0; i<speeds.size(); i++) 
                max=Math.max(max, ((Integer)speeds.get(i)).intValue());
            this.highestSpeed=max;
        }
    }

	/**
	 * Returns a new <tt>GETLine</tt> instance, where the <tt>GETLine</tt>
	 * class is an immutable struct that contains all data for the "GET" line
	 * of the HTTP request.
	 *
	 * @param socket the <tt>Socket</tt> instance over which we're reading
	 * @return the <tt>GETLine</tt> struct for the HTTP request
	 */
	private GETLine parseGET(Socket socket) throws IOException {

		// Set the timeout so that we don't do block reading.
		socket.setSoTimeout(SettingsManager.instance().getTimeout());
		// open the stream from the socket for reading
		//InputStream istream = socket.getInputStream();
		ByteReader br = new ByteReader(socket.getInputStream());
		
		// read the first line. if null, throw an exception
		String str = br.readLine();
		if (str == null) {
			throw new IOException();
		}

		str.trim();

		// handle the get request depending on what type of request it is
		if(this.isTraditionalGet(str)) {
			// handle the standard get request
			return this.parseTraditionalGet(str);
		}
		// handle the URN get request
		return this.parseURNGet(str);
  	}

	private boolean isTraditionalGet(final String GET_LINE) {
		return (GET_LINE.indexOf("/get/") != -1);
	}

	/**
	 * Returns whether or not the get request for the specified line is
	 * a URN request.
	 *
	 * @param GET_LINE the <tt>String</tt> to parse to check whether it's
	 *  following the URN request syntax as specified in HUGE v. 0.93
	 * @return <tt>true</tt> if the request is a valid URN request, 
	 *  <tt>false</tt> otherwise
	 */
	private boolean isURNGet(final String GET_LINE) {
		int slash1Index = GET_LINE.indexOf("/");
		int slash2Index = GET_LINE.indexOf("/", slash1Index);
		if((slash1Index==-1) || (slash2Index==-1)) {
			return false;
		}
		String idString = GET_LINE.substring(slash1Index, slash2Index);
		return idString.equalsIgnoreCase(UploadManager.URI_RES);
	}

	private static GETLine parseTraditionalGet(final String GET_LINE) 
		throws IOException {
		try {
			// Expecting "GET /get/0/sample.txt HTTP/1.0"
			// parse this for the appropriate information
			// find where the get is...
			int getIndex = GET_LINE.indexOf("/get/");	

			if(getIndex == -1) {
				// the protocol is unknown...
				throw new IOException("Could not parse standard get: get");
			}
			
			// find the next "/" after the "/get/".  the number 
			// between should be the index;
			int slashIndex = GET_LINE.indexOf( "/", (getIndex + 5) ); 
			if(slashIndex == -1) {
				// if the slash could not be found, throw an exception
				throw new IOException("Could not parse standard get: slash");
			}

			// get the index
			String str_index = GET_LINE.substring( (getIndex+5), slashIndex );
			int index = java.lang.Integer.parseInt(str_index);
			// get the filename, which should be right after
			// the "/", and before the next " ".
			int fileNameIndex = GET_LINE.indexOf( " HTTP/", slashIndex );
			if(fileNameIndex == -1) {
				// if the slash could not be found, throw an exception
				throw new IOException("Could not parse standard get: HTTP index");
			}
			
			String fileName = URLDecoder.decode(GET_LINE.substring( (slashIndex+1), 
																	fileNameIndex));
            //check if the protocol is HTTP1.1. Note that this is not a very 
            //strict check.
            boolean http11 = false;
            if(GET_LINE.endsWith("1.1"))
                http11 = true;
			return new GETLine(index, fileName, http11);
		} catch (NumberFormatException e) {
			throw new IOException();
		} catch (IndexOutOfBoundsException e) {
			throw new IOException();
		}
	}

	private static final String GET = "GET";
	private static final String URI_RES = "uri-res";
	private static final String NAME_TO_RESOURCE = "N2R?"; 
	private static final String HTTP = "HTTP"; 
	private static final String URN = "urn:";
	private static final String SHA1 = "sha1";
	private static final String BITPRINT = "bitprint";
	private static final String HTTP10 = "HTTP/1.0";
	private static final String HTTP11 = "HTTP/1.1";

	/**
	 * Parses the get line for a URN request, throwing an exception if 
	 * there are any errors in parsing.
	 *
	 * @param GET_LINE the <tt>String</tt> instance containing the get request
	 * @return a new <tt>GETLine</tt> instance containing all of the data
	 *  for the get request
	 */
	private GETLine parseURNGet(final String GET_LINE) 
		throws IOException {		

		// it's possible that we should be throwing exceptions here at, as
		// they're expensive and being used here really for normal flow
		// control
		if(!isValidURNGetRequest(GET_LINE)) {
			throw new IOException("INVALID URN REQUEST SYNTAX");
		}
		String urn    = this.getURN(GET_LINE);
		FileDesc desc = _fileManager.getFileDescForURN(urn);
		if(desc == null) {
			throw new IOException("NO MATCHING FILEDESC FOR URN");
		}		
		int fileIndex = _fileManager.getFileIndexForURN(urn);
		if(fileIndex == -1) {
			throw new IOException("NO MATCHING FILE INDEX FOR URN");
		}
		String fileName = desc._name;
		boolean isHTTP11 = this.isHTTP11Request(GET_LINE);
		return new GETLine(fileIndex, fileName, isHTTP11);		
	}

	/**
	 * Returns whether or not the get request is valid, as specified in
	 * HUGE v. 0.93 and IETF RFC 2169.
	 *
	 * @param GET_LINE the <tt>String</tt> instance containing the get request
	 * @return <tt>true</tt> if the reques is valid, <tt>false</tt> otherwise
	 */
	private boolean isValidURNGetRequest(final String GET_LINE) {
		return (this.isValidSize(GET_LINE) &&
				this.isValidGet(GET_LINE) &&
				this.isValidUriRes(GET_LINE) &&
				this.isValidResolutionProtocol(GET_LINE) && 
				this.isValidURN(GET_LINE) &&
				this.isValidHTTPSpecifier(GET_LINE));				
	}

	/** 
	 * Returns whether or not the specified get request meets size 
	 * requirements.
	 *
	 * @param GET_LINE the <tt>String</tt> instance containing the get request
	 * @return <tt>true</tt> if the get request starts with "GET "
	 *  (case-insensitive), <tt>false</tt> otherwise
	 */
	private final boolean isValidSize(final String GET_LINE) {
		int size = GET_LINE.length();
		if((size != 67) && (size != 111)) {
			return false;
		}
		return true;
	}


	/**
	 * Returns whether or not the get request corresponds with the standard 
	 * start of a get request.
	 *
	 * @param GET_LINE the <tt>String</tt> instance containing the get request
	 * @return <tt>true</tt> if the get request starts with "GET "
	 *  (case-insensitive), <tt>false</tt> otherwise
	 */
	private final boolean isValidGet(final String GET_LINE) {
		int firstSpace = GET_LINE.indexOf(" ");
		if(firstSpace == -1) {
			return false;
		}
		String getStr = GET_LINE.substring(0, firstSpace);
		if(!getStr.equalsIgnoreCase(UploadManager.GET)) {
			return false;
		}
		return true;
	}

	/**
	 * Returns whether or not the get request corresponds with the standard 
	 * uri-res request
	 *
	 * @param GET_LINE the <tt>String</tt> instance containing the get request
	 * @return <tt>true</tt> if the get request includes the standard "uri-res"
	 *  (case-insensitive) request, <tt>false</tt> otherwise
	 */
	private final boolean isValidUriRes(final String GET_LINE) {
		int firstSlash = GET_LINE.indexOf("/");
		if(firstSlash == -1) {
			return false;
		}
		int secondSlash = GET_LINE.indexOf("/", firstSlash+1);
		if(secondSlash == -1) {
			return false;
		}
		String uriStr = GET_LINE.substring(firstSlash+1, secondSlash);
		if(!uriStr.equalsIgnoreCase(UploadManager.URI_RES)) {
			return false;
		}
		return true;
	}

	/**
	 * Returns whether or not the specified "resolution protocol" is valid.
	 * We currently only support N2R, which specifies "Given a URN, return the
	 * named resource."
	 *
	 * @param GET_LINE the <tt>String</tt> instance containing the get request
	 * @return <tt>true</tt> if the resolution protocol is valid, <tt>false</tt>
	 *  otherwise
	 */
	private boolean isValidResolutionProtocol(final String GET_LINE) {
		int nIndex = GET_LINE.indexOf("2");
		if(nIndex == -1) {
			return false;
		}
		String n2r = GET_LINE.substring(nIndex-1, nIndex+3);

		// we could add more protocols to this check
		if(!n2r.equalsIgnoreCase(UploadManager.NAME_TO_RESOURCE)) {
			return false;
		}
		return true;
	}

	/**
	 * Returns a <tt>String</tt> containing the URN for the get request.  For
	 * a typical SHA1 request, this will return a 41 character URN, including
	 * the 32 character hash value.  For a full description of what qualifies
	 * as a valid URN, see RFC2141 ( http://www.ietf.org ).<p>
	 *
	 * The broad requirements of the URN are that it meet the following 
	 * syntax: <p>
	 *
	 * <URN> ::= "urn:" <NID> ":" <NSS>  <p>
	 * 
	 * where phrases enclosed in quotes are required and where "<NID>" is the
	 * Namespace Identifier and "<NSS>" is the Namespace Specific String.
	 *
	 * @param GET_LINE the <tt>String</tt> instance containing the get request
	 * @return a <tt>String</tt> containing the URN for the get request
	 * @throws <tt>IOException</tt> if there is an error parsing out the URN
	 *  from the line
	 */
	private boolean isValidURN(final String GET_LINE) {
		int colon1Index = GET_LINE.indexOf(":");
		if(colon1Index == -1) {
			return false;
		}

		// get the "urn:" substring so we can make sure it's there,
		// ignoring case
		String urnStr = GET_LINE.substring(colon1Index-3, colon1Index+1);

		// get the last colon -- this should separate the <NID>
		// from the <NIS>
		int colon2Index = GET_LINE.indexOf(":", colon1Index+1);

		// get the space index that cannot be included in either the
		// NID or the NSS and that must separate the NSS and the
		// HTTP version
		int spaceIndex = GET_LINE.indexOf(" ", colon2Index);
		
		if((colon2Index == -1) || 
		   !urnStr.equalsIgnoreCase(UploadManager.URN) ||
		   !isValidNID(GET_LINE.substring(colon1Index+1, colon2Index)) ||
		   !isValidNSS(GET_LINE.substring(colon2Index+1, spaceIndex))) {
			return false;
		}		
		return true;
	}


	/**
	 * Returns whether or not the specified Namespace Identifier String (NID) 
	 * is a valid NID.
	 *
	 * @param NID the Namespace Identifier String for a URN
	 * @return <tt>true</tt> if the NID is valid, <tt>false</tt> otherwise
	 */
	private boolean isValidNID(final String NID) {					
		// we should add other namespace identifiers to this check as
		// they become registered
		if(!NID.equalsIgnoreCase(UploadManager.SHA1) &&
		   !NID.equalsIgnoreCase(UploadManager.BITPRINT)) {
			return false;
		}
		return true;
	}

	/**
	 * Returns whether or not the specified Namespace Specific String (NSS) 
	 * is a valid NSS.
	 *
	 * @param NSS the Namespace Specific String for a URN
	 * @return <tt>true</tt> if the NSS is valid, <tt>false</tt> otherwise
	 */
	private boolean isValidNSS(final String NSS) {
		int length = NSS.length();

		// checks to make sure that it either is the length of a 32 
		// character SHA1 NSS, or is the length of a 72 character
		// bitprint NSS
		if((length != 32) && (length != 72)) {
			return false;
		}
		return true;
	}

	/**
	 * Returns whether or not the HTTP specifier for the URN get request
	 * is valid.
	 *
	 * @param GET_LINE the <tt>String</tt> instance containing the get request
	 * @return <tt>true</tt> if the HTTP specifier is valid, <tt>false</tt>
	 *  otherwise
	 */
	private boolean isValidHTTPSpecifier(final String GET_LINE) {
		int spaceIndex = GET_LINE.lastIndexOf(" ");
		if(spaceIndex == -1) {
			return false;
		}
		String httpStr = GET_LINE.substring(spaceIndex+1);
		if(!httpStr.equalsIgnoreCase(UploadManager.HTTP10) &&
		   !httpStr.equalsIgnoreCase(UploadManager.HTTP11)) {
			return false;
		}
		return true;
	}

	/**
	 * Returns a <tt>String</tt> containing the URN for the get request.  For
	 * a typical SHA1 request, this will return a 41 character URN, including
	 * the 32 character hash value.
	 *
	 * @param GET_LINE the <tt>String</tt> instance containing the get request
	 * @return a <tt>String</tt> containing the URN for the get request
	 * @throws <tt>IOException</tt> if there is an error parsing out the URN
	 *  from the line
	 */
	private String getURN(final String GET_LINE) 
		throws IOException {
		int qIndex     = GET_LINE.indexOf("?") + 1;
		int spaceIndex = GET_LINE.indexOf(" ", qIndex);		
		if((qIndex == -1) || (spaceIndex == -1)) {
			throw new IOException("ERROR PARSING URN FROM GET REQUEST");
		}
		return GET_LINE.substring(qIndex, spaceIndex);
	}

	/**
	 * Returns whether or the the specified get request is using HTTP 1.1.
	 *
	 * @return <tt>true</tt> if the get request specifies HTTP 1.1,
	 *  <tt>false</tt> otherwise
	 */
	private boolean isHTTP11Request(final String GET_LINE) {
		return GET_LINE.endsWith("1.1");
	}

	/**
	 * This is an immutable class that contains the data for the GET line of
	 * the HTTP request.
	 */
	private static class GETLine {
  		private final int _index;
  		private final String _fileName;
        /** flag indicating if the protocol is HTTP1.1 */
        private final boolean _http11;
        
		/**
		 * Constructs a new <tt>GETLine</tt> instance.
		 *
		 * @param index the index for the file to get
		 * @param fileName the name of the file to get
		 * @param http11 specifies whether or not it's an HTTP 1.1 request
		 */
		private GETLine(int index, String fileName, boolean http11) {
  			_index = index;
  			_fileName = fileName;
            _http11 = http11;
  		}
        
		/**
		 * Returns whether or not the request is an HTTP 1.1 request.
		 *
		 * @return <tt>true</tt> if this is an HTTP 1.1 request, <tt>false</tt>
		 *  otherwise
		 */
        private boolean isHTTP11() {
            return _http11;
        }
  	}

	/**
	 * Keeps track of a push requested file and the host that requested it.
	 */
	private class PushedFile {
		private final String _host;
        private final int _index;
		private final Date _time;        

		public PushedFile(String host, int index) {
			_host = host;
            _index = index;
			_time = new Date();
		}
		
        /** Returns true iff o is a PushedFile with same _host and _index.
         *  Time doesn't matter. */
		public boolean equals(Object o) {
            if (! (o instanceof PushedFile))
                return false;
            PushedFile pf=(PushedFile)o;
			return _index==pf._index && _host.equals(pf._host);
		}
		
		public boolean before(Date time) {
			return _time.before(time);
		}
		
	}


    /** Calls measureBandwidth on each uploader. */
    public synchronized void measureBandwidth() {
        for (Iterator iter = _activeUploadList.iterator(); iter.hasNext(); ) {
			BandwidthTracker bt = (BandwidthTracker)iter.next();
			bt.measureBandwidth();
		}
    }

    /** Returns the total upload throughput, i.e., the sum over all uploads. */
	public synchronized float getMeasuredBandwidth() {
        float sum=0;
        for (Iterator iter = _activeUploadList.iterator(); iter.hasNext(); ) {
			BandwidthTracker bt = (BandwidthTracker)iter.next();
			sum+=bt.getMeasuredBandwidth();
		}
        return sum;
	}

    /** Partial unit test. */
    /*
    public static void main(String args[]) {
        UploadManager upman=new UploadManager();
        upman.reportUploadSpeed(100000, 1000000);  //10 kB/s
        Assert.that(upman.measuredUploadSpeed()==-1);
        upman.reportUploadSpeed(100000, 2000000);  //20 kB/s
        Assert.that(upman.measuredUploadSpeed()==-1);
        upman.reportUploadSpeed(100000, 3000000);  //30 kB/s
        Assert.that(upman.measuredUploadSpeed()==-1);
        upman.reportUploadSpeed(100000, 4000000);  //40 kB/s
        Assert.that(upman.measuredUploadSpeed()==-1);
        upman.reportUploadSpeed(100000, 5000000);  //50 kB/s == 400 kb/sec
        Assert.that(upman.measuredUploadSpeed()==400);
        upman.reportUploadSpeed(100000, 6000000);  //60 kB/s == 480 kb/sec
        Assert.that(upman.measuredUploadSpeed()==480);
        upman.reportUploadSpeed(1, 1000);          //too little data to count
        Assert.that(upman.measuredUploadSpeed()==480);
        upman.reportUploadSpeed(100000, 1000000);  //10 kB/s = 80 kb/s
        upman.reportUploadSpeed(100000, 1000000);  
        upman.reportUploadSpeed(100000, 1000000);  
        upman.reportUploadSpeed(100000, 1000000);  
        upman.reportUploadSpeed(100000, 1000000);  
        Assert.that(upman.measuredUploadSpeed()==80);
    }
    */

	// unit test for the validating of GET requests
	public static void main(String[] args) {
		String [] validURNS = {
		    "GET /uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		    "GET /URI-RES/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		    "GET /uri-res/n2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		    "GET /uri-res/N2r?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		    "GET /uri-res/n2r?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		    "GET /uri-res/N2R?UrN:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		    "GET /uri-res/N2R?urn:sHa1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		    "GET /uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.1",
		    "GET /uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HtTP/1.0",
		    "GET /uri-res/N2R?urn:bitprint:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB."+
			"PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB1234567 HTTP/1.0",
		    "GET /uri-res/N2R?urn:bitprint:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB."+
			"PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB1234567 HTTP/1.1"
		};

		String [] invalidURNS = {
		    "GET /uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.2",
		    "GET /urires/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.1",
		    "/uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJcdirnZWUGYQYPFB HTTP/1.0",
		    "GET /uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.2",
		    "GET /uri-res/N2Rurn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		    "GET /uri-res/N2R?urn:sh1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.1",
		    "GET/uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		    "GET /uri-res/N2R?urn:bitprint::PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB."+
			"PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB1234567 HTTP/1.0",
		    "GET /uri-res/N2R?urn:sha1::PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB."+
			"PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB1234567 HTTP/1.0",
		    "GET /uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPF HTTP/1.0",
		    "GET /uri-res/N2R?ur:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		    "GET /uri-res/N2R? urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		    "GET /uri-res/N2R?  urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		    "GET /uri-res/N2R?                                                    "+
			"urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		    "GET /uri-res/N2R?urn:sha1: PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		    "GET /uri-res/ N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		    "GET /uri-res/N2?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		    "GET /uri-res/N2Rurn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		    "GET /uri-res/N2R?urnsha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		    "GET /uri-res/N2R?urn:sa1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0",
		    "GET /uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0 ",
		    " GET /uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB HTTP/1.0 ",
			" ",
			"GET",
		    "GET /uri-res/N2R?urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFBC HTTP/1.0",
		};
		UploadManager um = new UploadManager();
		boolean encounteredFailure = false;
		System.out.println("TESTING THAT VALID URN GET REQUESTS PASS..."); 
		for(int i=0; i<validURNS.length; i++) {
			if(um.isValidURNGetRequest(validURNS[i]) != true) {
				if(!encounteredFailure) {
					System.out.println(  ); 
					System.out.println("VALID URN TEST FAILED");
				}
				encounteredFailure = true;
				System.out.println(); 
				System.out.println("FAILED ON URN: ");
				System.out.println(validURNS[i]); 
			}
		}
		if(!encounteredFailure) {
			System.out.println("TEST PASSED"); 
		}
		System.out.println(); 
		System.out.println("TESTING THAT INVALID URN GET REQUESTS FAIL..."); 
		for(int i=0; i<invalidURNS.length; i++) {
			if(um.isValidURNGetRequest(invalidURNS[i]) == true) {
				if(!encounteredFailure) {
					System.out.println("INVALID URN TEST FAILED");
				}
				encounteredFailure = true;
				System.out.println(); 
				System.out.println("FAILED ON URN "+i+":");
				System.out.println(invalidURNS[i]); 
			}			
		}
		if(!encounteredFailure) {
			System.out.println("TEST PASSED"); 
		}
		System.out.println(); 
		if(!encounteredFailure) {
			System.out.println("ALL TESTS PASSED"); 
		}
	}
}
