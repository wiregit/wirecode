import com.sun.java.util.collections.*;
import java.io.*;
import java.net.*;
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

	/** the DownloadManager does not seem to have a constructor,
		which makes me wonder if this class should have one.  for
		now i'll leave it in, and consult with chris later. */
	public UploadManager() {}
	
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

	}

	//////////////////////// Private Interface /////////////////////////

	private static class GETLine(int index, String file) {
		int _index;
		String _file;
		GETLine(int index, String file) {
			_index = index;
			_file = file;
		}

	}

	private static GETLine parseGET(Socket socket) throws IOException {
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
		String str_index = str.subString( (g+5), d );
		int index = java.lang.Integer.parseInt(str_index);
		// get the filename, which should be right after
		// the "/", and before the next " ".
		int f = str.indexOf( " ", d );
		String file - str.subString( (d+1), f);
		
		return new GETLine(index, file);
	}

	private void readHeader(Socket socket) throws IOException {
        String str = " ";
        int uploadBegin = 0;
        int uploadEnd = 0;
		String userAgent;
		
		InputStream istream = socket.getInputStream();
		ByteReader br = new ByteReader(istream);
        
		while (true) {
			// read the line in from the socket.
            str = br.readLine();
			// break out of the loop if it is null or blank
            if ( (str==null) || (str.equals("")) )
                break;
			// Look for the Range: header
			// it will be in one of three forms.  either
			// ' - n ', ' m - n', or ' 0 - '
            if (str.indexOf("Range: bytes=") != -1) {
                String sub = str.substring(13);
				// remove the white space
                sub = sub.trim();   
                char c;
				// get the first character
                c = sub.charAt(0);
				// - n  
                if (c == '-') {  
                    String second = sub.substring(1);
                    second = second.trim();
                    uploadEnd = java.lang.Integer.parseInt(second);
                }
                else {                
					// m - n or 0 -
                    int dash = sub.indexOf("-");
                    String first = sub.substring(0, dash);
                    first = first.trim();
                    uploadBegin = java.lang.Integer.parseInt(first);
                    String second = sub.substring(dash+1);
                    second = second.trim();
                    if (!second.equals("")) 
                        uploadEnd = java.lang.Integer.parseInt(second);
                    
                }
            }

			// check the User-Agent field of the header information
			if (str.indexOf("User-Agent:") != -1) {
				// check for netscape, internet explorer,
				// or other free riding downoaders
				if (SettingsManager.instance().getAllowBrowser() == false) {
					// if we are not supposed to read from them
					// throw an exception
					if( (str.indexOf("Mozilla") != -1) ||
						(str.indexOf("DA") != -1) ||
						(str.indexOf("Download") != -1) ||
						(str.indexOf("FlashGet") != -1) ||
						(str.indexOf("GetRight") != -1) ||
						(str.indexOf("Go!Zilla") != -1) ||
						(str.indexOf("Inet") != -1) ||
						(str.indexOf("MIIxpc") != -1) ||
						(str.indexOf("MSProxy") != -1) ||
						(str.indexOf("Mass") != -1) ||
						(str.indexOf("MyGetRight") != -1) ||
						(str.indexOf("NetAnts") != -1) ||
						(str.indexOf("NetZip") != -1) ||
						(str.indexOf("RealDownload") != -1) ||
						(str.indexOf("SmartDownload") != -1) ||
						(str.indexOf("Teleport") != -1) ||
						(str.indexOf("WebDownloader") != -1) ) {
							HTTPUploader.doFreeloaderResponse(_socket);
						    throw new IOException("Web Browser");
						}
				}
				userAgent = str.substring(11).trim();
			}
		}
	}
}



