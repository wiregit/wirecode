package com.limegroup.gnutella.statistics;


/**
 * This class contains a type-safe enumeration of statistics for
 * individual Gnutella messages that have been received from other 
 * nodes on the network.  Each statistic maintains its own history, 
 * all messages received over a specific number of time intervals, 
 * etc.
 */
public class ReceivedMessageStat extends AdvancedStatistic {

	/**
	 * Constructs a new <tt>MessageStat</tt> instance.  Private to
     * ensure that only this class can construct new instances.
	 */
	private ReceivedMessageStat() {}

	/**
	 * Private class for keeping track of filtered messages.
	 */
	private static class FilteredReceivedMessageStat 
		extends ReceivedMessageStat {
		public void incrementStat() {
			super.incrementStat();
			ALL_FILTERED_MESSAGES.incrementStat();
		}
	}

	/**
	 * Private class for keeping track of duplicate queries.
	 */
	private static class DuplicateQueriesReceivedMessageStat
		extends ReceivedMessageStat {
		public void incrementStat() {
			super.incrementStat();
			ALL_DUPLICATE_QUERIES.incrementStat();
		}
	}

	/**
	 * Private class for keeping track of the number of UDP messages.
	 */
	private static class UDPReceivedMessageStat extends ReceivedMessageStat {
		public void incrementStat() {
			super.incrementStat();
			ALL_MESSAGES.incrementStat();
			UDP_ALL_MESSAGES.incrementStat();
		}
	}

	/**
	 * Private class for keeping track of the number of TCP messages.
	 */
	private static class TCPReceivedMessageStat extends ReceivedMessageStat {
		public void incrementStat() {
			super.incrementStat();
			ALL_MESSAGES.incrementStat();
			TCP_ALL_MESSAGES.incrementStat();
		}
	}
	
	/**
	 * Private class for keeping track of the number of Multicast messages.
	 */
	private static class MulticastReceivedMessageStat
	    extends ReceivedMessageStat {
        public void incrementStat() {
            super.incrementStat();
            MULTICAST_ALL_MESSAGES.incrementStat();
        }
    }


	/**
	 * <tt>Statistic</tt> for all messages received.
	 */
	public static final Statistic ALL_MESSAGES =
		new ReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for all UPD messages received.
	 */
	public static final Statistic UDP_ALL_MESSAGES =
		new ReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for all TCP messages received.
	 */
	public static final Statistic TCP_ALL_MESSAGES =
		new ReceivedMessageStat();

    /**
     * <tt>Statistic for all Multicast messages recieved.
     */
    public static final Statistic MULTICAST_ALL_MESSAGES =
        new ReceivedMessageStat();


    /**
     * <tt>Statistic for all 'What is New' queries recieved.
     */
    public static final Statistic WHAT_IS_NEW_QUERY_MESSAGES =
        new ReceivedMessageStat();


	/**
	 * <tt>Statistic</tt> for all filtered messages.
	 */
	public static final Statistic ALL_FILTERED_MESSAGES =
		new ReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for all duplicate quereies.
	 */
	public static final Statistic ALL_DUPLICATE_QUERIES =
		new ReceivedMessageStat();



	/////// individual message stats ///////

	/**
	 * <tt>Statistic</tt> for Gnutella pings received over UDP.
	 */
	public static final Statistic UDP_PING_REQUESTS = 
	    new UDPReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella pings received over TCP.
	 */
	public static final Statistic TCP_PING_REQUESTS = 
	    new TCPReceivedMessageStat();
	    
    /**
     * <tt>Statistic</tt> for Gnutella pings recieved over Multicast.
     */
    public static final Statistic MULTICAST_PING_REQUESTS =
        new MulticastReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella pongs received over UDP.
	 */
	public static final Statistic UDP_PING_REPLIES = 
	    new UDPReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella pongs received over TCP.
	 */
	public static final Statistic TCP_PING_REPLIES = 
	    new TCPReceivedMessageStat();
	    
    /**
     * <tt>Statistic</tt> for Gnutella pongs recieved over Multicast.
     */
    public static final Statistic MULTICAST_PING_REPLIES =
        new MulticastReceivedMessageStat();	    

	/**
	 * <tt>Statistic</tt> for Gnutella query requests received over 
	 * UDP.
	 */
	public static final Statistic UDP_QUERY_REQUESTS = 
	    new UDPReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella query requests received over 
	 * TCP.
	 */
	public static final Statistic TCP_QUERY_REQUESTS = 
	    new TCPReceivedMessageStat();
	    
    /**
     * <tt>Statistic</tt> for Gnutella query requests recieved over
     * Multicast.
     */
    public static final Statistic MULTICAST_QUERY_REQUESTS =
        new MulticastReceivedMessageStat();	    

	/**
	 * <tt>Statistic</tt> for Gnutella query replies received over 
	 * UDP.
	 */
	public static final Statistic UDP_QUERY_REPLIES = 
	    new UDPReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella query replies received over 
	 * TCP.
	 */
	public static final Statistic TCP_QUERY_REPLIES = 
	    new TCPReceivedMessageStat();
	    
    /**
     * <tt>Statistic</tt> for Gnutella query replies recieved over
     * Multicast.
     */
    public static final Statistic MULTICAST_QUERY_REPLIES =
        new MulticastReceivedMessageStat();	    

	/**
	 * <tt>Statistic</tt> for Gnutella push requests received over 
	 * UDP.
	 */
	public static final Statistic UDP_PUSH_REQUESTS = 
	    new UDPReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella push requests received over 
	 * TCP.
	 */
	public static final Statistic TCP_PUSH_REQUESTS = 
	    new TCPReceivedMessageStat();
	    
    /**
     * <tt>Statistic</tt> for Gnutella push requests received over
     * Multicast
     */
    public static final Statistic MULTICAST_PUSH_REQUESTS =
        new MulticastReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella route table messages received 
	 * over UDP.
	 */
	public static final Statistic UDP_ROUTE_TABLE_MESSAGES = 
	    new UDPReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella reset route table messages received 
	 * over TCP.
	 */
	public static final Statistic TCP_RESET_ROUTE_TABLE_MESSAGES = 
	    new TCPReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella patch route table messages received 
	 * over TCP.
	 */
	public static final Statistic TCP_PATCH_ROUTE_TABLE_MESSAGES = 
	    new TCPReceivedMessageStat();
	    
    /**
     * <tt>Statistic</tt> for Gnutella route table messages received
     * over Multicast.
     */
    public static final Statistic MULTICAST_ROUTE_TABLE_MESSAGES =
        new MulticastReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella filtered messages received 
	 * over UDP.
	 */
	public static final Statistic UDP_FILTERED_MESSAGES = 
	    new FilteredReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella filtered messages received 
	 * over TCP.
	 */
	public static final Statistic TCP_FILTERED_MESSAGES = 
	    new FilteredReceivedMessageStat();
	    
    /**
     * <tt>Statistic</tt> for Gnutella filter messages recieved
     * over Multicast.
     */
    public static final Statistic MULTICAST_FILTERED_MESSAGES =
        new FilteredReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for duplicate Gnutella queries received 
	 * over UDP.
	 */	
	public static final Statistic UDP_DUPLICATE_QUERIES =
		new DuplicateQueriesReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for duplicate Gnutella queries received 
	 * over TCP.
	 */	
	public static final Statistic TCP_DUPLICATE_QUERIES =
		new DuplicateQueriesReceivedMessageStat();
		
    /**
     * <tt>Statistic</tt> for duplicate Gnutella queries received
     * over Multicast
     */
    public static final Statistic MULTICAST_DUPLICATE_QUERIES =
        new DuplicateQueriesReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella hops flow messages received over 
	 * TCP.
	 */
	public static final Statistic TCP_HOPS_FLOW = 
	    new TCPReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella meta-vendor messages received over 
	 * TCP.
	 */
	public static final Statistic TCP_MESSAGES_SUPPORTED = 
	    new TCPReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella TCP ConnectBack messages received over 
	 * TCP.
	 */
	public static final Statistic TCP_TCP_CONNECTBACK = 
	    new TCPReceivedMessageStat();
	    
	/**
	 * <tt>Statistic</tt> for Gnutella UDP ConnectBack received over 
	 * TCP.
	 */
	public static final Statistic TCP_UDP_CONNECTBACK = 
	    new TCPReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella ReplyNumber VM received over 
	 * UDP.
	 */
	public static final Statistic UDP_REPLY_NUMBER = 
	    new UDPReceivedMessageStat();

	/**
	 * <tt>Statistic</tt> for Gnutella LimeACK VM received over 
	 * UDP.
	 */
	public static final Statistic UDP_LIME_ACK = 
	    new UDPReceivedMessageStat();
    
    /**
     * <tt>Statistic</tt> for Mojito DHT messages received over UDP
     */
    public static final Statistic UDP_DHT_MSG = 
        new UDPReceivedMessageStat();
}
