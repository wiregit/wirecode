package com.limegroup.gnutella;

import com.limegroup.gnutella.uploader.*;
import java.net.*;
import java.io.*;
import com.sun.java.util.collections.*;
import java.util.Date;

/**
 * The list of all the uploads in progress.
 *
 *
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|

public class UploadManager {

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
    private int _activeUploads= 0;

	/** The callback for notifying the GUI of major changes. */
    private ActivityCallback _callback;
    /** The message router to use for pushes. */
    private MessageRouter _router;
    /** Used for get addresses in pushes. */
    private Acceptor _acceptor;

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
                           Acceptor acceptor) {
        _callback = callback;
        _router = router;
        _acceptor = acceptor;

    }
                

    public synchronized void acceptUpload(Socket socket) {

		HTTPUploader uploader;
		GETLine line;
		try {
			line = parseGET(socket);
		} catch (IOException e) {
			// the GET line was wrong, just exit.
			return;
		} 

		// check if it complies with the restrictions.
		// if no, send an error.  
		// if yes, constroct the uploader
		uploader = new HTTPUploader(line._file, socket, line._index, this);

		String host = socket.getInetAddress().getHostAddress();

		insertAndTest(uploader, host);

		UploadRunner runner = new UploadRunner(uploader, host);
		Thread upThread = new Thread(runner);
		upThread.setDaemon(true);
		upThread.start();
	}

	public synchronized void acceptPushUpload(String file, 
											  String host, int port, 
											  int index, String guid) { 

		clearFailedPushes();

		Uploader uploader;
		uploader = new HTTPUploader(file, host, port, index, guid, this);
		// testing if we are either currently attempting a push, 
		// or we have unsuccessfully attempted a push with this host in the
		// past.
		if ( (! testAttemptedPush(host) )  ||
			 (! testFailedPush(host) ) )
			return;

		insertAndTest(uploader, host);
		insertAttemptedPush(host);

		UploadRunner runner = new UploadRunner(uploader, host);
		Thread upThread = new Thread(runner);
		upThread.setDaemon(true);
		upThread.start();
		
	}

	/** 
     * This method was added to adopt BearShare's busy bit
     * in the Query Hit Descriptor.  It takes no parameters
	 * and returns 'true' if there are no slots available
	 * for uploading.  It returns 'false' if there are slots
	 * available.  
     */
	public boolean isBusy() {
		// testTotalUploadLimit returns true is there are
		// slots available, false otherwise.
		return (! testTotalUploadLimit());
	}

	public int uploadsInProgress() {
		return _activeUploads;
	}

	//////////////////////// Private Interface /////////////////////////

    /** Increments the count of uploads in progress for host.
     *  If uploader has exceeded its limits, places it in LIMIT_REACHED state.
     *  Notifies callback of this.
     *      @modifies _uploadsInProgress, uploader, _callback */
	private void insertAndTest(Uploader uploader, String host) {
		// add to the Map
		insertIntoMap(host);

		if ( (! testPerHostLimit(host) ) || 
			 ( ! testTotalUploadLimit() ) )
			uploader.setState(Uploader.LIMIT_REACHED);

		_callback.addUpload(uploader);		

	}

    /** Increments the count of uploads in progress for host. 
     *      @modifies _uploadsInProgress */
	private void insertIntoMap(String host) {
		int numUploads = 1;
		// check to see if the map aleady contains
		// a reference to this host.  if so, get its
		// value.
		if ( _uploadsInProgress.containsKey(host) ) {
			Integer myInteger = (Integer)_uploadsInProgress.get(host);
			numUploads += myInteger.intValue();
		}
		_uploadsInProgress.put(host, new Integer(numUploads));		
	}

	private void removeFromMap(String host) {
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
		
	private boolean testTotalUploadLimit() {
		int max = SettingsManager.instance().getMaxUploads();
		int current = _uploadsInProgress.size();
		if (current > max)
			return false;
		return true;
	}

	private void insertFailedPush(String host) {
		_failedPushes.add(new PushedFile(host));
	}
	
	private boolean testFailedPush(String host) {
		PushedFile pf = new PushedFile(host);
		PushedFile pfile;
		Iterator iter = _failedPushes.iterator();
		while ( iter.hasNext() ) {
			pfile = (PushedFile)iter.next();
			if ( pf.equals(pfile) ) 
				return false;
		}
		return true;

	}

	private void insertAttemptedPush(String host) {
		_attemptingPushes.add(new PushedFile(host));
	}

	private boolean testAttemptedPush(String host) {
		PushedFile pf = new PushedFile(host);
		PushedFile pfile;
		Iterator iter = _attemptingPushes.iterator();
		while ( iter.hasNext() ) {
			pfile = (PushedFile)iter.next();
			if ( pf.equals(pfile) ) 
				return false;
		}
		return true;
	}
	
	private void removeAttemptedPush(String host) {
		PushedFile pf = new PushedFile(host);
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


	////////////////// Handle Bandwith Allocation //////////////////

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

	////////////////// Private Bandwith Calculation //////////////////
	
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
	 * @return a percentage of bandwith available.
	 */
	private float getBandwithPercentage() {
		return 0;
	}



	//////////////////////// Handle Parsing /////////////////////////

	

	private GETLine parseGET(Socket socket) throws IOException {
		try {
			// Set the timeout so that we don't do block reading.
			socket.setSoTimeout(SettingsManager.instance().getTimeout());
			// open the stream from the socket for reading
			InputStream istream = socket.getInputStream();
			ByteReader br = new ByteReader(istream);
			// read the first line. if null, throw an exception
			String str = br.readLine();
			if (str == null)
				throw new IOException();
			//Expecting "GET /get/0/sample.txt HTTP/1.0"
			// parse this for the appropriate information
			// find where the get is...
			int g = str.indexOf("/get/");
			// find the next "/" after the "/get/".  the number 
			// between should be the index;
			int d = str.indexOf( "/", (g + 5) ); 
			// get the index
			String str_index = str.substring( (g+5), d );
			int index = java.lang.Integer.parseInt(str_index);
			// get the filename, which should be right after
			// the "/", and before the next " ".
			int f = str.indexOf( " HTTP/", d );
			String file = str.substring( (d+1), f);
			return new GETLine(index, file);
		} catch (NumberFormatException e) {
			throw new IOException();
		} catch (IndexOutOfBoundsException e) {
			throw new IOException();
		}
  	}


	private class GETLine {
  		public int _index;
  		public String _file;
  		public GETLine(int index, String file) {
  			_index = index;
  			_file = file;
  		}

  	}

    /*
     * handles the threading 
     */
    private class UploadRunner implements Runnable {
		private Uploader _up;
		private String _host;
		public UploadRunner(Uploader up, String host) {
			_up = up;
			_host = host;
		}
		public void run() {
			try {
				// is connect always safe to call?  it should be
				// if it is a non-push upload, then connect should
				// just return.  if there is an error, it should
				// be because the connection failed.
				_up.connect();
				// start doesn't throw an exception.  rather, it
				synchronized(UploadManager.this) {
				    _activeUploads++;
				}
				
				// handles it internally.  is this the correct
				// way to handle it?
				_up.start();
			} catch (IOException e) {
				// if it fails, insert it into the push failed list
				// _up.setState(Uploader.PUSH_FAILED);
				insertFailedPush(_host);
				// remove it from the uploads in progress
				return;
			} finally {			    
				synchronized(UploadManager.this) {
				    _activeUploads--;
					removeFromMap(_host);
					removeAttemptedPush(_host);
				}
			}
			
		}  // run
	} // class UploadRunner
	

	/**
	 * keeps track of the host and time of pushed files
	 */
	private class PushedFile {
		private String _host;
		private Date _time;

		public PushedFile(String host) {
			_host = host;
			_time = new Date();
		}
		
		public boolean equals(PushedFile pf) {
			if (_host != pf._host)
				return false;
			return true;
		}
		
		public boolean before(Date time) {
			return _time.before(time);
		}
		
	}

}











