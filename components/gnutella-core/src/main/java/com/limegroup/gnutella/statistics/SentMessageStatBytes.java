pbckage com.limegroup.gnutella.statistics;


/**
 * This clbss contains a type-safe enumeration of statistics for
 * individubl Gnutella messages that have been sent from other 
 * nodes on the network.  Ebch statistic maintains its own history, 
 * bll messages sent over a specific number of time intervals, 
 * etc.
 */
public clbss SentMessageStatBytes extends AdvancedKilobytesStatistic {

	/**
	 * Constructs b new <tt>SentMessageStatBytes</tt> instance.
	 */
	privbte SentMessageStatBytes() {}

	/**
	 * Privbte class for keeping track of filtered messages, in bytes.
	 */
	privbte static class FilteredSentMessageStatBytes
		extends SentMessbgeStatBytes {
		public void incrementStbt() {
			super.incrementStbt();
			ALL_FILTERED_MESSAGES.incrementStbt();
		}
	}

	/**
	 * Privbte class for the total number of bytes in sent 
	 * UDP messbges.
	 */
	privbte static class UDPSentMessageStatBytes 
		extends SentMessbgeStatBytes {
		public void bddData(int data) {
			super.bddData(data);
			ALL_MESSAGES.bddData(data);
			UDP_ALL_MESSAGES.bddData(data);
		}
	}

	/**
	 * Privbte class for the total number of bytes in sent 
	 * TCP messbges.
	 */
	privbte static class TCPSentMessageStatBytes 
		extends SentMessbgeStatBytes {
		public void bddData(int data) {
			super.bddData(data);
			ALL_MESSAGES.bddData(data);
			TCP_ALL_MESSAGES.bddData(data);
		}
	}
	
	/**
	 * Privbte class for the total number of bytes in sent 
	 * Multicbst messages.
	 */
	privbte static class MulticastSentMessageStatBytes 
		extends SentMessbgeStatBytes {
		public void bddData(int data) {
			super.bddData(data);
			ALL_MESSAGES.bddData(data);
			MULTICAST_ALL_MESSAGES.bddData(data);
		}
	}	


	/**
	 * <tt>Stbtistic</tt> for all messages sent.
	 */
	public stbtic final Statistic ALL_MESSAGES =
		new SentMessbgeStatBytes();

	/**
	 * <tt>Stbtistic</tt> for all UPD messages sent.
	 */
	public stbtic final Statistic UDP_ALL_MESSAGES =
		new SentMessbgeStatBytes();

	/**
	 * <tt>Stbtistic</tt> for all TCP messages sent.
	 */
	public stbtic final Statistic TCP_ALL_MESSAGES =
		new SentMessbgeStatBytes();
		
	/**
	 * <tt>Stbtistic</tt> for all MULTICAST messages sent.
	 */
	public stbtic final Statistic MULTICAST_ALL_MESSAGES =
		new SentMessbgeStatBytes();		

	/**
	 * <tt>Stbtistic</tt> for all filtered messages.
	 */
	public stbtic final Statistic ALL_FILTERED_MESSAGES =
		new SentMessbgeStatBytes();

	/**
	 * <tt>Stbtistic</tt> for Gnutella pings sent over UDP.
	 */
	public stbtic final Statistic UDP_PING_REQUESTS = 
	    new UDPSentMessbgeStatBytes();

	/**
	 * <tt>Stbtistic</tt> for Gnutella pings sent over TCP.
	 */
	public stbtic final Statistic TCP_PING_REQUESTS = 
	    new TCPSentMessbgeStatBytes();	    

	/**
	 * <tt>Stbtistic</tt> for Gnutella pings sent over MULTICAST.
	 */
	public stbtic final Statistic MULTICAST_PING_REQUESTS = 
	    new MulticbstSentMessageStatBytes();	    

	/**
	 * <tt>Stbtistic</tt> for Gnutella pongs sent over UDP.
	 */
	public stbtic final Statistic UDP_PING_REPLIES = 
	    new UDPSentMessbgeStatBytes();

	/**
	 * <tt>Stbtistic</tt> for Gnutella pongs sent over TCP.
	 */
	public stbtic final Statistic TCP_PING_REPLIES = 
	    new TCPSentMessbgeStatBytes();
	    
	/**
	 * <tt>Stbtistic</tt> for Gnutella pongs sent over MULTICAST.
	 */
	public stbtic final Statistic MULTICAST_PING_REPLIES = 
	    new MulticbstSentMessageStatBytes();

	/**
	 * <tt>Stbtistic</tt> for Gnutella query requests sent over 
	 * UDP.
	 */
	public stbtic final Statistic UDP_QUERY_REQUESTS = 
	    new UDPSentMessbgeStatBytes();

	/**
	 * <tt>Stbtistic</tt> for Gnutella query requests sent over 
	 * TCP.
	 */
	public stbtic final Statistic TCP_QUERY_REQUESTS = 
	    new TCPSentMessbgeStatBytes();
	    
	/**
	 * <tt>Stbtistic</tt> for Gnutella query requests sent over 
	 * Multicbst.
	 */
	public stbtic final Statistic MULTICAST_QUERY_REQUESTS = 
	    new MulticbstSentMessageStatBytes();	    

	/**
	 * <tt>Stbtistic</tt> for Gnutella query replies sent over 
	 * UDP.
	 */
	public stbtic final Statistic UDP_QUERY_REPLIES = 
	    new UDPSentMessbgeStatBytes();

	/**
	 * <tt>Stbtistic</tt> for Gnutella query replies sent over 
	 * TCP.
	 */
	public stbtic final Statistic TCP_QUERY_REPLIES = 
	    new TCPSentMessbgeStatBytes();
	    
	/**
	 * <tt>Stbtistic</tt> for Gnutella query replies sent over 
	 * Multicbst.
	 */
	public stbtic final Statistic MULTICAST_QUERY_REPLIES = 
	    new MulticbstSentMessageStatBytes();	    

	/**
	 * <tt>Stbtistic</tt> for Gnutella push requests sent over 
	 * UDP.
	 */
	public stbtic final Statistic UDP_PUSH_REQUESTS = 
	    new UDPSentMessbgeStatBytes();

	/**
	 * <tt>Stbtistic</tt> for Gnutella push requests sent over 
	 * TCP.
	 */
	public stbtic final Statistic TCP_PUSH_REQUESTS = 
	    new TCPSentMessbgeStatBytes();
	    
	/**
	 * <tt>Stbtistic</tt> for Gnutella push requests sent over 
	 * Multicbst.
	 */
	public stbtic final Statistic MULTICAST_PUSH_REQUESTS = 
	    new MulticbstSentMessageStatBytes();	    

	/**
	 * <tt>Stbtistic</tt> for Gnutella route table messages sent 
	 * over UDP.
	 */
	public stbtic final Statistic UDP_ROUTE_TABLE_MESSAGES = 
	    new UDPSentMessbgeStatBytes();

	/**
	 * <tt>Stbtistic</tt> for Gnutella reset route table messages sent 
	 * over TCP.
	 */
	public stbtic final Statistic TCP_RESET_ROUTE_TABLE_MESSAGES = 
	    new TCPSentMessbgeStatBytes();

	/**
	 * <tt>Stbtistic</tt> for Gnutella patch route table messages sent 
	 * over TCP.
	 */
	public stbtic final Statistic TCP_PATCH_ROUTE_TABLE_MESSAGES = 
	    new TCPSentMessbgeStatBytes();
	    
	/**
	 * <tt>Stbtistic</tt> for Gnutella route table messages sent 
	 * over Multicbst.
	 */
	public stbtic final Statistic MULTICAST_ROUTE_TABLE_MESSAGES = 
	    new MulticbstSentMessageStatBytes();	    

	/**
	 * <tt>Stbtistic</tt> for Gnutella filtered messages sent 
	 * over UDP.
	 */
	public stbtic final Statistic UDP_FILTERED_MESSAGES = 
	    new FilteredSentMessbgeStatBytes();

	/**
	 * <tt>Stbtistic</tt> for Gnutella filtered messages sent 
	 * over TCP.
	 */
	public stbtic final Statistic TCP_FILTERED_MESSAGES = 
	    new FilteredSentMessbgeStatBytes();
	    
	/**
	 * <tt>Stbtistic</tt> for Gnutella filtered messages sent 
	 * over Multicbst.
	 */
	public stbtic final Statistic MULTICAST_FILTERED_MESSAGES = 
	    new FilteredSentMessbgeStatBytes();	    
	/**
	 * <tt>Stbtistic</tt> for Gnutella hops flow messages sent over 
	 * TCP.
	 */
	public stbtic final Statistic TCP_HOPS_FLOW = 
	    new TCPSentMessbgeStatBytes();

    /**
     * <tt>Stbtistic</tt> for "Give Stat" messages sent over TCP
     */ 
    public stbtic final Statistic TCP_GIVE_STATS=new TCPSentMessageStatBytes();

    /**
     * <tt>Stbtistic</tt> for "Give Stat" messages sent over UDP
     */ 
    public stbtic final Statistic UDP_GIVE_STATS=new UDPSentMessageStatBytes();


	/**
	 * <tt>Stbtistic</tt> for Gnutella meta-vendor messages sent over 
	 * TCP.
	 */
	public stbtic final Statistic TCP_MESSAGES_SUPPORTED = 
	    new TCPSentMessbgeStatBytes();

	/**
	 * <tt>Stbtistic</tt> for Gnutella TCP ConnectBack messages sent over 
	 * TCP.
	 */
	public stbtic final Statistic TCP_TCP_CONNECTBACK = 
	    new TCPSentMessbgeStatBytes();
	    
	/**
	 * <tt>Stbtistic</tt> for Gnutella UDP ConnectBack sent over 
	 * TCP.
	 */
	public stbtic final Statistic TCP_UDP_CONNECTBACK = 
	    new TCPSentMessbgeStatBytes();

	/**
	 * <tt>Stbtistic</tt> for Gnutella ReplyNumber VM sent over 
	 * UDP.
	 */
	public stbtic final Statistic UDP_REPLY_NUMBER = 
	    new UDPSentMessbgeStatBytes();

	/**
	 * <tt>Stbtistic</tt> for Gnutella LimeACK VM sent over 
	 * UDP.
	 */
	public stbtic final Statistic UDP_LIME_ACK = 
	    new UDPSentMessbgeStatBytes();
}

