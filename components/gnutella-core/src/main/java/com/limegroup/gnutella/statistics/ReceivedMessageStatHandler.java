package com.limegroup.gnutella.statistics;

import com.limegroup.gnutella.*;

/**
 * Wrapper class for keeping track of Gnutella message data.  For a given
 * Gnutella message, this class provides the simultaneous updating of both 
 * the number of messages received and the total bytes received.  All calls 
 * to add data for received Gnutella message statistics should go through 
 * this class to avoid losing any data.
 */
public final class ReceivedMessageStatHandler extends AbstractStatHandler {

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
									   Statistic limeByteStat) {
		super(numberStat, byteStat, limeNumberStat, limeByteStat,
			  BandwidthStat.GNUTELLA_DOWNSTREAM_BANDWIDTH);
	}
	

	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella pings received over UDP.
	 */
	public static final ReceivedMessageStatHandler UDP_PING_REQUESTS = 
		new ReceivedMessageStatHandler(ReceivedMessageStat.UDP_PING_REQUESTS, 
									   ReceivedMessageStatBytes.UDP_PING_REQUESTS,
									   LimeReceivedMessageStat.UDP_PING_REQUESTS,
									   LimeReceivedMessageStatBytes.UDP_PING_REQUESTS);

	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella pings received over TCP.
	 */
	public static final ReceivedMessageStatHandler TCP_PING_REQUESTS = 
		new ReceivedMessageStatHandler(ReceivedMessageStat.TCP_PING_REQUESTS,
									   ReceivedMessageStatBytes.TCP_PING_REQUESTS,
									   LimeReceivedMessageStat.TCP_PING_REQUESTS,
									   LimeReceivedMessageStatBytes.TCP_PING_REQUESTS);

	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella pongs received over UDP.
	 */
	public static final ReceivedMessageStatHandler UDP_PING_REPLIES = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.UDP_PING_REPLIES, 
									   ReceivedMessageStatBytes.UDP_PING_REPLIES,
									   LimeReceivedMessageStat.UDP_PING_REPLIES, 
									   LimeReceivedMessageStatBytes.UDP_PING_REPLIES);

	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella pongs received over TCP.
	 */
	public static final ReceivedMessageStatHandler TCP_PING_REPLIES = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.TCP_PING_REPLIES, 
									   ReceivedMessageStatBytes.TCP_PING_REPLIES,
									   LimeReceivedMessageStat.TCP_PING_REPLIES, 
									   LimeReceivedMessageStatBytes.TCP_PING_REPLIES);

	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella query requests received 
	 * over UDP.
	 */
	public static final ReceivedMessageStatHandler UDP_QUERY_REQUESTS = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.UDP_QUERY_REQUESTS, 
									   ReceivedMessageStatBytes.UDP_QUERY_REQUESTS,
									   LimeReceivedMessageStat.UDP_QUERY_REQUESTS, 
									   LimeReceivedMessageStatBytes.UDP_QUERY_REQUESTS);

	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella query requests received 
	 * over TCP.
	 */
	public static final ReceivedMessageStatHandler TCP_QUERY_REQUESTS = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.TCP_QUERY_REQUESTS, 
									   ReceivedMessageStatBytes.TCP_QUERY_REQUESTS,
									   LimeReceivedMessageStat.TCP_QUERY_REQUESTS, 
									   LimeReceivedMessageStatBytes.TCP_QUERY_REQUESTS);

	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella query replies received over 
	 * UDP.
	 */
	public static final ReceivedMessageStatHandler UDP_QUERY_REPLIES = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.UDP_QUERY_REPLIES, 
									   ReceivedMessageStatBytes.UDP_QUERY_REPLIES,
									   LimeReceivedMessageStat.UDP_QUERY_REPLIES, 
									   LimeReceivedMessageStatBytes.UDP_QUERY_REPLIES);

	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella query replies received over 
	 * TCP.
	 */
	public static final ReceivedMessageStatHandler TCP_QUERY_REPLIES = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.TCP_QUERY_REPLIES, 
									   ReceivedMessageStatBytes.TCP_QUERY_REPLIES,
									   LimeReceivedMessageStat.TCP_QUERY_REPLIES, 
									   LimeReceivedMessageStatBytes.TCP_QUERY_REPLIES);

	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella push requests received over 
	 * UDP.
	 */
	public static final ReceivedMessageStatHandler UDP_PUSH_REQUESTS = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.UDP_PUSH_REQUESTS, 
									   ReceivedMessageStatBytes.UDP_PUSH_REQUESTS,
									   LimeReceivedMessageStat.UDP_PUSH_REQUESTS, 
									   LimeReceivedMessageStatBytes.UDP_PUSH_REQUESTS);

	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella push requests received over 
	 * TCP.
	 */
	public static final ReceivedMessageStatHandler TCP_PUSH_REQUESTS = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.TCP_PUSH_REQUESTS, 
									   ReceivedMessageStatBytes.TCP_PUSH_REQUESTS,
									   LimeReceivedMessageStat.TCP_PUSH_REQUESTS, 
									   LimeReceivedMessageStatBytes.TCP_PUSH_REQUESTS);

	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella route table messages received 
	 * over UDP.
	 */
	public static final ReceivedMessageStatHandler UDP_ROUTE_TABLE_MESSAGES = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.UDP_ROUTE_TABLE_MESSAGES, 
									   ReceivedMessageStatBytes.UDP_ROUTE_TABLE_MESSAGES,
									   LimeReceivedMessageStat.UDP_ROUTE_TABLE_MESSAGES, 
									   LimeReceivedMessageStatBytes.UDP_ROUTE_TABLE_MESSAGES);

	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella route table messages received 
	 * over TCP.
	 */
	public static final ReceivedMessageStatHandler TCP_ROUTE_TABLE_MESSAGES = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.UDP_FILTERED_MESSAGES,
									   ReceivedMessageStatBytes.UDP_FILTERED_MESSAGES,
									   LimeReceivedMessageStat.UDP_FILTERED_MESSAGES,
									   LimeReceivedMessageStatBytes.UDP_FILTERED_MESSAGES);

	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella filtered messages received 
	 * over UDP.
	 */
	public static final ReceivedMessageStatHandler UDP_FILTERED_MESSAGES = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.UDP_FILTERED_MESSAGES,
									   ReceivedMessageStatBytes.UDP_FILTERED_MESSAGES,
									   LimeReceivedMessageStat.UDP_FILTERED_MESSAGES,
									   LimeReceivedMessageStatBytes.UDP_FILTERED_MESSAGES);

	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella filtered messages received 
	 * over TCP.
	 */
	public static final ReceivedMessageStatHandler TCP_FILTERED_MESSAGES = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.TCP_FILTERED_MESSAGES,
									   ReceivedMessageStatBytes.TCP_FILTERED_MESSAGES,
									   LimeReceivedMessageStat.TCP_FILTERED_MESSAGES,
									   LimeReceivedMessageStatBytes.TCP_FILTERED_MESSAGES);


	/**
	 * <tt>ReceivedMessageStatHandler</tt> for duplicate queries received 
	 * over UDP.
	 */
	public static final ReceivedMessageStatHandler UDP_DUPLICATE_QUERIES = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.UDP_DUPLICATE_QUERIES,
									   ReceivedMessageStatBytes.UDP_DUPLICATE_QUERIES,
									   LimeReceivedMessageStat.UDP_DUPLICATE_QUERIES,
									   LimeReceivedMessageStatBytes.UDP_DUPLICATE_QUERIES);

	/**
	 * <tt>ReceivedMessageStatHandler</tt> for duplicate queries received 
	 * over TCP.
	 */
	public static final ReceivedMessageStatHandler TCP_DUPLICATE_QUERIES = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.TCP_DUPLICATE_QUERIES,
									   ReceivedMessageStatBytes.TCP_DUPLICATE_QUERIES,
									   LimeReceivedMessageStat.TCP_DUPLICATE_QUERIES,
									   LimeReceivedMessageStatBytes.TCP_DUPLICATE_QUERIES);

	
}
