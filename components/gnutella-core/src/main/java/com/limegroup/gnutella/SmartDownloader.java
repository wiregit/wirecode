/**
 *  This is a subclass of HTTPDownloader.  Its
 *  supposed to be able to handle downloading an
 *  array of files, until a successful download
 *  takes place. 
 *
 * @author rsoule
 * @file SmartDownloader.java
 *
 */
 
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|

package com.limegroup.gnutella;

import java.io.*;
import java.net.*;
import java.util.*;
import com.sun.java.util.collections.*;

public class SmartDownloader extends HTTPDownloader {

	private RemoteFileDesc[] _remoteFiles;  // the array of connectionss
	private boolean _keepTrying;  // will be used to no longer continue 
	                              // trying other hosts 



	public SmartDownloader(MessageRouter router,  RemoteFileDesc[] files,
						   Acceptor acceptor, ActivityCallback callback) {

		super();
		_state = NOT_CONNECTED;  // not connected from the super class
		_remoteFiles = files;
		_router = router;
		_callback = callback;
		_acceptor = acceptor;
		_filename = files[0].getFileName();
		_amountRead = 0;
		_sizeOfFile = files[0].getSize();
		_downloadDir = "";
		_stateString = "";
		_smartDownload = true;
		_keepTrying = true;

		Arrays.sort(_remoteFiles, 
					new RemoteFileDesc.RemoteFileDescComparator());

	}

	public void run() {

		// not sure about the gui display portion of this

		_callback.addDownload(this);
		
		tryHost();

		_callback.removeDownload(this);

	}



	/* probably want to redefine doDownload() */
	public void tryHost() {

		int numFiles = _remoteFiles.length;  // the number of possible files;
		RemoteFileDesc file;

		int index;
		String filename;
		int port;
		String host;
		
		int counter = 0;

		while( (counter < numFiles) && (_keepTrying) ) {

			/* get each possible file and host */
			file = _remoteFiles[counter];

			index  = file.getIndex();
			filename = file.getFileName();
			host = file.getHost();
			port = file.getPort();
			
			// try to connect...
			String furl = "/get/" + String.valueOf(index) + "/" + filename;
			String protocal = "http";
			URLConnection conn;
			try {
				
				URL url = new URL(protocal, host, port, furl);
				conn = url.openConnection();
				conn.connect();
				// try to open an input stream to read the file
				_istream = conn.getInputStream();
				_br = new ByteReader(_istream);

				// if download fails, it should throw an IOException
				super.doDownload();  
				_state = COMPLETE;
				break;
			}
			catch (Exception e) {
				// there was an error, then the download failed.
				// increase the index, and try the next file
				counter++;
				_amountRead = 0;
				_sizeOfFile = -1;
			}

		} // end of while loop

		if (_state != COMPLETE) {
			_state = ERROR;
			_stateString = "Error";
		}
			
		
		
	}

	/**
	 * This method overwrites the super class's shutdown
	 * method.  The sole addition is of the _keepTrying
	 * variable, that will break  us out of the while loop
	 * in the tryHost method
	 */
	public void shutdown() {
		_keepTrying = false;
		super.shutdown();
	}

}









