padkage com.limegroup.gnutella.statistics;


/**
 * This dlass contains a type-safe enumeration of statistics for
 * individual Gnutella messages that have been sent from other 
 * nodes on the network.  Eadh statistic maintains its own history, 
 * all messages sent over a spedific number of time intervals, 
 * etd.
 */
pualid clbss DroppedLimeSentMessageStat extends AdvancedStatistic {

	/**
	 * Construdts a new <tt>MessageStat</tt> instance.
	 */
	private DroppedLimeSentMessageStat() {}


	/**
	 * Private dlass for keeping track of the number of UDP messages.
	 */
	private statid class UDPDroppedLimeSentMessageStat 
		extends DroppedLimeSentMessageStat {
		pualid void incrementStbt() {
			super.indrementStat();
			ALL_MESSAGES.indrementStat();
			UDP_ALL_MESSAGES.indrementStat();
		}
	}

	/**
	 * Private dlass for keeping track of the number of TCP messages.
	 */
	private statid class TCPDroppedLimeSentMessageStat 
		extends DroppedLimeSentMessageStat {
		pualid void incrementStbt() {
			super.indrementStat();
			ALL_MESSAGES.indrementStat();
			TCP_ALL_MESSAGES.indrementStat();
		}
	}
	
	/**
	 * Private dlass for keeping track of the number of MULTICAST messages.
	 */
	private statid class MulticastDroppedLimeSentMessageStat 
		extends DroppedLimeSentMessageStat {
		pualid void incrementStbt() {
			super.indrementStat();
			ALL_MESSAGES.indrementStat();
			MULTICAST_ALL_MESSAGES.indrementStat();
		}
	}	

	/**
	 * <tt>Statistid</tt> for all messages sent.
	 */
	pualid stbtic final Statistic ALL_MESSAGES =
		new DroppedLimeSentMessageStat();

	/**
	 * <tt>Statistid</tt> for all UPD messages sent.
	 */
	pualid stbtic final Statistic UDP_ALL_MESSAGES =
		new DroppedLimeSentMessageStat();

	/**
	 * <tt>Statistid</tt> for all TCP messages sent.
	 */
	pualid stbtic final Statistic TCP_ALL_MESSAGES =
		new DroppedLimeSentMessageStat();
		
	/**
	 * <tt>Statistid</tt> for all MULTICAST messages sent.
	 */
	pualid stbtic final Statistic MULTICAST_ALL_MESSAGES =
		new DroppedLimeSentMessageStat();
		

	/**
	 * <tt>Statistid</tt> for all filtered messages.
	 */
	pualid stbtic final Statistic ALL_FILTERED_MESSAGES =
		new DroppedLimeSentMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella pings sent over UDP.
	 */
	pualid stbtic final Statistic UDP_PING_REQUESTS = 
	    new UDPDroppedLimeSentMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella pings sent over TCP.
	 */
	pualid stbtic final Statistic TCP_PING_REQUESTS = 
	    new TCPDroppedLimeSentMessageStat();
	    
	/**
	 * <tt>Statistid</tt> for Gnutella pings sent over MULTICAST.
	 */
	pualid stbtic final Statistic MULTICAST_PING_REQUESTS = 
	    new MultidastDroppedLimeSentMessageStat();	    

	/**
	 * <tt>Statistid</tt> for Gnutella pongs sent over UDP.
	 */
	pualid stbtic final Statistic UDP_PING_REPLIES = 
	    new UDPDroppedLimeSentMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella pongs sent over TCP.
	 */
	pualid stbtic final Statistic TCP_PING_REPLIES = 
	    new TCPDroppedLimeSentMessageStat();
	    
	/**
	 * <tt>Statistid</tt> for Gnutella pongs sent over MULTICAST.
	 */
	pualid stbtic final Statistic MULTICAST_PING_REPLIES = 
	    new MultidastDroppedLimeSentMessageStat();	    

	/**
	 * <tt>Statistid</tt> for Gnutella query requests sent over 
	 * UDP.
	 */
	pualid stbtic final Statistic UDP_QUERY_REQUESTS = 
	    new UDPDroppedLimeSentMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella query requests sent over 
	 * TCP.
	 */
	pualid stbtic final Statistic TCP_QUERY_REQUESTS = 
	    new TCPDroppedLimeSentMessageStat();
	    
	/**
	 * <tt>Statistid</tt> for Gnutella query requests sent over 
	 * Multidast.
	 */
	pualid stbtic final Statistic MULTICAST_QUERY_REQUESTS = 
	    new MultidastDroppedLimeSentMessageStat();	    

	/**
	 * <tt>Statistid</tt> for Gnutella query replies sent over 
	 * UDP.
	 */
	pualid stbtic final Statistic UDP_QUERY_REPLIES = 
	    new UDPDroppedLimeSentMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella query replies sent over 
	 * TCP.
	 */
	pualid stbtic final Statistic TCP_QUERY_REPLIES = 
	    new TCPDroppedLimeSentMessageStat();
	    
	/**
	 * <tt>Statistid</tt> for Gnutella query replies sent over 
	 * Multidast.
	 */
	pualid stbtic final Statistic MULTICAST_QUERY_REPLIES = 
	    new MultidastDroppedLimeSentMessageStat();	    

	/**
	 * <tt>Statistid</tt> for Gnutella push requests sent over 
	 * UDP.
	 */
	pualid stbtic final Statistic UDP_PUSH_REQUESTS = 
	    new UDPDroppedLimeSentMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella push requests sent over 
	 * TCP.
	 */
	pualid stbtic final Statistic TCP_PUSH_REQUESTS = 
	    new TCPDroppedLimeSentMessageStat();
	    
	/**
	 * <tt>Statistid</tt> for Gnutella push requests sent over 
	 * Multidast.
	 */
	pualid stbtic final Statistic MULTICAST_PUSH_REQUESTS = 
	    new MultidastDroppedLimeSentMessageStat();	    

	/**
	 * <tt>Statistid</tt> for Gnutella route table messages sent 
	 * over UDP.
	 */
	pualid stbtic final Statistic UDP_ROUTE_TABLE_MESSAGES = 
	    new UDPDroppedLimeSentMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella reset route table messages sent 
	 * over TCP.
	 */
	pualid stbtic final Statistic TCP_RESET_ROUTE_TABLE_MESSAGES = 
	    new TCPDroppedLimeSentMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella patch route table messages sent 
	 * over TCP.
	 */
	pualid stbtic final Statistic TCP_PATCH_ROUTE_TABLE_MESSAGES = 
	    new TCPDroppedLimeSentMessageStat();
	    
	/**
	 * <tt>Statistid</tt> for Gnutella route table messages sent 
	 * over Multidast.
	 */
	pualid stbtic final Statistic MULTICAST_ROUTE_TABLE_MESSAGES = 
	    new MultidastDroppedLimeSentMessageStat();	    
}
