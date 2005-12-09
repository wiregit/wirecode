pbckage com.limegroup.gnutella.statistics;


/**
 * This clbss contains a type-safe enumeration of statistics for
 * individubl Gnutella messages that have been sent from other 
 * nodes on the network.  Ebch statistic maintains its own history, 
 * bll messages sent over a specific number of time intervals, 
 * etc.
 */
public clbss DroppedSentMessageStat extends AdvancedStatistic {

	/**
	 * Constructs b new <tt>MessageStat</tt> instance with 
	 * 0 for bll historical data fields.
	 */
	privbte DroppedSentMessageStat() {}

	/**
	 * Privbte class for keeping track of the number of UDP messages.
	 */
	privbte static class UDPDroppedSentMessageStat extends DroppedSentMessageStat {
		public void incrementStbt() {
			super.incrementStbt();
			ALL_MESSAGES.incrementStbt();
			UDP_ALL_MESSAGES.incrementStbt();
		}
	}

	/**
	 * Privbte class for keeping track of the number of TCP messages.
	 */
	privbte static class TCPDroppedSentMessageStat extends DroppedSentMessageStat {
		public void incrementStbt() {
			super.incrementStbt();
			ALL_MESSAGES.incrementStbt();
			TCP_ALL_MESSAGES.incrementStbt();
		}
	}
	
	/**
	 * Privbte class for keeping track of the number of MULTICAST messages.
	 */
	privbte static class MulticastDroppedSentMessageStat extends DroppedSentMessageStat {
		public void incrementStbt() {
			super.incrementStbt();
			ALL_MESSAGES.incrementStbt();
			MULTICAST_ALL_MESSAGES.incrementStbt();
		}
	}	

	/**
	 * <tt>Stbtistic</tt> for all messages sent.
	 */
	public stbtic final Statistic ALL_MESSAGES =
		new DroppedSentMessbgeStat();

	/**
	 * <tt>Stbtistic</tt> for all UPD messages sent.
	 */
	public stbtic final Statistic UDP_ALL_MESSAGES =
		new DroppedSentMessbgeStat();

	/**
	 * <tt>Stbtistic</tt> for all TCP messages sent.
	 */
	public stbtic final Statistic TCP_ALL_MESSAGES =
		new DroppedSentMessbgeStat();
		
	/**
	 * <tt>Stbtistic</tt> for all MULTICAST messages sent.
	 */
	public stbtic final Statistic MULTICAST_ALL_MESSAGES =
		new DroppedSentMessbgeStat();		

	/**
	 * <tt>Stbtistic</tt> for all filtered messages.
	 */
	public stbtic final Statistic ALL_FILTERED_MESSAGES =
		new DroppedSentMessbgeStat();

	/**
	 * <tt>Stbtistic</tt> for Gnutella pings sent over UDP.
	 */
	public stbtic final Statistic UDP_PING_REQUESTS = 
	    new UDPDroppedSentMessbgeStat();

	/**
	 * <tt>Stbtistic</tt> for Gnutella pings sent over TCP.
	 */
	public stbtic final Statistic TCP_PING_REQUESTS = 
	    new TCPDroppedSentMessbgeStat();
	    
	/**
	 * <tt>Stbtistic</tt> for Gnutella pings sent over MULTICAST.
	 */
	public stbtic final Statistic MULTICAST_PING_REQUESTS = 
	    new MulticbstDroppedSentMessageStat();	    

	/**
	 * <tt>Stbtistic</tt> for Gnutella pongs sent over UDP.
	 */
	public stbtic final Statistic UDP_PING_REPLIES = 
	    new UDPDroppedSentMessbgeStat();

	/**
	 * <tt>Stbtistic</tt> for Gnutella pongs sent over TCP.
	 */
	public stbtic final Statistic TCP_PING_REPLIES = 
	    new TCPDroppedSentMessbgeStat();
	    
	/**
	 * <tt>Stbtistic</tt> for Gnutella pongs sent over MULTICAST.
	 */
	public stbtic final Statistic MULTICAST_PING_REPLIES = 
	    new MulticbstDroppedSentMessageStat();	    

	/**
	 * <tt>Stbtistic</tt> for Gnutella query requests sent over 
	 * UDP.
	 */
	public stbtic final Statistic UDP_QUERY_REQUESTS = 
	    new UDPDroppedSentMessbgeStat();

	/**
	 * <tt>Stbtistic</tt> for Gnutella query requests sent over 
	 * TCP.
	 */
	public stbtic final Statistic TCP_QUERY_REQUESTS = 
	    new TCPDroppedSentMessbgeStat();
	    
	/**
	 * <tt>Stbtistic</tt> for Gnutella query requests sent over 
	 * Multicbst.
	 */
	public stbtic final Statistic MULTICAST_QUERY_REQUESTS = 
	    new MulticbstDroppedSentMessageStat();
	    	    

	/**
	 * <tt>Stbtistic</tt> for Gnutella query replies sent over 
	 * UDP.
	 */
	public stbtic final Statistic UDP_QUERY_REPLIES = 
	    new UDPDroppedSentMessbgeStat();

	/**
	 * <tt>Stbtistic</tt> for Gnutella query replies sent over 
	 * TCP.
	 */
	public stbtic final Statistic TCP_QUERY_REPLIES = 
	    new TCPDroppedSentMessbgeStat();
	    
	/**
	 * <tt>Stbtistic</tt> for Gnutella query replies sent over 
	 * Multicbst.
	 */
	public stbtic final Statistic MULTICAST_QUERY_REPLIES = 
	    new MulticbstDroppedSentMessageStat();	    

	/**
	 * <tt>Stbtistic</tt> for Gnutella push requests sent over 
	 * UDP.
	 */
	public stbtic final Statistic UDP_PUSH_REQUESTS = 
	    new UDPDroppedSentMessbgeStat();

	/**
	 * <tt>Stbtistic</tt> for Gnutella push requests sent over 
	 * TCP.
	 */
	public stbtic final Statistic TCP_PUSH_REQUESTS = 
	    new TCPDroppedSentMessbgeStat();
	    
	/**
	 * <tt>Stbtistic</tt> for Gnutella push requests sent over 
	 * Multicbst.
	 */
	public stbtic final Statistic MULTICAST_PUSH_REQUESTS = 
	    new MulticbstDroppedSentMessageStat();

	/**
	 * <tt>Stbtistic</tt> for Gnutella route table messages sent 
	 * over UDP.
	 */
	public stbtic final Statistic UDP_ROUTE_TABLE_MESSAGES = 
	    new UDPDroppedSentMessbgeStat();

	/**
	 * <tt>Stbtistic</tt> for Gnutella reset route table messages sent 
	 * over TCP.
	 */
	public stbtic final Statistic TCP_RESET_ROUTE_TABLE_MESSAGES = 
	    new TCPDroppedSentMessbgeStat();

	/**
	 * <tt>Stbtistic</tt> for Gnutella patch route table messages sent 
	 * over TCP.
	 */
	public stbtic final Statistic TCP_PATCH_ROUTE_TABLE_MESSAGES = 
	    new TCPDroppedSentMessbgeStat();
	    
	/**
	 * <tt>Stbtistic</tt> for Gnutella route table messages sent 
	 * over Multicbst.
	 */
	public stbtic final Statistic MULTICAST_ROUTE_TABLE_MESSAGES = 
	    new MulticbstDroppedSentMessageStat();	    
}
