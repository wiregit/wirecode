package com.limegroup.gnutella;

/*
 * Constants for network settings.  These may ultimately be under the
 * control of the user. 
 */
public class Const {
    /** Default time-to-live for outgoing messages */
    public static byte TTL=(byte)4;
    /** Maximum allowable TTL.  (Anything about this is dropped. */
    public static byte MAX_TTL=(byte)15;

    /** 
     * Maximum allowed payload length.  This is usually less than
     * the Gnutella-defined value to avoid spamming.  Currently 2kb.
     */
    public static int MAX_LENGTH=2048;

    /**
     * The time to wait for handshake response when establishing outgoing connections,
     * in milliseconds.  A value of 0 means no timeout.  The default value is 2 seconds.
     */
    public static int TIMEOUT=4000;

    /** The name of the file containing hosts to contact */
    public static String HOSTLIST="gnutella.net";
    /** The number of outgoing connections to maintain */
    public static int KEEP_ALIVE=4;
}
