package com.limegroup.gnutella;

import java.io.*;
import java.net.*;
import java.util.Locale;
import java.util.Properties;
import com.sun.java.util.collections.*;
import java.lang.IllegalArgumentException;
import java.util.Enumeration;
import java.util.StringTokenizer;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.util.StringUtils;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.settings.*;

/**
 * This class is still used to manage some property settings, but it is
 * being phased out in favor of classes subclassed from AbstractSettings.
 * Any new settings should be added to one of those classes.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class SettingsManager {

    /**
	 * Constant member variable for the main <tt>Properties</tt> instance.
	 */
    private static final Properties PROPS = LimeProps.instance().getProperties();

	/**
	 * Default name of the shared directory.
	 */
	private final String  DIRECTORY_NAME_FOR_SAVING_FILES = "Shared";

    /**
	 * Default name for the network discovery properties
	 */
    private final String ND_PROPS_NAME  = "nd.props";

    /**
     * Constant <tt>File</tt> instance for the properties file
     */
    private final File PROPS_FILE = LimeProps.instance().getPropertiesFile();
    
    /**
     * Stored default values learned from reflecting upon the class.
     */
    private Map defaultValues;    

    /**
     * Time interval, after which the accumulated information expires
     */
    private final long EXPIRY_INTERVAL = 14 * 24 * 60 * 60 * 1000; //14 days

	private final boolean DEFAULT_ALLOW_BROWSER  = false;
    /** Default setting for the time to live */
    private final byte    DEFAULT_TTL            = (byte)4;

    /** Default maximum packet length */
    private final int     DEFAULT_MAX_LENGTH     = 65536;
    /** Default timeout for persistent HTTP connections */
    private final int     DEFAULT_PERSISTENT_HTTP_CONNECTION_TIMEOUT = 15000;
    /** Default port*/
    private final int     DEFAULT_PORT           = 6346;
    /** Default network connection speed */
    private final int     DEFAULT_CONNECTION_SPEED          = 56;
    private final int     DEFAULT_UPLOAD_SPEED   = 100;
    /** Default limit for the number of searches */
    private final byte    DEFAULT_SEARCH_LIMIT   = (byte)64;
    /** Default client guid */
    private final String  DEFAULT_CLIENT_ID      = null;
    /** Default maximum number of connections */
    private final int     DEFAULT_MAX_INCOMING_CONNECTION=4;
    /** Default time to expire incomplete files, in days. */
    private final int     DEFAULT_INCOMPLETE_PURGE_TIME = 7;

	/** the number of uplads allowed per person at a given time */
    private final int DEFAULT_UPLOADS_PER_PERSON=3;

    private final int DEFAULT_UPLOAD_QUEUE_SIZE = 10;

    /** default banned ip addresses */
    private final String[] DEFAULT_BLACK_LISTED_IP_ADDRESSES     = {};
    private final String[] DEFAULT_WHITE_LISTED_IP_ADDRESSES     = {};
    private final String[] DEFAULT_BANNED_WORDS   = {};
    private final boolean DEFAULT_FILTER_ADULT   = false;
    private final boolean DEFAULT_FILTER_DUPLICATES = true;
    /** Filter .vbs files? */
    private final boolean DEFAULT_FILTER_VBS     = true;
    /** Filter .htm[l] files? */
    private final boolean DEFAULT_FILTER_HTML    = false;
    private final boolean DEFAULT_FILTER_GREEDY_QUERIES = true;
    private final boolean DEFAULT_FILTER_HIGHBIT_QUERIES = true;

    private final int     DEFAULT_PARALLEL_SEARCH  = 5;
    private final int     DEFAULT_MAX_SIM_DOWNLOAD = 4;
    /** Default for whether user should be prompted before downloading exe's. */
    private final boolean DEFAULT_PROMPT_EXE_DOWNLOAD = true;
    private final int     DEFAULT_MAX_UPLOADS      = 20;
    private final int     DEFAULT_SOFT_MAX_UPLOADS = 5;
    private final boolean DEFAULT_CLEAR_UPLOAD     = true;
    private final boolean DEFAULT_CLEAR_DOWNLOAD   = false;
    public static final String  DEFAULT_CONNECT_STRING
		= "GNUTELLA CONNECT/0.4";
    private final String  DEFAULT_CONNECT_OK_STRING
		= "GNUTELLA OK";
    private final int     DEFAULT_BASIC_INFO_FOR_QUERY = 1000;
    private final int     DEFAULT_ADVANCED_INFO_FOR_QUERY = 50;

    private final boolean DEFAULT_CHECK_AGAIN        = true;
    private final boolean DEFAULT_FORCE_IP_ADDRESS   = false;
    private final String  DEFAULT_FORCED_IP_ADDRESS_STRING  = "0.0.0.0";
    private final int     DEFAULT_FORCED_PORT         = 6346;
    private final int     DEFAULT_FREELOADER_FILES    = 1;
    private final int     DEFAULT_FREELOADER_ALLOWED  = 100;

	/**
	 * Default average amount of time that the application is run,
	 * set at 20 minutes.
	 */
	private final long    DEFAULT_AVERAGE_UPTIME      = 20*60;

	/**
	 * Default total amount of time the application has been run,
	 * set at 20 minutes to begin with, based on the default
	 * setting of having been run for one session for the
	 * average number of seconds.
	 */
	private final long    DEFAULT_TOTAL_UPTIME        = 20*60;

    private final long    DEFAULT_LAST_SHUTDOWN_TIME  = 0;
    private final float   DEFAULT_FRACTIONAL_UPTIME   = 0.0f;

	/**
	 * Default value for whether or not some installation sequence,
	 * whether it be InstallShield, InstallAnywhere, or our own
	 * setup wizard, has set up the user's properties.
	 */
	private final boolean DEFAULT_INSTALLED = false;
	private final int DEFAULT_APP_WIDTH = 840;
	private final int DEFAULT_APP_HEIGHT = 600;
	private final boolean DEFAULT_RUN_ONCE = false;

	/**
	 * Default value for whether or not the application is minimized
	 * to the SystemTray when the user exits.  This flag is initialized
	 * to true only if the user platform supports the SystemTray, making
	 * minimize to tray the default shutdown behavior on tray enabled
	 * systems.  DEFAULT_MINIMIZE_TO_TRAY is the logical complement of
	 * DEFAULT_SHUTDOWN_AFTER_TRANSFERS insuring that only one default
	 * shutdown operation is set.
	 */
	private final boolean DEFAULT_MINIMIZE_TO_TRAY =
		CommonUtils.supportsTray();

	/**
	 * Default value for whether or not the application waits until
	 * transfers in progress are complete before shutting down.  This
	 * flag is initialized to true only if the user platform does not
	 * support the system tray, making shutdown after transfers the
	 * default shutdown behavior on systems which DO NOT support the
	 * SystemTray.  DEFAULT_SHUTDOWN_AFTER_TRANSFERS is the logical
	 * complement of DEFAULT_MINIMIZE_TO_TRAY insuring that only one
	 * default shutdown operation is set.
	 */ 
	private final boolean DEFAULT_SHUTDOWN_AFTER_TRANSFERS = 
        CommonUtils.isMacOSX() ? false :
		!DEFAULT_MINIMIZE_TO_TRAY;       

	/**
	 * The default name of the jar to load.
	 */
	public static final String DEFAULT_JAR_NAME = "LimeWire.jar";
	
	/**
	 * The default classpath.
	 */
	public static final String DEFAULT_CLASSPATH = DEFAULT_JAR_NAME;
	
	private final boolean DEFAULT_CHAT_ENABLED        = true;
    private final boolean DEFAULT_PLAYER_ENABLED      = true;
	private final String DEFAULT_LANGUAGE             = "";
	private final String DEFAULT_COUNTRY              = "";
	private final String DEFAULT_LOCALE_VARIANT       = "";
	
    private final boolean DEFAULT_MONITOR_VIEW_ENABLED = true;
    private final boolean DEFAULT_CONNECTION_VIEW_ENABLED = false;
    private final boolean DEFAULT_LIBRARY_VIEW_ENABLED = true;
    private final boolean DEFAULT_SHOPPING_VIEW_ENABLED = true;


    //authentication settings
    private final boolean DEFAULT_ACCEPT_AUTHENTICATED_CONNECTIONS_ONLY
        = false;
    private final String DEFAULT_COOKIES_FILE
        = "lib" + File.separator + "Cookies.dat";

    /** Specifies if the node is acting as server */
    private final boolean DEFAULT_SERVER = false;

	/**
	 * The default minimum number of stars for search results, on a scale
	 * of 0 to 3 inclusive.
	 */
	private final int DEFAULT_MINIMUM_SEARCH_QUALITY  = 0;

	/**
	 * Value for the default minimum speed to allow in search results.
	 */
	private final int DEFAULT_MINIMUM_SEARCH_SPEED = 0;

	/**
	 * Constant default value for the maximum number of bytes ever passed
	 * per second upstream.
	 */
	private final int DEFAULT_MAX_UPLOAD_BYTES_PER_SEC = 0;

	/**
	 * Constant default value for the maximum number of bytes ever passed
	 * per second downstream.
	 */
	private final int DEFAULT_MAX_DOWNLOAD_BYTES_PER_SEC = 0;

	/**
	 * Default value for the number of times the application has been
	 * run on this machine.
	 */
	private final int DEFAULT_SESSIONS = 1;

    /**
     * The time when we last expired accumulated information
     */
    private final long DEFAULT_LAST_EXPIRE_TIME = 0L;

	/**
	 * Constant for the default save directory.
	 */
	private final File DEFAULT_DIRECTORY_FOR_SAVING_FILES =
	    new File(CommonUtils.getUserHomeDir(), DIRECTORY_NAME_FOR_SAVING_FILES);


	/**
	 * Default directories for file searching.
	 */
    private final String  DEFAULT_DIRECTORIES_TO_SEARCH_FOR_FILES =
        DEFAULT_DIRECTORY_FOR_SAVING_FILES.getAbsolutePath();

    /**
     * Default file extensions.
     */
    private final String  DEFAULT_EXTENSIONS_TO_SEARCH_FOR =
		"asx;html;htm;xml;txt;pdf;ps;rtf;doc;tex;mp3;mp4;wav;wax;au;aif;aiff;"+
		"ra;ram;wma;wm;wmv;mp2v;mlv;mpa;mpv2;mid;midi;rmi;aifc;snd;"+
		"mpg;mpeg;asf;qt;mov;avi;mpe;swf;dcr;gif;jpg;jpeg;jpe;png;tif;tiff;"+
		"exe;zip;gz;gzip;hqx;tar;tgz;z;rmj;lqt;rar;ace;sit;smi;img;ogg;rm;"+
		"bin;dmg;jve;nsv;med;mod;7z;iso";

    // The property key name constants
	private final String ALLOW_BROWSER         = "ALLOW_BROWSER";
    private final String TTL                   = "TTL";
    private final String MAX_LENGTH            = "MAX_LENGTH";
    private final String TIMEOUT               = "TIMEOUT";
    private final String PERSISTENT_HTTP_CONNECTION_TIMEOUT
        = "PERSISTENT_HTTP_CONNECTION_TIMEOUT";
    private final String PORT                  = "PORT";
    private final String CONNECTION_SPEED      = "CONNECTION_SPEED";
    private final String UPLOAD_SPEED          = "UPLOAD_SPEED";
    private final String SEARCH_LIMIT          = "SEARCH_LIMIT";
    private final String CLIENT_ID             = "CLIENT_ID";
    private final String MAX_INCOMING_CONNECTIONS
		= "MAX_INCOMING_CONNECTIONS";
    private final String DIRECTORY_FOR_SAVING_FILES
		= "DIRECTORY_FOR_SAVING_FILES";
    private final String INCOMPLETE_PURGE_TIME = "INCOMPLETE_PURGE_TIME";
    private final String DIRECTORIES_TO_SEARCH_FOR_FILES
		= "DIRECTORIES_TO_SEARCH_FOR_FILES";
    private final String EXTENSIONS_TO_SEARCH_FOR
		= "EXTENSIONS_TO_SEARCH_FOR";
    private final String BLACK_LISTED_IP_ADDRESSES
		= "BLACK_LISTED_IP_ADDRESSES";
    private final String WHITE_LISTED_IP_ADDRESSES            
		= "WHITE_LISTED_IP_ADDRESSES";
    private final String BANNED_WORDS          = "BANNED_WORDS";
    private final String FILTER_DUPLICATES     = "FILTER_DUPLICATES";
    private final String FILTER_ADULT          = "FILTER_ADULT";
    private final String FILTER_HTML           = "FILTER_HTML";
    private final String FILTER_VBS            = "FILTER_VBS";
    private final String FILTER_GREEDY_QUERIES = "FILTER_GREEDY_QUERIES";
    private final String FILTER_HIGHBIT_QUERIES
		= "FILTER_HIGHBIT_QUERIES";
    private final String PARALLEL_SEARCH       = "PARALLEL_SEARCH";
    private final String MAX_SIM_DOWNLOAD      = "MAX_SIM_DOWNLOAD";
    private final String PROMPT_EXE_DOWNLOAD   = "PROMPT_EXE_DOWNLOAD";
    private final String MAX_UPLOADS           = "MAX_UPLOADS";
    private final String SOFT_MAX_UPLOADS      = "SOFT_MAX_UPLOADS";
    private final String CLEAR_UPLOAD          = "CLEAR_UPLOAD";
    private final String CLEAR_DOWNLOAD        = "CLEAR_DOWNLOAD";

    private final String CONNECT_STRING        = "CONNECT_STRING";
    private final String CONNECT_OK_STRING     = "CONNECT_OK_STRING";
    private final String CHECK_AGAIN           = "CHECK_AGAIN";
    private final String BASIC_QUERY_INFO      = "BASIC_QUERY_INFO";
    private final String ADVANCED_QUERY_INFO   = "ADVANCED_QUERY_INFO";
    private final String FORCE_IP_ADDRESS      = "FORCE_IP_ADDRESS";
    private final String FORCED_IP_ADDRESS_STRING
        = "FORCED_IP_ADDRESS_STRING";
    private final String FORCED_PORT           = "FORCED_PORT";
    private final String FREELOADER_FILES      = "FREELOADER_FILES";
    private final String FREELOADER_ALLOWED    = "FREELOADER_ALLOWED";

    private final String UPLOADS_PER_PERSON    = "UPLOADS_PER_PERSON";
    private final String UPLOAD_QUEUE_SIZE     = "UPLOAD_QUEUE_SIZE";
    private final String AVERAGE_UPTIME        = "AVERAGE_UPTIME";
    private final String TOTAL_UPTIME          = "TOTAL_UPTIME";
    private final String SESSIONS              = "SESSIONS";
    private final String LAST_SHUTDOWN_TIME    = "LAST_SHUTDOWN_TIME";
    private final String FRACTIONAL_UPTIME     = "FRACTIONAL_UPTIME";
	private final String INSTALLED             = "INSTALLED";
	private final String APP_WIDTH             = "APP_WIDTH";
	private final String APP_HEIGHT            = "APP_HEIGHT";
	private final String RUN_ONCE              = "RUN_ONCE";
	private final String WINDOW_X              = "WINDOW_X";
	private final String WINDOW_Y              = "WINDOW_Y";
	private final String MINIMIZE_TO_TRAY      = "MINIMIZE_TO_TRAY";
	private final String SHUTDOWN_AFTER_TRANSFERS = "SHUTDOWN_AFTER_TRANSFERS";

	// this is necessary for pre-LW 2.4.0 RunLime classes
	private final String CLASSPATH = "CLASSPATH";

	/**
	 * This key for the jar file to load to start the program.
	 */
	private final String JAR_NAME = "JAR_NAME";

	/**
	 * Constant key for whether or not chat is enabled.
	 */
	private final String CHAT_ENABLED = "CHAT_ENABLED";

	/**
	 * Constant key for whether or not the internal player is enabled.
	 */
	private final String PLAYER_ENABLED = "PLAYER_ENABLED";

	/**
	 * Constant key for whether or not the Monitor Tab is enabled.
	 */
	private final String MONITOR_VIEW_ENABLED = "MONITOR_VIEW_ENABLED";

	/**
	 * Constant key for whether or not the Connection Tab is enabled.
	 */
	private final String CONNECTION_VIEW_ENABLED = "CONNECTION_VIEW_ENABLED";

	/**
	 * Constant key for whether or not the Library Tab is enabled.
	 */
	private final String LIBRARY_VIEW_ENABLED = "LIBRARY_VIEW_ENABLED";

	/**
	 * Constant key for whether or not the Shopping Tab is enabled.
	 */
	private final String SHOPPING_VIEW_ENABLED = "SHOPPING_VIEW_ENABLED";

	/**
	 * Constant key for the language we're currently using.
	 */
	private final String LANGUAGE = "LANGUAGE";


	/**
	 * Constant key for the country.
	 */
	private final String COUNTRY = "COUNTRY";
    
	/**
	 * Constant key for the locale variant.
	 */
	private final String LOCALE_VARIANT = "LOCALE_VARIANT";


    //authentication settings
    private final String ACCEPT_AUTHENTICATED_CONNECTIONS_ONLY
        = "ACCEPT_AUTHENTICATED_CONNECTIONS_ONLY";

    /**
     * The property that denotes the file that stores the
     * Schema Transformation DataMap
     */
    private final String COOKIES_FILE = "COOKIES_FILE";

    /** Specifies if the node is acting as server */
    private final String SERVER = "SERVER";

	/**
	 * Constant key for the minimum quality to allow in search results.
	 */
	private final String MINIMUM_SEARCH_QUALITY =
		"MINIMUM_SEARCH_QUALITY";

	/**
	 * Constant key for the minimum speed to allow in search results.
	 */
	private final String MINIMUM_SEARCH_SPEED =
		"MINIMUM_SEARCH_SPEED";

	/**
	 * Constant key for the maximum number of bytes per second ever passed
	 * upstream.
	 */
	private final String MAX_UPLOAD_BYTES_PER_SEC =
		"MAX_UPLOAD_BYTES_PER_SEC";

	/**
	 * Constant key for the maximum number of bytes per second ever passed
	 * downstream.
	 */
	private final String MAX_DOWNLOAD_BYTES_PER_SEC =
		"MAX_DOWNLOAD_BYTES_PER_SEC";

    /**
     * Property that denotes the time when we last expired accumulated
     * information
     */
    private final String LAST_EXPIRE_TIME = "LAST_EXPIRE_TIME";

	/** Variables for the various settings */
    private volatile boolean  _forceIPAddress;
    private volatile byte[]   _forcedIPAddress;
    private volatile int      _forcedPort;
	private volatile boolean  _allowBroswer;
    private volatile byte     _ttl;
    private volatile int      _maxLength;
    private volatile int      _persistentHTTPConnectionTimeout;
    private volatile String   _hostList;
    private volatile int      _port;
    private volatile int      _connectionSpeed;
    private volatile int      _uploadSpeed;
    private volatile byte     _searchLimit;
    private volatile String   _clientID;
    private volatile File     _saveDirectory;
    private volatile File     _incompleteDirectory;
    private volatile File[]   _directories = new File[0];
    private volatile int      _incompletePurgeTime;
    private volatile String   _extensions;
    private volatile String[] _bannedIps;
    private volatile String[] _allowedIps;
    private volatile String[] _bannedWords;
    private volatile boolean  _filterDuplicates;
    private volatile boolean  _filterAdult;
    private volatile boolean  _filterVbs;
    private volatile boolean  _filterHtml;
    private volatile boolean  _filterGreedyQueries;
    private volatile boolean  _filterBearShare;
    private volatile int      _parallelSearchMax;
    private volatile boolean  _clearCompletedUpload;
    private volatile boolean  _clearCompletedDownload;
    private volatile int      _maxSimDownload;
    private volatile boolean  _promptExeDownload;
    private volatile int      _maxUploads;
    private volatile int      _softMaxUploads;
    private volatile int      _uploadsPerPerson;
    private volatile int      _uploadQueueSize;

	private volatile boolean  _chatEnabled;
	private volatile boolean  _playerEnabled;

    private volatile boolean  _monitorViewEnabled;
    private volatile boolean  _connectionViewEnabled;
    private volatile boolean  _libraryViewEnabled;
    private volatile boolean  _shoppingViewEnabled;

    /** connectString_ is something like "GNUTELLA CONNECT..."
     *  connectStringOk_ is something like "GNUTELLA OK..."
     *  INVARIANT: connectString_=connectStringFirstWord_+"
     *             "+connectStringRemainder_
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
	private volatile long     _averageUptime;
	private volatile long     _totalUptime;
	private volatile int      _sessions;

	private volatile boolean  _installed;
	private volatile boolean  _acceptedIncoming = false;


    /** Specifies if the node is acting as server */
    private volatile boolean _server;

    /**
	 * Specialized constant properties file for the network discoverer.
     */
    private final Properties ND_PROPS = new Properties();;

    /**
     * Set up the manager instance to follow the singleton pattern.
     */
    private static final SettingsManager INSTANCE = new SettingsManager();

    /**
	 * Returns the <tt>SettingsManager</tt> instance.
	 *
	 * @return the <tt>SettingsManager</tt> instance
     */
    public static SettingsManager instance() {
        return INSTANCE;
    }

    /**
	 * Private constructor to ensure that this can only be constructed
	 * from inside this class.
     */
    private SettingsManager() {
        // load the specialized property file for network discovery
        try {
            FileInputStream ndfis = new FileInputStream(new File(ND_PROPS_NAME));
            try {ND_PROPS.load(ndfis);}
            catch(IOException ioe) {}
        }
        catch(FileNotFoundException fne){}
        catch(SecurityException se) {}

        // load the main application properties file
        Properties tempProps = new Properties();
        try {
            FileInputStream fis = new FileInputStream(PROPS_FILE);
            try {
                tempProps.load(fis);
                loadDefaults();
                try {
                    fis.close();
                    validateFile(tempProps);
                } catch(IOException e) {
			        // error closing the file, so continue using the
			        // defaults.
				}
            } catch(IOException e){loadDefaults();}
        }
        catch(FileNotFoundException fnfe){loadDefaults();}
        catch(SecurityException se){loadDefaults();}        
        try {
            String language = getLanguage();
            String country = getCountry();
            String localeVariant = getLocaleVariant();
            Locale.setDefault(new Locale(language, country, localeVariant));
        } catch(Exception e) {
            System.out.println("Could not set new default locale.");
        }
        
        //reset the values that have expired
        resetExpiredValues();
    }

    /**
     * Resets the expired values to defaults
     */
    private void resetExpiredValues(){
        //if hasnt expired, return
        if(System.currentTimeMillis() - getLastExpireTime() < EXPIRY_INTERVAL)
            return;

        //change the last expired time
        setLastExpireTime(System.currentTimeMillis());
        //reset the expired values;
        setAverageUptime(DEFAULT_AVERAGE_UPTIME);
        setMaxUpstreamBytesPerSec(DEFAULT_MAX_UPLOAD_BYTES_PER_SEC);
		setMaxDownstreamBytesPerSec(DEFAULT_MAX_DOWNLOAD_BYTES_PER_SEC);

		ConnectionSettings.EVER_ACCEPTED_INCOMING.revertToDefault();
		UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.revertToDefault();
    }

    /**
	 * Sets all of the properties manually to ensure that each
	 * property is valid.
	 *
	 * @param tempProps the temporary <tt>Properties</tt> file containing
	 *                  values that will be validated before being added
	 *                  to the "master" <tt>Properties</tt> file
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
				if(key.equals(ALLOW_BROWSER)) {
					boolean bs;
					if (p.equals("true"))
                        bs=true;
                    else if (p.equals("false"))
                        bs=false;
                    else
                        break;
                    setAllowBrowser(bs);
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
                else if(key.equals(SOFT_MAX_UPLOADS)) {
                    setSoftMaxUploads(Integer.parseInt(p));
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
                else if(key.equals(PERSISTENT_HTTP_CONNECTION_TIMEOUT)) {
                    setPersistentHTTPConnectionTimeout(Integer.parseInt(p));
                }
				else if(key.equals(UPLOADS_PER_PERSON)){
					setUploadsPerPerson(Integer.parseInt(p));
				}
                else if(key.equals(UPLOAD_QUEUE_SIZE)) {
                    setUploadQueueSize(Integer.parseInt(p));
                }
                else if(key.equals(PORT)) {
                    setPort(Integer.parseInt(p));
                }
                else if(key.equals(CONNECTION_SPEED)) {
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
				else if(key.equals(PLAYER_ENABLED)) {
					boolean bs;
                    if (p.equals("true"))
                        bs=true;
                    else if (p.equals("false"))
                        bs=false;
                    else
                        break;
                    setPlayerEnabled(bs);
				}
				else if(key.equals(MONITOR_VIEW_ENABLED)) {
					boolean bs;
                    if (p.equals("true"))
                        bs=true;
                    else if (p.equals("false"))
                        bs=false;
                    else
                        break;
                    setMonitorViewEnabled(bs);
				}
				else if(key.equals(CONNECTION_VIEW_ENABLED)) {
					boolean bs;
                    if (p.equals("true"))
                        bs=true;
                    else if (p.equals("false"))
                        bs=false;
                    else
                        break;
                    setConnectionViewEnabled(bs);
				}
				else if(key.equals(LIBRARY_VIEW_ENABLED)) {
					boolean bs;
                    if (p.equals("true"))
                        bs=true;
                    else if (p.equals("false"))
                        bs=false;
                    else
                        break;
                    setLibraryViewEnabled(bs);
				}
				else if(key.equals(SHOPPING_VIEW_ENABLED)) {
					boolean bs;
                    if (p.equals("true"))
                        bs=true;
                    else if (p.equals("false"))
                        bs=false;
                    else
                        break;
                    setShoppingViewEnabled(bs);
				}
                else if(key.equals(CLIENT_ID)) {
                    setClientID(p);
                }
                else if(key.equals(DIRECTORY_FOR_SAVING_FILES)) {
					try {
						setSaveDirectory(new File(p));
					} catch(IOException e) {
						try {
							setSaveDirectory(DEFAULT_DIRECTORY_FOR_SAVING_FILES);
						} catch(IOException ioe) {
							// this should never happen
						}
                        e.printStackTrace();
                        // this should never happen unless the user manually
                        // enters the save directory in the props file or
                        // if the user changes directory permissions
					}
                }

                else if(key.equals(INCOMPLETE_PURGE_TIME)) {
                    setIncompletePurgeTime(Integer.parseInt(p));
                }

                else if(key.equals(DIRECTORIES_TO_SEARCH_FOR_FILES)) {
                    setDirectories(p);
                }

                else if(key.equals(EXTENSIONS_TO_SEARCH_FOR)) {
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
                else if(key.equals(BLACK_LISTED_IP_ADDRESSES)) {
                    setBannedIps(decode(p));
                }
                else if(key.equals(WHITE_LISTED_IP_ADDRESSES)) {
                    setAllowedIps(decode(p));
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

                else if(key.equals(FILTER_HIGHBIT_QUERIES)) {
                    boolean bs;
                    if (p.equals("true"))
                        bs=true;
                    else if (p.equals("false"))
                        bs=false;
                    else
                        break;
                    setFilterBearShareQueries(bs);
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
				else if(key.equals(SESSIONS)) {
					setSessions(Integer.parseInt(p) + 1);
				}
				else if(key.equals(AVERAGE_UPTIME)) {
					setAverageUptime(Long.parseLong(p));
				}
				else if(key.equals(TOTAL_UPTIME)) {
					setTotalUptime(Long.parseLong(p));
				}
                else if(key.equals(LAST_SHUTDOWN_TIME)) {
                    setLastShutdownTime(Long.parseLong(p));
                }
                else if(key.equals(FRACTIONAL_UPTIME)) {
                    setFractionalUptime(Float.valueOf(p).floatValue());
                }
				else if(key.equals(INSTALLED)) {
					Boolean installed = Boolean.valueOf(p);
					setInstalled(installed.booleanValue());
				}
				else if(key.equals(APP_WIDTH)) {
					setAppWidth(Integer.parseInt(p));
				}
				else if(key.equals(APP_HEIGHT)) {
					setAppHeight(Integer.parseInt(p));
				}
				else if(key.equals(RUN_ONCE)) {
					Boolean runOnce = Boolean.valueOf(p);
					setRunOnce(runOnce.booleanValue());
				}

				else if(key.equals(WINDOW_X)) {
					setWindowX(Integer.parseInt(p));
				}
				else if(key.equals(WINDOW_Y)) {
					setWindowY(Integer.parseInt(p));
				}

				else if(key.equals(MINIMIZE_TO_TRAY)) {
					Boolean minimize = Boolean.valueOf(p);
					setMinimizeToTray(minimize.booleanValue());
				}

				else if(key.equals(SHUTDOWN_AFTER_TRANSFERS)) {
					Boolean afterTransfers = Boolean.valueOf(p);
					setShutdownAfterTransfers(afterTransfers.booleanValue());
				}
				else if(key.equals(LANGUAGE)) {
					setLanguage(p);
				}
				else if(key.equals(COUNTRY)) {
					setCountry(p);
				}
				else if(key.equals(LOCALE_VARIANT)) {
					setLocaleVariant(p);
				}
				else if(key.equals(MINIMUM_SEARCH_QUALITY)) {
					setMinimumSearchQuality(Integer.parseInt(p));
				}
				else if(key.equals(MINIMUM_SEARCH_SPEED)) {
					setMinimumSearchSpeed(Integer.parseInt(p));
				}
				else if(key.equals(MAX_UPLOAD_BYTES_PER_SEC)) {
					setMaxUpstreamBytesPerSec(Integer.parseInt(p));
				}
				else if(key.equals(MAX_DOWNLOAD_BYTES_PER_SEC)) {
					setMaxDownstreamBytesPerSec(Integer.parseInt(p));
				}
                else if(key.equals(LAST_EXPIRE_TIME)){
                    setLastExpireTime((new Long(p)).longValue());
                }
                else if(key.equals(ACCEPT_AUTHENTICATED_CONNECTIONS_ONLY)){
                    setAcceptAuthenticatedConnectionsOnly(
                        (Boolean.valueOf(p)).booleanValue());
                }
                else if(key.equals(COOKIES_FILE)){
                   setCookiesFile(p);
                }
                else if(key.equals(SERVER)){
                    setServer((Boolean.valueOf(p)).booleanValue());
                }
				else if(key.equals(JAR_NAME)) {
					setJarName(p);
				}
				else if(key.equals(CLASSPATH)) {
					setClasspath(p);
				}
			}
			catch(NumberFormatException nfe){ /* continue */ }
			catch(IllegalArgumentException iae){ /* continue */ }
			catch(ClassCastException cce){ /* continue */ }
		}
	}

    /**
	 * Load in the default values.  Any properties written to the real
     * properties file will overwrite these. This method ensures that some
     * reasonable values are always loaded even in the case of any
     * failure in loading the properties file from disk.
	 */
    public void loadDefaults() {
        
        reflectUponDefaults();
        
		setAllowBrowser(DEFAULT_ALLOW_BROWSER);
        setTTL(DEFAULT_TTL);
        setMaxLength(DEFAULT_MAX_LENGTH);
        setPersistentHTTPConnectionTimeout(
            DEFAULT_PERSISTENT_HTTP_CONNECTION_TIMEOUT);
        setPort(DEFAULT_PORT);
        setConnectionSpeed(DEFAULT_CONNECTION_SPEED);
        setUploadSpeed(DEFAULT_UPLOAD_SPEED);
        setSearchLimit(DEFAULT_SEARCH_LIMIT);
        setClientID( (new GUID(Message.makeGuid())).toHexString() );
        setBannedIps(DEFAULT_BLACK_LISTED_IP_ADDRESSES);
        setAllowedIps(DEFAULT_WHITE_LISTED_IP_ADDRESSES);
        setBannedWords(DEFAULT_BANNED_WORDS);
        setFilterAdult(DEFAULT_FILTER_ADULT);
        setFilterDuplicates(DEFAULT_FILTER_DUPLICATES);
        setFilterVbs(DEFAULT_FILTER_VBS);
        setFilterHtml(DEFAULT_FILTER_HTML);
        setFilterGreedyQueries(DEFAULT_FILTER_GREEDY_QUERIES);
        setExtensions(DEFAULT_EXTENSIONS_TO_SEARCH_FOR);
        setBannedIps(DEFAULT_BLACK_LISTED_IP_ADDRESSES);
        setBannedWords(DEFAULT_BANNED_WORDS);
        setFilterAdult(DEFAULT_FILTER_ADULT);
        setFilterDuplicates(DEFAULT_FILTER_DUPLICATES);
        setFilterVbs(DEFAULT_FILTER_VBS);
        setFilterHtml(DEFAULT_FILTER_HTML);
        setFilterGreedyQueries(DEFAULT_FILTER_GREEDY_QUERIES);
        setFilterBearShareQueries(DEFAULT_FILTER_HIGHBIT_QUERIES);
        setParallelSearchMax(DEFAULT_PARALLEL_SEARCH);
        setClearCompletedUpload(DEFAULT_CLEAR_UPLOAD);
        setClearCompletedDownload(DEFAULT_CLEAR_DOWNLOAD);
        setMaxSimDownload(DEFAULT_MAX_SIM_DOWNLOAD);
        setPromptExeDownload(DEFAULT_PROMPT_EXE_DOWNLOAD);
        setMaxUploads(DEFAULT_MAX_UPLOADS);
        setSoftMaxUploads(DEFAULT_SOFT_MAX_UPLOADS);
        setConnectString(DEFAULT_CONNECT_STRING);
        setConnectOkString(DEFAULT_CONNECT_OK_STRING);

        setCheckAgain(DEFAULT_CHECK_AGAIN);
        setBasicInfoForQuery(DEFAULT_BASIC_INFO_FOR_QUERY);
        setAdvancedInfoForQuery(DEFAULT_ADVANCED_INFO_FOR_QUERY);
        setForceIPAddress(DEFAULT_FORCE_IP_ADDRESS);
        setForcedIPAddressString(DEFAULT_FORCED_IP_ADDRESS_STRING);
        setForcedPort(DEFAULT_FORCED_PORT);
        setFreeloaderFiles(DEFAULT_FREELOADER_FILES);
        setFreeloaderAllowed(DEFAULT_FREELOADER_ALLOWED);

		setUploadsPerPerson(DEFAULT_UPLOADS_PER_PERSON);
        setUploadQueueSize(DEFAULT_UPLOAD_QUEUE_SIZE);
		setAverageUptime(DEFAULT_AVERAGE_UPTIME);
		setTotalUptime(DEFAULT_TOTAL_UPTIME);
        setLastShutdownTime(DEFAULT_LAST_SHUTDOWN_TIME);
        setFractionalUptime(DEFAULT_FRACTIONAL_UPTIME);
		setInstalled(DEFAULT_INSTALLED);
		setRunOnce(DEFAULT_RUN_ONCE);
		setMinimizeToTray(DEFAULT_MINIMIZE_TO_TRAY);
		setShutdownAfterTransfers(DEFAULT_SHUTDOWN_AFTER_TRANSFERS);

		setAppWidth(DEFAULT_APP_WIDTH);
		setAppHeight(DEFAULT_APP_HEIGHT);

		setChatEnabled(DEFAULT_CHAT_ENABLED);
		setPlayerEnabled(DEFAULT_PLAYER_ENABLED);

        //defaults for tabs...
        setMonitorViewEnabled(DEFAULT_MONITOR_VIEW_ENABLED);
        setConnectionViewEnabled(DEFAULT_CONNECTION_VIEW_ENABLED);
        setLibraryViewEnabled(DEFAULT_LIBRARY_VIEW_ENABLED);
        setShoppingViewEnabled(DEFAULT_SHOPPING_VIEW_ENABLED);

		setLanguage(DEFAULT_LANGUAGE);
		setCountry(DEFAULT_COUNTRY);
		setLocaleVariant(DEFAULT_LOCALE_VARIANT);

		setMinimumSearchQuality(DEFAULT_MINIMUM_SEARCH_QUALITY);
		setMinimumSearchSpeed(DEFAULT_MINIMUM_SEARCH_SPEED);
		setMaxUpstreamBytesPerSec(DEFAULT_MAX_UPLOAD_BYTES_PER_SEC);
		setMaxDownstreamBytesPerSec(DEFAULT_MAX_DOWNLOAD_BYTES_PER_SEC);

        //authentication settings
        setAcceptAuthenticatedConnectionsOnly(
            DEFAULT_ACCEPT_AUTHENTICATED_CONNECTIONS_ONLY);
        setCookiesFile(DEFAULT_COOKIES_FILE);

        setServer(DEFAULT_SERVER);

		setSessions(DEFAULT_SESSIONS);
		setAverageUptime(DEFAULT_AVERAGE_UPTIME);
		setTotalUptime(DEFAULT_TOTAL_UPTIME);
        setIncompletePurgeTime(DEFAULT_INCOMPLETE_PURGE_TIME);
        setLastExpireTime(DEFAULT_LAST_EXPIRE_TIME);
		setJarName(DEFAULT_JAR_NAME);
		setClasspath(DEFAULT_CLASSPATH);
    }
    
    /**
     * Dynamically scans this class to load up
     * a HashMap with default values.
     * Used to determine whether or not a value is default
     * at save them (to know if we should save it or not)
     */
    private void reflectUponDefaults() {
        // get a list of all the fields in this class
        Field[] fields = SettingsManager.class.getDeclaredFields();
        String theKey;
        String theValue;
        Class theClass;
        
        defaultValues = new HashMap( PROPS.size() );
        
        for( int i = 0; i < fields.length; i++) {
            // if this particular one is a default value ...
            if( fields[i].getName().startsWith("DEFAULT_")) {
                //assume the rest of its name is the key.
                theKey = fields[i].getName().substring(8);
                // now we have to mutate the value based on its type
                theClass = fields[i].getType();
                try {
                    if( theClass == String.class )
                        theValue = (String)fields[i].get(this);
                    else if (theClass == Boolean.TYPE )
                        theValue = fields[i].getBoolean(this) ? 
                            Boolean.TRUE.toString() : Boolean.FALSE.toString();
                    else if (theClass == Byte.TYPE )
                        theValue = Byte.toString(fields[i].getByte(this));
                    else if (theClass == Character.TYPE )
                        theValue = new Character(fields[i].getChar(this)).toString();
                    else if (theClass == Double.TYPE )
                        theValue = Double.toString(fields[i].getDouble(this));
                    else if (theClass == Float.TYPE )
                        theValue = Float.toString(fields[i].getFloat(this));
                    else if (theClass == Integer.TYPE )
                        theValue = Integer.toString(fields[i].getInt(this));
                    else if (theClass == Long.TYPE )
                        theValue = Long.toString(fields[i].getLong(this));
                    else if (theClass == Short.TYPE )
                        theValue = Short.toString(fields[i].getShort(this));
                    else
                        continue; //ignore anything else (including arrays)
                    // add this key/value pair to the default hashMap
                    defaultValues.put( theKey, theValue );
                } catch (Exception ignored) { }
            }
        }
    }
    
    /**
     * Determine whether or not this key is currently
     * stored as its default value.
     */
    public boolean isDefault(String theKey) {
        if ( defaultValues == null || PROPS == null )
            return false;
        String val = PROPS.getProperty(theKey);
        if (val == null)
            return false;
        return val.equals(defaultValues.get(theKey));
    }

    /**
	 * Returns whether or not uploads to browsers should be allowed.
	 *
	 * @return <tt>true</tt> is uploads to browsers should be allowed,
	 *         <tt>false</tt> otherwise
	 */
	public boolean getAllowBrowser() {return _allowBroswer;}

    /** Returns the time to live -- this is only really still here
	 *  for testing purposes.
	 */
    public byte getTTL(){return _ttl;}

    /** Returns the maximum allowable length of packets*/
    public int getMaxLength(){return _maxLength;}

    /** Returns the timeout value for persistent HTTP connections*/
    public int getPersistentHTTPConnectionTimeout(){
        return _persistentHTTPConnectionTimeout;
    }

    /** Returns the client's port number */
    public int getPort(){return _port;}

    /** Returns the client's connection speed in kilobits/sec
     *  (not kilobytes/sec) */
    public int getConnectionSpeed(){return _connectionSpeed;}

    public int getUploadSpeed() { return _uploadSpeed; }

    /** Returns the client's search speed */
    public byte getSearchLimit(){return _searchLimit;}

    /** Returns the client id number */
    public String getClientID(){return _clientID;}

	/** Returns the maximum number of uploads per person */
    public int getUploadsPerPerson(){return _uploadsPerPerson;}
    
    /** Returns the size of the upload queue */
    public int getUploadQueueSize() {return  _uploadQueueSize;}

    /**
	 * Returns a new <tt>File</tt> instance that denotes the abstract
	 * pathname of the directory for saving files.
	 *
	 * @return  A <tt>File</tt> instance denoting the abstract
	 *          pathname of the save directory.
	 *
	 * @throws  <tt>FileNotFoundException</tt>
	 *          If the incomplete directory is <tt>null</tt>.
	 */
    public File getSaveDirectory() throws FileNotFoundException {
		if(_saveDirectory == null) throw new FileNotFoundException();
		return _saveDirectory;
	}

	/**
	 * Returns the <tt>File</tt> instance denoting the abstract pathname
	 * of the default save directory.
	 *
	 * @return the <tt>File</tt> instance denoting the abstract pathname
	 * of the default save directory
	 */
	public File getSaveDefault() {
	    return DEFAULT_DIRECTORY_FOR_SAVING_FILES;
	}

	/** Returns true if the chat is enabled */
	public boolean getChatEnabled() {return _chatEnabled;}


	/** Returns true if the player is enabled */
	public boolean getPlayerEnabled() {
        if (CommonUtils.isMacClassic())
            return false;
        return _playerEnabled;
    }

	/** Returns true if the Monitor Tab should be enabled */
	public boolean getMonitorViewEnabled() {
        return _monitorViewEnabled;
    }

	/** Returns true if the Connection Tab should be enabled */
	public boolean getConnectionViewEnabled() {
        return _connectionViewEnabled;
    }

	/** Returns true if the Library Tab should be enabled */
	public boolean getLibraryViewEnabled() {
        return _libraryViewEnabled;
    }

	/** Returns true if the Shopping Tab should be enabled */
	public boolean getShoppingViewEnabled() {
        return _shoppingViewEnabled;
    }


    /**
	 * Returns a new <tt>File</tt> instance that denotes the abstract
	 * pathname of the directory for saving incomplete files.
	 *
	 * @return  A <tt>File</tt> instance denoting the abstract
	 *          pathname of the directory for saving incomplete files.
	 *
	 * @throws  <tt>FileNotFoundException</tt>
	 *          If the incomplete directory is <tt>null</tt>.
	 */
    public File getIncompleteDirectory() throws FileNotFoundException {
		if(_incompleteDirectory == null) throw new FileNotFoundException();
		return _incompleteDirectory;
    }

    /** Returns the minimum age of incomplete files, in days, before they
     *  are deleted from disk. */
    public int getIncompletePurgeTime() {
        return _incompletePurgeTime;
    }

    /**
	 * Returns the directories to search as an array of <tt>File</tt>
	 * instances.
	 *
	 * @return the directories to search as an array of <tt>File</tt>
	 *  instances
	 */
    public File[] getDirectories() {
		return _directories;
	}

	/**
	 * Returns an array of Strings of directory path names.  these are the
	 * pathnames of the shared directories as well as the pathname of
	 * the Incomplete directory.
     *
     * @return the array of <tt>File</tt> instances denoting the abstract
     *  pathnames of the shared directories, with the <tt>File</tt>
     *  instance denoting the abstract pathname of the incomplete directory
     *  appended to the end (the last <tt>File</tt> instance in the array),
     *  unless obtaining the incomplete directory throws an exception, in
     *  which case this will simply return the array of shared directories
	 */
	public File[] getDirectoriesWithIncompleteAsArray() {
        int newLength = _directories.length + 1;
        File[] newFiles = new File[newLength];
        File incompleteDir = null;
		try {
			incompleteDir = getIncompleteDirectory();
            newFiles[_directories.length] = incompleteDir;
            for(int i=0; i<_directories.length; i++) {
                newFiles[i] = _directories[i];
            }
		} catch(FileNotFoundException fnfe) {
            return _directories;
		}
		return newFiles;
	}

    /**
	 * Returns a new <tt>File</tt> instance that denotes the abstract
	 * pathname of the file with a snapshot of current downloading files.
	 *
	 * <p>This file is stored in the incomplete directory and is a read-only
     * property.
	 *
	 * @return  A <tt>File</tt> instance denoting the abstract
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


    /** Returns the string of file extensions*/
    public String getExtensions(){return _extensions;}

    /** Returns the string of default file extensions to share.*/
    public String getDefaultExtensions() {
		return DEFAULT_EXTENSIONS_TO_SEARCH_FOR;
	}

    /**
     * Returns an array of strings denoting the ip addresses that the
     * user has banned.
     *
     * @return an array of strings denoting banned ip addresses
     */
    public String[] getBannedIps(){return _bannedIps;}
    
    /**
     * Returns an array of strings denoting the ip addresses that the
     * user has allowed.
     *  
     * @return an array of strings denoting allowed ip addresses
     */
    public String[] getAllowedIps(){return _allowedIps;}
    
    /**
     * Returns an array of strings denoting words that the user has banned.
     *
     * @return an array of strings that the user has banned
     */
    public String[] getBannedWords(){return _bannedWords;}

    /**
     * Returns a <tt>boolean</tt> value indicating whether or not "adult
     * content" should be filtered from search results.
     *
     * @return <tt>true</tt> if adult content should be filtered out,
     *         <tt>false</tt> otherwise
     */
    public boolean getFilterAdult(){return _filterAdult;}

    /**
     * Returns a <tt>boolean</tt> value indicating whether or not duplicate
     * search results should be filtered out.
     *
     * @return <tt>true</tt> if duplicates should be filtered, <tt>false</tt>
     *         otherwise
     */
    public boolean getFilterDuplicates(){return _filterDuplicates;}

    /**
     * Returns a <tt>boolean</tt> value indicating whether or not html search
     * results should be filtered out.
     *
     * @return <tt>true</tt> if html should be filtered, <tt>false</tt>
     *         otherwise
     */
    public boolean getFilterHtml(){return _filterHtml;}

    /**
     * Returns a <tt>boolean</tt> value indicating whether or not vbs search
     * results should be filtered out.
     *
     * @return <tt>true</tt> if vbs files should be filtered, <tt>false</tt>
     *         otherwise
     */
    public boolean getFilterVbs(){return _filterVbs;}

    /**
     * Returns a <tt>boolean</tt> value indicating whether or not "greedy"
     * queries, such as "mp3," should be filtered out.
     *
     * @return <tt>true</tt> if greedy queries should be filtered,
     *         <tt>false</tt> otherwise
     */
    public boolean getFilterGreedyQueries() {return _filterGreedyQueries;}

    /**
     * Returns a <tt>boolean</tt> value indicating whether or not "bearshare"
     * queries should be filtered out.
     *
     * @return <tt>true</tt> if bearshare queries should be filtered,
     *         <tt>false</tt> otherwise
     */
    public boolean getFilterBearShareQueries() { return _filterBearShare; }

    /**
     * Returns the number of simultaneous searches to allow before the oldest
     * search panes start getting dropped.
     *
     * @return the number of simultaneous searches to allow before the oldest
     *         search panes start getting dropped
     */
    public int getParallelSearchMax(){return _parallelSearchMax;}

    /**
     * Returns the maximum number of simultaneous downloads to allow.
     *
     * @return the maximum number of simultaneous downloads to allow
     */
    public int getMaxSimDownload(){return _maxSimDownload;}

    /**
     * Returns a <tt>boolean</tt> specifying whether or not the user should
     * be prompted prior to downloading an exe file.
     *
     * @return <tt>true</tt> if the user should be prompted, <tt>false</tt>
     *         otherwise
     */
    public boolean getPromptExeDownload(){return _promptExeDownload;}

    /**
     * Returns the maximum number of simultaneous uploads to allow.
     *
     * @return the maximum number of simultaneous uploads to allow
     */
    public int getMaxUploads(){return _maxUploads;}

    /**
     * Returns the "soft maximum number" of simultaneous uploads to allow.
     *
     * @return the maximum number of simultaneous uploads to allow
     * @see setSoftMaxUploads
     */
    public int getSoftMaxUploads(){return _softMaxUploads;}

    /**
     * Returns a <tt>boolean</tt> specifying whether or not completed uploads
     * should automatically be cleared from the upload window.
     *
     * @return <tt>true</tt> if completed uploads should automatically be
     *         cleared, <tt>false</tt> otherwise
     */
    public boolean getClearCompletedUpload(){return _clearCompletedUpload;}

    /**
     * Returns a <tt>boolean</tt> specifying whether or not completed downloads
     * should automatically be cleared from the download window.
     *
     * @return <tt>true</tt> if completed downloads should automatically be
     *         cleared, <tt>false</tt> otherwise
     */
    public boolean getClearCompletedDownload(){return _clearCompletedDownload;}

    /**
     * Returns the connect <tt>String</tt> to use when making Gnutella
     * connections.
     *
     * @return the connect <tt>String</tt> to use when making Gnutella
     * connections
     */
    public String getConnectString() {
        return _connectString;
    }

    /** Returns the first word of the connect string.
     *  This is solely a convenience routine. */
    public String getConnectStringFirstWord() {
        return _connectStringFirstWord;
    }

    /**
     * Returns the remaining words of the connect string, without the leading
     * space.  This is solely a convenience routine.
     */
    public String getConnectStringRemainder() {
        return _connectStringRemainder;
    }

	/**
	 * Returns the string used for verifying a Gnutella connection.
	 *
	 * @return the string used for verifying a Gnutella connection
	 */
    public String getConnectOkString(){ return _connectOkString; }


    // SPECIALIZED METHODS FOR NETWORK DISCOVERY
    /** Returns the Network Discovery specialized properties file */
    public Properties getNDProps(){return ND_PROPS;}

    public int getBasicInfoSizeForQuery() {
		return _basicQueryInfo;
	}

    public int getAdvancedInfoSizeForQuery() {
		return _advancedQueryInfo;
	}

    /**
	 * Returns true iff this should force its IP address.
	 */
    public boolean getForceIPAddress() {
        return _forceIPAddress;
    }

    /**
	 * Returns the forced IP address as an array of bytes.
	 */
    public byte[] getForcedIPAddress() {
        return _forcedIPAddress;
    }

    /**
	 * Returns the forced IP address in dotted-quad format.
	 */
    public String getForcedIPAddressString() {
        return Message.ip2string(_forcedIPAddress);
    }

    /**
     * Returns the port to use when forcing the ip.
     *
     * @return the port to use when forcing the ip
     */
    public int getForcedPort() {
        return _forcedPort;
    }

	/**
	 * Returns a <tt>boolean<tt> indicating whether or not to check again
	 * for application updates.
	 *
	 * @return <tt>true</tt> if we should check again for updates,
	 *         <tt>false</tt> otherwise
	 */
    public boolean getCheckAgain() {
		Boolean b = Boolean.valueOf(PROPS.getProperty(CHECK_AGAIN));
        return b.booleanValue();
    }

	/**
	 * Returns the number of files required to not be considered a freeloader.
	 *
	 * @return the number of files required to not be considered a freeloader
	 */
    public int getFreeloaderFiles() {
        return _freeLoaderFiles;
    }

	/**
     * Returns the probability (expressed as a percentage) that an incoming
     * freeloader will be accepted.
	 *
	 * @return the probability (expressed as a percentage) that an incoming
     * freeloader will be accepted
	 */
    public int getFreeloaderAllowed() {
        return _freeLoaderAllowed;
    }

	/**
	 * Returns the average time that the user runs the application.
	 *
	 * @return the average time the user runs the application in one
	 *         session
	 */
	public long getAverageUptime() {
		return _averageUptime;
	}

	/**
	 * Returns the total amount of time that this user has run
	 * the application.
	 *
	 * @return the total amount of time that the user has run the
	 *         the application over all sessions
	 */
	public long getTotalUptime() {
		return _totalUptime;
	}

	/**
	 * Returns the number of times the application has been run.
	 *
	 * @return the total number of times the application has been run
	 */
	public int getSessions() {
		return _sessions;
	}

    /** 
     * Returns the fraction of time this is running.
     * @return a value between 0.0 and 1.0, inclusive
     * @see setFractionalUptime 
     */
    public float getFractionalUptime() {
        return getFloatValue(FRACTIONAL_UPTIME);
    }

    /** 
     * Returns the system time LimeWire was last shut down.
     * @return the system time in milliseconds
     * @see setLastShutdownTime 
     */
    public long getLastShutdownTime() {
        return getLongValue(LAST_SHUTDOWN_TIME);
    }

	/**
	 * Returns a boolean indicating whether or not the program
	 * has been "installed," with the properties set correctly.
	 *
	 * @return <tt>true</tt> if the application has been installed in some
	 *         manner, either throw an installer or through our own
	 *         installer, <tt>false</tt> otherwise
	 */
	public boolean getInstalled() {
		return _installed;
	}

	/**
	 * Returns the width that the application should be sized to.
	 *
	 * @return the width of the application main window in pixels
	 */
	public int getAppWidth() {
		return Integer.parseInt(PROPS.getProperty(APP_WIDTH));
	}

	/**
	 * Returns the height that the application should be sized to.
	 *
	 * @return the height of the application main window in pixels
	 */
	public int getAppHeight() {
		return Integer.parseInt(PROPS.getProperty(APP_HEIGHT));
	}

	/**
	 * Returns a <tt>boolean</tt> specifying whether or not the
	 * application has been run one time or not.
	 *
	 * @return <tt>true</tt> if the application has been run once before
	 *         this session, <tt>false</tt> otherwise
	 */
	public boolean getRunOnce() {
		Boolean b = Boolean.valueOf(PROPS.getProperty(RUN_ONCE));
		return b.booleanValue();
	}

	/**
	 * Returns an integer value for the x position of the window
	 * set by the user in a previous session.
	 *
	 * @return the final x position of the main application window
	 *         in the previous session
	 */
	public int getWindowX() {
		return Integer.parseInt(PROPS.getProperty(WINDOW_X));
	}

	/**
	 * Returns an integer value for the y position of the window
	 * set by the user in a previous session.
	 *
	 * @return the final y position of the main application window
	 *         in the previous session
	 */
	public int getWindowY() {
		return Integer.parseInt(PROPS.getProperty(WINDOW_Y));
	}

	/**
	 * Returns a boolean specifying whether or not to minimize
	 * the application to the system tray.
	 *
	 * @return <tt>true</tt> if the application should be minimized to the
	 *         system tray, <tt>false</tt> otherwise
	 */
	public boolean getMinimizeToTray() {
		return getBooleanValue(MINIMIZE_TO_TRAY);
	}

	/**
	 * Returns a boolean specifying whether or not to shutdown the application
	 * only after file transfers are complete
	 *
	 * @param whenReady <tt>true</tt> if the application should shutdown
	 *          only after file transfers are complete, false otherwise
	 */
	public boolean getShutdownAfterTransfers() {
		Boolean b = Boolean.valueOf(PROPS.getProperty(SHUTDOWN_AFTER_TRANSFERS));
		return b.booleanValue();
	}

	/**
	 * Returns a <tt>String</tt> instance specifying the language to use
	 * for the application.
	 *
	 * @return a <tt>String</tt> specifying the language to use for the
	 *         application
	 */
	public String getLanguage() {
		return PROPS.getProperty(LANGUAGE);
	}

	/**
	 * Returns a <tt>String</tt> instance specifying the country to use
	 * for the application.
	 *
	 * @return a <tt>String</tt> specifying the country to use for the
	 *         application
	 */
	public String getCountry() {
		return PROPS.getProperty(COUNTRY);
	}

	/**
	 * Returns a <tt>String</tt> instance specifying the locale variant
	 * to use for the application.
	 *
	 * @return a <tt>String</tt> specifying the locale variant to use for
	 *         the application
	 */
	public String getLocaleVariant() {
		return PROPS.getProperty(LOCALE_VARIANT);
	}

	/**
	 * Returns the minimum search quality (number of stars) to show in the
	 * search window.
	 *
	 * @return the minimum search quality, on a scale of 0 to 3 inclusive
	 */
	public int getMinimumSearchQuality() {
		return getIntValue(MINIMUM_SEARCH_QUALITY);
	}

	/**
	 * Returns the minimum speed for search results to display in the
	 * search window.
	 *
	 * @return the minimum search speed to display
	 */
	public int getMinimumSearchSpeed() {
		return getIntValue(MINIMUM_SEARCH_SPEED);
	}

	/**
	 * Returns the maximum number of upstream bytes per second ever
	 * passed by this node.
	 *
	 * @return the maximum number of upstream bytes per second ever
	 * passed by this node
	 */
	public int getMaxUpstreamBytesPerSec() {
		return getIntValue(MAX_UPLOAD_BYTES_PER_SEC);
	}

	/**
	 * Returns the maximum number of downstream bytes per second ever
	 * passed by this node.
	 *
	 * @return the maximum number of downstream bytes per second ever
	 * passed by this node
	 */
	public int getMaxDownstreamBytesPerSec() {
		return getIntValue(MAX_DOWNLOAD_BYTES_PER_SEC);
	}

	/**
     * Returns The time when we last expired accumulated information
	 * @return The time when we last expired accumulated information
     */
    public long getLastExpireTime(){
        return getLongValue(LAST_EXPIRE_TIME);
    }

    /**
     * Tells whether this node should accept authenticated connections only
     * @return true, if this node should accept authenticated connections
     * only, false otherwise
     */
    public boolean acceptAuthenticatedConnectionsOnly() {
        return Boolean.valueOf(PROPS.getProperty(
            ACCEPT_AUTHENTICATED_CONNECTIONS_ONLY)).booleanValue();
    }

    /**
     * Returns the name of the file that stores cookies
     * @return The name of the cookies file
     */
    public String getCookiesFile() {
        return PROPS.getProperty(COOKIES_FILE);
    }

    /**
     * Tells whether the node is gonna be a supernode or not
     * @return true, if supernode, false otherwise
     */
    public boolean isServer() {
        return _server;
    }

	/**
	 * Updates the average, total, and current update settings based on
	 * previous settings and the <tt>newTime</tt> argument for the number
	 * of seconds that have passed since the last update.
	 *
	 * @param newTime the number of seconds that have passed since
	 *                the last time the uptime was updated (since the
	 *                last time this method was called).
	 */
	public void updateUptime(final int newTime) {
		_totalUptime += newTime;
		_averageUptime = _totalUptime/_sessions;
		setTotalUptime(_totalUptime);
		setAverageUptime(_averageUptime);
	}

	/**
	 * Sets the total number of times the application  has been run --
	 * used in calculating the average amount of time this user
	 * leaves the application on.
	 *
	 * @param sessions the total number of sessions that the application
	 *                 has been run
	 */
	private void setSessions(final int sessions) {
		_sessions = sessions;
		PROPS.put(SESSIONS, Integer.toString(_sessions));
	}

	/**
	 * Sets the average time this user leaves the application running.
	 *
	 * @param averageUptime the average time this user leaves the
	 *                      application running
	 */
	private void setAverageUptime(long averageUptime) {
		_averageUptime = averageUptime;
		PROPS.put(AVERAGE_UPTIME, Long.toString(averageUptime));
	}

	/**
	 * Sets the total time this user has used the application.
	 *
	 * @param totalUptime the total time the application has been run
	 */
	private void setTotalUptime(long totalUptime) {
		_totalUptime = totalUptime;
		String s = Long.toString(_totalUptime);
		PROPS.put(TOTAL_UPTIME, s);
	}

    /**
     * Sets the fraction of time this is running, a unitless quality.  This is
     * used to identify highly available hosts with big pongs.  This value
     * should only be updated once per session.
     * 
     * @param fraction a number between 0.0 and 1.0f, inclusive
     * @see com.limegroup.gnutella.Statistics#calculateFractionalUptime
     */
    public void setFractionalUptime(float fraction) {
        //TODO: what if fraction is <0 or >1?
        setFloatValue(FRACTIONAL_UPTIME, fraction);
    }

    /**
     * Sets the time that this was last shutdown.
     *
     * @param time the system time in milliseconds of the last shutdown
     * @see com.limegroup.gnutella.Statistics#calculateFractionalUptime
     */
    public void setLastShutdownTime(long time) {
        //TODO: what if time is negative?
        setLongValue(LAST_SHUTDOWN_TIME, time);
    }

    /**
	 * Sets the maximum length of packets (spam protection)
	 */
    public void setMaxLength(int maxLength) {
		_maxLength = maxLength;
		String s = Integer.toString(_maxLength);
		PROPS.put(MAX_LENGTH, s);
    }

    /**
	 * Sets the timeout value for persistent HTTP connections
     * @param timeout The timeout (in milliseconds) to be set
	 */
    public void setPersistentHTTPConnectionTimeout(int timeout) {
		_persistentHTTPConnectionTimeout = timeout;
		String s = Integer.toString(_persistentHTTPConnectionTimeout);
		PROPS.put(PERSISTENT_HTTP_CONNECTION_TIMEOUT, s);
    }


    /**
	 * Returns the maximum number of connections for the given connection
     * speed.
	 */
    public int maxConnections() {
        int speed=getConnectionSpeed();
        if (speed<=56)    //modems
            return 3;
        else if (speed<=350)  //cable
            return 6;
        else if (speed<=1000) //T1
            return 10;
        else                  //T3: no limit
            return 12;
    }


    /**
	 * Sets the limit for the number of searches
     * throws an exception on negative limits
     * and limits of 10,000 or more.
	 */
    public void setSearchLimit(byte limit) {
        if(limit < 0 || limit > 10000)
            throw new IllegalArgumentException();
        else {
            _searchLimit = limit;
            String s = Byte.toString(_searchLimit);
            PROPS.put(SEARCH_LIMIT, s);
        }
    }

    /** Sets the client (gu) ID number */
    public void setClientID(String clientID) {
		_clientID = clientID;
		PROPS.put(CLIENT_ID, _clientID);
    }

    public void setBasicInfoForQuery(int basicInfo) {
        _basicQueryInfo = basicInfo;
        String s = Integer.toString(basicInfo);
        PROPS.put(BASIC_QUERY_INFO, s);
    }

    /**
     * Sets the maximum number of uploads per person to allow (the uploads
     * per unique uploader).
     *
     * @param uploads the number of uploads to allow
     */
	public void setUploadsPerPerson(int uploads) {
		_uploadsPerPerson = uploads;
		String s = Integer.toString(uploads);
        PROPS.put(UPLOADS_PER_PERSON , s);
	}

    /**
     * Sets the number of uploads we want to queue 
     */
	public void setUploadQueueSize(int size) {
		_uploadQueueSize = size;
		String s = Integer.toString(size);
        PROPS.put(UPLOAD_QUEUE_SIZE , s);
	}

    public void setAdvancedInfoForQuery(int advancedInfo) {
        _advancedQueryInfo = advancedInfo;
        String s = Integer.toString(advancedInfo);
        PROPS.put(ADVANCED_QUERY_INFO, s);
    }

    /**
	 * Sets the directory for saving files.
	 *
	 * @param   saveDir  A <tt>File</tt> instance denoting the
	 *                   abstract pathname of the directory for
	 *                   saving files.
	 *
	 * @throws  <tt>IOException</tt>
	 *          If the directory denoted by the directory pathname
	 *          String parameter did not exist prior to this method
	 *          call and could not be created, or if the canonical
	 *          path could not be retrieved from the file system.
	 *
	 * @throws  <tt>NullPointerException</tt>
	 *          If the "dir" parameter is null.
	 */
    public void setSaveDirectory(File saveDir) throws IOException {
		if(saveDir == null) throw new NullPointerException();
		if(!saveDir.isDirectory()) {
			if(!saveDir.mkdirs()) throw new IOException();
		}

		String parentDir = saveDir.getParent();
		File incDir = new File(parentDir, "Incomplete");
		if(!incDir.isDirectory()) {
			if(!incDir.mkdirs()) throw new IOException();
		}

		if(!saveDir.canRead() || !saveDir.canWrite() ||
		   !incDir.canRead()  || !incDir.canWrite()) {
			throw new IOException();
		}
		_saveDirectory       = saveDir;
		_incompleteDirectory = incDir;
		
		setStringValue(DIRECTORY_FOR_SAVING_FILES, saveDir.getAbsolutePath());
    }


	/**
	 * This method sets the shared directories based on the
	 * string of semi-colon delimited directories stored in
	 * the props file.
	 *
	 * @param dirs the string of directories
	 */
	private void setDirectories(final String dirs) {
		StringTokenizer st = new StringTokenizer(dirs, ";");
		int length = st.countTokens();
		File[] files = new File[length];
		for(int i=0; i<length; i++) {
			files[i] = new File(st.nextToken());
		}
		setDirectories(files);
	}

    /** Sets the minimum age in days for which incomplete files will be deleted.
     *  This values may be zero or negative; doing so will cause LimeWire to
     *  delete ALL incomplete files on startup. */
    public void setIncompletePurgeTime(int days) {
        _incompletePurgeTime=days;
        PROPS.put(INCOMPLETE_PURGE_TIME, Integer.toString(days));
    }

    /**
	 * Sets the shared directories.  This method filters
     * out any duplicate or invalid directories in the string.
     * Note, however, that it does not currently filter out
     * listing subdirectories that have parent directories
     * also in the string.
	 *
	 * @param dirs an array of <tt>File</tt> instances denoting
	 *  the abstract pathnames of the shared directories
	 */
	public void setDirectories(final File[] dirArray) {

		// ok, let's prune out any duplicates if they're there
		HashMap directories = new HashMap();
		for(int i=0; i<dirArray.length; i++) {
			if(dirArray[i].isDirectory())
				directories.put(dirArray[i], "");
		}

		Set fileSet = directories.keySet();

		Object[] prunedFiles = fileSet.toArray();
		StringBuffer sb = new StringBuffer();
		for(int z=0; z<prunedFiles.length; z++) {
			if(prunedFiles[z] != null) {
				sb.append(prunedFiles[z]);
				sb.append(';');
			}
		}
		_directories = new File[prunedFiles.length];
		for(int r=0; r<prunedFiles.length; r++) {
			_directories[r] = (File)prunedFiles[r];
		}
        PROPS.put(DIRECTORIES_TO_SEARCH_FOR_FILES, sb.toString());
	}

    /**
	 * Adds one directory to the directory string only if
     * it is a directory and is not already listed.
	 *
	 * @param dir  a <tt>File</tt> instance denoting the
	 *             abstract pathname of the new directory
	 *             to add
	 *
	 * @throws  IOException
	 *          if the directory denoted by the directory pathname
	 *          String parameter did not exist prior to this method
	 *          call and could not be created, or if the canonical
	 *          path could not be retrieved from the file system
	 */
    public void addDirectory(File dir) throws IOException {
		if(!dir.isDirectory()) throw new IOException();

		if(_directories == null) {
			_directories = new File[1];
			_directories[0] = dir;
		}
		else {
			int newLength = _directories.length + 1;
			File[] newFiles = new File[newLength];

			for(int i=0; i<_directories.length; i++) {
				newFiles[i] = _directories[i];
			}
			newFiles[_directories.length] = dir;
			// this will prune it out if it's a duplicate and add it too
			setDirectories(newFiles);
		}
	}

    /**
	 * Sets the file extensions that are shared.
	 *
	 * @param ext the semi-colon delimited string of shared file extensions
	 */
    public void setExtensions(String ext) {
        _extensions = ext;
        PROPS.put(EXTENSIONS_TO_SEARCH_FOR, ext);
    }

    /**
	 * Sets the time to live.
	 */
	private void setTTL(byte ttl) {
		_ttl = ttl;
		String s = Byte.toString(_ttl);
		PROPS.put(TTL, s);
     }

    /**
	 * Sets the port to connect on
	 */
    public void setPort(int port) {
        // if the entered port is outside accepted
        // port numbers, throw the exception
        if(port > 65536 || port < 0)
            throw new IllegalArgumentException();
        else {
            _port = port;
            String s = Integer.toString(_port);
            PROPS.put(PORT, s);
        }
    }

    /**
	 * Sets the connection speed.  throws an exception if you
	 * try to set the speed far faster than a T3 line or less than
     * 0.
	 */
    public void setConnectionSpeed(int speed) {
        if(speed < 0 || speed > 20000)
            throw new IllegalArgumentException();
        else {
            _connectionSpeed = speed;
            String s = Integer.toString(_connectionSpeed);
            PROPS.put(CONNECTION_SPEED, s);
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
            PROPS.put(UPLOAD_SPEED, s);
        }
    }

	/**
	 * Sets the string for making gnutella connections.
	 *
	 * @param connect the connect string
	 */
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

        PROPS.put(CONNECT_STRING, connect);
    }

	/**
	 * Sets the string for verifying Gnutella connections.
	 *
	 * @param ok the string for verifying Gnutella connections
	 */
    public void setConnectOkString(String ok)
        throws IllegalArgumentException {
        if (ok.length()<1)
            throw new IllegalArgumentException();

        _connectOkString = ok;
        PROPS.put(CONNECT_OK_STRING, ok);
    }

	/**
	 * Sets the maximum number of simultaneous searches to allow.
	 *
	 * @param max the maximum number of simultaneous searches
	 */
    public void setParallelSearchMax(int max) {
        if(max<1)
            throw new IllegalArgumentException();
        else {
            _parallelSearchMax = max;
            String s = String.valueOf(max);
            PROPS.put(PARALLEL_SEARCH, s);
        }
    }


	/**
	 * Sets whether or not the application should prompt the user before
	 * downloading exe files.
	 *
	 * @param prompt specifies whether or not the application should prompt
	 *               the user before downloading exe files
	 */
    public void setPromptExeDownload(boolean prompt) {
        _promptExeDownload = prompt;
        String s = String.valueOf(prompt);
        PROPS.put(PROMPT_EXE_DOWNLOAD, s);
    }

	/**
	 * Sets whether or not chat should be enabled.
	 *
	 * @param chatEnabled specified whether or not chat is enabled
	 */
	public void setChatEnabled(boolean chatEnabled) {
		_chatEnabled = chatEnabled;
		String s = String.valueOf(chatEnabled);
		PROPS.put(CHAT_ENABLED, s);
	}


	/**
	 * Sets whether or not player should be enabled.
	 *
	 * @param playerEnabled specified whether or not player is enabled
	 */
	public void setPlayerEnabled(boolean playerEnabled) {
		_playerEnabled = playerEnabled;
		String s = String.valueOf(playerEnabled);
		PROPS.put(PLAYER_ENABLED, s);
	}

	/**
	 * Sets whether or not Monitor Tab should be enabled.
	 *
	 * @param monitorEnabled specified whether or not Monitor Tab is enabled.
	 */
	public void setMonitorViewEnabled(boolean monitorEnabled) {
		_monitorViewEnabled = monitorEnabled;
		String s = String.valueOf(monitorEnabled);
		PROPS.put(MONITOR_VIEW_ENABLED, s);
	}

	/**
	 * Sets whether or not Connection Tab should be enabled.
	 *
	 * @param connectionEnabled specified whether or not Monitor Tab is enabled.
	 */
	public void setConnectionViewEnabled(boolean connectionEnabled) {
		_connectionViewEnabled = connectionEnabled;
		String s = String.valueOf(connectionEnabled);
		PROPS.put(CONNECTION_VIEW_ENABLED, s);
	}

	/**
	 * Sets whether or not Library Tab should be enabled.
	 *
	 * @param libraryEnabled specified whether or not Library Tab is enabled.
	 */
	public void setLibraryViewEnabled(boolean libraryEnabled) {
		_libraryViewEnabled = libraryEnabled;
		String s = String.valueOf(libraryEnabled);
		PROPS.put(LIBRARY_VIEW_ENABLED, s);
	}

	/**
	 * Sets whether or not Shopping Tab should be enabled.
	 *
	 * @param shoppingEnabled specified whether or not Shopping Tab is enabled.
	 */
	public void setShoppingViewEnabled(boolean shoppingEnabled) {
		_shoppingViewEnabled = shoppingEnabled;
		String s = String.valueOf(shoppingEnabled);
		PROPS.put(SHOPPING_VIEW_ENABLED, s);
	}

	/**
	 * Sets the maximum number of simultaneous downloads to allow.
	 *
	 * @param max the maximum number of simultaneous downloads to allow
	 */
    public void setMaxSimDownload(int max) {
		_maxSimDownload = max;
		String s = String.valueOf(max);
		PROPS.put(MAX_SIM_DOWNLOAD, s);
    }

	/**
	 * Sets the maximum number of simultaneous uploads to allow.
	 *
	 * @param max the maximum number of simultaneous uploads to allow
	 */
    public void setMaxUploads(int max) {
		_maxUploads = max;
		String s = String.valueOf(max);
		PROPS.put(MAX_UPLOADS, s);
    }

	/**
	 * Sets the "soft" maximum number of simultaneous uploads to allow,
     * i.e., the minimum number of people to allow before determining
     * whether to allow more uploads.
	 *
	 * @param max the soft maximum number of simultaneous uploads
	 */
    public void setSoftMaxUploads(int max) {
		_softMaxUploads = max;
		String s = String.valueOf(max);
		PROPS.put(SOFT_MAX_UPLOADS, s);
    }

    public void setClearCompletedUpload(boolean b) {
		_clearCompletedUpload = b;
		String s = String.valueOf(b);
		PROPS.put(CLEAR_UPLOAD, s);
    }

	/**
	 * Sets whether or not completed downloads should be automatically
	 * cleared or not.
	 *
	 * @param clear specifies whether or not they should be
	 * automatically cleared
	 */
	public void setClearCompletedDownload(boolean clear) {
		_clearCompletedDownload = clear;
		PROPS.put(CLEAR_DOWNLOAD, String.valueOf(clear));
    }

	/**
	 * Sets whether or not the users ip address should be forced to
	 * the value they have entered.
	 *
	 * @param clear specifies whether or not the ip address should
	 * be forced
	 */
    public void setForceIPAddress(boolean force) {
        String c;
        if (force == true)
            c = "true";
        else
            c = "false";
        _forceIPAddress = force;
        PROPS.put(FORCE_IP_ADDRESS, c);
    }

	/**
	 * Sets whether or not browsers should be allowed to perform uploads.
	 *
	 * @param <tt>boolean</tt> specifying whether or not browsers should
	 *        ever be allowed to perform uploads
	 */
    public void setAllowBrowser(boolean allow) {
        String c;
        if (allow == true)
            c = "true";
        else
            c = "false";
        _allowBroswer = allow;
        PROPS.put(ALLOW_BROWSER, c);
    }

    /**
     * Sets the force IP address to the given address.
     * If address is in symbolic form, blocks while
     * resolving it.
     *
     * @param address an IP address in dotted quad (e.g., 1.2.3.4)
     *  or symbolic form (e.g., sparky.limewire.com)
     * @exception IllegalArgumentException address wasn't
     *   in a valid format.
     */
    public void setForcedIPAddressString(String address)
            throws IllegalArgumentException {
        try {
			if(address.equals(DEFAULT_FORCED_IP_ADDRESS_STRING)) {
				_forcedIPAddress = new byte[4];
				_forcedIPAddress[0] = 0;
				_forcedIPAddress[1] = 0;
				_forcedIPAddress[2] = 0;
				_forcedIPAddress[3] = 0;
			} else {
				InetAddress ia = InetAddress.getByName(address);
				_forcedIPAddress = ia.getAddress();
			}
            PROPS.put(FORCED_IP_ADDRESS_STRING, address);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Sets the port to use when forcing the ip address.
     *
     * @param port the port to use for forcing the ip address
     */
    public void setForcedPort(int port) {
        // if the entered port is outside accepted
        // port numbers, throw the exception
        if(port > 65536 || port < 1)
            throw new IllegalArgumentException();
        else {
            _forcedPort = port;
            String s = Integer.toString(_forcedPort);
            PROPS.put(FORCED_PORT, s);
        }
    }

	/**
	 * Sets whether or not the program has been installed, either by
	 * a third-party installer, or by our own.
	 *
	 * @param installed specifies whether or not the application has
	 *                  been installed
	 */
	public void setInstalled(boolean installed) {
        _installed = installed;
        String s = String.valueOf(installed);
        PROPS.put(INSTALLED, s);
    }

	/**
	 * Sets the array of ip addresses that the user has banned.
	 *
	 * @param bannedIps the array of ip addresses that the user has banned
	 *                  from their machine
	 */
    public void setBannedIps(String[] bannedIps) {
        if(bannedIps == null)
            throw new IllegalArgumentException();
        else {
            _bannedIps = bannedIps;
            PROPS.put(BLACK_LISTED_IP_ADDRESSES,encode(bannedIps));
        }
    }

	/**
	 * Sets the array of ip addresses that the user has allowed.
	 *
	 * @param bannedIps the array of ip addresses that the user has allowed
	 *                  from their machine
	 */
    public void setAllowedIps(String[] allowedIps) {
        if(allowedIps == null)
            throw new IllegalArgumentException();
        else {
            _allowedIps = allowedIps;
            PROPS.put(WHITE_LISTED_IP_ADDRESSES,encode(allowedIps));
        }
    }

	/**
	 * Sets the array of words that the user has banned from appearing in
	 * search results.
	 *
	 * @param bannedIps the array of words that the user has banned from
	 *                  appearing in search results
	 */
    public void setBannedWords(String[] bannedWords) {
        if(bannedWords == null)
            throw new IllegalArgumentException();
        else {
            _bannedWords = bannedWords;
            PROPS.put(BANNED_WORDS,
                       encode(bannedWords));
        }
    }

	/**
	 * Sets whether or not search results including "adult content" are
	 * banned.
	 *
	 * @param filterAdult specifies whether or not search results with
	 *                    words designated as "adult content" should be
	 *                    banned
	 */
    public void setFilterAdult(boolean filterAdult) {
		_filterAdult = filterAdult;
		Boolean b = filterAdult ? Boolean.TRUE : Boolean.FALSE;
		String s = b.toString();
		PROPS.put(FILTER_ADULT, s);
    }

    public void setFilterDuplicates(boolean filterDuplicates) {
		_filterDuplicates = filterDuplicates;
		Boolean b = filterDuplicates ? Boolean.TRUE : Boolean.FALSE;
		String s = b.toString();
		PROPS.put(FILTER_DUPLICATES, s);
    }

    public void setFilterHtml(boolean filterHtml) {
		_filterHtml = filterHtml;
		Boolean b = filterHtml ? Boolean.TRUE : Boolean.FALSE;
		String s = b.toString();
		PROPS.put(FILTER_HTML, s);
    }

    public void setFilterVbs(boolean filterVbs) {
		_filterVbs = filterVbs;
		Boolean b = filterVbs ? Boolean.TRUE : Boolean.FALSE;
		String s = b.toString();
		PROPS.put(FILTER_VBS, s);
    }

    public void setFilterGreedyQueries(boolean yes) {
        _filterGreedyQueries = yes;
        Boolean b = yes ? Boolean.TRUE : Boolean.FALSE;
        String s = b.toString();
        PROPS.put(FILTER_GREEDY_QUERIES, s);
    }


    public void setFilterBearShareQueries(boolean yes) {
        _filterBearShare = yes;
        Boolean b = yes ? Boolean.TRUE : Boolean.FALSE;
        String s = b.toString();
        PROPS.put(FILTER_HIGHBIT_QUERIES, s);
    }

    /**
     * Sets the probability (expressed as a percentage) that an incoming
     * freeloader will be accepted.   For example, if allowed==50, an incoming
     * connection has a 50-50 chance being accepted.  If allowed==100, all
     * incoming connections are accepted.  Throws IllegalArgumentException if
     * allowed<0 or allowed>100.
     */
    public void setFreeloaderAllowed(int allowed)
		throws IllegalArgumentException {
        if (allowed>100 || allowed<0)
            throw new IllegalArgumentException();
        _freeLoaderAllowed = allowed;
        String s = Integer.toString(allowed);
        PROPS.put(FREELOADER_ALLOWED, s);
    }

    /**
     * Sets minimum the number of files a host must share to not be considered
     * a freeloader.  For example, if files==0, no host is considered a
     * freeloader.  Throws IllegalArgumentException if files<0.
     */
    public void setFreeloaderFiles(final int files)
		throws IllegalArgumentException {
        if (files<0)
            throw new IllegalArgumentException();
        _freeLoaderFiles = files;
        String s = Integer.toString(files);
        PROPS.put(FREELOADER_FILES, s);
    }


	/**
	 * Sets the boolean for whether or not we should check again for an update.
	 *
	 * @param check <tt>boolean</tt> value specifying whether or not to check
	 *              again for updates
	 */
    public void setCheckAgain(final boolean check) {
		Boolean b = check ? Boolean.TRUE : Boolean.FALSE;
        PROPS.put(CHECK_AGAIN, b.toString());
    }

	/**
	 * Sets the width that the application should be.
	 *
	 * @param width the width in pixels of the main application window
	 */
	public void setAppWidth(final int width) {
        String s = Integer.toString(width);
		PROPS.put(APP_WIDTH, s);
	}

	/**
	 * Sets the height that the application should be.
	 *
	 * @param height the height in pixels of the main application window
	 */
	public void setAppHeight(final int height) {
		PROPS.put(APP_HEIGHT, Integer.toString(height));
	}

	/**
	 * Sets the flag for whether or not the application has been run one
	 * time before this.
	 *
	 * @param runOnce <tt>boolean</tt> for whether or not the application has
	 *                been run once
	 */
	public void setRunOnce(final boolean runOnce) {
		Boolean b = runOnce ? Boolean.TRUE : Boolean.FALSE;
		PROPS.put(RUN_ONCE, b.toString());
	}

	/**
	 * Set the x position of the window for the next time the application
	 * is started.
	 *
	 * @param x the x position of the main application window
	 */
	public void setWindowX(final int x) {
		PROPS.put(WINDOW_X, Integer.toString(x));
	}

	/**
	 * Set the y position of the window for the next time the application
	 * is started.
	 *
	 * @param y the y position of the main application window
	 */
	public void setWindowY(final int y) {
		PROPS.put(WINDOW_Y, Integer.toString(y));
	}

	/**
	 * Sets the flag for whether or not the application should be minimized
	 * to the system tray on windows.
	 *
	 * @param minimize <tt>boolean</tt> for whether or not the application
	 *                 should be minimized to the tray
	 */
	public void setMinimizeToTray(final boolean minimize) {
		setBooleanValue(MINIMIZE_TO_TRAY, minimize);
	}

	/**
	 * Sets the flag for whether or not the application should shutdown
	 * immediately, or when file transfers are complete
	 *
	 * @param minimize <tt>boolean</tt> for whether or not the application
	 *                 should shutdown only after transfers are complete
	 */
	public void setShutdownAfterTransfers(final boolean whenReady) {
		setBooleanValue(SHUTDOWN_AFTER_TRANSFERS, whenReady);
	}


	/**
	 * Sets the language to use for the application.
	 *
	 * @param language the language to use
	 */
	public void setLanguage(final String language) {
		PROPS.put(LANGUAGE, language);
	}

	/**
	 * Sets the country to use for the application.
	 *
	 * @param country the country to use
	 */
	public void setCountry(final String country) {
		PROPS.put(COUNTRY, country);
	}

	/**
	 * Sets the locale variant to use for the application.
	 *
	 * @param localeVariant the locale variant to use
	 */
	public void setLocaleVariant(final String localeVariant) {
		PROPS.put(LOCALE_VARIANT, localeVariant);
	}

	/**
	 * Sets the minimum quality (number of stars) for search results to
	 * display.
	 *
	 * @param quality the minimum quality for search results, from 0 to 3,
	 *                inclusive
	 */
	public void setMinimumSearchQuality(final int quality) {
		PROPS.put(MINIMUM_SEARCH_QUALITY, Integer.toString(quality));
	}

	/**
	 * Sets the minimum speed for search results to display.
	 *
	 * @param speed the minimum speed to display
	 */
	public void setMinimumSearchSpeed(final int speed) {
		PROPS.put(MINIMUM_SEARCH_SPEED, Integer.toString(speed));
	}

	/**
	 * Sets the maximum number of upstream bytes per second ever passed by
	 * this node.
	 *
	 * @param bytes the maximum number of upstream bytes per second ever
	 *              passed by this node
	 */
	public void setMaxUpstreamBytesPerSec(final int bytes) {
		PROPS.put(MAX_UPLOAD_BYTES_PER_SEC, Integer.toString(bytes));
	}


	/**
	 * Sets the maximum number of downstream bytes per second ever passed by
	 * this node.
	 *
	 * @param bytes the maximum number of downstream bytes per second ever
	 *              passed by this node
	 */
	public void setMaxDownstreamBytesPerSec(final int bytes) {
		PROPS.put(MAX_DOWNLOAD_BYTES_PER_SEC, Integer.toString(bytes));
	}


    /**
     * Returns The time when we last expired accumulated information
      * @return The time when we last expired accumulated information
     */
    public void setLastExpireTime(long lastExpireTime){
        setLongValue(LAST_EXPIRE_TIME, lastExpireTime);
    }

	/**
	 * Sets the name of the jar file to load on startup, which is read
	 * in from the properties file by RunLime.
	 *
	 * @param jarName the name of the jar file to load
	 */
	public void setJarName(final String jarName) {
		setStringValue(JAR_NAME, jarName);

		// make sure that the jar name and the classpath correspond
		// in the case where an old RunLime.jar has updated to
		// a LimeWire version that uses JAR_NAME instead of
		// CLASSPATH, there will be no JAR_NAME entry in 
		// limewire.props, but there will be CLASSPATH, so this will
		// get overwritten when setClasspath is called when the 
		// CLASSPATH is read from the props file
		setStringValue(CLASSPATH, jarName);
	}

	/**
	 * Sets the classpath for legacy RunLime.jars.
	 *
	 * @param classpath the classpath to set
	 */
	private void setClasspath(final String classpath) {
		setStringValue(CLASSPATH, classpath);

		// see commment above -- JAR_NAME and CLASSPATH should
		// always be the same value
		setStringValue(JAR_NAME, classpath);
	}

    /**
     * Sets whether the node is a server or not
     * @param isServer true, if the node is server, false otherwise
     */
    public void setServer(boolean isServer){
        this._server = isServer;
		setBooleanValue(SERVER, isServer);
    }

    /**
     * Sets the name of the file that stores cookies
     * @param filename The file name to be set
     */
    public void setCookiesFile(final String filename) {
		setStringValue(COOKIES_FILE, filename);
    }

    /**
     * Sets the flag indicating whether this node should accept
     * only authenticated connections
     * @param flag the flag value to be set
     */
    public void setAcceptAuthenticatedConnectionsOnly(final boolean flag) {
		setBooleanValue(ACCEPT_AUTHENTICATED_CONNECTIONS_ONLY, flag);
    }

	/**
	 * Sets the <tt>boolean</tt> value for the specified key as a
	 * <tt>String</tt> entry.
	 *
	 * @param KEY the key for the value to set
	 * @param BOOL the <tt>boolean</tt> value to set
	 */
	private void setBooleanValue(final String KEY, final boolean BOOL) {
		PROPS.put(KEY, String.valueOf(BOOL));
	}

    /**
	 * Sets the <tt>long</tt> value for the specified key as a
	 * <tt>String</tt> entry.
	 *
	 * @param KEY the key for the value to set
	 * @param LONG the <tt>long</tt> value to set
	 */
	private void setLongValue(final String KEY, final long LONG) {
		PROPS.put(KEY, String.valueOf(LONG));
	}

	/**
	 * Sets the <tt>int</tt> value for the specified key as a
	 * <tt>String</tt> entry.
	 *
	 * @param KEY the key for the value to set
	 * @param INT the <tt>int</tt> value to set
	 */
	private void setIntValue(final String KEY, final int INT) {
		PROPS.put(KEY, Integer.toString(INT));
	}

	/**
	 * Sets the <tt>float</tt> value for the specified key as a
	 * <tt>Float</tt> entry.
	 *
	 * @param KEY the key for the value to set
	 * @param FLOAT the <tt>float</tt> value to set
	 */
	private void setFloatValue(final String KEY, final float FLOAT) {
		PROPS.put(KEY, Float.toString(FLOAT));
	}

	/**
	 * Sets the <tt>String</tt> value for the specified key.
	 *
	 * @param KEY the key for the value to set
	 * @param STR the <tt>String</tt> value to set
	 */
	private void setStringValue(final String KEY, final String STR) {
		PROPS.put(KEY, STR);
	}

	/**
	 * Sets the <tt>File</tt> value for the specified key.
	 *
	 * @param key the key for the value to set
	 * @param file the <tt>File</tt> value to set
	 */
	private void setFileValue(final String key, final File file) {
		PROPS.put(key, file.getAbsolutePath());
	}

	/**
	 * Returns the <tt>boolean</tt> value for the specified
	 * key.
	 *
	 * @param KEY the key for the desired value
	 * @return the <tt>boolean</tt> value associated with the
	 *  specified key
	 */
	private boolean getBooleanValue(final String KEY) {
		return Boolean.valueOf(PROPS.getProperty(KEY)).booleanValue();
	}

    /**
	 * Returns the <tt>long</tt> value for the specified
	 * key.
	 *
	 * @param KEY the key for the desired value
	 * @return the <tt>long</tt> value associated with the
	 *  specified key
	 */
	private long getLongValue(final String KEY) {
		return Long.valueOf(PROPS.getProperty(KEY)).longValue();
	}

	/**
	 * Returns the <tt>String</tt> value associated with the
	 * specified key.
	 *
	 * @param KEY the key for the desired value
	 * @return the <tt>String</tt> value associated with the
	 *  specified key
	 */
	private String getStringValue(final String KEY) {
		return PROPS.getProperty(KEY);
	}

	/**
	 * Returns the <tt>int</tt> value associated with the
	 * specified key.
	 *
	 * @param KEY the key for the desired value
	 * @return the <tt>int</tt> value associated with the
	 *  specified key
	 */
	private int getIntValue(final String KEY) {
		return Integer.parseInt(PROPS.getProperty(KEY));
	}

	/**
	 * Returns the <tt>float</tt> value associated with the
	 * specified key.
	 *
	 * @param KEY the key for the desired value
	 * @return the <tt>float</tt> value associated with the
	 *  specified key
	 */
	private float getFloatValue(final String KEY) {
		return Float.valueOf(PROPS.getProperty(KEY)).floatValue();
	}

	/**
	 * Returns the <tt>File</tt> value associated with the 
	 * specified key.
	 *
	 * @param key the key for the desired value
	 * @return the <tt>File</tt> value associated with the
	 *  specified key
	 */
	private File getFileValue(final String key) {
		return new File(PROPS.getProperty(key));
	}

    /**
	 * Writes out the properties file to with the specified
     * name in the user's install directory.  This should only
	 * get called once when the program shuts down.
     */
    public void writeProperties() {
        SettingsHandler.save();
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
//          //PROPS.put(INSTALLED, s);
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

