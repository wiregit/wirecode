/**
 * auth: rsoule
 * file: HTTPUploader.java
 * desc: Read data from disk and write to the net.
 *
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|

package com.limegroup.gnutella;

import java.io.*;
import java.net.*;
import java.util.Date;
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
    /** Used to check if the file exists. */
    private FileManager _fileManager;
	

	/** the DownloadManager does not seem to have a constructor,
		which makes me wonder if this class should have one.  for
		now i'll leave it in, and consult with chris later. */
	public UploadManager() {}


	////////////////// Main Public Interface ///////////////////

	/** 
     * Initializes this manager. <b>This method must be called before any other
     * methods are used.</b> 
     *     @param callback the UI callback to notify of download changes
     *     @param router the message router to use for sending push requests
     *     @param acceptor used to get my IP address and port for pushes
     *     @param fileManager used to check if files exist
     */
    public void initialize(ActivityCallback callback,
                           MessageRouter router,
                           Acceptor acceptor,
                           FileManager fileManager) {
        _callback = callback;
        _router = router;
        _acceptor = acceptor;
        _fileManager = fileManager;
    }
                

	public void acceptUpload(Socket socket) {

	}











	/*******************   OLD STUFF  ***********************/

}


//  public class HTTPUploader implements Runnable, Comparable {
//      /**
//       * The list of all files that we've tried unsuccessfully to upload
//       * via pushes.  (Here successful means we were able to connect.
//       * It does not mean the file was actually transferred.) If we get
//       * another push request from one of these hosts (e.g., because the
//       * host is firewalled and sends multiple push packets) we will not
//       * try again.
//       *
//       * INVARIANT: for all i>j, ! failedPushes[i].before(failedPushes[j])
//       *
//       * This is not really the best place to store the data, but it is
//       * the most appropriate place until HTTPManager is persistent.
//       * Besides, it's consistent with the push requests stored in
//       * HTTPDownloader.
//       */
//      private static List /* of PushRequestedFile */ failedPushes=
//          Collections.synchronizedList(new LinkedList());
//      private static List /* of PushRequestedFile */ attemptingPushes=
//          Collections.synchronizedList(new LinkedList());
//      /** The maximum time in SECONDS after an unsuccessful push until we will
//       *  try the push again.  This should be larger than the 15+4*30=135 sec
//       *  window in which Gnotella resends pushes by default */
//      private static final int PUSH_INVALIDATE_TIME=60*5;  //5 minutes

//      /** The number of uploads in progress, i.e., already connected and
//       *  transferring.  This is used to calculate the bandwidth available to
//       *  throttled uploads.  LOCKING: obtain uploadsCountLock first. */
//      private static volatile int uploadCount=0;
//      private static Object uploadCountLock=new Object();
//      private boolean       uploadCountIncremented = false;
//      private boolean       limitExceeded  = false;
//      private boolean       _isPushAttempt = false;


//  	/**
//  	 * A Map of all the uploads in progress.  If the number
//  	 * of uploads by a single user exceeds the SettingsManager's
//  	 * uploadsPerPerson_ variable, then the upload is denied, 
//  	 * and the used gets a Try Again Later message.
//       *
//       * LOCKING: obtain _uploadsInProgress' monitor before modifying
//       */
//  	private static Map /* String -> Integer */ _uploadsInProgress =
//  		new HashMap();


//  	/**
//  	 * A SortedSet of the full uploads in progress.  
//  	 * This is used to shutdown "Gnutella" uploads as needed.
//  	 */
//  	private static List _fullUploads =
//  		Collections.synchronizedList(new LinkedList());



//      //////////// Initialized in Constructors ///////////
//      public static final int NOT_CONNECTED = 0;
//      public static final int CONNECTED = 1;
//      public static final int ERROR = 2;
//      public static final int COMPLETE = 3;
//      /** One of NOT_CONNECTED, CONNECTED, ERROR, COMPLETE */
//      private int    _state;
//      private String _stateString=null;
//      /** True if the connection is server-side,
//       *  false if client-side (actively establishes connection). */
//      private boolean _isServer;

//      private ActivityCallback _callback;
//      private FileManager _fmanager;

//      /** The name of the remote host, in dotted decimal */
//      private String _host;
//      /** The port on the remote host. */
//      private int _port;
//      /** Index of the file to upload. */
//      private int _index;
//      /** Begin and end indices to upload.  If _uploadEnd
//       *  is 0, upload the whole file.
//       *  INVARIANT: _uploadBegin<=_uploadEnd */
//      private int _uploadBegin;
//      private int _uploadEnd;
//      private int _amountRead;
//      private int _priorAmountRead;


//      //////////// Initialized Multiple Places //////////
//      /** The socket connecting us to the host.
//       *  Server-side: initialized in constructor.
//       *  Client-side: initialized in connect(). */
//      private Socket _socket;
//      /** The stream to the foreign host.
//       *  Server-side: initialized in constructor.
//       *  Client-side: initialized in run(). */
//      private OutputStream _ostream;
//      /** The filename.
//       *  Server-side: initialized in constructor.
//       *  Client-side: initialized in prepareFile */
//      private String _filename;
//      /** The client ID of the pusher, as a hex string.
//       *  Server-side: NOT USED.
//       *  Client-side: constructor. */
//      private String _clientGUID;


//      ////////// Initialized in prepareFile ////////////
//      private FileDesc _fdesc;
//      FileInputStream _fis;
//      private int BUFFSIZE = 1024;
//      private int _sizeOfFile;

//  	// The User-Agent for the request.  Used to prioritize newer clients
//  	private String  _userAgent   = null;
//  	// A cleanup boolean for the main run loop
//  	private boolean _cleanupDone = false;

//      /**
//       * Prepares a server-side upload.  The file
//       * will actually be transferred when run() is
//       * called.
//       *
//       * @param s the socket to use for transfer
//       * @param file the file to transfer
//       * @param index the index of the file to transfer
//       * @param m my manager.  (used for callbacks, etc.)
//       * @param begin the begin offset (in bytes) of the file
//       *  to transfer
//       * @param end the end offset (in bytes) of the file to
//       *  transfer, or 0 to transfer the whole thing.  <b>Must
//       *  be greater than equal to end.</b>
//       */
//      public HTTPUploader(Socket s, String file,
//              int index, ActivityCallback callback,
//              int begin, int end) {
//          _state = CONNECTED;
//          _isServer = true;
//          _callback = callback;
//          _fmanager = FileManager.instance();
//          _host = s.getInetAddress().getHostAddress();
//          _port = s.getPort();
//          _index = index;
//          _uploadBegin = begin;
//          _uploadEnd = end;
//          _amountRead = 0;
//          _priorAmountRead = 0;
//          _socket = s;
//          try {
//              //The try-catch below is a work-around for JDK bug 4091706.
//              _ostream = s.getOutputStream();
//          } catch (IOException e) {
//              _state = ERROR;
//          } 
//          _filename = file;
//      }


//      /**
//       * Prepares a client-side upload via push.  This does not block.
//       * The call to run will actually establish the connection, send
//       * GIV, wait for GET, and send OK followed by data.
//       *
//       * If we tried to push this file earlier but could not connect,
//       * this is put in the ERROR state.  Subsequently calling run
//       * will cause the method to exit immediately.  The GUI will
//       * never be notified.
//       *
//       * @param host the host to push the file to
//       * @param port the port to contact that host on
//       * @param index the index of the file to push
//       * @param guidString my client ID (given to remote host as
//       *  a crude form of authentication) as a hex String representation
//       *  as given by GUID.toHexString
//       * @param m my manager.  (used for callbacks, etc.)
//       */
//      public HTTPUploader(String host, int port,
//              int index,
//              String guidString, ActivityCallback callback) {
//          _state = NOT_CONNECTED;
//          _isServer = false;
//          _callback = callback;
//          _fmanager = FileManager.instance();
//          _host = host;
//          _port = port;
//          _index = index;
//          _uploadBegin = 0;
//          _uploadEnd = 0;
//          _amountRead = 0;
//          _priorAmountRead = 0;
//          _clientGUID=guidString;
//          _isPushAttempt = true;

//          synchronized (failedPushes) {
//              //First remove all files that were pushed more than a few
//              //minutes ago.
//              Date time=new Date();
//              time.setTime(time.getTime()-(PUSH_INVALIDATE_TIME*1000));
//              Iterator iter=failedPushes.iterator();
//              while (iter.hasNext()) {
//                  PushedFile pf=(PushedFile)iter.next();
//                  if (pf.before(time))
//                      iter.remove();
//              }

//              //Now see if we tried unsuccessfully to push this file
//              //earlier.  If so put this in the error state.  Note that
//              //this could be integrated with the loop above, but the
//              //savings aren't worth it.
//              iter=failedPushes.iterator();
//              PushedFile thisFile=new PushedFile(_host, _port, _index);
//              while (iter.hasNext()) {
//                  PushedFile pf=(PushedFile)iter.next();
//                  if (pf.equals(thisFile)) {
//                      _state = ERROR;
//                      break;
//                  }
//              }
//          }
//          // Check for and add attempted push for this file
//          synchronized (attemptingPushes) {
//              //Now see if we are attempting to pus this now
//              Iterator iter=attemptingPushes.iterator();
//              PushedFile thisFile=new PushedFile(_host, _port, _index);
//              while (iter.hasNext()) {
//                  PushedFile pf=(PushedFile)iter.next();
//                  if (pf.equals(thisFile)) {
//                      _state = ERROR;
//                      break;
//                  }
//              }
//              if ( _state != ERROR ) {
//                  attemptingPushes.add(thisFile);
//              }
//          }
//      }

//      /**
//       * Connects a client-side upload connection.
//       *
//       * @requires this is an unconnected client-side upload connection
//       * @modifies this, network
//       * @effects establishes the outgoing connection, sends the give
//       *  message, and waits for the get.  Throws IOException if any
//       *  part of this sequence failed.
//       */
//      public void connect() throws IOException {
//          //1. Establish connection.
//          try {
//              _socket=new Socket(_host, _port);
//          } catch (SecurityException e) {
//              throw new IOException();
//          }

//          _ostream=_socket.getOutputStream();
//          BufferedWriter out=new BufferedWriter(new OutputStreamWriter(_ostream));

//          //3. Send GIV
//          Assert.that(_filename!=null);
//          String give="GIV "+_index+":"+_clientGUID+"/"+_filename+"\n\n";
//          out.write(give);
//          out.flush();

//          //4. Wait for   "GET /get/0/sample.txt HTTP/1.0"
//          //   But use timeouts and don't wait too long.
//          //   This code is stolen from HTTPManager.
//          //   It should really be factored into some method.
//          //   TODO2:  range headers.
//          try {
//              //The try-catch below is a work-around for JDK bug 4091706.
//              ByteReader in=null;
//              try {                
//                  in=new ByteReader(_socket.getInputStream());
//              } catch (Exception e) {
//                  throw new IOException();
//              }
//              _socket.setSoTimeout(SettingsManager.instance().getTimeout());
//              String line=in.readLine();

//  			//  TODO1:  Once this code has been out long enough, we need to 
//  			//  revisit this code and get the pushed UserAgent.  
//  			//  Currently, we do not depreferenced pushed "Gnutella"
//  			//  Because we were sending Gnutella back ourselves.
//  			//String line2=in.readLine();
//  			//

//              _socket.setSoTimeout(0);
//              if (line==null)
//                  throw new IOException();

//              if (! line.startsWith("GET "))
//                  throw new IOException();
//              String command=line.substring(4,line.length());

//                                                 /* I need to get the filename */
//              String parse[] = HTTPUtil.stringSplit(command, '/');
//                                                 /* and the index, but i'm */
//                                                 /* upset this is way hackey */
//              if (parse.length!=4)
//                  throw new IOException();
//              if (!parse[0].equals("get"))
//                  throw new IOException();

//              //Check that the filename matches what we sent
//              //in the GIV request.  I guess it doesn't need
//              //to match technically, but we check to be safe.
//              String filename = parse[2].substring(0,
//                  parse[2].lastIndexOf("HTTP")-1);
//              if (! filename.equals(_filename))
//                  throw new IOException();
//              int index = java.lang.Integer.parseInt(parse[1]);
//              if (index!=_index)
//              throw new IOException();

//              //2. Check for upload overload
//              while ( limitExceeded = testAndIncrementUploadCount() )
//              {
//  				// If you can't blow away a "Gnutella" upload
//  				if ( ! HTTPUploader.checkForLowPriorityUpload(_userAgent) )
//  				{
//                      //send 503 Limit Exceeded Headers
//                      doLimitReachedAfterConnect();
//                      throw new IOException();
//  				}
//              }

//          } catch (IndexOutOfBoundsException e) {
//              throw new IOException();
//          } catch (NumberFormatException e) {
//              throw new IOException();
//          } catch (IllegalArgumentException e) {
//              throw new IOException();
//          }
//      }

//      /**
//       * @requires filename filled in.
//       * @modifies this._fdesc...this._priorAmountRead
//       * @effects prepares the file named _filename for uploading by setting
//       *  this._fdesc...this._priorAmountRead.  Throws FileNotFoundException
//       *  if the file can't be uploaded.
//       */
//      private void prepareFile() throws FileNotFoundException {
//          //TODO: this used to have a special case for
//          //zero-length files.  Does it still work ok?
//          try {                            /* look for the file */
//              _fdesc = _fmanager.get(_index);
//          }                                /* if its not found... */
//          catch (IndexOutOfBoundsException e) {
//              throw new FileNotFoundException();
//          }

//          /* For client-side uploads, get name. For server-side
//           * uploads, check to see that the index matches the
//           * filename. */
//          if (_filename==null) {
//              _filename=_fdesc._name;
//          } else {
//             if (! (_fdesc._name).equals(_filename))  /* matches the name */
//                 throw new FileNotFoundException();
//          }

//          _sizeOfFile = _fdesc._size;

//          try {
//              String f = _fdesc._path;
//              File myFile = new File(f);  /* _path is the full name */
//              String foo = myFile.toString();
//              _fis = new FileInputStream(myFile);
//          } catch (Exception e) {
//              throw new FileNotFoundException();
//          }
//      }

//      public String getFileName() {
//          return _filename;
//      }

//      public int getContentLength() {
//          return _sizeOfFile;
//      }

//      public int getAmountRead() {
//          return _amountRead;
//      }

//      public int getPriorAmountRead() {
//          return _priorAmountRead;
//      }

//      public void setPriorAmountRead( int val ) {
//          _priorAmountRead = val;
//      }

//      public InetAddress getInetAddress() {
//          if (_socket != null)
//              return _socket.getInetAddress();
//          else {
//              try {
//                  return InetAddress.getByName(new String(_host));
//              } catch (Exception e) {
//              }
//          }
//          return null;
//      }

//      public void writeHeader() throws IOException {
//          try {
//              String str = "HTTP 200 OK \r\n";

//              String version = SettingsManager.instance().getCurrentVersion();

//              _ostream.write(str.getBytes());
//              str = "Server: LimeWire " + version + " \r\n";
//              _ostream.write(str.getBytes());
//              String type = getMimeType();       /* write this method later  */
//              str = "Content-type:" + type + "\r\n";
//              _ostream.write(str.getBytes());
//              str = "Content-length:"+ (_sizeOfFile - _uploadBegin) + "\r\n";
//              _ostream.write(str.getBytes());

//              // Version 0.5 of limewire misinterpreted Content-range
//              // to be 1 - n instead of 0 - (n-1), but because this is
//              // an optional field in the regular case, we don't need
//              // to send it.
//              if (_uploadBegin != 0) {
//                  str = "Content-range: bytes=" + _uploadBegin  +
//                  "-" + ( _sizeOfFile - 1 )+ "/" + _sizeOfFile + "\r\n";
//              }

//              _ostream.write(str.getBytes());

//              str = "\r\n";
//              _ostream.write(str.getBytes());
//          } catch (IOException e) {
//              throw e;
//          }
//      }

//      public void run() {
		
//  		prepareFile();
		
		
		
//  		_cleanupDone = false;
//  		_fullUploads.add(this);  // Add to the User-Agent priority queue
//  		boolean uplimit = testAndIncrementNumUploads();
//  		_callback.addUpload(this);
//  		//1. For push requests only, establish the connection.
//  		if (! _isServer) {
//  			connect();
//  			_stateString = "Try Again Later";
//  			doLimitReached(_socket);
//  			throw new IOException();
//  		} // end of if (! _isServer)
		
//  		//3. Actually do the transfer.
//  		if ( uploadCountIncremented ) {
//  			doUpload();             //sends headers via writeHeader
//  			_state = COMPLETE;
//  		}
//  	} catch (IOException e) {
//  		_state = ERROR;
//  	} finally {
//  		doCleanup();
//  	}
//  }

//  	/**
//  	 *  Cleanup after an upload.  This should only be called from the finally 
//       *  clause in run or a forced cleanup.
//  	 */
//  	public synchronized void doCleanup()
//  	{
//  		if ( _cleanupDone )
//  			return;
//  		_fullUploads.remove(this);
//  		decrementNumUploads();
//  		if ( uploadCountIncremented )
//  			synchronized(uploadCountLock) { uploadCount--; }
//  		if ( _isPushAttempt )
//  			cleanupAttemptedPush();
//  		shutdown();
//  		_callback.removeUpload(this);
//  		_cleanupDone = true;
//  	}

//      private void cleanupAttemptedPush()
//      {
//          Iterator iter;
//          synchronized (attemptingPushes) {
//              //Now see if we are attempting to push this now
//              iter=attemptingPushes.iterator();
//              PushedFile thisFile=new PushedFile(_host, _port, _index);
//              while (iter.hasNext()) {
//                  PushedFile pf=(PushedFile)iter.next();
//                  if (pf.equals(thisFile)) {
//                      iter.remove();
//                      break;
//                  }
//              }
//          }
//      }

//      public void doUpload() throws IOException {
//          writeHeader();
//          int c = -1;
//          int available = 0;
//          byte[] buf = new byte[1024];

//          long a = _fis.skip(_uploadBegin);
//          _amountRead+=a;

//          SettingsManager manager=SettingsManager.instance();
//          int speed=manager.getUploadSpeed();
//          if (speed==100) {
//              //Special case: upload as fast as possible
//              while (true) {
//                  try {
//                      c = _fis.read(buf);
//                  }
//                  catch (IOException e) {
//                      throw e;
//                  }
//                  if (c == -1)
//                      break;
//                  try {
//                      _ostream.write(buf, 0, c);
//                  } catch (IOException e) {
//                      throw e;
//                  }
//                  _amountRead += c;
//              }
//          } else {
//              //Normal case: throttle uploads. Similar to above but we
//              //sleep after sending data.
//              final int cycleTime=1000;
//          outerLoop:
//              while (true) {
//                  //1. Calculate max upload bandwidth for this connection in
//                  //kiloBYTES/sec.  The user has specified a theoretical link bandwidth
//                  //(in kiloBITS/s) and the percentage of this bandwidth to use for
//                  //uploads. We divide this bandwidth equally among all the
//                  //uploads in progress.  TODO: if one connection isn't using all
//                  //the bandwidth, some coul get more.
//                  int theoreticalBandwidth=
//                      (int)(((float)manager.getConnectionSpeed())/8.f);
//                  int maxBandwidth=(int)(theoreticalBandwidth*((float)speed/100.)
//                                               /(float)uploadCount);

//                  //2. Send burstSize bytes of data as fast as possible, recording
//                  //the time to send this.  How big should burstSize be?  We want
//                  //the total time to complete one send/sleep iteration to be
//                  //about one second. (Any less is inefficient.  Any more might
//                  //cause the user to see erratic transfer speeds.)  So we send
//                  //1000*maxBandwidth bytes.
//                  int burstSize=maxBandwidth*cycleTime;
//                  int burstSent=0;
//                  Date start=new Date();
//                  while (burstSent<burstSize) {
//                      try {
//                          c = _fis.read(buf);
//                      }
//                      catch (IOException e) {
//                          throw e;
//                      }
//                      if (c == -1)
//                          break outerLoop;  //get out of BOTH loops
//                      try {
//                          _ostream.write(buf, 0, c);
//                      } catch (IOException e) {
//                          throw e;
//                      }
//                      _amountRead += c;
//                      burstSent += c;
//                  }
//                  Date stop=new Date();

//                  //3.  Pause as needed so as not to exceed maxBandwidth.
//                  int elapsed=(int)(stop.getTime()-start.getTime());
//                  int sleepTime=cycleTime-elapsed;
//                  if (sleepTime>0) {
//                      try {
//                          Thread.currentThread().sleep(sleepTime);
//                      } catch (InterruptedException e) { }
//                  }
//  //                  System.out.println("  sent "+burstSent+" of "+burstSize
//  //                                     +" bytes in "+elapsed+" msecs at "
//  //                                     +measuredBandwidth+" kb/sec, paused for "
//  //                                     +sleepTime+" msecs");
//              }
//          }

//          try {
//              _ostream.close();
//          }
//          catch (IOException e) {
//              throw e;
//          }

//          _state = COMPLETE;

//      }

 //     /**
//       *   Get an unsynchronized version of the total upload count.
//       */
//      public static int getUploadCount()
//      {
//          return uploadCount;
//      }

//      /**
//       *  Increment the uploadCount if the limit has not been exceeded.
//       *  Record whether this is an active upload.
//       *  Return true if the limit has been exceeded.
//       */
   

//      private String getMimeType() {         /* eventually this method should */
//          String mimetype;                /* determine the mime type of a file */
//          mimetype = "application/binary"; /* fill in the details of this later */
//          return mimetype;                   /* assume binary for now */
//      }

//      private void doNoSuchFile() {
//          /* Sends a 404 Not Found message */
//          try {
//              /* is this the right format? */
//              String str;
//              str = "HTTP/1.1 404 Not Found \r\n";
//              _ostream.write(str.getBytes());
//              str = "\r\n";     /* Even if it is, can Gnutella */
//              _ostream.write(str.getBytes());
//              _ostream.flush();    /* clients handle it? */
//          } catch (Exception e) {
//              _state = ERROR;
//          }
//      }

//      /**
//       *   Handle too many upload requests
//       */
//      public static void doLimitReached(Socket s) {
//          OutputStream ostream = null;
//          /* Sends a 503 Service Unavailable message */
//          try {
//              ostream = s.getOutputStream();
//              /* is this the right format? */
//              String str;
//              String errMsg = "Server busy.  Too many active downloads.";
//              str = "HTTP/1.1 503 Service Unavailable\r\n";
//              ostream.write(str.getBytes());
//              str = "Server: " + "LimeWire" + "\r\n";
//              ostream.write(str.getBytes());
//              str = "Content-Type: text/plain\r\n";
//              ostream.write(str.getBytes());
//              str = "Content-Length: " + errMsg.length() + "\r\n";
//              ostream.write(str.getBytes());
//              str = "\r\n";
//              ostream.write(str.getBytes());
//              ostream.write(errMsg.getBytes());
//              ostream.flush();
//          } catch (Exception e) {
//          }
//          try {
//              ostream.close();
//              s.close();
//          } catch (Exception e) {
//          }
//      }

//      /**
//       *   Handle a web based freeloader
//       */
//      public static void doFreeloaderResponse(Socket s) {
//          OutputStream ostream = null;
//          /* Sends a 402 Browser Request Denied message */
//          try {
//              ostream = s.getOutputStream();
//              /* is this the right format? */
//              String str;
//              String errMsg = HTTPPage.responsePage;
//              str = "HTTP 200 OK \r\n";
//              ostream.write(str.getBytes());
//              str = "Server: " + "LimeWire" + "\r\n";
//              ostream.write(str.getBytes());
//              str = "Content-Type: text/html\r\n";
//              ostream.write(str.getBytes());
//              str = "Content-Length: " + errMsg.length() + "\r\n";
//              ostream.write(str.getBytes());
//              str = "\r\n";
//              ostream.write(str.getBytes());
//              ostream.write(errMsg.getBytes());
//              ostream.flush();
//          } catch (Exception e) {
//          }
//          try {
//              ostream.close();
//              s.close();
//          } catch (Exception e) {
//          }
//      }

//      /**
//       *   Handle too many upload requests after push has connected
//       */
//      public void doLimitReachedAfterConnect() {

//          /* Sends a 503 Service Unavailable message */
//          try {
//              /* is this the right format? */
//              String str;
//              String errMsg = "Server busy.  Too many active downloads.";
//              str = "3 Upload limit reached\r\n";
//              _ostream.write(str.getBytes());
//              str = "\r\n";
//              _ostream.write(str.getBytes());
//              _ostream.write(errMsg.getBytes());
//              _ostream.flush();
//          } catch (Exception e) {
//          }
//          try {
//              _ostream.close();
//              _socket.close();
//          } catch (Exception e) {
//          }
//      }


//      public void shutdown()
//      {
//          try {
//              _fis.close();
//          } catch (Exception e) {
//          }
//          try {
//              _ostream.close();
//          } catch (Exception e) {
//          }
//          try {
//              _socket.close();
//          } catch (Exception e) {
//          }
//      }

//      public int getState() {
//          return _state;
//      }


//  	/**
//  	 *  Retrieve what is normally a text error state
//  	 */
//      public String getStateString() {
//          return _stateString;
//      }

//  	/**
//  	 *  Set what is normally a text error state
//  	 */
//      public void setStateString( String stateString ) {
//          _stateString = stateString;
//      }

//      ///** Unit test for timestamp stuff. */
//      /*
//      public static void main(String args[]) {
//          ConnectionManager manager=new ConnectionManager(6344);
//          FileManager.instance().addDirectories(".");
//          manager.setActivityCallback(new Main());
//          HTTPUploader uploader;

//          //Try failed upload #1.
//          System.out.println("Trying upload to non-existent host.  Please wait...");
//          uploader=new HTTPUploader("bogus.host", 6347, 0,
//                                    "bogus client string", manager);
//          Assert.that(uploader.getState()!=ERROR);
//          uploader.run();
//          Assert.that(uploader.getState()==ERROR);

//          //Try uploading same file again.  This should not even bother connecting.
//          System.out.println("Trying upload again...");
//          uploader=new HTTPUploader("bogus.host", 6347, 0,
//                                    "bogus client string", manager);
//          Assert.that(uploader.getState()==ERROR);

//          //Wait the expiration time and try again.
//          int n=PUSH_INVALIDATE_TIME+2;
//          System.out.println("Waiting for a "+n+"seconds...");
//          try {
//              Thread.currentThread().sleep(n*1000);
//          } catch (InterruptedException e) { }

//          System.out.println("  ...and checking state.");
//          uploader=new HTTPUploader("bogus.host", 6347, 0,
//                                    "bogus client string", manager);
//          Assert.that(uploader.getState()!=ERROR);
//      }
//      */


//      /** A file that we requested via a push message. */
//      private static class PushedFile {
//          String host;
//          int port;
//          int index;
//          Date time;

//          /**
//           * @param host the host to connect to, in symbolic form or
//           *  dotted-quad notation
//           * @param port the port to connect to on host
//           * @param index the index of the file we are trying to GIV
//           */
//          public PushedFile(String host, int port, int index) {
//              this.host=host;
//              this.port=port;
//              this.index=index;
//              this.time=new Date();
//          }

//          /** Returns true if this request was made before the given time. */
//          public boolean before(Date time) {
//              return this.time.before(time);
//          }

//          public boolean equals(Object o) {
//              if (! (o instanceof PushedFile))
//                  return false;

//              PushedFile pf=(PushedFile)o;
//              return this.host.equals(pf.host)
//                  && this.port==pf.port
//                  && this.index==pf.index;
//          }

//          public int hashCode() {
//              //This is good enough
//              return host.hashCode()+index;
//          }

//          public String toString() {
//              return "<"+host+", "+port+", "+index+",  "+time+">";
//          }
//      }
//  }
