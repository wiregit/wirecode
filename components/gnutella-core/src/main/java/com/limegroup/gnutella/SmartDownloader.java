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

import com.limegroup.gnutella.util.*;
import java.io.*;
import java.net.*;
import java.util.*;
import com.sun.java.util.collections.*;

public class SmartDownloader extends HTTPDownloader {

	protected RemoteFileDesc[] _remoteFiles;  // the array of connectionss
	protected boolean _keepTrying;  // will be used to no longer continue 
	                              // trying other hosts 
	private BinaryHeap _qDownloads;


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
		Arrays.sort(_remoteFiles);

		int size = _remoteFiles.length; 
		_qDownloads = new BinaryHeap(size);

		for (int i = 0; i < size; i++) 
			_qDownloads.insert(_remoteFiles[i]);

	}

	public void run() {

		// not sure about the gui display portion of this

		_callback.addDownload(this);
		
		tryHost();

		_callback.removeDownload(this);

	}

	private void print() {
		Iterator i = _qDownloads.iterator();
		RemoteFileDesc r;
		while (i.hasNext()) {
			r = (RemoteFileDesc)i.next();
			r.print();
		}
	}


	private void tryHost() {

		RemoteFileDesc file;
		int size;

		while (_keepTrying) {
			
			size = _qDownloads.size();
			
			if (size == 0)
				return;

			print();

			for (int i= 0; i < size; i++) {
				
				// get a host (file) from the queue
				file = (RemoteFileDesc)_qDownloads.extractMax();
				
				// 1. try to connect
				if ( tryConnect(file) ) {
  					// 2. try to download
					  if ( tryDownload(file) ) {
  						_keepTrying = false;;
  						break;
  					}
  				}
				
			}
			
			if (_keepTrying) {
				// wait..
				try {
					Thread.sleep(calculateWait());
  				} catch (InterruptedException e) {
  				}
			}

		}

	}

	private int calculateWait() {
		return 10000;
	}


	// returns a boolean for whether or not the file
	// was downloaded succesfully
	private boolean tryDownload(RemoteFileDesc file) {
		try {
			// attempt the download
			super.doDownload();
		} catch (IOException ioe) {
			String msg = ioe.getMessage();
			String try_again = "Try Again Later";
			if (try_again.equals(msg)) {
				// if it fails becasue the server is busy				
				// then, insert the file back into the queue
				file.incrementNumAttempts();
				_qDownloads.insert(file);
			}
			return false;
		} catch (Exception e) {
			// if it failed for some other resaon
			// re-initialize the download values.
			_amountRead = 0;
			_sizeOfFile = -1;
			return false;
		}
		if (_state != COMPLETE) {
			_state = ERROR;
			_stateString = "Error";
			return false;
		}
		return true;
		
	}


	private boolean tryConnect(RemoteFileDesc file) {
		/* the information needed for establishing a connection */
		int index  = file.getIndex();
		String filename = file.getFileName();
		String host = file.getHost();
		int port = file.getPort();
		/* try to connect... */
		String furl = "/get/" + String.valueOf(index) + "/" + filename;
		String protocal = "http";
		URLConnection conn = null;
		try {
			URL url = new URL(protocal, host, port, furl);
			conn = url.openConnection();
			conn.connect();
		}
		catch (IOException e) {
			if (conn == null) 
				return false;
			// a safety check
			String str = conn.getHeaderField(0);
			// if there is, check to see if the server is busy
			if (str != null && (str.indexOf(" 503 " ) > 0) ) {
				// if busy inset back into the loop
				file.incrementNumAttempts();
				_qDownloads.insert(file);
				
			}
			return false;
		}
		try {
			// try to open an input stream to read the file
			_istream = conn.getInputStream();
			_br = new ByteReader(_istream);
		} catch (IOException e) {
			return false;
		}
		return true;
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









