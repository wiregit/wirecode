/**
 * auth: rsoule
 * file: HTTPDownloader.java
 * desc: Read data from the net and write to disk.
 *
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|

package com.limegroup.gnutella;

import java.io.*;
import java.net.*;
import java.util.Date;
import java.util.StringTokenizer;
import com.sun.java.util.collections.*;

public class HTTPDownloader implements Runnable {

    public static final int NOT_CONNECTED = 0;
    public static final int CONNECTED     = 1;
    public static final int ERROR         = 2;
    public static final int COMPLETE      = 3;
    /** Push initialization */
    public static final int REQUESTING    = 4;
    public static final int QUEUED        = 5;

    private static final String TryAgainLater = "Try Again Later";


    protected InputStream _istream;
    protected String _filename;
    protected int _sizeOfFile;
    protected int _amountRead;
    protected MessageRouter _router;
    protected Acceptor _acceptor;
    protected ActivityCallback _callback;
    private Socket _socket;
    protected String _downloadDir;
    private FileOutputStream _fos;

	String _protocol;
	String _host;
	int _port;
    int _index;
	byte[] _guid;
    protected ByteReader _br;
    private int _mode;

    protected int    _state;
    protected String _stateString = null;

    private boolean _resume;
    private boolean        _wasShutdown = false;
    private HTTPDownloader _replacement = null;
    private PushRequestedFile savedPRF  = null;

	protected boolean _smartDownload;

    /** Time count of some requesting operation */
    private int timeCount;

    /**
     * The list of all files we've requested via push messages.  Only
     * files matching this description may be accepted from incoming
     * connections.  Duplicates ARE allowed in the (rare) case that
     * the user wants to download the same file multiple times.  This
     * list is static because it must be shared by all
     * HTTPDownloader's.  Note that it is synchronized.
     */
    private static List /* of PushRequestedFile */ requested=
        Collections.synchronizedList(new LinkedList());
    /** The maximum time, in SECONDS, allowed between a push request and an
     *  incoming push connection. */
    private static final int PUSH_INVALIDATE_TIME=3*60;  //3 minutes

    /*
     * Server side download in response to incoming PUT request.
     *
     * @requires a GIV command was just read from s, e.g.,
     *     "GIV 0:BC1F6870696111D4A74D0001031AE043/sample.txt"
     * @effects Creates a downloader that will download the specified file
     *  in the background when the run method is called.   Throws
     *  IllegalAccessException if we never requested the file.
     */
    public HTTPDownloader(Socket s, String file, int index, byte[] clientGUID,
                          MessageRouter router, Acceptor acceptor,
                          ActivityCallback callback)
              throws IllegalAccessException {
        //Authentication: check if we are requested via push.  First we clear
        //out very old entries in requested.
        Date time=new Date();
        time.setTime(time.getTime()-(PUSH_INVALIDATE_TIME*1000));
        synchronized (requested) {
            Iterator iter=requested.iterator();
            while (iter.hasNext()) {
                PushRequestedFile prf=(PushRequestedFile)iter.next();
                if (prf.before(time))
                    iter.remove();
            }
        }

        byte[] ip=s.getInetAddress().getAddress();
        PushRequestedFile prf=new PushRequestedFile(clientGUID, file,
                                ip, index, this);

        // Find the original Push request and mutate it into this object
        boolean found = findAndMutate(prf);
        //boolean found=requested.remove(prf);
        if (! found)
            throw new IllegalAccessException();

        _guid=clientGUID;
        construct(file, router, acceptor, callback);
        _mode = 1;
        _socket = s;
        _index=index;
    }

    /* The client side get */
    public HTTPDownloader(String protocol, String host,
              int port, int index, String file, MessageRouter router,
              Acceptor acceptor, ActivityCallback callback, byte[] guid,
              int size ) {
        construct(file, router, acceptor, callback);
        _mode = 2;
        _socket = null;
        _protocol = protocol;
        _host = host;
        _index = index;
        _port = port;
        _guid = guid;
        _sizeOfFile = size;
    }

	/** 
	 * This is a bit of a hack.  I've added this protected 
	 * constructor so that the subclass SmartDownloader can
	 * actually subclass without the normal constructors
	 */
	protected HTTPDownloader() {

	}


    public void setDownloadInfo(HTTPDownloader down) {

    _protocol = down.getProtocal();
    _host = down.getHost();
    _index = down.getIndex();
    _port = down.getPort();
    _guid = down.getGUID();
	_sizeOfFile = down.getContentLength();

    }

    public String getProtocal() {return _protocol;}
    public String getHost() {return _host;}
    public int getIndex() {return _index;}
    public int getPort() {return _port;}
    public byte[] getGUID() {return _guid;}
    public String getFileName() {return _filename;}
    public int getContentLength() {return _sizeOfFile;}
    public int getAmountRead() {return _amountRead;}
    public int getState() {return _state;}
    public String getStateString() { return _stateString; }
    public void setResume() {_resume = true;}

    public InetAddress getInetAddress() {
        if (_socket != null)
            return _socket.getInetAddress();
        else {
            try {
                return InetAddress.getByName(new String(_host));
            }
            catch (Exception e) {
            }
        }
        return null;
    }


    public void construct(String file, MessageRouter router, Acceptor acceptor,
                          ActivityCallback callback) {
        _state = NOT_CONNECTED;
        _filename = file;
		_amountRead = 0;
		 //_amountRead = 1;
        _sizeOfFile = -1;
        _router = router;
        _acceptor = acceptor;
        _callback = callback;
        _downloadDir = "";
		_smartDownload = false;
    }

    /**
     *  Ensure that a download is not queued prior to activation
     */
    public void ensureDequeued() {
        if ( _state == QUEUED )
            _state = NOT_CONNECTED;
    }


    /** Sends an HTTP GET request, i.e., "GET /get/0/sample.txt HTTP/1.0"
     *  (Response will be read later in readHeader()). */
    public void initOne() {
        try {
            _istream = _socket.getInputStream();
            _br = new ByteReader(_istream);
        }
        catch (Exception e) {
            _state = ERROR;
            return;
        }

        _state = CONNECTED;

        _resume = false;

        //Note that the following code is similar to initTwo except
        //that it does not use the built in Java URL/URLConnection
        //classes (since we've already
        //established the connection).  Ideally we should have special methods
        //to handle the HTTP formatting, but this is kind of a hack.
        try {
            OutputStream os = _socket.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(os);
            BufferedWriter out=new BufferedWriter(osw);

            out.write("GET /get/"+_index+"/"+_filename+" HTTP/1.0\r\n");
            out.write("User-Agent: Gnutella\r\n");
            out.write("\r\n");
            out.flush();
        } catch (IOException e) {
            _state = ERROR;
            return;
        }
    }


    public void initTwo() {
        URLConnection conn;

        String furl = "/get/" + String.valueOf(_index) + "/" + _filename;

        _state = NOT_CONNECTED;
        conn = null;

        try {
            URL url = new URL(_protocol, _host, _port, furl);
            conn = url.openConnection();
            conn.connect();
            _istream = conn.getInputStream();
            _br = new ByteReader(_istream);
        }
        catch (java.net.MalformedURLException e) {
            sendPushRequest();
            return;
        }
        catch (IOException e) {

			//  There appears to be a delay in going to Push here so preset it
            _state    = REQUESTING;

            // Handle immediate error cases
            String str=conn.getHeaderField(0);
            if ( str != null && str.indexOf(" 404 ") > 0 )
            {
                _state = ERROR;
                _stateString = "File Not Found";
                return;
            }
            else if ( str != null && str.indexOf(" 503 ") > 0 )
            {
                _state = ERROR;
                _stateString = TryAgainLater;
                return;
            }
            sendPushRequest();
            return;
        }
        catch (Exception e) {
            _state = ERROR;
            return;
        }

        _resume = false;

        _state = CONNECTED;
    }

    public void initThree() {
		
		// Reset any error message
        _stateString = null;

		// First, get the incomplete directory where the 
		// file will be temperarily downloaded.
		SettingsManager sm = SettingsManager.instance();
		String incompleteDir;
		incompleteDir = sm.getIncompleteDirectory();
		// check to see if we actually get a directory
		if (incompleteDir == "") {
			// the incomplete directory is null
			// so i don't think we should be downloading
			_state = ERROR;
			return;
		}
		
		// now, check to see if a file of that name alread
		// exists in the temporary directory.
		String incompletePath;
		incompletePath = incompleteDir + _filename;
		File incompleteFile = new File(incompletePath);
		// incompleteFile represents the file as it would
		// be named in the temporary incomplete directory.

		int start = 0;
		_resume = false;
		// if there is a file, set the initial amount
		// read at the size of that file, otherwise leave
		// it at zero.
		if (incompleteFile.exists()) {
			// dont alert an error if the file doesn't 
			// exist, just assume a starting range of 0;
			start = (int)incompleteFile.length();
			_resume = true;
		}
		// convert the int start to String equivalent
		String startRange = java.lang.String.valueOf(start);

		// Now, try to establish a socket connection
		URLConnection conn;
		String furl = "/get/" + String.valueOf(_index) + "/" + _filename;
				
		try {
			URL url = new URL(_protocol, _host, _port, furl);
			conn = url.openConnection();
			conn.setRequestProperty("Range", "bytes="+ startRange + "-");
            conn.connect();
		} catch (Exception e) {
			// for some reason the connection could not
			// be established;
			_stateString = "Resumed Connection Failed";
            _state = ERROR;		_resume = true;
            return;
		}
		// try to get the input stream from the connection
		try {
            _istream = conn.getInputStream();
            _br = new ByteReader(_istream);
		}
		catch (Exception e) {
			// for some reason the inputstream could
			// not be opened.
			_stateString = "Resumed Connection Failed";
            _state = ERROR;
            return;
		}

		_state = CONNECTED;


    }

    public void run() {

        if (_mode == 1){
            // Need to mutate the original connection into this connection
            //if ( _state != QUEUED )
                //_callback.addDownload(this);
            initOne();
        }
        else if (_mode == 2) {
            if ( _state != QUEUED )
                _callback.addDownload(this);
            initTwo();
        }
        else if (_mode == 3) {
            initThree();
        }
        else
            Assert.that(false, "Illegal state for download connection.");

        if (_state == CONNECTED) {
			try {
				doDownload();
			}
			catch(IOException e) {
				_state = ERROR;
				_stateString = e.getMessage();
				if (_stateString.equals(""))
					_stateString = "Error";
			}
			// check to see if there is an error condition 
			// after attempting the download
			if (_state == ERROR) {
				// if there is, close the streams
				errorClose();
			}
            _callback.removeDownload(this);
        } else if (_state == ERROR) {
            _callback.removeDownload(this);
        }

        //TODO: what if in queued state?  -  GUI Currently Handles it.
    }

    public void resume() {
        _mode = 3;
    }

    public void setQueued() {
        _state = QUEUED;
    }



    /**
     * @requires this is an outgoing (normal) HTTPDownloader
     * @effects sends a push request for the file specified at
     *  at construction time from host specified in the constructor.
     */
    public void sendPushRequest() {
        final byte[] clientGUID=_guid;

        if ( _wasShutdown )
            return;

        _state    = REQUESTING;
        timeCount = 0;


        //Record this push so incoming push connections can be verified.
        Assert.that(clientGUID!=null);
        Assert.that(_filename!=null);
        byte[] remoteIP=null;
        try {
            remoteIP=InetAddress.getByName(_host).getAddress();
        } catch (UnknownHostException e) {
            //This shouldn't ever fail because _host should be
            //in dotted decimal fashion.  If it were to someone be
            //thrown legitimately, there would be no point in continuing
            //since we would never accept the corresponding incoming
            //push connection.
            _state = ERROR;
            return;
        }
        PushRequestedFile prf=new PushRequestedFile(clientGUID, _filename,
                                remoteIP, _index, this);
        savedPRF = prf;
        requested.add(prf);

        //TODO1: Should this be a new mGUID or the mGUID of the corresponding
        //query and reply?  I claim it should be a totally new mGUID, since
        //push requests are ONLY routed based on their cGUID.  This way, clients
        //could record mGUID's of push requests to avoid routing duplicate
        //push requests.  While there should never be duplicate pushes if people
        //route push request, this is unfortunately NOT the case.
        byte[] messageGUID = Message.makeGuid();
        byte ttl = SettingsManager.instance().getTTL();
        byte[] myIP = _acceptor.getAddress();
        int myPort= _acceptor.getPort();

        PushRequest push = new PushRequest(messageGUID, ttl, clientGUID,
                           _index, myIP, myPort);

        try {
            _router.sendPushRequest(push);
        } catch (Exception e) {
            _state = ERROR;
            return;
        }
    }
	
	public void doDownload() throws IOException {

		readHeader();

		SettingsManager settings = SettingsManager.instance();
		
		String download_dir = settings.getSaveDirectory();
		String incomplete_dir = settings.getIncompleteDirectory();
		
		// a reference file that will be stored, until the download
		// is complete.
		File incomplete_file = new File(incomplete_dir, _filename);
		String path_to_incomplete = incomplete_file.getAbsolutePath();
		
		// the eventual fully downloaded file, and the path to it.
		File complete_file = new File(download_dir, _filename);
		String path_to_complete = complete_file.getAbsolutePath();
		
		File shared = new File(download_dir);
		String shared_path = shared.getCanonicalPath();
		
		File parent_of_shared = new File(complete_file.getParent());
		String path_to_parent = parent_of_shared.getCanonicalPath();
		
		if (!path_to_parent.equals(shared_path)) {
			// need to add an error message here
			throw new IOException("Invalid path");  
		}

		if ( ( complete_file.exists() ) && (!_smartDownload ) ){
			// ask the user if the file should be overwritten
			if ( ! _callback.overwriteFile(_filename) ) 
				throw new IOException("File Already Exists");
		}
		
		_fos = new FileOutputStream(path_to_incomplete, _resume);

		int c = -1;
		
		byte[] buf = new byte[1024];

		while (true) {
			
  			if (_amountRead == _sizeOfFile) {
				_state = COMPLETE;
				break;
			}
			
  			// just a safety check.  hopefully this wouldn't
  			// happen, but if it does, need a way to exit 
  			// gracefully...
  			if (_amountRead > _sizeOfFile) 
				throw new IOException("File Size To Large");
			
			c = _br.read(buf);

			if (c == -1) 
				break;
			
			_fos.write(buf, 0, c);
			
			_amountRead+=c;

		}  // end of while loop

		_br.close();
		_fos.close();

		//Move from temporary directory to final directory.
		if ( _amountRead == _sizeOfFile ) {
			//If target doesn't exist, this will fail silently.  Otherwise,
			//it's always safe to do this since we prompted the user above.
			complete_file.delete();
			boolean ok = incomplete_file.renameTo(complete_file);
			if (! ok) 
				throw new IOException("Couldn't Move to Library");
			//renameTo is not guaranteed to work, esp. when the
			//file is being moved across file systems.  

			_state = COMPLETE;
			FileManager.getFileManager().addFileIfShared(path_to_complete);

		}			
		
		else 
			throw new IOException("File Incomplete");

	}

	// this method handles cexpplicitly closing the streams 
	// if an error condition was somehow reached.
	private void errorClose() {
		try {

			if (_br != null) // i dont think this should ever happen
				_br.close();
			
			if (_fos != null)
				_fos.close();

		} catch(IOException e) {
			return;
		}
	}


    public void readHeader() throws IOException {
        String str = " ";

        boolean foundLength = false;
        boolean foundRangeInitial = false;
        boolean foundRangeFinal = false;
        int     tempSize = -1;
        int     lineNumber = -1;

        while (true) {

			str = _br.readLine();
			lineNumber++;

            //EOF?
            if (str==null || str.equals(""))
                break;

            // Handle errors not conforming to HTTP spec
            if ( lineNumber == 0 ) {
            
                // Handle a 503 error from Gnotella/Gnutella
                if ( str.equals("3") || 
					 str.startsWith("3 Upload limit reached") )
					throw new IOException("Try Again Later");

                // Handle a 404 error from Gnotella/Gnutella
                if ( str.equals("4") || str.startsWith("4 File Not Found") ) 
					throw new IOException("File Not Found");
                
            }

            if (str.toUpperCase().indexOf("CONTENT-LENGTH:") != -1)  {

                String sub;
                try {
                    sub=str.substring(15);
                } catch (IndexOutOfBoundsException e) {
					throw new IOException("Problem Reading Header");
                }
                sub = sub.trim();
                try {
                    tempSize = java.lang.Integer.parseInt(sub);
                }
                catch (NumberFormatException e) {
					throw new IOException("Problem Reading Header");
                }

                foundLength = true;
            }

            if (str.toUpperCase().indexOf("CONTENT-RANGE:") != -1) {

				int dash;
				int slash;

				String beforeDash;
				int numBeforeDash;

				String afterSlash;
				int numAfterSlash;

				String beforeSlash;
				int numBeforeSlash;

                try {
					str = str.substring(21);

					dash=str.indexOf('-');
					slash = str.indexOf('/');

					afterSlash = str.substring(slash+1);
					afterSlash = afterSlash.trim();

                    beforeDash = str.substring(0, dash);
					beforeDash = beforeDash.trim();

					beforeSlash = str.substring(dash+1, slash);
					beforeSlash = beforeSlash.trim();

                } catch (IndexOutOfBoundsException e) {
					throw new IOException("Problem Reading Header");
                }
				try {
					numAfterSlash = java.lang.Integer.parseInt(afterSlash);
					numBeforeDash = java.lang.Integer.parseInt(beforeDash);
                    numBeforeSlash = java.lang.Integer.parseInt(beforeSlash);
                }
                catch (NumberFormatException e) {
					throw new IOException("Problem Reading Header");
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
				

				_amountRead = numBeforeDash;
				tempSize = numAfterSlash;
                _resume = true;
				foundLength = true;
				
            }
        }

        if (foundLength) {
            if ( tempSize != -1 ) {
				_sizeOfFile = tempSize;
				
			}
        } 
		else 
			throw new IOException("Problem Reading Header");

    }

    public void shutdown()
    {
        _wasShutdown = true;

        if ( _state != COMPLETE )
            _state = ERROR;

        // Deactivate any pending push
        if ( savedPRF != null )
            requested.remove(savedPRF);

        try {
            _fos.close();
            _socket.close();
        } catch (Exception e) {
        }
    }

    /**
     *  Tell this downloader who (push) is replacing it
     */
    public void setReplacement( HTTPDownloader down )
    {
        _replacement = down;
    }

    /**
     *  Get this downloaders (push) replacement
     */
    public HTTPDownloader getReplacement( )
    {
        return( _replacement );
    }


    /**
     *  Find a push request and flag it for replacement by incoming
     */
    public boolean findAndMutate(PushRequestedFile prf)
    {
        PushRequestedFile curPRF = null;
        Iterator iter=requested.iterator();
        while (iter.hasNext()) {
            curPRF=(PushRequestedFile)iter.next();

            if (prf.equals(curPRF))
                break;
        }

        // If found then tell the original request, what its replacement is
        if ( curPRF != null ) {
            requested.remove(curPRF);

            // Drop a cancelled request
            if ( curPRF.down.getState() != REQUESTING )
            return false;

            curPRF.down.setReplacement(prf.down);
                setDownloadInfo(curPRF.down);
            return true;
        }
        return false;
    }

    /**
     *  Get an integer time count
     */
    public int getTimeCount() {
        return (timeCount);
    }

    /**
     *  Inc an integer time count
     */
    public void incTimeCount() {
        timeCount++;
    }

}

/** A file that we requested via a push message. */
class PushRequestedFile {
    byte[] clientGUID;
    String filename;
    byte[] ip;
    int index;
    Date time;
    HTTPDownloader down;

    public PushRequestedFile(byte[] clientGUID, String filename,
                 byte[] ip, int index, HTTPDownloader down) {
        this.clientGUID=clientGUID;
        this.filename=filename;
        this.ip=ip;
        this.index=index;
        this.time=new Date();
        this.down=down;
    }

    /** Returns true if this request was made before the given time. */
    public boolean before(Date time) {
        return this.time.before(time);
    }

    public boolean equals(Object o) {
        if (! (o instanceof PushRequestedFile))
            return false;

        PushRequestedFile prf=(PushRequestedFile)o;
        return Arrays.equals(clientGUID, prf.clientGUID)
            && filename.equals(prf.filename)
            //If the following line is uncommented,
            //the IP address on the socket must match that
            //of the query reply.  But this will almost
            //always fail if the remote host is behind a firewall--
            //which is the whole reason to use pushes in the
            //first place!  Yes, this is a potential security
            //flaw.  TODO: We should really allow users to adjust
            //whether they want to take the risk.
//          && Arrays.equals(ip, prf.ip)
            && index==prf.index;
    }


    public int hashCode() {
        //This is good enough since we'll rarely request the
        //same file over and over.
        return filename.hashCode();
    }

    public String toString() {
        String ips=ByteOrder.ubyte2int(ip[0])+"."
                 + ByteOrder.ubyte2int(ip[1])+"."
                 + ByteOrder.ubyte2int(ip[2])+"."
                 + ByteOrder.ubyte2int(ip[3]);
        return "<"+filename+", "+index+", "
            +(new GUID(clientGUID).toString())+", "+ips+">";
    }
}

