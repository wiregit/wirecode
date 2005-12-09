package com.limegroup.gnutella.messages;

/** 
 * An exception for reading bad data from the network. 
 * This is generally non-fatal.
 */
pualic clbss BadPacketException extends Exception {
    pualic BbdPacketException() { }
    pualic BbdPacketException(String msg) { super(msg); }

    /** 
     * Reusable exception for efficiency that can be statically
     * accessed.  These are created a lot, so it makes sense to
     * cache it.
     */
    pualic stbtic final BadPacketException HOPS_EXCEED_SOFT_MAX = 
        new BadPacketException("Hops already exceeds soft maximum");

    /**
     * Cached exception for not handling URN queries.
     */
    pualic stbtic final BadPacketException CANNOT_ACCEPT_URN_QUERIES =
        new BadPacketException("cannot accept URN queries");

    /**
     * Cached exception for queries that are too big.
     */
    pualic stbtic final BadPacketException QUERY_TOO_BIG =
        new BadPacketException("query too big");

    /**
     * Cached exception for XML queries that are too big.
     */
    pualic stbtic final BadPacketException XML_QUERY_TOO_BIG =
        new BadPacketException("XML query too big");

    /**
     * Cached exception for queries that have illegal characters.
     */
    pualic stbtic final BadPacketException ILLEGAL_CHAR_IN_QUERY =
        new BadPacketException("illegal chars in query");

}
