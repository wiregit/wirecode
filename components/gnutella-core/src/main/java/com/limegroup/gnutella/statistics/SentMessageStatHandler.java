package com.limegroup.gnutella.statistics;


/**
 * Wrapper class for keeping track of Gnutella message data.  For a given
 * Gnutella message, this class provides the simultaneous updating of both 
 * the number of messages sent and the total bytes sent.  All calls to add
 * data for sent Gnutella message statistics should go through this class 
 * to avoid losing any data.
 */
public class SentMessageStatHandler extends AbstractStatHandler {

	/**
	 * Creates a new <tt>SentMessageStatHandler</tt> instance.  
	 * Private constructor to ensure that no other classes can
	 * construct this class, following the type-safe enum pattern.
	 *
	 * @param numberStat the statistic that is simply incremented with
	 *  each new message
	 * @param byteStat the statistic for keeping track of the total bytes
	 */
	private SentMessageStatHandler(Statistic numberStat, 
								   Statistic byteStat,
								   Statistic limeNumberStat,
								   Statistic limeByteStat) {
		super(numberStat, byteStat, limeNumberStat, limeByteStat,
			  BandwidthStat.GNUTELLA_MESSAGE_UPSTREAM_BANDWIDTH);
		
	}
	

	/**
	 * <tt>SentMessageStatHandler</tt> for Gnutella pings sent over UDP.
	 */
	public static final SentMessageStatHandler UDP_PING_REQUESTS = 
		new SentMessageStatHandler(SentMessageStat.UDP_PING_REQUESTS, 
								   SentMessageStatBytes.UDP_PING_REQUESTS,
								   LimeSentMessageStat.UDP_PING_REQUESTS, 
								   LimeSentMessageStatBytes.UDP_PING_REQUESTS);

	/**
	 * <tt>SentMessageStatHandler</tt> for Gnutella pings sent over TCP.
	 */
	public static final SentMessageStatHandler TCP_PING_REQUESTS = 
		new SentMessageStatHandler(SentMessageStat.TCP_PING_REQUESTS,
								   SentMessageStatBytes.TCP_PING_REQUESTS,
								   LimeSentMessageStat.TCP_PING_REQUESTS,
								   LimeSentMessageStatBytes.TCP_PING_REQUESTS);
								   
	/**
	 * <tt>SentMessageStatHandler</tt> for Gnutella pings sent over MULTICAST.
	 */
	public static final SentMessageStatHandler MULTICAST_PING_REQUESTS = 
		new SentMessageStatHandler(SentMessageStat.MULTICAST_PING_REQUESTS,
								   SentMessageStatBytes.MULTICAST_PING_REQUESTS,
								   LimeSentMessageStat.MULTICAST_PING_REQUESTS,
								   LimeSentMessageStatBytes.MULTICAST_PING_REQUESTS);								   

	/**
	 * <tt>SentMessageStatHandler</tt> for Gnutella pongs sent over UDP.
	 */
	public static final SentMessageStatHandler UDP_PING_REPLIES = 
	    new SentMessageStatHandler(SentMessageStat.UDP_PING_REPLIES, 
								   SentMessageStatBytes.UDP_PING_REPLIES,
								   LimeSentMessageStat.UDP_PING_REPLIES, 
								   LimeSentMessageStatBytes.UDP_PING_REPLIES);

	/**
	 * <tt>SentMessageStatHandler</tt> for Gnutella pongs sent over TCP.
	 */
	public static final SentMessageStatHandler TCP_PING_REPLIES = 
	    new SentMessageStatHandler(SentMessageStat.TCP_PING_REPLIES, 
								   SentMessageStatBytes.TCP_PING_REPLIES,
								   LimeSentMessageStat.TCP_PING_REPLIES, 
								   LimeSentMessageStatBytes.TCP_PING_REPLIES);
								   

	/**
	 * <tt>SentMessageStatHandler</tt> for Gnutella pongs sent over MULTICAST.
	 */
	public static final SentMessageStatHandler MULTICAST_PING_REPLIES = 
	    new SentMessageStatHandler(SentMessageStat.MULTICAST_PING_REPLIES, 
								   SentMessageStatBytes.MULTICAST_PING_REPLIES,
								   LimeSentMessageStat.MULTICAST_PING_REPLIES, 
								   LimeSentMessageStatBytes.MULTICAST_PING_REPLIES);								   

	/**
	 * <tt>SentMessageStatHandler</tt> for Gnutella query requests sent 
	 * over UDP.
	 */
	public static final SentMessageStatHandler UDP_QUERY_REQUESTS = 
	    new SentMessageStatHandler(SentMessageStat.UDP_QUERY_REQUESTS, 
								   SentMessageStatBytes.UDP_QUERY_REQUESTS,
								   LimeSentMessageStat.UDP_QUERY_REQUESTS, 
								   LimeSentMessageStatBytes.UDP_QUERY_REQUESTS);

	/**
	 * <tt>SentMessageStatHandler</tt> for Gnutella query requests sent 
	 * over TCP.
	 */
	public static final SentMessageStatHandler TCP_QUERY_REQUESTS = 
	    new SentMessageStatHandler(SentMessageStat.TCP_QUERY_REQUESTS, 
								   SentMessageStatBytes.TCP_QUERY_REQUESTS,
								   LimeSentMessageStat.TCP_QUERY_REQUESTS, 
								   LimeSentMessageStatBytes.TCP_QUERY_REQUESTS);
								   
	/**
	 * <tt>SentMessageStatHandler</tt> for Gnutella query requests sent 
	 * over MULTICAST.
	 */
	public static final SentMessageStatHandler MULTICAST_QUERY_REQUESTS = 
	    new SentMessageStatHandler(SentMessageStat.MULTICAST_QUERY_REQUESTS, 
								   SentMessageStatBytes.MULTICAST_QUERY_REQUESTS,
								   LimeSentMessageStat.MULTICAST_QUERY_REQUESTS, 
								   LimeSentMessageStatBytes.MULTICAST_QUERY_REQUESTS);								   

	/**
	 * <tt>SentMessageStatHandler</tt> for Gnutella query replies sent over 
	 * UDP.
	 */
	public static final SentMessageStatHandler UDP_QUERY_REPLIES = 
	    new SentMessageStatHandler(SentMessageStat.UDP_QUERY_REPLIES, 
								   SentMessageStatBytes.UDP_QUERY_REPLIES,
								   LimeSentMessageStat.UDP_QUERY_REPLIES, 
								   LimeSentMessageStatBytes.UDP_QUERY_REPLIES);

	/**
	 * <tt>SentMessageStatHandler</tt> for Gnutella query replies sent over 
	 * TCP.
	 */
	public static final SentMessageStatHandler TCP_QUERY_REPLIES = 
	    new SentMessageStatHandler(SentMessageStat.TCP_QUERY_REPLIES, 
								   SentMessageStatBytes.TCP_QUERY_REPLIES,
								   LimeSentMessageStat.TCP_QUERY_REPLIES, 
								   LimeSentMessageStatBytes.TCP_QUERY_REPLIES);
								   
	/**
	 * <tt>SentMessageStatHandler</tt> for Gnutella query replies sent over 
	 * MULTICAST.
	 */
	public static final SentMessageStatHandler MULTICAST_QUERY_REPLIES = 
	    new SentMessageStatHandler(SentMessageStat.MULTICAST_QUERY_REPLIES, 
								   SentMessageStatBytes.MULTICAST_QUERY_REPLIES,
								   LimeSentMessageStat.MULTICAST_QUERY_REPLIES, 
								   LimeSentMessageStatBytes.TCP_QUERY_REPLIES);								   

	/**
	 * <tt>SentMessageStatHandler</tt> for Gnutella push requests sent over 
	 * UDP.
	 */
	public static final SentMessageStatHandler UDP_PUSH_REQUESTS = 
	    new SentMessageStatHandler(SentMessageStat.UDP_PUSH_REQUESTS, 
								   SentMessageStatBytes.UDP_PUSH_REQUESTS,
								   LimeSentMessageStat.UDP_PUSH_REQUESTS, 
								   LimeSentMessageStatBytes.UDP_PUSH_REQUESTS);

	/**
	 * <tt>SentMessageStatHandler</tt> for Gnutella push requests sent over 
	 * TCP.
	 */
	public static final SentMessageStatHandler TCP_PUSH_REQUESTS = 
	    new SentMessageStatHandler(SentMessageStat.TCP_PUSH_REQUESTS, 
								   SentMessageStatBytes.TCP_PUSH_REQUESTS,
								   LimeSentMessageStat.TCP_PUSH_REQUESTS, 
								   LimeSentMessageStatBytes.TCP_PUSH_REQUESTS);
								   
	/**
	 * <tt>SentMessageStatHandler</tt> for Gnutella push requests sent over 
	 * MULTICAST.
	 */
	public static final SentMessageStatHandler MULTICAST_PUSH_REQUESTS = 
	    new SentMessageStatHandler(SentMessageStat.MULTICAST_PUSH_REQUESTS, 
								   SentMessageStatBytes.MULTICAST_PUSH_REQUESTS,
								   LimeSentMessageStat.MULTICAST_PUSH_REQUESTS, 
								   LimeSentMessageStatBytes.MULTICAST_PUSH_REQUESTS);								   

	/**
	 * <tt>SentMessageStatHandler</tt> for Gnutella route table messages sent 
	 * over UDP.
	 */
	public static final SentMessageStatHandler UDP_ROUTE_TABLE_MESSAGES = 
	    new SentMessageStatHandler(SentMessageStat.UDP_ROUTE_TABLE_MESSAGES, 
								   SentMessageStatBytes.UDP_ROUTE_TABLE_MESSAGES,
								   LimeSentMessageStat.UDP_ROUTE_TABLE_MESSAGES, 
								   LimeSentMessageStatBytes.UDP_ROUTE_TABLE_MESSAGES);

	/**
	 * <tt>SentMessageStatHandler</tt> for Gnutella reset route table messages 
	 * sent ver TCP.
	 */
	public static final SentMessageStatHandler TCP_RESET_ROUTE_TABLE_MESSAGES = 
	    new SentMessageStatHandler(SentMessageStat.TCP_RESET_ROUTE_TABLE_MESSAGES,
								   SentMessageStatBytes.TCP_RESET_ROUTE_TABLE_MESSAGES,
								   LimeSentMessageStat.TCP_RESET_ROUTE_TABLE_MESSAGES,
								   LimeSentMessageStatBytes.TCP_RESET_ROUTE_TABLE_MESSAGES);

	/**
	 * <tt>SentMessageStatHandler</tt> for Gnutella patch route table messages 
	 * sent over TCP.
	 */
	public static final SentMessageStatHandler TCP_PATCH_ROUTE_TABLE_MESSAGES = 
	    new SentMessageStatHandler(SentMessageStat.TCP_PATCH_ROUTE_TABLE_MESSAGES,
								   SentMessageStatBytes.TCP_PATCH_ROUTE_TABLE_MESSAGES,
								   LimeSentMessageStat.TCP_PATCH_ROUTE_TABLE_MESSAGES,
								   LimeSentMessageStatBytes.TCP_PATCH_ROUTE_TABLE_MESSAGES);
								   
	/**
	 * <tt>SentMessageStatHandler</tt> for Gnutella route table messages sent 
	 * over MULTICAST.
	 */
	public static final SentMessageStatHandler MULTICAST_ROUTE_TABLE_MESSAGES = 
	    new SentMessageStatHandler(SentMessageStat.MULTICAST_ROUTE_TABLE_MESSAGES,
								   SentMessageStatBytes.MULTICAST_ROUTE_TABLE_MESSAGES,
								   LimeSentMessageStat.MULTICAST_ROUTE_TABLE_MESSAGES,
								   LimeSentMessageStatBytes.MULTICAST_ROUTE_TABLE_MESSAGES);								   

	/**
	 * <tt>SentMessageStatHandler</tt> for Gnutella filtered messages sent 
	 * over UDP.
	 */
	public static final SentMessageStatHandler UDP_FILTERED_MESSAGES = 
	    new SentMessageStatHandler(SentMessageStat.UDP_FILTERED_MESSAGES,
								   SentMessageStatBytes.UDP_FILTERED_MESSAGES,
								   LimeSentMessageStat.UDP_FILTERED_MESSAGES,
								   LimeSentMessageStatBytes.UDP_FILTERED_MESSAGES);

	/**
	 * <tt>SentMessageStatHandler</tt> for Gnutella filtered messages sent 
	 * over TCP.
	 */
	public static final SentMessageStatHandler TCP_FILTERED_MESSAGES = 
	    new SentMessageStatHandler(SentMessageStat.TCP_FILTERED_MESSAGES,
								   SentMessageStatBytes.TCP_FILTERED_MESSAGES,
								   LimeSentMessageStat.TCP_FILTERED_MESSAGES,
								   LimeSentMessageStatBytes.TCP_FILTERED_MESSAGES);
								   
	/**
	 * <tt>SentMessageStatHandler</tt> for Gnutella filtered messages sent 
	 * over MULTICAST.
	 */
	public static final SentMessageStatHandler MULTICAST_FILTERED_MESSAGES = 
	    new SentMessageStatHandler(SentMessageStat.MULTICAST_FILTERED_MESSAGES,
								   SentMessageStatBytes.MULTICAST_FILTERED_MESSAGES,
								   LimeSentMessageStat.MULTICAST_FILTERED_MESSAGES,
								   LimeSentMessageStatBytes.MULTICAST_FILTERED_MESSAGES);								   

	public static final SentMessageStatHandler UDP_LIME_ACK = 
	    new SentMessageStatHandler(SentMessageStat.UDP_LIME_ACK,
								   SentMessageStatBytes.UDP_LIME_ACK,
								   LimeSentMessageStat.UDP_LIME_ACK,
								   LimeSentMessageStatBytes.UDP_LIME_ACK);


	public static final SentMessageStatHandler TCP_HOPS_FLOW = 
	    new SentMessageStatHandler(SentMessageStat.TCP_HOPS_FLOW,
								   SentMessageStatBytes.TCP_HOPS_FLOW,
								   LimeSentMessageStat.TCP_HOPS_FLOW,
								   LimeSentMessageStatBytes.TCP_HOPS_FLOW);

    public static final SentMessageStatHandler TCP_GIVE_STATS = 
        new SentMessageStatHandler(SentMessageStat.TCP_GIVE_STATS,
                                   SentMessageStatBytes.TCP_GIVE_STATS,
                                   LimeSentMessageStat.TCP_GIVE_STATS,
                                   LimeSentMessageStatBytes.TCP_GIVE_STATS);

	public static final SentMessageStatHandler UDP_GIVE_STATS = 
	    new SentMessageStatHandler(SentMessageStat.UDP_GIVE_STATS,
								   SentMessageStatBytes.UDP_GIVE_STATS,
								   LimeSentMessageStat.UDP_GIVE_STATS,
								   LimeSentMessageStatBytes.UDP_GIVE_STATS);

    

	public static final SentMessageStatHandler TCP_TCP_CONNECTBACK = 
	    new SentMessageStatHandler(SentMessageStat.TCP_TCP_CONNECTBACK,
								   SentMessageStatBytes.TCP_TCP_CONNECTBACK,
								   LimeSentMessageStat.TCP_TCP_CONNECTBACK,
								   LimeSentMessageStatBytes.TCP_TCP_CONNECTBACK);


	public static final SentMessageStatHandler TCP_UDP_CONNECTBACK = 
	    new SentMessageStatHandler(SentMessageStat.TCP_UDP_CONNECTBACK,
								   SentMessageStatBytes.TCP_UDP_CONNECTBACK,
								   LimeSentMessageStat.TCP_UDP_CONNECTBACK,
								   LimeSentMessageStatBytes.TCP_UDP_CONNECTBACK);


	public static final SentMessageStatHandler TCP_MESSAGES_SUPPORTED = 
	    new SentMessageStatHandler(SentMessageStat.TCP_MESSAGES_SUPPORTED,
								   SentMessageStatBytes.TCP_MESSAGES_SUPPORTED,
								   LimeSentMessageStat.TCP_MESSAGES_SUPPORTED,
								   LimeSentMessageStatBytes.TCP_MESSAGES_SUPPORTED);


	public static final SentMessageStatHandler UDP_REPLY_NUMBER = 
	    new SentMessageStatHandler(SentMessageStat.UDP_REPLY_NUMBER,
								   SentMessageStatBytes.UDP_REPLY_NUMBER,
								   LimeSentMessageStat.UDP_REPLY_NUMBER,
								   LimeSentMessageStatBytes.UDP_REPLY_NUMBER);

    public static final SentMessageStatHandler UDP_DHT_MSG = 
        new SentMessageStatHandler(SentMessageStat.UDP_DHT_MSG,
                                   SentMessageStatBytes.UDP_DHT_MSG,
                                   LimeSentMessageStat.UDP_DHT_MSG,
                                   LimeSentMessageStatBytes.UDP_DHT_MSG);

}
