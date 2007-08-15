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
public class DroppedLimeSentMessageStat extends AdvancedStatistic {

	/**
	 * Constructs a new <tt>MessageStat</tt> instance.
	 */
	private DroppedLimeSentMessageStat() {}


	/**
	 * Private class for keeping track of the number of UDP messages.
	 */
	private static class UDPDroppedLimeSentMessageStat 
		extends DroppedLimeSentMessageStat {
		public void incrementStat() {
			super.incrementStat();
			ALL_MESSAGES.incrementStat();
			UDP_ALL_MESSAGES.incrementStat();
		}
	}

	/**
	 * Private class for keeping track of the number of TCP messages.
	 */
	private static class TCPDroppedLimeSentMessageStat 
		extends DroppedLimeSentMessageStat {
		public void incrementStat() {
			super.incrementStat();
			ALL_MESSAGES.incrementStat();
			TCP_ALL_MESSAGES.incrementStat();
		}
	}
	
	/**
	 * Private class for keeping track of the number of MULTICAST messages.
	 */
	private static class MulticastDroppedLimeSentMessageStat 
		extends DroppedLimeSentMessageStat {
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
		new DroppedLimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for all UPD messages sent.
	 */
	public static final Statistic UDP_ALL_MESSAGES =
		new DroppedLimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for all TCP messages sent.
	 */
	public static final Statistic TCP_ALL_MESSAGES =
		new DroppedLimeSentMessageStat();
		
	/**
	 * <tt>Statistic</tt> for all MULTICAST messages sent.
	 */
	public static final Statistic MULTICAST_ALL_MESSAGES =
		new DroppedLimeSentMessageStat();
		

	/**
	 * <tt>Statistic</tt> for all filtered messages.
	 */
	public static final Statistic ALL_FILTERED_MESSAGES =
		new DroppedLimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella pings sent over UDP.
	 */
	public static final Statistic UDP_PING_REQUESTS = 
	    new UDPDroppedLimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella pings sent over TCP.
	 */
	public static final Statistic TCP_PING_REQUESTS = 
	    new TCPDroppedLimeSentMessageStat();
	    
	/**
	 * <tt>Statistic</tt> for Gnutella pings sent over MULTICAST.
	 */
	public static final Statistic MULTICAST_PING_REQUESTS = 
	    new MulticastDroppedLimeSentMessageStat();	    

	/**
	 * <tt>Statistic</tt> for Gnutella pongs sent over UDP.
	 */
	public static final Statistic UDP_PING_REPLIES = 
	    new UDPDroppedLimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella pongs sent over TCP.
	 */
	public static final Statistic TCP_PING_REPLIES = 
	    new TCPDroppedLimeSentMessageStat();
	    
	/**
	 * <tt>Statistic</tt> for Gnutella pongs sent over MULTICAST.
	 */
	public static final Statistic MULTICAST_PING_REPLIES = 
	    new MulticastDroppedLimeSentMessageStat();	    

	/**
	 * <tt>Statistic</tt> for Gnutella query requests sent over 
	 * UDP.
	 */
	public static final Statistic UDP_QUERY_REQUESTS = 
	    new UDPDroppedLimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella query requests sent over 
	 * TCP.
	 */
	public static final Statistic TCP_QUERY_REQUESTS = 
	    new TCPDroppedLimeSentMessageStat();
	    
	/**
	 * <tt>Statistic</tt> for Gnutella query requests sent over 
	 * Multicast.
	 */
	public static final Statistic MULTICAST_QUERY_REQUESTS = 
	    new MulticastDroppedLimeSentMessageStat();	    

	/**
	 * <tt>Statistic</tt> for Gnutella query replies sent over 
	 * UDP.
	 */
	public static final Statistic UDP_QUERY_REPLIES = 
	    new UDPDroppedLimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella query replies sent over 
	 * TCP.
	 */
	public static final Statistic TCP_QUERY_REPLIES = 
	    new TCPDroppedLimeSentMessageStat();
	    
	/**
	 * <tt>Statistic</tt> for Gnutella query replies sent over 
	 * Multicast.
	 */
	public static final Statistic MULTICAST_QUERY_REPLIES = 
	    new MulticastDroppedLimeSentMessageStat();	    

	/**
	 * <tt>Statistic</tt> for Gnutella push requests sent over 
	 * UDP.
	 */
	public static final Statistic UDP_PUSH_REQUESTS = 
	    new UDPDroppedLimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella push requests sent over 
	 * TCP.
	 */
	public static final Statistic TCP_PUSH_REQUESTS = 
	    new TCPDroppedLimeSentMessageStat();
	    
	/**
	 * <tt>Statistic</tt> for Gnutella push requests sent over 
	 * Multicast.
	 */
	public static final Statistic MULTICAST_PUSH_REQUESTS = 
	    new MulticastDroppedLimeSentMessageStat();	    

	/**
	 * <tt>Statistic</tt> for Gnutella route table messages sent 
	 * over UDP.
	 */
	public static final Statistic UDP_ROUTE_TABLE_MESSAGES = 
	    new UDPDroppedLimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella reset route table messages sent 
	 * over TCP.
	 */
	public static final Statistic TCP_RESET_ROUTE_TABLE_MESSAGES = 
	    new TCPDroppedLimeSentMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella patch route table messages sent 
	 * over TCP.
	 */
	public static final Statistic TCP_PATCH_ROUTE_TABLE_MESSAGES = 
	    new TCPDroppedLimeSentMessageStat();
	    
	/**
	 * <tt>Statistic</tt> for Gnutella route table messages sent 
	 * over Multicast.
	 */
	public static final Statistic MULTICAST_ROUTE_TABLE_MESSAGES = 
	    new MulticastDroppedLimeSentMessageStat();	    
}
