package com.limegroup.gnutella;

import java.io.*;
import java.util.Properties;
import com.sun.java.util.collections.*;
import java.lang.IllegalArgumentException;
import java.util.Enumeration;
import java.util.StringTokenizer;
import com.limegroup.gnutella.util.StringUtils;

/**
 * This class manages the property settings.  It maintains
 * default settings for values not set in the saved
 * settings files and updates those settings based on user
 * input, checking for errors where appropriate.  It also
 * saves the settings file to disk when the session
 * terminates.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public class SettingsManager implements SettingsInterface {

	private final String CURRENT_DIRECTORY = System.getProperty("user.dir");
 
	/** Variables for the various settings */
    private volatile boolean  _forceIPAddress;
    private volatile byte[]   _forcedIPAddress;
    private volatile String   _forcedIPAddressString;
    private volatile int      _forcedPort;
	private volatile boolean  _allowBroswer;
    private volatile byte     _ttl;
    private volatile byte     _softmaxttl;
    private volatile byte     _maxttl;
    private volatile int      _maxLength;
    private volatile int      _timeout;
    private volatile String   _hostList;
    private volatile int      _keepAlive;
    private volatile int      _port;
    private volatile int      _connectionSpeed;
    private volatile int      _uploadSpeed;
    private volatile byte     _searchLimit;
    private volatile String   _clientID;
    private volatile int      _maxIncomingConn;
    private volatile File     _saveDirectory;
    private volatile File     _incompleteDirectory;
    private volatile String   _directories;
    private volatile String   _extensions;
    private volatile String[] _bannedIps;
    private volatile String[] _bannedWords;
    private volatile boolean  _filterDuplicates;
    private volatile boolean  _filterAdult;
    private volatile boolean  _filterVbs;
    private volatile boolean  _filterHtml;
    private volatile boolean  _filterGreedyQueries;
    private volatile boolean  _filterBearShare;
    private volatile boolean  _useQuickConnect;
    private volatile String[] _quickConnectHosts;
    private volatile int      _parallelSearchMax;
    private volatile boolean  _clearCompletedUpload;
    private volatile boolean  _clearCompletedDownload;
    private volatile int      _maxSimDownload;
    private volatile boolean  _promptExeDownload;
    private volatile int      _maxUploads;
    private volatile int      _searchAnimationTime;
    private volatile int      _uploadsPerPerson;

	private volatile boolean  _chatEnabled;          

    /** connectString_ is something like "GNUTELLA CONNECT..."
     *  connectStringOk_ is something like "GNUTELLA OK..."
     *  INVARIANT: connectString_=connectStringFirstWord_+" "+connectStringRemainder_
     *             connectString!=""
     *             connectStringFirstWord does not contain spaces
     */
    private volatile String   _connectString;
    private volatile String   _connectStringFirstWord;
    private volatile String   _connectStringRemainder;
    private volatile String   _connectOkString;
    private volatile int      _basicQueryInfo;
    private volatile int      _advancedQueryInfo;
    private volatile int      _freeLoaderFiles;
    private volatile int      _freeLoaderAllowed;
	private volatile int      _sessions;
	private volatile long     _averageUptime;
	private volatile long     _totalUptime;
	private volatile boolean  _installed;
	private volatile boolean  _acceptedIncoming = false;


    /** 
	 * Set up a local variable for the properties
	 */
    private volatile Properties _props;

    /** 
	 * Specialized properties file for the network discoverer
     */
    private volatile Properties _ndProps;

    /**
     * Set up the manager instance to follow the singleton pattern.
     */
    private static SettingsManager _instance = new SettingsManager();


    /**
     * This method provides the only access
     * to an instance of this class in
     * accordance with the singleton pattern
     */
    public static SettingsManager instance() {
        return _instance;
    }

    /** 
	 * The constructor is private to ensure that only one copy 
	 * will be created
     */
    private SettingsManager() {
        _props      = new Properties();
        _ndProps    = new Properties();
		
        FileInputStream fis;
        try {
            fis = new FileInputStream(new File(ND_PROPS_NAME));
            try {_ndProps.load(fis);}
            catch(IOException ioe) {}
        }
        catch(FileNotFoundException fne){}
        catch(SecurityException se) {}
		initialize();
    }


    /** Check the properties file and set the props */
    private void initialize() {
        Properties tempProps = new Properties();
        FileInputStream fis;
        try {
            fis = new FileInputStream(new File(PROPS_NAME));
            try {
                tempProps.load(fis);
                loadDefaults();
                try {
                    fis.close();
                    validateFile(tempProps);
                }
                catch(IOException e) {
					// error closing the file, so continue using the 
					// defaults.
				}
            }
            catch(IOException e){loadDefaults();}
        }
        catch(FileNotFoundException fnfe){loadDefaults();}
        catch(SecurityException se){loadDefaults();}
    }

    /** 
	 * Sets all of the properties manually to ensure that each
	 * property is valid.
     */
    private void validateFile(Properties tempProps) {
        String p;
        Enumeration enum = tempProps.propertyNames();
        while(enum.hasMoreElements()){
            String key;
            try {
                key = (String)enum.nextElement();
                p = tempProps.getProperty(key);
                if(key.equals(TTL)) {
                    setTTL(Byte.parseByte(p));
                }
				else if(key.equals(ALLOW_BROWSER)) {
					boolean bs;  
					if (p.equals("true"))
                        bs=true;
                    else if (p.equals("false"))
                        bs=false;
                    else
                        break;
                    setAllowBrowser(bs);
				}
                else if(key.equals(SOFT_MAX_TTL)) {
                    setSoftMaxTTL(Byte.parseByte(p));
                }
                else if(key.equals(MAX_TTL)) {
                    setMaxTTL(Byte.parseByte(p));
                }
                else if(key.equals(MAX_LENGTH)) {
                    setMaxLength(Integer.parseInt(p));
                }
                else if(key.equals(PARALLEL_SEARCH)) {
                    setParallelSearchMax(Integer.parseInt(p));
                }
                else if(key.equals(MAX_SIM_DOWNLOAD)) {
                    setMaxSimDownload(Integer.parseInt(p));
                } 
                else if(key.equals(PROMPT_EXE_DOWNLOAD)) {
                    boolean bs;
                    if (p.equals("true"))
                        bs=true;
                    else if (p.equals("false"))
                        bs=false;
                    else
                        break;
                    setPromptExeDownload(bs);
                }
                else if(key.equals(MAX_UPLOADS)) {
                    setMaxUploads(Integer.parseInt(p));
                }
                else if(key.equals(CLEAR_DOWNLOAD)) {
                    boolean bs;
                    if (p.equals("true"))
                        bs=true;
                    else if (p.equals("false"))
                        bs=false;
                    else
                        break;
                    setClearCompletedDownload(bs);
                }
                else if(key.equals(CLEAR_UPLOAD)) {
                    boolean bs;
                    if (p.equals("true"))
                        bs=true;
                    else if (p.equals("false"))
                        bs=false;
                    else
                        break;
                    setClearCompletedUpload(bs);
                }
                else if(key.equals(TIMEOUT)) {
                    setTimeout(Integer.parseInt(p));
                }
				else if(key.equals(UPLOADS_PER_PERSON)){
					setUploadsPerPerson(Integer.parseInt(p));
				}
                else if(key.equals(KEEP_ALIVE)) {
                    //Verified for real later.  See note below.
                    setKeepAlive(Integer.parseInt(p));
                }
                else if(key.equals(PORT)) {
                    setPort(Integer.parseInt(p));
                }
                else if(key.equals(SPEED)) {
                    setConnectionSpeed(Integer.parseInt(p));
                }
                else if(key.equals(UPLOAD_SPEED)) {
                    setUploadSpeed(Integer.parseInt(p));
                }
                else if(key.equals(SEARCH_LIMIT)) {
                    setSearchLimit(Byte.parseByte(p));
                }
				else if(key.equals(CHAT_ENABLED)) {
					boolean bs;
                    if (p.equals("true"))
                        bs=true;
                    else if (p.equals("false"))
                        bs=false;
                    else
                        break;
                    setChatEnabled(bs);
				}
                else if(key.equals(CLIENT_ID)) {
                    setClientID(p);
                }

                else if(key.equals(MAX_INCOMING_CONNECTIONS)) {
                    //Verified for real later.  See note below.
                    setMaxIncomingConnections(Integer.parseInt(p));
                }

                else if(key.equals(SAVE_DIRECTORY)) {
					try {
						setSaveDirectory(new File(p));
					} catch(IOException ioe) {
						// if we get an IOException, then the save 
						// directory could not be set for some reason,
						// so simply use the default
						try {
							setSaveDirectory(getSaveDefault());							
							addDirectory(getSaveDefault());
						} catch(IOException ioe2) {
							// not much we can do if this also throws
							// an exception.
						}
					}
                }

                else if(key.equals(DIRECTORIES)) {
                    setDirectories(p);
                }

                else if(key.equals(EXTENSIONS)) {
                    setExtensions(p);
                }
                else if(key.equals(CHECK_AGAIN)) {
                    boolean bs;
                    if (p.equals("true"))
                        bs=true;
                    else if (p.equals("false"))
                        bs=false;
                    else
                        break;
                    setCheckAgain(bs);
                }
                else if(key.equals(BANNED_IPS)) {
                    setBannedIps(decode(p));
                }
                else if(key.equals(BANNED_WORDS)) {
                    setBannedWords(decode(p));
                }
                else if(key.equals(FILTER_ADULT)) {
                    boolean bs;
                    if (p.equals("true"))
                        bs=true;
                    else if (p.equals("false"))
                        bs=false;
                    else
                        break;
                    setFilterAdult(bs);
                }
                else if(key.equals(FILTER_DUPLICATES)) {
                    boolean bs;
                    if (p.equals("true"))
                        bs=true;
                    else if (p.equals("false"))
                        bs=false;
                    else
                        break;
                    setFilterDuplicates(bs);
                }
                else if(key.equals(FILTER_HTML)) {
                    boolean bs;
                    if (p.equals("true"))
                        bs=true;
                    else if (p.equals("false"))
                        bs=false;
                    else
                        break;
                    setFilterHtml(bs);
                }
                else if(key.equals(FILTER_VBS)) {
                    boolean bs;
                    if (p.equals("true"))
                        bs=true;
                    else if (p.equals("false"))
                        bs=false;
                    else
                        break;
                    setFilterVbs(bs);
                }
                else if(key.equals(FILTER_GREEDY_QUERIES)) {
                    boolean bs;
                    if (p.equals("true"))
                        bs=true;
                    else if (p.equals("false"))
                        bs=false;
                    else
                        break;
                    setFilterGreedyQueries(bs);
                }

                else if(key.equals(FILTER_BEARSHARE_QUERIES)) {
                    boolean bs;
                    if (p.equals("true"))
                        bs=true;
                    else if (p.equals("false"))
                        bs=false;
                    else
                        break;
                    setFilterBearShareQueries(bs);
                }

                else if(key.equals(QUICK_CONNECT_HOSTS)) {
                    setQuickConnectHosts(decode(p));
                }

                else if(key.equals(USE_QUICK_CONNECT)) {
                    boolean bs;
                    if (p.equals("true"))
                        bs=true;
                    else if (p.equals("false"))
                        bs=false;
                    else
                        break;
                    setUseQuickConnect(bs);
                }
                else if(key.equals(SEARCH_ANIMATION_TIME)) {
                    setSearchAnimationTime(Integer.parseInt(p));
                }
                else if(key.equals(CONNECT_STRING)) {
                    setConnectString(p);
                }
                else if(key.equals(CONNECT_OK_STRING)){
                    setConnectOkString(p);
                }

                else if(key.equals(BASIC_QUERY_INFO)){
                    setBasicInfoForQuery(Integer.parseInt(p));
                }

                else if(key.equals(ADVANCED_QUERY_INFO)){
                    setAdvancedInfoForQuery(Integer.parseInt(p));
                }
                else if(key.equals(FORCE_IP_ADDRESS)){
                    boolean bs;
                    if (p.equals("true"))
                        bs=true;
                    else if (p.equals("false"))
                        bs=false;
                    else
                        break;
                    setForceIPAddress(bs);
                }
                else if(key.equals(FORCED_IP_ADDRESS)){
                    setForcedIPAddress(p.getBytes());
                }
                else if(key.equals(FORCED_IP_ADDRESS_STRING)){
                    setForcedIPAddressString(p);
                }
                else if(key.equals(FORCED_PORT)){
                    setForcedPort(Integer.parseInt(p));
                }
                else if(key.equals(FREELOADER_FILES)) {
                    setFreeloaderFiles(Integer.parseInt(p));
                }
                else if(key.equals(FREELOADER_ALLOWED)) {
                    setFreeloaderAllowed(Integer.parseInt(p));
                }
				else if(key.equals(AVERAGE_UPTIME)) {
					setAverageUptime(Long.parseLong(p));
				}
				else if(key.equals(TOTAL_UPTIME)) {
					setTotalUptime(Long.parseLong(p));
				}
				else if(key.equals(SESSIONS)) {
					setSessions(Integer.parseInt(p)+1);
				}
				else if(key.equals(INSTALLED)) {
					Boolean installed = new Boolean(p);
					setInstalled(installed.booleanValue());
				}
				else if(key.equals(APP_WIDTH)) {
					setAppWidth(Integer.parseInt(p));
				}
				else if(key.equals(APP_HEIGHT)) {
					setAppHeight(Integer.parseInt(p));
				}
				else if(key.equals(RUN_ONCE)) {
					Boolean runOnce = new Boolean(p);
					setRunOnce(runOnce.booleanValue());
				}

				else if(key.equals(WINDOW_X)) {
					setWindowX(Integer.parseInt(p));
				}
				else if(key.equals(WINDOW_Y)) {
					setWindowY(Integer.parseInt(p));
				}

				else if(key.equals(SHOW_TRAY_DIALOG)) {
					Boolean showTrayDialog = new Boolean(p);
					setShowTrayDialog(showTrayDialog.booleanValue());
				}

				else if(key.equals(MINIMIZE_TO_TRAY)) {
					Boolean minimize = new Boolean(p);
					setMinimizeToTray(minimize.booleanValue());
				}

				else if(key.equals(SHOW_CLOSE_DIALOG)) {
					Boolean showCloseDialog = new Boolean(p);
					setShowCloseDialog(showCloseDialog.booleanValue());
				}
                else if(key.equals(CLASSPATH)){
                    setClassPath(p);
                }
                else if(key.equals(MAIN_CLASS)){
                    setMainClass(p);
                }
            }
            catch(NumberFormatException nfe){ /* continue */ }
            catch(IllegalArgumentException iae){ /* continue */ }
            catch(ClassCastException cce){ /* continue */ }
        }

        //Special case: if this is a modem, ensure that KEEP_ALIVE and
        //MAX_INCOMING_CONNECTIONS are sufficiently low.
        if ( getConnectionSpeed()<=56 ) { //modem
            setKeepAlive(Math.min(2, getKeepAlive()));
            setMaxIncomingConnections(2);
        }
    }

    /** 
	 * Load in the default values.  Any properties
     * written to the real properties file will overwrite
     * these. 
	 */
    private void loadDefaults() {
		setAllowBrowser(DEFAULT_ALLOW_BROWSER);
        setMaxTTL(DEFAULT_MAX_TTL);
        setSoftMaxTTL(DEFAULT_SOFT_MAX_TTL);
        setTTL(DEFAULT_TTL);
        setMaxLength(DEFAULT_MAX_LENGTH);
        setTimeout(DEFAULT_TIMEOUT);
        setKeepAlive(DEFAULT_KEEP_ALIVE);
        setPort(DEFAULT_PORT);
        setConnectionSpeed(DEFAULT_SPEED);
        setUploadSpeed(DEFAULT_UPLOAD_SPEED);
        setSearchLimit(DEFAULT_SEARCH_LIMIT);
        setClientID( (new GUID(Message.makeGuid())).toHexString() );
        setMaxIncomingConnections(DEFAULT_MAX_INCOMING_CONNECTION);
        setBannedIps(DEFAULT_BANNED_IPS);
        setBannedWords(DEFAULT_BANNED_WORDS);
        setFilterAdult(DEFAULT_FILTER_ADULT);
        setFilterDuplicates(DEFAULT_FILTER_DUPLICATES);
        setFilterVbs(DEFAULT_FILTER_VBS);
        setFilterHtml(DEFAULT_FILTER_HTML);
        setFilterGreedyQueries(DEFAULT_FILTER_GREEDY_QUERIES);
        setExtensions(DEFAULT_EXTENSIONS);
        setBannedIps(DEFAULT_BANNED_IPS);
        setBannedWords(DEFAULT_BANNED_WORDS);
        setFilterAdult(DEFAULT_FILTER_ADULT);
        setFilterDuplicates(DEFAULT_FILTER_DUPLICATES);
        setFilterVbs(DEFAULT_FILTER_VBS);
        setFilterHtml(DEFAULT_FILTER_HTML);
        setFilterGreedyQueries(DEFAULT_FILTER_GREEDY_QUERIES);
        setFilterBearShareQueries(DEFAULT_FILTER_BEARSHARE_QUERIES);
        setUseQuickConnect(DEFAULT_USE_QUICK_CONNECT);
        setQuickConnectHosts(DEFAULT_QUICK_CONNECT_HOSTS);
        setParallelSearchMax(DEFAULT_PARALLEL_SEARCH);
        setClearCompletedUpload(DEFAULT_CLEAR_UPLOAD);
        setClearCompletedDownload(DEFAULT_CLEAR_DOWNLOAD);
        setMaxSimDownload(DEFAULT_MAX_SIM_DOWNLOAD);
        setPromptExeDownload(DEFAULT_PROMPT_EXE_DOWNLOAD);
        setMaxUploads(DEFAULT_MAX_UPLOADS);
        setSearchAnimationTime(DEFAULT_SEARCH_ANIMATION_TIME);
        setConnectString(DEFAULT_CONNECT_STRING);
        setConnectOkString(DEFAULT_CONNECT_OK_STRING);

        setCheckAgain(DEFAULT_CHECK_AGAIN);
        setBasicInfoForQuery(DEFAULT_BASIC_INFO_FOR_QUERY);
        setAdvancedInfoForQuery(DEFAULT_ADVANCED_INFO_FOR_QUERY);
        setForceIPAddress(DEFAULT_FORCE_IP_ADDRESS);
        setForcedIPAddress(DEFAULT_FORCED_IP_ADDRESS);
        setForcedIPAddressString(DEFAULT_FORCED_IP_ADDRESS_STRING);
        setForcedPort(DEFAULT_FORCED_PORT);
        setFreeloaderFiles(DEFAULT_FREELOADER_FILES);
        setFreeloaderAllowed(DEFAULT_FREELOADER_ALLOWED);

		setUploadsPerPerson(DEFAULT_UPLOADS_PER_PERSON);
		setAverageUptime(DEFAULT_AVERAGE_UPTIME);
		setTotalUptime(DEFAULT_TOTAL_UPTIME);
		setSessions(DEFAULT_SESSIONS);
		setInstalled(DEFAULT_INSTALLED);
		setRunOnce(DEFAULT_RUN_ONCE);
		setShowTrayDialog(DEFAULT_SHOW_TRAY_DIALOG);
		setMinimizeToTray(DEFAULT_MINIMIZE_TO_TRAY);
		setShowCloseDialog(DEFAULT_SHOW_CLOSE_DIALOG);
		setClassPath(DEFAULT_CLASSPATH);
		setMainClass(DEFAULT_MAIN_CLASS);

		setAppWidth(DEFAULT_APP_WIDTH);
		setAppHeight(DEFAULT_APP_HEIGHT);

		setChatEnabled(DEFAULT_CHAT_ENABLED);
    }


    /******************************************************
     *************  START OF ACCESSOR METHODS *************
     ******************************************************/

	public boolean getAllowBrowser() {return _allowBroswer;}

    /** returns the time to live */
    public byte getTTL(){return _ttl;}

    /** return the soft maximum time to live */
    public byte getSoftMaxTTL(){return _softmaxttl;}

    /** returns the maximum time to live*/
    public byte getMaxTTL(){return _maxttl;}

    /** returns the maximum allowable length of packets*/
    public int getMaxLength(){return _maxLength;}

    /** returns the timeout value*/
    public int getTimeout(){return _timeout;}

    /** 
	 * returns a string specifying the full
     * pathname of the file listing the hosts 
	 */
    public String getHostList() {
		File hostListFile = new File(CURRENT_DIRECTORY, HOST_LIST_NAME);		
		return hostListFile.getAbsolutePath();
	}

    /** returns the keep alive value */
    public int getKeepAlive(){return _keepAlive;}

    /** returns the client's port number */
    public int getPort(){return _port;}

    /** returns the client's connection speed in kilobits/sec
     *  (not kilobytes/sec) */
    public int getConnectionSpeed(){return _connectionSpeed;}

    public int getUploadSpeed() { return _uploadSpeed; }

    /** returns the client's search speed */
    public byte getSearchLimit(){return _searchLimit;}

    /** returns the client id number */
    public String getClientID(){return _clientID;}

    /** returns the maximum number of connections to hold */
    public int getMaxIncomingConnections(){return _maxIncomingConn;}

	/** returns the maximum number of uploads per person */
    public int getUploadsPerPerson(){return _uploadsPerPerson;}

    /** 
	 * Returns a new <code>File</code> instance that denotes the abstract
	 * pathname of the directory for saving files.
	 *
	 * @return  A <code>File</code> instance denoting the abstract
	 *          pathname of the save directory.
	 *
	 * @throws  FileNotFoundException
	 *          If the incomplete directory is <code>null</code>.      
	 */
    public File getSaveDirectory() throws FileNotFoundException {
		if(_saveDirectory == null) throw new FileNotFoundException();
		return _saveDirectory;
	}

	/** returns true if the chat is enabled */
	public boolean getChatEnabled() {return _chatEnabled;}

    /** 
	 * Returns a new <code>File</code> instance that denotes the abstract
	 * pathname of the directory for saving incomplete files.
	 *
	 * @return  A <code>File</code> instance denoting the abstract
	 *          pathname of the directory for saving incomplete files.
	 *
	 * @throws  FileNotFoundException
	 *          If the incomplete directory is <code>null</code>.      
	 */
    public File getIncompleteDirectory() throws FileNotFoundException {
		if(_incompleteDirectory == null) throw new FileNotFoundException();
		return _incompleteDirectory;
    }

    /** 
	 * Returns a new <code>File</code> instance that denotes the abstract
	 * pathname of the default directory for saving incomplete files. This
	 * is a shared directory within the current working directory.
	 *
	 * @return  A <code>File</code> instance denoting the abstract
	 *          pathname of the default directory for saving files.
	 */	
    public File getSaveDefault() {		
		return new File(SAVE_DIRECTORY_NAME);
    }

    /** returns the directories to search */
    public String getDirectories(){return _directories;}

	/** returns the shared directories as an array of pathname strings. */
    public String[] getDirectoriesAsArray() {
		if(_directories == null) return new String[0];		
        _directories.trim();
        return StringUtils.split(_directories, ';');
    }

	/**
	 * returns an array of Strings of directory path names.  these are the
	 * pathnames of the shared directories as well as the pathname of 
	 * the Incomplete directory.
	 */
	public String[] getDirectoriesWithIncompleteAsArray() {
		String temp = _directories;
        temp.trim();
		if(!temp.endsWith(";")) 
			temp += ";";
		String incompleteDir = "";
		try {
			incompleteDir = getIncompleteDirectory().getAbsolutePath();
		} catch(FileNotFoundException fnfe) {			
		}
		temp += incompleteDir;
        return StringUtils.split(temp, ';');		
	}
    
    /** 
	 * Returns a new <code>File</code> instance that denotes the abstract
	 * pathname of the file with a snapshot of current downloading files.
	 *
	 * <p>This file is stored in the incomplete directory and is a read-only
     * property.
	 *
	 * @return  A <code>File</code> instance denoting the abstract
	 *          pathname of the file with a snapshot of current downloading
	 *          files.
	 */
    public File getDownloadSnapshotFile() {
		File incompleteDir = null;
		try {
			incompleteDir = getIncompleteDirectory();
		} catch(FileNotFoundException fnfe) {
			// this is ok, as incompleteDir will remain null, and this will
			// return the snapshot file from the current directory.
		}
        return (new File(incompleteDir, "downloads.dat"));
    }


    /** returns the string of file extensions*/
    public String getExtensions(){return _extensions;}

    /** returns the string of default file extensions to share.*/
    public String getDefaultExtensions() {
		return SettingsInterface.DEFAULT_EXTENSIONS;
	}

    public String[] getBannedIps(){return _bannedIps;}
    public String[] getBannedWords(){return _bannedWords;}
    public boolean getFilterAdult(){return _filterAdult;}
    public boolean getFilterDuplicates(){return _filterDuplicates;}
    public boolean getFilterHtml(){return _filterHtml;}
    public boolean getFilterVbs(){return _filterVbs;}
    public boolean getFilterGreedyQueries() { return _filterGreedyQueries; }
    public boolean getFilterBearShareQueries() { return _filterBearShare; }

    public boolean getUseQuickConnect(){return _useQuickConnect;}
    public String[] getQuickConnectHosts(){return _quickConnectHosts;}
    public int getParallelSearchMax(){return _parallelSearchMax;}
    public int getMaxSimDownload(){return _maxSimDownload;}
    public boolean getPromptExeDownload(){return _promptExeDownload;}
    public int getMaxUploads(){return _maxUploads;}
    public boolean getClearCompletedUpload(){return _clearCompletedUpload;}
    public boolean getClearCompletedDownload(){return _clearCompletedDownload;}
    public int getSearchAnimationTime(){ return _searchAnimationTime; }

    public String getConnectString(){ return _connectString; }
    /** Returns the first word of the connect string.
     *  This is solely a convenience routine. */
    public String getConnectStringFirstWord(){ return _connectStringFirstWord; }
    /** Returns the remaing words of the connect string, without the leading space.
     *  This is solely a convenience routine. */
    public String getConnectStringRemainder(){ return _connectStringRemainder; }
    public String getConnectOkString(){ return _connectOkString; }


    // SPECIALIZED METHODS FOR NETWORK DISCOVERY
    /** returns the Network Discovery specialized properties file */
    public Properties getNDProps(){return _ndProps;}

    /** returns the path of the properties and host list files */
    public String getPath() {return CURRENT_DIRECTORY + File.separator;}

    public int getBasicInfoSizeForQuery() {return _basicQueryInfo;}

    public int getAdvancedInfoSizeForQuery() {return _advancedQueryInfo;}

    public boolean getForceIPAddress() {
        return _forceIPAddress;
    }

    public byte[] getForcedIPAddress() {
        return _forcedIPAddress;
    }

    public String getForcedIPAddressString() {
        return _forcedIPAddressString;
    }

    public int getForcedPort() {
        return _forcedPort;
    }

	/**
	 * returns a boolean indicating whether or not to check again
	 * for LimeWire updates.
	 */
    public boolean getCheckAgain() {
		Boolean b = new Boolean(_props.getProperty(CHECK_AGAIN));
        return b.booleanValue();
    }

    public int getFreeloaderFiles() {
        return _freeLoaderFiles;
    }
    public int getFreeloaderAllowed() {
        return _freeLoaderAllowed;
    }

	/**
	 * returns the average time that the user runs LimeWire.
	 */
	public long getAverageUptime() {
		return _averageUptime;
	}

	/**
	 * returns the total amount of time that this user has run 
	 * LimeWire.
	 */
	public long getTotalUptime() {
		return _totalUptime;
	}

	/**
	 * returns the number of times LimeWire has been run.
	 */
	public int getSessions() {
		return _sessions;
	}

	/** 
	 * returns a boolean indicating whether or not the program 
	 * has been "installed," with the properties set correctly. 
	 */
	public boolean getInstalled() {
		return _installed;
	}

	/**
	 * returns the width that the application should be sized to.
	 */
	public int getAppWidth() {
		return Integer.parseInt(_props.getProperty(APP_WIDTH));
	}

	/**
	 * returns the height that the application should be sized to.
	 */
	public int getAppHeight() {
		return Integer.parseInt(_props.getProperty(APP_HEIGHT));
	}

	/**
	 * returns a boolean specifying whether or not the 
	 * application has been run one time or not.
	 */
	public boolean getRunOnce() {
		Boolean b = Boolean.valueOf(_props.getProperty(RUN_ONCE));
		return b.booleanValue();
	}

	/**
	 * returns an integer value for the x position of the window
	 * set by the user in a previous session.
	 */
	public int getWindowX() {
		return Integer.parseInt(_props.getProperty(WINDOW_X));
	}

	/**
	 * returns an integer value for the y position of the window
	 * set by the user in a previous session.
	 */
	public int getWindowY() {
		return Integer.parseInt(_props.getProperty(WINDOW_Y));
	}

	/**
	 * returns a boolean specifying whether or not the tray
	 * dialog window should be shown.
	 */
	public boolean getShowTrayDialog() {
		Boolean b = Boolean.valueOf(_props.getProperty(SHOW_TRAY_DIALOG));
		return b.booleanValue();	
	}

	/**
	 * returns a boolean specifying whether or not to minimize 
	 * the application to the system tray.
	 */
	public boolean getMinimizeToTray() {
		Boolean b = new Boolean(_props.getProperty(MINIMIZE_TO_TRAY));
		return b.booleanValue();	
	}

	/**
	 * returns true is an incoming connection has ever been established
	 * during a single session
	 */
	public boolean getAcceptedIncoming() {return _acceptedIncoming;}

	/**
	 * returns a boolean specifying whether or not the close
	 * dialog window should be shown.
	 */
	public boolean getShowCloseDialog() {
		Boolean b = Boolean.valueOf(_props.getProperty(SHOW_CLOSE_DIALOG));
		return b.booleanValue();	
	}
  
    /**
     * returns the classpath string used for loading jar files
	 * on startup.
     */
    public String getClassPath() {
        return _props.getProperty(CLASSPATH);
    }

    /**
     * returns the main class to load on startup.
     */
    public String getMainClass() {
        return _props.getProperty(MAIN_CLASS);
    }

    /******************************************************
     **************  END OF ACCESSOR METHODS **************
     ******************************************************/


    /******************************************************
     *************  START OF MUTATOR METHODS **************
     ******************************************************/

	/** 
	 * updates all of the uptime settings based on the
	 * passed in time value for the most recent session. 
	 */
	public void updateUptime(int currentTime) {
		_totalUptime += currentTime;
		_averageUptime = _totalUptime/_sessions;
		setTotalUptime(_totalUptime);
		setAverageUptime(_averageUptime);
	}

	/**  
	 * sets the total number of times limewire has been run -- 
	 * used in calculating the average amount of time this user 
	 * leaves limewire on.
	 */
	private void setSessions(int sessions) {
		if(_sessions < 1)
			_sessions = 10;
		_sessions = sessions;
		String s = Integer.toString(_sessions);
		_props.put(SESSIONS, s);
	}

	/** 
	 * sets the average time this user leaves LimeWire running.
	 */
	private void setAverageUptime(long averageUptime) {
		_averageUptime = averageUptime;
		String s = Long.toString(_averageUptime);
		_props.put(AVERAGE_UPTIME, s);
	}

	/** 
	 * sets the total time this user has used LimeWire
	 */
	private void setTotalUptime(long totalUptime) {
		_totalUptime = totalUptime;
		String s = Long.toString(_totalUptime);
		_props.put(TOTAL_UPTIME, s);
	}

    /** 
	 * sets the maximum length of packets (spam protection)
	 */
    public void setMaxLength(int maxLength) {
		_maxLength = maxLength;
		String s = Integer.toString(_maxLength);
		_props.put(MAX_LENGTH, s);        
    }

    /** 
	 * sets the timeout 
	 */
    public void setTimeout(int timeout) {
		_timeout = timeout;
		String s = Integer.toString(_timeout);
		_props.put(TIMEOUT, s);        
    }

    /**
     * Sets the keepAlive without checking the maximum value.
     * Throws IllegalArgumentException if keepAlive is negative.
     */
    public void setKeepAlive(int keepAlive)
        throws IllegalArgumentException {
        try {
            setKeepAlive(keepAlive, false);
        } catch (BadConnectionSettingException e) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Sets the keep alive. If keepAlive is negative, throws
     * BadConnectionSettingException with a suggested value of 0.
     *
     * If checkLimit is true, BadConnectionSettingException is thrown if
     * keepAlive is too large for the current connection speed.  The suggestions
     * are not necessarily guaranteed to be valid however.
     */
    public void setKeepAlive(int keepAlive, boolean checkLimit)
        throws BadConnectionSettingException {
        int incoming=getMaxIncomingConnections();
        if (checkLimit) {
            int max=maxConnections();
            //Too high for this connection speed?  Decrease it.
            if (keepAlive > max) {
                throw new BadConnectionSettingException(
                    BadConnectionSettingException.TOO_HIGH_FOR_SPEED,
                    max, maxConnections());
            }
        }

        if (keepAlive<0) {
            throw new BadConnectionSettingException(
                BadConnectionSettingException.NEGATIVE_VALUE,
                0, getMaxIncomingConnections());
        } else {
            _keepAlive = keepAlive;
            String s = Integer.toString(_keepAlive);
            _props.put(KEEP_ALIVE, s);
        }
    }

    /** 
	 * Returns the maximum number of connections for the given connection
     * speed.  
	 */
    private int maxConnections() {
        int speed=getConnectionSpeed();
        //I'm copying these numbers out of GUIStyles.  I don't want this to
        //depend on GUI code, though.
        if (speed<=56)    //modems
            return 3;
        else if (speed<=350)  //cable
            return 6;
        else if (speed<=1000) //T1
            return 10;
        else                  //T3: no limit
            return Integer.MAX_VALUE;
    }


    /** 
	 * sets the limit for the number of searches
     * throws an exception on negative limits
     * and limits of 10,000 or more 
	 */
    public void setSearchLimit(byte limit) {
        if(limit < 0 || limit > 10000)
            throw new IllegalArgumentException();
        else {
            _searchLimit = limit;
            String s = Byte.toString(_searchLimit);
            _props.put(SEARCH_LIMIT, s);
        }
    }

    /** sets the client (gu) ID number */
    public void setClientID(String clientID) {
		_clientID = clientID;
		_props.put(CLIENT_ID, _clientID);
    }

    /**
     * Sets the max number of incoming connections without checking the maximum
     * value. Throws IllegalArgumentException if maxConn is negative.
     */
    public void setMaxIncomingConnections(int maxConn)
        throws IllegalArgumentException {
        try {
            setMaxIncomingConnections(maxConn, false);
        } catch (BadConnectionSettingException e) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Sets the maximum number of incoming connections (incoming and
     * outgoing). If maxConn is negative, throws
     * BadConnectionSettingException with a suggested value of 0.
     *
     * If checkLimit is true, then if keepAlive is too large for the current
     * connection speed or too small for the current number of outgoing
     * connections, throws BadConnectionSettingException with suggested new
     * values.  The suggestions attempt to set MAX_INCOMING_CONNECTIONS to
     * maxConn, even if that means adjusting the KEEP_ALIVE.  The suggestions are
     * not necessarily guaranteed to be valid however.
     */
    public void setMaxIncomingConnections(int maxConn,
										  boolean checkLimit)
        throws BadConnectionSettingException {
        if (checkLimit) {
            int max=maxConnections();
            //Too high for this connection speed?  Decrease it.
            if (maxConn > max) {
                throw new BadConnectionSettingException(
                    BadConnectionSettingException.TOO_HIGH_FOR_SPEED,
                    getKeepAlive(), max);
            }
        }

        if(maxConn < 0) {
            throw new BadConnectionSettingException(
                BadConnectionSettingException.NEGATIVE_VALUE,
                getKeepAlive(), 0);
        } else {
            _maxIncomingConn = maxConn;
            String s = Integer.toString(maxConn);
            _props.put(MAX_INCOMING_CONNECTIONS, s);
        }
    }

    /** 
	 * sets the hard maximum time to live 
	 */
    public void setMaxTTL(byte maxttl) throws IllegalArgumentException {
        if(maxttl < 0 || maxttl > 50)
            throw new IllegalArgumentException();
        else {
            _maxttl = maxttl;
            String s = Byte.toString(_maxttl);
            _props.put(MAX_TTL, s);
        }
    }

    public void setBasicInfoForQuery(int basicInfo) {
        _basicQueryInfo = basicInfo;
        String s = Integer.toString(basicInfo);
        _props.put(BASIC_QUERY_INFO, s);
    }

	public void setUploadsPerPerson(int uploads) {
		_uploadsPerPerson = uploads;
		String s = Integer.toString(uploads);
        _props.put(UPLOADS_PER_PERSON , s);
	}


    public void setAdvancedInfoForQuery(int advancedInfo) {
        _advancedQueryInfo = advancedInfo;
        String s = Integer.toString(advancedInfo);
        _props.put(ADVANCED_QUERY_INFO, s);
    }


    /******************************************************
     *********  START OF CONFIGURATION SETTINGS ***********
     ******************************************************/

    /** 
	 * Sets the directory for saving files. 
	 *
	 * @param   saveDir  A <code>File</code> instance denoting the 
	 *                   abstract pathname of the directory for
	 *                   saving files.  
	 *
	 * @throws  IOException 
	 *          If the directory denoted by the directory pathname
	 *          String parameter did not exist prior to this method
	 *          call and could not be created, or if the canonical
	 *          path could not be retrieved from the file system.
	 * 
	 * @throws  NullPointerException 
	 *          If the "dir" parameter is null.
	 */
    public void setSaveDirectory(File saveDir) throws IOException {
		if(saveDir == null) throw new NullPointerException();
		String parentDir = saveDir.getParent();

		File incDir = new File(parentDir, "Incomplete");
		if(!saveDir.isDirectory()) {
			if(!saveDir.mkdirs()) throw new IOException();
		}

		if(!incDir.isDirectory()) {
			if(!incDir.mkdirs()) throw new IOException();
		}
		_saveDirectory       = saveDir;
		_incompleteDirectory = incDir;
		
		_props.put(SAVE_DIRECTORY, saveDir.getAbsolutePath());
    }

    /** 
	 * Sets the shared directories.  This method filters
     * out any duplicate or invalid directories in the string.
     * Note, however, that it does not currently filter out
     * listing subdirectories that have parent directories
     * also in the string. 
	 *
	 * @param dirs A semicolon delimited <code>String</code> instance 
	 *             containing the paths of shared directories.
	 */
    public void setDirectories(final String dirs) {
        boolean dirsModified = false;
		
		// this is necessary because the getDirectoriesAsArray
		// method creates its array from the _directories variable.
		_directories = dirs;
        String[] dirArray = getDirectoriesAsArray();
        int i = 0;
        while(i < dirArray.length) {
            if(dirArray[i] != null) {
                File f = new File(dirArray[i]);
                if(f.isDirectory()) {
                    int count = 0;
                    int z = 0;
                    String str = "";
                    try {str = f.getCanonicalPath();}
                    catch(IOException ioe) {break;}
                    while(z < dirArray.length) {
                        if(dirArray[z] != null) {
                            File file = new File(dirArray[z]);
                            String name = "";
                            try {name = file.getCanonicalPath();}
                            catch(IOException ioe) {break;}
                            if(str.equals(name)) {
                                count++;
                                if(count > 1) {
                                    dirArray[z] = null;
                                    dirsModified = true;
                                }
                            }
                        }
                        z++;
                    }
                }
                else {
                    dirArray[i] = null;
                    dirsModified = true;
                }
            }
            i++;
        }
        if(dirsModified) {
            i = 0;
            StringBuffer sb = new StringBuffer();
            while(i < dirArray.length) {
                if(dirArray[i] != null) {
                    sb.append(dirArray[i]);
                    sb.append(';');
                }
                i++;
            }
            _directories = sb.toString();
        }
        _props.put(DIRECTORIES, _directories);
    }

    /** 
	 * Adds one directory to the directory string only if
     * it is a directory and is not already listed. 
	 *
	 * @param dir  A <code>File</code> instance denoting the 
	 *             abstract pathname of the new directory 
	 *             to add.
	 *
	 * @throws  IOException 
	 *          If the directory denoted by the directory pathname
	 *          String parameter did not exist prior to this method
	 *          call and could not be created, or if the canonical
	 *          path could not be retrieved from the file system.
	 */
    public void addDirectory(File dir) throws IOException {
		if(!dir.isDirectory()) throw new IOException();
		String[] dirs = getDirectoriesAsArray();
		String newPath = "";
		newPath = dir.getCanonicalPath();
		int i = 0;
		while(i < dirs.length) {
			File file = new File(dirs[i]);
			String name = "";
			name = file.getCanonicalPath();
			if(name.equals(newPath)) {
				// throw the exception because the shared 
				// directory already exists
				throw new IOException();
			}
			i++;
		}
		if(!_directories.endsWith(";"))
			_directories += ";";
		_directories += newPath;
		_directories += ";";
		_props.put(DIRECTORIES, _directories);
    }

    /** 
	 * set the extensions to search for 
	 */
    public void setExtensions(String ext) {
        _extensions = ext;
        _props.put(EXTENSIONS, ext);
    }

    /** 
	 * sets the time to live 
	 */
    public void setTTL(byte ttl) {
        if (ttl < 1 || ttl > 14)
            throw new IllegalArgumentException();
        else {
            _ttl = ttl;
            String s = Byte.toString(_ttl);
            _props.put(TTL, s);
        }
    }

    /** 
	 * sets the soft maximum time to live 
	 */
    public void setSoftMaxTTL(byte softmaxttl) {
        if (softmaxttl < 0 || softmaxttl > 14)
            throw new IllegalArgumentException();
        else {
            _softmaxttl = softmaxttl;
            String s = Byte.toString(softmaxttl);
            _props.put(SOFT_MAX_TTL, s);
        }
    }

    /** 
	 * sets the port to connect on 
	 */
    public void setPort(int port) {
        // if the entered port is outside accepted
        // port numbers, throw the exception
        if(port > 65536 || port < 0)
            throw new IllegalArgumentException();
        else {
            _port = port;
            String s = Integer.toString(_port);
            _props.put(PORT, s);
        }
    }

    /** 
	 * sets the connection speed.  throws an
     * exception if you try to set the speed
     * far faster than a T3 line or less than
     * 0.
	 */
    public void setConnectionSpeed(int speed) {
        if(speed < 0 || speed > 20000)
            throw new IllegalArgumentException();
        else {
            _connectionSpeed = speed;
            String s = Integer.toString(_connectionSpeed);
            _props.put(SPEED, s);
        }
    }

    /** 
	 * Sets the percentage of total bandwidth (as given by
     * CONNECTION_SPEED) to use for uploads.  This is shared
     * equally among all uploads.  Throws IllegalArgumentException
     * if speed<0 or speed>100. 
	 */
    public void setUploadSpeed(int speed) {
        if (speed<0 || speed>100)
            throw new IllegalArgumentException();
        else {
            _uploadSpeed = speed;
            String s = Integer.toString(_uploadSpeed);
            _props.put(UPLOAD_SPEED, s);
        }
    }

    public void setConnectString(String connect)
        throws IllegalArgumentException {
        int i=connect.indexOf(" ");
        String firstWord;
        String remainder;

        if (connect.length()<1)
            throw new IllegalArgumentException();

        //No space in connect or (first) space is last is problematic.
        if (i==-1 || i==(connect.length()-1)) {
            throw new IllegalArgumentException();
        }

        firstWord=connect.substring(0,i);
        remainder=connect.substring(i+1);


        //Disallow GIV and GET.  Also disallow other HTTP methods
        //in case we want them in the future.
        String uppered=firstWord.toUpperCase();
        if (uppered.equals("GIV")
            || uppered.equals("GET")
            || uppered.equals("PUT")
            || uppered.equals("POST")
            || uppered.equals("HEAD")
            || uppered.equals("DELETE")) {
            throw new IllegalArgumentException();
        }

        //Everything ok.
        _connectString = connect;
        _connectStringFirstWord = firstWord;
        _connectStringRemainder = remainder;

        _props.put(CONNECT_STRING, connect);
    }

    public void setConnectOkString(String ok)
        throws IllegalArgumentException {
        if (ok.length()<1)
            throw new IllegalArgumentException();

        _connectOkString = ok;
        _props.put(CONNECT_OK_STRING, ok);
    }

    public void setParallelSearchMax(int max) {
        if(max<1)
            throw new IllegalArgumentException();
        else {
            _parallelSearchMax = max;
            String s = String.valueOf(max);
            _props.put(PARALLEL_SEARCH, s);
        }
    }

    public void setMaxSimDownload(int max) {
		_maxSimDownload = max;
		String s = String.valueOf(max);
		_props.put(MAX_SIM_DOWNLOAD, s);        
    }


    public void setPromptExeDownload(boolean prompt) {        
        _promptExeDownload = prompt;
        String s = String.valueOf(prompt);
        _props.put(PROMPT_EXE_DOWNLOAD, s);
    }

	public void setChatEnabled(boolean chatEnabled) {
		_chatEnabled = chatEnabled;
		String s = String.valueOf(chatEnabled);
		_props.put(CHAT_ENABLED, s);
	}


    public void setMaxUploads(int max) {
		_maxUploads = max;
		String s = String.valueOf(max);
		_props.put(MAX_UPLOADS, s);
    }

    public void setClearCompletedUpload(boolean b) {
		_clearCompletedUpload = b;
		String s = String.valueOf(b);
		_props.put(CLEAR_UPLOAD, s);
    }

    public void setClearCompletedDownload(boolean b) {
		_clearCompletedDownload = b;
		String s = String.valueOf(b);
		_props.put(CLEAR_DOWNLOAD, s);
    }

    public void setForceIPAddress(boolean force) {
        String c;
        if (force == true)
            c = "true";
        else
            c = "false";
        _forceIPAddress = force;
        _props.put(FORCE_IP_ADDRESS, c);
    }


    public void setAllowBrowser(boolean allow) {
        String c;
        if (allow == true)
            c = "true";
        else
            c = "false";
        _allowBroswer = allow;
        _props.put(ALLOW_BROWSER, c);
    }

    public void setForcedIPAddress(byte[] address) {
        _forcedIPAddress = address;
        _props.put(FORCED_IP_ADDRESS, new String(address));
    }

    public void setForcedIPAddressString(String address) {
        _forcedIPAddressString = address;
        _props.put(FORCED_IP_ADDRESS_STRING, address);
    }

    public void setForcedPort(int port) {
        // if the entered port is outside accepted
        // port numbers, throw the exception
        if(port > 65536 || port < 1)
            throw new IllegalArgumentException();
        else {
            _forcedPort = port;
            String s = Integer.toString(_forcedPort);
            _props.put(FORCED_PORT, s);
        }
    }
	
	/** set whether or not the program has been installed */
	public void setInstalled(boolean installed) {        
        _installed = installed;
        String s = String.valueOf(installed);
        _props.put(INSTALLED, s);
    }

    /******************************************************
     *********  END OF CONFIGURATION SETTINGS *************
     ******************************************************/

    public void setBannedIps(String[] bannedIps) {
        if(bannedIps == null)
            throw new IllegalArgumentException();
        else {
            _bannedIps = bannedIps;
            _props.put(BANNED_IPS,
                       encode(bannedIps));
        }
    }

    public void setBannedWords(String[] bannedWords) {
        if(bannedWords == null)
            throw new IllegalArgumentException();
        else {
            _bannedWords = bannedWords;
            _props.put(BANNED_WORDS,
                       encode(bannedWords));
        }
    }

    public void setFilterAdult(boolean filterAdult) {
		_filterAdult = filterAdult;
		Boolean b = new Boolean(filterAdult);
		String s = b.toString();
		_props.put(FILTER_ADULT, s);
    }

    public void setFilterDuplicates(boolean filterDuplicates) {
		_filterDuplicates = filterDuplicates;
		Boolean b = new Boolean(filterDuplicates);
		String s = b.toString();
		_props.put(FILTER_DUPLICATES, s);
    }

    public void setFilterHtml(boolean filterHtml) {
		_filterHtml = filterHtml;
		Boolean b = new Boolean(filterHtml);
		String s = b.toString();
		_props.put(FILTER_HTML, s);
    }

    public void setFilterVbs(boolean filterVbs) {
		_filterVbs = filterVbs;
		Boolean b = new Boolean(filterVbs);
		String s = b.toString();
		_props.put(FILTER_VBS, s);
    }

    public void setFilterGreedyQueries(boolean yes) {
        _filterGreedyQueries = yes;
        Boolean b = new Boolean(yes);
        String s = b.toString();
        _props.put(FILTER_GREEDY_QUERIES, s);
    }


    public void setFilterBearShareQueries(boolean yes) {
        _filterBearShare = yes;
        Boolean b = new Boolean(yes);
        String s = b.toString();
        _props.put(FILTER_BEARSHARE_QUERIES, s);
    }

    public void setUseQuickConnect(boolean useQuickConnect) {
		_useQuickConnect = useQuickConnect;
		Boolean b = new Boolean(useQuickConnect);
		String s = b.toString();
		_props.put(USE_QUICK_CONNECT, s);
    }

    public void setQuickConnectHosts(String[] hosts) {
        if(hosts == null)
            throw new IllegalArgumentException();
        else {
            _quickConnectHosts = hosts;
            _props.put(QUICK_CONNECT_HOSTS,
                       encode(hosts));
        }
    }


    public void setSearchAnimationTime(int seconds) {
        if(seconds < 0)
            throw new IllegalArgumentException();
        else {
            _searchAnimationTime = seconds;
            String s = Integer.toString(seconds);
            _props.put(SEARCH_ANIMATION_TIME, s);
        }
    }

    /**
     * Sets the probability (expressed as a percentage) that an incoming
     * freeloader will be accepted.   For example, if allowed==50, an incoming
     * connection has a 50-50 chance being accepted.  If allowed==100, all
     * incoming connections are accepted.  Throws IllegalArgumentException if
     * allowed<0 or allowed>100.
     */
    public void setFreeloaderAllowed(int allowed)
        throws IllegalArgumentException
    {
        if (allowed>100 || allowed<0)
            throw new IllegalArgumentException();
        _freeLoaderAllowed = allowed;
        String s = Integer.toString(allowed);
        _props.put(FREELOADER_ALLOWED, s);
    }

    /**
     * Sets minimum the number of files a host must share to not be considered a
     * freeloader.  For example, if files==0, no host is considered a
     * freeloader.  Throws IllegalArgumentException if files<0.
     */
    public void setFreeloaderFiles(int files) 
		throws IllegalArgumentException {
        if (files<0)
            throw new IllegalArgumentException();
        _freeLoaderFiles = files;
        String s = Integer.toString(files);
        _props.put(FREELOADER_FILES, s);
    }

	
	/**
	 * sets the boolean for whether or not we should check again for an update.
	 */
    public void setCheckAgain(boolean check) {
		Boolean b = new Boolean(check);
        _props.put(CHECK_AGAIN, b.toString());
    }

	/**
	 * sets the width that the application should be.
	 */
	public void setAppWidth(int width) {
        String s = Integer.toString(width);
		_props.put(APP_WIDTH, s);
	}

	/**
	 * sets the height that the application should be.
	 * @requires the height must be greater than zero.
	 */
	public void setAppHeight(int height) {
        String s = Integer.toString(height);
		_props.put(APP_HEIGHT, s);
	}

	/**
	 * sets the flag for whether or not the application
	 * has been run one time before this.
	 */
	public void setRunOnce(boolean runOnce) {
		Boolean b = new Boolean(runOnce);
		_props.put(RUN_ONCE, b.toString());
	}

	/**
	 * set the x position of the window for the next
	 * time the application is started.
	 */
	public void setWindowX(int x) {
		_props.put(WINDOW_X, Integer.toString(x));
	}

	/**
	 * set the y position of the window for the next
	 * time the application is started.
	 */
	public void setWindowY(int y) {
		_props.put(WINDOW_Y, Integer.toString(y));
	}

	/**
	 * sets the flag for whether or not the tray dialog
	 * window should be shown.
	 */
	public void setShowTrayDialog(boolean showDialog) {
		Boolean b = new Boolean(showDialog);
		_props.put(SHOW_TRAY_DIALOG, b.toString());
	}

	/**
	 * sets the flag for whether or not the application
	 * should be minimized to the system tray on windows
	 */
	public void setMinimizeToTray(boolean minimize) {
		Boolean b = new Boolean(minimize);
		_props.put(MINIMIZE_TO_TRAY, b.toString());
	}	

	/**
	 * sets whether or not the application has accepted an incoming
	 * connection during this session.
	 */
	public void setAcceptedIncoming(boolean incoming) {
		_acceptedIncoming = incoming;
    }

	/**
	 * sets the flag for whether or not the close dialog
	 * window should be shown.
	 */
	public void setShowCloseDialog(boolean showDialog) {
		Boolean b = new Boolean(showDialog);
		_props.put(SHOW_CLOSE_DIALOG, b.toString());
	}
    
    /**
     * sets the classpath for loading files at startup.
     */
    public void setClassPath(String classpath) {
        _props.put(CLASSPATH, classpath);
    }

    /**
     * sets the main class to use at startup.
     */
    public void setMainClass(String mainClass) {
        _props.put(MAIN_CLASS, mainClass);
    }
	
    /******************************************************
     ***************  END OF MUTATOR METHODS **************
     ******************************************************/


    /** 
	 * writes out the properties file to with the specified
     * name in the user's install directory.  This should only
	 * get called once when the program shuts down.
     */
    public void writeProperties() {
		FileOutputStream ostream = null;
		try {
			ostream = new FileOutputStream(new File(PROPS_NAME));
			_props.save(ostream, "");
			ostream.close();
		}
		catch (Exception e){}
		finally {
			try {
				if(ostream != null) ostream.close();
			}
			catch(IOException io) {}
		}
	}

    private static final String STRING_DELIMETER=";";

    /**  Returns a string encoding of array. Inverse of decode. */
    private static String encode(String[] array) {
        //TODO1: ";" ==> "\;"
        StringBuffer buf=new StringBuffer();
        for (int i=0; i<(array.length-1); i++) { //don't put ";" after last word
            buf.append(array[i]);
            buf.append(STRING_DELIMETER);
        }
        if (array.length!=0)
            buf.append(array[array.length-1]); //add last word
        return buf.toString();
    }

    /** Returns the array encoded in s.  Inverse of encode. */
    private static String[] decode(String s) {
        //TODO1: "\;" ==> ";"
        StringTokenizer lexer=new StringTokenizer(s,STRING_DELIMETER);
        Vector buf=new Vector();
        while (lexer.hasMoreTokens())
            buf.add(lexer.nextToken());
        String[] ret=new String[buf.size()];
        buf.copyInto(ret);
        return ret;
    }
    
	// unit tests
//  	public static void main(String args[]) {
//  		SettingsManager settings = SettingsManager.instance();
//  		String incDir  = settings.getIncompleteDirectory();
//  		String saveDir = settings.getSaveDirectory();
//  		String saveDefaultDir = settings.getSaveDefault();

//  		File incFile = new File(incDir);
//  		File saveFile = new File(saveDir);
//  		File saveDefaultFile = new File(saveDefaultDir);
//  		System.out.println("incDir:         "+incDir);
//  		System.out.println("saveDir:        "+saveDir);
//  		System.out.println("saveDefaultDir: "+saveDefaultDir);		
//  		System.out.println("incDir isDirectory():         "+incFile.isDirectory());
//  		System.out.println("saveDir isDirectory():        "+saveFile.isDirectory());
//  		System.out.println("saveDefaultDir isDirectory(): "+saveDefaultFile.isDirectory());
//  		System.out.println("host list path: " + settings.getHostList());
//  	}
	
//  	// test verifying that the limewire.lax file is actually a Java Properties file.
//  	public static void main(String[] args) {
//  		Properties props = new Properties();
//  		FileInputStream fis;
//  		try {
//  			fis = new FileInputStream(new File("limewire.lax"));
//  			props.load(fis);
//  			Enumeration enum = props.propertyNames();
//  			String key = "";
//  			String value = "";
//  			while(enum.hasMoreElements()) {
//  				key = (String)enum.nextElement();
//  				System.out.print(key);
//  				System.out.print("="+props.getProperty(key));
//  				System.out.println();
//  			}
//  		} catch(FileNotFoundException fnfe) {
//  		} catch(SecurityException se) {
//  		} catch(IOException ioe) {
//  		}
		
//  	}
	
//  	public static void main(String args[]) {
//          boolean installed = true;
//          String s = String.valueOf(installed);
//  		System.out.println("string: "+s);

		
//  		Boolean b = new Boolean(s);
//  		System.out.println("boolean: "+b.booleanValue());
//          //_props.put(INSTALLED, s);
//  	}

    //      /** Unit test */
    //      public static void main(String args[]) {
    //      String[] original=new String[] {"a", "bee", "see"};
    //      String encoded=encode(original);
    //      String[] decoded=decode(encoded);
    //      Assert.that(Arrays.equals(original, decoded));

    //      original=new String[] {"a"};
    //      encoded=encode(original);
    //      decoded=decode(encoded);
    //      Assert.that(Arrays.equals(original, decoded));

    //      original=new String[] {};
    //      encoded=encode(original);
    //      decoded=decode(encoded);
    //      Assert.that(Arrays.equals(original, decoded));

    //      SettingsManager manager=SettingsManager.instance();
    //      manager.setConnectString("TEST STRING");
    //      Assert.that(manager.getConnectString().equals("TEST STRING"));
    //      Assert.that(manager.getConnectStringFirstWord().equals("TEST"));
    //      Assert.that(manager.getConnectStringRemainder().equals("STRING"));

    //      manager.setConnectString("TEST");
    //      Assert.that(manager.getConnectString().equals("TEST"));
    //      Assert.that(manager.getConnectStringFirstWord().equals("TEST"));
    //      Assert.that(manager.getConnectStringRemainder().equals(""));
    //      }

    // test for setDirectories method
//      public static void main(String args[]) {
//          System.out.println("_directories: "+ _directories);
//          SettingsManager settings = SettingsManager.instance();
//          System.out.println("_directories: "+ _directories);
//          settings.setDirectories("c:\\p;c:\\p;c:\\pC:\\My Music;C:\\Program Files;"+
//                                  "C:\\Program Files\\LimeWire;"+
//                                  "C:\\Program Files\\LimeWire;C:\\Program Files;C:\\My Music;"+
//                                  "c:\\My Music;c:\\Program Files\\Direct;"+
//                                  "C:\\Program Files\\direct\\;C:\\ProgramFiles");
//          System.out.println("_directories: "+ _directories);
//      }

}
