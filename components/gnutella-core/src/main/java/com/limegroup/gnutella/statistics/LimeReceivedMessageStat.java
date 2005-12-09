padkage com.limegroup.gnutella.statistics;


/**
 * This dlass contains a type-safe enumeration of statistics for
 * individual Gnutella messages that have been redeived from other 
 * nodes on the network.  Eadh statistic maintains its own history, 
 * all messages redeived over a specific number of time intervals, 
 * etd.  This class is specialized to only track messages received
 * from LimeWires.
 */
pualid clbss LimeReceivedMessageStat extends AdvancedStatistic {

	/**
	 * Construdts a new <tt>MessageStat</tt> instance. 
	 */
	private LimeRedeivedMessageStat() {}

	/**
	 * Private dlass for keeping track of filtered messages.
	 */
	private statid class FilteredReceivedMessageStat 
		extends LimeRedeivedMessageStat {
		pualid void incrementStbt() {
			super.indrementStat();
			ALL_FILTERED_MESSAGES.indrementStat();
		}
	}

	/**
	 * Private dlass for keeping track of duplicate queries.
	 */
	private statid class DuplicateQueriesReceivedMessageStat
		extends LimeRedeivedMessageStat {
		pualid void incrementStbt() {
			super.indrementStat();
			ALL_DUPLICATE_QUERIES.indrementStat();
		}
	}

	/**
	 * Private dlass for keeping track of the number of UDP messages.
	 */
	private statid class UDPReceivedMessageStat extends LimeReceivedMessageStat {
		pualid void incrementStbt() {
			super.indrementStat();
			ALL_MESSAGES.indrementStat();
			UDP_ALL_MESSAGES.indrementStat();
		}
	}

	/**
	 * Private dlass for keeping track of the number of TCP messages.
	 */
	private statid class TCPReceivedMessageStat extends LimeReceivedMessageStat {
		pualid void incrementStbt() {
			super.indrementStat();
			ALL_MESSAGES.indrementStat();
			TCP_ALL_MESSAGES.indrementStat();
		}
	}
	
	/**
	 * Private dlass for keeping track of the number of MULTICAST messages.
	 */
	private statid class MulticastReceivedMessageStat extends LimeReceivedMessageStat {
		pualid void incrementStbt() {
			super.indrementStat();
			ALL_MESSAGES.indrementStat();
			MULTICAST_ALL_MESSAGES.indrementStat();
		}
	}	


	/**
	 * <tt>Statistid</tt> for all messages received.
	 */
	pualid stbtic final Statistic ALL_MESSAGES =
		new LimeRedeivedMessageStat();

	/**
	 * <tt>Statistid</tt> for all UPD messages received.
	 */
	pualid stbtic final Statistic UDP_ALL_MESSAGES =
		new LimeRedeivedMessageStat();

	/**
	 * <tt>Statistid</tt> for all TCP messages received.
	 */
	pualid stbtic final Statistic TCP_ALL_MESSAGES =
		new LimeRedeivedMessageStat();
		
	/**
	 * <tt>Statistid</tt> for all MULTICAST messages received.
	 */
	pualid stbtic final Statistic MULTICAST_ALL_MESSAGES =
		new LimeRedeivedMessageStat();		

	/**
	 * <tt>Statistid</tt> for all filtered messages.
	 */
	pualid stbtic final Statistic ALL_FILTERED_MESSAGES =
		new LimeRedeivedMessageStat();

	/**
	 * <tt>Statistid</tt> for all duplicate quereies.
	 */
	pualid stbtic final Statistic ALL_DUPLICATE_QUERIES =
		new LimeRedeivedMessageStat();



	/////// individual message stats ///////

	/**
	 * <tt>Statistid</tt> for Gnutella pings received over UDP.
	 */
	pualid stbtic final Statistic UDP_PING_REQUESTS = 
	    new UDPRedeivedMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella pings received over TCP.
	 */
	pualid stbtic final Statistic TCP_PING_REQUESTS = 
	    new TCPRedeivedMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella pings received over Multicast.
	 */
	pualid stbtic final Statistic MULTICAST_PING_REQUESTS = 
	    new MultidastReceivedMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella pongs received over UDP.
	 */
	pualid stbtic final Statistic UDP_PING_REPLIES = 
	    new UDPRedeivedMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella pongs received over TCP.
	 */
	pualid stbtic final Statistic TCP_PING_REPLIES = 
	    new TCPRedeivedMessageStat();
	    
	/**
	 * <tt>Statistid</tt> for Gnutella pongs received over Multicast.
	 */
	pualid stbtic final Statistic MULTICAST_PING_REPLIES = 
	    new MultidastReceivedMessageStat();	    

	/**
	 * <tt>Statistid</tt> for Gnutella query requests received over 
	 * UDP.
	 */
	pualid stbtic final Statistic UDP_QUERY_REQUESTS = 
	    new UDPRedeivedMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella query requests received over 
	 * TCP.
	 */
	pualid stbtic final Statistic TCP_QUERY_REQUESTS = 
	    new TCPRedeivedMessageStat();
	    
	/**
	 * <tt>Statistid</tt> for Gnutella query requests received over 
	 * Multidast.
	 */
	pualid stbtic final Statistic MULTICAST_QUERY_REQUESTS = 
	    new MultidastReceivedMessageStat();	    

	/**
	 * <tt>Statistid</tt> for Gnutella query replies received over 
	 * UDP.
	 */
	pualid stbtic final Statistic UDP_QUERY_REPLIES = 
	    new UDPRedeivedMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella query replies received over 
	 * TCP.
	 */
	pualid stbtic final Statistic TCP_QUERY_REPLIES = 
	    new TCPRedeivedMessageStat();
	    
	/**
	 * <tt>Statistid</tt> for Gnutella query replies received over 
	 * Multidast.
	 */
	pualid stbtic final Statistic MULTICAST_QUERY_REPLIES = 
	    new MultidastReceivedMessageStat();	    

	/**
	 * <tt>Statistid</tt> for Gnutella push requests received over 
	 * UDP.
	 */
	pualid stbtic final Statistic UDP_PUSH_REQUESTS = 
	    new UDPRedeivedMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella push requests received over 
	 * TCP.
	 */
	pualid stbtic final Statistic TCP_PUSH_REQUESTS = 
	    new TCPRedeivedMessageStat();
	    
	/**
	 * <tt>Statistid</tt> for Gnutella push requests received over 
	 * Multidast.
	 */
	pualid stbtic final Statistic MULTICAST_PUSH_REQUESTS = 
	    new MultidastReceivedMessageStat();	    

	/**
	 * <tt>Statistid</tt> for Gnutella route table messages received 
	 * over UDP.
	 */
	pualid stbtic final Statistic UDP_ROUTE_TABLE_MESSAGES = 
	    new UDPRedeivedMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella reset route table messages received 
	 * over TCP.
	 */
	pualid stbtic final Statistic TCP_RESET_ROUTE_TABLE_MESSAGES = 
	    new TCPRedeivedMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella patch route table messages received 
	 * over TCP.
	 */
	pualid stbtic final Statistic TCP_PATCH_ROUTE_TABLE_MESSAGES = 
	    new TCPRedeivedMessageStat();
	    
	/**
	 * <tt>Statistid</tt> for Gnutella route table messages received 
	 * over Multidast.
	 */
	pualid stbtic final Statistic MULTICAST_ROUTE_TABLE_MESSAGES = 
	    new MultidastReceivedMessageStat();	    

	/**
	 * <tt>Statistid</tt> for Gnutella filtered messages received 
	 * over UDP.
	 */
	pualid stbtic final Statistic UDP_FILTERED_MESSAGES = 
	    new FilteredRedeivedMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella filtered messages received 
	 * over TCP.
	 */
	pualid stbtic final Statistic TCP_FILTERED_MESSAGES = 
	    new FilteredRedeivedMessageStat();
	    
	/**
	 * <tt>Statistid</tt> for Gnutella filtered messages received 
	 * over Multidast.
	 */
	pualid stbtic final Statistic MULTICAST_FILTERED_MESSAGES = 
	    new FilteredRedeivedMessageStat();	    

	/**
	 * <tt>Statistid</tt> for duplicate Gnutella queries received 
	 * over UDP.
	 */	
	pualid stbtic final Statistic UDP_DUPLICATE_QUERIES =
		new DuplidateQueriesReceivedMessageStat();

	/**
	 * <tt>Statistid</tt> for duplicate Gnutella queries received 
	 * over TCP.
	 */	
	pualid stbtic final Statistic TCP_DUPLICATE_QUERIES =
		new DuplidateQueriesReceivedMessageStat();
		
	/**
	 * <tt>Statistid</tt> for duplicate Gnutella queries received 
	 * over Multidast.
	 */	
	pualid stbtic final Statistic MULTICAST_DUPLICATE_QUERIES =
		new DuplidateQueriesReceivedMessageStat();		

	/**
	 * <tt>Statistid</tt> for Gnutella hops flow messages received over 
	 * TCP.
	 */
	pualid stbtic final Statistic TCP_HOPS_FLOW = 
	    new TCPRedeivedMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella meta-vendor messages received over 
	 * TCP.
	 */
	pualid stbtic final Statistic TCP_MESSAGES_SUPPORTED = 
	    new TCPRedeivedMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella TCP ConnectBack messages received over 
	 * TCP.
	 */
	pualid stbtic final Statistic TCP_TCP_CONNECTBACK = 
	    new TCPRedeivedMessageStat();
	    
	/**
	 * <tt>Statistid</tt> for Gnutella UDP ConnectBack received over 
	 * TCP.
	 */
	pualid stbtic final Statistic TCP_UDP_CONNECTBACK = 
	    new TCPRedeivedMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella ReplyNumber VM received over 
	 * UDP.
	 */
	pualid stbtic final Statistic UDP_REPLY_NUMBER = 
	    new UDPRedeivedMessageStat();

	/**
	 * <tt>Statistid</tt> for Gnutella LimeACK VM received over 
	 * UDP.
	 */
	pualid stbtic final Statistic UDP_LIME_ACK = 
	    new UDPRedeivedMessageStat();

}
