package com.limegroup.gnutella.statistics;

import org.limewire.statistic.AdvancedStatistic;
import org.limewire.statistic.BasicStatistic;
import org.limewire.statistic.Statistic;


/**
 * This class contains a type-safe enumeration of statistics for
 * individual Gnutella messages that have been sent from other 
 * nodes on the network.  Each statistic maintains its own history, 
 * all messages sent over a specific number of time intervals, 
 * etc.
 */
public class LimeSentMessageStat extends AdvancedStatistic {

	/**
	 * Constructs a new <tt>MessageStat</tt> instance.
	 */
	private LimeSentMessageStat() {}

	/**
	 * Private class for keeping track of filtered messages.
	 */
	private static class FilteredLimeSentMessageStat 
		extends LimeSentMessageStat {
		public void incrementStat() {
			super.incrementStat();
			ALL_FILTERED_MESSAGES.incrementStat();
		}
	}

	/**
	 * Private class for keeping track of the number of UDP messages.
	 */
	private static class UDPLimeSentMessageStat extends LimeSentMessageStat {
		public void incrementStat() {
			super.incrementStat();
			ALL_MESSAGES.incrementStat();
			UDP_ALL_MESSAGES.incrementStat();
		}
	}

	/**
	 * Private class for keeping track of the number of TCP messages.
	 */
	private static class TCPLimeSentMessageStat extends LimeSentMessageStat {
		public void incrementStat() {
			super.incrementStat();
			ALL_MESSAGES.incrementStat();
			TCP_ALL_MESSAGES.incrementStat();
		}
	}
	

	/**
	 * Private class for keeping track of the number of MULTICAST messages.
	 */
	private static class MulticastLimeSentMessageStat extends LimeSentMessageStat {
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
		new LimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for all UPD messages sent.
	 */
	public static final Statistic UDP_ALL_MESSAGES =
		new LimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for all TCP messages sent.
	 */
	public static final Statistic TCP_ALL_MESSAGES =
		new LimeSentMessageStat();
		
	/**
	 * <tt>Statistic</tt> for all MULTICAST messages sent.
	 */
	public static final Statistic MULTICAST_ALL_MESSAGES =
		new LimeSentMessageStat();		

	/**
	 * <tt>Statistic</tt> for all filtered messages.
	 */
	public static final Statistic ALL_FILTERED_MESSAGES =
		new LimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella pings sent over UDP.
	 */
	public static final Statistic UDP_PING_REQUESTS = 
	    new UDPLimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella pings sent over TCP.
	 */
	public static final Statistic TCP_PING_REQUESTS = 
	    new TCPLimeSentMessageStat();
	    
	/**
	 * <tt>Statistic</tt> for Gnutella pings sent over MULTICAST.
	 */
	public static final Statistic MULTICAST_PING_REQUESTS = 
	    new MulticastLimeSentMessageStat();	    

	/**
	 * <tt>Statistic</tt> for Gnutella pongs sent over UDP.
	 */
	public static final Statistic UDP_PING_REPLIES = 
	    new UDPLimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella pongs sent over TCP.
	 */
	public static final Statistic TCP_PING_REPLIES = 
	    new TCPLimeSentMessageStat();
	    
	/**
	 * <tt>Statistic</tt> for Gnutella pongs sent over MULTICAST.
	 */
	public static final Statistic MULTICAST_PING_REPLIES = 
	    new MulticastLimeSentMessageStat();	    

	/**
	 * <tt>Statistic</tt> for Gnutella query requests sent over 
	 * UDP.
	 */
	public static final Statistic UDP_QUERY_REQUESTS = 
	    new UDPLimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella query requests sent over 
	 * TCP.
	 */
	public static final Statistic TCP_QUERY_REQUESTS = 
	    new TCPLimeSentMessageStat();
	    

	/**
	 * <tt>Statistic</tt> for Gnutella query requests sent over 
	 * Multicast.
	 */
	public static final Statistic MULTICAST_QUERY_REQUESTS = 
	    new MulticastLimeSentMessageStat();	    

	/**
	 * <tt>Statistic</tt> for Gnutella query replies sent over 
	 * UDP.
	 */
	public static final Statistic UDP_QUERY_REPLIES = 
	    new UDPLimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella query replies sent over 
	 * TCP.
	 */
	public static final Statistic TCP_QUERY_REPLIES = 
	    new TCPLimeSentMessageStat();
	    
	/**
	 * <tt>Statistic</tt> for Gnutella query replies sent over 
	 * Multicast.
	 */
	public static final Statistic MULTICAST_QUERY_REPLIES = 
	    new MulticastLimeSentMessageStat();	    

	/**
	 * <tt>Statistic</tt> for Gnutella push requests sent over 
	 * UDP.
	 */
	public static final Statistic UDP_PUSH_REQUESTS = 
	    new UDPLimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella push requests sent over 
	 * TCP.
	 */
	public static final Statistic TCP_PUSH_REQUESTS = 
	    new TCPLimeSentMessageStat();
	    
	/**
	 * <tt>Statistic</tt> for Gnutella push requests sent over 
	 * Multicast.
	 */
	public static final Statistic MULTICAST_PUSH_REQUESTS = 
	    new MulticastLimeSentMessageStat();	    

	/**
	 * <tt>Statistic</tt> for Gnutella route table messages sent 
	 * over UDP.
	 */
	public static final Statistic UDP_ROUTE_TABLE_MESSAGES = 
	    new UDPLimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella reset route table messages sent 
	 * over TCP.
	 */
	public static final Statistic TCP_RESET_ROUTE_TABLE_MESSAGES = 
	    new TCPLimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella patch route table messages sent 
	 * over TCP.
	 */
	public static final Statistic TCP_PATCH_ROUTE_TABLE_MESSAGES = 
	    new TCPLimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella route table messages sent 
	 * over Multicast.
	 */
	public static final Statistic MULTICAST_ROUTE_TABLE_MESSAGES = 
	    new MulticastLimeSentMessageStat();	    

	/**
	 * <tt>Statistic</tt> for Gnutella filtered messages sent 
	 * over UDP.
	 */
	public static final Statistic UDP_FILTERED_MESSAGES = 
	    new FilteredLimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella filtered messages sent 
	 * over TCP.
	 */
	public static final Statistic TCP_FILTERED_MESSAGES = 
	    new FilteredLimeSentMessageStat();
	    
	/**
	 * <tt>Statistic</tt> for Gnutella filtered messages sent 
	 * over Multicast.
	 */
	public static final Statistic MULTICAST_FILTERED_MESSAGES = 
	    new FilteredLimeSentMessageStat();	    

	/**
	 * <tt>Statistic</tt> for Gnutella hops flow messages sent over 
	 * TCP.
	 */
	public static final Statistic TCP_HOPS_FLOW = 
	    new TCPLimeSentMessageStat();

    /**
     * <tt>Statistic</tt> for Gnutella TCP GIVE_STATS message
     */ 
	public static final Statistic TCP_GIVE_STATS = new TCPLimeSentMessageStat();

    /**
     * <tt>Statistic</tt> for Gnutella UDP GIVE_STATS message
     */ 
	public static final Statistic UDP_GIVE_STATS = new UDPLimeSentMessageStat();



	/**
	 * <tt>Statistic</tt> for Gnutella meta-vendor messages sent over 
	 * TCP.
	 */
	public static final Statistic TCP_MESSAGES_SUPPORTED = 
	    new TCPLimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella TCP ConnectBack messages sent over 
	 * TCP.
	 */
	public static final Statistic TCP_TCP_CONNECTBACK = 
	    new TCPLimeSentMessageStat();
	    
	/**
	 * <tt>Statistic</tt> for Gnutella UDP ConnectBack sent over 
	 * TCP.
	 */
	public static final Statistic TCP_UDP_CONNECTBACK = 
	    new TCPLimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella ReplyNumber VM sent over 
	 * UDP.
	 */
	public static final Statistic UDP_REPLY_NUMBER = 
	    new UDPLimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella LimeACK VM sent over 
	 * UDP.
	 */
	public static final Statistic UDP_LIME_ACK = 
	    new UDPLimeSentMessageStat();

    /**
     * <tt>Statistic</tt> for Mojito DHT messages sent over UDP.
     */
    public static final Statistic UDP_DHT_MSG = 
        new UDPLimeSentMessageStat();
    
    /**
     * <tt>Statistic</tt> for lime replies sent.
     */
    public static final Statistic LIME_REPLIES_SENT = 
        new BasicStatistic(){};

}
