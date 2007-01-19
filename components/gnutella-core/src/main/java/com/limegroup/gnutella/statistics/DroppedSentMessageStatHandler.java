package com.limegroup.gnutella.statistics;

import org.limewire.statistic.Statistic;


/**
 * Wrapper class for keeping track of Gnutella message data.  For a given
 * Gnutella message, this class provides the simultaneous updating of both 
 * the number of messages sent and the total bytes sent.  All calls to add
 * data for sent Gnutella message statistics should go through this class 
 * to avoid losing any data.
 */
public class DroppedSentMessageStatHandler extends AbstractStatHandler {

	/**
	 * Creates a new <tt>DroppedSentMessageStatHandler</tt> instance.  
	 * Private constructor to ensure that no other classes can
	 * construct this class, following the type-safe enum pattern.
	 *
	 * @param numberStat the statistic that is simply incremented with
	 *  each new message
	 * @param byteStat the statistic for keeping track of the total bytes
	 */
	private DroppedSentMessageStatHandler(Statistic numberStat, 
										  Statistic byteStat,
										  Statistic limeNumberStat,
										  Statistic limeByteStat) {
		super(numberStat, byteStat, limeNumberStat, limeByteStat);
		
	}	

	/**
	 * <tt>DroppedSentMessageStatHandler</tt> for Gnutella pings sent over UDP.
	 */
	public static final DroppedSentMessageStatHandler UDP_PING_REQUESTS = 
		new DroppedSentMessageStatHandler(DroppedSentMessageStat.UDP_PING_REQUESTS, 
								   DroppedSentMessageStatBytes.UDP_PING_REQUESTS,
								   DroppedLimeSentMessageStat.UDP_PING_REQUESTS, 
								   DroppedLimeSentMessageStatBytes.UDP_PING_REQUESTS);

	/**
	 * <tt>DroppedSentMessageStatHandler</tt> for Gnutella pings sent over TCP.
	 */
	public static final DroppedSentMessageStatHandler TCP_PING_REQUESTS = 
		new DroppedSentMessageStatHandler(DroppedSentMessageStat.TCP_PING_REQUESTS,
								   DroppedSentMessageStatBytes.TCP_PING_REQUESTS,
								   DroppedLimeSentMessageStat.TCP_PING_REQUESTS,
								   DroppedLimeSentMessageStatBytes.TCP_PING_REQUESTS);
								   
	/**
	 * <tt>DroppedSentMessageStatHandler</tt> for Gnutella pings sent over MULTICAST.
	 */
	public static final DroppedSentMessageStatHandler MULTICAST_PING_REQUESTS = 
		new DroppedSentMessageStatHandler(DroppedSentMessageStat.MULTICAST_PING_REQUESTS,
								   DroppedSentMessageStatBytes.MULTICAST_PING_REQUESTS,
								   DroppedLimeSentMessageStat.MULTICAST_PING_REQUESTS,
								   DroppedLimeSentMessageStatBytes.MULTICAST_PING_REQUESTS);								   

	/**
	 * <tt>DroppedSentMessageStatHandler</tt> for Gnutella pongs sent over UDP.
	 */
	public static final DroppedSentMessageStatHandler UDP_PING_REPLIES = 
	    new DroppedSentMessageStatHandler(DroppedSentMessageStat.UDP_PING_REPLIES, 
								   DroppedSentMessageStatBytes.UDP_PING_REPLIES,
								   DroppedLimeSentMessageStat.UDP_PING_REPLIES, 
								   DroppedLimeSentMessageStatBytes.UDP_PING_REPLIES);

	/**
	 * <tt>DroppedSentMessageStatHandler</tt> for Gnutella pongs sent over TCP.
	 */
	public static final DroppedSentMessageStatHandler TCP_PING_REPLIES = 
	    new DroppedSentMessageStatHandler(DroppedSentMessageStat.TCP_PING_REPLIES, 
								   DroppedSentMessageStatBytes.TCP_PING_REPLIES,
								   DroppedLimeSentMessageStat.TCP_PING_REPLIES, 
								   DroppedLimeSentMessageStatBytes.TCP_PING_REPLIES);
								   
	/**
	 * <tt>DroppedSentMessageStatHandler</tt> for Gnutella pongs sent over MULTICAST.
	 */
	public static final DroppedSentMessageStatHandler MULTICAST_PING_REPLIES = 
	    new DroppedSentMessageStatHandler(DroppedSentMessageStat.MULTICAST_PING_REPLIES, 
								   DroppedSentMessageStatBytes.MULTICAST_PING_REPLIES,
								   DroppedLimeSentMessageStat.MULTICAST_PING_REPLIES, 
								   DroppedLimeSentMessageStatBytes.MULTICAST_PING_REPLIES);								   

	/**
	 * <tt>DroppedSentMessageStatHandler</tt> for Gnutella query requests sent 
	 * over UDP.
	 */
	public static final DroppedSentMessageStatHandler UDP_QUERY_REQUESTS = 
	    new DroppedSentMessageStatHandler(DroppedSentMessageStat.UDP_QUERY_REQUESTS, 
								   DroppedSentMessageStatBytes.UDP_QUERY_REQUESTS,
								   DroppedLimeSentMessageStat.UDP_QUERY_REQUESTS, 
								   DroppedLimeSentMessageStatBytes.UDP_QUERY_REQUESTS);

	/**
	 * <tt>DroppedSentMessageStatHandler</tt> for Gnutella query requests sent 
	 * over TCP.
	 */
	public static final DroppedSentMessageStatHandler TCP_QUERY_REQUESTS = 
	    new DroppedSentMessageStatHandler(DroppedSentMessageStat.TCP_QUERY_REQUESTS, 
								   DroppedSentMessageStatBytes.TCP_QUERY_REQUESTS,
								   DroppedLimeSentMessageStat.TCP_QUERY_REQUESTS, 
								   DroppedLimeSentMessageStatBytes.TCP_QUERY_REQUESTS);
								   
	/**
	 * <tt>DroppedSentMessageStatHandler</tt> for Gnutella query requests sent 
	 * over MULTICAST.
	 */
	public static final DroppedSentMessageStatHandler MULTICAST_QUERY_REQUESTS = 
	    new DroppedSentMessageStatHandler(DroppedSentMessageStat.MULTICAST_QUERY_REQUESTS, 
								   DroppedSentMessageStatBytes.MULTICAST_QUERY_REQUESTS,
								   DroppedLimeSentMessageStat.MULTICAST_QUERY_REQUESTS, 
								   DroppedLimeSentMessageStatBytes.MULTICAST_QUERY_REQUESTS);
								   

	/**
	 * <tt>DroppedSentMessageStatHandler</tt> for Gnutella query replies sent over 
	 * UDP.
	 */
	public static final DroppedSentMessageStatHandler UDP_QUERY_REPLIES = 
	    new DroppedSentMessageStatHandler(DroppedSentMessageStat.UDP_QUERY_REPLIES, 
								   DroppedSentMessageStatBytes.UDP_QUERY_REPLIES,
								   DroppedLimeSentMessageStat.UDP_QUERY_REPLIES, 
								   DroppedLimeSentMessageStatBytes.UDP_QUERY_REPLIES);

	/**
	 * <tt>DroppedSentMessageStatHandler</tt> for Gnutella query replies sent over 
	 * TCP.
	 */
	public static final DroppedSentMessageStatHandler TCP_QUERY_REPLIES = 
	    new DroppedSentMessageStatHandler(DroppedSentMessageStat.TCP_QUERY_REPLIES, 
								   DroppedSentMessageStatBytes.TCP_QUERY_REPLIES,
								   DroppedLimeSentMessageStat.TCP_QUERY_REPLIES, 
								   DroppedLimeSentMessageStatBytes.TCP_QUERY_REPLIES);
								   
	/**
	 * <tt>DroppedSentMessageStatHandler</tt> for Gnutella query replies sent over 
	 * MULTICAST.
	 */
	public static final DroppedSentMessageStatHandler MULTICAST_QUERY_REPLIES = 
	    new DroppedSentMessageStatHandler(DroppedSentMessageStat.MULTICAST_QUERY_REPLIES, 
								   DroppedSentMessageStatBytes.MULTICAST_QUERY_REPLIES,
								   DroppedLimeSentMessageStat.MULTICAST_QUERY_REPLIES, 
								   DroppedLimeSentMessageStatBytes.MULTICAST_QUERY_REPLIES);								   

	/**
	 * <tt>DroppedSentMessageStatHandler</tt> for Gnutella push requests sent over 
	 * UDP.
	 */
	public static final DroppedSentMessageStatHandler UDP_PUSH_REQUESTS = 
	    new DroppedSentMessageStatHandler(DroppedSentMessageStat.UDP_PUSH_REQUESTS, 
								   DroppedSentMessageStatBytes.UDP_PUSH_REQUESTS,
								   DroppedLimeSentMessageStat.UDP_PUSH_REQUESTS, 
								   DroppedLimeSentMessageStatBytes.UDP_PUSH_REQUESTS);

	/**
	 * <tt>DroppedSentMessageStatHandler</tt> for Gnutella push requests sent over 
	 * TCP.
	 */
	public static final DroppedSentMessageStatHandler TCP_PUSH_REQUESTS = 
	    new DroppedSentMessageStatHandler(DroppedSentMessageStat.TCP_PUSH_REQUESTS, 
								   DroppedSentMessageStatBytes.TCP_PUSH_REQUESTS,
								   DroppedLimeSentMessageStat.TCP_PUSH_REQUESTS, 
								   DroppedLimeSentMessageStatBytes.TCP_PUSH_REQUESTS);
								   
	/**
	 * <tt>DroppedSentMessageStatHandler</tt> for Gnutella push requests sent over 
	 * MULTICAST.
	 */
	public static final DroppedSentMessageStatHandler MULTICAST_PUSH_REQUESTS = 
	    new DroppedSentMessageStatHandler(DroppedSentMessageStat.MULTICAST_PUSH_REQUESTS, 
								   DroppedSentMessageStatBytes.MULTICAST_PUSH_REQUESTS,
								   DroppedLimeSentMessageStat.MULTICAST_PUSH_REQUESTS, 
								   DroppedLimeSentMessageStatBytes.MULTICAST_PUSH_REQUESTS);								   

	/**
	 * <tt>DroppedSentMessageStatHandler</tt> for Gnutella route table messages sent 
	 * over UDP.
	 */
	public static final DroppedSentMessageStatHandler UDP_ROUTE_TABLE_MESSAGES = 
	    new DroppedSentMessageStatHandler(DroppedSentMessageStat.UDP_ROUTE_TABLE_MESSAGES, 
								   DroppedSentMessageStatBytes.UDP_ROUTE_TABLE_MESSAGES,
								   DroppedLimeSentMessageStat.UDP_ROUTE_TABLE_MESSAGES, 
								   DroppedLimeSentMessageStatBytes.UDP_ROUTE_TABLE_MESSAGES);

	/**
	 * <tt>DroppedSentMessageStatHandler</tt> for Gnutella reset route table messages 
	 * sent over TCP.
	 */
	public static final DroppedSentMessageStatHandler TCP_RESET_ROUTE_TABLE_MESSAGES = 
	    new DroppedSentMessageStatHandler(DroppedSentMessageStat.TCP_RESET_ROUTE_TABLE_MESSAGES,
								   DroppedSentMessageStatBytes.TCP_RESET_ROUTE_TABLE_MESSAGES,
								   DroppedLimeSentMessageStat.TCP_RESET_ROUTE_TABLE_MESSAGES,
								   DroppedLimeSentMessageStatBytes.TCP_RESET_ROUTE_TABLE_MESSAGES);

	/**
	 * <tt>DroppedSentMessageStatHandler</tt> for Gnutella patch route table messages 
	 * sent over TCP.
	 */
	public static final DroppedSentMessageStatHandler TCP_PATCH_ROUTE_TABLE_MESSAGES = 
	    new DroppedSentMessageStatHandler(DroppedSentMessageStat.TCP_PATCH_ROUTE_TABLE_MESSAGES,
								   DroppedSentMessageStatBytes.TCP_PATCH_ROUTE_TABLE_MESSAGES,
								   DroppedLimeSentMessageStat.TCP_PATCH_ROUTE_TABLE_MESSAGES,
								   DroppedLimeSentMessageStatBytes.TCP_PATCH_ROUTE_TABLE_MESSAGES);
								   
	/**
	 * <tt>DroppedSentMessageStatHandler</tt> for Gnutella route table messages sent 
	 * over MULTICAST.
	 */
	public static final DroppedSentMessageStatHandler MULTICAST_ROUTE_TABLE_MESSAGES = 
	    new DroppedSentMessageStatHandler(DroppedSentMessageStat.MULTICAST_ROUTE_TABLE_MESSAGES,
								   DroppedSentMessageStatBytes.MULTICAST_ROUTE_TABLE_MESSAGES,
								   DroppedLimeSentMessageStat.MULTICAST_ROUTE_TABLE_MESSAGES,
								   DroppedLimeSentMessageStatBytes.MULTICAST_ROUTE_TABLE_MESSAGES);								   
}
