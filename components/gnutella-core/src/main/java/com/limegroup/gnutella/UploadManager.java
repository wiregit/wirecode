package com.limegroup.gnutella;

import com.limegroup.gnutella.uploader.*;
import com.limegroup.gnutella.util.Buffer;
import java.net.*;
import java.io.*;
import com.sun.java.util.collections.*;
import java.util.Date;
import com.limegroup.gnutella.util.URLDecoder;
import com.limegroup.gnutella.downloader.*; //for testing
import com.limegroup.gnutella.tests.util.*; //for testing
import com.limegroup.gnutella.tests.stubs.*; //for testing
import java.util.StringTokenizer;

/**
 * The list of all the uploads in progress.
 * 
 * 
 */

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
	private Map /* String -> Integer */ _uploadsInProgress =
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
    
    /** 
     * The file index used in this structure to indicate a browse host
     * request
     */
    public static final int BROWSE_HOST_FILE_INDEX = -1;


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
                uploader = new HTTPUploader(line._file, socket, line._index, 
                    this, _fileManager, _router);
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
            //close the socket
            close(socket);
        }
	}
    
    /**
     * Does some book-keeping and makes the downloader, start the download
     * @param uploader This method assumes that uploader is connected.
     */
    private void doSingleUpload(HTTPUploader uploader, String host,
        int index) {
        long startTime=-1;

        // check if it complies with the restrictions.
        //and set the uploader state accordingly
        boolean accepted=insertAndTest(uploader, host);

        // don't want to show browse host status in gui...
        if (uploader.getState() != Uploader.BROWSE_HOST)
            //We are going to notify the gui about the new upload, and let it 
            //decide what to do with it - will act depending on it's state
            _callback.addUpload(uploader);

        
        //Note: We do not call connect() anymore. That's because connect would
        //never do anything in the case of a normal upload  - becasue the
        //HTTPUploader already have a socket. connect() would only be executed
        // if a push was calling this method. 
        //Now the acceptPushDownload method connects directly.
        
        synchronized(this) { 
            if (accepted) 
                _activeUploads++; 
        }

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

        //clean up
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
            if (accepted)
                _activeUploads--;
            _callback.removeUpload(uploader);		
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
	 * <p>
     * The thread makes the uploader connect (which does the connecting 
     * and also writes out the GIV, so the state of the returned socket 
     * is the same as a socket which the accpetUpload methos would 
     * expect)and delegates to the acceptUpload method with the socket 
     * it gets from connecting.
     * <p>
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
		final HTTPUploader GIVuploader = new HTTPUploader
                         (file, host, port, index, guid, this, _fileManager,
                          _router);
        //Note: GIVuploader is just used to connect, and while connecting, 
        //the GIVuploader uploads the GIV message.

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
                    //create the socket and send the GIV message
                    Socket s = GIVuploader.connect();
                    //delegate to the normal upload
                    acceptUpload(s);
                    //Note: we do not do any book-keeping - like incrementing
                    //_activeUploads. Because that is taken care off in 
                    //acceptUpload.
                }catch(IOException ioe){//connection failed? do book-keeping
                    synchronized(UploadManager.this) { 
                        insertFailedPush(host, index);  
                    }
                }
                finally {
                    //close the socket
                    GIVuploader.stop();
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
		// return true if Limewire is shutting down
		if (RouterService.instance().getIsShuttingDown())
		    return true;
		
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
     *  Always accept Browse Host requests, though....
     *  Notifies callback of this.
     *      @modifies _uploadsInProgress, uploader, _callback */
	private synchronized boolean insertAndTest(HTTPUploader uploader, 
                                               String host) {
		// add to the Map
		insertIntoMapAndList(uploader, host);

		if ( ( (! testPerHostLimit(host) ) ||
               ( this.isBusy() ) ) &&
             uploader.getState() != Uploader.BROWSE_HOST ) {                 
            uploader.setState(Uploader.LIMIT_REACHED);
            return false;
        }
        return true;
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
                float speed = 0;
                try {
                    speed=upload.getMeasuredBandwidth();
                } catch (InsufficientDataException ide) {
                    speed = 0;
                }
                fastest=Math.max(fastest,speed);
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
	 * returns the ip address of the machine
	 * that this limewire is running on.
	 */
	public byte[] getThisAddress() {
		return _acceptor.getAddress() ;
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

	//////////////////////// Handle Parsing /////////////////////////

	

	private static GETLine parseGET(Socket socket) throws IOException {
		try {
            int index = -1;
            String file = null;
			// Set the timeout so that we don't do block reading.
			socket.setSoTimeout(SettingsManager.instance().getTimeout());
			// open the stream from the socket for reading
			InputStream istream = socket.getInputStream();
			ByteReader br = new ByteReader(istream);
            //Expecting "GET /get/0/sample.txt HTTP/1.0"
			// read the first line ("GET " has already been read). 
			String str = br.readLine().trim();
			if (str == null)
				throw new IOException();
            
            //tokenize the string to separate out file information part
            //and the http information part
            StringTokenizer st = new StringTokenizer(str);
            //file information part: /get/0/sample.txt
            String fileInfoPart = st.nextToken().trim();
            //http information part: HTTP/1.0
            String httpInfoPart = st.nextToken().trim();
            
            if(fileInfoPart.equals("/")) {
                //special case for browse host request
                index = BROWSE_HOST_FILE_INDEX;
                file = "Browse-Host Request";
            } else {
                //NORMAL CASE
                // parse this for the appropriate information
                // find where the get is...
                int g = str.indexOf("/get/");
                // find the next "/" after the "/get/".  the number 
                // between should be the index;
                int d = str.indexOf( "/", (g + 5) ); 
                // get the index
                String str_index = str.substring( (g+5), d );
                index = java.lang.Integer.parseInt(str_index);
                // get the filename, which should be right after
                // the "/", and before the next " ".
                int f = str.indexOf( " HTTP/", d );
                file = URLDecoder.decode(str.substring( (d+1), f));
            }
            //check if the protocol is HTTP1.1. Note that this is not a very 
            //strict check.
            boolean http11 = false;
            if(str.endsWith("1.1"))
                http11 = true;
			return new GETLine(index, file, http11);
		} catch (NumberFormatException e) {
			throw new IOException();
		} catch (IndexOutOfBoundsException e) {
			throw new IOException();
		} catch (java.util.NoSuchElementException nsee) {
            throw new IOException();
        }
  	}


	private static class GETLine {
        int _index;
  		String _file;
        /** flag indicating if the protocol is HTTP1.1 */
        boolean _http11;
        
  		public GETLine(int index, String file, boolean http11) {
  			_index = index;
  			_file = file;
            _http11 = http11;
  		}
        
        boolean isHTTP11() {
            return _http11;
        }

  	}

	/**
	 * Keeps track of a push requested file and the host that requested it.
	 */
	private class PushedFile {
		private String _host;
        private int _index;
		private Date _time;        

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
            float curr = 0;
            try {
                curr = bt.getMeasuredBandwidth();
            } catch(InsufficientDataException ide) {
                curr = 0;
            }
			sum+= curr;
		}
        return sum;
	}

    /** Partial unit test. */
//      public static void main(String args[]) {
//          //Test measured upload speed code
//          UploadManager upman=new UploadManager();
//          testBandwidthTracker(upman);

//          //Test limits
//          FileManager fileMan=new FileManagerStub();
//          FileDesc fd=fileMan.get(0);
//          RemoteFileDesc rfd=new RemoteFileDesc(
//              "x.x.x.x", 0, fd._index, fd._name, fd._size,
//              new byte[16], 56, false, 3);

//          upman=new UploadManager();
//          upman.initialize(new com.limegroup.gnutella.Main(),
//                           null,
//                           null,
//                           fileMan);
//          testTotalUploadLimit(upman, rfd);

//          upman=new UploadManager();
//          upman.initialize(new com.limegroup.gnutella.Main(),
//                           null,
//                           null,
//                           fileMan);
//          testPerPersonUploadLimit(upman, rfd);

//          upman=new UploadManager();
//          upman.initialize(new com.limegroup.gnutella.Main(),
//                           null,
//                           null,
//                           fileMan);
//          testSoftUploadLimit(upman, rfd);
//      }

//      private static void testBandwidthTracker(UploadManager upman) {
//          System.out.print("-Testing bandwidth tracker...");
//          upman.reportUploadSpeed(100000, 1000000);  //10 kB/s
//          Assert.that(upman.measuredUploadSpeed()==-1);
//          upman.reportUploadSpeed(100000, 2000000);  //20 kB/s
//          Assert.that(upman.measuredUploadSpeed()==-1);
//          upman.reportUploadSpeed(100000, 3000000);  //30 kB/s
//          Assert.that(upman.measuredUploadSpeed()==-1);
//          upman.reportUploadSpeed(100000, 4000000);  //40 kB/s
//          Assert.that(upman.measuredUploadSpeed()==-1);
//          upman.reportUploadSpeed(100000, 5000000);  //50 kB/s == 400 kb/sec
//          Assert.that(upman.measuredUploadSpeed()==400);
//          upman.reportUploadSpeed(100000, 6000000);  //60 kB/s == 480 kb/sec
//          Assert.that(upman.measuredUploadSpeed()==480);
//          upman.reportUploadSpeed(1, 1000);          //too little data to count
//          Assert.that(upman.measuredUploadSpeed()==480);
//          upman.reportUploadSpeed(100000, 1000000);  //10 kB/s = 80 kb/s
//          upman.reportUploadSpeed(100000, 1000000);  
//          upman.reportUploadSpeed(100000, 1000000);  
//          upman.reportUploadSpeed(100000, 1000000);  
//          upman.reportUploadSpeed(100000, 1000000);  
//          Assert.that(upman.measuredUploadSpeed()==80);
//          System.out.println("passed");
//      }
    
//      private static void testTotalUploadLimit(UploadManager upman, 
//                                               RemoteFileDesc rfd) {
//          SettingsManager.instance().setMaxUploads(2);
//          SettingsManager.instance().setSoftMaxUploads(99999);
//          SettingsManager.instance().setUploadsPerPerson(99999);               
        
//          System.out.print("-Testing total upload limit...");
//          try {
//              //Add two downloaders
//              HTTPDownloader d1=addUploader(upman, rfd, "1.1.1.1", true);
//              HTTPDownloader d2=addUploader(upman, rfd, "1.1.1.2", true);
//              //Third is denied
//              try {
//                  HTTPDownloader d3=addUploader(upman, rfd, "1.1.1.3", true);
//                  Assert.that(false, "Downloader denied");
//              } catch (TryAgainLaterException e) {            
//              }
//              //But killing 1st allows third
//              kill(d1);
//              HTTPDownloader d3=addUploader(upman, rfd, "1.1.1.3", true);
//              //But not a fourth
//              try {
//                  HTTPDownloader d4=addUploader(upman, rfd, "1.1.1.3", true);
//                  Assert.that(false, "Downloader denied");
//              } catch (TryAgainLaterException e) {            
//              }
//              System.out.println("passed");
//          } catch (Throwable e) {
//              System.out.println("FAILED!");
//              e.printStackTrace();
//          }
//      }

    
//      private static void testPerPersonUploadLimit(UploadManager upman, 
//                                                   RemoteFileDesc rfd) {
//          SettingsManager.instance().setMaxUploads(99999);
//          SettingsManager.instance().setSoftMaxUploads(99999);
//          SettingsManager.instance().setUploadsPerPerson(2);               

//          System.out.print("-Testing per person upload limit...");    
//          try {
//              //Add two downloaders
//              HTTPDownloader d1=addUploader(upman, rfd, "1.1.1.1", true);
//              HTTPDownloader d2=addUploader(upman, rfd, "1.1.1.1", true);
//              //Third from same address is denied
//              try {
//                  //TODO: we have to disable blocking behavior below since
//                  //TryAgainLater uploads ARE included in _uploadsInProgress.
//                  //That's really a bug, but we can live with it.
//                  HTTPDownloader d3=addUploader(upman, rfd, "1.1.1.1", false);
//                  Assert.that(false, "Downloader denied");
//              } catch (TryAgainLaterException e) {            
//              }
//              //But allow another with different address in
//              HTTPDownloader d3=addUploader(upman, rfd, "1.1.1.2", true);
//              //And killing d1 allows another from the first address
//              kill(d1);
//              HTTPDownloader d4=addUploader(upman, rfd, "1.1.1.1", true);
//              System.out.println("passed");
//          } catch (Throwable e) {
//              System.out.println("FAILED");
//              e.printStackTrace();
//          }
//      }
    
//      private static void testSoftUploadLimit(UploadManager upman, 
//                                                   RemoteFileDesc rfd) {
//          SettingsManager.instance().setMaxUploads(99999);
//          SettingsManager.instance().setSoftMaxUploads(2);
//          SettingsManager.instance().setUploadsPerPerson(99999);               

//          System.out.print("-Testing soft upload limit (incomplete)...");    
//          //TODO: this doesn't test that the number of slots is increased
//          //if all uploaders are fast.
//          try {
//              //Add two downloaders
//              HTTPDownloader d1=addUploader(upman, rfd, "1.1.1.1", true);
//              HTTPDownloader d2=addUploader(upman, rfd, "1.1.1.2", true);
//              //Third is denied
//              try {
//                  HTTPDownloader d3=addUploader(upman, rfd, "1.1.1.3", true);
//                  Assert.that(false, "Downloader denied");
//              } catch (TryAgainLaterException e) {            
//              }
//              //But killing 1st allows third
//              kill(d1);
//              HTTPDownloader d3=addUploader(upman, rfd, "1.1.1.3", true);
//              //But not a fourth
//              try {
//                  HTTPDownloader d4=addUploader(upman, rfd, "1.1.1.3", true);
//                  Assert.that(false, "Downloader denied");
//              } catch (TryAgainLaterException e) {            
//              }
//              System.out.println("passed");
//          } catch (Throwable e) {
//              System.out.println("FAILED!");
//              e.printStackTrace();
//          }
//      }

    
//      /** 
//       * Adds a downloader to upman, returning the downloader.  The downloader
//       * is connected but doesn't actually read the contents.
//       *
//       * @param upman the UploadManager responsible for accepting or denying
//       *  the upload
//       * @param rfd the file requested by the downloader
//       * @param ip the address reported by the downloader.  This need
//       *  not be a connectable address, though it must be resolvable
//       * @param block if true, force the uploader to block after writing
//       *  hundredth byte
//       * @exception TryAgainLaterException the downloader was denied 
//       *  because upman was busy
//       * @exception IOException some other exception
//       */     
//      private static HTTPDownloader addUploader(final UploadManager upman, 
//                                                RemoteFileDesc rfd,
//                                                String ip,
//                                                boolean block) 
//              throws TryAgainLaterException, IOException {        
//          //Allow some fudging to prevent race conditons.
//          try { Thread.sleep(200); } catch (InterruptedException e) { }

//          PipedSocketFactory psf=new PipedSocketFactory(
//                  "127.0.0.1", ip, block ? 100 : -1, -1);
//          final Socket sa=psf.getSocketA();
//          Thread runner=new Thread() {
//              public void run() {
//                  upman.acceptUpload(sa);
//              }
//          };
//          runner.setDaemon(true);
//          runner.start();       

//          Socket sb=psf.getSocketB();
//          File tmp=File.createTempFile("UploadManager_Test", "dat");
//          HTTPDownloader downloader=new HTTPDownloader(                                           sb, rfd, tmp, 0, rfd.getSize());
//          try {
//              downloader.connect(); //may throw TryAgainLater, etc.
//              return downloader;
//          } finally {
//              tmp.delete();
//          }
//      }

//      private static void kill(HTTPDownloader downloader) {
//          downloader.stop();
//          try { Thread.sleep(400); } catch (InterruptedException ignored) { }
//      }
}
