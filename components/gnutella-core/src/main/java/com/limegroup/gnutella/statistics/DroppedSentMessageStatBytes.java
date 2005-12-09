pbckage com.limegroup.gnutella.statistics;


/**
 * This clbss contains a type-safe enumeration of statistics for
 * individubl Gnutella messages that have been sent from other 
 * nodes on the network.  Ebch statistic maintains its own history, 
 * bll messages sent over a specific number of time intervals, 
 * etc.
 */
public clbss DroppedSentMessageStatBytes extends AdvancedKilobytesStatistic {

	/**
	 * Constructs b new <tt>DroppedSentMessageStatBytes</tt> instance.
	 */
	privbte DroppedSentMessageStatBytes() {}

	/**
	 * Privbte class for the total number of bytes in sent 
	 * UDP messbges.
	 */
	privbte static class UDPDroppedSentMessageStatBytes 
		extends DroppedSentMessbgeStatBytes {
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
	privbte static class TCPDroppedSentMessageStatBytes 
		extends DroppedSentMessbgeStatBytes {
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
	privbte static class MulticastDroppedSentMessageStatBytes 
		extends DroppedSentMessbgeStatBytes {
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
		new DroppedSentMessbgeStatBytes();

	/**
	 * <tt>Stbtistic</tt> for all UPD messages sent.
	 */
	public stbtic final Statistic UDP_ALL_MESSAGES =
		new DroppedSentMessbgeStatBytes();

	/**
	 * <tt>Stbtistic</tt> for all TCP messages sent.
	 */
	public stbtic final Statistic TCP_ALL_MESSAGES =
		new DroppedSentMessbgeStatBytes();
		
	/**
	 * <tt>Stbtistic</tt> for all MULTICAST messages sent.
	 */
	public stbtic final Statistic MULTICAST_ALL_MESSAGES =
		new DroppedSentMessbgeStatBytes();		

	/**
	 * <tt>Stbtistic</tt> for all filtered messages.
	 */
	public stbtic final Statistic ALL_FILTERED_MESSAGES =
		new DroppedSentMessbgeStatBytes();

	/**
	 * <tt>Stbtistic</tt> for Gnutella pings sent over UDP.
	 */
	public stbtic final Statistic UDP_PING_REQUESTS = 
	    new UDPDroppedSentMessbgeStatBytes();

	/**
	 * <tt>Stbtistic</tt> for Gnutella pings sent over TCP.
	 */
	public stbtic final Statistic TCP_PING_REQUESTS = 
	    new TCPDroppedSentMessbgeStatBytes();
	    
	/**
	 * <tt>Stbtistic</tt> for Gnutella pings sent over MULTICAST.
	 */
	public stbtic final Statistic MULTICAST_PING_REQUESTS = 
	    new MulticbstDroppedSentMessageStatBytes();	    

	/**
	 * <tt>Stbtistic</tt> for Gnutella pongs sent over UDP.
	 */
	public stbtic final Statistic UDP_PING_REPLIES = 
	    new UDPDroppedSentMessbgeStatBytes();

	/**
	 * <tt>Stbtistic</tt> for Gnutella pongs sent over TCP.
	 */
	public stbtic final Statistic TCP_PING_REPLIES = 
	    new TCPDroppedSentMessbgeStatBytes();
	    
	/**
	 * <tt>Stbtistic</tt> for Gnutella pongs sent over MULTICAST.
	 */
	public stbtic final Statistic MULTICAST_PING_REPLIES = 
	    new MulticbstDroppedSentMessageStatBytes();	    

	/**
	 * <tt>Stbtistic</tt> for Gnutella query requests sent over 
	 * UDP.
	 */
	public stbtic final Statistic UDP_QUERY_REQUESTS = 
	    new UDPDroppedSentMessbgeStatBytes();

	/**
	 * <tt>Stbtistic</tt> for Gnutella query requests sent over 
	 * TCP.
	 */
	public stbtic final Statistic TCP_QUERY_REQUESTS = 
	    new TCPDroppedSentMessbgeStatBytes();
	    
	/**
	 * <tt>Stbtistic</tt> for Gnutella query requests sent over 
	 * Multicbst.
	 */
	public stbtic final Statistic MULTICAST_QUERY_REQUESTS = 
	    new MulticbstDroppedSentMessageStatBytes();
	    

	/**
	 * <tt>Stbtistic</tt> for Gnutella query replies sent over 
	 * UDP.
	 */
	public stbtic final Statistic UDP_QUERY_REPLIES = 
	    new UDPDroppedSentMessbgeStatBytes();

	/**
	 * <tt>Stbtistic</tt> for Gnutella query replies sent over 
	 * TCP.
	 */
	public stbtic final Statistic TCP_QUERY_REPLIES = 
	    new TCPDroppedSentMessbgeStatBytes();	    

	/**
	 * <tt>Stbtistic</tt> for Gnutella query replies sent over 
	 * Multicbst.
	 */
	public stbtic final Statistic MULTICAST_QUERY_REPLIES = 
	    new MulticbstDroppedSentMessageStatBytes();	    

	/**
	 * <tt>Stbtistic</tt> for Gnutella push requests sent over 
	 * UDP.
	 */
	public stbtic final Statistic UDP_PUSH_REQUESTS = 
	    new UDPDroppedSentMessbgeStatBytes();

	/**
	 * <tt>Stbtistic</tt> for Gnutella push requests sent over 
	 * TCP.
	 */
	public stbtic final Statistic TCP_PUSH_REQUESTS = 
	    new TCPDroppedSentMessbgeStatBytes();
	    
	/**
	 * <tt>Stbtistic</tt> for Gnutella push requests sent over 
	 * Multicbst.
	 */
	public stbtic final Statistic MULTICAST_PUSH_REQUESTS = 
	    new MulticbstDroppedSentMessageStatBytes();	    

	/**
	 * <tt>Stbtistic</tt> for Gnutella route table messages sent 
	 * over UDP.
	 */
	public stbtic final Statistic UDP_ROUTE_TABLE_MESSAGES = 
	    new UDPDroppedSentMessbgeStatBytes();

	/**
	 * <tt>Stbtistic</tt> for Gnutella reset route table messages sent 
	 * over TCP.
	 */
	public stbtic final Statistic TCP_RESET_ROUTE_TABLE_MESSAGES = 
	    new TCPDroppedSentMessbgeStatBytes();

	/**
	 * <tt>Stbtistic</tt> for Gnutella patch route table messages sent 
	 * over TCP.
	 */
	public stbtic final Statistic TCP_PATCH_ROUTE_TABLE_MESSAGES = 
	    new TCPDroppedSentMessbgeStatBytes();
	    
    /**
	 * <tt>Stbtistic</tt> for Gnutella route table messages sent 
	 * over Multicbst.
	 */
	public stbtic final Statistic MULTICAST_ROUTE_TABLE_MESSAGES = 
	    new MulticbstDroppedSentMessageStatBytes();	    
}
