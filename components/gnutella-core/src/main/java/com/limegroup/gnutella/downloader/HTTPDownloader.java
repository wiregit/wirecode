/**
 * auth: rsoule
 * file: HTTPDownloader.java
 * desc: Read data from the net and write to disk.
 *
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|

package com.limegroup.gnutella;

import com.limegroup.gnutella.downloader.*;
import java.io.*;
import java.net.*;

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

	/**
	 * @param file the name of the file	
	 * @param index the index of the file that the client sent
	 * @param guid the unique identifier of the client
	 *
	 */
	public HTTPDownloader(String file, Socket socket, 
							 int index, byte[] guid) 
		throws IOException {
		_filename = file;
		_index = index;
		_guid = guid;

		_amountRead = 0;
		_fileSize = -1;
		_initialReadingPoint = 0;

		connect(socket, file, index);
		
	}
	
	public HTTPDownloader(String file, String host, 
							 int port, int index, byte[] guid, 
							 int size, boolean resume) // size???
		throws IOException {

		_filename = file;
		_index = index;
		_guid = guid;
		_amountRead = 0;
		_fileSize = size;
		_initialReadingPoint = 0;
		
		if (resume) {
			// First, get the incomplete directory where the 
			// file will be temperarily downloaded.
			SettingsManager sm = SettingsManager.instance();
			String incompleteDir;
			incompleteDir = sm.getIncompleteDirectory();
			// check to see if we actually get a directory
			
			if (incompleteDir == "") 
				throw new NullIncompleteDirectoryException();
			
			// now, check to see if a file of that name alread
			// exists in the temporary directory.
			String incompletePath;
			incompletePath = incompleteDir + _filename;
			File incompleteFile = new File(incompletePath);
			// incompleteFile represents the file as it would
			// be named in the temporary incomplete directory.
			
			if (incompleteFile.exists()) {
				// dont alert an error if the file doesn't 
				// exist, just assume a starting range of 0;
				_initialReadingPoint = (int)incompleteFile.length();
			}
		}

		connect(host, port, file, index);
		
	}

	/**
	 * Private connection methods
	 */

	private void connect(String host, int port, String file, int index ) 
		throws IOException {
		Socket socket = new Socket(host, port);
		connect(socket, file, index);
	}

	private void connect(Socket s, String file, int index) throws IOException {
		_socket = s;
		try {
			InputStream istream = _socket.getInputStream();
			_byteReader = new ByteReader(istream);
			OutputStream os = _socket.getOutputStream();
			OutputStreamWriter osw = new OutputStreamWriter(os);
			BufferedWriter out=new BufferedWriter(osw);
			String startRange = java.lang.String.valueOf(_initialReadingPoint);
			out.write("GET /get/"+index+"/"+file+" HTTP/1.0\r\n");
			out.write("User-Agent: Gnutella\r\n");
			out.write("Range: bytes=" + startRange + "-\r\n");
			out.write("\r\n");
			out.flush();
		} catch (ConnectException e) {
			throw new CantConnectException();
		} catch (java.net.MalformedURLException e) {
			throw new BadURLException();
		} 
	}
	
	/** 
     * Start the download.  Throws IOException if the headers
     * couldn't be read, there remote host has no more download slots,
     * or the download was interrupted.  (In the future different exceptions
     * will be thrown for each of these cases.
     *     @modifies this
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
			throw new InvalidPathException();  
		}

		if ( complete_file.exists() ){
			// ask the user if the file should be overwritten
			// if ( ! _callback.overwriteFile(_filename) ) 
			// TODO: Need to ask the user - but how?
			throw new FileExistsException();
		}
		


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

		//Move from temporary directory to final directory.
		if ( _amountRead == _fileSize ) {
			//If target doesn't exist, this will fail silently.  Otherwise,
			//it's always safe to do this since we prompted the user above.
			complete_file.delete();
			boolean ok = incomplete_file.renameTo(complete_file);
			if (! ok) 
				throw new FileCantBeMovedException();
			//renameTo is not guaranteed to work, esp. when the
			//file is being moved across file systems.  

			FileManager.instance().addFileIfShared(path_to_complete);

		} else 
			throw new FileIncompleteException();
	}
}










