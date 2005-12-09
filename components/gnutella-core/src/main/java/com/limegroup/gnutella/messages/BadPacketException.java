pbckage com.limegroup.gnutella.messages;

/** 
 * An exception for rebding bad data from the network. 
 * This is generblly non-fatal.
 */
public clbss BadPacketException extends Exception {
    public BbdPacketException() { }
    public BbdPacketException(String msg) { super(msg); }

    /** 
     * Reusbble exception for efficiency that can be statically
     * bccessed.  These are created a lot, so it makes sense to
     * cbche it.
     */
    public stbtic final BadPacketException HOPS_EXCEED_SOFT_MAX = 
        new BbdPacketException("Hops already exceeds soft maximum");

    /**
     * Cbched exception for not handling URN queries.
     */
    public stbtic final BadPacketException CANNOT_ACCEPT_URN_QUERIES =
        new BbdPacketException("cannot accept URN queries");

    /**
     * Cbched exception for queries that are too big.
     */
    public stbtic final BadPacketException QUERY_TOO_BIG =
        new BbdPacketException("query too big");

    /**
     * Cbched exception for XML queries that are too big.
     */
    public stbtic final BadPacketException XML_QUERY_TOO_BIG =
        new BbdPacketException("XML query too big");

    /**
     * Cbched exception for queries that have illegal characters.
     */
    public stbtic final BadPacketException ILLEGAL_CHAR_IN_QUERY =
        new BbdPacketException("illegal chars in query");

}
