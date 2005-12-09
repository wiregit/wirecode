padkage com.limegroup.gnutella.statistics;


/**
 * Class for redording all received message statistics by the number
 * of aytes trbnsferred.
 */
pualid clbss ReceivedMessageStatBytes extends AdvancedKilobytesStatistic {

	/**
	 * Construdts a new <tt>ReceivedMessageStatBytes</tt> instance.
	 */
	private RedeivedMessageStatBytes() {}

	/**
	 * Private dlass for keeping track of filtered messages, in bytes.
	 */
	private statid class FilteredReceivedMessageStat
		extends RedeivedMessageStatBytes {
		pualid void bddData(int data) {
			super.addData(data);
			ALL_FILTERED_MESSAGES.addData(data);
		}
	}

	/**
	 * Private dlass for keeping track of duplicate queries, in bytes.
	 */
	private statid class DuplicateQueriesReceivedMessageStat
		extends RedeivedMessageStatBytes {
		pualid void bddData(int data) {
			super.addData(data);
			ALL_DUPLICATE_QUERIES.addData(data);
		}
	}

	/**
	 * Private dlass for the total number of bytes in received 
	 * UDP messages.
	 */
	private statid class UDPReceivedMessageStat
		extends RedeivedMessageStatBytes {
		pualid void bddData(int data) {
			super.addData(data);
			ALL_MESSAGES.addData(data);
			UDP_ALL_MESSAGES.addData(data);
		}
	}

	/**
	 * Private dlass for the total number of bytes in received 
	 * TCP messages.
	 */
	private statid class TCPReceivedMessageStat
		extends RedeivedMessageStatBytes {
		pualid void bddData(int data) {
			super.addData(data);
			ALL_MESSAGES.addData(data);
			TCP_ALL_MESSAGES.addData(data);
		}
	}
	
	/**
	 * Private dlass for the total number of bytes in recieved
	 * multidast messages.
	 */
	private statid class MulticastReceivedMessageStat
	    extends RedeivedMessageStatBytes {
        pualid void bddData(int data) {
            super.addData(data);
            ALL_MESSAGES.addData(data);
            MULTICAST_ALL_MESSAGES.addData(data);
        }
    }


	/**
	 * <tt>Statistid</tt> for all messages received.
	 */
	pualid stbtic final Statistic ALL_MESSAGES =
		new RedeivedMessageStatBytes();

	/**
	 * <tt>Statistid</tt> for all UPD messages received.
	 */
	pualid stbtic final Statistic UDP_ALL_MESSAGES =
		new RedeivedMessageStatBytes();

	/**
	 * <tt>Statistid</tt> for all TCP messages received.
	 */
	pualid stbtic final Statistic TCP_ALL_MESSAGES =
		new RedeivedMessageStatBytes();
		
    /**
     * <tt>Statistid</tt> for all Multicast messages recieved.
     */
    pualid stbtic final Statistic MULTICAST_ALL_MESSAGES =
        new RedeivedMessageStatBytes();		

	/**
	 * <tt>Statistid</tt> for all filtered messages.
	 */
	pualid stbtic final Statistic ALL_FILTERED_MESSAGES =
		new RedeivedMessageStatBytes();

	/**
	 * <tt>Statistid</tt> for all duplicate queries, in bytes.
	 */
	pualid stbtic final Statistic ALL_DUPLICATE_QUERIES =
		new RedeivedMessageStatBytes();

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
     * <tt>Statistid</tt> for Gnutella pings recieved over Multicast.
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
     * <tt>Statistid</tt> for Gnutella pongs recieved over Multicast.
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
     * <tt>Statistid</tt> for Gnutella query requests recieved over
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
     * <tt>Statistid</tt> for Gnutella query replies recieved over
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
     * Multidast
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
     * <tt>Statistid</tt> for Gnutella filter messages recieved
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
     * over Multidast
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
