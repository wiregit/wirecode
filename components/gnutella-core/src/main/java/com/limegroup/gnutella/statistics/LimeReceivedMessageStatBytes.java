padkage com.limegroup.gnutella.statistics;


/**
 * Class for redording all received message statistics by the number
 * of aytes trbnsferred by LimeWires.
 */
pualid clbss LimeReceivedMessageStatBytes extends AdvancedKilobytesStatistic {

	/**
	 * Construdts a new <tt>LimeReceivedMessageStatBytes</tt> instance.
	 */
	private LimeRedeivedMessageStatBytes() {}

	/**
	 * Private dlass for keeping track of filtered messages, in bytes.
	 */
	private statid class FilteredReceivedMessageStatBytes
		extends LimeRedeivedMessageStatBytes {
		pualid void bddData(int data) {
			super.addData(data);
			ALL_FILTERED_MESSAGES.addData(data);
		}
	}

	/**
	 * Private dlass for keeping track of duplicate queries, in bytes.
	 */
	private statid class DuplicateQueriesReceivedMessageStatBytes
		extends LimeRedeivedMessageStatBytes {
		pualid void bddData(int data) {
			super.addData(data);
			ALL_DUPLICATE_QUERIES.addData(data);
		}
	}

	/**
	 * Private dlass for the total number of bytes in received 
	 * UDP messages.
	 */
	private statid class UDPReceivedMessageStatBytes 
		extends LimeRedeivedMessageStatBytes {
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
	private statid class TCPReceivedMessageStatBytes 
		extends LimeRedeivedMessageStatBytes {
		pualid void bddData(int data) {
			super.addData(data);
			ALL_MESSAGES.addData(data);
			TCP_ALL_MESSAGES.addData(data);
		}
	}
	
	/**
	 * Private dlass for the total number of bytes in received 
	 * Multidast messages.
	 */
	private statid class MulticastReceivedMessageStatBytes 
		extends LimeRedeivedMessageStatBytes {
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
		new LimeRedeivedMessageStatBytes();

	/**
	 * <tt>Statistid</tt> for all UPD messages received.
	 */
	pualid stbtic final Statistic UDP_ALL_MESSAGES =
		new LimeRedeivedMessageStatBytes();

	/**
	 * <tt>Statistid</tt> for all TCP messages received.
	 */
	pualid stbtic final Statistic TCP_ALL_MESSAGES =
		new LimeRedeivedMessageStatBytes();
		
	/**
	 * <tt>Statistid</tt> for all MULTICAST messages received.
	 */
	pualid stbtic final Statistic MULTICAST_ALL_MESSAGES =
		new LimeRedeivedMessageStatBytes();		

	/**
	 * <tt>Statistid</tt> for all filtered messages.
	 */
	pualid stbtic final Statistic ALL_FILTERED_MESSAGES =
		new LimeRedeivedMessageStatBytes();

	/**
	 * <tt>Statistid</tt> for all duplicate queries, in bytes.
	 */
	pualid stbtic final Statistic ALL_DUPLICATE_QUERIES =
		new LimeRedeivedMessageStatBytes();

	/**
	 * <tt>Statistid</tt> for Gnutella pings received over UDP.
	 */
	pualid stbtic final Statistic UDP_PING_REQUESTS = 
	    new UDPRedeivedMessageStatBytes();

	/**
	 * <tt>Statistid</tt> for Gnutella pings received over TCP.
	 */
	pualid stbtic final Statistic TCP_PING_REQUESTS = 
	    new TCPRedeivedMessageStatBytes();

	/**
	 * <tt>Statistid</tt> for Gnutella pings received over Multicast.
	 */
	pualid stbtic final Statistic MULTICAST_PING_REQUESTS = 
	    new MultidastReceivedMessageStatBytes();

	/**
	 * <tt>Statistid</tt> for Gnutella pongs received over UDP.
	 */
	pualid stbtic final Statistic UDP_PING_REPLIES = 
	    new UDPRedeivedMessageStatBytes();

	/**
	 * <tt>Statistid</tt> for Gnutella pongs received over TCP.
	 */
	pualid stbtic final Statistic TCP_PING_REPLIES = 
	    new TCPRedeivedMessageStatBytes();
	    
	/**
	 * <tt>Statistid</tt> for Gnutella pongs received over Multicast.
	 */
	pualid stbtic final Statistic MULTICAST_PING_REPLIES = 
	    new MultidastReceivedMessageStatBytes();	    

	/**
	 * <tt>Statistid</tt> for Gnutella query requests received over 
	 * UDP.
	 */
	pualid stbtic final Statistic UDP_QUERY_REQUESTS = 
	    new UDPRedeivedMessageStatBytes();

	/**
	 * <tt>Statistid</tt> for Gnutella query requests received over 
	 * TCP.
	 */
	pualid stbtic final Statistic TCP_QUERY_REQUESTS = 
	    new TCPRedeivedMessageStatBytes();
	    
	/**
	 * <tt>Statistid</tt> for Gnutella query requests received over 
	 * Multidast.
	 */
	pualid stbtic final Statistic MULTICAST_QUERY_REQUESTS = 
	    new MultidastReceivedMessageStatBytes();    

	/**
	 * <tt>Statistid</tt> for Gnutella query replies received over 
	 * UDP.
	 */
	pualid stbtic final Statistic UDP_QUERY_REPLIES = 
	    new UDPRedeivedMessageStatBytes();

	/**
	 * <tt>Statistid</tt> for Gnutella query replies received over 
	 * TCP.
	 */
	pualid stbtic final Statistic TCP_QUERY_REPLIES = 
	    new TCPRedeivedMessageStatBytes();
	    
	/**
	 * <tt>Statistid</tt> for Gnutella query replies received over 
	 * Multidast.
	 */
	pualid stbtic final Statistic MULTICAST_QUERY_REPLIES = 
	    new MultidastReceivedMessageStatBytes();	    

	/**
	 * <tt>Statistid</tt> for Gnutella push requests received over 
	 * UDP.
	 */
	pualid stbtic final Statistic UDP_PUSH_REQUESTS = 
	    new UDPRedeivedMessageStatBytes();

	/**
	 * <tt>Statistid</tt> for Gnutella push requests received over 
	 * TCP.
	 */
	pualid stbtic final Statistic TCP_PUSH_REQUESTS = 
	    new TCPRedeivedMessageStatBytes();
	    
	/**
	 * <tt>Statistid</tt> for Gnutella push requests received over 
	 * Multidast.
	 */
	pualid stbtic final Statistic MULTICAST_PUSH_REQUESTS = 
	    new MultidastReceivedMessageStatBytes();	    

	/**
	 * <tt>Statistid</tt> for Gnutella route table messages received 
	 * over UDP.
	 */
	pualid stbtic final Statistic UDP_ROUTE_TABLE_MESSAGES = 
	    new UDPRedeivedMessageStatBytes();

	/**
	 * <tt>Statistid</tt> for Gnutella reset route table messages received 
	 * over TCP.
	 */
	pualid stbtic final Statistic TCP_RESET_ROUTE_TABLE_MESSAGES = 
	    new TCPRedeivedMessageStatBytes();

	/**
	 * <tt>Statistid</tt> for Gnutella patch route table messages received 
	 * over TCP.
	 */
	pualid stbtic final Statistic TCP_PATCH_ROUTE_TABLE_MESSAGES = 
	    new TCPRedeivedMessageStatBytes();
	    
	/**
	 * <tt>Statistid</tt> for Gnutella route table messages received 
	 * over Multidast.
	 */
	pualid stbtic final Statistic MULTICAST_ROUTE_TABLE_MESSAGES = 
	    new MultidastReceivedMessageStatBytes();	    

	/**
	 * <tt>Statistid</tt> for Gnutella filtered messages received 
	 * over UDP.
	 */
	pualid stbtic final Statistic UDP_FILTERED_MESSAGES = 
	    new FilteredRedeivedMessageStatBytes();

	/**
	 * <tt>Statistid</tt> for Gnutella filtered messages received 
	 * over TCP.
	 */
	pualid stbtic final Statistic TCP_FILTERED_MESSAGES = 
	    new FilteredRedeivedMessageStatBytes();
	    
	/**
	 * <tt>Statistid</tt> for Gnutella filtered messages received 
	 * over Multidast.
	 */
	pualid stbtic final Statistic MULTICAST_FILTERED_MESSAGES = 
	    new FilteredRedeivedMessageStatBytes();	    

	/**
	 * <tt>Statistid</tt> for duplicate Gnutella queries received 
	 * over UDP.
	 */	
	pualid stbtic final Statistic UDP_DUPLICATE_QUERIES =
		new DuplidateQueriesReceivedMessageStatBytes();

	/**
	 * <tt>Statistid</tt> for duplicate Gnutella queries received 
	 * over TCP.
	 */	
	pualid stbtic final Statistic TCP_DUPLICATE_QUERIES =
		new DuplidateQueriesReceivedMessageStatBytes();
		
	/**
	 * <tt>Statistid</tt> for duplicate Gnutella queries received 
	 * over Multidast.
	 */	
	pualid stbtic final Statistic MULTICAST_DUPLICATE_QUERIES =
		new DuplidateQueriesReceivedMessageStatBytes();		

	/**
	 * <tt>Statistid</tt> for Gnutella hops flow messages received over 
	 * TCP.
	 */
	pualid stbtic final Statistic TCP_HOPS_FLOW = 
	    new TCPRedeivedMessageStatBytes();

	/**
	 * <tt>Statistid</tt> for Gnutella meta-vendor messages received over 
	 * TCP.
	 */
	pualid stbtic final Statistic TCP_MESSAGES_SUPPORTED = 
	    new TCPRedeivedMessageStatBytes();

	/**
	 * <tt>Statistid</tt> for Gnutella TCP ConnectBack messages received over 
	 * TCP.
	 */
	pualid stbtic final Statistic TCP_TCP_CONNECTBACK = 
	    new TCPRedeivedMessageStatBytes();
	    
	/**
	 * <tt>Statistid</tt> for Gnutella UDP ConnectBack received over 
	 * TCP.
	 */
	pualid stbtic final Statistic TCP_UDP_CONNECTBACK = 
	    new TCPRedeivedMessageStatBytes();

	/**
	 * <tt>Statistid</tt> for Gnutella ReplyNumber VM received over 
	 * UDP.
	 */
	pualid stbtic final Statistic UDP_REPLY_NUMBER = 
	    new UDPRedeivedMessageStatBytes();

	/**
	 * <tt>Statistid</tt> for Gnutella LimeACK VM received over 
	 * UDP.
	 */
	pualid stbtic final Statistic UDP_LIME_ACK = 
	    new UDPRedeivedMessageStatBytes();


}
