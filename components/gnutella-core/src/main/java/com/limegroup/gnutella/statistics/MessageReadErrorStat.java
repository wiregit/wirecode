package com.limegroup.gnutella.statistics;

/**
 * @author afisk
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class MessageReadErrorStat extends AdvancedStatistic {

    /**
     * 
     */
    private MessageReadErrorStat() {}

    /**
     * Statistic for reading errors due to the connection being closed during
     * header reading.
     */
    public static final Statistic CONNECTION_CLOSED_READING_HEADER =
        new MessageReadErrorStat();
        
    /**
     * Statistic for reading errors due to the connection being closed during
     * payload reading.
     */
    public static final Statistic CONNECTION_CLOSED_READING_PAYLOAD =
        new MessageReadErrorStat();
        
    /**
     * Statistic for reading errors due to an invalid header.
     */
    public static final Statistic INVALID_HEADER =
        new MessageReadErrorStat();
        
    /**
     * Statistic for reading errors due to an invalid payload.
     */
    public static final Statistic INVALID_PAYLOAD =
        new MessageReadErrorStat();
 
    /**
     * Statistic for reading errors due to an invalid message length.
     */
    public static final Statistic BAD_MESSAGE_LENGTH =
        new MessageReadErrorStat();       
        
    /**
     * Statistic for reading errors due to a negative hops value.
     */
    public static final Statistic NEGATIVE_HOPS =
        new MessageReadErrorStat();       
        
    /**
     * Statistic for reading errors due to a negative ttl value.
     */
    public static final Statistic NEGATIVE_TTL =
        new MessageReadErrorStat();      

    /**
     * Statistic for reading errors due to hops being over the soft max.
     */
    public static final Statistic HOPS_OVER_SOFT_MAX =
        new MessageReadErrorStat();      

    /**
     * Statistic for reading errors due to hops plus TTL over hard max.
     */
    public static final Statistic HOPS_PLUS_TTL_OVER_HARD_MAX =
        new MessageReadErrorStat();      

    /**
     * Statistic for BadPacketException errors reading messages.
     */        
    public static final Statistic BAD_PACKET_EXCEPTIONS =
        new MessageReadErrorStat();

    /**
     * Statistic for IOException errors reading messages.
     */        
    public static final Statistic IO_EXCEPTIONS =
        new MessageReadErrorStat();
}
