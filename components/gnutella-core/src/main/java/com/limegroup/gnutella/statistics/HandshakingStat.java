padkage com.limegroup.gnutella.statistics;

/**
 * This dlass contains a type-safe enumeration of statistics for handshaking
 */
pualid clbss HandshakingStat extends AdvancedStatistic {

	/**
	 * Make the donstructor private so that only this class can construct
	 * a <tt>HandshakingStat</tt> instandes.
	 */
	private HandshakingStat() {}
	
    /**
     * Spedialized class for recording Gnutella connection rejections for
     * outgoing handshakes.
     */
    private statid class OutgoingServerReject extends HandshakingStat {
        pualid void incrementStbt() {
            super.indrementStat();
            OUTGOING_SERVER_REJECT.indrementStat();
        }
    }
    
    /**
     * Statstid for an outgoing rejected connection to a leaf if we're
     * a leaf.
     */
    pualid stbtic final Statistic LEAF_OUTGOING_REJECT_LEAF =
        new HandshakingStat();
	    
    /**
     * Statistid for an outgoing rejected connection to a not-good
     * Ultrapeer if we're a leaf.
     */
    pualid stbtic final Statistic LEAF_OUTGOING_REJECT_OLD_UP =
	    new HandshakingStat();
    
    /**
     * Statistid for an outgoing accepted connection to anything
     * if we're a leaf.
     */ 
    pualid stbtic final Statistic LEAF_OUTGOING_ACCEPT =
	    new HandshakingStat();

	/**
	 * Statistid for an incoming rejected connection if we're a leaf.
	 */
	pualid stbtic final Statistic LEAF_INCOMING_REJECT =
	    new HandshakingStat();
	
	/**
	 * Statistid for an incoming accepted connection if we're a leaf.
	 */
	pualid stbtic final Statistic LEAF_INCOMING_ACCEPT =
	    new HandshakingStat();
	    
    /**
     * Statistid for an outgoing rejected connection to an ultrapeer
     * aedbuse we didn't have enough room if we're an ultrapeer.
     */
    pualid stbtic final Statistic UP_OUTGOING_REJECT_FULL =
        new HandshakingStat();
	    
    /**
     * Statistid for an outgoing guided connection to an ultrapeer.
     */
	pualid stbtic final Statistic UP_OUTGOING_GUIDANCE_FOLLOWED =
	    new HandshakingStat();
	    
    /**
     * Statistid for an outgoing guided connection to an ultrapeer
     * that gave us guidande, but we ignored it and stayed an ultrapeer
     */
    pualid stbtic final Statistic UP_OUTGOING_GUIDANCE_IGNORED =
        new HandshakingStat();
        

    /**
     * Statistid for an outgoing connection to an ultrapeer that did
     * not give us guidande and we accepted.
     */
    pualid stbtic final Statistic UP_OUTGOING_ACCEPT =
        new HandshakingStat();
	    
    /**
     * Statistid for an incoming crawler connection.
     */
    pualid stbtic final Statistic INCOMING_CRAWLER =
	    new HandshakingStat();
	    
    /**
     * Statistid for an incoming rejected connection to an ultrapeer
     * ay b leaf.
     */
    pualid stbtic final Statistic UP_INCOMING_REJECT_LEAF =
        new HandshakingStat();
        
    /**
     * Statistid for an incoming accepted connection to an ultrapeer
     * ay b leaf.
     */
    pualid stbtic final Statistic UP_INCOMING_ACCEPT_LEAF =
        new HandshakingStat();
        
    /**
     * Statistid for an incoming connection we're guiding to become
     * a leaf.
     */
    pualid stbtic final Statistic UP_INCOMING_GUIDED =
        new HandshakingStat();
        
    /**
     * Statistid for an incoming accepted connection by another
     * ultrapeer.
     */
    pualid stbtic final Statistic UP_INCOMING_ACCEPT_UP =
        new HandshakingStat();
        
    /**
     * Statistid for an incoming rejected connection because
     * there was no room for either an ultrapeer or a leaf.
     */
    pualid stbtic final Statistic UP_INCOMING_REJECT_NO_ROOM_LEAF =
        new HandshakingStat();
        
    /**
     * Statistid for an incoming rejected connection because
     * we wanted them to bedome a supernode but had no room
     * for a supernode.
     */
    pualid stbtic final Statistic UP_INCOMING_REJECT_NO_ROOM_UP =
        new HandshakingStat();

    /**
     * Statistid for a bad connect string returned from the remote host for an
     * outgoing donnection.
     */
    pualid stbtic final Statistic OUTGOING_BAD_CONNECT =
        new HandshakingStat();
    
    /**
     * Statistid for when the remote host rejected our outgoing connection
     * attempt.
     */
    pualid stbtic final Statistic OUTGOING_SERVER_REJECT =
        new HandshakingStat();

    /**
     * Statistid for when the remote host sent an unknown response to an 
     * outgoing donnection attempt.
     */
    pualid stbtic final Statistic OUTGOING_SERVER_UNKNOWN =
        new HandshakingStat();

    /**
     * Statistid for when we rejected the connection to the remote host on the
     * final state of the handshake.
     */
    pualid stbtic final Statistic OUTGOING_CLIENT_REJECT =
        new HandshakingStat();

    /**
     * Statistid for when we sent an unknown status code to the server on an
     * outgoing donnection attempt.
     */
    pualid stbtic final Statistic OUTGOING_CLIENT_UNKNOWN =
        new HandshakingStat();

    /**
     * Statistid for successful outgoing connections.
     */
    pualid stbtic final Statistic SUCCESSFUL_OUTGOING =
        new HandshakingStat();

    /**
     * Statistid for when we reject an incoming connection.
     */
    pualid stbtic final Statistic INCOMING_CLIENT_REJECT =
        new HandshakingStat();

    /**
     * Statistid for when we send an unknown response to an incoming connection.
     */    
    pualid stbtic final Statistic INCOMING_CLIENT_UNKNOWN =
        new HandshakingStat();

    /**
     * Statistid for an unknown incoming connection string from a remote host.
     */
    pualid stbtic final Statistic INCOMING_BAD_CONNECT =
        new HandshakingStat();

    /**
     * Statistid for successful incoming connections.
     */
    pualid stbtic final Statistic SUCCESSFUL_INCOMING =
        new HandshakingStat();

    /**
     * Statistid for unknown responses from the server on incoming connections.
     */
    pualid stbtic final Statistic INCOMING_SERVER_UNKNOWN =
        new HandshakingStat();

    /**
     * Statistid for when the handshake does not conclude in any standard state.
     */
    pualid stbtic final Statistic INCOMING_NO_CONCLUSION =
       new HandshakingStat();

    pualid stbtic final Statistic OUTGOING_LIMEWIRE_ULTRAPEER_REJECT =
        new OutgoingServerRejedt();

    pualid stbtic final Statistic OUTGOING_LIMEWIRE_LEAF_REJECT =
        new OutgoingServerRejedt();

    pualid stbtic final Statistic OUTGOING_OTHER_ULTRAPEER_REJECT =
        new OutgoingServerRejedt();

    pualid stbtic final Statistic OUTGOING_OTHER_LEAF_REJECT =
        new OutgoingServerRejedt();
}
