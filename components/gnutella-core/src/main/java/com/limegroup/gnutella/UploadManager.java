package com.limegroup.gnutella;
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
	 * A Map of all the uploads in progress.  If the number
	 * of uploads by a single user exceeds the SettingsManager's
	 * uploadsPerPerson_ variable, then the upload is denied, 
	 * and the used gets a Try Again Later message.
     *

     */
	private static Map /* String -> Integer */ _uploadsInProgress =
		new HashMap();
	/**
	 * A SortedSet of the full uploads in progress.  
	 * This is used to shutdown "Gnutella" uploads as needed.
	 */
	private static List _fullUploads = new LinkedList();

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

		System.out.println("acceptUpload");
		

		HTTPUploader uploader;
		GETLine line;
		try {
			line = parseGET(socket);
		} catch (IOException e) {
			// the GET line was wrong, just exit.
			return;
		} 
		
		clearFailedPushes();

		// check if it complies with the restrictions.
		// if no, send an error.  
		// if yes, constroct the uploader
		uploader = new HTTPUploader(line._file, socket, line._index);

		if (uploader.getState() == uploader.COULDNT_CONNECT)
			return;

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

		System.out.println("acceptPushUpload");

		clearFailedPushes();

		Uploader uploader;
		uploader = new HTTPUploader(file, host, port, index, guid);
		// check to see if the file is in the attempted pushes
		// or if the upload limits have been reached for some 
		// reason.

		insertAndTest(uploader, host);

		if (! testAttemptedPush(host) )
			return;

		insertAttemptedPush(host);

		if (! testFailedPush(host) )
			uploader.setState(Uploader.PUSH_FAILED);

		UploadRunner runner = new UploadRunner(uploader, host);
		Thread upThread = new Thread(runner);
		upThread.setDaemon(true);
		upThread.start();
		
	}

	//////////////////////// Private Interface /////////////////////////


	private void insertAndTest(Uploader uploader, String host) {
		// add to the Map
		insertIntoMap(host);

		if ( (! testPerHostLimit(host) ) || 
			 ( ! testTotalUploadLimit() ) )
			uploader.setState(Uploader.LIMIT_REACHED);

		_callback.addUpload(uploader);		

	}

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
		// throws ExceededPerHostLimitException {
		if ( _uploadsInProgress.containsKey(host) ) {
			Integer value = (Integer)_uploadsInProgress.get(host);
			int current = value.intValue();
			int max = SettingsManager.instance().getUploadsPerPerson();
			if (current > max)
				// throw new ExceededPerHostLimitException();
				return false;
		}
		return true;
	}
		
	private boolean testTotalUploadLimit() {
		// throws ExceededTotalUploadLimitException {
		int max = SettingsManager.instance().getMaxUploads();
		int current = _uploadsInProgress.size();
		if (current > max)
			// throw new ExceededTotalUploadLimitException();
			return false;
		return true;
	}

	private void insertFailedPush(String host) {
		_failedPushes.add(new PushedFile(host));
	}
	
	private boolean testFailedPush(String host) {
		//  // throws FailedPushException {
//  		if ( _failedPushes.contains(host) )
//  			return false;
//  		// throw new FailedPushException(); 
//  		return true;
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
		//		throws FailedPushException {
		//  if ( _attemptingPushes.contains(host) )
//  			throw new FailedPushException();
		PushedFile pf = new PushedFile(host);
		PushedFile pfile;
		Iterator iter = _attemptingPushes.iterator();
		while ( iter.hasNext() ) {
			pfile = (PushedFile)iter.next();
			if ( pf.equals(pfile) ) 
				// throw new FailedPushException();
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
				_attemptingPushes.remove(host);
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

	//////////////////////// Handle Parsing /////////////////////////

	

	private GETLine parseGET(Socket socket) throws IOException {
		try {
			// System.out.println("GETLINE PARSEGET");
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











