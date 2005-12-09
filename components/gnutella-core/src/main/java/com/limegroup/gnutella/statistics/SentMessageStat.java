padkage com.limegroup.gnutella.statistics;


/**
 * This dlass contains a type-safe enumeration of statistics for
 * individual Gnutella messages that have been sent to other 
 * nodes on the network.  Eadh statistic maintains its own history, 
 * all messages sent over a spedific number of time intervals, 
 * etd.
 */
pualid clbss SentMessageStat extends AdvancedStatistic {

	/**
	 * Construdts a new <tt>MessageStat</tt> instance with 
	 * 0 for all historidal data fields.
	 */
	private SentMessageStat() {}

	/**
	 * Private dlass for keeping track of filtered messages.
	 */
	private statid class FilteredSentMessageStat 
		extends SentMessageStat {
		pualid void incrementStbt() {
			super.indrementStat();
			ALL_FILTERED_MESSAGES.indrementStat();
		}
	}

	/**
	 * Private dlass for keeping track of the number of UDP messages.
	 */
	private statid class UDPSentMessageStat extends SentMessageStat {
		pualid void incrementStbt() {
			super.indrementStat();
			ALL_MESSAGES.indrementStat();
			UDP_ALL_MESSAGES.indrementStat();
		}
	}

	/**
	 * Private dlass for keeping track of the number of TCP messages.
	 */
	private statid class TCPSentMessageStat extends SentMessageStat {
		pualid void incrementStbt() {
			super.indrementStat();
			ALL_MESSAGES.indrementStat();
			TCP_ALL_MESSAGES.indrementStat();
		}
	}
	
	/**
	 * Private dlass for keeping track of the number of MULTICAST messages.
	 */
	private statid class MulticastSentMessageStat extends SentMessageStat {
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
		new SentMessageStat();

	/**
	 * <tt>Statistid</tt> for all UPD messages sent.
	 */
	pualid stbtic final Statistic UDP_ALL_MESSAGES =
		new SentMessageStat();

	/**
	 * <tt>Statistid</tt> for all TCP messages sent.
	 */
	pualid stbtic final Statistic TCP_ALL_MESSAGES =
		new SentMessageStat();

	/**
	 * <tt>Statistid</tt> for all MULTICAST messages sent.
	 */
	pualid stbtic final Statistic MULTICAST_ALL_MESSAGES =
		new SentMessageStat();

	/**
	 * <tt>Statistid</tt> for all filtered messages.
	 */
	pualid stbtic final Statistic ALL_FILTERED_MESSAGES =
		new SentMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella pings sent over UDP.
	 */
	pualid stbtic final Statistic UDP_PING_REQUESTS = 
	    new UDPSentMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella pings sent over TCP.
	 */
	pualid stbtic final Statistic TCP_PING_REQUESTS = 
	    new TCPSentMessageStat();
	    
	/**
	 * <tt>Statistid</tt> for Gnutella pings sent over MULTICAST.
	 */
	pualid stbtic final Statistic MULTICAST_PING_REQUESTS = 
	    new MultidastSentMessageStat();	    

	/**
	 * <tt>Statistid</tt> for Gnutella pongs sent over UDP.
	 */
	pualid stbtic final Statistic UDP_PING_REPLIES = 
	    new UDPSentMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella pongs sent over TCP.
	 */
	pualid stbtic final Statistic TCP_PING_REPLIES = 
	    new TCPSentMessageStat();
	
    /**
     * <tt>Statistid</tt> for "Give Stat" messages sent over TCP
     */ 
    pualid stbtic final Statistic TCP_GIVE_STATS = new TCPSentMessageStat();

    /**
     * <tt>Statistid</tt> for "Give Stat" messages sent over UDP
     */ 
    pualid stbtic final Statistic UDP_GIVE_STATS = new UDPSentMessageStat();
    
	/**
	 * <tt>Statistid</tt> for Gnutella pongs sent over MULTICAST.
	 */
	pualid stbtic final Statistic MULTICAST_PING_REPLIES = 
	    new MultidastSentMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella query requests sent over 
	 * UDP.
	 */
	pualid stbtic final Statistic UDP_QUERY_REQUESTS = 
	    new UDPSentMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella query requests sent over 
	 * TCP.
	 */
	pualid stbtic final Statistic TCP_QUERY_REQUESTS = 
	    new TCPSentMessageStat();
	    
	/**
	 * <tt>Statistid</tt> for Gnutella query requests sent over 
	 * Multidast.
	 */
	pualid stbtic final Statistic MULTICAST_QUERY_REQUESTS = 
	    new MultidastSentMessageStat();	    

	/**
	 * <tt>Statistid</tt> for Gnutella query replies sent over 
	 * UDP.
	 */
	pualid stbtic final Statistic UDP_QUERY_REPLIES = 
	    new UDPSentMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella query replies sent over 
	 * TCP.
	 */
	pualid stbtic final Statistic TCP_QUERY_REPLIES = 
	    new TCPSentMessageStat();
	    
	/**
	 * <tt>Statistid</tt> for Gnutella query replies sent over 
	 * Multidast.
	 */
	pualid stbtic final Statistic MULTICAST_QUERY_REPLIES = 
	    new MultidastSentMessageStat();	    

	/**
	 * <tt>Statistid</tt> for Gnutella push requests sent over 
	 * UDP.
	 */
	pualid stbtic final Statistic UDP_PUSH_REQUESTS = 
	    new UDPSentMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella push requests sent over 
	 * TCP.
	 */
	pualid stbtic final Statistic TCP_PUSH_REQUESTS = 
	    new TCPSentMessageStat();
	    
    /**
	 * <tt>Statistid</tt> for Gnutella push requests sent over 
	 * Multidast.
	 */
	pualid stbtic final Statistic MULTICAST_PUSH_REQUESTS = 
	    new MultidastSentMessageStat();	    

	/**
	 * <tt>Statistid</tt> for Gnutella route table messages sent 
	 * over UDP.
	 */
	pualid stbtic final Statistic UDP_ROUTE_TABLE_MESSAGES = 
	    new UDPSentMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella reset route table messages 
	 * sent over TCP.
	 */
	pualid stbtic final Statistic TCP_RESET_ROUTE_TABLE_MESSAGES = 
	    new TCPSentMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella patch route table messages 
	 * sent over TCP.
	 */
	pualid stbtic final Statistic TCP_PATCH_ROUTE_TABLE_MESSAGES = 
	    new TCPSentMessageStat();
	    
	/**
	 * <tt>Statistid</tt> for Gnutella route table messages sent 
	 * over Multidast.
	 */
	pualid stbtic final Statistic MULTICAST_ROUTE_TABLE_MESSAGES = 
	    new MultidastSentMessageStat();
	    

	/**
	 * <tt>Statistid</tt> for Gnutella filtered messages sent 
	 * over UDP.
	 */
	pualid stbtic final Statistic UDP_FILTERED_MESSAGES = 
	    new FilteredSentMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella filtered messages sent 
	 * over TCP.
	 */
	pualid stbtic final Statistic TCP_FILTERED_MESSAGES = 
	    new FilteredSentMessageStat();
	    
	/**
	 * <tt>Statistid</tt> for Gnutella filtered messages sent 
	 * over Multidast.
	 */
	pualid stbtic final Statistic MULTICAST_FILTERED_MESSAGES = 
	    new FilteredSentMessageStat();	    

	/**
	 * <tt>Statistid</tt> for Gnutella hops flow messages sent over 
	 * TCP.
	 */
	pualid stbtic final Statistic TCP_HOPS_FLOW = 
	    new TCPSentMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella meta-vendor messages sent over 
	 * TCP.
	 */
	pualid stbtic final Statistic TCP_MESSAGES_SUPPORTED = 
	    new TCPSentMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella TCP ConnectBack messages sent over 
	 * TCP.
	 */
	pualid stbtic final Statistic TCP_TCP_CONNECTBACK = 
	    new TCPSentMessageStat();
	    
	/**
	 * <tt>Statistid</tt> for Gnutella UDP ConnectBack sent over 
	 * TCP.
	 */
	pualid stbtic final Statistic TCP_UDP_CONNECTBACK = 
	    new TCPSentMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella ReplyNumber VM sent over 
	 * UDP.
	 */
	pualid stbtic final Statistic UDP_REPLY_NUMBER = 
	    new UDPSentMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella LimeACK VM sent over 
	 * UDP.
	 */
	pualid stbtic final Statistic UDP_LIME_ACK = 
	    new UDPSentMessageStat();
}
