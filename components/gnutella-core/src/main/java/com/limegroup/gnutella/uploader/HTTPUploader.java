package com.limegroup.gnutella.uploader;

/**
 * Read data from disk and write to the net.
 *
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|

import java.io.*;
import java.net.*;
import java.util.Date;
import java.util.StringTokenizer;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.StringUtils;
import com.limegroup.gnutella.util.URLDecoder;


public class HTTPUploader implements Uploader {

	protected OutputStream _ostream;
	protected InputStream _fis;
	protected Socket _socket;
	protected int _amountRead;
	protected int _uploadBegin;
	protected int _uploadEnd;
	protected int _fileSize;
	protected int _index;
	protected String _filename;
	protected String _hostName;
	protected String _guid;
	protected int _port;
	protected int _stateNum = CONNECTING;

	private UploadState _state;
	private final UploadManager _manager;
	
	private boolean  _chatEnabled;
	private String  _chatHost;
	private int _chatPort;

	private URN _urn = null;

	/**
	 * The URN specified in the X-Gnutella-Content-URN header, if any.
	 */
	private URN _requestedURN = null;

	/**
	 * <tt>Map</tt> instance for storing unique alternate locations sent in 
	 * the upload header.
	 */
	private Map _alternateLocations = null;

	private FileDesc _fileDesc;

    private final FileManager _fileManager;

    private final BandwidthTrackerImpl bandwidthTracker=
		new BandwidthTrackerImpl();

	/****************** Constructors ***********************/
	/**
	 * There are two constructors that are necessary.  The 
	 * first is to handle the case where there is a regular
	 * upload.  in that case, the manager class has already
	 * processed a message that looks like: 
	 * GET /get/0/sample.txt HTTP/1.0
	 * and already given a socket connection.  all that we
	 * need to do is actually upload the file to the socket.
	 *
	 * In the second case, we have recieved a push request,
	 * so we are going to need to establish the connection
	 * on this end.  We do this by creating the socket, and
	 * then writing out the GIV 0:99999999/sample.txt
	 * and then wait for the GET to come back.
	 */

	// Regular upload
	/**
	 * Consructor for a "normal" non-push upload.  Note that this can
	 * still be a URN get request.
	 *
	 * @param fileName the name of the file
	 * @param socket the <tt>Socket</tt> instance to serve the upload over
	 * @param index the index of the file in the set of shared files
	 * @param um a reference to the <tt>UploadManager</tt> instance 
	 * @param fm a reference to the <tt>FileManager</tt> instance
	 */
	public HTTPUploader(String fileName, Socket socket, int index, 
						UploadManager um, FileManager fm) {
		_socket = socket;
		_hostName = _socket.getInetAddress().getHostAddress();
		_filename = fileName;
		_manager = um;
		_index = index;
		_amountRead = 0;
        _fileManager = fm;

		try {
			// This line can't be moved, or FileNotFoundUploadState
			// will have a null pointer exception.
			_ostream = _socket.getOutputStream();
			_fileDesc = _fileManager.get(_index);
			_fileSize = _fileDesc._size;
			_urn = _fileDesc.getSHA1Urn();
			setState(CONNECTING);
		} catch (IndexOutOfBoundsException e) {
			// this is an unlikely case, but if for
			// some reason the index is no longer valid.
			setState(FILE_NOT_FOUND);
		} catch (IOException e) {
			// FileManager.get() throws an IOException if
			// the file has been deleted
			setState(INTERRUPTED);
		}
	}
		

	// Push requested Upload
	public HTTPUploader(String file, String host, int port, int index,
						String guid, UploadManager m, FileManager fm) {
		_filename = file;
		_manager = m;
		_index = index;
		_uploadBegin = 0;
		_amountRead = 0;
		_hostName = host;
		_guid = guid;
		_port = port;
        _fileManager = fm;
		try {
			_fileDesc = _fileManager.get(_index);
			_fileSize = _fileDesc._size;
			setState(CONNECTING);
		} catch (IndexOutOfBoundsException e) {
			setState(PUSH_FAILED);
		}
	}

	// This method must be called in the case of a push.
	public void connect() throws IOException {

		// the socket should not be null if this is a non-push
		// connect.  in case connect is called after a non-push,
		// we just return.
		if (_socket != null)
			return;

		try {
			// try to create the socket.
			_socket = new Socket(_hostName, _port);
			// open a stream for writing to the socket
			_ostream = _socket.getOutputStream();
			// ask chris about Assert
			Assert.that(_filename != null);  
			// write out the giv

			Assert.that(_filename != "");  

			String giv; 
			giv = "GIV " + _index + ":" + _guid + "/" + _filename + "\n\n";
			_ostream.write(giv.getBytes());
			_ostream.flush();
			
			// Wait to recieve the GET
			InputStream istream = _socket.getInputStream(); 
			ByteReader in = new ByteReader(istream);
			// set a time out for how long to wait for the push
			int time = SettingsManager.instance().getTimeout();
			_socket.setSoTimeout(time);
			
			// read directly from the socket
			String str;
			str = in.readLine();
			// not sure why we set this to zero, if we set it above
			_socket.setSoTimeout(0);
			
			// check the line, to see what was read.
			if (str == null)
				throw new IOException();
			// check for the 'GET'
			if (! str.startsWith("GET"))
				throw new IOException();
			String command = str.substring(4, str.length());
			// using this utility method, a bit hackey
			String parse[] = StringUtils.split(command, '/');
			// do some safety checks
			if (parse.length != 4) 
				throw new IOException();
			if (! parse[0].equals("get"))
				throw new IOException();
			
			//Check that the filename matches what we sent
			//in the GIV request.  I guess it doesn't need
			//to match technically, but we check to be safe.
			int end = parse[2].lastIndexOf("HTTP") - 1;
			String filename = URLDecoder.decode(parse[2].substring(0, end));
			// some safety checks - make sure name and index match.
			if (! filename.equals(_filename))
				throw new IOException();
			int pindex = java.lang.Integer.parseInt(parse[1]);
			if (pindex!= _index)
				throw new IOException();
			// catch any of the possible exceptions
		} catch (SecurityException e) {
			this.setState(Uploader.PUSH_FAILED);
			throw new IOException();
		} catch (IndexOutOfBoundsException e) {
			this.setState(Uploader.PUSH_FAILED);
            throw new IOException();
        } catch (NumberFormatException e) {
			this.setState(Uploader.PUSH_FAILED);
            throw new IOException();
        } catch (IllegalArgumentException e) {
			this.setState(Uploader.PUSH_FAILED);
            throw new IOException();
        } catch (IOException e) {
			this.setState(Uploader.PUSH_FAILED);
			throw new IOException();
		}
	}

    
	/**
	 * Is called by the thread.  makes the
	 * actual call upload the file or appropriate
	 * error information.
	 */
	public void start() {
		try {
			prepareFile();
		} catch (IOException e) {
			setState(FILE_NOT_FOUND);
		}
		try {
			readHeader();
			_state.doUpload(this);		   
		} catch (FreeloaderUploadingException e) { 
			setState(FREELOADER);
			try {
			    _state.doUpload(this);
			} catch (IOException e2) {};
		} catch (IOException e) {
			setState(INTERRUPTED);
		}
		// this is necessary to avoid writing the same alternate
		// locations back to the requester as they sent in their
		// original headers
		_fileDesc.commitTemporaryAlternateLocations();
	}

    /**
	 * closes the outputstream, inputstream, and socket
	 * if they are not null.
	 */
	public void stop() {
		try {
			if (_ostream != null)
				_ostream.close();
		} catch (IOException e) {}
		try {
			if (_fis != null)
				_fis.close();
		} catch (IOException e) {}
		try {
			if (_socket != null) 
				_socket.close();
		} catch (IOException e) {}
	}
    
	/**
	 * This method changes the appropriate state class based on
	 * the integer representing the state.  I'm not sure if this
	 * is a good idea, since it results in a case statement, that
	 * i was trying to avoid with 
	 */
	public void setState(int state) {
		_stateNum = state;
		switch (state) {
		case CONNECTING:
			_state = new NormalUploadState();
			break;
		case LIMIT_REACHED:
			_state = new LimitReachedUploadState();
			break;
		case PUSH_FAILED:
			_state = new PushFailedUploadState();
			break;
		case FREELOADER:     
			_state = new FreeloaderUploadState();
			break;
		case FILE_NOT_FOUND:
			_state = new FileNotFoundUploadState();
		case COMPLETE:
		case INTERRUPTED:
			break;
		}
	}
	
	/**
	 * Stores the alternate location for this upload in the associated 
	 * <tt>FileDesc</tt> instance.
	 */
	public void storeAlternateLocation() {
		// ignore if this is a push upload
		if(_socket == null) return;
		try {
			AlternateLocation al = 
			    new AlternateLocation(_hostName, _fileDesc.getSHA1Urn());
			_fileDesc.addAlternateLocation(al);
		} catch(MalformedURLException e) {
			// if the url is invalid, it simply will not be added to the list
			// of alternate locations
		}		
	}

	/****************** accessor methods *****************/


	public OutputStream getOutputStream() {return _ostream;}
	public int getIndex() {return _index;}
	public String getFileName() {return _filename;}
	public int getFileSize() {return _fileSize;}
	public InputStream getInputStream() {return _fis;}
    /** The number of bytes read, including any skipped by the Range header. */
	public int amountUploaded() {return _amountRead;}
	public void setAmountUploaded(int amount) {_amountRead = amount;}
    /** The byte offset where we should start the upload. */
	public int getUploadBegin() {return _uploadBegin;}
    /** Returns the offset of the last byte to send <b>PLUS ONE</b>. */
    public int getUploadEnd() {return _uploadEnd;}
	public int getState() {return _stateNum;}
	public String getHost() {return _hostName;}
	public UploadManager getManager() {return _manager;}

	public String getThisHost() {return _manager.getThisHost(); } 
	public int getThisPort() {return _manager.getThisPort(); }

	public boolean chatEnabled() {return _chatEnabled;}
	public String getChatHost() {return _chatHost;}
	public int getChatPort() {return _chatPort;}

	/**
	 * Accessor for the <tt>URN</tt> instance for the file requested, which
	 * is <tt>null</tt> if there is no URN for the file.
	 *
	 * @return the <tt>URN</tt> instance for the file being uploaded, which
	 *  can be <tt>null</tt> if no URN has been assigned
	 */
	public URN getUrn() {return _urn;}

	/**
	 * Returns the requested <tt>URN</tt> as specified in the 
	 * "X-Gnutella-Content-URN" extension header, defined in HUGE v0.93.
	 *
	 * @return the requested <tt>URN</tt>
	 */
	public URN getRequestedUrn() {return _requestedURN;}

	/**
	 * Returns the <tt>FileDesc</tt> instance for this uploader.
	 *
	 * @return the <tt>FileDesc</tt> instance for this uploader, or
	 *  <tt>null</tt> if the <tt>FileDesc</tt> could not be assigned
	 *  from the shared files
	 */
	public FileDesc getFileDesc() {return _fileDesc;}
	/****************** private methods *******************/


	private void readHeader() throws IOException {

        String str = " ";
        _uploadBegin = 0;
        _uploadEnd = 0;
		String userAgent;
		
		InputStream istream = _socket.getInputStream();
		ByteReader br = new ByteReader(istream);
        
		// NOTE: it might improve readability to move
		// the try and catches around the big loops.

		while (true) {
			// read the line in from the socket.
            str = br.readLine();
			// break out of the loop if it is null or blank
            if ( (str==null) || (str.equals("")) )
                break;

			else if (str.toUpperCase().indexOf("CHAT:") != -1) {
				String sub;
				try {
					sub = str.substring(5);
				} catch (IndexOutOfBoundsException e) {
					throw new IOException();
                }
				sub = sub.trim();
				int colon = sub.indexOf(":");
				String host  = sub.substring(0,colon);
				host = host.trim();
				String sport = sub.substring(colon+1);
				sport = sport.trim();

				int port; 
				try {
					port = java.lang.Integer.parseInt(sport);
				} catch (NumberFormatException e) {
					throw new IOException();
                }
				_chatEnabled = true;
				_chatHost = host;
				_chatPort = port;
			}
			// Look for range header of form, "Range: bytes=", "Range:bytes=",
			// "Range: bytes ", etc.  Note that the "=" is required by HTTP, but
            //  old versions of BearShare do not send it.  The value following the
            //  bytes unit will be in the form '-n', 'm-n', or 'm-'.
            else if (indexOfIgnoreCase(str, "Range:") == 0) {
                //Set 'sub' to the value after the "bytes=" or "bytes ".  Note
                //that we don't validate the data between "Range:" and the
                //bytes.
				String sub;
				String second;
				try {
                    int i=str.indexOf("bytes");    //TODO: use constant
                    if (i<0)
                        throw new IOException();
                    i+=6;                          //TODO: use constant
					sub = str.substring(i);
				} catch (IndexOutOfBoundsException e) {
					throw new IOException();
				}
				// remove the white space
                sub = sub.trim();   
                char c;
				// get the first character
				try {
					c = sub.charAt(0);
				} catch (IndexOutOfBoundsException e) {
					throw new IOException();
				}
				// - n  
                if (c == '-') {  
					// String second;
					try {
						second = sub.substring(1);
					} catch (IndexOutOfBoundsException e) {
						throw new IOException();
					}
                    second = second.trim();
					try {
                        //A range request for "-3" means return the last 3 bytes
                        //of the file.  (LW used to incorrectly return bytes
                        //0-3.)  
                        _uploadBegin = _fileSize-Integer.parseInt(second);
						_uploadEnd = _fileSize;
					} catch (NumberFormatException e) {
						throw new IOException();
					}
                }
                else {                
					// m - n or 0 -
                    int dash = sub.indexOf("-");
					String first;
					try {
						first = sub.substring(0, dash);
					} catch (IndexOutOfBoundsException e) {
						throw new IOException();
					}
                    first = first.trim();
					try {
						_uploadBegin = java.lang.Integer.parseInt(first);
					} catch (NumberFormatException e) {
						throw new IOException();
					}
					try {
						second = sub.substring(dash+1);
					} catch (IndexOutOfBoundsException e) {
						throw new IOException();
					}
                    second = second.trim();
                    if (!second.equals("")) 
						try {
                            //HTTP range requests are inclusive.  So "1-3" means
                            //bytes 1, 2, and 3.  But _uploadEnd is an EXCLUSIVE
                            //index, so increment by 1.
							_uploadEnd = java.lang.Integer.parseInt(second)+1;
                    } catch (NumberFormatException e) {
						throw new IOException();
					}
                }
            }

			// check the User-Agent field of the header information
			else if (indexOfIgnoreCase(str, "User-Agent:") != -1) {
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
						throw new FreeloaderUploadingException();
					}
				}
				userAgent = str.substring(11).trim();
			}

			else if(indexOfIgnoreCase(str, HTTPConstants.CONTENT_URN_HEADER)!=-1) {
				_requestedURN = HTTPUploader.readContentURN(str);
			}
			else if(indexOfIgnoreCase(str, HTTPConstants.ALTERNATE_LOCATION_HEADER)!=-1) {
				HTTPUploader.readAlternateLocations(this._fileDesc, str);
			}
		}

		if (_uploadEnd == 0)
			_uploadEnd = _fileSize;
	}

	/**
	 * This method parses the "X-Gnutella-Content-URN" header, as specified
	 * in HUGE v0.93.  This assigns the requested urn value for this 
	 * upload, which otherwise remains null.
	 *
	 * @param CONTENT_URN_STR the string containing the header
	 * @return a new <tt>URN</tt> instance for the request line, or 
	 *  <tt>null</tt> if there was any problem creating it
	 */
	private static URN readContentURN(final String CONTENT_URN_STR) {
		int offset = CONTENT_URN_STR.indexOf(":");
		int spaceIndex = CONTENT_URN_STR.indexOf(" ");
		if(offset == -1) {
			return null;
		}
		if(spaceIndex == -1) {
			// this means that there's no space after the colon
			offset++;
		}
		else if((spaceIndex - offset) == 1) {
			// this means that there is a space after the colon,
			// so the urn is offset by one more index
			offset += 2;
		}
		else {
			// otherwise, the request is of an unknown form, so just
			// return without setting _requestedURN
			return null;
		}
		
		String urnStr = CONTENT_URN_STR.substring(offset);
		try {
			return URNFactory.createURN(urnStr);
		} catch(IOException e) {
			// this will be thrown if the URN string was invalid for any
			// reason -- just return null
			return null;
		}		
	}
	
	/**
	 * Reads alternate location header.  The header can contain only one
	 * alternate location, or it can contain many in the same header.
	 * This method adds them all to the <tt>FileDesc</tt> for this
	 * uploader.  This will not allow more than 20 alternate locations
	 * for a single file.
	 *
	 * @param FILE_DESC the <tt>FileDesc</tt> to insert alternate 
	 *  locations into
	 * @param ALT_HEADER the full alternate locations header
	 */
	private static void readAlternateLocations(final FileDesc FILE_DESC,
											   final String ALT_HEADER) {
		int colonIndex = ALT_HEADER.indexOf(":");
		if(colonIndex == -1) {
			return;
		}
		final String ALTERNATE_LOCATIONS = 
		    ALT_HEADER.substring(colonIndex+1).trim();
		StringTokenizer st = new StringTokenizer(ALTERNATE_LOCATIONS, ",");

		// this limits the number of alternate location headers to read
		// to 20
		int i=0;
		while(st.hasMoreTokens() && (i<20)) {
			HTTPUploader.storeAlternateLocation(FILE_DESC, 
												st.nextToken().trim());
			i++;
		}
	}

	/**
	 * Reads an individual alternate location and adds a new 
	 * <tt>AlternateLocation</tt> instance to the specified 
	 * <tt>FileDesc</tt>.
	 *
	 * @param FILE_DESC the <tt>FileDesc</tt> to insert alternate locations 
	 *  into
	 * @param LOCATION the string representation of the individual alternate 
	 *  location to add
	 */
	private static void storeAlternateLocation(final FileDesc FILE_DESC,
											   final String LOCATION) {
		// note that this removes other "whitespace" characters besides
		// space and tab, which is not strictly correct
		final String LINE = LOCATION.trim();		
		AlternateLocation al = null;
		try {
			al = new AlternateLocation(LINE);
		} catch(IOException e) {
			e.printStackTrace();
			// just return without adding it.
			return;
		}
		FILE_DESC.addTemporaryAlternateLocation(al);
	}

	/**
	 * a helper method to compare two strings 
	 * ignoring their case.
	 */ 
	private int indexOfIgnoreCase(String str, String section) {
		// convert both strings to lower case -- this is expensive
		String aaa = str.toLowerCase();
		String bbb = section.toLowerCase();
		// then look for the index...
		return aaa.indexOf(bbb);
	}


	/**
	 * prepares the file to be read for sending accross the socket
	 */
	private void prepareFile() throws IOException {
		// get the appropriate file descriptor
		FileDesc fdesc;
		try {
			fdesc = _fileManager.get(_index);
		} catch (IndexOutOfBoundsException e) {
			throw new IOException();
		}
		
		/* For regular (client-side) uploads, get name. 
		 * For pushed (server-side) uploads, check to see that 
		 * the index matches the filename. */
		String name = fdesc._name;
		if (_filename == null) {
            _filename = name;
        } else {
			/* matches the name */
			if ( !name.equals(_filename) ) {
				throw new IOException();
			}
        }

		// set the file size
        _fileSize = fdesc._size;

		// get the fileInputStream
		_fis = fdesc.getInputStream();

	}
  
    public void measureBandwidth() {
        bandwidthTracker.measureBandwidth(amountUploaded());
    }

    public float getMeasuredBandwidth() {
        return bandwidthTracker.getMeasuredBandwidth();
    }
    
    //inherit doc comment
    public boolean getCloseConnection() {
        return _state.getCloseConnection();
    }

	/*
	public static void main(String[] args) {
		String str = HTTPConstants.CONTENT_URN_HEADER +
		" urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB";
		URN urn = HTTPUploader.readContentURN(str);

		String alt0 = "X-Gnutella-Alternate-Location: http://Y.Y.Y.Y:6352/get/2/"+
		"lime%20capital%20management%2001.mpg "+
		"2002-04-09T20:32:33Z";
		String alt1 = "X-Gnutella-Alternate-Location: http://Y.Y.Y.Y:6352/get/2/"+
		"lime%20capital%20management%2001.mpg "+
		"2002-04-09T20:32:33Z, "+
		"http://Y.Y.Y.Y:6352/get/2/"+
		"lime%20capital%20management%2002.mpg "+
		"2002-04-09T20:32:33Z, "+
		"http://Y.Y.Y.Y:6352/get/2/"+
		"lime%20capital%20management%2003.mpg "+
		"2002-04-09T20:32:33Z";
		
		FileDesc newFileDesc = new FileDesc();
		HTTPUploader.readAlternateLocations(newFileDesc, alt0);
		HTTPUploader.readAlternateLocations(newFileDesc, alt1);
		System.out.println("FileDesc: ");
		System.out.println(newFileDesc); 
		System.out.println("size: "+newFileDesc.size()); 
	}
	*/
}










