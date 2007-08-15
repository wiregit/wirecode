package com.limegroup.gnutella.statistics;

import org.limewire.statistic.AdvancedStatistic;
import org.limewire.statistic.Statistic;


/**
 * This class contains a type-safe enumeration of statistics for
 * individual Gnutella messages that have been sent to other 
 * nodes on the network.  Each statistic maintains its own history, 
 * all messages sent over a specific number of time intervals, 
 * etc.
 */
public class SentMessageStat extends AdvancedStatistic {

	/**
	 * Constructs a new <tt>MessageStat</tt> instance with 
	 * 0 for all historical data fields.
	 */
	private SentMessageStat() {}

	/**
	 * Private class for keeping track of filtered messages.
	 */
	private static class FilteredSentMessageStat 
		extends SentMessageStat {
		public void incrementStat() {
			super.incrementStat();
			ALL_FILTERED_MESSAGES.incrementStat();
		}
	}

	/**
	 * Private class for keeping track of the number of UDP messages.
	 */
	private static class UDPSentMessageStat extends SentMessageStat {
		public void incrementStat() {
			super.incrementStat();
			ALL_MESSAGES.incrementStat();
			UDP_ALL_MESSAGES.incrementStat();
		}
	}

	/**
	 * Private class for keeping track of the number of TCP messages.
	 */
	private static class TCPSentMessageStat extends SentMessageStat {
		public void incrementStat() {
			super.incrementStat();
			ALL_MESSAGES.incrementStat();
			TCP_ALL_MESSAGES.incrementStat();
		}
	}
	
	/**
	 * Private class for keeping track of the number of MULTICAST messages.
	 */
	private static class MulticastSentMessageStat extends SentMessageStat {
		public void incrementStat() {
			super.incrementStat();
			ALL_MESSAGES.incrementStat();
			MULTICAST_ALL_MESSAGES.incrementStat();
		}
	}	

	/**
	 * <tt>Statistic</tt> for all messages sent.
	 */
	public static final Statistic ALL_MESSAGES =
		new SentMessageStat();

	/**
	 * <tt>Statistic</tt> for all UPD messages sent.
	 */
	public static final Statistic UDP_ALL_MESSAGES =
		new SentMessageStat();

	/**
	 * <tt>Statistic</tt> for all TCP messages sent.
	 */
	public static final Statistic TCP_ALL_MESSAGES =
		new SentMessageStat();

	/**
	 * <tt>Statistic</tt> for all MULTICAST messages sent.
	 */
	public static final Statistic MULTICAST_ALL_MESSAGES =
		new SentMessageStat();

	/**
	 * <tt>Statistic</tt> for all filtered messages.
	 */
	public static final Statistic ALL_FILTERED_MESSAGES =
		new SentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella pings sent over UDP.
	 */
	public static final Statistic UDP_PING_REQUESTS = 
	    new UDPSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella pings sent over TCP.
	 */
	public static final Statistic TCP_PING_REQUESTS = 
	    new TCPSentMessageStat();
	    
	/**
	 * <tt>Statistic</tt> for Gnutella pings sent over MULTICAST.
	 */
	public static final Statistic MULTICAST_PING_REQUESTS = 
	    new MulticastSentMessageStat();	    

	/**
	 * <tt>Statistic</tt> for Gnutella pongs sent over UDP.
	 */
	public static final Statistic UDP_PING_REPLIES = 
	    new UDPSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella pongs sent over TCP.
	 */
	public static final Statistic TCP_PING_REPLIES = 
	    new TCPSentMessageStat();
	
    /**
     * <tt>Statistic</tt> for "Give Stat" messages sent over TCP
     */ 
    public static final Statistic TCP_GIVE_STATS = new TCPSentMessageStat();

    /**
     * <tt>Statistic</tt> for "Give Stat" messages sent over UDP
     */ 
    public static final Statistic UDP_GIVE_STATS = new UDPSentMessageStat();
    
	/**
	 * <tt>Statistic</tt> for Gnutella pongs sent over MULTICAST.
	 */
	public static final Statistic MULTICAST_PING_REPLIES = 
	    new MulticastSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella query requests sent over 
	 * UDP.
	 */
	public static final Statistic UDP_QUERY_REQUESTS = 
	    new UDPSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella query requests sent over 
	 * TCP.
	 */
	public static final Statistic TCP_QUERY_REQUESTS = 
	    new TCPSentMessageStat();
	    
	/**
	 * <tt>Statistic</tt> for Gnutella query requests sent over 
	 * Multicast.
	 */
	public static final Statistic MULTICAST_QUERY_REQUESTS = 
	    new MulticastSentMessageStat();	    

	/**
	 * <tt>Statistic</tt> for Gnutella query replies sent over 
	 * UDP.
	 */
	public static final Statistic UDP_QUERY_REPLIES = 
	    new UDPSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella query replies sent over 
	 * TCP.
	 */
	public static final Statistic TCP_QUERY_REPLIES = 
	    new TCPSentMessageStat();
	    
	/**
	 * <tt>Statistic</tt> for Gnutella query replies sent over 
	 * Multicast.
	 */
	public static final Statistic MULTICAST_QUERY_REPLIES = 
	    new MulticastSentMessageStat();	    

	/**
	 * <tt>Statistic</tt> for Gnutella push requests sent over 
	 * UDP.
	 */
	public static final Statistic UDP_PUSH_REQUESTS = 
	    new UDPSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella push requests sent over 
	 * TCP.
	 */
	public static final Statistic TCP_PUSH_REQUESTS = 
	    new TCPSentMessageStat();
	    
    /**
	 * <tt>Statistic</tt> for Gnutella push requests sent over 
	 * Multicast.
	 */
	public static final Statistic MULTICAST_PUSH_REQUESTS = 
	    new MulticastSentMessageStat();	    

	/**
	 * <tt>Statistic</tt> for Gnutella route table messages sent 
	 * over UDP.
	 */
	public static final Statistic UDP_ROUTE_TABLE_MESSAGES = 
	    new UDPSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella reset route table messages 
	 * sent over TCP.
	 */
	public static final Statistic TCP_RESET_ROUTE_TABLE_MESSAGES = 
	    new TCPSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella patch route table messages 
	 * sent over TCP.
	 */
	public static final Statistic TCP_PATCH_ROUTE_TABLE_MESSAGES = 
	    new TCPSentMessageStat();
	    
	/**
	 * <tt>Statistic</tt> for Gnutella route table messages sent 
	 * over Multicast.
	 */
	public static final Statistic MULTICAST_ROUTE_TABLE_MESSAGES = 
	    new MulticastSentMessageStat();
	    

	/**
	 * <tt>Statistic</tt> for Gnutella filtered messages sent 
	 * over UDP.
	 */
	public static final Statistic UDP_FILTERED_MESSAGES = 
	    new FilteredSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella filtered messages sent 
	 * over TCP.
	 */
	public static final Statistic TCP_FILTERED_MESSAGES = 
	    new FilteredSentMessageStat();
	    
	/**
	 * <tt>Statistic</tt> for Gnutella filtered messages sent 
	 * over Multicast.
	 */
	public static final Statistic MULTICAST_FILTERED_MESSAGES = 
	    new FilteredSentMessageStat();	    

	/**
	 * <tt>Statistic</tt> for Gnutella hops flow messages sent over 
	 * TCP.
	 */
	public static final Statistic TCP_HOPS_FLOW = 
	    new TCPSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella meta-vendor messages sent over 
	 * TCP.
	 */
	public static final Statistic TCP_MESSAGES_SUPPORTED = 
	    new TCPSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella TCP ConnectBack messages sent over 
	 * TCP.
	 */
	public static final Statistic TCP_TCP_CONNECTBACK = 
	    new TCPSentMessageStat();
	    
	/**
	 * <tt>Statistic</tt> for Gnutella UDP ConnectBack sent over 
	 * TCP.
	 */
	public static final Statistic TCP_UDP_CONNECTBACK = 
	    new TCPSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella ReplyNumber VM sent over 
	 * UDP.
	 */
	public static final Statistic UDP_REPLY_NUMBER = 
	    new UDPSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella LimeACK VM sent over 
	 * UDP.
	 */
	public static final Statistic UDP_LIME_ACK = 
	    new UDPSentMessageStat();
    
    /**
     * <tt>Statistic</tt> for Mojito DHT messages sent over UDP.
     */
    public static final Statistic UDP_DHT_MSG = 
        new UDPSentMessageStat();
}
