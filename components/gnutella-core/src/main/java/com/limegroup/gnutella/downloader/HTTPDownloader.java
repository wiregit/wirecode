/**
 * auth: rsoule
 * file: HTTPDownloader.java
 * desc: Read data from the net and write to disk.
 *
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|

package com.limegroup.gnutella;

import com.limegroup.gnutella.downloader.*;
import com.limegroup.gnutella.util.SocketOpener;
import java.io.*;
import java.net.*;

/**
 * Downloads a file over an HTTP connection.  This class is as simple as possible.
 * It does not deal with retries, prioritizing hosts, etc.  Nor does it check
 * whether a file already exists; it just writes over anything on disk.
 */
public class HTTPDownloader {

	private int _index;
	private String _filename; 
	private byte[] _guid;

	private int _amountRead;
	private int _fileSize;
	private int _initialReadingPoint;

	private ByteReader _byteReader;
	private FileOutputStream _fos;
	private Socket _socket;
    private File _incompleteFile;

	/**
     * Creates a server-side push download.
     * 
	 * @param file the name of the file	
     * @param socket the socket to download from.  The "GIV..." line must
     *  have been read from socket.  HTTP headers may not have been read or 
     *  buffered.
	 * @param index the index of the file that the uploader sent in the GIV line.
	 * @param guid the unique identifier of the uploader sent in the GIV line.
     * @param incompleteFile the temp file to use while downloading.  No other 
     *  thread or process should be using this file.
     *
	 * @exception CantConnectException couldn't connect to the host.
	 */
	public HTTPDownloader(String file, Socket socket, 
                          int index, byte[] guid,
                          File incompleteFile) 
		throws IOException {
		_filename = file;
		_index = index;
		_guid = guid;

		_amountRead = 0;
		_fileSize = -1;
		_initialReadingPoint = 0;
        _incompleteFile=incompleteFile;

		connect(socket, file, index);
		
	}
	
    /**
     * Creates a client-side normal download.
     *
     * @param file the name of the file to get
     * @param host the name of the host to connect to
     * @param port the port to use on "host"
     * @param index the index of the file on the server
     * @param guid the uploader's unique server GUID.  I don't think this is
     *  currently used.
     * @param size the expected size of the file when the download is complete
     * @param resume true if we should initially resume for the temporary file
     * @param timeout the amount of time, in milliseconds, to wait
     *  when establishing a connection.   Must be non-negative.  A
     *  timeout of 0 means no timeout.
     * @param incompleteFile the temp file to use while downloading.  No other 
     *  thread or process should be using this file.
     *
     * @exception CantConnectException couldn't connect to the host.
     */
	public HTTPDownloader(String file, String host, 
                          int port, int index, byte[] guid, 
                          int size, boolean resume, int timeout,
                          File incompleteFile) 
		throws IOException {

		_filename = file;
		_index = index;
		_guid = guid;
		_amountRead = 0;
		_fileSize = size;
		_initialReadingPoint = 0;
		
        _incompleteFile=incompleteFile;
		if (resume) {			
			// now, check to see if a file of that name alread exists in the
			// temporary directory.  incompleteFile represents the file as it
			// would be named in the temporary incomplete directory
			if (incompleteFile.exists()) {
				// dont alert an error if the file doesn't 
				// exist, just assume a starting range of 0;
				_initialReadingPoint = (int)incompleteFile.length();
                _amountRead = _initialReadingPoint;
			}
		}

		connect(host, port, file, index, timeout);		
	}

	/**
	 * Private connection methods
	 */

	private void connect(String host, int port,
                         String file, int index,
                         int timeout ) 
		throws CantConnectException {
        try {
            Socket socket = (new SocketOpener(host, port)).connect(timeout);
            connect(socket, file, index);
        } catch (IOException e) {
            throw new CantConnectException();
        }
	}

	private void connect(Socket s, String file, int index) throws IOException {
		_socket = s;

        //The try-catch below is a work-around for JDK bug 4091706.
        InputStream istream=null;
        try {
            istream=_socket.getInputStream(); 
        } catch (Exception e) {
            throw new IOException();
        }
        _byteReader = new ByteReader(istream);
        OutputStream os = _socket.getOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(os);
        BufferedWriter out=new BufferedWriter(osw);
        String startRange = java.lang.String.valueOf(_initialReadingPoint);
        out.write("GET /get/"+index+"/"+file+" HTTP/1.0\r\n");
        out.write("User-Agent: LimeWire\r\n");
        out.write("Range: bytes=" + startRange + "-\r\n");
        out.write("\r\n");
        out.flush();
	}
	
	/** 
     * Start the download, returning when done.  Throws IOException if
     * there is a problem.
     *     @modifies this
     *     @exception TryAgainLaterException the host is busy
     *     @exception NotSharingException the host isn't sharing files
     *     @exception FileIncompleteException transfer interrupted, either
     *      locally or remotely
     */
	public void start() throws IOException {
		readHeader();
		doDownload();
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

	/* Construction time variables */
	public int getIndex() {return _index;}
  	public String getFileName() {return _filename;}
  	public byte[] getGUID() {return _guid;}


	/*************************************************************/

	/* PRIVATE INTERNAL METHODS */
	private void readHeader() throws IOException {

		String str = " ";

		if (_byteReader == null) 
			throw new ReaderIsNullException();

		// Read the first line and then check for any possible errors
		str = _byteReader.readLine();  
		if (str==null || str.equals(""))
			return;
		
		// TODO:  Now that we are no longer truncating the
		// str, we need to correct the possible errors
		// that we are looking for

  		if ( str.indexOf("503") > 0 ) 
    			throw new TryAgainLaterException();
		else if ( str.indexOf("404") > 0 ) 
			throw new com.limegroup.gnutella.downloader.FileNotFoundException();
        else if ( str.indexOf("410") > 0 )
            throw new com.limegroup.gnutella.downloader.NotSharingException();
		else if ( (str.indexOf("HTTP") < 0 ) && (str.indexOf("OK") < 0 ) )
			throw new NoHTTPOKException();

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

	private void doDownload() throws IOException {
        //1. For security, check that download location is OK.
        //   This is to prevent against any files with '.' or '/' or '\'.
		SettingsManager settings = SettingsManager.instance();		
		String download_dir = settings.getSaveDirectory();

		File complete_file = new File(download_dir, _filename);
		String path_to_complete = complete_file.getAbsolutePath();
		
		File shared = new File(download_dir);
		String shared_path = shared.getCanonicalPath();
		
		File parent_of_shared = new File(complete_file.getParent());
		String path_to_parent = parent_of_shared.getCanonicalPath();
		
		if (!path_to_parent.equals(shared_path)) {
			// need to add an error message here
			throw new InvalidPathException();  
		}

	  
        //2. Do actual download, appending to incomplete file if necessary.
		String path_to_incomplete = _incompleteFile.getAbsolutePath();
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
			//If target doesn't exist, this will fail silently.  Otherwise, it's
			//always safe to do this since we prompted the user in
			//SearchView/DownloadManager.
			complete_file.delete();
			boolean ok = _incompleteFile.renameTo(complete_file);
			if (! ok) 
				throw new FileCantBeMovedException();
			//renameTo is not guaranteed to work, esp. when the
			//file is being moved across file systems.  

			FileManager.instance().addFileIfShared(path_to_complete);

		} else 
			throw new FileIncompleteException();
	}
}










