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
     * Statistic for an outgoing rejected connection to a non-ultrapeer
     * if we're an ultrapeer.
     */
    public static final Statistic UP_OUTGOING_REJECT_NON_UP =
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
	public static final Statistic UP_OUTGOING_ACCEPT_GUIDANCE =
	    new HandshakingStat();
	    
    /**
     * Statistic for an outgoing guided connection to an ultrapeer
     * that was rejected because we didn't want to become a leaf
     * to that ultrapeer.
     */
    public static final Statistic UP_OUTGOING_REJECT_GUIDANCE =
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
}
