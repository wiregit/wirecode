package com.limegroup.gnutella;
import java.net.*;
import java.io.*;
import com.sun.java.util.collections.*;

/**
 * The list of all the uploads in progress.
 *
 *
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|

public class UploadManager {
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
        Collections.synchronizedList(new LinkedList());
    private List /* of PushRequestedFile */ _attemptingPushes=
        Collections.synchronizedList(new LinkedList());
	/**
	 * A Map of all the uploads in progress.  If the number
	 * of uploads by a single user exceeds the SettingsManager's
	 * uploadsPerPerson_ variable, then the upload is denied, 
	 * and the used gets a Try Again Later message.
     *
     * LOCKING: obtain _uploadsInProgress' monitor before modifying
     */
	private static Map /* String -> Integer */ _uploadsInProgress =
		new HashMap();
	/**
	 * A SortedSet of the full uploads in progress.  
	 * This is used to shutdown "Gnutella" uploads as needed.
	 */
	private static List _fullUploads =
		Collections.synchronizedList(new LinkedList());

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
                

	public void acceptUpload(Socket socket) {
		System.out.println("acceptUpload called!");
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
		uploader = new HTTPUploader(line._file, socket, line._index);

		String host = socket.getInetAddress().getHostAddress();

		// add to the Map
		insertIntoMap(host);
		try {
			testPerHostLimit(host); 
			testTotalUploadLimit();
		} catch (ExceededTotalUploadLimitException e) {
			uploader.setState(Uploader.LIMIT_REACHED);
		} catch (ExceededPerHostLimitException e) {
			uploader.setState(Uploader.LIMIT_REACHED);
		}
		// 3) start the upload
		_callback.addUpload(uploader);
		uploader.start();
		// remove from the map
		removeFromMap(host);

	}

	public void acceptPushUpload(String file, String host, int port, 
								 int index, String guid) { 
		Uploader uploader;
		uploader = new HTTPUploader(file, host, port, index, guid);
		// check to see if the file is in the attempted pushes
		// or if the upload limits have been reached for some 
		// reason.
		// add it to the uploads in progress
		try {
			testAttemptedPush(host);
		} catch (FailedPushException e) {
			// if we've attempted this push before, don't
			// do anything.  
			return;  
		}
		insertIntoMap(host);
		insertAttemptedPush(host);
		try {
			testFailedPush(host);
			testPerHostLimit(host); 
			testTotalUploadLimit();
		} catch (ExceededTotalUploadLimitException e) {
			uploader.setState(Uploader.LIMIT_REACHED);
		} catch (FailedPushException e) {
			uploader.setState(Uploader.PUSH_FAILED);
		} catch (ExceededPerHostLimitException e) {
			uploader.setState(Uploader.LIMIT_REACHED);
		}
		try {
			// attempt to connect via push
			uploader.connect();
		} catch (IOException e) {
			// if it fails, insert it into the push failed list
			uploader.setState(Uploader.PUSH_FAILED);
			insertFailedPush(host);
		}

		_callback.addUpload(uploader);		
		uploader.start();

		// remove it from the uploads in progress
		removeFromMap(host);
		removeAttemptedPush(host);
		
	}

	//////////////////////// Private Interface /////////////////////////


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

	private void testPerHostLimit(String host) 
		throws ExceededPerHostLimitException {
		if ( _uploadsInProgress.containsKey(host) ) {
			Integer value = (Integer)_uploadsInProgress.get(host);
			int current = value.intValue();
			int max = SettingsManager.instance().getUploadsPerPerson();
			if (current >= max)
				throw new ExceededPerHostLimitException();
		}
	}
		
	private void testTotalUploadLimit() 
		throws ExceededTotalUploadLimitException {
		int max = SettingsManager.instance().getMaxUploads();
		int current = _uploadsInProgress.size();
		if (current >= max)
			throw new ExceededTotalUploadLimitException();
	}

	private void insertFailedPush(String host) {
		_failedPushes.add(host);
	}
	
	private void testFailedPush(String host) 
		throws FailedPushException {
		if ( _failedPushes.contains(host) )
			throw new FailedPushException(); 
	}

	private void insertAttemptedPush(String host) {
		_attemptingPushes.add(host);
	}

	private void testAttemptedPush(String host)
		throws FailedPushException {
		if ( _attemptingPushes.contains(host) )
			throw new FailedPushException();
			 
	}
	
	private void removeAttemptedPush(String host) {
		_attemptingPushes.remove(host);
	}


	//////////////////////// Handle Parsing /////////////////////////

	

	private GETLine parseGET(Socket socket) throws IOException {

		System.out.println("GETLINE PARSEGET");
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
  		int f = str.indexOf( " ", d );
  		String file = str.substring( (d+1), f);
		


		System.out.println("  >> THe file " + file);
		System.out.println("  >>THe index " + index);

  		return new GETLine(index, file);
  	}


	private class GETLine {
  		public int _index;
  		public String _file;
  		public GETLine(int index, String file) {
  			_index = index;
  			_file = file;
  		}

  	}

}



