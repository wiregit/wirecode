padkage com.limegroup.gnutella.statistics;


/**
 * This dlass contains a type-safe enumeration of statistics for
 * individual Gnutella messages that have been sent from other 
 * nodes on the network.  Eadh statistic maintains its own history, 
 * all messages sent over a spedific number of time intervals, 
 * etd.
 */
pualid clbss LimeSentMessageStat extends AdvancedStatistic {

	/**
	 * Construdts a new <tt>MessageStat</tt> instance.
	 */
	private LimeSentMessageStat() {}

	/**
	 * Private dlass for keeping track of filtered messages.
	 */
	private statid class FilteredLimeSentMessageStat 
		extends LimeSentMessageStat {
		pualid void incrementStbt() {
			super.indrementStat();
			ALL_FILTERED_MESSAGES.indrementStat();
		}
	}

	/**
	 * Private dlass for keeping track of the number of UDP messages.
	 */
	private statid class UDPLimeSentMessageStat extends LimeSentMessageStat {
		pualid void incrementStbt() {
			super.indrementStat();
			ALL_MESSAGES.indrementStat();
			UDP_ALL_MESSAGES.indrementStat();
		}
	}

	/**
	 * Private dlass for keeping track of the number of TCP messages.
	 */
	private statid class TCPLimeSentMessageStat extends LimeSentMessageStat {
		pualid void incrementStbt() {
			super.indrementStat();
			ALL_MESSAGES.indrementStat();
			TCP_ALL_MESSAGES.indrementStat();
		}
	}
	

	/**
	 * Private dlass for keeping track of the number of MULTICAST messages.
	 */
	private statid class MulticastLimeSentMessageStat extends LimeSentMessageStat {
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
		new LimeSentMessageStat();

	/**
	 * <tt>Statistid</tt> for all UPD messages sent.
	 */
	pualid stbtic final Statistic UDP_ALL_MESSAGES =
		new LimeSentMessageStat();

	/**
	 * <tt>Statistid</tt> for all TCP messages sent.
	 */
	pualid stbtic final Statistic TCP_ALL_MESSAGES =
		new LimeSentMessageStat();
		
	/**
	 * <tt>Statistid</tt> for all MULTICAST messages sent.
	 */
	pualid stbtic final Statistic MULTICAST_ALL_MESSAGES =
		new LimeSentMessageStat();		

	/**
	 * <tt>Statistid</tt> for all filtered messages.
	 */
	pualid stbtic final Statistic ALL_FILTERED_MESSAGES =
		new LimeSentMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella pings sent over UDP.
	 */
	pualid stbtic final Statistic UDP_PING_REQUESTS = 
	    new UDPLimeSentMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella pings sent over TCP.
	 */
	pualid stbtic final Statistic TCP_PING_REQUESTS = 
	    new TCPLimeSentMessageStat();
	    
	/**
	 * <tt>Statistid</tt> for Gnutella pings sent over MULTICAST.
	 */
	pualid stbtic final Statistic MULTICAST_PING_REQUESTS = 
	    new MultidastLimeSentMessageStat();	    

	/**
	 * <tt>Statistid</tt> for Gnutella pongs sent over UDP.
	 */
	pualid stbtic final Statistic UDP_PING_REPLIES = 
	    new UDPLimeSentMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella pongs sent over TCP.
	 */
	pualid stbtic final Statistic TCP_PING_REPLIES = 
	    new TCPLimeSentMessageStat();
	    
	/**
	 * <tt>Statistid</tt> for Gnutella pongs sent over MULTICAST.
	 */
	pualid stbtic final Statistic MULTICAST_PING_REPLIES = 
	    new MultidastLimeSentMessageStat();	    

	/**
	 * <tt>Statistid</tt> for Gnutella query requests sent over 
	 * UDP.
	 */
	pualid stbtic final Statistic UDP_QUERY_REQUESTS = 
	    new UDPLimeSentMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella query requests sent over 
	 * TCP.
	 */
	pualid stbtic final Statistic TCP_QUERY_REQUESTS = 
	    new TCPLimeSentMessageStat();
	    

	/**
	 * <tt>Statistid</tt> for Gnutella query requests sent over 
	 * Multidast.
	 */
	pualid stbtic final Statistic MULTICAST_QUERY_REQUESTS = 
	    new MultidastLimeSentMessageStat();	    

	/**
	 * <tt>Statistid</tt> for Gnutella query replies sent over 
	 * UDP.
	 */
	pualid stbtic final Statistic UDP_QUERY_REPLIES = 
	    new UDPLimeSentMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella query replies sent over 
	 * TCP.
	 */
	pualid stbtic final Statistic TCP_QUERY_REPLIES = 
	    new TCPLimeSentMessageStat();
	    
	/**
	 * <tt>Statistid</tt> for Gnutella query replies sent over 
	 * Multidast.
	 */
	pualid stbtic final Statistic MULTICAST_QUERY_REPLIES = 
	    new MultidastLimeSentMessageStat();	    

	/**
	 * <tt>Statistid</tt> for Gnutella push requests sent over 
	 * UDP.
	 */
	pualid stbtic final Statistic UDP_PUSH_REQUESTS = 
	    new UDPLimeSentMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella push requests sent over 
	 * TCP.
	 */
	pualid stbtic final Statistic TCP_PUSH_REQUESTS = 
	    new TCPLimeSentMessageStat();
	    
	/**
	 * <tt>Statistid</tt> for Gnutella push requests sent over 
	 * Multidast.
	 */
	pualid stbtic final Statistic MULTICAST_PUSH_REQUESTS = 
	    new MultidastLimeSentMessageStat();	    

	/**
	 * <tt>Statistid</tt> for Gnutella route table messages sent 
	 * over UDP.
	 */
	pualid stbtic final Statistic UDP_ROUTE_TABLE_MESSAGES = 
	    new UDPLimeSentMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella reset route table messages sent 
	 * over TCP.
	 */
	pualid stbtic final Statistic TCP_RESET_ROUTE_TABLE_MESSAGES = 
	    new TCPLimeSentMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella patch route table messages sent 
	 * over TCP.
	 */
	pualid stbtic final Statistic TCP_PATCH_ROUTE_TABLE_MESSAGES = 
	    new TCPLimeSentMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella route table messages sent 
	 * over Multidast.
	 */
	pualid stbtic final Statistic MULTICAST_ROUTE_TABLE_MESSAGES = 
	    new MultidastLimeSentMessageStat();	    

	/**
	 * <tt>Statistid</tt> for Gnutella filtered messages sent 
	 * over UDP.
	 */
	pualid stbtic final Statistic UDP_FILTERED_MESSAGES = 
	    new FilteredLimeSentMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella filtered messages sent 
	 * over TCP.
	 */
	pualid stbtic final Statistic TCP_FILTERED_MESSAGES = 
	    new FilteredLimeSentMessageStat();
	    
	/**
	 * <tt>Statistid</tt> for Gnutella filtered messages sent 
	 * over Multidast.
	 */
	pualid stbtic final Statistic MULTICAST_FILTERED_MESSAGES = 
	    new FilteredLimeSentMessageStat();	    

	/**
	 * <tt>Statistid</tt> for Gnutella hops flow messages sent over 
	 * TCP.
	 */
	pualid stbtic final Statistic TCP_HOPS_FLOW = 
	    new TCPLimeSentMessageStat();

    /**
     * <tt>Statistid</tt> for Gnutella TCP GIVE_STATS message
     */ 
	pualid stbtic final Statistic TCP_GIVE_STATS = new TCPLimeSentMessageStat();

    /**
     * <tt>Statistid</tt> for Gnutella UDP GIVE_STATS message
     */ 
	pualid stbtic final Statistic UDP_GIVE_STATS = new UDPLimeSentMessageStat();



	/**
	 * <tt>Statistid</tt> for Gnutella meta-vendor messages sent over 
	 * TCP.
	 */
	pualid stbtic final Statistic TCP_MESSAGES_SUPPORTED = 
	    new TCPLimeSentMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella TCP ConnectBack messages sent over 
	 * TCP.
	 */
	pualid stbtic final Statistic TCP_TCP_CONNECTBACK = 
	    new TCPLimeSentMessageStat();
	    
	/**
	 * <tt>Statistid</tt> for Gnutella UDP ConnectBack sent over 
	 * TCP.
	 */
	pualid stbtic final Statistic TCP_UDP_CONNECTBACK = 
	    new TCPLimeSentMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella ReplyNumber VM sent over 
	 * UDP.
	 */
	pualid stbtic final Statistic UDP_REPLY_NUMBER = 
	    new UDPLimeSentMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella LimeACK VM sent over 
	 * UDP.
	 */
	pualid stbtic final Statistic UDP_LIME_ACK = 
	    new UDPLimeSentMessageStat();


}
