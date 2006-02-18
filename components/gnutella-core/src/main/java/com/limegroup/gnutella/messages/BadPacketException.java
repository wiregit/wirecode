
// Commented for the Learning branch

package com.limegroup.gnutella.messages;

/**
 * Throw a BadPacketException when a remote computer sends a packet that has a part that can't be correct.
 * 
 * A remote computer is sending us Gnutella packets over our TCP socket connection to it.
 * Code in LimeWire parses the packet, identifying parts and reading data from it.
 * If a part it reads contains data that cannot be correct, the code will throw a BadPacketException.
 * 
 * This doesn't mean that we have to disconnect from the remote computer.
 * We can just move on to the next packet and try again.
 */
public class BadPacketException extends Exception {

    /**
     * Make a new BadPacketException to throw when packet data is corrupted.
     */
    public BadPacketException() {}

    /**
     * Make a new BadPacketException to throw when packet data is corrupted.
     * 
     * @param msg Text that describes what part wasn't right
     */
    public BadPacketException(String msg) {

        // Call the Exception constructor to have it save the 
        super(msg);
    }

    /*
     * TODO:kfaaborg None of the following cached BadPacketException exceptions are in use.
     */

    /**
     * Not used.
     * 
     * Reusable exception for efficiency that can be statically accessed.  These are created a lot, so it makes sense to cache it.
     */
    public static final BadPacketException HOPS_EXCEED_SOFT_MAX = new BadPacketException("Hops already exceeds soft maximum");

    /**
     * Not used.
     * 
     * Cached exception for not handling URN queries.
     */
    public static final BadPacketException CANNOT_ACCEPT_URN_QUERIES = new BadPacketException("cannot accept URN queries");

    /**
     * Not used.
     * 
     * Cached exception for queries that are too big.
     */
    public static final BadPacketException QUERY_TOO_BIG = new BadPacketException("query too big");

    /**
     * Not used.
     * 
     * Cached exception for XML queries that are too big.
     */
    public static final BadPacketException XML_QUERY_TOO_BIG = new BadPacketException("XML query too big");

    /**
     * Not used.
     * 
     * Cached exception for queries that have illegal characters.
     */
    public static final BadPacketException ILLEGAL_CHAR_IN_QUERY = new BadPacketException("illegal chars in query");
}
