package com.limegroup.gnutella;


import java.util.*;

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

    /** Mutator methods */
    public void setTTL(byte ttl) 
	throws IllegalArgumentException;
    public void setMaxTTL(byte maxttl) 
	throws IllegalArgumentException;
    public void setMaxLength(int maxLength)
	throws IllegalArgumentException;
    public void setTimeout(int timeout)
	throws IllegalArgumentException;
    public void setHostList(String hostList)
	throws IllegalArgumentException;
    public void setKeepAlive(int keepAlive)
	throws IllegalArgumentException;
    public void setPort(int port)
	throws IllegalArgumentException;
    public void setConnectionSpeed(int speed)
	throws IllegalArgumentException;
    public void setSearchLimit(short limit)
	throws IllegalArgumentException;
    public void setStats(boolean stats)
	throws IllegalArgumentException;
    public void setClientID(String clientID)
	throws IllegalArgumentException;

    /** Default settings */
    public static byte    DEFAULT_TTL          = (byte)4;
    public static byte    DEFAULT_MAX_TTL      = (byte)10;
    public static int     DEFAULT_MAX_LENGTH   = 65536;
    public static int     DEFAULT_TIMEOUT      = 4000;
    public static String  DEFAULT_HOST_LIST    = "gnutella.net";
    public static int     DEFAULT_KEEP_ALIVE   = 0;
    public static int     DEFAULT_PORT         = 6346;
    public static int     DEFAULT_SPEED        = 56;
    public static short   DEFAULT_SEARCH_LIMIT = (short)64;

    public static String  DEFAULT_CLIENT_ID    = "A0B447F77853D411B05B0001023AF3D6";
    public static boolean DEFAULT_STATS        = false;

    /** The property key name constants */
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

    /** Default name for the properties file */
    public static String FILE_NAME            = "limewire.props";
}




