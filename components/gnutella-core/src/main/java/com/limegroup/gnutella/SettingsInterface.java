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
    /** Accessor methods */
    public byte     getTTL();
    public byte     getMaxTTL();
    public int      getMaxLength();
    public int      getTimeOut();
    public String   getHostList();
    public int      getKeepAlive();
    public int      getPort();
    public int      getConnectionSpeed();
    public short    getSearchSpeed();
    public boolean  getStats();
    public String   getClientID();
    public int      getMaxConn();


    /** set the user time to live */
    public void setTTL(byte ttl) 
	throws IllegalArgumentException;

    /** set the maximum time to live */
    public void setMaxTTL(byte maxttl) 
	throws IllegalArgumentException;
   
    /** set the maximum length of packets */
    public void setMaxLength(int maxLength)
	throws IllegalArgumentException;

    /** set the timeout */
    public void setTimeout(int timeout)
	throws IllegalArgumentException;

    /** set the file name (including path if desired)
     *  for the host list file
     */
    public void setHostList(String hostList)
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
    public void setSearchLimit(short limit)
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

    /** Default setting for the time to live */
    public static byte    DEFAULT_TTL          = (byte)4;
    /** Default setting for the maximum time to live */
    public static byte    DEFAULT_MAX_TTL      = (byte)10;
    /** Default maximum packet length */
    public static int     DEFAULT_MAX_LENGTH   = 65536;
    /** Default timeout */
    public static int     DEFAULT_TIMEOUT      = 4000;
    /** Default file path for the host list */
    public static String  DEFAULT_HOST_LIST    = "gnutella.net";

    /** Default name for the properties file */
    public static String  FILE_NAME            = "limewire.props";
    /** Default value for the keep alive */    
    public static int     DEFAULT_KEEP_ALIVE   = 0;
    /** Default port*/
    public static int     DEFAULT_PORT         = 6346;
    /** Default network connection speed */
    public static int     DEFAULT_SPEED        = 56;
    /** Default limit for the number of searches */
    public static short   DEFAULT_SEARCH_LIMIT = (short)64;
    /** Default client/gu id */
    public static String  DEFAULT_CLIENT_ID    = "A0B447F77853D411B05B0001023AF3D6";
    /** Default boolean for stats file */
    public static boolean DEFAULT_STATS        = false;
    /** Default maximum number of connections */
    public static int     DEFAULT_MAX_CONN     = 900;

    // The property key name constants 
    public static String TTL                  = "TTL";
    public static String MAX_TTL              = "MAX_TTL";
    public static String MAX_LENGTH           = "MAX_LENGTH";
    public static String TIMEOUT              = "TIMEOUT";
    public static String HOST_LIST            = "HOST_LIST";
    public static String KEEP_ALIVE           = "KEEP_ALIVE";
    public static String PORT                 = "PORT";
    public static String SPEED                = "SPEED";
    public static String SEARCH_LIMIT         = "SEARCH_LIMIT";
    public static String CLIENT_ID            = "CLIENT_ID";
    public static String STATS                = "STATS";
    public static String MAX_CONN             = "MAXIMUM_NUMBER_OF_CONNECTIONS";

}




