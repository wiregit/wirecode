/**
 * Read data from the net and write to disk.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|

package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.SocketOpener;
import java.io.*;
import java.net.*;
import com.limegroup.gnutella.util.CommonUtils;
import java.util.StringTokenizer;

/**
 * Downloads a file over an HTTP connection.  This class is as simple as possible.
 * It does not deal with retries, prioritizing hosts, etc.  Nor does it check
 * whether a file already exists; it just writes over anything on disk.<p>
 *
 * It is necessary to explicitly initialize an HTTPDownloader with the
 * connect(..)  method.  (Hence HTTPDownloader behaves much like Connection.)
 * Typical use is as follows: 
 *
 * <pre>
 * HTTPDownloader dl=new HTTPDownloader(host, port);
 * dl.connect();
 * dl.doDownload();
 * </pre>
 */
public class HTTPDownloader {
    /** The length of the buffer used in downloading. */
    public static final int BUF_LENGTH=1024;

    private RemoteFileDesc _rfd;
    private boolean _isPush;
	private long _index;
	private String _filename; 
	private byte[] _guid;

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
	private RandomAccessFile _fos;
	private Socket _socket;  //initialized in HTTPDownloader(Socket) or connect
    private File _incompleteFile;

	private int _port;
	private String _host;
	
	private boolean _chatEnabled = false; // for now

    /**
     * Creates an uninitialized client-side normal download.  Call connect() on
     * this before any other methods.  Non-blocking.
     *
     * @param rfd complete information for the file to download, including
     *  host address and port
     * @param incompleteFile the temp file to use while downloading, which need
     *  not exist.
     * @param start the place to start reading from the network and writing to 
     *  the file
     * @param stop the last byte to read+1
     */
	public HTTPDownloader(RemoteFileDesc rfd,
                          File incompleteFile,
                          int start,
                          int stop) {
        //Dirty secret: this is implemented with the push constructor!
        this(null, rfd, incompleteFile, start, stop);
        _isPush=false;
	}	

	/**
     * Creates an uninitialized server-side push download.  Call connect() on
     * this before any other methods.  Non-blocking.
     * 
     * @param socket the socket to download from.  The "GIV..." line must
     *  have been read from socket.  HTTP headers may not have been read or 
     *  buffered.
     * @param rfd complete information for the file to download, including
     *  host address and port
     * @param incompleteFile the temp file to use while downloading, which need
     *  not exist.
     * @param start the place to start reading from network and writing to
     *  the file
     * @param stop the last byte to read+1
     */
	public HTTPDownloader(Socket socket,
                          RemoteFileDesc rfd,
                          File incompleteFile,
                          int start,
                          int stop) {
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
        
		_amountRead = 0;
        _amountToRead = stop-start;
		_initialReadingPoint = start;
    }


    ///////////////////////////////// Connection /////////////////////////////

    /** 
     * Initializes this without timeout; same as connect(0). 
     * @see connect(int)
     */
    public void connect() throws IOException {
        connect(0);
    }

    /** 
     * Initializes this by connecting to the remote host (in the case of a
     * normal client-side download), sending a GET request, and reading all
     * headers.  Blocks for up to timeout milliseconds trying to connect, unless
     * timeout is zero, in which case there is no timeout.  This MUST be
     * uninitialized, i.e., connect may not be called more than once.
     *
     * @param timeout the timeout to use for connecting, in milliseconds,
     *  or zero if no timeout
     * @exception CantConnectException could not establish a TCP connection
     * @exception TryAgainLaterException the host is busy
     * @exception FileNotFoundException the host doesn't recognize the file
     * @exception NotSharingException the host isn't sharing files (BearShare)
     * @exception miscellaneous error 
     */
	public void connect(int timeout) throws IOException {        
        //Connect, if not already done.  Ignore 
        //The try-catch below is a work-around for JDK bug 4091706.
        InputStream istream=null;
        try {            
            if (_socket==null) {
                if (timeout==0)  //minor optimization
                    _socket=new Socket(_host, _port);
                else
                    _socket=(new SocketOpener(_host, _port)).connect(timeout);
            }
            istream=_socket.getInputStream(); 
        } catch (Exception e) {
            throw new CantConnectException();
        }
        _byteReader = new ByteReader(istream);

        //Write GET request and headers.  TODO: we COULD specify the end of the
        //range (i.e., start+bytes).  But why bother?
        OutputStream os = _socket.getOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(os);
        BufferedWriter out=new BufferedWriter(osw);
        String startRange = java.lang.String.valueOf(_initialReadingPoint);
        out.write("GET /get/"+_index+"/"+_filename+" HTTP/1.0\r\n");
        out.write("User-Agent: "+CommonUtils.getVendor()+"\r\n");
        out.write("Range: bytes=" + startRange + "-\r\n");
        SettingsManager sm=SettingsManager.instance();
		if (sm.getChatEnabled() ) {
            //Get our own address and port.  This duplicates the getAddress an
            //getPort methods of Acceptor.  Unfortunately we don't have a
            //reference to Acceptor, nor do we particularly want one.
            int port;
            String host;
            if ( sm.getForceIPAddress() ) {
                port = sm.getForcedPort();
                host = sm.getForcedIPAddressString();
            } else {
                port = sm.getPort();
                host = _socket.getLocalAddress().getHostAddress();
            }
            out.write("Chat: " + host + ":" + port + "\r\n");            
		}
        out.write("\r\n");
        out.flush();

        //Read response.
        readHeaders();
	}
	

    /*
     * Reads the headers from this, setting _initialReadingPoint and _amountToRead.
     * Throws any of the exceptions listed in connect().  
     */
	private void readHeaders() throws IOException {
		if (_byteReader == null) 
			throw new ReaderIsNullException();

		// Read the response code from the first line and check for any errors
		String str = _byteReader.readLine();  
		if (str==null || str.equals(""))
			return;
        int code=parseHTTPCode(str);		

		//Accept any 2xx's, but reject other codes.
		if ( (code < 200) || (code > 300) ) {
			if (code == 404)
				throw new 
				    com.limegroup.gnutella.downloader.FileNotFoundException();
			else if (code == 410)
				throw new 
                    com.limegroup.gnutella.downloader.NotSharingException();
			else if (code == 503)
				throw new TryAgainLaterException();
			// a general catch for 4xx and 5xx's
			// should maybe be a different exception?
			// else if ( (code >= 400) && (code < 600) ) 
			else 
				throw new IOException();			
		}

        //Now read each header...
		while (true) {            
			str = _byteReader.readLine();			
            if (str==null || str.equals(""))
                break;

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
                    throw new IOException(
                        "Unexpected start offset; too dumb to recover");
            }
        }
        
    }


    /**
     * Returns the HTTP response code from the given string, throwing
     * an exception if it couldn't be parsed.
     *
     * @param str an HTTP response string, e.g., "HTTP 200 OK \r\n"
     * @exception NoHTTPOKException str didn't contain "HTTP"
     * @exception ProblemReadingHeaderException some other problem
     *  extracting result code
     */
    private static int parseHTTPCode(String str) throws IOException {		
		StringTokenizer tokenizer = new StringTokenizer(str, " ");		
		String token;

		// just a safety
		if (! tokenizer.hasMoreTokens() )
			throw new NoHTTPOKException();

		token = tokenizer.nextToken();
		
		// the first token should contain HTTP
		if (token.toUpperCase().indexOf("HTTP") < 0 )
			throw new NoHTTPOKException();
		
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

    
    /////////////////////////////// Download ////////////////////////////////

    /*
     * Downloads the content from the server and writes it to a temporary
     * file.  Blocking.  This MUST be initialized via connect() beforehand, and
     * doDownload MUST NOT have already been called.
     *  
     * @exception FileIncompleteException transfer interrupted, either
     *  locally or remotely.  TODO: is this ALWAYS thrown during 
     *  interruption?
     * @exception FileCantBeMovedException file downloaded but  couldn't be
     *  moved to library
     * @exception IOException file couldn't be downloaded for some other
     *  reason 
     */
	public void doDownload() throws IOException {
		_fos = new RandomAccessFile(_incompleteFile, "rw");
        _fos.seek(_initialReadingPoint);

		int c = -1;
		
		byte[] buf = new byte[BUF_LENGTH];

		while (true) {
			//It's possible that we've read more than requested because of a
			//call to setAmountToRead from another thread.  This used to be an
			//error resulting in FileTooLargeException. TODO: what should we do
            //here now?
  			if (_amountRead >= _amountToRead) 
				break;
			
            int left=_amountToRead - _amountRead;
			c = _byteReader.read(buf, 0, Math.min(BUF_LENGTH, left));

			if (c == -1) 
				break;
			
			_fos.write(buf, 0, c);
			
			_amountRead+=c;

		}  // end of while loop

		_byteReader.close();
		_fos.close();


		if ( _amountRead != _amountToRead ) {
            throw new FileIncompleteException();
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
            if (_fos != null)
                _fos.close();
        } catch (IOException e) { }
        try {
            if (_socket != null)
                _socket.close();
        } catch (IOException e) { }
	}

    /**
     * Instructs this to stop after reading the given byte in the file.
     * @param stop the byte offset to stop at
     */
    public void stopAt(int stop) {
        _amountToRead=(stop-_initialReadingPoint);
    }


    ///////////////////////////// Accessors ///////////////////////////////////

    public int getInitialReadingPoint() {return _initialReadingPoint;}
	public int getAmountRead() {return _amountRead;}
	public int getAmountToRead() {return _amountToRead;}

    /** 
     * Forces this to not write past the given byte of the file, if it has not
     * already done so.  Typically this is called to reduce the download window;
     * doing otherwise will typically result in incomplete downloads.
     * 
     * @param stop a byte index into the file, using 0 to N-1 notation.  
     */
    public InetAddress getInetAddress() {return _socket.getInetAddress();}
	public boolean chatEnabled() {
		return _chatEnabled;
	}

	public long getIndex() {return _index;}
  	public String getFileName() {return _filename;}
  	public byte[] getGUID() {return _guid;}
	public int getPort() {return _port;}
    /** Returns the RemoteFileDesc passed to this' constructor. */
    public RemoteFileDesc getRemoteFileDesc() {return _rfd;}
    /** Returns true iff this is a push download. */
    public boolean isPush() {return _isPush;}
	

	////////////////////////////// Unit Test ////////////////////////////////////

	
//  	private HTTPDownloader(String str) {
//  		ByteArrayInputStream stream = new ByteArrayInputStream(str.getBytes());
//  		_byteReader = new ByteReader(stream);
//  	}

//  	public static void main(String[] argv) {
//          //Unit tests for parseContentRangeStart
//          try {
//              Assert.that(parseContentRangeStart("Content-range: bytes 1-9/10")==1);
//              Assert.that(parseContentRangeStart("Content-range:bytes=1-9/10")==1);
//              Assert.that(parseContentRangeStart("Content-range:bytes */10")==0);
//              Assert.that(parseContentRangeStart("Content-range:bytes */*")==0);
//              Assert.that(parseContentRangeStart("Content-range:bytes 1-9/*")==1);
//              Assert.that(parseContentRangeStart("Content-range:bytes 1-9/*")==1);
//              Assert.that(parseContentRangeStart("Content-range:bytes 1-10/10")==0);
//          } catch (IOException e) {
//              e.printStackTrace();
//              Assert.that(false);
//          }
//          try {
//              parseContentRangeStart("Content-range:bytes 1 10 10");
//          } catch (IOException e) {
//              Assert.that(true);
//          }
        

//          //readHeaders tests
//  		String str;
//  		HTTPDownloader down;
//  		boolean ok = true;

//  		System.out.println("Starting Test...");

//  		str = "HTTP 200 OK\r\n";
//  		down = new HTTPDownloader(str);
//  		try {
//  			down.readHeaders();
//  			down.stop();
//  		} catch (IOException e) {
//  			// should not throw an error
//  			Assert.that(false);
//  		}
		
//  		str = "HTTP 404 File Not Found \r\n";
//  		down = new HTTPDownloader(str);

//  		try {
//  			down.readHeaders();
//  			down.stop();
//  			Assert.that(false);
//  		} catch (FileNotFoundException e) {

//  		} catch (IOException e) {
//  			Assert.that(false);
//  		}

//  		str = "HTTP 410 Not Sharing \r\n";
//  		down = new HTTPDownloader(str);
//  		try {
//  			down.readHeaders();
//  			down.stop();
//  			Assert.that(false);
//  		} catch (NotSharingException e) {
//  		}catch (IOException e) {
//  			Assert.that(false);
//  		}

//  		str = "HTTP 412 \r\n";
//  		down = new HTTPDownloader(str);
//  		try {
//  			down.readHeaders();
//  			down.stop();
//  			Assert.that(false);
//  		} catch (IOException e) { 
//  		}

//  		str = "HTTP 503 \r\n";
//  		down = new HTTPDownloader(str);
//  		try {
//  			down.readHeaders();
//  			down.stop();
//  			Assert.that(false);
//  		} catch (TryAgainLaterException e) {
//  		} catch (IOException e) {
//  			Assert.that(false);
//  		}

//  		str = "HTTP 210 \r\n";
//  		down = new HTTPDownloader(str);
//  		try {
//  			down.readHeaders();
//  			down.stop();
//  		} catch (IOException e) {
//  			Assert.that(false);
//  		}

//  		str = "HTTP 204 Partial Content\r\n";
//  		down = new HTTPDownloader(str);
//  		try {
//  			down.readHeaders();
//  			down.stop();
//  		} catch (IOException e) {
//  			Assert.that(false);
//  		}


//  		str = "HTTP 200 OK\r\nUser-Agent: LimeWire\r\n\r\nx";
//  		down = new HTTPDownloader(str);
//  		try {
//  			down.readHeaders();
//  			Assert.that((char)down._byteReader.read()=='x');
//  			down.stop();
//  		} catch (IOException e) {
//  			Assert.that(false);
//  		}
		
//  		str = "200 OK\r\n";
//  		down = new HTTPDownloader(str);
//  		try {
//  			down.readHeaders();
//  			down.stop();
//  			Assert.that(false);
//  		} catch (NoHTTPOKException e) {
//  		}catch (IOException e) {
//  			Assert.that(false);
//  		}

//  		System.out.println("Test SUCCEEDED!");

//  	}


}










