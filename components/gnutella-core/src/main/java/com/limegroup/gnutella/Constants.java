
// Edited for the Learning branch

package com.limegroup.gnutella;

/**
* A class to keep together the constants that may be used by multiple classes
* @author  Anurag Singla
*/
public final class Constants {

    private Constants() {}

    /** "," a comma in a string, used as a separator in text. */
    public static final String ENTRY_SEPARATOR = ",";
    
    /**
     * Mime Type to be used when returning QueryReplies on receiving a
     * HTTP request (or some other content request)
     */
    public static final String QUERYREPLY_MIME_TYPE = 
        "application/x-gnutella-packets";
    
    /**
     * 8 seconds, the timeout to use on sockets
     */
    public static final int TIMEOUT = 8000;  

    /**
     * how long a minute is.  Not final so that tests can change it.
     */
    public static long MINUTE = 60*1000;
}
