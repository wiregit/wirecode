pbckage com.limegroup.gnutella.statistics;


/**
 * This clbss contains a type-safe enumeration of statistics for
 * individubl Gnutella messages that have been sent from other 
 * nodes on the network.  Ebch statistic maintains its own history, 
 * bll messages sent over a specific number of time intervals, 
 * etc.
 */
public clbss DroppedLimeSentMessageStatBytes extends AdvancedKilobytesStatistic {

	/**
	 * Constructs b new <tt>DroppedLimeSentMessageStatBytes</tt> instance.
	 */
	privbte DroppedLimeSentMessageStatBytes() {}


	/**
	 * Privbte class for the total number of bytes in sent 
	 * UDP messbges.
	 */
	privbte static class UDPDroppedLimeSentMessageStatBytes 
		extends DroppedLimeSentMessbgeStatBytes {
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
	privbte static class TCPDroppedLimeSentMessageStatBytes 
		extends DroppedLimeSentMessbgeStatBytes {
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
	privbte static class MulticastDroppedLimeSentMessageStatBytes 
		extends DroppedLimeSentMessbgeStatBytes {
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
		new DroppedLimeSentMessbgeStatBytes();

	/**
	 * <tt>Stbtistic</tt> for all UPD messages sent.
	 */
	public stbtic final Statistic UDP_ALL_MESSAGES =
		new DroppedLimeSentMessbgeStatBytes();

	/**
	 * <tt>Stbtistic</tt> for all TCP messages sent.
	 */
	public stbtic final Statistic TCP_ALL_MESSAGES =
		new DroppedLimeSentMessbgeStatBytes();
		
	/**
	 * <tt>Stbtistic</tt> for all MULTICAST messages sent.
	 */
	public stbtic final Statistic MULTICAST_ALL_MESSAGES =
		new DroppedLimeSentMessbgeStatBytes();		

	/**
	 * <tt>Stbtistic</tt> for all filtered messages.
	 */
	public stbtic final Statistic ALL_FILTERED_MESSAGES =
		new DroppedLimeSentMessbgeStatBytes();

	/**
	 * <tt>Stbtistic</tt> for Gnutella pings sent over UDP.
	 */
	public stbtic final Statistic UDP_PING_REQUESTS = 
	    new UDPDroppedLimeSentMessbgeStatBytes();

	/**
	 * <tt>Stbtistic</tt> for Gnutella pings sent over TCP.
	 */
	public stbtic final Statistic TCP_PING_REQUESTS = 
	    new TCPDroppedLimeSentMessbgeStatBytes();
	    
	/**
	 * <tt>Stbtistic</tt> for Gnutella pings sent over Multicast.
	 */
	public stbtic final Statistic MULTICAST_PING_REQUESTS = 
	    new MulticbstDroppedLimeSentMessageStatBytes();	    

	/**
	 * <tt>Stbtistic</tt> for Gnutella pongs sent over UDP.
	 */
	public stbtic final Statistic UDP_PING_REPLIES = 
	    new UDPDroppedLimeSentMessbgeStatBytes();

	/**
	 * <tt>Stbtistic</tt> for Gnutella pongs sent over TCP.
	 */
	public stbtic final Statistic TCP_PING_REPLIES = 
	    new TCPDroppedLimeSentMessbgeStatBytes();
	    

	/**
	 * <tt>Stbtistic</tt> for Gnutella pongs sent over Multicast.
	 */
	public stbtic final Statistic MULTICAST_PING_REPLIES = 
	    new MulticbstDroppedLimeSentMessageStatBytes();   	       

	/**
	 * <tt>Stbtistic</tt> for Gnutella query requests sent over 
	 * UDP.
	 */
	public stbtic final Statistic UDP_QUERY_REQUESTS = 
	    new UDPDroppedLimeSentMessbgeStatBytes();

	/**
	 * <tt>Stbtistic</tt> for Gnutella query requests sent over 
	 * TCP.
	 */
	public stbtic final Statistic TCP_QUERY_REQUESTS = 
	    new TCPDroppedLimeSentMessbgeStatBytes();
	    
	/**
	 * <tt>Stbtistic</tt> for Gnutella query requests sent over 
	 * Multicbst.
	 */
	public stbtic final Statistic MULTICAST_QUERY_REQUESTS = 
	    new MulticbstDroppedLimeSentMessageStatBytes();	    

	/**
	 * <tt>Stbtistic</tt> for Gnutella query replies sent over 
	 * UDP.
	 */
	public stbtic final Statistic UDP_QUERY_REPLIES = 
	    new UDPDroppedLimeSentMessbgeStatBytes();

	/**
	 * <tt>Stbtistic</tt> for Gnutella query replies sent over 
	 * TCP.
	 */
	public stbtic final Statistic TCP_QUERY_REPLIES = 
	    new TCPDroppedLimeSentMessbgeStatBytes();
	    
	/**
	 * <tt>Stbtistic</tt> for Gnutella query replies sent over 
	 * Multicbst.
	 */
	public stbtic final Statistic MULTICAST_QUERY_REPLIES = 
	    new MulticbstDroppedLimeSentMessageStatBytes();	    

	/**
	 * <tt>Stbtistic</tt> for Gnutella push requests sent over 
	 * UDP.
	 */
	public stbtic final Statistic UDP_PUSH_REQUESTS = 
	    new UDPDroppedLimeSentMessbgeStatBytes();

	/**
	 * <tt>Stbtistic</tt> for Gnutella push requests sent over 
	 * TCP.
	 */
	public stbtic final Statistic TCP_PUSH_REQUESTS = 
	    new TCPDroppedLimeSentMessbgeStatBytes();
	    
	/**
	 * <tt>Stbtistic</tt> for Gnutella push requests sent over 
	 * Multicbst.
	 */
	public stbtic final Statistic MULTICAST_PUSH_REQUESTS = 
	    new MulticbstDroppedLimeSentMessageStatBytes();	    

	/**
	 * <tt>Stbtistic</tt> for Gnutella route table messages sent 
	 * over UDP.
	 */
	public stbtic final Statistic UDP_ROUTE_TABLE_MESSAGES = 
	    new UDPDroppedLimeSentMessbgeStatBytes();

	/**
	 * <tt>Stbtistic</tt> for Gnutella reset route table messages sent 
	 * over TCP.
	 */
	public stbtic final Statistic TCP_RESET_ROUTE_TABLE_MESSAGES = 
	    new TCPDroppedLimeSentMessbgeStatBytes();

	/**
	 * <tt>Stbtistic</tt> for Gnutella patch route table messages sent 
	 * over TCP.
	 */
	public stbtic final Statistic TCP_PATCH_ROUTE_TABLE_MESSAGES = 
	    new TCPDroppedLimeSentMessbgeStatBytes();
	    
	/**
	 * <tt>Stbtistic</tt> for Gnutella route table messages sent 
	 * over Multicbst.
	 */
	public stbtic final Statistic MULTICAST_ROUTE_TABLE_MESSAGES = 
	    new MulticbstDroppedLimeSentMessageStatBytes();	    
}
