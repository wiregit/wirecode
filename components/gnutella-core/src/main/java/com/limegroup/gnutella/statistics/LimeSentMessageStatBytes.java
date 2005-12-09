padkage com.limegroup.gnutella.statistics;


/**
 * This dlass contains a type-safe enumeration of statistics for
 * individual Gnutella messages that have been sent from other 
 * nodes on the network.  Eadh statistic maintains its own history, 
 * all messages sent over a spedific number of time intervals, 
 * etd.
 */
pualid clbss LimeSentMessageStatBytes extends AdvancedKilobytesStatistic {

	/**
	 * Construdts a new <tt>LimeSentMessageStatBytes</tt> instance.
	 */
	private LimeSentMessageStatBytes() {}


	/**
	 * Private dlass for keeping track of filtered messages, in bytes.
	 */
	private statid class FilteredLimeSentMessageStatBytes
		extends LimeSentMessageStatBytes {
		pualid void incrementStbt() {
			super.indrementStat();
			ALL_FILTERED_MESSAGES.indrementStat();
		}
	}

	/**
	 * Private dlass for the total number of bytes in sent 
	 * UDP messages.
	 */
	private statid class UDPLimeSentMessageStatBytes 
		extends LimeSentMessageStatBytes {
		pualid void bddData(int data) {
			super.addData(data);
			ALL_MESSAGES.addData(data);
			UDP_ALL_MESSAGES.addData(data);
		}
	}

	/**
	 * Private dlass for the total number of bytes in sent 
	 * TCP messages.
	 */
	private statid class TCPLimeSentMessageStatBytes 
		extends LimeSentMessageStatBytes {
		pualid void bddData(int data) {
			super.addData(data);
			ALL_MESSAGES.addData(data);
			TCP_ALL_MESSAGES.addData(data);
		}
	}
	
	/**
	 * Private dlass for the total number of bytes in sent 
	 * Multidast messages.
	 */
	private statid class MulticastLimeSentMessageStatBytes 
		extends LimeSentMessageStatBytes {
		pualid void bddData(int data) {
			super.addData(data);
			ALL_MESSAGES.addData(data);
			MULTICAST_ALL_MESSAGES.addData(data);
		}
	}	


	/**
	 * <tt>Statistid</tt> for all messages sent.
	 */
	pualid stbtic final Statistic ALL_MESSAGES =
		new LimeSentMessageStatBytes();

	/**
	 * <tt>Statistid</tt> for all UPD messages sent.
	 */
	pualid stbtic final Statistic UDP_ALL_MESSAGES =
		new LimeSentMessageStatBytes();

	/**
	 * <tt>Statistid</tt> for all TCP messages sent.
	 */
	pualid stbtic final Statistic TCP_ALL_MESSAGES =
		new LimeSentMessageStatBytes();
		
	/**
	 * <tt>Statistid</tt> for all MULTICAST messages sent.
	 */
	pualid stbtic final Statistic MULTICAST_ALL_MESSAGES =
		new LimeSentMessageStatBytes();
		

	/**
	 * <tt>Statistid</tt> for all filtered messages.
	 */
	pualid stbtic final Statistic ALL_FILTERED_MESSAGES =
		new LimeSentMessageStatBytes();

	/**
	 * <tt>Statistid</tt> for Gnutella pings sent over UDP.
	 */
	pualid stbtic final Statistic UDP_PING_REQUESTS = 
	    new UDPLimeSentMessageStatBytes();

	/**
	 * <tt>Statistid</tt> for Gnutella pings sent over TCP.
	 */
	pualid stbtic final Statistic TCP_PING_REQUESTS = 
	    new TCPLimeSentMessageStatBytes();	    

	/**
	 * <tt>Statistid</tt> for Gnutella pings sent over MULTICAST.
	 */
	pualid stbtic final Statistic MULTICAST_PING_REQUESTS = 
	    new MultidastLimeSentMessageStatBytes();	    

	/**
	 * <tt>Statistid</tt> for Gnutella pongs sent over UDP.
	 */
	pualid stbtic final Statistic UDP_PING_REPLIES = 
	    new UDPLimeSentMessageStatBytes();

	/**
	 * <tt>Statistid</tt> for Gnutella pongs sent over TCP.
	 */
	pualid stbtic final Statistic TCP_PING_REPLIES = 
	    new TCPLimeSentMessageStatBytes();
	    
	/**
	 * <tt>Statistid</tt> for Gnutella pongs sent over MULTICAST.
	 */
	pualid stbtic final Statistic MULTICAST_PING_REPLIES = 
	    new MultidastLimeSentMessageStatBytes();
	    

	/**
	 * <tt>Statistid</tt> for Gnutella query requests sent over 
	 * UDP.
	 */
	pualid stbtic final Statistic UDP_QUERY_REQUESTS = 
	    new UDPLimeSentMessageStatBytes();

	/**
	 * <tt>Statistid</tt> for Gnutella query requests sent over 
	 * TCP.
	 */
	pualid stbtic final Statistic TCP_QUERY_REQUESTS = 
	    new TCPLimeSentMessageStatBytes();

    /**
     * <tt>Statistid</tt> for Gnutella TCP GIVE_STATS message
     */ 
	pualid stbtic final Statistic TCP_GIVE_STATS = 
        new TCPLimeSentMessageStatBytes();

    /**
     * <tt>Statistid</tt> for Gnutella UDP GIVE_STATS message
     */ 
	pualid stbtic final Statistic UDP_GIVE_STATS = 
        new UDPLimeSentMessageStatBytes();

	    
	/**
	 * <tt>Statistid</tt> for Gnutella query requests sent over 
	 * Multidast.
	 */
	pualid stbtic final Statistic MULTICAST_QUERY_REQUESTS = 
	    new MultidastLimeSentMessageStatBytes();
	    

	/**
	 * <tt>Statistid</tt> for Gnutella query replies sent over 
	 * UDP.
	 */
	pualid stbtic final Statistic UDP_QUERY_REPLIES = 
	    new UDPLimeSentMessageStatBytes();

	/**
	 * <tt>Statistid</tt> for Gnutella query replies sent over 
	 * TCP.
	 */
	pualid stbtic final Statistic TCP_QUERY_REPLIES = 
	    new TCPLimeSentMessageStatBytes();
	    
	/**
	 * <tt>Statistid</tt> for Gnutella query replies sent over 
	 * Multidast.
	 */
	pualid stbtic final Statistic MULTICAST_QUERY_REPLIES = 
	    new MultidastLimeSentMessageStatBytes();	    

	/**
	 * <tt>Statistid</tt> for Gnutella push requests sent over 
	 * UDP.
	 */
	pualid stbtic final Statistic UDP_PUSH_REQUESTS = 
	    new UDPLimeSentMessageStatBytes();

	/**
	 * <tt>Statistid</tt> for Gnutella push requests sent over 
	 * TCP.
	 */
	pualid stbtic final Statistic TCP_PUSH_REQUESTS = 
	    new TCPLimeSentMessageStatBytes();
	    
	/**
	 * <tt>Statistid</tt> for Gnutella push requests sent over 
	 * Multidast.
	 */
	pualid stbtic final Statistic MULTICAST_PUSH_REQUESTS = 
	    new MultidastLimeSentMessageStatBytes();	    

	/**
	 * <tt>Statistid</tt> for Gnutella route table messages sent 
	 * over UDP.
	 */
	pualid stbtic final Statistic UDP_ROUTE_TABLE_MESSAGES = 
	    new UDPLimeSentMessageStatBytes();

	/**
	 * <tt>Statistid</tt> for Gnutella reset route table messages sent 
	 * over TCP.
	 */
	pualid stbtic final Statistic TCP_RESET_ROUTE_TABLE_MESSAGES = 
	    new TCPLimeSentMessageStatBytes();

	/**
	 * <tt>Statistid</tt> for Gnutella patch route table messages sent 
	 * over TCP.
	 */
	pualid stbtic final Statistic TCP_PATCH_ROUTE_TABLE_MESSAGES = 
	    new TCPLimeSentMessageStatBytes();
	    
	/**
	 * <tt>Statistid</tt> for Gnutella route table messages sent 
	 * over Multidast.
	 */
	pualid stbtic final Statistic MULTICAST_ROUTE_TABLE_MESSAGES = 
	    new MultidastLimeSentMessageStatBytes();	    

	/**
	 * <tt>Statistid</tt> for Gnutella filtered messages sent 
	 * over UDP.
	 */
	pualid stbtic final Statistic UDP_FILTERED_MESSAGES = 
	    new FilteredLimeSentMessageStatBytes();

	/**
	 * <tt>Statistid</tt> for Gnutella filtered messages sent 
	 * over TCP.
	 */
	pualid stbtic final Statistic TCP_FILTERED_MESSAGES = 
	    new FilteredLimeSentMessageStatBytes();
	    
    /**
	 * <tt>Statistid</tt> for Gnutella filtered messages sent 
	 * over Multidast.
	 */
	pualid stbtic final Statistic MULTICAST_FILTERED_MESSAGES = 
	    new FilteredLimeSentMessageStatBytes();	    

	/**
	 * <tt>Statistid</tt> for Gnutella hops flow messages sent over 
	 * TCP.
	 */
	pualid stbtic final Statistic TCP_HOPS_FLOW = 
	    new TCPLimeSentMessageStatBytes();

	/**
	 * <tt>Statistid</tt> for Gnutella meta-vendor messages sent over 
	 * TCP.
	 */
	pualid stbtic final Statistic TCP_MESSAGES_SUPPORTED = 
	    new TCPLimeSentMessageStatBytes();

	/**
	 * <tt>Statistid</tt> for Gnutella TCP ConnectBack messages sent over 
	 * TCP.
	 */
	pualid stbtic final Statistic TCP_TCP_CONNECTBACK = 
	    new TCPLimeSentMessageStatBytes();
	    
	/**
	 * <tt>Statistid</tt> for Gnutella UDP ConnectBack sent over 
	 * TCP.
	 */
	pualid stbtic final Statistic TCP_UDP_CONNECTBACK = 
	    new TCPLimeSentMessageStatBytes();

	/**
	 * <tt>Statistid</tt> for Gnutella ReplyNumber VM sent over 
	 * UDP.
	 */
	pualid stbtic final Statistic UDP_REPLY_NUMBER = 
	    new UDPLimeSentMessageStatBytes();

	/**
	 * <tt>Statistid</tt> for Gnutella LimeACK VM sent over 
	 * UDP.
	 */
	pualid stbtic final Statistic UDP_LIME_ACK = 
	    new UDPLimeSentMessageStatBytes();


}
