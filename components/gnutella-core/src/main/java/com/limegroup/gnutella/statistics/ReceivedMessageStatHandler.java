package com.limegroup.gnutella.statistics;

import com.limegroup.gnutella.*;

/**
 * Wrapper class for keeping track of Gnutella message data.  For a given
 * Gnutella message, this class provides the simultaneous updating of both 
 * the number of messages received and the total bytes received.  All calls to add
 * data for Gnutella message statistics should go through this class to 
 * avoid losing any data.
 */
public final class ReceivedMessageStatHandler {

	/**
	 * The <tt>Statistic</tt> that should be incremented for each new 
	 * message.
	 */
	private final Statistic NUMBER_STAT;

	/**
	 * The <tt>Statistic</tt> for the number of bytes for this message
	 * type.  For each new message added, the number of bytes are added
	 * to this <tt>Statistic</tt>.
	 */
	private final Statistic BYTE_STAT;


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
									   Statistic byteStat) {
		NUMBER_STAT = numberStat;
		BYTE_STAT = byteStat;
	} 

	/**
	 * Adds the specified <tt>Message</tt> to the stored data
	 *
	 * @param msg the received <tt>Message</tt> to add to the data
	 */
	public void addMessage(Message msg) {
		NUMBER_STAT.incrementStat();
		BYTE_STAT.addData(msg.getTotalLength());
		BandwidthStat.GNUTELLA_DOWNSTREAM_BANDWIDTH.addData(msg.getTotalLength());
	}
	

	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella pings received over UDP.
	 */
	public static final ReceivedMessageStatHandler UDP_PING_REQUESTS = 
		new ReceivedMessageStatHandler(ReceivedMessageStat.UDP_PING_REQUESTS, 
									   ReceivedMessageStat.UDP_PING_REQUESTS_BYTES);

	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella pings received over TCP.
	 */
	public static final ReceivedMessageStatHandler TCP_PING_REQUESTS = 
		new ReceivedMessageStatHandler(ReceivedMessageStat.TCP_PING_REQUESTS,
									   ReceivedMessageStat.TCP_PING_REQUESTS_BYTES);

	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella pongs received over UDP.
	 */
	public static final ReceivedMessageStatHandler UDP_PING_REPLIES = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.UDP_PING_REPLIES, 
									   ReceivedMessageStat.UDP_PING_REPLIES_BYTES);

	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella pongs received over TCP.
	 */
	public static final ReceivedMessageStatHandler TCP_PING_REPLIES = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.TCP_PING_REPLIES, 
									   ReceivedMessageStat.TCP_PING_REPLIES_BYTES);

	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella query requests received 
	 * over UDP.
	 */
	public static final ReceivedMessageStatHandler UDP_QUERY_REQUESTS = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.UDP_QUERY_REQUESTS, 
									   ReceivedMessageStat.UDP_QUERY_REQUESTS_BYTES);

	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella query requests received 
	 * over TCP.
	 */
	public static final ReceivedMessageStatHandler TCP_QUERY_REQUESTS = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.TCP_QUERY_REQUESTS, 
								   ReceivedMessageStat.TCP_QUERY_REQUESTS_BYTES);

	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella query replies received over 
	 * UDP.
	 */
	public static final ReceivedMessageStatHandler UDP_QUERY_REPLIES = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.UDP_QUERY_REPLIES, 
								   ReceivedMessageStat.UDP_QUERY_REPLIES_BYTES);

	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella query replies received over 
	 * TCP.
	 */
	public static final ReceivedMessageStatHandler TCP_QUERY_REPLIES = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.TCP_QUERY_REPLIES, 
									   ReceivedMessageStat.TCP_QUERY_REPLIES_BYTES);

	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella push requests received over 
	 * UDP.
	 */
	public static final ReceivedMessageStatHandler UDP_PUSH_REQUESTS = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.UDP_PUSH_REQUESTS, 
									   ReceivedMessageStat.UDP_PUSH_REQUESTS_BYTES);

	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella push requests received over 
	 * TCP.
	 */
	public static final ReceivedMessageStatHandler TCP_PUSH_REQUESTS = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.TCP_PUSH_REQUESTS, 
									   ReceivedMessageStat.TCP_PUSH_REQUESTS_BYTES);

	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella route table messages received 
	 * over UDP.
	 */
	public static final ReceivedMessageStatHandler UDP_ROUTE_TABLE_MESSAGES = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.UDP_ROUTE_TABLE_MESSAGES, 
									   ReceivedMessageStat.UDP_ROUTE_TABLE_MESSAGES_BYTES);

	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella route table messages received 
	 * over TCP.
	 */
	public static final ReceivedMessageStatHandler TCP_ROUTE_TABLE_MESSAGES = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.UDP_FILTERED_MESSAGES,
									   ReceivedMessageStat.UDP_FILTERED_MESSAGES_BYTES);

	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella filtered messages received 
	 * over UDP.
	 */
	public static final ReceivedMessageStatHandler UDP_FILTERED_MESSAGES = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.UDP_FILTERED_MESSAGES,
									   ReceivedMessageStat.UDP_FILTERED_MESSAGES_BYTES);

	/**
	 * <tt>ReceivedMessageStatHandler</tt> for Gnutella filtered messages received 
	 * over TCP.
	 */
	public static final ReceivedMessageStatHandler TCP_FILTERED_MESSAGES = 
	    new ReceivedMessageStatHandler(ReceivedMessageStat.TCP_FILTERED_MESSAGES,
									   ReceivedMessageStat.TCP_FILTERED_MESSAGES_BYTES);
}
