package com.limegroup.gnutella;

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
	public static final boolean DEFAULT_ALLOW_BROWSER  = false;
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
    public static final int     DEFAULT_KEEP_ALIVE     = 4;
    /** Default port*/
    public static final int     DEFAULT_PORT           = 6346;
    /** Default network connection speed */
    public static final int     DEFAULT_SPEED          = 56;
    public static final int     DEFAULT_UPLOAD_SPEED   = 50;
    /** Default limit for the number of searches */
    public static final byte    DEFAULT_SEARCH_LIMIT   = (byte)64;
    /** Default client guid */
    public static final String  DEFAULT_LIENT_ID      = null;
    /** Default maximum number of connections */
    public static final int     DEFAULT_MAX_INCOMING_CONNECTION=4;
    /** Default directories for file searching */
    public static final String  DEFAULT_SAVE_DIRECTORY = "";
    /** Default directories for file searching */
    public static final String  DEFAULT_DIRECTORIES    = "";
    /** Default file extensions */
    public static final String  DEFAULT_EXTENSIONS     =
    "html;htm;xml;txt;pdf;ps;rtf;doc;tex;mp3;wav;au;aif;aiff;ra;ram;"+
    "mpg;mpeg;asf;qt;mov;avi;mpe;swf;dcr;gif;jpg;jpeg;jpe;png;tif;tiff;"+
    "exe;zip;gz;gzip;hqx;tar;tgz;z;rmj;lqt;rar;ace;sit;smi";


	/* the number of uplads allowed per person at a given time */
    public static final int     DEFAULT_UPLOADS_PER_PERSON=3;

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
    /** Default for whether user should be prompted before downloading exe's. */
    public static final boolean DEFAULT_PROMPT_EXE_DOWNLOAD = true;
    public static final int     DEFAULT_MAX_UPLOADS      = 8;
    public static final boolean DEFAULT_CLEAR_UPLOAD     = false;
    public static final boolean DEFAULT_CLEAR_DOWNLOAD   = false;
    public static final int     DEFAULT_SEARCH_ANIMATION_TIME = 20;
    public static final String  DEFAULT_CONNECT_STRING    = "GNUTELLA CONNECT/0.4";
    public static final String  DEFAULT_CONNECT_OK_STRING = "GNUTELLA OK";
    public static final int     DEFAULT_BASIC_INFO_FOR_QUERY = 1000;
    public static final int     DEFAULT_ADVANCED_INFO_FOR_QUERY = 50;

    public static final boolean DEFAULT_CHECK_AGAIN = true;
    public static final boolean DEFAULT_FORCE_IP_ADDRESS = false;
    public static final byte[]  DEFAULT_FORCED_IP_ADDRESS = {};
    public static final String  DEFAULT_FORCED_IP_ADDRESS_STRING = "";
    public static final int     DEFAULT_FORCED_PORT = 6346;
    public static final int     DEFAULT_FREELOADER_FILES = 1;
    public static final int     DEFAULT_FREELOADER_ALLOWED = 100;
	public static final long    DEFAULT_AVERAGE_UPTIME = 200;
	public static final long    DEFAULT_TOTAL_UPTIME = 0;
	public static final int     DEFAULT_SESSIONS = 1;

    // The property key name constants
	public static final String ALLOW_BROWSER  = "ALLOW_BROWSER";
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
    public static final String PROMPT_EXE_DOWNLOAD="PROMPT_EXE_DOWNLOAD";
    public static final String MAX_UPLOADS     ="MAX_UPLOADS";
    public static final String CLEAR_UPLOAD   = "CLEAR_UPLOAD";
    public static final String CLEAR_DOWNLOAD = "CLEAR_DOWNLOAD";
    public static final String SEARCH_ANIMATION_TIME = "SEARCH_ANIMATION_TIME";
    public static final String SAVE_DEFAULT   = "SAVE_DEFAULT";

    public static final String CONNECT_STRING = "CONNECT_STRING";
    public static final String CONNECT_OK_STRING = "CONNECT_OK_STRING";
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

    public static final String UPLOADS_PER_PERSON = "UPLOADS_PER_PERSON";
    public static final String AVERAGE_UPTIME     = "AVERAGE_UPTIME";
    public static final String TOTAL_UPTIME       = "TOTAL_UPTIME";
    public static final String SESSIONS           = "SESSIONS";
	public static final String DELETE_OLD_JAR     = "DELETE_OLD_JAR";
	public static final String OLD_JAR_NAME       = "OLD_JAR_NAME";
}
