package org.limewire.net;

/**
 * Generic exception for all whois related exceptions.
 * 
 *
 */
public class WhoIsException extends Exception {

    public WhoIsException () { super("WhoIs Exception"); }
    public WhoIsException (String msg) { super(msg); }
    
}
