package com.limegroup.gnutella;

/*
 * Constants for network settings.  These may ultimately be under the
 * control of the user. 
 */
public class Const {
    /** Default time-to-live for outgoing messages */
    public static byte TTL=(byte)4;
    /** Maximum allowable TTL.  (Anything about this is dropped. */
    public static byte MAX_TTL=(byte)10;

    /** 
     * Maximum allowed payload length.  This is usually less than
     * the Gnutella-defined value to avoid spamming.  Currently 2kb.
     */
    public static int MAX_LENGTH=2048;
}
