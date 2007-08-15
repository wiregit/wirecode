package com.limegroup.gnutella.statistics;

import org.limewire.statistic.AdvancedStatistic;
import org.limewire.statistic.Statistic;


/**
 * This class contains a type-safe enumeration of statistics for
 * individual Gnutella messages that have been sent from other 
 * nodes on the network.  Each statistic maintains its own history, 
 * all messages sent over a specific number of time intervals, 
 * etc.
 */
public class DroppedSentMessageStat extends AdvancedStatistic {

	/**
	 * Constructs a new <tt>MessageStat</tt> instance with 
	 * 0 for all historical data fields.
	 */
	private DroppedSentMessageStat() {}

	/**
	 * Private class for keeping track of the number of UDP messages.
	 */
	private static class UDPDroppedSentMessageStat extends DroppedSentMessageStat {
		public void incrementStat() {
			super.incrementStat();
			ALL_MESSAGES.incrementStat();
			UDP_ALL_MESSAGES.incrementStat();
		}
	}

	/**
	 * Private class for keeping track of the number of TCP messages.
	 */
	private static class TCPDroppedSentMessageStat extends DroppedSentMessageStat {
		public void incrementStat() {
			super.incrementStat();
			ALL_MESSAGES.incrementStat();
			TCP_ALL_MESSAGES.incrementStat();
		}
	}
	
	/**
	 * Private class for keeping track of the number of MULTICAST messages.
	 */
	private static class MulticastDroppedSentMessageStat extends DroppedSentMessageStat {
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
		new DroppedSentMessageStat();

	/**
	 * <tt>Statistic</tt> for all UPD messages sent.
	 */
	public static final Statistic UDP_ALL_MESSAGES =
		new DroppedSentMessageStat();

	/**
	 * <tt>Statistic</tt> for all TCP messages sent.
	 */
	public static final Statistic TCP_ALL_MESSAGES =
		new DroppedSentMessageStat();
		
	/**
	 * <tt>Statistic</tt> for all MULTICAST messages sent.
	 */
	public static final Statistic MULTICAST_ALL_MESSAGES =
		new DroppedSentMessageStat();		

	/**
	 * <tt>Statistic</tt> for all filtered messages.
	 */
	public static final Statistic ALL_FILTERED_MESSAGES =
		new DroppedSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella pings sent over UDP.
	 */
	public static final Statistic UDP_PING_REQUESTS = 
	    new UDPDroppedSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella pings sent over TCP.
	 */
	public static final Statistic TCP_PING_REQUESTS = 
	    new TCPDroppedSentMessageStat();
	    
	/**
	 * <tt>Statistic</tt> for Gnutella pings sent over MULTICAST.
	 */
	public static final Statistic MULTICAST_PING_REQUESTS = 
	    new MulticastDroppedSentMessageStat();	    

	/**
	 * <tt>Statistic</tt> for Gnutella pongs sent over UDP.
	 */
	public static final Statistic UDP_PING_REPLIES = 
	    new UDPDroppedSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella pongs sent over TCP.
	 */
	public static final Statistic TCP_PING_REPLIES = 
	    new TCPDroppedSentMessageStat();
	    
	/**
	 * <tt>Statistic</tt> for Gnutella pongs sent over MULTICAST.
	 */
	public static final Statistic MULTICAST_PING_REPLIES = 
	    new MulticastDroppedSentMessageStat();	    

	/**
	 * <tt>Statistic</tt> for Gnutella query requests sent over 
	 * UDP.
	 */
	public static final Statistic UDP_QUERY_REQUESTS = 
	    new UDPDroppedSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella query requests sent over 
	 * TCP.
	 */
	public static final Statistic TCP_QUERY_REQUESTS = 
	    new TCPDroppedSentMessageStat();
	    
	/**
	 * <tt>Statistic</tt> for Gnutella query requests sent over 
	 * Multicast.
	 */
	public static final Statistic MULTICAST_QUERY_REQUESTS = 
	    new MulticastDroppedSentMessageStat();
	    	    

	/**
	 * <tt>Statistic</tt> for Gnutella query replies sent over 
	 * UDP.
	 */
	public static final Statistic UDP_QUERY_REPLIES = 
	    new UDPDroppedSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella query replies sent over 
	 * TCP.
	 */
	public static final Statistic TCP_QUERY_REPLIES = 
	    new TCPDroppedSentMessageStat();
	    
	/**
	 * <tt>Statistic</tt> for Gnutella query replies sent over 
	 * Multicast.
	 */
	public static final Statistic MULTICAST_QUERY_REPLIES = 
	    new MulticastDroppedSentMessageStat();	    

	/**
	 * <tt>Statistic</tt> for Gnutella push requests sent over 
	 * UDP.
	 */
	public static final Statistic UDP_PUSH_REQUESTS = 
	    new UDPDroppedSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella push requests sent over 
	 * TCP.
	 */
	public static final Statistic TCP_PUSH_REQUESTS = 
	    new TCPDroppedSentMessageStat();
	    
	/**
	 * <tt>Statistic</tt> for Gnutella push requests sent over 
	 * Multicast.
	 */
	public static final Statistic MULTICAST_PUSH_REQUESTS = 
	    new MulticastDroppedSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella route table messages sent 
	 * over UDP.
	 */
	public static final Statistic UDP_ROUTE_TABLE_MESSAGES = 
	    new UDPDroppedSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella reset route table messages sent 
	 * over TCP.
	 */
	public static final Statistic TCP_RESET_ROUTE_TABLE_MESSAGES = 
	    new TCPDroppedSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella patch route table messages sent 
	 * over TCP.
	 */
	public static final Statistic TCP_PATCH_ROUTE_TABLE_MESSAGES = 
	    new TCPDroppedSentMessageStat();
	    
	/**
	 * <tt>Statistic</tt> for Gnutella route table messages sent 
	 * over Multicast.
	 */
	public static final Statistic MULTICAST_ROUTE_TABLE_MESSAGES = 
	    new MulticastDroppedSentMessageStat();	    
}
