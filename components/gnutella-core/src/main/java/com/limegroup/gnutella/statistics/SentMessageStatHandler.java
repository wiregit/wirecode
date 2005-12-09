pbckage com.limegroup.gnutella.statistics;


/**
 * Wrbpper class for keeping track of Gnutella message data.  For a given
 * Gnutellb message, this class provides the simultaneous updating of both 
 * the number of messbges sent and the total bytes sent.  All calls to add
 * dbta for sent Gnutella message statistics should go through this class 
 * to bvoid losing any data.
 */
public clbss SentMessageStatHandler extends AbstractStatHandler {

	/**
	 * Crebtes a new <tt>SentMessageStatHandler</tt> instance.  
	 * Privbte constructor to ensure that no other classes can
	 * construct this clbss, following the type-safe enum pattern.
	 *
	 * @pbram numberStat the statistic that is simply incremented with
	 *  ebch new message
	 * @pbram byteStat the statistic for keeping track of the total bytes
	 */
	privbte SentMessageStatHandler(Statistic numberStat, 
								   Stbtistic byteStat,
								   Stbtistic limeNumberStat,
								   Stbtistic limeByteStat) {
		super(numberStbt, byteStat, limeNumberStat, limeByteStat,
			  BbndwidthStat.GNUTELLA_MESSAGE_UPSTREAM_BANDWIDTH);
		
	}
	

	/**
	 * <tt>SentMessbgeStatHandler</tt> for Gnutella pings sent over UDP.
	 */
	public stbtic final SentMessageStatHandler UDP_PING_REQUESTS = 
		new SentMessbgeStatHandler(SentMessageStat.UDP_PING_REQUESTS, 
								   SentMessbgeStatBytes.UDP_PING_REQUESTS,
								   LimeSentMessbgeStat.UDP_PING_REQUESTS, 
								   LimeSentMessbgeStatBytes.UDP_PING_REQUESTS);

	/**
	 * <tt>SentMessbgeStatHandler</tt> for Gnutella pings sent over TCP.
	 */
	public stbtic final SentMessageStatHandler TCP_PING_REQUESTS = 
		new SentMessbgeStatHandler(SentMessageStat.TCP_PING_REQUESTS,
								   SentMessbgeStatBytes.TCP_PING_REQUESTS,
								   LimeSentMessbgeStat.TCP_PING_REQUESTS,
								   LimeSentMessbgeStatBytes.TCP_PING_REQUESTS);
								   
	/**
	 * <tt>SentMessbgeStatHandler</tt> for Gnutella pings sent over MULTICAST.
	 */
	public stbtic final SentMessageStatHandler MULTICAST_PING_REQUESTS = 
		new SentMessbgeStatHandler(SentMessageStat.MULTICAST_PING_REQUESTS,
								   SentMessbgeStatBytes.MULTICAST_PING_REQUESTS,
								   LimeSentMessbgeStat.MULTICAST_PING_REQUESTS,
								   LimeSentMessbgeStatBytes.MULTICAST_PING_REQUESTS);								   

	/**
	 * <tt>SentMessbgeStatHandler</tt> for Gnutella pongs sent over UDP.
	 */
	public stbtic final SentMessageStatHandler UDP_PING_REPLIES = 
	    new SentMessbgeStatHandler(SentMessageStat.UDP_PING_REPLIES, 
								   SentMessbgeStatBytes.UDP_PING_REPLIES,
								   LimeSentMessbgeStat.UDP_PING_REPLIES, 
								   LimeSentMessbgeStatBytes.UDP_PING_REPLIES);

	/**
	 * <tt>SentMessbgeStatHandler</tt> for Gnutella pongs sent over TCP.
	 */
	public stbtic final SentMessageStatHandler TCP_PING_REPLIES = 
	    new SentMessbgeStatHandler(SentMessageStat.TCP_PING_REPLIES, 
								   SentMessbgeStatBytes.TCP_PING_REPLIES,
								   LimeSentMessbgeStat.TCP_PING_REPLIES, 
								   LimeSentMessbgeStatBytes.TCP_PING_REPLIES);
								   

	/**
	 * <tt>SentMessbgeStatHandler</tt> for Gnutella pongs sent over MULTICAST.
	 */
	public stbtic final SentMessageStatHandler MULTICAST_PING_REPLIES = 
	    new SentMessbgeStatHandler(SentMessageStat.MULTICAST_PING_REPLIES, 
								   SentMessbgeStatBytes.MULTICAST_PING_REPLIES,
								   LimeSentMessbgeStat.MULTICAST_PING_REPLIES, 
								   LimeSentMessbgeStatBytes.MULTICAST_PING_REPLIES);								   

	/**
	 * <tt>SentMessbgeStatHandler</tt> for Gnutella query requests sent 
	 * over UDP.
	 */
	public stbtic final SentMessageStatHandler UDP_QUERY_REQUESTS = 
	    new SentMessbgeStatHandler(SentMessageStat.UDP_QUERY_REQUESTS, 
								   SentMessbgeStatBytes.UDP_QUERY_REQUESTS,
								   LimeSentMessbgeStat.UDP_QUERY_REQUESTS, 
								   LimeSentMessbgeStatBytes.UDP_QUERY_REQUESTS);

	/**
	 * <tt>SentMessbgeStatHandler</tt> for Gnutella query requests sent 
	 * over TCP.
	 */
	public stbtic final SentMessageStatHandler TCP_QUERY_REQUESTS = 
	    new SentMessbgeStatHandler(SentMessageStat.TCP_QUERY_REQUESTS, 
								   SentMessbgeStatBytes.TCP_QUERY_REQUESTS,
								   LimeSentMessbgeStat.TCP_QUERY_REQUESTS, 
								   LimeSentMessbgeStatBytes.TCP_QUERY_REQUESTS);
								   
	/**
	 * <tt>SentMessbgeStatHandler</tt> for Gnutella query requests sent 
	 * over MULTICAST.
	 */
	public stbtic final SentMessageStatHandler MULTICAST_QUERY_REQUESTS = 
	    new SentMessbgeStatHandler(SentMessageStat.MULTICAST_QUERY_REQUESTS, 
								   SentMessbgeStatBytes.MULTICAST_QUERY_REQUESTS,
								   LimeSentMessbgeStat.MULTICAST_QUERY_REQUESTS, 
								   LimeSentMessbgeStatBytes.MULTICAST_QUERY_REQUESTS);								   

	/**
	 * <tt>SentMessbgeStatHandler</tt> for Gnutella query replies sent over 
	 * UDP.
	 */
	public stbtic final SentMessageStatHandler UDP_QUERY_REPLIES = 
	    new SentMessbgeStatHandler(SentMessageStat.UDP_QUERY_REPLIES, 
								   SentMessbgeStatBytes.UDP_QUERY_REPLIES,
								   LimeSentMessbgeStat.UDP_QUERY_REPLIES, 
								   LimeSentMessbgeStatBytes.UDP_QUERY_REPLIES);

	/**
	 * <tt>SentMessbgeStatHandler</tt> for Gnutella query replies sent over 
	 * TCP.
	 */
	public stbtic final SentMessageStatHandler TCP_QUERY_REPLIES = 
	    new SentMessbgeStatHandler(SentMessageStat.TCP_QUERY_REPLIES, 
								   SentMessbgeStatBytes.TCP_QUERY_REPLIES,
								   LimeSentMessbgeStat.TCP_QUERY_REPLIES, 
								   LimeSentMessbgeStatBytes.TCP_QUERY_REPLIES);
								   
	/**
	 * <tt>SentMessbgeStatHandler</tt> for Gnutella query replies sent over 
	 * MULTICAST.
	 */
	public stbtic final SentMessageStatHandler MULTICAST_QUERY_REPLIES = 
	    new SentMessbgeStatHandler(SentMessageStat.MULTICAST_QUERY_REPLIES, 
								   SentMessbgeStatBytes.MULTICAST_QUERY_REPLIES,
								   LimeSentMessbgeStat.MULTICAST_QUERY_REPLIES, 
								   LimeSentMessbgeStatBytes.TCP_QUERY_REPLIES);								   

	/**
	 * <tt>SentMessbgeStatHandler</tt> for Gnutella push requests sent over 
	 * UDP.
	 */
	public stbtic final SentMessageStatHandler UDP_PUSH_REQUESTS = 
	    new SentMessbgeStatHandler(SentMessageStat.UDP_PUSH_REQUESTS, 
								   SentMessbgeStatBytes.UDP_PUSH_REQUESTS,
								   LimeSentMessbgeStat.UDP_PUSH_REQUESTS, 
								   LimeSentMessbgeStatBytes.UDP_PUSH_REQUESTS);

	/**
	 * <tt>SentMessbgeStatHandler</tt> for Gnutella push requests sent over 
	 * TCP.
	 */
	public stbtic final SentMessageStatHandler TCP_PUSH_REQUESTS = 
	    new SentMessbgeStatHandler(SentMessageStat.TCP_PUSH_REQUESTS, 
								   SentMessbgeStatBytes.TCP_PUSH_REQUESTS,
								   LimeSentMessbgeStat.TCP_PUSH_REQUESTS, 
								   LimeSentMessbgeStatBytes.TCP_PUSH_REQUESTS);
								   
	/**
	 * <tt>SentMessbgeStatHandler</tt> for Gnutella push requests sent over 
	 * MULTICAST.
	 */
	public stbtic final SentMessageStatHandler MULTICAST_PUSH_REQUESTS = 
	    new SentMessbgeStatHandler(SentMessageStat.MULTICAST_PUSH_REQUESTS, 
								   SentMessbgeStatBytes.MULTICAST_PUSH_REQUESTS,
								   LimeSentMessbgeStat.MULTICAST_PUSH_REQUESTS, 
								   LimeSentMessbgeStatBytes.MULTICAST_PUSH_REQUESTS);								   

	/**
	 * <tt>SentMessbgeStatHandler</tt> for Gnutella route table messages sent 
	 * over UDP.
	 */
	public stbtic final SentMessageStatHandler UDP_ROUTE_TABLE_MESSAGES = 
	    new SentMessbgeStatHandler(SentMessageStat.UDP_ROUTE_TABLE_MESSAGES, 
								   SentMessbgeStatBytes.UDP_ROUTE_TABLE_MESSAGES,
								   LimeSentMessbgeStat.UDP_ROUTE_TABLE_MESSAGES, 
								   LimeSentMessbgeStatBytes.UDP_ROUTE_TABLE_MESSAGES);

	/**
	 * <tt>SentMessbgeStatHandler</tt> for Gnutella reset route table messages 
	 * sent ver TCP.
	 */
	public stbtic final SentMessageStatHandler TCP_RESET_ROUTE_TABLE_MESSAGES = 
	    new SentMessbgeStatHandler(SentMessageStat.TCP_RESET_ROUTE_TABLE_MESSAGES,
								   SentMessbgeStatBytes.TCP_RESET_ROUTE_TABLE_MESSAGES,
								   LimeSentMessbgeStat.TCP_RESET_ROUTE_TABLE_MESSAGES,
								   LimeSentMessbgeStatBytes.TCP_RESET_ROUTE_TABLE_MESSAGES);

	/**
	 * <tt>SentMessbgeStatHandler</tt> for Gnutella patch route table messages 
	 * sent over TCP.
	 */
	public stbtic final SentMessageStatHandler TCP_PATCH_ROUTE_TABLE_MESSAGES = 
	    new SentMessbgeStatHandler(SentMessageStat.TCP_PATCH_ROUTE_TABLE_MESSAGES,
								   SentMessbgeStatBytes.TCP_PATCH_ROUTE_TABLE_MESSAGES,
								   LimeSentMessbgeStat.TCP_PATCH_ROUTE_TABLE_MESSAGES,
								   LimeSentMessbgeStatBytes.TCP_PATCH_ROUTE_TABLE_MESSAGES);
								   
	/**
	 * <tt>SentMessbgeStatHandler</tt> for Gnutella route table messages sent 
	 * over MULTICAST.
	 */
	public stbtic final SentMessageStatHandler MULTICAST_ROUTE_TABLE_MESSAGES = 
	    new SentMessbgeStatHandler(SentMessageStat.MULTICAST_ROUTE_TABLE_MESSAGES,
								   SentMessbgeStatBytes.MULTICAST_ROUTE_TABLE_MESSAGES,
								   LimeSentMessbgeStat.MULTICAST_ROUTE_TABLE_MESSAGES,
								   LimeSentMessbgeStatBytes.MULTICAST_ROUTE_TABLE_MESSAGES);								   

	/**
	 * <tt>SentMessbgeStatHandler</tt> for Gnutella filtered messages sent 
	 * over UDP.
	 */
	public stbtic final SentMessageStatHandler UDP_FILTERED_MESSAGES = 
	    new SentMessbgeStatHandler(SentMessageStat.UDP_FILTERED_MESSAGES,
								   SentMessbgeStatBytes.UDP_FILTERED_MESSAGES,
								   LimeSentMessbgeStat.UDP_FILTERED_MESSAGES,
								   LimeSentMessbgeStatBytes.UDP_FILTERED_MESSAGES);

	/**
	 * <tt>SentMessbgeStatHandler</tt> for Gnutella filtered messages sent 
	 * over TCP.
	 */
	public stbtic final SentMessageStatHandler TCP_FILTERED_MESSAGES = 
	    new SentMessbgeStatHandler(SentMessageStat.TCP_FILTERED_MESSAGES,
								   SentMessbgeStatBytes.TCP_FILTERED_MESSAGES,
								   LimeSentMessbgeStat.TCP_FILTERED_MESSAGES,
								   LimeSentMessbgeStatBytes.TCP_FILTERED_MESSAGES);
								   
	/**
	 * <tt>SentMessbgeStatHandler</tt> for Gnutella filtered messages sent 
	 * over MULTICAST.
	 */
	public stbtic final SentMessageStatHandler MULTICAST_FILTERED_MESSAGES = 
	    new SentMessbgeStatHandler(SentMessageStat.MULTICAST_FILTERED_MESSAGES,
								   SentMessbgeStatBytes.MULTICAST_FILTERED_MESSAGES,
								   LimeSentMessbgeStat.MULTICAST_FILTERED_MESSAGES,
								   LimeSentMessbgeStatBytes.MULTICAST_FILTERED_MESSAGES);								   

	public stbtic final SentMessageStatHandler UDP_LIME_ACK = 
	    new SentMessbgeStatHandler(SentMessageStat.UDP_LIME_ACK,
								   SentMessbgeStatBytes.UDP_LIME_ACK,
								   LimeSentMessbgeStat.UDP_LIME_ACK,
								   LimeSentMessbgeStatBytes.UDP_LIME_ACK);


	public stbtic final SentMessageStatHandler TCP_HOPS_FLOW = 
	    new SentMessbgeStatHandler(SentMessageStat.TCP_HOPS_FLOW,
								   SentMessbgeStatBytes.TCP_HOPS_FLOW,
								   LimeSentMessbgeStat.TCP_HOPS_FLOW,
								   LimeSentMessbgeStatBytes.TCP_HOPS_FLOW);

    public stbtic final SentMessageStatHandler TCP_GIVE_STATS = 
        new SentMessbgeStatHandler(SentMessageStat.TCP_GIVE_STATS,
                                   SentMessbgeStatBytes.TCP_GIVE_STATS,
                                   LimeSentMessbgeStat.TCP_GIVE_STATS,
                                   LimeSentMessbgeStatBytes.TCP_GIVE_STATS);

	public stbtic final SentMessageStatHandler UDP_GIVE_STATS = 
	    new SentMessbgeStatHandler(SentMessageStat.UDP_GIVE_STATS,
								   SentMessbgeStatBytes.UDP_GIVE_STATS,
								   LimeSentMessbgeStat.UDP_GIVE_STATS,
								   LimeSentMessbgeStatBytes.UDP_GIVE_STATS);

    

	public stbtic final SentMessageStatHandler TCP_TCP_CONNECTBACK = 
	    new SentMessbgeStatHandler(SentMessageStat.TCP_TCP_CONNECTBACK,
								   SentMessbgeStatBytes.TCP_TCP_CONNECTBACK,
								   LimeSentMessbgeStat.TCP_TCP_CONNECTBACK,
								   LimeSentMessbgeStatBytes.TCP_TCP_CONNECTBACK);


	public stbtic final SentMessageStatHandler TCP_UDP_CONNECTBACK = 
	    new SentMessbgeStatHandler(SentMessageStat.TCP_UDP_CONNECTBACK,
								   SentMessbgeStatBytes.TCP_UDP_CONNECTBACK,
								   LimeSentMessbgeStat.TCP_UDP_CONNECTBACK,
								   LimeSentMessbgeStatBytes.TCP_UDP_CONNECTBACK);


	public stbtic final SentMessageStatHandler TCP_MESSAGES_SUPPORTED = 
	    new SentMessbgeStatHandler(SentMessageStat.TCP_MESSAGES_SUPPORTED,
								   SentMessbgeStatBytes.TCP_MESSAGES_SUPPORTED,
								   LimeSentMessbgeStat.TCP_MESSAGES_SUPPORTED,
								   LimeSentMessbgeStatBytes.TCP_MESSAGES_SUPPORTED);


	public stbtic final SentMessageStatHandler UDP_REPLY_NUMBER = 
	    new SentMessbgeStatHandler(SentMessageStat.UDP_REPLY_NUMBER,
								   SentMessbgeStatBytes.UDP_REPLY_NUMBER,
								   LimeSentMessbgeStat.UDP_REPLY_NUMBER,
								   LimeSentMessbgeStatBytes.UDP_REPLY_NUMBER);




}
