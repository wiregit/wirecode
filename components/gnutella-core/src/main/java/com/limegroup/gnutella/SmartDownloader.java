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
		// _sizeOfFile = -1; // 
		_sizeOfFile = files[0].getSize();
		_downloadDir = "";
		_stateString = "";
	}

	public void run() {

		// not sure about the gui display portion of this

		_callback.addDownload(this);
		
		doDownload();

		_callback.removeDownload(this);

	}



	/* probably want to redefine doDownload() */
	public void doDownload() {

		int numFiles = _remoteFiles.length;  // the number of possible files;
		RemoteFileDesc file;
		String host;
		int port;
		int index = 0;

		while (index < numFiles) {

			/* get each possible file and host */
			file = _remoteFiles[index];

			/* see if we can connect to it */
			  if ( tryHost(file.getIndex(), file.getFileName(),
						   file.getHost(), file.getPort() ) ){
				  _state = COMPLETE;
				  return;
			  } 

			index++;

		}
		
		_state = ERROR;
		
	}

	/* try to download from one particular host */
	public boolean tryHost(int index, String filename, 
						   String host, int port) {
			
		// try to connect...
		String furl = "/get/" + String.valueOf(index) + "/" + filename;
		String protocal = "http";
		URLConnection conn;
		try {
			URL url = new URL(protocal, host, port, furl);
			conn = url.openConnection();
			conn.connect();
		} catch (MalformedURLException e) {
			// if you can't connect, return false
			return false;
		} catch (IOException e) {
			return false;
		}

		try {
			// try to open an input stream to read the file
			_istream = conn.getInputStream();
			_br = new ByteReader(_istream);
		}
		catch (Exception e) {
			// if you can't, return false
			return false;
		}

		// otherwise, try to read the header...
		readHeader();

		// if there is a problem, return false
        if ( _state == ERROR ) {
			
			_state = NOT_CONNECTED;
            return false;
		}

		// check to see if the file already exists, etc.
        SettingsManager set = SettingsManager.instance();

        String downloadDir = set.getSaveDirectory();

        String incompleteDir = set.getIncompleteDirectory();

        File myFile = new File(incompleteDir, filename);
        String pathname = myFile.getAbsolutePath();

        File myTest = new File(downloadDir, filename);
        String path = myTest.getAbsolutePath();

        // This is necessary, and a little tricky. I
		//  check to see if the canonical path of the
		//  parent of the requested file is equivalent
		//  to the canonical path of the shared directory. */

        File f;
        String p;
        try {
            File shared = new File(downloadDir);
            String shared_path = shared.getCanonicalPath();

            f = new File(myTest.getParent());
            p = f.getCanonicalPath();

            if (!p.equals(shared_path)) {
                _state = NOT_CONNECTED;
                return false;
            }
        } catch (Exception e) {
            _state = NOT_CONNECTED;
            return false;
        }

		if (   myFile.exists()  
			|| myTest.exists()  ) {
            // ask the user if the file should be overwritten
            if ( ! _callback.overwriteFile(filename) ) {
                _stateString = "File Already Exists";
                _state = NOT_CONNECTED;
                return false;
            }
        }
		
		FileOutputStream fos;

		try {
            fos = new FileOutputStream(pathname);
        }
        catch (FileNotFoundException e) {
            _state = NOT_CONNECTED;
            return false;
        }
        catch (Exception e) {
            _state = NOT_CONNECTED;
            return false;
        }
		
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
			if (_amountRead > _sizeOfFile) {
                _state = NOT_CONNECTED;
                return false;
            }

            try {
                c = _br.read(buf);
            }
            catch (Exception e) {
                _state = NOT_CONNECTED;
                return false;
            }

            if (c == -1) {
                break;
            }

            try {
                fos.write(buf, 0, c);
            }
            catch (Exception e) {
				e.printStackTrace();
                _state = ERROR;
                break;
            }

            _amountRead+=c;

        }

		try {
            _br.close();
            fos.close();
        }
        catch (IOException e) {
            _state = NOT_CONNECTED;
            return false;
        }

        //Move from temporary directory to final directory.
        if ( _amountRead == _sizeOfFile ) {
            String pname = downloadDir + _filename;
            File target=new File(pname);
            //If target doesn't exist, this will fail silently.  Otherwise,
            //it's always safe to do this since we prompted the user above.
            target.delete();
            boolean ok=myFile.renameTo(target);
            if (! ok) {
                //renameTo is not guaranteed to work, esp. when the
                //file is being moved across file systems. 
                _state = NOT_CONNECTED;
                _stateString = "Couldn't Move to Library";
                return false;
            }

            _state = COMPLETE;
            FileManager.getFileManager().addFileIfShared(pname);

        }

        else
        {
            _state = NOT_CONNECTED;
            _stateString = "Interrupted";
			return false;
        }

		return true;
		// return false;
	}
	

}









