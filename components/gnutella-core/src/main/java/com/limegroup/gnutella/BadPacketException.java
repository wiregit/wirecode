package com.limegroup.gnutella;

/** 
 * An exception for reading bad data from the network. 
 * This is generally non-fatal.
 */
public class BadPacketException extends Exception {
    public BadPacketException() { }
    public BadPacketException(String msg) { super(msg); }
}
