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
    private boolean _isPush;
	private int _index;
	private String _filename; 
	private byte[] _guid;

	private int _amountRead;
	private int _fileSize;
	private int _initialReadingPoint;

	private ByteReader _byteReader;
	private FileOutputStream _fos;
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
     * @param incompleteFile the temp file to use while downloading.  No other 
     *  thread or process should be using this file, at least when doDownload
     *  is called 
     */
	public HTTPDownloader(RemoteFileDesc rfd,
                          File incompleteFile) {
        //Dirty secret: this is implemented with the push constructor!
        this(null, rfd, incompleteFile);
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
     * @param incompleteFile the temp file to use while downloading.  No other 
     *  thread or process should be using this file, at least when doDownload
     *  is called 
     */
	public HTTPDownloader(Socket socket,
                          RemoteFileDesc rfd,
                          File incompleteFile) {
        _isPush=true;
        _socket=socket;
        _incompleteFile=incompleteFile;
		_filename = rfd.getFileName();
		_index = rfd.getIndex();
		_guid = rfd.getClientGUID();
		_fileSize = rfd.getSize();
		_port = rfd.getPort();
		_host = rfd.getHost();
		_chatEnabled = rfd.chatEnabled();

        //If the incomplete file exists, set up a resume just past the end.
        //Otherwise, begin from the start.
		_amountRead = 0;
		_initialReadingPoint = 0;
        if (_incompleteFile.exists()) {
            _initialReadingPoint = (int)incompleteFile.length();
            _amountRead = _initialReadingPoint;
        }
    }

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
     * @exception TryAgainLaterException the host is busy
     * @exception FileNotFoundException the host doesn't recognize the file
     * @exception NotSharingException the host isn't sharing files
     * @exception IOException couldn't contact server in time, or miscellaneous 
     *  error 
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
            throw new IOException();
        }
        _byteReader = new ByteReader(istream);

        //Write GET request and headers
        OutputStream os = _socket.getOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(os);
        BufferedWriter out=new BufferedWriter(osw);
        String startRange = java.lang.String.valueOf(_initialReadingPoint);
        out.write("GET /get/"+_index+"/"+_filename+" HTTP/1.0\r\n");
        out.write("User-Agent: "+CommonUtils.getVendor()+"\r\n");
        out.write("Range: bytes=" + startRange + "-\r\n");
		if (SettingsManager.instance().getChatEnabled() ) {
			int port = SettingsManager.instance().getPort();
			out.write("Chat: " + _host + ":" + port + "\r\n");;
		}
        out.write("\r\n");
        out.flush();

        //Read response.
        readHeader();
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

	/* Public Accessor Methods */

	public int getAmountRead() {return _amountRead;}
	public int getFileSize() {return _fileSize;}
	public int getInitialRead() {return _initialReadingPoint;}
    public InetAddress getInetAddress() {return _socket.getInetAddress();}

	public boolean chatEnabled() {
		return _chatEnabled;
	}

	/* Construction time variables */
	public int getIndex() {return _index;}
  	public String getFileName() {return _filename;}
  	public byte[] getGUID() {return _guid;}
	public int getPort() {return _port;}
    /** Returns true iff this is a push download. */
    public boolean isPush() {return _isPush;}
	


    /*
     * Reads the headers from this, setting _initialReadingPoint and _fileSize.
     * Throws any of the exceptions listed in connect().  
     */
	private void readHeader() throws IOException {

		String str = " ";

		if (_byteReader == null) 
			throw new ReaderIsNullException();

		// Read the first line and then check for any possible errors
		str = _byteReader.readLine();  
		if (str==null || str.equals(""))
			return;
		
		// str should be some sort of HTTP connect string.
		// The string should look like:	
		// str = "HTTP 200 OK \r\n";
		// We will accept any 2xx's, but reject other codes.
		
		// create a new String tokenizer with the space as the 
		// delimeter.
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
		int code;
		try {
			code = java.lang.Integer.parseInt(num);
		} catch (NumberFormatException e) {
			throw new ProblemReadingHeaderException();
		}
		
		// accept anything that is 2xx
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

		// if we've gotten this far, then we can assume that we should
		// be alright to prodeed.

		// str = _byteReader.readLine();
	
		while (true) {
			if (str.toUpperCase().indexOf("CONTENT-LENGTH:") != -1)  {

                String sub;
                try {
                    sub=str.substring(15);
                } catch (IndexOutOfBoundsException e) {
					throw new ProblemReadingHeaderException();
                }
                sub = sub.trim();
				int tempSize;
				
                try {
                    tempSize = java.lang.Integer.parseInt(sub);
                }
                catch (NumberFormatException e) {
					throw new ProblemReadingHeaderException();
                }

				_fileSize = tempSize;
				
            }  // end of content length if
			
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
					throw new ProblemReadingHeaderException();
                }
				try {
					numAfterSlash = java.lang.Integer.parseInt(afterSlash);
					numBeforeDash = java.lang.Integer.parseInt(beforeDash);
                    numBeforeSlash = java.lang.Integer.parseInt(beforeSlash);
                }
                catch (NumberFormatException e) {
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

				_initialReadingPoint = numBeforeDash;
				// _amountRead = numBeforeSlash;
				_fileSize = numAfterSlash;

            } // end of content range if

			str = _byteReader.readLine();
			
            //EOF?
            if (str==null || str.equals(""))
                break;
        }
    }

    /*
     * Downloads the content from the server, writes it to a temporary
     * file, and copies the temporary file to the library.  Blocking.
     * This MUST be initialized via connect() beforehand, and doDownload
     * MUST NOT have already been called.
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
        //1. For security, check that download location is OK.
        //   This is to prevent against any files with '.' or '/' or '\'.
		SettingsManager settings = SettingsManager.instance();		
		File download_dir = settings.getSaveDirectory();

		File complete_file = new File(download_dir, _filename);
		
		//File shared = new File(download_dir);
		String shared_path = download_dir.getCanonicalPath();
		
		File parent_of_shared = new File(complete_file.getParent());
		String path_to_parent = parent_of_shared.getCanonicalPath();
		
		if (!path_to_parent.equals(shared_path)) {
			// need to add an error message here
			throw new InvalidPathException();  
		}

	  
        //2. Do actual download, appending to incomplete file if necessary.
		String path_to_incomplete = _incompleteFile.getCanonicalPath();
		boolean append = false;

		if (_initialReadingPoint > 0)
			append = true;

		_fos = new FileOutputStream(path_to_incomplete, append);

		int c = -1;
		
		byte[] buf = new byte[1024];

		while (true) {
			
  			if (_amountRead == _fileSize) 
				break;
						
  			// just a safety check.  hopefully this wouldn't
  			// happen, but if it does, need a way to exit 
  			// gracefully...
  			if (_amountRead > _fileSize) 
				throw new FileTooLargeException();
			
			c = _byteReader.read(buf);

			if (c == -1) 
				break;
			
			_fos.write(buf, 0, c);
			
			_amountRead+=c;

		}  // end of while loop

		_byteReader.close();
		_fos.close();


		//3. If not interrupted, move from temporary directory to final directory.
		if ( _amountRead == _fileSize ) {
			//Delete target.  If target doesn't exist, this will fail silently.
			//Otherwise, it's always safe to do this since we prompted the user
			//in SearchView/DownloadManager.
			complete_file.delete();
            //Try moving file.  If we couldn't move the file, i.e., because
            //someone is previewing it or it's on a different volume, try copy
            //instead.  If that failed, notify user.
            if (!_incompleteFile.renameTo(complete_file))
                if (! CommonUtils.copy(_incompleteFile, complete_file))
                    throw new FileCantBeMovedException();
            //Add file to library.
			FileManager.instance().addFileIfShared(complete_file);
		} else 
			throw new FileIncompleteException();
	}


	/****************** UNIT TEST *********************/
	
//  	private HTTPDownloader(String str) {
//  		ByteArrayInputStream stream = new ByteArrayInputStream(str.getBytes());
//  		_byteReader = new ByteReader(stream);
//  	}

//  	public static void main(String[] argv) {
//  		String str;
//  		HTTPDownloader down;
//  		boolean ok = true;

//  		System.out.println("Starting Test...");

//  		str = "HTTP 200 OK\r\n";
//  		down = new HTTPDownloader(str);
//  		try {
//  			down.readHeader();
//  			down.stop();
//  		} catch (IOException e) {
//  			// should not throw an error
//  			Assert.that(false);
//  		}
		
//  		str = "HTTP 404 File Not Found \r\n";
//  		down = new HTTPDownloader(str);

//  		try {
//  			down.readHeader();
//  			down.stop();
//  			Assert.that(false);
//  		} catch (FileNotFoundException e) {

//  		} catch (IOException e) {
//  			Assert.that(false);
//  		}

//  		str = "HTTP 410 Not Sharing \r\n";
//  		down = new HTTPDownloader(str);
//  		try {
//  			down.readHeader();
//  			down.stop();
//  			Assert.that(false);
//  		} catch (NotSharingException e) {
//  		}catch (IOException e) {
//  			Assert.that(false);
//  		}

//  		str = "HTTP 412 \r\n";
//  		down = new HTTPDownloader(str);
//  		try {
//  			down.readHeader();
//  			down.stop();
//  			Assert.that(false);
//  		} catch (IOException e) { 
//  		}

//  		str = "HTTP 503 \r\n";
//  		down = new HTTPDownloader(str);
//  		try {
//  			down.readHeader();
//  			down.stop();
//  			Assert.that(false);
//  		} catch (TryAgainLaterException e) {
//  		} catch (IOException e) {
//  			Assert.that(false);
//  		}

//  		str = "HTTP 210 \r\n";
//  		down = new HTTPDownloader(str);
//  		try {
//  			down.readHeader();
//  			down.stop();
//  		} catch (IOException e) {
//  			Assert.that(false);
//  		}

//  		str = "HTTP 204 Partial Content\r\n";
//  		down = new HTTPDownloader(str);
//  		try {
//  			down.readHeader();
//  			down.stop();
//  		} catch (IOException e) {
//  			Assert.that(false);
//  		}


//  		str = "HTTP 200 OK\r\nUser-Agent: LimeWire\r\n\r\nx";
//  		down = new HTTPDownloader(str);
//  		try {
//  			down.readHeader();
//  			Assert.that((char)down._byteReader.read()=='x');
//  			down.stop();
//  		} catch (IOException e) {
//  			Assert.that(false);
//  		}
		
//  		str = "200 OK\r\n";
//  		down = new HTTPDownloader(str);
//  		try {
//  			down.readHeader();
//  			down.stop();
//  			Assert.that(false);
//  		} catch (NoHTTPOKException e) {
//  		}catch (IOException e) {
//  			Assert.that(false);
//  		}

//  		System.out.println("Test SUCCEEDED!");

//  	}


}










