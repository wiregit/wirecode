package com.limegroup.gnutella.statistics;

import org.limewire.statistic.AdvancedKilobytesStatistic;
import org.limewire.statistic.Statistic;


/**
 * This class contains a type-safe enumeration of statistics for
 * individual Gnutella messages that have been sent from other 
 * nodes on the network.  Each statistic maintains its own history, 
 * all messages sent over a specific number of time intervals, 
 * etc.
 */
public class LimeSentMessageStatBytes extends AdvancedKilobytesStatistic {

	/**
	 * Constructs a new <tt>LimeSentMessageStatBytes</tt> instance.
	 */
	private LimeSentMessageStatBytes() {}


	/**
	 * Private class for keeping track of filtered messages, in bytes.
	 */
	private static class FilteredLimeSentMessageStatBytes
		extends LimeSentMessageStatBytes {
		public void incrementStat() {
			super.incrementStat();
			ALL_FILTERED_MESSAGES.incrementStat();
		}
	}

	/**
	 * Private class for the total number of bytes in sent 
	 * UDP messages.
	 */
	private static class UDPLimeSentMessageStatBytes 
		extends LimeSentMessageStatBytes {
		public void addData(int data) {
			super.addData(data);
			ALL_MESSAGES.addData(data);
			UDP_ALL_MESSAGES.addData(data);
		}
	}

	/**
	 * Private class for the total number of bytes in sent 
	 * TCP messages.
	 */
	private static class TCPLimeSentMessageStatBytes 
		extends LimeSentMessageStatBytes {
		public void addData(int data) {
			super.addData(data);
			ALL_MESSAGES.addData(data);
			TCP_ALL_MESSAGES.addData(data);
		}
	}
	
	/**
	 * Private class for the total number of bytes in sent 
	 * Multicast messages.
	 */
	private static class MulticastLimeSentMessageStatBytes 
		extends LimeSentMessageStatBytes {
		public void addData(int data) {
			super.addData(data);
			ALL_MESSAGES.addData(data);
			MULTICAST_ALL_MESSAGES.addData(data);
		}
	}	


	/**
	 * <tt>Statistic</tt> for all messages sent.
	 */
	public static final Statistic ALL_MESSAGES =
		new LimeSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for all UPD messages sent.
	 */
	public static final Statistic UDP_ALL_MESSAGES =
		new LimeSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for all TCP messages sent.
	 */
	public static final Statistic TCP_ALL_MESSAGES =
		new LimeSentMessageStatBytes();
		
	/**
	 * <tt>Statistic</tt> for all MULTICAST messages sent.
	 */
	public static final Statistic MULTICAST_ALL_MESSAGES =
		new LimeSentMessageStatBytes();
		

	/**
	 * <tt>Statistic</tt> for all filtered messages.
	 */
	public static final Statistic ALL_FILTERED_MESSAGES =
		new LimeSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella pings sent over UDP.
	 */
	public static final Statistic UDP_PING_REQUESTS = 
	    new UDPLimeSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella pings sent over TCP.
	 */
	public static final Statistic TCP_PING_REQUESTS = 
	    new TCPLimeSentMessageStatBytes();	    

	/**
	 * <tt>Statistic</tt> for Gnutella pings sent over MULTICAST.
	 */
	public static final Statistic MULTICAST_PING_REQUESTS = 
	    new MulticastLimeSentMessageStatBytes();	    

	/**
	 * <tt>Statistic</tt> for Gnutella pongs sent over UDP.
	 */
	public static final Statistic UDP_PING_REPLIES = 
	    new UDPLimeSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella pongs sent over TCP.
	 */
	public static final Statistic TCP_PING_REPLIES = 
	    new TCPLimeSentMessageStatBytes();
	    
	/**
	 * <tt>Statistic</tt> for Gnutella pongs sent over MULTICAST.
	 */
	public static final Statistic MULTICAST_PING_REPLIES = 
	    new MulticastLimeSentMessageStatBytes();
	    

	/**
	 * <tt>Statistic</tt> for Gnutella query requests sent over 
	 * UDP.
	 */
	public static final Statistic UDP_QUERY_REQUESTS = 
	    new UDPLimeSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella query requests sent over 
	 * TCP.
	 */
	public static final Statistic TCP_QUERY_REQUESTS = 
	    new TCPLimeSentMessageStatBytes();

    /**
     * <tt>Statistic</tt> for Gnutella TCP GIVE_STATS message
     */ 
	public static final Statistic TCP_GIVE_STATS = 
        new TCPLimeSentMessageStatBytes();

    /**
     * <tt>Statistic</tt> for Gnutella UDP GIVE_STATS message
     */ 
	public static final Statistic UDP_GIVE_STATS = 
        new UDPLimeSentMessageStatBytes();

	    
	/**
	 * <tt>Statistic</tt> for Gnutella query requests sent over 
	 * Multicast.
	 */
	public static final Statistic MULTICAST_QUERY_REQUESTS = 
	    new MulticastLimeSentMessageStatBytes();
	    

	/**
	 * <tt>Statistic</tt> for Gnutella query replies sent over 
	 * UDP.
	 */
	public static final Statistic UDP_QUERY_REPLIES = 
	    new UDPLimeSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella query replies sent over 
	 * TCP.
	 */
	public static final Statistic TCP_QUERY_REPLIES = 
	    new TCPLimeSentMessageStatBytes();
	    
	/**
	 * <tt>Statistic</tt> for Gnutella query replies sent over 
	 * Multicast.
	 */
	public static final Statistic MULTICAST_QUERY_REPLIES = 
	    new MulticastLimeSentMessageStatBytes();	    

	/**
	 * <tt>Statistic</tt> for Gnutella push requests sent over 
	 * UDP.
	 */
	public static final Statistic UDP_PUSH_REQUESTS = 
	    new UDPLimeSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella push requests sent over 
	 * TCP.
	 */
	public static final Statistic TCP_PUSH_REQUESTS = 
	    new TCPLimeSentMessageStatBytes();
	    
	/**
	 * <tt>Statistic</tt> for Gnutella push requests sent over 
	 * Multicast.
	 */
	public static final Statistic MULTICAST_PUSH_REQUESTS = 
	    new MulticastLimeSentMessageStatBytes();	    

	/**
	 * <tt>Statistic</tt> for Gnutella route table messages sent 
	 * over UDP.
	 */
	public static final Statistic UDP_ROUTE_TABLE_MESSAGES = 
	    new UDPLimeSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella reset route table messages sent 
	 * over TCP.
	 */
	public static final Statistic TCP_RESET_ROUTE_TABLE_MESSAGES = 
	    new TCPLimeSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella patch route table messages sent 
	 * over TCP.
	 */
	public static final Statistic TCP_PATCH_ROUTE_TABLE_MESSAGES = 
	    new TCPLimeSentMessageStatBytes();
	    
	/**
	 * <tt>Statistic</tt> for Gnutella route table messages sent 
	 * over Multicast.
	 */
	public static final Statistic MULTICAST_ROUTE_TABLE_MESSAGES = 
	    new MulticastLimeSentMessageStatBytes();	    

	/**
	 * <tt>Statistic</tt> for Gnutella filtered messages sent 
	 * over UDP.
	 */
	public static final Statistic UDP_FILTERED_MESSAGES = 
	    new FilteredLimeSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella filtered messages sent 
	 * over TCP.
	 */
	public static final Statistic TCP_FILTERED_MESSAGES = 
	    new FilteredLimeSentMessageStatBytes();
	    
    /**
	 * <tt>Statistic</tt> for Gnutella filtered messages sent 
	 * over Multicast.
	 */
	public static final Statistic MULTICAST_FILTERED_MESSAGES = 
	    new FilteredLimeSentMessageStatBytes();	    

	/**
	 * <tt>Statistic</tt> for Gnutella hops flow messages sent over 
	 * TCP.
	 */
	public static final Statistic TCP_HOPS_FLOW = 
	    new TCPLimeSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella meta-vendor messages sent over 
	 * TCP.
	 */
	public static final Statistic TCP_MESSAGES_SUPPORTED = 
	    new TCPLimeSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella TCP ConnectBack messages sent over 
	 * TCP.
	 */
	public static final Statistic TCP_TCP_CONNECTBACK = 
	    new TCPLimeSentMessageStatBytes();
	    
	/**
	 * <tt>Statistic</tt> for Gnutella UDP ConnectBack sent over 
	 * TCP.
	 */
	public static final Statistic TCP_UDP_CONNECTBACK = 
	    new TCPLimeSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella ReplyNumber VM sent over 
	 * UDP.
	 */
	public static final Statistic UDP_REPLY_NUMBER = 
	    new UDPLimeSentMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella LimeACK VM sent over 
	 * UDP.
	 */
	public static final Statistic UDP_LIME_ACK = 
	    new UDPLimeSentMessageStatBytes();

    /**
     * <tt>Statistic</tt> for Mojito DHT messages sent over UDP.
     */
    public static final Statistic UDP_DHT_MSG = 
        new UDPLimeSentMessageStatBytes();
    
}
