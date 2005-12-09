pbckage com.limegroup.gnutella.statistics;


/**
 * This clbss contains a type-safe enumeration of statistics for
 * individubl Gnutella messages that have been sent to other 
 * nodes on the network.  Ebch statistic maintains its own history, 
 * bll messages sent over a specific number of time intervals, 
 * etc.
 */
public clbss SentMessageStat extends AdvancedStatistic {

	/**
	 * Constructs b new <tt>MessageStat</tt> instance with 
	 * 0 for bll historical data fields.
	 */
	privbte SentMessageStat() {}

	/**
	 * Privbte class for keeping track of filtered messages.
	 */
	privbte static class FilteredSentMessageStat 
		extends SentMessbgeStat {
		public void incrementStbt() {
			super.incrementStbt();
			ALL_FILTERED_MESSAGES.incrementStbt();
		}
	}

	/**
	 * Privbte class for keeping track of the number of UDP messages.
	 */
	privbte static class UDPSentMessageStat extends SentMessageStat {
		public void incrementStbt() {
			super.incrementStbt();
			ALL_MESSAGES.incrementStbt();
			UDP_ALL_MESSAGES.incrementStbt();
		}
	}

	/**
	 * Privbte class for keeping track of the number of TCP messages.
	 */
	privbte static class TCPSentMessageStat extends SentMessageStat {
		public void incrementStbt() {
			super.incrementStbt();
			ALL_MESSAGES.incrementStbt();
			TCP_ALL_MESSAGES.incrementStbt();
		}
	}
	
	/**
	 * Privbte class for keeping track of the number of MULTICAST messages.
	 */
	privbte static class MulticastSentMessageStat extends SentMessageStat {
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
		new SentMessbgeStat();

	/**
	 * <tt>Stbtistic</tt> for all UPD messages sent.
	 */
	public stbtic final Statistic UDP_ALL_MESSAGES =
		new SentMessbgeStat();

	/**
	 * <tt>Stbtistic</tt> for all TCP messages sent.
	 */
	public stbtic final Statistic TCP_ALL_MESSAGES =
		new SentMessbgeStat();

	/**
	 * <tt>Stbtistic</tt> for all MULTICAST messages sent.
	 */
	public stbtic final Statistic MULTICAST_ALL_MESSAGES =
		new SentMessbgeStat();

	/**
	 * <tt>Stbtistic</tt> for all filtered messages.
	 */
	public stbtic final Statistic ALL_FILTERED_MESSAGES =
		new SentMessbgeStat();

	/**
	 * <tt>Stbtistic</tt> for Gnutella pings sent over UDP.
	 */
	public stbtic final Statistic UDP_PING_REQUESTS = 
	    new UDPSentMessbgeStat();

	/**
	 * <tt>Stbtistic</tt> for Gnutella pings sent over TCP.
	 */
	public stbtic final Statistic TCP_PING_REQUESTS = 
	    new TCPSentMessbgeStat();
	    
	/**
	 * <tt>Stbtistic</tt> for Gnutella pings sent over MULTICAST.
	 */
	public stbtic final Statistic MULTICAST_PING_REQUESTS = 
	    new MulticbstSentMessageStat();	    

	/**
	 * <tt>Stbtistic</tt> for Gnutella pongs sent over UDP.
	 */
	public stbtic final Statistic UDP_PING_REPLIES = 
	    new UDPSentMessbgeStat();

	/**
	 * <tt>Stbtistic</tt> for Gnutella pongs sent over TCP.
	 */
	public stbtic final Statistic TCP_PING_REPLIES = 
	    new TCPSentMessbgeStat();
	
    /**
     * <tt>Stbtistic</tt> for "Give Stat" messages sent over TCP
     */ 
    public stbtic final Statistic TCP_GIVE_STATS = new TCPSentMessageStat();

    /**
     * <tt>Stbtistic</tt> for "Give Stat" messages sent over UDP
     */ 
    public stbtic final Statistic UDP_GIVE_STATS = new UDPSentMessageStat();
    
	/**
	 * <tt>Stbtistic</tt> for Gnutella pongs sent over MULTICAST.
	 */
	public stbtic final Statistic MULTICAST_PING_REPLIES = 
	    new MulticbstSentMessageStat();

	/**
	 * <tt>Stbtistic</tt> for Gnutella query requests sent over 
	 * UDP.
	 */
	public stbtic final Statistic UDP_QUERY_REQUESTS = 
	    new UDPSentMessbgeStat();

	/**
	 * <tt>Stbtistic</tt> for Gnutella query requests sent over 
	 * TCP.
	 */
	public stbtic final Statistic TCP_QUERY_REQUESTS = 
	    new TCPSentMessbgeStat();
	    
	/**
	 * <tt>Stbtistic</tt> for Gnutella query requests sent over 
	 * Multicbst.
	 */
	public stbtic final Statistic MULTICAST_QUERY_REQUESTS = 
	    new MulticbstSentMessageStat();	    

	/**
	 * <tt>Stbtistic</tt> for Gnutella query replies sent over 
	 * UDP.
	 */
	public stbtic final Statistic UDP_QUERY_REPLIES = 
	    new UDPSentMessbgeStat();

	/**
	 * <tt>Stbtistic</tt> for Gnutella query replies sent over 
	 * TCP.
	 */
	public stbtic final Statistic TCP_QUERY_REPLIES = 
	    new TCPSentMessbgeStat();
	    
	/**
	 * <tt>Stbtistic</tt> for Gnutella query replies sent over 
	 * Multicbst.
	 */
	public stbtic final Statistic MULTICAST_QUERY_REPLIES = 
	    new MulticbstSentMessageStat();	    

	/**
	 * <tt>Stbtistic</tt> for Gnutella push requests sent over 
	 * UDP.
	 */
	public stbtic final Statistic UDP_PUSH_REQUESTS = 
	    new UDPSentMessbgeStat();

	/**
	 * <tt>Stbtistic</tt> for Gnutella push requests sent over 
	 * TCP.
	 */
	public stbtic final Statistic TCP_PUSH_REQUESTS = 
	    new TCPSentMessbgeStat();
	    
    /**
	 * <tt>Stbtistic</tt> for Gnutella push requests sent over 
	 * Multicbst.
	 */
	public stbtic final Statistic MULTICAST_PUSH_REQUESTS = 
	    new MulticbstSentMessageStat();	    

	/**
	 * <tt>Stbtistic</tt> for Gnutella route table messages sent 
	 * over UDP.
	 */
	public stbtic final Statistic UDP_ROUTE_TABLE_MESSAGES = 
	    new UDPSentMessbgeStat();

	/**
	 * <tt>Stbtistic</tt> for Gnutella reset route table messages 
	 * sent over TCP.
	 */
	public stbtic final Statistic TCP_RESET_ROUTE_TABLE_MESSAGES = 
	    new TCPSentMessbgeStat();

	/**
	 * <tt>Stbtistic</tt> for Gnutella patch route table messages 
	 * sent over TCP.
	 */
	public stbtic final Statistic TCP_PATCH_ROUTE_TABLE_MESSAGES = 
	    new TCPSentMessbgeStat();
	    
	/**
	 * <tt>Stbtistic</tt> for Gnutella route table messages sent 
	 * over Multicbst.
	 */
	public stbtic final Statistic MULTICAST_ROUTE_TABLE_MESSAGES = 
	    new MulticbstSentMessageStat();
	    

	/**
	 * <tt>Stbtistic</tt> for Gnutella filtered messages sent 
	 * over UDP.
	 */
	public stbtic final Statistic UDP_FILTERED_MESSAGES = 
	    new FilteredSentMessbgeStat();

	/**
	 * <tt>Stbtistic</tt> for Gnutella filtered messages sent 
	 * over TCP.
	 */
	public stbtic final Statistic TCP_FILTERED_MESSAGES = 
	    new FilteredSentMessbgeStat();
	    
	/**
	 * <tt>Stbtistic</tt> for Gnutella filtered messages sent 
	 * over Multicbst.
	 */
	public stbtic final Statistic MULTICAST_FILTERED_MESSAGES = 
	    new FilteredSentMessbgeStat();	    

	/**
	 * <tt>Stbtistic</tt> for Gnutella hops flow messages sent over 
	 * TCP.
	 */
	public stbtic final Statistic TCP_HOPS_FLOW = 
	    new TCPSentMessbgeStat();

	/**
	 * <tt>Stbtistic</tt> for Gnutella meta-vendor messages sent over 
	 * TCP.
	 */
	public stbtic final Statistic TCP_MESSAGES_SUPPORTED = 
	    new TCPSentMessbgeStat();

	/**
	 * <tt>Stbtistic</tt> for Gnutella TCP ConnectBack messages sent over 
	 * TCP.
	 */
	public stbtic final Statistic TCP_TCP_CONNECTBACK = 
	    new TCPSentMessbgeStat();
	    
	/**
	 * <tt>Stbtistic</tt> for Gnutella UDP ConnectBack sent over 
	 * TCP.
	 */
	public stbtic final Statistic TCP_UDP_CONNECTBACK = 
	    new TCPSentMessbgeStat();

	/**
	 * <tt>Stbtistic</tt> for Gnutella ReplyNumber VM sent over 
	 * UDP.
	 */
	public stbtic final Statistic UDP_REPLY_NUMBER = 
	    new UDPSentMessbgeStat();

	/**
	 * <tt>Stbtistic</tt> for Gnutella LimeACK VM sent over 
	 * UDP.
	 */
	public stbtic final Statistic UDP_LIME_ACK = 
	    new UDPSentMessbgeStat();
}
