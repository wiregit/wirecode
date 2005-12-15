
// Edited for the Learning branch

package com.limegroup.gnutella.statistics;

/**
 * This class contains a type-safe enumeration of statistics for handshaking
 */
public class HandshakingStat extends AdvancedStatistic {

	/**
	 * Make the constructor private so that only this class can construct
	 * a <tt>HandshakingStat</tt> instances.
	 */
	private HandshakingStat() {}
	
    /**
     * Specialized class for recording Gnutella connection rejections for
     * outgoing handshakes.
     */
    private static class OutgoingServerReject extends HandshakingStat {
        public void incrementStat() {
            super.incrementStat();
            OUTGOING_SERVER_REJECT.incrementStat();
        }
    }
    
    /**
     * Statstic for an outgoing rejected connection to a leaf if we're
     * a leaf.
     */
    public static final Statistic LEAF_OUTGOING_REJECT_LEAF =
        new HandshakingStat();
	    
    /**
     * Statistic for an outgoing rejected connection to a not-good
     * Ultrapeer if we're a leaf.
     */
    public static final Statistic LEAF_OUTGOING_REJECT_OLD_UP =
	    new HandshakingStat();
    
    /**
     * Statistic for an outgoing accepted connection to anything
     * if we're a leaf.
     */ 
    public static final Statistic LEAF_OUTGOING_ACCEPT =
	    new HandshakingStat();

	/**
	 * Statistic for an incoming rejected connection if we're a leaf.
	 */
	public static final Statistic LEAF_INCOMING_REJECT =
	    new HandshakingStat();
	
	/**
	 * Statistic for an incoming accepted connection if we're a leaf.
	 */
	public static final Statistic LEAF_INCOMING_ACCEPT =
	    new HandshakingStat();
	    
    /**
     * Statistic for an outgoing rejected connection to an ultrapeer
     * because we didn't have enough room if we're an ultrapeer.
     */
    public static final Statistic UP_OUTGOING_REJECT_FULL =
        new HandshakingStat();
	    
    /**
     * Statistic for an outgoing guided connection to an ultrapeer.
     */
	public static final Statistic UP_OUTGOING_GUIDANCE_FOLLOWED =
	    new HandshakingStat();
	    
    /**
     * Statistic for an outgoing guided connection to an ultrapeer
     * that gave us guidance, but we ignored it and stayed an ultrapeer
     */
    public static final Statistic UP_OUTGOING_GUIDANCE_IGNORED =
        new HandshakingStat();
        

    /**
     * Statistic for an outgoing connection to an ultrapeer that did
     * not give us guidance and we accepted.
     */
    public static final Statistic UP_OUTGOING_ACCEPT =
        new HandshakingStat();
	    
    /**
     * Statistic for an incoming crawler connection.
     */
    public static final Statistic INCOMING_CRAWLER =
	    new HandshakingStat();
	    
    /**
     * Statistic for an incoming rejected connection to an ultrapeer
     * by a leaf.
     */
    public static final Statistic UP_INCOMING_REJECT_LEAF =
        new HandshakingStat();
        
    /**
     * Statistic for an incoming accepted connection to an ultrapeer
     * by a leaf.
     */
    public static final Statistic UP_INCOMING_ACCEPT_LEAF =
        new HandshakingStat();
        
    /**
     * Statistic for an incoming connection we're guiding to become
     * a leaf.
     */
    public static final Statistic UP_INCOMING_GUIDED =
        new HandshakingStat();
        
    /**
     * Statistic for an incoming accepted connection by another
     * ultrapeer.
     */
    public static final Statistic UP_INCOMING_ACCEPT_UP =
        new HandshakingStat();
        
    /**
     * Statistic for an incoming rejected connection because
     * there was no room for either an ultrapeer or a leaf.
     */
    public static final Statistic UP_INCOMING_REJECT_NO_ROOM_LEAF =
        new HandshakingStat();
        
    /**
     * Statistic for an incoming rejected connection because
     * we wanted them to become a supernode but had no room
     * for a supernode.
     */
    public static final Statistic UP_INCOMING_REJECT_NO_ROOM_UP =
        new HandshakingStat();

    /**
     * We connected to a computer, sent our stage 1 headers, and it said something that doesn't start "GNUTELLA/0.6".
     */
    public static final Statistic OUTGOING_BAD_CONNECT = new HandshakingStat();
    
    /**
     * Statistic for when the remote host rejected our outgoing connection
     * attempt.
     */
    public static final Statistic OUTGOING_SERVER_REJECT =
        new HandshakingStat();

    /**
     * Statistic for when the remote host sent an unknown response to an 
     * outgoing connection attempt.
     */
    public static final Statistic OUTGOING_SERVER_UNKNOWN =
        new HandshakingStat();

    /**
     * Statistic for when we rejected the connection to the remote host on the
     * final state of the handshake.
     */
    public static final Statistic OUTGOING_CLIENT_REJECT =
        new HandshakingStat();

    /**
     * Statistic for when we sent an unknown status code to the server on an
     * outgoing connection attempt.
     */
    public static final Statistic OUTGOING_CLIENT_UNKNOWN =
        new HandshakingStat();

    /**
     * Statistic for successful outgoing connections.
     */
    public static final Statistic SUCCESSFUL_OUTGOING =
        new HandshakingStat();

    /** A remote computer connected to us, and we rejected it with stage 2 headers that said 503. */
    public static final Statistic INCOMING_CLIENT_REJECT = new HandshakingStat();

    /** A remote computer connected to us, and we rejected it with stage 2 headers that had an unusual status code. */
    public static final Statistic INCOMING_CLIENT_UNKNOWN = new HandshakingStat();

    /** The remote computer connected to us, we sent stage 2, then they sent stage 3 that didn't start "GNUTELLA/0.6". */
    public static final Statistic INCOMING_BAD_CONNECT = new HandshakingStat();

    /** We've successfully finished shaking hands with a remote computer that contacted us. */
    public static final Statistic SUCCESSFUL_INCOMING = new HandshakingStat();

    /**
     * Statistic for unknown responses from the server on incoming connections.
     */
    public static final Statistic INCOMING_SERVER_UNKNOWN =
        new HandshakingStat();

    /** This statistic was written for the unfinished challenge response handshaking feature, and is not used. */
    public static final Statistic INCOMING_NO_CONCLUSION = new HandshakingStat();

    /**
     * We connected to a LimeWire ultrapeer, and it rejected our connection in stage 2.
     */
    public static final Statistic OUTGOING_LIMEWIRE_ULTRAPEER_REJECT = new OutgoingServerReject();

    /**
     * We connected to a LimeWire leaf, and it rejected our connection in stage 2.
     */
    public static final Statistic OUTGOING_LIMEWIRE_LEAF_REJECT = new OutgoingServerReject();
    
    /**
     * We connected to an ultrapeer running something other than LimeWire, and it rejected our connection in stage 2.
     */
    public static final Statistic OUTGOING_OTHER_ULTRAPEER_REJECT = new OutgoingServerReject();
    
    /**
     * We connected to a remote leaf running something other than LimeWire, and it rejected our connection in stage 2.
     */
    public static final Statistic OUTGOING_OTHER_LEAF_REJECT = new OutgoingServerReject();
}
