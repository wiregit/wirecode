package com.limegroup.gnutella;

import java.io.*;
import java.util.Properties;
import com.sun.java.util.collections.*;
import java.lang.IllegalArgumentException;
import java.util.Enumeration;
import java.util.StringTokenizer;


/**
 *  This class manages the property settings.  It maintains
 *  default settings for values not set in the saved
 *  settings files and updates those settings based on user
 *  input, checking for errors where appropriate.  It also
 *  saves the settings file to disk when the session
 *  terminates.
 *
 * @author Adam Fisk
 */

public class SettingsManager implements SettingsInterface
{
	private static boolean forceIPAdress_;
	private static String forcedIPAdress_;
	private static int forcedPort_;

	/** lastVersionChecked is the most recent version number checked.  Also,
	 * there is a boolean for don't check again. */
    private static final String CURRENT_VERSION=DEFAULT_LAST_VERSION_CHECKED;
	private String lastVersionChecked_;
	private boolean checkAgain_;

    boolean write_ = false;
    /** Variables for the various settings */
    private static byte     ttl_;
    private static byte     softmaxttl_;
    private static byte     maxttl_;
    private static int      maxLength_;
    private static int      timeout_;
    private static String   hostList_;
    private static int      keepAlive_;
    private static int      port_;
    private static int      connectionSpeed_;
    private static int      uploadSpeed_;
    private static byte     searchLimit_;
    private static String   clientID_;
    private static int      maxIncomingConn_;
    private static int      localIP_;  // not yet implemented
    private static String   saveDirectory_;
    private static String   directories_;
    private static String   extensions_;
    private static String   incompleteDirectory_;
    private static String[] bannedIps_;
    private static String[] bannedWords_;
    private static boolean  filterDuplicates_;
    private static boolean  filterAdult_;
    private static boolean  filterVbs_;
    private static boolean  filterHtml_;
    private static boolean  filterGreedyQueries_;
    private static boolean  useQuickConnect_;
    private static String[] quickConnectHosts_;
    private static int      parallelSearchMax_;
    private static boolean  clearCompletedUpload_;
    private static boolean  clearCompletedDownload_;
    private static int      maxSimDownload_;
    private static int      maxUploads_;	

    private static int      searchAnimationTime_;
    private static String   saveDefault_;

    /** connectString_ is something like "GNUTELLA CONNECT..."
     *  connectStringOk_ is something like "GNUTELLA OK..."
     *  INVARIANT: connectString_=connectStringFirstWord_+" "+connectStringRemainder_
     *             connectString!=""
     *             connectStringFirstWord does not contain spaces
     */
    private static String   connectString_;
    private static String   connectStringFirstWord_;
    private static String   connectStringRemainder_;
    private static String   connectOkString_;
	private static int      basicQueryInfo_;
	private static int      advancedQueryInfo_;
    private static int      freeLoaderFiles_;
    private static int      freeLoaderAllowed_;

    /** Set up a local variable for the properties */
    private static Properties props_;

    /** Specialized properties file for the
     *  network discoverer
     */
    private static Properties ndProps_;

    /**
     *  Set up the manager instance to follow the
     *  singleton pattern.
     */
    private static SettingsManager instance_=new SettingsManager();

    /** a string for the file separator */
    private String fileSep_;

    private String home_;
    private String fileName_;
    private String ndFileName_;
    private String saveShareDir_;

    /**
     * This method provides the only access
     * to an instance of this class in
     * accordance with the singleton pattern
     */
    public static SettingsManager instance() {
        return instance_;
    }

    /** The constructor is private to ensure
     *  that only one copy will be created
     */
    private SettingsManager() {
        props_      = new Properties();
        ndProps_    = new Properties();
        home_       = System.getProperty("user.dir");
        home_       += File.separator;
        fileName_   = home_;
        ndFileName_ = home_;
        fileName_   += SettingsInterface.DEFAULT_FILE_NAME;
        ndFileName_ += SettingsInterface.DEFAULT_ND_PROPS_NAME;
        FileInputStream fis;
        try {
            fis = new FileInputStream(ndFileName_);
            try {ndProps_.load(fis);}
            catch(IOException ioe) {}
        }
        catch(FileNotFoundException fne){}
        catch(SecurityException se) {}
        initSettings();
    }


    /** Check the properties file and set the props */
    private void initSettings() {
        Properties tempProps = new Properties();
        FileInputStream fis;
        try {
            fis = new FileInputStream(fileName_);
            try {
                tempProps.load(fis);
                loadDefaults();
                try {
                    fis.close();
                    validateFile(tempProps);
                }
                catch(IOException e){loadDefaults();}
            }
            catch(IOException e){loadDefaults();}
        }
        catch(FileNotFoundException fnfe){loadDefaults();}
        catch(SecurityException se){loadDefaults();}
    }

    /** Makes sure that each property in the file
     *  is valid.  If not, it sets that property
     *  to the default value.
     */
    private void validateFile(Properties tempProps)
        throws IOException {
        write_ = false;
        String p;
        Enumeration enum = tempProps.propertyNames();
        while(enum.hasMoreElements()){
            String key;
            try {
                key = (String)enum.nextElement();
                p = tempProps.getProperty(key);
                if(key.equals(SettingsInterface.TTL)) {
					setTTL(Byte.parseByte(p));
                }
                else if(key.equals(SettingsInterface.SOFT_MAX_TTL)) {
					setSoftMaxTTL(Byte.parseByte(p));
				}
                else if(key.equals(SettingsInterface.MAX_TTL)) {
					setMaxTTL(Byte.parseByte(p));
                }
                else if(key.equals(SettingsInterface.MAX_LENGTH)) {
					setMaxLength(Integer.parseInt(p));
                }
                else if(key.equals(SettingsInterface.PARALLEL_SEARCH)) {
					setParallelSearchMax(Integer.parseInt(p)); 
                }
                else if(key.equals(SettingsInterface.MAX_SIM_DOWNLOAD)) {					
					setMaxSimDownload(Integer.parseInt(p)); 
                }
                else if(key.equals(SettingsInterface.MAX_UPLOADS)) {
					setMaxUploads(Integer.parseInt(p)); 
                }
                else if(key.equals(SettingsInterface.CLEAR_DOWNLOAD)) {
                    boolean bs;
                    if (p.equals("true"))
                        bs=true;
                    else if (p.equals("false"))
                        bs=false;
                    else
                        return;
                    setClearCompletedDownload(bs); 
                }
                else if(key.equals(SettingsInterface.CLEAR_UPLOAD)) {
                    boolean bs;
                    if (p.equals("true"))
                        bs=true;
                    else if (p.equals("false"))
                        bs=false;
                    else
                        return;
                    setClearCompletedUpload(bs); 
                }
                else if(key.equals(SettingsInterface.TIMEOUT)) {
					setTimeout(Integer.parseInt(p));
                }
                else if(key.equals(SettingsInterface.KEEP_ALIVE)) {
					//Verified for real later.  See note below.
					setKeepAlive(Integer.parseInt(p));
                }
                else if(key.equals(SettingsInterface.PORT)) {
					setPort(Integer.parseInt(p));
                }
                else if(key.equals(SettingsInterface.SPEED)) {
					setConnectionSpeed(Integer.parseInt(p));
                }
                else if(key.equals(SettingsInterface.UPLOAD_SPEED)) {
					setUploadSpeed(Integer.parseInt(p));
                }
                else if(key.equals(SettingsInterface.SEARCH_LIMIT)) {
					setSearchLimit(Byte.parseByte(p));
                }

                else if(key.equals(SettingsInterface.CLIENT_ID)) {
                    setClientID(p);
                }

                else if(key.equals(SettingsInterface.MAX_INCOMING_CONNECTIONS)) {
					//Verified for real later.  See note below.
					setMaxIncomingConnections(Integer.parseInt(p));
                }

                else if(key.equals(SettingsInterface.SAVE_DIRECTORY)) {
                    setSaveDirectory(p);
                }

                else if(key.equals(SettingsInterface.DIRECTORIES)) {
                    setDirectories(p);
                }

                else if(key.equals(SettingsInterface.EXTENSIONS)) {
                    setExtensions(p);
                }
				else if(key.equals(SettingsInterface.LAST_VERSION_CHECKED)) {
                    setLastVersionChecked(p);
                }
				else if(key.equals(SettingsInterface.CHECK_AGAIN)) {
                    boolean bs;
                    if (p.equals("true"))
                        bs=true;
                    else if (p.equals("false"))
                        bs=false;
                    else
                        return;
                    setCheckAgain(bs); 
                }

                else if(key.equals(SettingsInterface.INCOMPLETE_DIR)) {
                    setIncompleteDirectory(p);
                }
                else if(key.equals(SettingsInterface.BANNED_IPS)) {
                    setBannedIps(decode(p));
                }
                else if(key.equals(SettingsInterface.BANNED_WORDS)) {
                    setBannedWords(decode(p));
                }
                else if(key.equals(SettingsInterface.FILTER_ADULT)) {
                    boolean bs;
                    if (p.equals("true"))
                        bs=true;
                    else if (p.equals("false"))
                        bs=false;
                    else
                        return;
                    setFilterAdult(bs);
                }
                else if(key.equals(SettingsInterface.FILTER_DUPLICATES)) {
                    boolean bs;
                    if (p.equals("true"))
                        bs=true;
                    else if (p.equals("false"))
                        bs=false;
                    else
                        return;
                    setFilterDuplicates(bs);
                }
                else if(key.equals(SettingsInterface.FILTER_HTML)) {
                    boolean bs;
                    if (p.equals("true"))
                        bs=true;
                    else if (p.equals("false"))
                        bs=false;
                    else
                        return;
                    setFilterHtml(bs);
                }
                else if(key.equals(SettingsInterface.FILTER_VBS)) {
                    boolean bs;
                    if (p.equals("true"))
                        bs=true;
                    else if (p.equals("false"))
                        bs=false;
                    else
                        return;
                    setFilterVbs(bs);
                }
                else if(key.equals(SettingsInterface.FILTER_GREEDY_QUERIES)) {
                    boolean bs;
                    if (p.equals("true"))
                        bs=true;
                    else if (p.equals("false"))
                        bs=false;
                    else
                        return;
                    setFilterGreedyQueries(bs);
                }
                else if(key.equals(SettingsInterface.USE_QUICK_CONNECT)) {
                    boolean bs;
                    if (p.equals("true"))
                        bs=true;
                    else if (p.equals("false"))
                        bs=false;
                    else
                        return;
                    setUseQuickConnect(bs);
                }
                else if(key.equals(SettingsInterface.QUICK_CONNECT_HOSTS)) {
                    setQuickConnectHosts(decode(p));
                }
                else if(key.equals(SettingsInterface.SEARCH_ANIMATION_TIME)) {
					setSearchAnimationTime(Integer.parseInt(p));
                }
                else if(key.equals(SettingsInterface.SAVE_DEFAULT)){
                    setSaveDefault(p);
                }
                else if(key.equals(SettingsInterface.CONNECT_STRING)) {
                    setConnectString(p);
                }
                else if(key.equals(SettingsInterface.CONNECT_OK_STRING)){
                    setConnectOkString(p);
                }

                else if(key.equals(SettingsInterface.BASIC_QUERY_INFO)){
                    setBasicInfoForQuery(Integer.parseInt(p));
                }

                else if(key.equals(SettingsInterface.ADVANCED_QUERY_INFO)){
                    setAdvancedInfoForQuery(Integer.parseInt(p));
                }
                else if(key.equals(SettingsInterface.FORCE_IP_ADDRESS)){  
					boolean bs;
                    if (p.equals("true"))
                        bs=true;
                    else if (p.equals("false"))
                        bs=false;
                    else
                        return;
                    setForceIPAddress(bs);
				}
                else if(key.equals(SettingsInterface.FORCED_IP_ADDRESS)){
					setForcedIPAddress(p);
				}
                else if(key.equals(SettingsInterface.FORCED_PORT)){
                    setForcedPort(Integer.parseInt(p));
				}
                else if(key.equals(SettingsInterface.FREELOADER_FILES)) {
                    setFreeloaderFiles(Integer.parseInt(p));
				}
                else if(key.equals(SettingsInterface.FREELOADER_ALLOWED)) {
                    setFreeloaderAllowed(Integer.parseInt(p));
				}
            }
            catch(NumberFormatException nfe){ /* continue */ }
            catch(IllegalArgumentException iae){ /* continue */ }
            catch(ClassCastException cce){ /* continue */ }
        }

        //Special case: the legality of KEEP_ALIVE and MAX_INCOMING_CONNECTIONS
        //are dependent on the connection speed.  Now that the connection speed
        //has been loaded in, verify the properties for real.  If they don't
        //work, use suggested value.
        int incoming=getMaxIncomingConnections();
        int outgoing=getKeepAlive();
        setBothConnections(incoming, outgoing);
            
        write_ = true;
        writeProperties();
    }

    /** Loads in the default values.  Used
     *  when no properties file exists.
     */
    private void loadDefaults()
    {
        setMaxTTL(SettingsInterface.DEFAULT_MAX_TTL);
        setSoftMaxTTL(SettingsInterface.DEFAULT_SOFT_MAX_TTL);
        setTTL(SettingsInterface.DEFAULT_TTL);
        setMaxLength(SettingsInterface.DEFAULT_MAX_LENGTH);
        setTimeout(SettingsInterface.DEFAULT_TIMEOUT);
        setHostList(SettingsInterface.DEFAULT_HOST_LIST);
        setKeepAlive(SettingsInterface.DEFAULT_KEEP_ALIVE);
        setPort(SettingsInterface.DEFAULT_PORT);
        setConnectionSpeed(SettingsInterface.DEFAULT_SPEED);
        setUploadSpeed(SettingsInterface.DEFAULT_UPLOAD_SPEED);
        setSearchLimit(SettingsInterface.DEFAULT_SEARCH_LIMIT);
        //setClientID(SettingsInterface.DEFAULT_CLIENT_ID);
        setClientID( (new GUID(Message.makeGuid())).toHexString() );
        setMaxIncomingConnections(
           SettingsInterface.DEFAULT_MAX_INCOMING_CONNECTION);
        setBannedIps(SettingsInterface.DEFAULT_BANNED_IPS);
        setBannedWords(SettingsInterface.DEFAULT_BANNED_WORDS);
        setFilterAdult(SettingsInterface.DEFAULT_FILTER_ADULT);
        setFilterDuplicates(SettingsInterface.DEFAULT_FILTER_DUPLICATES);
        setFilterVbs(SettingsInterface.DEFAULT_FILTER_VBS);
        setFilterHtml(SettingsInterface.DEFAULT_FILTER_HTML);
        setFilterGreedyQueries(
                    SettingsInterface.DEFAULT_FILTER_GREEDY_QUERIES);
        setExtensions(SettingsInterface.DEFAULT_EXTENSIONS);
        setDirectories(home_);
        setSaveDirectory(home_);
        setSaveDefault(home_);
        setIncompleteDirectory(home_);
        //setInstallDir("");
        setUseQuickConnect(SettingsInterface.DEFAULT_USE_QUICK_CONNECT);
        setQuickConnectHosts(SettingsInterface.DEFAULT_QUICK_CONNECT_HOSTS);
        setParallelSearchMax(SettingsInterface.DEFAULT_PARALLEL_SEARCH);
        setClearCompletedUpload(SettingsInterface.DEFAULT_CLEAR_UPLOAD);
        setClearCompletedDownload(SettingsInterface.DEFAULT_CLEAR_DOWNLOAD);
        setMaxSimDownload(SettingsInterface.DEFAULT_MAX_SIM_DOWNLOAD);
        setMaxUploads(SettingsInterface.DEFAULT_MAX_UPLOADS);
        setSearchAnimationTime(SettingsInterface.DEFAULT_SEARCH_ANIMATION_TIME);
        setConnectString(SettingsInterface.DEFAULT_CONNECT_STRING);
        setConnectOkString(SettingsInterface.DEFAULT_CONNECT_OK_STRING);

		// RJS - setting the default values... 
		setLastVersionChecked(SettingsInterface.DEFAULT_LAST_VERSION_CHECKED);
		setCheckAgain(SettingsInterface.DEFAULT_CHECK_AGAIN);
		setBasicInfoForQuery(SettingsInterface.DEFAULT_BASIC_INFO_FOR_QUERY);
		setAdvancedInfoForQuery(SettingsInterface.DEFAULT_ADVANCED_INFO_FOR_QUERY);
		setForceIPAddress(DEFAULT_FORCE_IP_ADDRESS);
		setForcedIPAddress(DEFAULT_FORCED_IP_ADDRESS);
		setForcedPort(DEFAULT_FORCED_PORT);
        setFreeloaderFiles(DEFAULT_FREELOADER_FILES);
        setFreeloaderAllowed(DEFAULT_FREELOADER_ALLOWED);
        
		write_ = true;
        writeProperties();
    }


    /******************************************************
     *************  START OF ACCESSOR METHODS *************
     ******************************************************/

    /** returns the time to live */
    public byte getTTL(){return ttl_;}

    /** return the soft maximum time to live */
    public byte getSoftMaxTTL(){return softmaxttl_;}

    /** returns the maximum time to live*/
    public byte getMaxTTL(){return maxttl_;}

    /** returns the maximum allowable length of packets*/
    public int getMaxLength(){return maxLength_;}

    /** returns the timeout value*/
    public int getTimeout(){return timeout_;}

    /** returns a string specifying the full
     *  pathname of the file listing the hosts */
    public String getHostList(){return hostList_;}

    /** returns the keep alive value */
    public int getKeepAlive(){return keepAlive_;}

    /** returns the client's port number */
    public int getPort(){return port_;}

    /** returns the client's connection speed */
    public int getConnectionSpeed(){return connectionSpeed_;}

    public int getUploadSpeed() { return uploadSpeed_; }

    /** returns the client's search speed */
    public byte getSearchLimit(){return searchLimit_;}

    /** returns the client id number */
    public String getClientID(){return clientID_;}

    /** returns the maximum number of connections to hold */
    public int getMaxIncomingConnections(){return maxIncomingConn_;}

    /** returns the directory to save to */
    public String getSaveDirectory() {
        File file = new File(saveDirectory_);
        if(!file.isDirectory()) {
            boolean dirsMade = file.mkdirs();
            if(!dirsMade)
                return "";
        }
        return saveDirectory_;
    }

    /** returns the incomplete directory */
    public String getIncompleteDirectory() {
        File file = new File(incompleteDirectory_);
        if(!file.isDirectory()) {
            boolean dirsMade = file.mkdirs();
            if(!dirsMade)
                return "";
        }
        return incompleteDirectory_;
    }

    /** returns the default save directory */
    public String getSaveDefault() {
        File file = new File(saveDefault_);
        if(!file.isDirectory()) {
            boolean dirsMade = file.mkdirs();
            if(!dirsMade)
                return "";
        }
        return saveDefault_;
    }

    /** returns the directories to search */
    public String getDirectories(){return directories_;}
	
	public String[] getDirectoriesAsArray() {
		directories_.trim();
		return HTTPUtil.stringSplit(directories_, ';');
	}

    /** returns the string of file extensions*/
    public String getExtensions(){return extensions_;}

    public String[] getBannedIps(){return bannedIps_;}
    public String[] getBannedWords(){return bannedWords_;}
    public boolean getFilterAdult(){return filterAdult_;}
    public boolean getFilterDuplicates(){return filterDuplicates_;}
    public boolean getFilterHtml(){return filterHtml_;}
    public boolean getFilterVbs(){return filterVbs_;}
    public boolean getFilterGreedyQueries() { return filterGreedyQueries_; }

    public boolean getUseQuickConnect(){return useQuickConnect_;}
    public String[] getQuickConnectHosts(){return quickConnectHosts_;}
    public int getParallelSearchMax(){return parallelSearchMax_;}
    public int getMaxSimDownload(){return maxSimDownload_;}
    public int getMaxUploads(){return maxUploads_;}
    public boolean getClearCompletedUpload(){return clearCompletedUpload_;}
    public boolean getClearCompletedDownload(){return clearCompletedDownload_;}
    public int getSearchAnimationTime(){ return searchAnimationTime_; }

    public String getConnectString(){ return connectString_; }
    /** Returns the first word of the connect string.
     *  This is solely a convenience routine. */
    public String getConnectStringFirstWord(){ return connectStringFirstWord_; }
    /** Returns the remaing words of the connect string, without the leading space.
     *  This is solely a convenience routine. */
    public String getConnectStringRemainder(){ return connectStringRemainder_; }
    public String getConnectOkString(){ return connectOkString_; }


    /** specialized method for getting the number
     *  of files scanned */
    public int getFilesScanned()
    {return FileManager.getFileManager().getNumFiles();}

    // SPECIALIZED METHODS FOR NETWORK DISCOVERY
    /** returns the Network Discovery specialized properties file */
    public Properties getNDProps(){return ndProps_;}

    /** returns the path of the properties and host list files */
    public String getPath() {return home_;}

	public int getBasicInfoSizeForQuery() {return basicQueryInfo_;}

	public int getAdvancedInfoSizeForQuery() {return advancedQueryInfo_;}
	
	public boolean getForceIPAddress() {
		return forceIPAdress_;
	}

	public String getForcedIPAddress() {
		return forcedIPAdress_;
	}

	public int getForcedPort() {
		return forcedPort_;
	}

	/**
	 * private methods to handle versioning 
	 * control information
	 */
	public String getCurrentVersion() {
        //This is intentionally hard-coded in.
		return CURRENT_VERSION;
	}
	public String getLastVersionChecked() {
		return lastVersionChecked_;
	}
	public boolean getCheckAgain() {
		return checkAgain_;
	}
    public int getFreeloaderFiles() {
        return freeLoaderFiles_;
    }
    public int getFreeloaderAllowed() {
        return freeLoaderAllowed_;
    }

    

    /******************************************************
     **************  END OF ACCESSOR METHODS **************
     ******************************************************/

    /******************************************************
     *************  START OF MUTATOR METHODS **************
     ******************************************************/

    /** sets the maximum length of packets (spam protection)*/
    public synchronized void setMaxLength(int maxLength)
        throws IllegalArgumentException {
        if(false)
            throw new IllegalArgumentException();
        else {
            maxLength_ = maxLength;
            String s = Integer.toString(maxLength_);
            props_.put(SettingsInterface.MAX_LENGTH, s);
            writeProperties();
        }
    }

    /** sets the timeout */
    public synchronized void setTimeout(int timeout)
        throws IllegalArgumentException {
        if(false)
            throw new IllegalArgumentException();
        else {
            timeout_ = timeout;
            String s = Integer.toString(timeout_);
            props_.put(SettingsInterface.TIMEOUT, s);
            writeProperties();
        }

    }

    /**
     * Sets the keepAlive without checking the maximum value.
     * Throws IllegalArgumentException if keepAlive is negative.
     */
    public synchronized void setKeepAlive(int keepAlive)
        throws IllegalArgumentException {
        try {
            setKeepAlive(keepAlive, false);
        } catch (BadConnectionSettingException e) {
            throw new IllegalArgumentException();
        }
    }

    /** Internal method to set both connections at the same time,
     *  adjust both values as necessary. */
    private synchronized void setBothConnections(int outgoing,
                                                 int incoming) {
        incoming=Math.max(0, incoming);
        incoming=Math.min(incoming, maxConnections(true));
        outgoing=Math.max(0, outgoing);
        outgoing=Math.min(outgoing, maxConnections(false));
        if (incoming < outgoing)
            outgoing=incoming;
        setKeepAlive(outgoing);
        setMaxIncomingConnections(incoming);
    }

    /**
     * Sets the keep alive. If keepAlive is negative, throws
     * BadConnectionSettingException with a suggested value of 0.
     *
     * If checkLimit is true, then if keepAlive is too large for the current
     * connection speed or too large for the current number of incoming
     * connections, BadConnectionSettingException is thrown with suggested new
     * values.  The suggestions attempt to set KEEP_ALIVE to keepAlive, even if
     * that means adjusting MAX_INCOMING_CONNECTIONS.  The suggestions are not
     * necessarily guaranteed to be valid however.
     */
    public synchronized void setKeepAlive(int keepAlive,
                                          boolean checkLimit)
        throws BadConnectionSettingException {
        int incoming=getMaxIncomingConnections();
        if (checkLimit) {
            int max=maxConnections(false);
            //Too high for this connection speed?  Decrease it.
            if (keepAlive > max) {
                throw new BadConnectionSettingException(
                    BadConnectionSettingException.TOO_HIGH_FOR_SPEED,
                    max, maxConnections(true));
            }
            //Too high for the number of incoming connections?
            //Increase those.
            if (keepAlive > incoming) {
                throw new BadConnectionSettingException(
                    BadConnectionSettingException.OUT_GREATER_THAN_IN,
                    keepAlive, Math.min(keepAlive, maxConnections(true)));
            }
        }

        if (keepAlive<0) {
            throw new BadConnectionSettingException(
                BadConnectionSettingException.NEGATIVE_VALUE,
                0, incoming);
        } else {
            keepAlive_ = keepAlive;
            String s = Integer.toString(keepAlive_);
            props_.put(SettingsInterface.KEEP_ALIVE, s);
            writeProperties();
        }
    }

    /** Returns the maximum number of incoming/outgoing connections for the
     *  given connection speed. 
     */
    private int maxConnections(boolean incoming) {
        int speed=getConnectionSpeed();
        //I'm copying these numbers out of GUIStyles.  I don't want this to
        //depend on GUI code, though.  Ideally we'd restrict modem users to only
        //ONE incoming connection.  But that breaks the rule that incoming
        //connects>=outgoing.
        if (speed<=56)        //ISDN
            return 2;
        else if (speed<=350)  //cable
            return 4;
        else if (speed<=1000) //T1
            return 6;
        else                  //T3: no limit
            return Integer.MAX_VALUE;
    }


    /** sets the limit for the number of searches
     *  throws an exception on negative limits
     *  and limits of 10,000 or more */
    public synchronized void setSearchLimit(byte limit) {
        if(limit < 0 || limit > 10000)
            throw new IllegalArgumentException();
        else {
            searchLimit_ = limit;
            String s = Byte.toString(searchLimit_);
            props_.put(SettingsInterface.SEARCH_LIMIT, s);
            writeProperties();
        }
    }

    /** sets the client (gu) ID number */
    public synchronized void setClientID(String clientID) {
        if(false)
            throw new IllegalArgumentException();
        else {
            clientID_ = clientID;
            props_.put(SettingsInterface.CLIENT_ID, clientID_);
            writeProperties();
        }
    }

    /**
     * Sets the max number of incoming connections without checking the maximum
     * value. Throws IllegalArgumentException if maxConn is negative.  
     */
    public synchronized void setMaxIncomingConnections(int maxConn)
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
    public synchronized void setMaxIncomingConnections(int maxConn,
                                                       boolean checkLimit)
        throws BadConnectionSettingException {
        int outgoing=getKeepAlive();
        if (checkLimit) {
            int max=maxConnections(true);
            //Too high for this connection speed?  Decrease it.
            if (maxConn > max) {
                throw new BadConnectionSettingException(
                    BadConnectionSettingException.TOO_HIGH_FOR_SPEED,
                    outgoing, max);
            }
            //Too low for the number of outgoing connections?
            //Decrease those.
            if (maxConn < outgoing) {
                throw new BadConnectionSettingException(
                    BadConnectionSettingException.OUT_GREATER_THAN_IN,
                    maxConn, maxConn);
            }
        }

        if(maxConn < 0) {
            throw new BadConnectionSettingException(
                BadConnectionSettingException.NEGATIVE_VALUE,
                0, 0);
        } else {
            maxIncomingConn_ = maxConn;
            String s = Integer.toString(maxConn);
            props_.put(SettingsInterface.MAX_INCOMING_CONNECTIONS, s);
            writeProperties();
        }
    }

    public synchronized void setIncompleteDirectory(String dir) {
        if(!dir.endsWith(File.separator))
            dir += File.separator;
        File f = new File(dir);
        boolean b = f.isDirectory();
        if(b == false)
            throw new IllegalArgumentException();
        else {
            incompleteDirectory_ = dir;
            props_.put(SettingsInterface.INCOMPLETE_DIR, dir);
        }
    }

    /** sets the hard maximum time to live */
    public synchronized void setMaxTTL(byte maxttl)
        throws IllegalArgumentException {
        if(maxttl < 0 || maxttl > 50)
            throw new IllegalArgumentException();
        else {
            maxttl_ = maxttl;
            String s = Byte.toString(maxttl_);
            props_.put(SettingsInterface.MAX_TTL, s);
        }
    }

	/** sets the default save directory for when the user 
	 *  presses the "use default" button in the config 
	 *  window.  this method should only get called at
	 *  install time, and is therefore not synchronized */
    public void setSaveDefault(String dir) {
        if(!dir.endsWith(File.separator))
            dir += File.separator;
        File f = new File(dir);
        boolean b = f.isDirectory();
        if(!b)
            throw new IllegalArgumentException();
        else {
            saveDefault_ = dir;
            props_.put(SettingsInterface.SAVE_DEFAULT, dir);
            //writeProperties();
        }
    }

	public void setBasicInfoForQuery(int basicInfo) {
		basicQueryInfo_ = basicInfo;
		String s = Integer.toString(basicInfo);
		props_.put(SettingsInterface.BASIC_QUERY_INFO, s);
	}
	
	public void setAdvancedInfoForQuery(int advancedInfo) {
		advancedQueryInfo_ = advancedInfo;
		String s = Integer.toString(advancedInfo);
		props_.put(SettingsInterface.ADVANCED_QUERY_INFO, s);
	}

    /******************************************************
     *********  START OF CONFIGURATION SETTINGS ***********
     ******************************************************/

    /** set the directory for saving files */
    public void setSaveDirectory(String dir) {
        if(!dir.endsWith(File.separator))
            dir += File.separator;
        File f = new File(dir);
        boolean b = f.isDirectory();
        if(b == false)
            throw new IllegalArgumentException();
        else {
            saveDirectory_ = dir;
            props_.put(SettingsInterface.SAVE_DIRECTORY, dir);
        }
    }

    /** set the directories to search.  this is synchronized
	 *  because some gui elements may want to make this call
	 *  in a separate thread (they probably should, since this
	 *  method could take awhile. this method will also filter
	 *  out any duplicate or invalid directories in the string. */
    public synchronized void setDirectories(String dir) {
		boolean dirsModified = false;
		directories_ = dir;
		String[] dirs = getDirectoriesAsArray();
		int i = 0;
		while(i < dirs.length) {
			if(dirs[i] != null) {
				File f = new File(dirs[i]);
				if(f.isDirectory()) {
					int count = 0;
					int z = 0;
					String str = "";
					try {str = f.getCanonicalPath();}
					catch(IOException ioe) {break;}
					while(z < dirs.length) {
						if(dirs[z] != null) { 
							File file = new File(dirs[z]);
							String name = "";
							try {name = file.getCanonicalPath();}
							catch(IOException ioe) {break;}
							if(str.equals(name)) { 
								count++;			
								if(count > 1) {
									dirs[z] = null;
									dirsModified = true;
								}
							}
						}
						z++;
					}					
				}
				else {
					dirs[i] = null;
					dirsModified = true;
				}
			}
			i++;
		}
		if(dirsModified) {
			i = 0;
			StringBuffer sb = new StringBuffer();
			while(i < dirs.length) {
				if(dirs[i] != null) {
					sb.append(dirs[i]);
					sb.append(';');
				}
				i++;
			}
			directories_ = sb.toString();
		}
        FileManager.getFileManager().reset();
        FileManager.getFileManager().addDirectories(directories_);        
        props_.put(SettingsInterface.DIRECTORIES, directories_);
    }

//  	public static void main(String args[]) {
//  		System.out.println("directories_: "+ directories_);
//  		SettingsManager settings = SettingsManager.instance();
//  		System.out.println("directories_: "+ directories_);
//  		settings.setDirectories("c:\\p;c:\\p;c:\\pC:\\My Music;C:\\Program Files;"+
//  								"C:\\Program Files\\LimeWire;"+
//  								"C:\\Program Files\\LimeWire;C:\\Program Files;C:\\My Music;"+
//  								"c:\\My Music;c:\\Program Files\\Direct;"+
//  								"C:\\Program Files\\direct\\;C:\\ProgramFiles");
//  		System.out.println("directories_: "+ directories_);
//  	}

    /** set the extensions to search for */
    public void setExtensions(String ext) {
        FileManager.getFileManager().setExtensions(ext);
		if(getDirectories() != null) {
			FileManager.getFileManager().reset();
			FileManager.getFileManager().addDirectories(getDirectories());
		}
        extensions_ = ext;
        props_.put(SettingsInterface.EXTENSIONS, ext);
    }

    /** sets the time to live */
    public void setTTL(byte ttl)
        throws IllegalArgumentException {
        if (ttl < 1 || ttl > 14)
            throw new IllegalArgumentException();
        else {
            ttl_ = ttl;
            String s = Byte.toString(ttl_);
            props_.put(SettingsInterface.TTL, s);
        }
    }

    /** sets the soft maximum time to live */
    public void setSoftMaxTTL(byte softmaxttl) {
        if (softmaxttl < 0 || softmaxttl > 14)
            throw new IllegalArgumentException();
        else {
            softmaxttl_ = softmaxttl;
            String s = Byte.toString(softmaxttl);
            props_.put(SettingsInterface.SOFT_MAX_TTL, s);
        }
    }

    /** sets the port to connect on */
    public synchronized void setPort(int port) {
        // if the entered port is outside accepted
        // port numbers, throw the exception
        if(port > 65536 || port < 0)
            throw new IllegalArgumentException();
        else {
            port_ = port;
            String s = Integer.toString(port_);
            props_.put(SettingsInterface.PORT, s);
        }
    }

    /** sets the connection speed.  throws an
     *  exception if you try to set the speed
     *  far faster than a T3 line or less than
     *  0.*/
    public void setConnectionSpeed(int speed) {
        if(speed < 0 || speed > 20000)
            throw new IllegalArgumentException();
        else {
            connectionSpeed_ = speed;
            String s = Integer.toString(connectionSpeed_);
            props_.put(SettingsInterface.SPEED, s);
        }
    }

    /** Sets the percentage of total bandwidth (as given by
     *  CONNECTION_SPEED) to use for uploads.  This is shared
     *  equally among all uploads.  Throws IllegalArgumentException
     *  if speed<0 or speed>100. */
    public synchronized void setUploadSpeed(int speed) {
        if (speed<0 || speed>100)
            throw new IllegalArgumentException();
        else {
            uploadSpeed_ = speed;
            String s = Integer.toString(uploadSpeed_);
            props_.put(SettingsInterface.UPLOAD_SPEED, s);
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
        connectString_=connect;
        connectStringFirstWord_=firstWord;
        connectStringRemainder_=remainder;

        props_.put(SettingsInterface.CONNECT_STRING, connect);
    }

    public void setConnectOkString(String ok)
        throws IllegalArgumentException {
        if (ok.length()<1)
            throw new IllegalArgumentException();

        connectOkString_=ok;
        props_.put(SettingsInterface.CONNECT_OK_STRING, ok);
    }

    public synchronized void setParallelSearchMax(int max) {
        if(max<1)
            throw new IllegalArgumentException();
        else {
            parallelSearchMax_ = max;
            String s = String.valueOf(max);
            props_.put(SettingsInterface.PARALLEL_SEARCH, s);
        }
    }

    public void setMaxSimDownload(int max) {
        if(false)
            throw new IllegalArgumentException();
        else {
            maxSimDownload_ = max;
            String s = String.valueOf(max);
            props_.put(SettingsInterface.MAX_SIM_DOWNLOAD, s);
        }
    }

    public void setMaxUploads(int max) {
        if(false)
            throw new IllegalArgumentException();
        else {
            maxUploads_ = max;
            String s = String.valueOf(max);
            props_.put(SettingsInterface.MAX_UPLOADS, s);
        }
    }

    public void setClearCompletedUpload(boolean b) {
        if(false)
            throw new IllegalArgumentException();
        else {
            clearCompletedUpload_ = b;
            String s = String.valueOf(b);
            props_.put(SettingsInterface.CLEAR_UPLOAD, s);
        }
    }

    public void setClearCompletedDownload(boolean b) {
        if(false)
            throw new IllegalArgumentException();
        else {
            clearCompletedDownload_ = b;
            String s = String.valueOf(b);
            props_.put(SettingsInterface.CLEAR_DOWNLOAD, s);
        }
    }
	
	public void setForceIPAddress(boolean force) {
		String c;
		if (force == true)
			c = "true";
		else 
			c = "false";
		forceIPAdress_ = force;
		props_.put(SettingsInterface.FORCE_IP_ADDRESS, c);
	}

	public void setForcedIPAddress(String address) {
		if (address == null)
			throw new IllegalArgumentException();
		forcedIPAdress_ = address;
		props_.put(SettingsInterface.FORCED_IP_ADDRESS, address);
	}

	public void setForcedPort(int port) {
        // if the entered port is outside accepted
        // port numbers, throw the exception
        if(port > 65536 || port < 1)
            throw new IllegalArgumentException();
		else {
			forcedPort_ = port;
			String s = Integer.toString(forcedPort_);
			props_.put(SettingsInterface.FORCED_PORT, s);
		}
	}
	

    /******************************************************
     *********  END OF CONFIGURATION SETTINGS *************
     ******************************************************/

    public synchronized void setBannedIps(String[] bannedIps) {
        if(bannedIps == null)
            throw new IllegalArgumentException();
        else {
            bannedIps_ = bannedIps;
            props_.put(SettingsInterface.BANNED_IPS,
                       encode(bannedIps));
            writeProperties();
        }
    }

    public synchronized void setBannedWords(String[] bannedWords) {
        if(bannedWords == null)
            throw new IllegalArgumentException();
        else {
            bannedWords_ = bannedWords;
            props_.put(SettingsInterface.BANNED_WORDS,
                       encode(bannedWords));
            writeProperties();
        }
    }

    public synchronized void setFilterAdult(boolean filterAdult) {
        if(false)
            throw new IllegalArgumentException();
        else {
            filterAdult_ = filterAdult;
            Boolean b = new Boolean(filterAdult);
            String s = b.toString();
            props_.put(SettingsInterface.FILTER_ADULT, s);
            writeProperties();
        }
    }

    public synchronized void setFilterDuplicates(boolean filterDuplicates) {
        if(false)
            throw new IllegalArgumentException();
        else {
            filterDuplicates_ = filterDuplicates;
            Boolean b = new Boolean(filterDuplicates);
            String s = b.toString();
            props_.put(SettingsInterface.FILTER_DUPLICATES, s);
            writeProperties();
        }
    }

    public synchronized void setFilterHtml(boolean filterHtml) {
        if(false)
            throw new IllegalArgumentException();
        else {
            filterHtml_ = filterHtml;
            Boolean b = new Boolean(filterHtml);
            String s = b.toString();
            props_.put(SettingsInterface.FILTER_HTML, s);
            writeProperties();
        }
    }

    public synchronized void setFilterVbs(boolean filterVbs) {
        if(false)
            throw new IllegalArgumentException();
        else {
            filterVbs_ = filterVbs;
            Boolean b = new Boolean(filterVbs);
            String s = b.toString();
            props_.put(SettingsInterface.FILTER_VBS, s);
            writeProperties();
        }
    }

    public synchronized void setFilterGreedyQueries(boolean yes) {
        filterGreedyQueries_ = yes;
        Boolean b = new Boolean(yes);
        String s = b.toString();
        props_.put(SettingsInterface.FILTER_GREEDY_QUERIES, s);
        writeProperties();
    }

    public synchronized void setUseQuickConnect(boolean useQuickConnect) {
        if(false)
            throw new IllegalArgumentException();
        else {
            useQuickConnect_ = useQuickConnect;
            Boolean b = new Boolean(useQuickConnect);
            String s = b.toString();
            props_.put(SettingsInterface.USE_QUICK_CONNECT, s);
            writeProperties();
        }
    }

    public synchronized void setQuickConnectHosts(String[] hosts) {
        if(hosts == null)
            throw new IllegalArgumentException();
        else {
            quickConnectHosts_ = hosts;
            props_.put(SettingsInterface.QUICK_CONNECT_HOSTS,
                       encode(hosts));
            writeProperties();
        }
    }


    public synchronized void setSearchAnimationTime(int seconds) {
        if(seconds < 0)
            throw new IllegalArgumentException();
        else {
            searchAnimationTime_=seconds;
            String s = Integer.toString(seconds);
            props_.put(SettingsInterface.SEARCH_ANIMATION_TIME, s);
            writeProperties();
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
        this.freeLoaderAllowed_=allowed;
        String s = Integer.toString(allowed);
        props_.put(SettingsInterface.FREELOADER_ALLOWED, s);
        writeProperties();
    }

    /** 
     * Sets minimum the number of files a host must share to not be considered a
     * freeloader.  For example, if files==0, no host is considered a
     * freeloader.  Throws IllegalArgumentException if files<0.  
     */
    public void setFreeloaderFiles(int files) 
        throws IllegalArgumentException
    {
        if (files<0)
            throw new IllegalArgumentException();
        this.freeLoaderFiles_=files;
        String s = Integer.toString(files);
        props_.put(SettingsInterface.FREELOADER_FILES, s);
        writeProperties();
    }

	/**
	 * private methods to handle versioning 
	 * control information
	 */
	public void setLastVersionChecked(String last) {
		lastVersionChecked_ = last;
		props_.put(SettingsInterface.LAST_VERSION_CHECKED, last);
		writeProperties();
	}
	
	public void setCheckAgain(boolean check) {
		checkAgain_ = check;
		String c;
		if (check == true)
			c = "true";
		else 
			c = "false";
		props_.put(SettingsInterface.CHECK_AGAIN, c);
        writeProperties();
	}

    /**
     *  Sets the pathname String for the file that
     *  lists the default hosts.  This is a unique
     *  method in that the host list cannot be set
     *  in the properties file
     */
    private void setHostList(String hostList) {
        String fn = home_ + hostList;
        File f = new File(fn);
        if(f.isFile() == true)
            hostList_ = fn;
        else {
            try {
                FileWriter fw = new FileWriter(fn);
                hostList_ = fn;
            }
            catch(IOException e){
                // not sure what to do if the filewriter
                // fails to create a file
            }
        }
    }

    /******************************************************
     ***************  END OF MUTATOR METHODS **************
     ******************************************************/


    /** writes out the Network Discovery specialized
     *  properties file
     */
    public synchronized void writeNDProps() {
        FileOutputStream ostream = null;
        try {
            ostream = new FileOutputStream(ndFileName_);
            props_.save(ostream, "Properties file for Network Discovery");
            ostream.close();
        }
        catch (Exception e){}
        finally {
            try {
                ostream.close();
            }
            catch(IOException io) {}
        }
    }

    /** writes out the properties file to with the specified
     *  name in the user's home directory
     */
    public synchronized void writeProperties() {
        if(write_) {
            FileOutputStream ostream = null;
            try {
                ostream = new FileOutputStream(fileName_);
                props_.save(ostream, "");
                ostream.close();
            }
            catch (Exception e){}
            finally {
                try {
                    ostream.close();
                }
                catch(IOException io) {}
            }
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

    public synchronized void setWrite(boolean write) {
        write_ = write;
    }

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
}
