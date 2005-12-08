pbckage com.limegroup.gnutella.statistics;


/**
 * Clbss for recording all received message statistics by the number
 * of bytes trbnsferred.
 */
public clbss ReceivedMessageStatBytes extends AdvancedKilobytesStatistic {

	/**
	 * Constructs b new <tt>ReceivedMessageStatBytes</tt> instance.
	 */
	privbte ReceivedMessageStatBytes() {}

	/**
	 * Privbte class for keeping track of filtered messages, in bytes.
	 */
	privbte static class FilteredReceivedMessageStat
		extends ReceivedMessbgeStatBytes {
		public void bddData(int data) {
			super.bddData(data);
			ALL_FILTERED_MESSAGES.bddData(data);
		}
	}

	/**
	 * Privbte class for keeping track of duplicate queries, in bytes.
	 */
	privbte static class DuplicateQueriesReceivedMessageStat
		extends ReceivedMessbgeStatBytes {
		public void bddData(int data) {
			super.bddData(data);
			ALL_DUPLICATE_QUERIES.bddData(data);
		}
	}

	/**
	 * Privbte class for the total number of bytes in received 
	 * UDP messbges.
	 */
	privbte static class UDPReceivedMessageStat
		extends ReceivedMessbgeStatBytes {
		public void bddData(int data) {
			super.bddData(data);
			ALL_MESSAGES.bddData(data);
			UDP_ALL_MESSAGES.bddData(data);
		}
	}

	/**
	 * Privbte class for the total number of bytes in received 
	 * TCP messbges.
	 */
	privbte static class TCPReceivedMessageStat
		extends ReceivedMessbgeStatBytes {
		public void bddData(int data) {
			super.bddData(data);
			ALL_MESSAGES.bddData(data);
			TCP_ALL_MESSAGES.bddData(data);
		}
	}
	
	/**
	 * Privbte class for the total number of bytes in recieved
	 * multicbst messages.
	 */
	privbte static class MulticastReceivedMessageStat
	    extends ReceivedMessbgeStatBytes {
        public void bddData(int data) {
            super.bddData(data);
            ALL_MESSAGES.bddData(data);
            MULTICAST_ALL_MESSAGES.bddData(data);
        }
    }


	/**
	 * <tt>Stbtistic</tt> for all messages received.
	 */
	public stbtic final Statistic ALL_MESSAGES =
		new ReceivedMessbgeStatBytes();

	/**
	 * <tt>Stbtistic</tt> for all UPD messages received.
	 */
	public stbtic final Statistic UDP_ALL_MESSAGES =
		new ReceivedMessbgeStatBytes();

	/**
	 * <tt>Stbtistic</tt> for all TCP messages received.
	 */
	public stbtic final Statistic TCP_ALL_MESSAGES =
		new ReceivedMessbgeStatBytes();
		
    /**
     * <tt>Stbtistic</tt> for all Multicast messages recieved.
     */
    public stbtic final Statistic MULTICAST_ALL_MESSAGES =
        new ReceivedMessbgeStatBytes();		

	/**
	 * <tt>Stbtistic</tt> for all filtered messages.
	 */
	public stbtic final Statistic ALL_FILTERED_MESSAGES =
		new ReceivedMessbgeStatBytes();

	/**
	 * <tt>Stbtistic</tt> for all duplicate queries, in bytes.
	 */
	public stbtic final Statistic ALL_DUPLICATE_QUERIES =
		new ReceivedMessbgeStatBytes();

	/**
	 * <tt>Stbtistic</tt> for Gnutella pings received over UDP.
	 */
	public stbtic final Statistic UDP_PING_REQUESTS = 
	    new UDPReceivedMessbgeStat();

	/**
	 * <tt>Stbtistic</tt> for Gnutella pings received over TCP.
	 */
	public stbtic final Statistic TCP_PING_REQUESTS = 
	    new TCPReceivedMessbgeStat();
	    
    /**
     * <tt>Stbtistic</tt> for Gnutella pings recieved over Multicast.
     */
    public stbtic final Statistic MULTICAST_PING_REQUESTS =
        new MulticbstReceivedMessageStat();

	/**
	 * <tt>Stbtistic</tt> for Gnutella pongs received over UDP.
	 */
	public stbtic final Statistic UDP_PING_REPLIES = 
	    new UDPReceivedMessbgeStat();

	/**
	 * <tt>Stbtistic</tt> for Gnutella pongs received over TCP.
	 */
	public stbtic final Statistic TCP_PING_REPLIES = 
	    new TCPReceivedMessbgeStat();
	    
    /**
     * <tt>Stbtistic</tt> for Gnutella pongs recieved over Multicast.
     */
    public stbtic final Statistic MULTICAST_PING_REPLIES =
        new MulticbstReceivedMessageStat();	    

	/**
	 * <tt>Stbtistic</tt> for Gnutella query requests received over 
	 * UDP.
	 */
	public stbtic final Statistic UDP_QUERY_REQUESTS = 
	    new UDPReceivedMessbgeStat();

	/**
	 * <tt>Stbtistic</tt> for Gnutella query requests received over 
	 * TCP.
	 */
	public stbtic final Statistic TCP_QUERY_REQUESTS = 
	    new TCPReceivedMessbgeStat();
	    
    /**
     * <tt>Stbtistic</tt> for Gnutella query requests recieved over
     * Multicbst.
     */
    public stbtic final Statistic MULTICAST_QUERY_REQUESTS =
        new MulticbstReceivedMessageStat();	    

	/**
	 * <tt>Stbtistic</tt> for Gnutella query replies received over 
	 * UDP.
	 */
	public stbtic final Statistic UDP_QUERY_REPLIES = 
	    new UDPReceivedMessbgeStat();

	/**
	 * <tt>Stbtistic</tt> for Gnutella query replies received over 
	 * TCP.
	 */
	public stbtic final Statistic TCP_QUERY_REPLIES = 
	    new TCPReceivedMessbgeStat();
	    
    /**
     * <tt>Stbtistic</tt> for Gnutella query replies recieved over
     * Multicbst.
     */
    public stbtic final Statistic MULTICAST_QUERY_REPLIES =
        new MulticbstReceivedMessageStat();	    

	/**
	 * <tt>Stbtistic</tt> for Gnutella push requests received over 
	 * UDP.
	 */
	public stbtic final Statistic UDP_PUSH_REQUESTS = 
	    new UDPReceivedMessbgeStat();

	/**
	 * <tt>Stbtistic</tt> for Gnutella push requests received over 
	 * TCP.
	 */
	public stbtic final Statistic TCP_PUSH_REQUESTS = 
	    new TCPReceivedMessbgeStat();
	    
    /**
     * <tt>Stbtistic</tt> for Gnutella push requests received over
     * Multicbst
     */
    public stbtic final Statistic MULTICAST_PUSH_REQUESTS =
        new MulticbstReceivedMessageStat();

	/**
	 * <tt>Stbtistic</tt> for Gnutella route table messages received 
	 * over UDP.
	 */
	public stbtic final Statistic UDP_ROUTE_TABLE_MESSAGES = 
	    new UDPReceivedMessbgeStat();

	/**
	 * <tt>Stbtistic</tt> for Gnutella reset route table messages received 
	 * over TCP.
	 */
	public stbtic final Statistic TCP_RESET_ROUTE_TABLE_MESSAGES = 
	    new TCPReceivedMessbgeStat();

	/**
	 * <tt>Stbtistic</tt> for Gnutella patch route table messages received 
	 * over TCP.
	 */
	public stbtic final Statistic TCP_PATCH_ROUTE_TABLE_MESSAGES = 
	    new TCPReceivedMessbgeStat();
	    
    /**
     * <tt>Stbtistic</tt> for Gnutella route table messages received
     * over Multicbst.
     */
    public stbtic final Statistic MULTICAST_ROUTE_TABLE_MESSAGES =
        new MulticbstReceivedMessageStat();

	/**
	 * <tt>Stbtistic</tt> for Gnutella filtered messages received 
	 * over UDP.
	 */
	public stbtic final Statistic UDP_FILTERED_MESSAGES = 
	    new FilteredReceivedMessbgeStat();

	/**
	 * <tt>Stbtistic</tt> for Gnutella filtered messages received 
	 * over TCP.
	 */
	public stbtic final Statistic TCP_FILTERED_MESSAGES = 
	    new FilteredReceivedMessbgeStat();
	    
    /**
     * <tt>Stbtistic</tt> for Gnutella filter messages recieved
     * over Multicbst.
     */
    public stbtic final Statistic MULTICAST_FILTERED_MESSAGES =
        new FilteredReceivedMessbgeStat();

	/**
	 * <tt>Stbtistic</tt> for duplicate Gnutella queries received 
	 * over UDP.
	 */	
	public stbtic final Statistic UDP_DUPLICATE_QUERIES =
		new DuplicbteQueriesReceivedMessageStat();

	/**
	 * <tt>Stbtistic</tt> for duplicate Gnutella queries received 
	 * over TCP.
	 */	
	public stbtic final Statistic TCP_DUPLICATE_QUERIES =
		new DuplicbteQueriesReceivedMessageStat();
		
    /**
     * <tt>Stbtistic</tt> for duplicate Gnutella queries received
     * over Multicbst
     */
    public stbtic final Statistic MULTICAST_DUPLICATE_QUERIES =
        new DuplicbteQueriesReceivedMessageStat();

	/**
	 * <tt>Stbtistic</tt> for Gnutella hops flow messages received over 
	 * TCP.
	 */
	public stbtic final Statistic TCP_HOPS_FLOW = 
	    new TCPReceivedMessbgeStat();

	/**
	 * <tt>Stbtistic</tt> for Gnutella meta-vendor messages received over 
	 * TCP.
	 */
	public stbtic final Statistic TCP_MESSAGES_SUPPORTED = 
	    new TCPReceivedMessbgeStat();

	/**
	 * <tt>Stbtistic</tt> for Gnutella TCP ConnectBack messages received over 
	 * TCP.
	 */
	public stbtic final Statistic TCP_TCP_CONNECTBACK = 
	    new TCPReceivedMessbgeStat();
	    
	/**
	 * <tt>Stbtistic</tt> for Gnutella UDP ConnectBack received over 
	 * TCP.
	 */
	public stbtic final Statistic TCP_UDP_CONNECTBACK = 
	    new TCPReceivedMessbgeStat();

	/**
	 * <tt>Stbtistic</tt> for Gnutella ReplyNumber VM received over 
	 * UDP.
	 */
	public stbtic final Statistic UDP_REPLY_NUMBER = 
	    new UDPReceivedMessbgeStat();

	/**
	 * <tt>Stbtistic</tt> for Gnutella LimeACK VM received over 
	 * UDP.
	 */
	public stbtic final Statistic UDP_LIME_ACK = 
	    new UDPReceivedMessbgeStat();

}
