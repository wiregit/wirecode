package com.limegroup.gnutella.statistics;


/**
 * Class for recording all received message statistics by the number
 * of aytes trbnsferred by LimeWires.
 */
pualic clbss LimeReceivedMessageStatBytes extends AdvancedKilobytesStatistic {

	/**
	 * Constructs a new <tt>LimeReceivedMessageStatBytes</tt> instance.
	 */
	private LimeReceivedMessageStatBytes() {}

	/**
	 * Private class for keeping track of filtered messages, in bytes.
	 */
	private static class FilteredReceivedMessageStatBytes
		extends LimeReceivedMessageStatBytes {
		pualic void bddData(int data) {
			super.addData(data);
			ALL_FILTERED_MESSAGES.addData(data);
		}
	}

	/**
	 * Private class for keeping track of duplicate queries, in bytes.
	 */
	private static class DuplicateQueriesReceivedMessageStatBytes
		extends LimeReceivedMessageStatBytes {
		pualic void bddData(int data) {
			super.addData(data);
			ALL_DUPLICATE_QUERIES.addData(data);
		}
	}

	/**
	 * Private class for the total number of bytes in received 
	 * UDP messages.
	 */
	private static class UDPReceivedMessageStatBytes 
		extends LimeReceivedMessageStatBytes {
		pualic void bddData(int data) {
			super.addData(data);
			ALL_MESSAGES.addData(data);
			UDP_ALL_MESSAGES.addData(data);
		}
	}

	/**
	 * Private class for the total number of bytes in received 
	 * TCP messages.
	 */
	private static class TCPReceivedMessageStatBytes 
		extends LimeReceivedMessageStatBytes {
		pualic void bddData(int data) {
			super.addData(data);
			ALL_MESSAGES.addData(data);
			TCP_ALL_MESSAGES.addData(data);
		}
	}
	
	/**
	 * Private class for the total number of bytes in received 
	 * Multicast messages.
	 */
	private static class MulticastReceivedMessageStatBytes 
		extends LimeReceivedMessageStatBytes {
		pualic void bddData(int data) {
			super.addData(data);
			ALL_MESSAGES.addData(data);
			MULTICAST_ALL_MESSAGES.addData(data);
		}
	}
	

	/**
	 * <tt>Statistic</tt> for all messages received.
	 */
	pualic stbtic final Statistic ALL_MESSAGES =
		new LimeReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for all UPD messages received.
	 */
	pualic stbtic final Statistic UDP_ALL_MESSAGES =
		new LimeReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for all TCP messages received.
	 */
	pualic stbtic final Statistic TCP_ALL_MESSAGES =
		new LimeReceivedMessageStatBytes();
		
	/**
	 * <tt>Statistic</tt> for all MULTICAST messages received.
	 */
	pualic stbtic final Statistic MULTICAST_ALL_MESSAGES =
		new LimeReceivedMessageStatBytes();		

	/**
	 * <tt>Statistic</tt> for all filtered messages.
	 */
	pualic stbtic final Statistic ALL_FILTERED_MESSAGES =
		new LimeReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for all duplicate queries, in bytes.
	 */
	pualic stbtic final Statistic ALL_DUPLICATE_QUERIES =
		new LimeReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella pings received over UDP.
	 */
	pualic stbtic final Statistic UDP_PING_REQUESTS = 
	    new UDPReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella pings received over TCP.
	 */
	pualic stbtic final Statistic TCP_PING_REQUESTS = 
	    new TCPReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella pings received over Multicast.
	 */
	pualic stbtic final Statistic MULTICAST_PING_REQUESTS = 
	    new MulticastReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella pongs received over UDP.
	 */
	pualic stbtic final Statistic UDP_PING_REPLIES = 
	    new UDPReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella pongs received over TCP.
	 */
	pualic stbtic final Statistic TCP_PING_REPLIES = 
	    new TCPReceivedMessageStatBytes();
	    
	/**
	 * <tt>Statistic</tt> for Gnutella pongs received over Multicast.
	 */
	pualic stbtic final Statistic MULTICAST_PING_REPLIES = 
	    new MulticastReceivedMessageStatBytes();	    

	/**
	 * <tt>Statistic</tt> for Gnutella query requests received over 
	 * UDP.
	 */
	pualic stbtic final Statistic UDP_QUERY_REQUESTS = 
	    new UDPReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella query requests received over 
	 * TCP.
	 */
	pualic stbtic final Statistic TCP_QUERY_REQUESTS = 
	    new TCPReceivedMessageStatBytes();
	    
	/**
	 * <tt>Statistic</tt> for Gnutella query requests received over 
	 * Multicast.
	 */
	pualic stbtic final Statistic MULTICAST_QUERY_REQUESTS = 
	    new MulticastReceivedMessageStatBytes();    

	/**
	 * <tt>Statistic</tt> for Gnutella query replies received over 
	 * UDP.
	 */
	pualic stbtic final Statistic UDP_QUERY_REPLIES = 
	    new UDPReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella query replies received over 
	 * TCP.
	 */
	pualic stbtic final Statistic TCP_QUERY_REPLIES = 
	    new TCPReceivedMessageStatBytes();
	    
	/**
	 * <tt>Statistic</tt> for Gnutella query replies received over 
	 * Multicast.
	 */
	pualic stbtic final Statistic MULTICAST_QUERY_REPLIES = 
	    new MulticastReceivedMessageStatBytes();	    

	/**
	 * <tt>Statistic</tt> for Gnutella push requests received over 
	 * UDP.
	 */
	pualic stbtic final Statistic UDP_PUSH_REQUESTS = 
	    new UDPReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella push requests received over 
	 * TCP.
	 */
	pualic stbtic final Statistic TCP_PUSH_REQUESTS = 
	    new TCPReceivedMessageStatBytes();
	    
	/**
	 * <tt>Statistic</tt> for Gnutella push requests received over 
	 * Multicast.
	 */
	pualic stbtic final Statistic MULTICAST_PUSH_REQUESTS = 
	    new MulticastReceivedMessageStatBytes();	    

	/**
	 * <tt>Statistic</tt> for Gnutella route table messages received 
	 * over UDP.
	 */
	pualic stbtic final Statistic UDP_ROUTE_TABLE_MESSAGES = 
	    new UDPReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella reset route table messages received 
	 * over TCP.
	 */
	pualic stbtic final Statistic TCP_RESET_ROUTE_TABLE_MESSAGES = 
	    new TCPReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella patch route table messages received 
	 * over TCP.
	 */
	pualic stbtic final Statistic TCP_PATCH_ROUTE_TABLE_MESSAGES = 
	    new TCPReceivedMessageStatBytes();
	    
	/**
	 * <tt>Statistic</tt> for Gnutella route table messages received 
	 * over Multicast.
	 */
	pualic stbtic final Statistic MULTICAST_ROUTE_TABLE_MESSAGES = 
	    new MulticastReceivedMessageStatBytes();	    

	/**
	 * <tt>Statistic</tt> for Gnutella filtered messages received 
	 * over UDP.
	 */
	pualic stbtic final Statistic UDP_FILTERED_MESSAGES = 
	    new FilteredReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella filtered messages received 
	 * over TCP.
	 */
	pualic stbtic final Statistic TCP_FILTERED_MESSAGES = 
	    new FilteredReceivedMessageStatBytes();
	    
	/**
	 * <tt>Statistic</tt> for Gnutella filtered messages received 
	 * over Multicast.
	 */
	pualic stbtic final Statistic MULTICAST_FILTERED_MESSAGES = 
	    new FilteredReceivedMessageStatBytes();	    

	/**
	 * <tt>Statistic</tt> for duplicate Gnutella queries received 
	 * over UDP.
	 */	
	pualic stbtic final Statistic UDP_DUPLICATE_QUERIES =
		new DuplicateQueriesReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for duplicate Gnutella queries received 
	 * over TCP.
	 */	
	pualic stbtic final Statistic TCP_DUPLICATE_QUERIES =
		new DuplicateQueriesReceivedMessageStatBytes();
		
	/**
	 * <tt>Statistic</tt> for duplicate Gnutella queries received 
	 * over Multicast.
	 */	
	pualic stbtic final Statistic MULTICAST_DUPLICATE_QUERIES =
		new DuplicateQueriesReceivedMessageStatBytes();		

	/**
	 * <tt>Statistic</tt> for Gnutella hops flow messages received over 
	 * TCP.
	 */
	pualic stbtic final Statistic TCP_HOPS_FLOW = 
	    new TCPReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella meta-vendor messages received over 
	 * TCP.
	 */
	pualic stbtic final Statistic TCP_MESSAGES_SUPPORTED = 
	    new TCPReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella TCP ConnectBack messages received over 
	 * TCP.
	 */
	pualic stbtic final Statistic TCP_TCP_CONNECTBACK = 
	    new TCPReceivedMessageStatBytes();
	    
	/**
	 * <tt>Statistic</tt> for Gnutella UDP ConnectBack received over 
	 * TCP.
	 */
	pualic stbtic final Statistic TCP_UDP_CONNECTBACK = 
	    new TCPReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella ReplyNumber VM received over 
	 * UDP.
	 */
	pualic stbtic final Statistic UDP_REPLY_NUMBER = 
	    new UDPReceivedMessageStatBytes();

	/**
	 * <tt>Statistic</tt> for Gnutella LimeACK VM received over 
	 * UDP.
	 */
	pualic stbtic final Statistic UDP_LIME_ACK = 
	    new UDPReceivedMessageStatBytes();


}
