package com.limegroup.gnutella.statistics;

import org.limewire.statistic.Statistic;


/**
 * Wrapper class for keeping track of Gnutella message data.  For a given
 * Gnutella message, this class provides the simultaneous updating of both 
 * the number of messages received and the total bytes received.  All calls 
 * to add data for received Gnutella message statistics should go through 
 * this class to avoid losing any data.
 */
public final class ReceivedMessageStatHandler extends AbstractMessageStatHandler {

	/**
	 * Creates a new <tt>ReceivedMessageStatHandler</tt> instance.  
	 * Private constructor to ensure that no other classes can
	 * construct this class, following the type-safe enum pattern.
	 *
	 * @param numberStat the statistic that is simply incremented with
	 *  each new message
	 * @param byteStat the statistic for keeping track of the total bytes
	 */
	private ReceivedMessageStatHandler(Statistic numberStat, 
									   Statistic byteStat,
									   Statistic limeNumberStat,
									   Statistic limeByteStat,
									   String fileName) {
		super(numberStat, byteStat, limeNumberStat, limeByteStat,
			  BandwidthStat.GNUTELLA_MESSAGE_DOWNSTREAM_BANDWIDTH, fileName);
	}
	

	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella pings received over UDP.
	 */
	public static final ReceivedMessageStatHandler UDP_PING_REQUESTS = 
		new ReceivedMessageStatHandler(ReceivedMessageStat.UDP_PING_REQUESTS, 
									   ReceivedMessageStatBytes.UDP_PING_REQUESTS,
									   LimeReceivedMessageStat.UDP_PING_REQUESTS,
									   LimeReceivedMessageStatBytes.UDP_PING_REQUESTS,
									   "RECEIVED_UDP_PING_REQUESTS");
	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella pings received over TCP.
	 */
	public static final ReceivedMessageStatHandler TCP_PING_REQUESTS = 
		new ReceivedMessageStatHandler(ReceivedMessageStat.TCP_PING_REQUESTS,
									   ReceivedMessageStatBytes.TCP_PING_REQUESTS,
									   LimeReceivedMessageStat.TCP_PING_REQUESTS,
									   LimeReceivedMessageStatBytes.TCP_PING_REQUESTS,
									   "RECEIVED_TCP_PING_REQUESTS");

	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella pings received over Multicast.
	 */
	public static final ReceivedMessageStatHandler MULTICAST_PING_REQUESTS = 
		new ReceivedMessageStatHandler(ReceivedMessageStat.MULTICAST_PING_REQUESTS,
									   ReceivedMessageStatBytes.MULTICAST_PING_REQUESTS,
									   LimeReceivedMessageStat.MULTICAST_PING_REQUESTS,
									   LimeReceivedMessageStatBytes.MULTICAST_PING_REQUESTS,
									   "RECEIVED_MULTICAST_PING_REQUESTS");
									   
	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella pongs received over UDP.
	 */
	public static final ReceivedMessageStatHandler UDP_PING_REPLIES = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.UDP_PING_REPLIES, 
									   ReceivedMessageStatBytes.UDP_PING_REPLIES,
									   LimeReceivedMessageStat.UDP_PING_REPLIES, 
									   LimeReceivedMessageStatBytes.UDP_PING_REPLIES,
									   "RECEIVED_UDP_PING_REPLIES");
	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella pongs received over TCP.
	 */
	public static final ReceivedMessageStatHandler TCP_PING_REPLIES = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.TCP_PING_REPLIES, 
									   ReceivedMessageStatBytes.TCP_PING_REPLIES,
									   LimeReceivedMessageStat.TCP_PING_REPLIES, 
									   LimeReceivedMessageStatBytes.TCP_PING_REPLIES,
									   "RECEIVED_TCP_PING_REPLIES");
									   
	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella pongs received over Multicast.
	 */
	public static final ReceivedMessageStatHandler MULTICAST_PING_REPLIES = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.MULTICAST_PING_REPLIES, 
									   ReceivedMessageStatBytes.MULTICAST_PING_REPLIES,
									   LimeReceivedMessageStat.MULTICAST_PING_REPLIES, 
									   LimeReceivedMessageStatBytes.MULTICAST_PING_REPLIES,
									   "RECEIVED_MULTICAST_PING_REPLIES");
									   
	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella query requests received 
	 * over UDP.
	 */
	public static final ReceivedMessageStatHandler UDP_QUERY_REQUESTS = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.UDP_QUERY_REQUESTS, 
									   ReceivedMessageStatBytes.UDP_QUERY_REQUESTS,
									   LimeReceivedMessageStat.UDP_QUERY_REQUESTS, 
									   LimeReceivedMessageStatBytes.UDP_QUERY_REQUESTS,
									   "RECEIVED_UDP_QUERY_REQUESTS");
	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella query requests received 
	 * over TCP.
	 */
	public static final ReceivedMessageStatHandler TCP_QUERY_REQUESTS = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.TCP_QUERY_REQUESTS, 
									   ReceivedMessageStatBytes.TCP_QUERY_REQUESTS,
									   LimeReceivedMessageStat.TCP_QUERY_REQUESTS, 
									   LimeReceivedMessageStatBytes.TCP_QUERY_REQUESTS,
									   "RECEIVED_TCP_QUERY_REQUESTS");
									   
	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella query requests received 
	 * over Multicast.
	 */
	public static final ReceivedMessageStatHandler MULTICAST_QUERY_REQUESTS = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.MULTICAST_QUERY_REQUESTS, 
									   ReceivedMessageStatBytes.MULTICAST_QUERY_REQUESTS,
									   LimeReceivedMessageStat.MULTICAST_QUERY_REQUESTS, 
									   LimeReceivedMessageStatBytes.MULTICAST_QUERY_REQUESTS,
									   "RECEIVED_MULTICAST_QUERY_REQUESTS");
									   									   
	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella query replies received over 
	 * UDP.
	 */
	public static final ReceivedMessageStatHandler UDP_QUERY_REPLIES = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.UDP_QUERY_REPLIES, 
									   ReceivedMessageStatBytes.UDP_QUERY_REPLIES,
									   LimeReceivedMessageStat.UDP_QUERY_REPLIES, 
									   LimeReceivedMessageStatBytes.UDP_QUERY_REPLIES,
									   "RECEIVED_UDP_QUERY_REPLIES");
	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella query replies received over 
	 * TCP.
	 */
	public static final ReceivedMessageStatHandler TCP_QUERY_REPLIES = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.TCP_QUERY_REPLIES, 
									   ReceivedMessageStatBytes.TCP_QUERY_REPLIES,
									   LimeReceivedMessageStat.TCP_QUERY_REPLIES, 
									   LimeReceivedMessageStatBytes.TCP_QUERY_REPLIES,
									   "RECEIVED_TCP_QUERY_REPLIES");
									   
	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella query replies received over 
	 * Multicast.
	 */
	public static final ReceivedMessageStatHandler MULTICAST_QUERY_REPLIES = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.MULTICAST_QUERY_REPLIES, 
									   ReceivedMessageStatBytes.MULTICAST_QUERY_REPLIES,
									   LimeReceivedMessageStat.MULTICAST_QUERY_REPLIES, 
									   LimeReceivedMessageStatBytes.MULTICAST_QUERY_REPLIES,
									   "RECEIVED_MULTICAST_QUERY_REPLIES");
									   									   
	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella push requests received over 
	 * UDP.
	 */
	public static final ReceivedMessageStatHandler UDP_PUSH_REQUESTS = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.UDP_PUSH_REQUESTS, 
									   ReceivedMessageStatBytes.UDP_PUSH_REQUESTS,
									   LimeReceivedMessageStat.UDP_PUSH_REQUESTS, 
									   LimeReceivedMessageStatBytes.UDP_PUSH_REQUESTS,
									   "RECEIVED_UDP_PUSH_REQUESTS");
	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella push requests received over 
	 * TCP.
	 */
	public static final ReceivedMessageStatHandler TCP_PUSH_REQUESTS = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.TCP_PUSH_REQUESTS, 
									   ReceivedMessageStatBytes.TCP_PUSH_REQUESTS,
									   LimeReceivedMessageStat.TCP_PUSH_REQUESTS, 
									   LimeReceivedMessageStatBytes.TCP_PUSH_REQUESTS,
									   "RECEIVED_TCP_PUSH_REQUESTS");
									   
	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella push requests received over 
	 * Multicast.
	 */
	public static final ReceivedMessageStatHandler MULTICAST_PUSH_REQUESTS = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.MULTICAST_PUSH_REQUESTS, 
									   ReceivedMessageStatBytes.MULTICAST_PUSH_REQUESTS,
									   LimeReceivedMessageStat.MULTICAST_PUSH_REQUESTS, 
									   LimeReceivedMessageStatBytes.MULTICAST_PUSH_REQUESTS,
									   "RECEIVED_MULTICAST_PUSH_REQUESTS");
									   									   
	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella route table messages received 
	 * over UDP.
	 */
	public static final ReceivedMessageStatHandler UDP_ROUTE_TABLE_MESSAGES = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.UDP_ROUTE_TABLE_MESSAGES, 
									   ReceivedMessageStatBytes.UDP_ROUTE_TABLE_MESSAGES,
									   LimeReceivedMessageStat.UDP_ROUTE_TABLE_MESSAGES, 
									   LimeReceivedMessageStatBytes.UDP_ROUTE_TABLE_MESSAGES,
									   "RECEIVED_UDP_ROUTE_TABLE_MESSAGES");
	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella reset route table messages
     * received over TCP.
	 */
	public static final ReceivedMessageStatHandler TCP_RESET_ROUTE_TABLE_MESSAGES = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.TCP_RESET_ROUTE_TABLE_MESSAGES, 
									   ReceivedMessageStatBytes.TCP_RESET_ROUTE_TABLE_MESSAGES,
									   LimeReceivedMessageStat.TCP_RESET_ROUTE_TABLE_MESSAGES, 
									   LimeReceivedMessageStatBytes.TCP_RESET_ROUTE_TABLE_MESSAGES,
									   "RECEIVED_TCP_RESET_ROUTE_TABLE_MESSAGES");

	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella route table patch messages
     * received over TCP.
	 */
	public static final ReceivedMessageStatHandler TCP_PATCH_ROUTE_TABLE_MESSAGES = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.TCP_PATCH_ROUTE_TABLE_MESSAGES, 
									   ReceivedMessageStatBytes.TCP_PATCH_ROUTE_TABLE_MESSAGES,
									   LimeReceivedMessageStat.TCP_PATCH_ROUTE_TABLE_MESSAGES, 
									   LimeReceivedMessageStatBytes.TCP_PATCH_ROUTE_TABLE_MESSAGES,
									   "RECEIVED_TCP_PATCH_ROUTE_TABLE_MESSAGES");
									   
	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella route table messages received 
	 * over Multicast.
	 */
	public static final ReceivedMessageStatHandler MULTICAST_ROUTE_TABLE_MESSAGES = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.MULTICAST_ROUTE_TABLE_MESSAGES, 
									   ReceivedMessageStatBytes.MULTICAST_ROUTE_TABLE_MESSAGES,
									   LimeReceivedMessageStat.MULTICAST_ROUTE_TABLE_MESSAGES, 
									   LimeReceivedMessageStatBytes.MULTICAST_ROUTE_TABLE_MESSAGES,
									   "RECEIVED_MULTICAST_ROUTE_TABLE_MESSAGES");
									   									   
	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella filtered messages received 
	 * over UDP.
	 */
	public static final ReceivedMessageStatHandler UDP_FILTERED_MESSAGES = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.UDP_FILTERED_MESSAGES,
									   ReceivedMessageStatBytes.UDP_FILTERED_MESSAGES,
									   LimeReceivedMessageStat.UDP_FILTERED_MESSAGES,
									   LimeReceivedMessageStatBytes.UDP_FILTERED_MESSAGES,
									   "RECEIVED_UDP_FILTERED_MESSAGES");
	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella filtered messages received 
	 * over TCP.
	 */
	public static final ReceivedMessageStatHandler TCP_FILTERED_MESSAGES = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.TCP_FILTERED_MESSAGES,
									   ReceivedMessageStatBytes.TCP_FILTERED_MESSAGES,
									   LimeReceivedMessageStat.TCP_FILTERED_MESSAGES,
									   LimeReceivedMessageStatBytes.TCP_FILTERED_MESSAGES,
									   "RECEIVED_TCP_FILTERED_MESSAGES");
									   
	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella filtered messages received 
	 * over Multicast.
	 */
	public static final ReceivedMessageStatHandler MULTICAST_FILTERED_MESSAGES = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.MULTICAST_FILTERED_MESSAGES,
									   ReceivedMessageStatBytes.MULTICAST_FILTERED_MESSAGES,
									   LimeReceivedMessageStat.MULTICAST_FILTERED_MESSAGES,
									   LimeReceivedMessageStatBytes.MULTICAST_FILTERED_MESSAGES,
									   "RECEIVED_MULTICAST_FILTERED_MESSAGES");	
									   								   

	/**
	 * <tt>ReceivedMessageStatHandler</tt> for duplicate queries received 
	 * over UDP.
	 */
	public static final ReceivedMessageStatHandler UDP_DUPLICATE_QUERIES = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.UDP_DUPLICATE_QUERIES,
									   ReceivedMessageStatBytes.UDP_DUPLICATE_QUERIES,
									   LimeReceivedMessageStat.UDP_DUPLICATE_QUERIES,
									   LimeReceivedMessageStatBytes.UDP_DUPLICATE_QUERIES,
									   "RECEIVED_UDP_DUPLICATE_QUERIES");
	/**
	 * <tt>ReceivedMessageStatHandler</tt> for duplicate queries received 
	 * over TCP.
	 */
	public static final ReceivedMessageStatHandler TCP_DUPLICATE_QUERIES = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.TCP_DUPLICATE_QUERIES,
									   ReceivedMessageStatBytes.TCP_DUPLICATE_QUERIES,
									   LimeReceivedMessageStat.TCP_DUPLICATE_QUERIES,
									   LimeReceivedMessageStatBytes.TCP_DUPLICATE_QUERIES,
									   "RECEIVED_TCP_DUPLICATE_QUERIES");
									   
	/**
	 * <tt>ReceivedMessageStatHandler</tt> for duplicate queries received 
	 * over Multicast.
	 */
	public static final ReceivedMessageStatHandler MULTICAST_DUPLICATE_QUERIES = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.MULTICAST_DUPLICATE_QUERIES,
									   ReceivedMessageStatBytes.MULTICAST_DUPLICATE_QUERIES,
									   LimeReceivedMessageStat.MULTICAST_DUPLICATE_QUERIES,
									   LimeReceivedMessageStatBytes.MULTICAST_DUPLICATE_QUERIES,
									   "RECEIVED_MULTICAST_DUPLICATE_QUERIES");									   

	public static final ReceivedMessageStatHandler UDP_LIME_ACK = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.UDP_LIME_ACK,
                                       ReceivedMessageStatBytes.UDP_LIME_ACK,
                                       LimeReceivedMessageStat.UDP_LIME_ACK,
                                       LimeReceivedMessageStatBytes.UDP_LIME_ACK,
                                       "RECEIVED_UDP_LIME_ACK");

    
	public static final ReceivedMessageStatHandler TCP_HOPS_FLOW = 
        new ReceivedMessageStatHandler(ReceivedMessageStat.TCP_HOPS_FLOW,
                                       ReceivedMessageStatBytes.TCP_HOPS_FLOW,
                                       LimeReceivedMessageStat.TCP_HOPS_FLOW,
                                       LimeReceivedMessageStatBytes.TCP_HOPS_FLOW,
                                       "RECEIVED_UDP_HOPS_FLOW");
    

	public static final ReceivedMessageStatHandler TCP_TCP_CONNECTBACK = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.TCP_TCP_CONNECTBACK,
                                       ReceivedMessageStatBytes.TCP_TCP_CONNECTBACK,
                                       LimeReceivedMessageStat.TCP_TCP_CONNECTBACK,
                                       LimeReceivedMessageStatBytes.TCP_TCP_CONNECTBACK, 
                                       "RECEIVED_TCP_TCP_CONNECTBACK");


	public static final ReceivedMessageStatHandler TCP_UDP_CONNECTBACK = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.TCP_UDP_CONNECTBACK,
                                       ReceivedMessageStatBytes.TCP_UDP_CONNECTBACK,
                                       LimeReceivedMessageStat.TCP_UDP_CONNECTBACK,
                                       LimeReceivedMessageStatBytes.TCP_UDP_CONNECTBACK, 
                                       "RECEIVED_TCP_UDP_CONNECTBACK");


	public static final ReceivedMessageStatHandler TCP_MESSAGES_SUPPORTED = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.TCP_MESSAGES_SUPPORTED,
                                       ReceivedMessageStatBytes.TCP_MESSAGES_SUPPORTED,
                                       LimeReceivedMessageStat.TCP_MESSAGES_SUPPORTED,
                                       LimeReceivedMessageStatBytes.TCP_MESSAGES_SUPPORTED,
                                       "RECEIVED_TCP_MESSAGES_SUPPORTED");
    

	public static final ReceivedMessageStatHandler UDP_REPLY_NUMBER = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.UDP_REPLY_NUMBER,
                                       ReceivedMessageStatBytes.UDP_REPLY_NUMBER,
                                       LimeReceivedMessageStat.UDP_REPLY_NUMBER,
                                       LimeReceivedMessageStatBytes.UDP_REPLY_NUMBER,
                                       "RECEIVED_UDP_REPLY_NUMBER");


	public static final ReceivedMessageStatHandler UDP_DHT_MESSAGE = 
        new ReceivedMessageStatHandler(ReceivedMessageStat.UDP_DHT_MSG,
                                       ReceivedMessageStatBytes.UDP_DHT_MSG,
                                       LimeReceivedMessageStat.UDP_DHT_MSG,
                                       LimeReceivedMessageStatBytes.UDP_DHT_MSG,
                                       "RECEIVED_UDP_DHT_MSG");


}
