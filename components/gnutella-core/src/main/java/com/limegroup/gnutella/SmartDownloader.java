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

public class SmartDownloader { //extends HTTPDownloader {

	private RemoteFileDesc[] _remoteFiles;  // the array of connectionss

	public SmartDownloader(MessageRouter router,  RemoteFileDesc[] files,
						   Acceptor acceptor, ActivityCallback callback) {

		_remoteFiles = files;

	}


	/* probably want to redefine doDownload() */
	public void doDownload() {

		int numFiles = _remoteFiles.length;  // the number of possible files;
		RemoteFileDesc file;
		String host;
		int port;
		int index = 0;

		while (index < numFiles) {

			/* get the first file and client */
			file = _remoteFiles[index];

			  if ( tryHost(file.getIndex(), file.getFileName(),
						   file.getHost(), file.getPort() ) ){
				  break;
			  } 

			index++;

		}
		
	}

	/* try to download from one particular host */
	public boolean tryHost(int index, String filename, 
						   String host, int port) {
						   
		String furl = "/get/" + String.valueOf(index) + "/" + filename;
		String protocal = "HTTP";
		try {
			URL url = new URL(protocal, host, port, furl);
		} catch (MalformedURLException e) {
			return false;
		}
		return false;
	}
	
	/* this will establish a connection
	   for a particular endpoint */
	private ByteReader getConnection() {
		return null;
	} 

}
