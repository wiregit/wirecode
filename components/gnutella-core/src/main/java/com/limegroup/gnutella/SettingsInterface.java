package com.limegroup.gnutella;

import java.util.*;
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
    public byte       getSearchLimit();
    public boolean    getStats();
    public String     getClientID();
    public int        getMaxConn();
    public String     getSaveDirectory();
    public String     getDirectories();
    public String     getExtensions();
    public String[]   getBannedIps();
    public String[]   getBannedWords();
    public boolean    getFilterDuplicates();
    public boolean    getFilterAdult();
    public boolean    getFilterVbs();
    public boolean    getFilterHtml();
    public boolean    getUseQuickConnect();
    public String[]   getQuickConnectHosts();
    public int        getParallelSearchMax();
    public int        getMaxSimDownload();
    public boolean    getClearCompletedDownload();
    /** special method for getting the number of files scanned */
    public int        getFilesScanned();
    public int        getSearchAnimationTime();

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

    /** set the port */
    public void setPort(int port)
	throws IllegalArgumentException;
    
    /** set the network connection speed */
    public void setConnectionSpeed(int speed)
	throws IllegalArgumentException;

    /** set the maximum number of searches */
    public void setSearchLimit(byte limit)
	throws IllegalArgumentException;

    /** set the boolean specifying whether 
     *  the stats file exists 
     */
    public void setStats(boolean stats)
	throws IllegalArgumentException;

    /** set the client (gu) id */
    public void setClientID(String clientID)
	throws IllegalArgumentException;

    /** set the maximum number of connections */
    public void setMaxConn(int maxConn)
	throws IllegalArgumentException;

    /** sets the directory for saving files*/
    public void setSaveDirectory(String dir)
	throws IllegalArgumentException;

    /** sets the string list of directories*/
    public void setDirectories(String directories)
	throws IllegalArgumentException;
    
    /** sets the string of extensions*/
    public void setExtensions(String extensions)
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

    public void setUseQuickConnect(boolean b);
    public void setQuickConnectHosts(String[] hosts);
    public void setParallelSearchMax(int max);
    public void setMaxSimDownload(int max);
    public void setClearCompletedDownload(boolean b);
    public void setSearchAnimationTime(int seconds);

    /** specialized method for writing the 
     *  properties file for the network discoverer
     */
    public void writeNDProps();

    /** Default setting for the time to live */
    public static final byte    DEFAULT_TTL            = (byte)7;
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
    public static final int     DEFAULT_KEEP_ALIVE     = 4;
    /** Default port*/
    public static final int     DEFAULT_PORT           = 6346;
    /** Default network connection speed */
    public static final int     DEFAULT_SPEED          = 56;
    /** Default limit for the number of searches */
    public static final byte    DEFAULT_SEARCH_LIMIT   = (byte)64;
    /** Default client/gu id */
    //public static final String  DEFAULT_CLIENT_ID      = "A0B447F77853D411B05B0001023AF3D6";
    public static final String  DEFAULT_CLIENT_ID      = null;
    /** Default boolean for stats file */
    public static final boolean DEFAULT_STATS          = false;
    /** Default maximum number of connections */
    public static final int     DEFAULT_MAX_CONN       = 900;
    /** Default directories for file searching */
    public static final String  DEFAULT_SAVE_DIRECTORY = "";
    /** Default directories for file searching */
    public static final String  DEFAULT_DIRECTORIES    = "";
    /** Default file extensions */
    public static final String  DEFAULT_EXTENSIONS     = "mp3;"+
	"mp2;mp1;mpg;vqf;avi;mpeg;wav;mod;voc;it;xm;s3m;stm;"+
	"wma;mov;asf;zip;rar";
    /** default banned ip addresses */
    public static final String[] DEFAULT_BANNED_IPS     = {};
    public static final String[] DEFAULT_BANNED_WORDS   = {};
    public static final boolean DEFAULT_FILTER_ADULT   = false;
    public static final boolean DEFAULT_FILTER_DUPLICATES = true;
    /** Filter .vbs files? */
    public static final boolean DEFAULT_FILTER_VBS     = true;
    /** Filter .htm[l] files? */
    public static final boolean DEFAULT_FILTER_HTML    = false;
    /** Use quick connect hosts instead of gnutella.net? */
    public static final boolean DEFAULT_USE_QUICK_CONNECT = false;
    /** List of hosts to try on quick connect */
    public static final String[] DEFAULT_QUICK_CONNECT_HOSTS 
	= {"gnutellahosts.com:6346", "gnet.ath.cx:6346",
	   "gnet1.ath.cx:6346","gnet2.ath.cx:6346","gnet3.ath.cx:6346",
	   "gnet4.ath.cx:6346"};
    public static final int     DEFAULT_PARALLEL_SEARCH  = 5;
    public static final int     DEFAULT_MAX_SIM_DOWNLOAD = 2;
    public static final boolean DEFAULT_CLEAR_DOWNLOAD   = false;
    public static final int     DEFAULT_SEARCH_ANIMATION_TIME = 45;

    // The property key name constants 
    public static final String TTL            = "TTL";
    public static final String SOFT_MAX_TTL   = "SOFT_MAX_TTL";
    public static final String MAX_TTL        = "MAX_TTL";
    public static final String MAX_LENGTH     = "MAX_LENGTH";
    public static final String TIMEOUT        = "TIMEOUT";
    public static final String KEEP_ALIVE     = "KEEP_ALIVE";
    public static final String PORT           = "PORT";
    public static final String SPEED          = "CONNECTION_SPEED";
    public static final String SEARCH_LIMIT   = "SEARCH_LIMIT";
    public static final String CLIENT_ID      = "CLIENT_ID";
    public static final String STATS          = "STATS";
    public static final String MAX_CONN       = "MAXIMUM_NUMBER_OF_CONNECTIONS";
    public static final String SAVE_DIRECTORY = "DIRECTORY_FOR_SAVING_FILES";
    public static final String DIRECTORIES    = "DIRECTORIES_TO_SEARCH_FOR_FILES";
    public static final String EXTENSIONS     = "EXTENSIONS_TO_SEARCH_FOR";
    public static final String BANNED_IPS     = "BLACK_LISTED_IP_ADDRESSES";
    public static final String BANNED_WORDS   = "BANNED_WORDS";
    public static final String FILTER_DUPLICATES = "FILTER_DUPLICATES";
    public static final String FILTER_ADULT   = "FILTER_ADULT";
    public static final String FILTER_HTML    = "FILTER_HTML";
    public static final String FILTER_VBS     = "FILTER_VBS";
    public static final String USE_QUICK_CONNECT = "USE_QUICK_CONNECT";
    public static final String QUICK_CONNECT_HOSTS = "QUICK_CONNECT_HOSTS";
    public static final String PARALLEL_SEARCH= "PARALLEL_SEARCH";
    public static final String MAX_SIM_DOWNLOAD="MAX_SIM_DOWNLOAD";
    public static final String CLEAR_DOWNLOAD = "CLEAR_DOWNLOAD";
    public static final String SEARCH_ANIMATION_TIME = "SEARCH_ANIMATION_TIME";

    public static final String HEADER = "Properties file for the LimeWire gnutella client.\nYou can modify any of the default properties here if\nyou wish, but if your modifications do not fit the\nrange of expected values for specific properties, those\nproperties will revert to their default values.\n\n";
}




