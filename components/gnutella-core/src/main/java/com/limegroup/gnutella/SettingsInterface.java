package com.limegroup.gnutella;

import java.util.Properties;
import com.sun.java.util.collections.*;
import java.io.IOException;

/**
 *  This class sets up an interface for the SettingsManager
 *  It's designed to make life simpler, so hopefully it
 *  will!  This contains all of the default properties
 *  for the program.  If you want to change any defaults,
 *  change them here.  This also contains all of the
 *  accessor and mutator methods that you can call on the
 *  properties manager.
 *
 *  @author Adam Fisk
 */

public interface SettingsInterface
{
    // Accessor methods
    public byte       getTTL();
    public byte       getSoftMaxTTL();
    public byte       getMaxTTL();
    public int        getMaxLength();
    public int        getTimeout();
    public String     getHostList();
    public int        getKeepAlive();
    public int        getPort();
    public int        getConnectionSpeed();
    public int        getUploadSpeed();
    public byte       getSearchLimit();
    public String     getClientID();
    public int        getMaxIncomingConnections();
    public String     getSaveDirectory();
    public String     getSaveDefault();
    public String     getDirectories();
    public String     getExtensions();
    public String     getIncompleteDirectory();
    public String[]   getBannedIps();
    public String[]   getBannedWords();
    public boolean    getFilterDuplicates();
    public boolean    getFilterAdult();
    public boolean    getFilterVbs();
    public boolean    getFilterHtml();
    public boolean    getFilterGreedyQueries();
    public boolean    getFilterBearShareQueries();
    public boolean    getUseQuickConnect();
    public String[]   getQuickConnectHosts();
    public int        getParallelSearchMax();
    public int        getMaxSimDownload();
    public int        getMaxUploads();
    public boolean    getClearCompletedUpload();
    public boolean    getClearCompletedDownload();
    /** special method for getting the number of files scanned */
    public int        getFilesScanned();
    public int        getSearchAnimationTime();		
	
    public String     getConnectString();
    public String     getConnectOkString();

   /** The current version of LimeWire.  This is read-only. */
	public String getCurrentVersion();
	public String getLastVersionChecked();
	public boolean getCheckAgain();

    public int getFreeloaderFiles();
    public int getFreeloaderAllowed();

    /** writes out the properties to disk */
    public void writeProperties();

    /** returns the Properties file for Network Discovery */
    public Properties getNDProps();

    /** returns the path where the properties and host
     *  host list file get saved*/
    public String getPath();

    /** set the user time to live */
    public void setTTL(byte ttl)
        throws IllegalArgumentException;

    /** set the soft maximum time to live (TTLs above this are readjusted) */
    public void setSoftMaxTTL(byte smaxttl)
        throws IllegalArgumentException;

    /** set the hard maximum time to live (TTLs above this are dropped) */
    public void setMaxTTL(byte maxttl)
        throws IllegalArgumentException;

    /** set the maximum length of packets */
    public void setMaxLength(int maxLength)
        throws IllegalArgumentException;

    /** set the timeout */
    public void setTimeout(int timeout)
        throws IllegalArgumentException;

    /** set the keep alive */
    public void setKeepAlive(int keepAlive)
        throws IllegalArgumentException;

    /** set the keep alive, ensuring it is not too large. */
    public void setKeepAlive(int keepAlive, boolean checkUpperLimit)
        throws BadConnectionSettingException;

    /** set the port */
    public void setPort(int port)
        throws IllegalArgumentException;

    /** set the network connection speed */
    public void setConnectionSpeed(int speed)
        throws IllegalArgumentException;

    /** sets the percentage of bandwidth to use for uploads. */
    public void setUploadSpeed(int speed)
    throws IllegalArgumentException;

    /** set the maximum number of searches */
    public void setSearchLimit(byte limit)
        throws IllegalArgumentException;

    /** set the client (gu) id */
    public void setClientID(String clientID)
        throws IllegalArgumentException;

    /** set the maximum number of connections */
    public void setMaxIncomingConnections(int maxConn)
        throws IllegalArgumentException;

    /** set the maximum number of incoming connections,
     *  ensuring the value is not too large. */
    public void setMaxIncomingConnections(int maxConn, boolean checkUpperLimit)
        throws BadConnectionSettingException;

    /** sets the directory for saving files*/
    public void setSaveDirectory(String dir)
        throws IllegalArgumentException;
    /** sets the default directory for saving files*/
    public void setSaveDefault(String dir)
        throws IllegalArgumentException;

    /** sets the string list of directories*/
    public void setDirectories(String directories)
        throws IllegalArgumentException;

    /** sets the string of extensions*/
    public void setExtensions(String extensions)
        throws IllegalArgumentException;

    /** sets the directory for saving incomplete files */
    public void setIncompleteDirectory(String dir)
        throws IllegalArgumentException;

    public void setBannedIps(String[] ips)
        throws IllegalArgumentException;

    /** Set the list of words to ban in queries.
     *  If words is not a semicolon-separated list of white-space
     *  free words, throws IllegalArgumentException. */
    public void setBannedWords(String[] words)
        throws IllegalArgumentException;

    /** Sets whether duplicate packets should be ignored. */
    public void setFilterDuplicates(boolean b);

    /** Sets whether queries that are not
     *  family-friendly should be ignored. */
    public void setFilterAdult(boolean b);
    public void setFilterVbs(boolean b);
    public void setFilterHtml(boolean b);
    public void setFilterGreedyQueries(boolean b);
    public void setFilterBearShareQueries(boolean b);

    public void setUseQuickConnect(boolean b);
    public void setQuickConnectHosts(String[] hosts);
    public void setParallelSearchMax(int max);
    public void setMaxSimDownload(int max);
    public void setMaxUploads(int max);
    public void setClearCompletedUpload(boolean b);
    public void setClearCompletedDownload(boolean b);
    public void setSearchAnimationTime(int seconds);

    /** Sets the handshake string for initializing outgoing connections,
     *  without trailing newlines. */
    public void setConnectString(String connect);
    /** Sets the handshake string for initializing outgoing connections,
     *  without trailing newlines. */
    public void setConnectOkString(String ok);
	
	public void setLastVersionChecked(String last);
	public void setCheckAgain(boolean check);

    public void setFreeloaderFiles(int files);
    public void setFreeloaderAllowed(int probability);

    /** specialized method for writing the
     *  properties file for the network discoverer
     */
    public void writeNDProps();
    public void setWrite(boolean b);

    /** Default setting for the time to live */
    public static final byte    DEFAULT_TTL            = (byte)5;
    /** Default setting for the soft maximum time to live */
    public static final byte    DEFAULT_SOFT_MAX_TTL   = (byte)10;
    /** Default setting for the hard maximum time to live */
    public static final byte    DEFAULT_MAX_TTL        = (byte)50;
    /** Default maximum packet length */
    public static final int     DEFAULT_MAX_LENGTH     = 65536;
    /** Default timeout */
    public static final int     DEFAULT_TIMEOUT        = 8000;
    /** Default file path for the host list */
    public static final String  DEFAULT_HOST_LIST      = "gnutella.net";
    /** Default name for the properties file */
    public static final String  DEFAULT_FILE_NAME      = "limewire.props";
    /** Default name for the network discovery properties */
    public static final String  DEFAULT_ND_PROPS_NAME  = "nd.props";
    /** Default value for the keep alive */
    public static final int     DEFAULT_KEEP_ALIVE     = 3;
    /** Default port*/
    public static final int     DEFAULT_PORT           = 6346;
    /** Default network connection speed */
    public static final int     DEFAULT_SPEED          = 56;
    public static final int     DEFAULT_UPLOAD_SPEED   = 100;
    /** Default limit for the number of searches */
    public static final byte    DEFAULT_SEARCH_LIMIT   = (byte)64;
    /** Default client/gu id */
    //public static final String  DEFAULT_CLIENT_ID      = "A0B447F77853D411B05B0001023AF3D6";
    public static final String  DEFAULT_CLIENT_ID      = null;
    /** Default maximum number of connections */
    public static final int     DEFAULT_MAX_INCOMING_CONNECTION=3;
    /** Default directories for file searching */
    public static final String  DEFAULT_SAVE_DIRECTORY = "";
    /** Default directories for file searching */
    public static final String  DEFAULT_DIRECTORIES    = "";
    /** Default file extensions */
    public static final String  DEFAULT_EXTENSIONS     =
    "html;htm;xml;txt;pdf;ps;rtf;doc;tex;mp3;wav;au;aif;aiff;ra;ram;"+
    "mpg;mpeg;asf;qt;mov;avi;mpe;swf;dcr;gif;jpg;jpeg;jpe;png;tif;tiff;"+
    "exe;zip;gz;gzip;hqx;tar;tgz;z";
	

    /** default banned ip addresses */
    public static final String[] DEFAULT_BANNED_IPS     = {};
    public static final String[] DEFAULT_BANNED_WORDS   = {};
    public static final boolean DEFAULT_FILTER_ADULT   = false;
    public static final boolean DEFAULT_FILTER_DUPLICATES = true;
    /** Filter .vbs files? */
    public static final boolean DEFAULT_FILTER_VBS     = true;
    /** Filter .htm[l] files? */
    public static final boolean DEFAULT_FILTER_HTML    = false;
    public static final boolean DEFAULT_FILTER_GREEDY_QUERIES = true;
    public static final boolean DEFAULT_FILTER_BEARSHARE_QUERIES = true;
    /** Use quick connect hosts instead of gnutella.net? */
    public static final boolean DEFAULT_USE_QUICK_CONNECT = true;
    /** List of hosts to try on quick connect */
    public static final String[] DEFAULT_QUICK_CONNECT_HOSTS
    = {"router.limewire.com:6346",
       "gnutellahosts.com:6346",
    };
    public static final int     DEFAULT_PARALLEL_SEARCH  = 5;
    public static final int     DEFAULT_MAX_SIM_DOWNLOAD = 4;
    public static final int     DEFAULT_MAX_UPLOADS      = 4;
    public static final boolean DEFAULT_CLEAR_UPLOAD     = false;
    public static final boolean DEFAULT_CLEAR_DOWNLOAD   = false;
    public static final int     DEFAULT_SEARCH_ANIMATION_TIME = 45;
    public static final String  DEFAULT_CONNECT_STRING    = "GNUTELLA CONNECT/0.4";
    public static final String  DEFAULT_CONNECT_OK_STRING = "GNUTELLA OK";
    public static final int     DEFAULT_BASIC_INFO_FOR_QUERY = 1000;
    public static final int     DEFAULT_ADVANCED_INFO_FOR_QUERY = 50;	

    public static final String  DEFAULT_LAST_VERSION_CHECKED = "0.5";
    public static final boolean DEFAULT_CHECK_AGAIN = true;
	public static final boolean DEFAULT_FORCE_IP_ADDRESS = false;
	public static final byte[]  DEFAULT_FORCED_IP_ADDRESS = {};
	public static final String  DEFAULT_FORCED_IP_ADDRESS_STRING = "";
	public static final int     DEFAULT_FORCED_PORT = 6346;
    public static final int     DEFAULT_FREELOADER_FILES = 1;
    public static final int     DEFAULT_FREELOADER_ALLOWED = 100;

    // The property key name constants
    public static final String TTL            = "TTL";
    public static final String SOFT_MAX_TTL   = "SOFT_MAX_TTL";
    public static final String MAX_TTL        = "MAX_TTL";
    public static final String MAX_LENGTH     = "MAX_LENGTH";
    public static final String TIMEOUT        = "TIMEOUT";
    public static final String KEEP_ALIVE     = "KEEP_ALIVE";
    public static final String PORT           = "PORT";
    public static final String SPEED          = "CONNECTION_SPEED";
    public static final String UPLOAD_SPEED   = "UPLOAD_SPEED";
    public static final String SEARCH_LIMIT   = "SEARCH_LIMIT";
    public static final String CLIENT_ID      = "CLIENT_ID";
    public static final String MAX_INCOMING_CONNECTIONS
        = "MAX_INCOMING_CONNECTIONS";
    public static final String SAVE_DIRECTORY = "DIRECTORY_FOR_SAVING_FILES";
    public static final String INCOMPLETE_DIR = "INCOMPLETE_FILE_DIRECTORY";
    public static final String DIRECTORIES    = "DIRECTORIES_TO_SEARCH_FOR_FILES";
    public static final String EXTENSIONS     = "EXTENSIONS_TO_SEARCH_FOR";
    public static final String BANNED_IPS     = "BLACK_LISTED_IP_ADDRESSES";
    public static final String BANNED_WORDS   = "BANNED_WORDS";
    public static final String FILTER_DUPLICATES = "FILTER_DUPLICATES";
    public static final String FILTER_ADULT   = "FILTER_ADULT";
    public static final String FILTER_HTML    = "FILTER_HTML";
    public static final String FILTER_VBS     = "FILTER_VBS";
    public static final String FILTER_GREEDY_QUERIES = "FILTER_GREEDY_QUERIES";
    public static final String FILTER_BEARSHARE_QUERIES = "FILTER_HIGHBIT_QUERIES";
    public static final String USE_QUICK_CONNECT = "USE_QUICK_CONNECT";
    public static final String QUICK_CONNECT_HOSTS = "QUICK_CONNECT_HOSTS";
    public static final String PARALLEL_SEARCH= "PARALLEL_SEARCH";
    public static final String MAX_SIM_DOWNLOAD="MAX_SIM_DOWNLOAD";
    public static final String MAX_UPLOADS     ="MAX_UPLOADS";
    public static final String CLEAR_UPLOAD   = "CLEAR_UPLOAD";
    public static final String CLEAR_DOWNLOAD = "CLEAR_DOWNLOAD";
    public static final String SEARCH_ANIMATION_TIME = "SEARCH_ANIMATION_TIME";
    public static final String SAVE_DEFAULT   = "SAVE_DEFAULT";

    public static final String CONNECT_STRING = "CONNECT_STRING";
    public static final String CONNECT_OK_STRING = "CONNECT_OK_STRING";
	public static final String LAST_VERSION_CHECKED = "LAST_VERSION_CHECKED";
    public static final String CHECK_AGAIN = "CHECK_AGAIN";
	public static final String BASIC_QUERY_INFO = "BASIC_QUERY_INFO";
	public static final String ADVANCED_QUERY_INFO = "ADVANCED_QUERY_INFO";
	public static final String FORCE_IP_ADDRESS = "FORCE_IP_ADDRESS";
	public static final String FORCED_IP_ADDRESS = "FORCED_IP_ADDRESS";
	public static final String FORCED_IP_ADDRESS_STRING 
		= "FORCED_IP_ADDRESS_STRING";
	public static final String FORCED_PORT = "FORCED_PORT";
    public static final String FREELOADER_FILES = "FREELOADER_FILES";
    public static final String FREELOADER_ALLOWED = "FREELOADER_ALLOWED";

}
