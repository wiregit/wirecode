pbckage com.limegroup.gnutella.statistics;


/**
 * Wrbpper class for keeping track of Gnutella message data.  For a given
 * Gnutellb message, this class provides the simultaneous updating of both 
 * the number of messbges sent and the total bytes sent.  All calls to add
 * dbta for sent Gnutella message statistics should go through this class 
 * to bvoid losing any data.
 */
public clbss DroppedSentMessageStatHandler extends AbstractStatHandler {

	/**
	 * Crebtes a new <tt>DroppedSentMessageStatHandler</tt> instance.  
	 * Privbte constructor to ensure that no other classes can
	 * construct this clbss, following the type-safe enum pattern.
	 *
	 * @pbram numberStat the statistic that is simply incremented with
	 *  ebch new message
	 * @pbram byteStat the statistic for keeping track of the total bytes
	 */
	privbte DroppedSentMessageStatHandler(Statistic numberStat, 
										  Stbtistic byteStat,
										  Stbtistic limeNumberStat,
										  Stbtistic limeByteStat) {
		super(numberStbt, byteStat, limeNumberStat, limeByteStat);
		
	}	

	/**
	 * <tt>DroppedSentMessbgeStatHandler</tt> for Gnutella pings sent over UDP.
	 */
	public stbtic final DroppedSentMessageStatHandler UDP_PING_REQUESTS = 
		new DroppedSentMessbgeStatHandler(DroppedSentMessageStat.UDP_PING_REQUESTS, 
								   DroppedSentMessbgeStatBytes.UDP_PING_REQUESTS,
								   DroppedLimeSentMessbgeStat.UDP_PING_REQUESTS, 
								   DroppedLimeSentMessbgeStatBytes.UDP_PING_REQUESTS);

	/**
	 * <tt>DroppedSentMessbgeStatHandler</tt> for Gnutella pings sent over TCP.
	 */
	public stbtic final DroppedSentMessageStatHandler TCP_PING_REQUESTS = 
		new DroppedSentMessbgeStatHandler(DroppedSentMessageStat.TCP_PING_REQUESTS,
								   DroppedSentMessbgeStatBytes.TCP_PING_REQUESTS,
								   DroppedLimeSentMessbgeStat.TCP_PING_REQUESTS,
								   DroppedLimeSentMessbgeStatBytes.TCP_PING_REQUESTS);
								   
	/**
	 * <tt>DroppedSentMessbgeStatHandler</tt> for Gnutella pings sent over MULTICAST.
	 */
	public stbtic final DroppedSentMessageStatHandler MULTICAST_PING_REQUESTS = 
		new DroppedSentMessbgeStatHandler(DroppedSentMessageStat.MULTICAST_PING_REQUESTS,
								   DroppedSentMessbgeStatBytes.MULTICAST_PING_REQUESTS,
								   DroppedLimeSentMessbgeStat.MULTICAST_PING_REQUESTS,
								   DroppedLimeSentMessbgeStatBytes.MULTICAST_PING_REQUESTS);								   

	/**
	 * <tt>DroppedSentMessbgeStatHandler</tt> for Gnutella pongs sent over UDP.
	 */
	public stbtic final DroppedSentMessageStatHandler UDP_PING_REPLIES = 
	    new DroppedSentMessbgeStatHandler(DroppedSentMessageStat.UDP_PING_REPLIES, 
								   DroppedSentMessbgeStatBytes.UDP_PING_REPLIES,
								   DroppedLimeSentMessbgeStat.UDP_PING_REPLIES, 
								   DroppedLimeSentMessbgeStatBytes.UDP_PING_REPLIES);

	/**
	 * <tt>DroppedSentMessbgeStatHandler</tt> for Gnutella pongs sent over TCP.
	 */
	public stbtic final DroppedSentMessageStatHandler TCP_PING_REPLIES = 
	    new DroppedSentMessbgeStatHandler(DroppedSentMessageStat.TCP_PING_REPLIES, 
								   DroppedSentMessbgeStatBytes.TCP_PING_REPLIES,
								   DroppedLimeSentMessbgeStat.TCP_PING_REPLIES, 
								   DroppedLimeSentMessbgeStatBytes.TCP_PING_REPLIES);
								   
	/**
	 * <tt>DroppedSentMessbgeStatHandler</tt> for Gnutella pongs sent over MULTICAST.
	 */
	public stbtic final DroppedSentMessageStatHandler MULTICAST_PING_REPLIES = 
	    new DroppedSentMessbgeStatHandler(DroppedSentMessageStat.MULTICAST_PING_REPLIES, 
								   DroppedSentMessbgeStatBytes.MULTICAST_PING_REPLIES,
								   DroppedLimeSentMessbgeStat.MULTICAST_PING_REPLIES, 
								   DroppedLimeSentMessbgeStatBytes.MULTICAST_PING_REPLIES);								   

	/**
	 * <tt>DroppedSentMessbgeStatHandler</tt> for Gnutella query requests sent 
	 * over UDP.
	 */
	public stbtic final DroppedSentMessageStatHandler UDP_QUERY_REQUESTS = 
	    new DroppedSentMessbgeStatHandler(DroppedSentMessageStat.UDP_QUERY_REQUESTS, 
								   DroppedSentMessbgeStatBytes.UDP_QUERY_REQUESTS,
								   DroppedLimeSentMessbgeStat.UDP_QUERY_REQUESTS, 
								   DroppedLimeSentMessbgeStatBytes.UDP_QUERY_REQUESTS);

	/**
	 * <tt>DroppedSentMessbgeStatHandler</tt> for Gnutella query requests sent 
	 * over TCP.
	 */
	public stbtic final DroppedSentMessageStatHandler TCP_QUERY_REQUESTS = 
	    new DroppedSentMessbgeStatHandler(DroppedSentMessageStat.TCP_QUERY_REQUESTS, 
								   DroppedSentMessbgeStatBytes.TCP_QUERY_REQUESTS,
								   DroppedLimeSentMessbgeStat.TCP_QUERY_REQUESTS, 
								   DroppedLimeSentMessbgeStatBytes.TCP_QUERY_REQUESTS);
								   
	/**
	 * <tt>DroppedSentMessbgeStatHandler</tt> for Gnutella query requests sent 
	 * over MULTICAST.
	 */
	public stbtic final DroppedSentMessageStatHandler MULTICAST_QUERY_REQUESTS = 
	    new DroppedSentMessbgeStatHandler(DroppedSentMessageStat.MULTICAST_QUERY_REQUESTS, 
								   DroppedSentMessbgeStatBytes.MULTICAST_QUERY_REQUESTS,
								   DroppedLimeSentMessbgeStat.MULTICAST_QUERY_REQUESTS, 
								   DroppedLimeSentMessbgeStatBytes.MULTICAST_QUERY_REQUESTS);
								   

	/**
	 * <tt>DroppedSentMessbgeStatHandler</tt> for Gnutella query replies sent over 
	 * UDP.
	 */
	public stbtic final DroppedSentMessageStatHandler UDP_QUERY_REPLIES = 
	    new DroppedSentMessbgeStatHandler(DroppedSentMessageStat.UDP_QUERY_REPLIES, 
								   DroppedSentMessbgeStatBytes.UDP_QUERY_REPLIES,
								   DroppedLimeSentMessbgeStat.UDP_QUERY_REPLIES, 
								   DroppedLimeSentMessbgeStatBytes.UDP_QUERY_REPLIES);

	/**
	 * <tt>DroppedSentMessbgeStatHandler</tt> for Gnutella query replies sent over 
	 * TCP.
	 */
	public stbtic final DroppedSentMessageStatHandler TCP_QUERY_REPLIES = 
	    new DroppedSentMessbgeStatHandler(DroppedSentMessageStat.TCP_QUERY_REPLIES, 
								   DroppedSentMessbgeStatBytes.TCP_QUERY_REPLIES,
								   DroppedLimeSentMessbgeStat.TCP_QUERY_REPLIES, 
								   DroppedLimeSentMessbgeStatBytes.TCP_QUERY_REPLIES);
								   
	/**
	 * <tt>DroppedSentMessbgeStatHandler</tt> for Gnutella query replies sent over 
	 * MULTICAST.
	 */
	public stbtic final DroppedSentMessageStatHandler MULTICAST_QUERY_REPLIES = 
	    new DroppedSentMessbgeStatHandler(DroppedSentMessageStat.MULTICAST_QUERY_REPLIES, 
								   DroppedSentMessbgeStatBytes.MULTICAST_QUERY_REPLIES,
								   DroppedLimeSentMessbgeStat.MULTICAST_QUERY_REPLIES, 
								   DroppedLimeSentMessbgeStatBytes.MULTICAST_QUERY_REPLIES);								   

	/**
	 * <tt>DroppedSentMessbgeStatHandler</tt> for Gnutella push requests sent over 
	 * UDP.
	 */
	public stbtic final DroppedSentMessageStatHandler UDP_PUSH_REQUESTS = 
	    new DroppedSentMessbgeStatHandler(DroppedSentMessageStat.UDP_PUSH_REQUESTS, 
								   DroppedSentMessbgeStatBytes.UDP_PUSH_REQUESTS,
								   DroppedLimeSentMessbgeStat.UDP_PUSH_REQUESTS, 
								   DroppedLimeSentMessbgeStatBytes.UDP_PUSH_REQUESTS);

	/**
	 * <tt>DroppedSentMessbgeStatHandler</tt> for Gnutella push requests sent over 
	 * TCP.
	 */
	public stbtic final DroppedSentMessageStatHandler TCP_PUSH_REQUESTS = 
	    new DroppedSentMessbgeStatHandler(DroppedSentMessageStat.TCP_PUSH_REQUESTS, 
								   DroppedSentMessbgeStatBytes.TCP_PUSH_REQUESTS,
								   DroppedLimeSentMessbgeStat.TCP_PUSH_REQUESTS, 
								   DroppedLimeSentMessbgeStatBytes.TCP_PUSH_REQUESTS);
								   
	/**
	 * <tt>DroppedSentMessbgeStatHandler</tt> for Gnutella push requests sent over 
	 * MULTICAST.
	 */
	public stbtic final DroppedSentMessageStatHandler MULTICAST_PUSH_REQUESTS = 
	    new DroppedSentMessbgeStatHandler(DroppedSentMessageStat.MULTICAST_PUSH_REQUESTS, 
								   DroppedSentMessbgeStatBytes.MULTICAST_PUSH_REQUESTS,
								   DroppedLimeSentMessbgeStat.MULTICAST_PUSH_REQUESTS, 
								   DroppedLimeSentMessbgeStatBytes.MULTICAST_PUSH_REQUESTS);								   

	/**
	 * <tt>DroppedSentMessbgeStatHandler</tt> for Gnutella route table messages sent 
	 * over UDP.
	 */
	public stbtic final DroppedSentMessageStatHandler UDP_ROUTE_TABLE_MESSAGES = 
	    new DroppedSentMessbgeStatHandler(DroppedSentMessageStat.UDP_ROUTE_TABLE_MESSAGES, 
								   DroppedSentMessbgeStatBytes.UDP_ROUTE_TABLE_MESSAGES,
								   DroppedLimeSentMessbgeStat.UDP_ROUTE_TABLE_MESSAGES, 
								   DroppedLimeSentMessbgeStatBytes.UDP_ROUTE_TABLE_MESSAGES);

	/**
	 * <tt>DroppedSentMessbgeStatHandler</tt> for Gnutella reset route table messages 
	 * sent over TCP.
	 */
	public stbtic final DroppedSentMessageStatHandler TCP_RESET_ROUTE_TABLE_MESSAGES = 
	    new DroppedSentMessbgeStatHandler(DroppedSentMessageStat.TCP_RESET_ROUTE_TABLE_MESSAGES,
								   DroppedSentMessbgeStatBytes.TCP_RESET_ROUTE_TABLE_MESSAGES,
								   DroppedLimeSentMessbgeStat.TCP_RESET_ROUTE_TABLE_MESSAGES,
								   DroppedLimeSentMessbgeStatBytes.TCP_RESET_ROUTE_TABLE_MESSAGES);

	/**
	 * <tt>DroppedSentMessbgeStatHandler</tt> for Gnutella patch route table messages 
	 * sent over TCP.
	 */
	public stbtic final DroppedSentMessageStatHandler TCP_PATCH_ROUTE_TABLE_MESSAGES = 
	    new DroppedSentMessbgeStatHandler(DroppedSentMessageStat.TCP_PATCH_ROUTE_TABLE_MESSAGES,
								   DroppedSentMessbgeStatBytes.TCP_PATCH_ROUTE_TABLE_MESSAGES,
								   DroppedLimeSentMessbgeStat.TCP_PATCH_ROUTE_TABLE_MESSAGES,
								   DroppedLimeSentMessbgeStatBytes.TCP_PATCH_ROUTE_TABLE_MESSAGES);
								   
	/**
	 * <tt>DroppedSentMessbgeStatHandler</tt> for Gnutella route table messages sent 
	 * over MULTICAST.
	 */
	public stbtic final DroppedSentMessageStatHandler MULTICAST_ROUTE_TABLE_MESSAGES = 
	    new DroppedSentMessbgeStatHandler(DroppedSentMessageStat.MULTICAST_ROUTE_TABLE_MESSAGES,
								   DroppedSentMessbgeStatBytes.MULTICAST_ROUTE_TABLE_MESSAGES,
								   DroppedLimeSentMessbgeStat.MULTICAST_ROUTE_TABLE_MESSAGES,
								   DroppedLimeSentMessbgeStatBytes.MULTICAST_ROUTE_TABLE_MESSAGES);								   
}
