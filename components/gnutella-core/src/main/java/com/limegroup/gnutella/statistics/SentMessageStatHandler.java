package com.limegroup.gnutella.statistics;

import com.limegroup.gnutella.*;

/**
 * Wrapper class for keeping track of Gnutella message data.  For a given
 * Gnutella message, this class provides the simultaneous updating of both 
 * the number of messages sent and the total bytes sent.  All calls to add
 * data for Gnutella message statistics should go through this class to 
 * avoid losing any data.
 */
public final class SentMessageStatHandler {

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
	 * Creates a new <tt>SentMessageStatHandler</tt> instance.  
	 * Private constructor to ensure that no other classes can
	 * construct this class, following the type-safe enum pattern.
	 *
	 * @param numberStat the statistic that is simply incremented with
	 *  each new message
	 * @param byteStat the statistic for keeping track of the total bytes
	 */
	private SentMessageStatHandler(Statistic numberStat, 
									   Statistic byteStat) {
		NUMBER_STAT = numberStat;
		BYTE_STAT = byteStat;
	} 

	/**
	 * Adds the specified <tt>Message</tt> to the stored data
	 *
	 * @param msg the sent <tt>Message</tt> to add to the data
	 */
	public void addMessage(Message msg) {
		NUMBER_STAT.incrementStat();
		BYTE_STAT.addData(msg.getTotalLength());
		BandwidthStat.GNUTELLA_UPSTREAM_BANDWIDTH.addData(msg.getTotalLength());
	}
	

	/**
	 * <tt>SentMessageStatHandler</tt> for Gnutella pings sent over UDP.
	 */
	public static final SentMessageStatHandler UDP_PING_REQUESTS = 
		new SentMessageStatHandler(SentMessageStat.UDP_PING_REQUESTS, 
								   SentMessageStat.UDP_PING_REQUESTS_BYTES);

	/**
	 * <tt>SentMessageStatHandler</tt> for Gnutella pings sent over TCP.
	 */
	public static final SentMessageStatHandler TCP_PING_REQUESTS = 
		new SentMessageStatHandler(SentMessageStat.TCP_PING_REQUESTS,
								   SentMessageStat.TCP_PING_REQUESTS_BYTES);

	/**
	 * <tt>SentMessageStatHandler</tt> for Gnutella pongs sent over UDP.
	 */
	public static final SentMessageStatHandler UDP_PING_REPLIES = 
	    new SentMessageStatHandler(SentMessageStat.UDP_PING_REPLIES, 
								   SentMessageStat.UDP_PING_REPLIES_BYTES);

	/**
	 * <tt>SentMessageStatHandler</tt> for Gnutella pongs sent over TCP.
	 */
	public static final SentMessageStatHandler TCP_PING_REPLIES = 
	    new SentMessageStatHandler(SentMessageStat.TCP_PING_REPLIES, 
								   SentMessageStat.TCP_PING_REPLIES_BYTES);

	/**
	 * <tt>SentMessageStatHandler</tt> for Gnutella query requests sent 
	 * over UDP.
	 */
	public static final SentMessageStatHandler UDP_QUERY_REQUESTS = 
	    new SentMessageStatHandler(SentMessageStat.UDP_QUERY_REQUESTS, 
								   SentMessageStat.UDP_QUERY_REQUESTS_BYTES);

	/**
	 * <tt>SentMessageStatHandler</tt> for Gnutella query requests sent 
	 * over TCP.
	 */
	public static final SentMessageStatHandler TCP_QUERY_REQUESTS = 
	    new SentMessageStatHandler(SentMessageStat.TCP_QUERY_REQUESTS, 
								   SentMessageStat.TCP_QUERY_REQUESTS_BYTES);

	/**
	 * <tt>SentMessageStatHandler</tt> for Gnutella query replies sent over 
	 * UDP.
	 */
	public static final SentMessageStatHandler UDP_QUERY_REPLIES = 
	    new SentMessageStatHandler(SentMessageStat.UDP_QUERY_REPLIES, 
								   SentMessageStat.UDP_QUERY_REPLIES_BYTES);

	/**
	 * <tt>SentMessageStatHandler</tt> for Gnutella query replies sent over 
	 * TCP.
	 */
	public static final SentMessageStatHandler TCP_QUERY_REPLIES = 
	    new SentMessageStatHandler(SentMessageStat.TCP_QUERY_REPLIES, 
								   SentMessageStat.TCP_QUERY_REPLIES_BYTES);

	/**
	 * <tt>SentMessageStatHandler</tt> for Gnutella push requests sent over 
	 * UDP.
	 */
	public static final SentMessageStatHandler UDP_PUSH_REQUESTS = 
	    new SentMessageStatHandler(SentMessageStat.UDP_PUSH_REQUESTS, 
								   SentMessageStat.UDP_PUSH_REQUESTS_BYTES);

	/**
	 * <tt>SentMessageStatHandler</tt> for Gnutella push requests sent over 
	 * TCP.
	 */
	public static final SentMessageStatHandler TCP_PUSH_REQUESTS = 
	    new SentMessageStatHandler(SentMessageStat.TCP_PUSH_REQUESTS, 
								   SentMessageStat.TCP_PUSH_REQUESTS_BYTES);

	/**
	 * <tt>SentMessageStatHandler</tt> for Gnutella route table messages sent 
	 * over UDP.
	 */
	public static final SentMessageStatHandler UDP_ROUTE_TABLE_MESSAGES = 
	    new SentMessageStatHandler(SentMessageStat.UDP_ROUTE_TABLE_MESSAGES, 
								   SentMessageStat.UDP_ROUTE_TABLE_MESSAGES_BYTES);

	/**
	 * <tt>SentMessageStatHandler</tt> for Gnutella route table messages sent 
	 * over TCP.
	 */
	public static final SentMessageStatHandler TCP_ROUTE_TABLE_MESSAGES = 
	    new SentMessageStatHandler(SentMessageStat.UDP_FILTERED_MESSAGES,
								   SentMessageStat.UDP_FILTERED_MESSAGES_BYTES);

	/**
	 * <tt>SentMessageStatHandler</tt> for Gnutella filtered messages sent 
	 * over UDP.
	 */
	public static final SentMessageStatHandler UDP_FILTERED_MESSAGES = 
	    new SentMessageStatHandler(SentMessageStat.UDP_FILTERED_MESSAGES,
								   SentMessageStat.UDP_FILTERED_MESSAGES_BYTES);

	/**
	 * <tt>SentMessageStatHandler</tt> for Gnutella filtered messages sent 
	 * over TCP.
	 */
	public static final SentMessageStatHandler TCP_FILTERED_MESSAGES = 
	    new SentMessageStatHandler(SentMessageStat.TCP_FILTERED_MESSAGES,
								   SentMessageStat.TCP_FILTERED_MESSAGES_BYTES);
}
