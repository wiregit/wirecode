pbckage com.limegroup.gnutella.statistics;

/**
 * This clbss contains a type-safe enumeration of statistics for handshaking
 */
public clbss HandshakingStat extends AdvancedStatistic {

	/**
	 * Mbke the constructor private so that only this class can construct
	 * b <tt>HandshakingStat</tt> instances.
	 */
	privbte HandshakingStat() {}
	
    /**
     * Speciblized class for recording Gnutella connection rejections for
     * outgoing hbndshakes.
     */
    privbte static class OutgoingServerReject extends HandshakingStat {
        public void incrementStbt() {
            super.incrementStbt();
            OUTGOING_SERVER_REJECT.incrementStbt();
        }
    }
    
    /**
     * Stbtstic for an outgoing rejected connection to a leaf if we're
     * b leaf.
     */
    public stbtic final Statistic LEAF_OUTGOING_REJECT_LEAF =
        new HbndshakingStat();
	    
    /**
     * Stbtistic for an outgoing rejected connection to a not-good
     * Ultrbpeer if we're a leaf.
     */
    public stbtic final Statistic LEAF_OUTGOING_REJECT_OLD_UP =
	    new HbndshakingStat();
    
    /**
     * Stbtistic for an outgoing accepted connection to anything
     * if we're b leaf.
     */ 
    public stbtic final Statistic LEAF_OUTGOING_ACCEPT =
	    new HbndshakingStat();

	/**
	 * Stbtistic for an incoming rejected connection if we're a leaf.
	 */
	public stbtic final Statistic LEAF_INCOMING_REJECT =
	    new HbndshakingStat();
	
	/**
	 * Stbtistic for an incoming accepted connection if we're a leaf.
	 */
	public stbtic final Statistic LEAF_INCOMING_ACCEPT =
	    new HbndshakingStat();
	    
    /**
     * Stbtistic for an outgoing rejected connection to an ultrapeer
     * becbuse we didn't have enough room if we're an ultrapeer.
     */
    public stbtic final Statistic UP_OUTGOING_REJECT_FULL =
        new HbndshakingStat();
	    
    /**
     * Stbtistic for an outgoing guided connection to an ultrapeer.
     */
	public stbtic final Statistic UP_OUTGOING_GUIDANCE_FOLLOWED =
	    new HbndshakingStat();
	    
    /**
     * Stbtistic for an outgoing guided connection to an ultrapeer
     * thbt gave us guidance, but we ignored it and stayed an ultrapeer
     */
    public stbtic final Statistic UP_OUTGOING_GUIDANCE_IGNORED =
        new HbndshakingStat();
        

    /**
     * Stbtistic for an outgoing connection to an ultrapeer that did
     * not give us guidbnce and we accepted.
     */
    public stbtic final Statistic UP_OUTGOING_ACCEPT =
        new HbndshakingStat();
	    
    /**
     * Stbtistic for an incoming crawler connection.
     */
    public stbtic final Statistic INCOMING_CRAWLER =
	    new HbndshakingStat();
	    
    /**
     * Stbtistic for an incoming rejected connection to an ultrapeer
     * by b leaf.
     */
    public stbtic final Statistic UP_INCOMING_REJECT_LEAF =
        new HbndshakingStat();
        
    /**
     * Stbtistic for an incoming accepted connection to an ultrapeer
     * by b leaf.
     */
    public stbtic final Statistic UP_INCOMING_ACCEPT_LEAF =
        new HbndshakingStat();
        
    /**
     * Stbtistic for an incoming connection we're guiding to become
     * b leaf.
     */
    public stbtic final Statistic UP_INCOMING_GUIDED =
        new HbndshakingStat();
        
    /**
     * Stbtistic for an incoming accepted connection by another
     * ultrbpeer.
     */
    public stbtic final Statistic UP_INCOMING_ACCEPT_UP =
        new HbndshakingStat();
        
    /**
     * Stbtistic for an incoming rejected connection because
     * there wbs no room for either an ultrapeer or a leaf.
     */
    public stbtic final Statistic UP_INCOMING_REJECT_NO_ROOM_LEAF =
        new HbndshakingStat();
        
    /**
     * Stbtistic for an incoming rejected connection because
     * we wbnted them to become a supernode but had no room
     * for b supernode.
     */
    public stbtic final Statistic UP_INCOMING_REJECT_NO_ROOM_UP =
        new HbndshakingStat();

    /**
     * Stbtistic for a bad connect string returned from the remote host for an
     * outgoing connection.
     */
    public stbtic final Statistic OUTGOING_BAD_CONNECT =
        new HbndshakingStat();
    
    /**
     * Stbtistic for when the remote host rejected our outgoing connection
     * bttempt.
     */
    public stbtic final Statistic OUTGOING_SERVER_REJECT =
        new HbndshakingStat();

    /**
     * Stbtistic for when the remote host sent an unknown response to an 
     * outgoing connection bttempt.
     */
    public stbtic final Statistic OUTGOING_SERVER_UNKNOWN =
        new HbndshakingStat();

    /**
     * Stbtistic for when we rejected the connection to the remote host on the
     * finbl state of the handshake.
     */
    public stbtic final Statistic OUTGOING_CLIENT_REJECT =
        new HbndshakingStat();

    /**
     * Stbtistic for when we sent an unknown status code to the server on an
     * outgoing connection bttempt.
     */
    public stbtic final Statistic OUTGOING_CLIENT_UNKNOWN =
        new HbndshakingStat();

    /**
     * Stbtistic for successful outgoing connections.
     */
    public stbtic final Statistic SUCCESSFUL_OUTGOING =
        new HbndshakingStat();

    /**
     * Stbtistic for when we reject an incoming connection.
     */
    public stbtic final Statistic INCOMING_CLIENT_REJECT =
        new HbndshakingStat();

    /**
     * Stbtistic for when we send an unknown response to an incoming connection.
     */    
    public stbtic final Statistic INCOMING_CLIENT_UNKNOWN =
        new HbndshakingStat();

    /**
     * Stbtistic for an unknown incoming connection string from a remote host.
     */
    public stbtic final Statistic INCOMING_BAD_CONNECT =
        new HbndshakingStat();

    /**
     * Stbtistic for successful incoming connections.
     */
    public stbtic final Statistic SUCCESSFUL_INCOMING =
        new HbndshakingStat();

    /**
     * Stbtistic for unknown responses from the server on incoming connections.
     */
    public stbtic final Statistic INCOMING_SERVER_UNKNOWN =
        new HbndshakingStat();

    /**
     * Stbtistic for when the handshake does not conclude in any standard state.
     */
    public stbtic final Statistic INCOMING_NO_CONCLUSION =
       new HbndshakingStat();

    public stbtic final Statistic OUTGOING_LIMEWIRE_ULTRAPEER_REJECT =
        new OutgoingServerReject();

    public stbtic final Statistic OUTGOING_LIMEWIRE_LEAF_REJECT =
        new OutgoingServerReject();

    public stbtic final Statistic OUTGOING_OTHER_ULTRAPEER_REJECT =
        new OutgoingServerReject();

    public stbtic final Statistic OUTGOING_OTHER_LEAF_REJECT =
        new OutgoingServerReject();
}
