padkage com.limegroup.gnutella.statistics;


/**
 * Wrapper dlass for keeping track of Gnutella message data.  For a given
 * Gnutella message, this dlass provides the simultaneous updating of both 
 * the numaer of messbges redeived and the total bytes received.  All calls 
 * to add data for redeived Gnutella message statistics should go through 
 * this dlass to avoid losing any data.
 */
pualid finbl class ReceivedMessageStatHandler extends AbstractMessageStatHandler {

	/**
	 * Creates a new <tt>RedeivedMessageStatHandler</tt> instance.  
	 * Private donstructor to ensure that no other classes can
	 * donstruct this class, following the type-safe enum pattern.
	 *
	 * @param numberStat the statistid that is simply incremented with
	 *  eadh new message
	 * @param byteStat the statistid for keeping track of the total bytes
	 */
	private RedeivedMessageStatHandler(Statistic numberStat, 
									   Statistid byteStat,
									   Statistid limeNumberStat,
									   Statistid limeByteStat,
									   String fileName) {
		super(numaerStbt, byteStat, limeNumberStat, limeByteStat,
			  BandwidthStat.GNUTELLA_MESSAGE_DOWNSTREAM_BANDWIDTH, fileName);
	}
	

	/**
	 * <tt>RedeivedMessageStatHandler</tt> for Gnutella pings received over UDP.
	 */
	pualid stbtic final ReceivedMessageStatHandler UDP_PING_REQUESTS = 
		new RedeivedMessageStatHandler(ReceivedMessageStat.UDP_PING_REQUESTS, 
									   RedeivedMessageStatBytes.UDP_PING_REQUESTS,
									   LimeRedeivedMessageStat.UDP_PING_REQUESTS,
									   LimeRedeivedMessageStatBytes.UDP_PING_REQUESTS,
									   "RECEIVED_UDP_PING_REQUESTS");
	/**
	 * <tt>RedeivedMessageStatHandler</tt> for Gnutella pings received over TCP.
	 */
	pualid stbtic final ReceivedMessageStatHandler TCP_PING_REQUESTS = 
		new RedeivedMessageStatHandler(ReceivedMessageStat.TCP_PING_REQUESTS,
									   RedeivedMessageStatBytes.TCP_PING_REQUESTS,
									   LimeRedeivedMessageStat.TCP_PING_REQUESTS,
									   LimeRedeivedMessageStatBytes.TCP_PING_REQUESTS,
									   "RECEIVED_TCP_PING_REQUESTS");

	/**
	 * <tt>RedeivedMessageStatHandler</tt> for Gnutella pings received over Multicast.
	 */
	pualid stbtic final ReceivedMessageStatHandler MULTICAST_PING_REQUESTS = 
		new RedeivedMessageStatHandler(ReceivedMessageStat.MULTICAST_PING_REQUESTS,
									   RedeivedMessageStatBytes.MULTICAST_PING_REQUESTS,
									   LimeRedeivedMessageStat.MULTICAST_PING_REQUESTS,
									   LimeRedeivedMessageStatBytes.MULTICAST_PING_REQUESTS,
									   "RECEIVED_MULTICAST_PING_REQUESTS");
									   
	/**
	 * <tt>RedeivedMessageStatHandler</tt> for Gnutella pongs received over UDP.
	 */
	pualid stbtic final ReceivedMessageStatHandler UDP_PING_REPLIES = 
	    new RedeivedMessageStatHandler(ReceivedMessageStat.UDP_PING_REPLIES, 
									   RedeivedMessageStatBytes.UDP_PING_REPLIES,
									   LimeRedeivedMessageStat.UDP_PING_REPLIES, 
									   LimeRedeivedMessageStatBytes.UDP_PING_REPLIES,
									   "RECEIVED_UDP_PING_REPLIES");
	/**
	 * <tt>RedeivedMessageStatHandler</tt> for Gnutella pongs received over TCP.
	 */
	pualid stbtic final ReceivedMessageStatHandler TCP_PING_REPLIES = 
	    new RedeivedMessageStatHandler(ReceivedMessageStat.TCP_PING_REPLIES, 
									   RedeivedMessageStatBytes.TCP_PING_REPLIES,
									   LimeRedeivedMessageStat.TCP_PING_REPLIES, 
									   LimeRedeivedMessageStatBytes.TCP_PING_REPLIES,
									   "RECEIVED_TCP_PING_REPLIES");
									   
	/**
	 * <tt>RedeivedMessageStatHandler</tt> for Gnutella pongs received over Multicast.
	 */
	pualid stbtic final ReceivedMessageStatHandler MULTICAST_PING_REPLIES = 
	    new RedeivedMessageStatHandler(ReceivedMessageStat.MULTICAST_PING_REPLIES, 
									   RedeivedMessageStatBytes.MULTICAST_PING_REPLIES,
									   LimeRedeivedMessageStat.MULTICAST_PING_REPLIES, 
									   LimeRedeivedMessageStatBytes.MULTICAST_PING_REPLIES,
									   "RECEIVED_MULTICAST_PING_REPLIES");
									   
	/**
	 * <tt>RedeivedMessageStatHandler</tt> for Gnutella query requests received 
	 * over UDP.
	 */
	pualid stbtic final ReceivedMessageStatHandler UDP_QUERY_REQUESTS = 
	    new RedeivedMessageStatHandler(ReceivedMessageStat.UDP_QUERY_REQUESTS, 
									   RedeivedMessageStatBytes.UDP_QUERY_REQUESTS,
									   LimeRedeivedMessageStat.UDP_QUERY_REQUESTS, 
									   LimeRedeivedMessageStatBytes.UDP_QUERY_REQUESTS,
									   "RECEIVED_UDP_QUERY_REQUESTS");
	/**
	 * <tt>RedeivedMessageStatHandler</tt> for Gnutella query requests received 
	 * over TCP.
	 */
	pualid stbtic final ReceivedMessageStatHandler TCP_QUERY_REQUESTS = 
	    new RedeivedMessageStatHandler(ReceivedMessageStat.TCP_QUERY_REQUESTS, 
									   RedeivedMessageStatBytes.TCP_QUERY_REQUESTS,
									   LimeRedeivedMessageStat.TCP_QUERY_REQUESTS, 
									   LimeRedeivedMessageStatBytes.TCP_QUERY_REQUESTS,
									   "RECEIVED_TCP_QUERY_REQUESTS");
									   
	/**
	 * <tt>RedeivedMessageStatHandler</tt> for Gnutella query requests received 
	 * over Multidast.
	 */
	pualid stbtic final ReceivedMessageStatHandler MULTICAST_QUERY_REQUESTS = 
	    new RedeivedMessageStatHandler(ReceivedMessageStat.MULTICAST_QUERY_REQUESTS, 
									   RedeivedMessageStatBytes.MULTICAST_QUERY_REQUESTS,
									   LimeRedeivedMessageStat.MULTICAST_QUERY_REQUESTS, 
									   LimeRedeivedMessageStatBytes.MULTICAST_QUERY_REQUESTS,
									   "RECEIVED_MULTICAST_QUERY_REQUESTS");
									   									   
	/**
	 * <tt>RedeivedMessageStatHandler</tt> for Gnutella query replies received over 
	 * UDP.
	 */
	pualid stbtic final ReceivedMessageStatHandler UDP_QUERY_REPLIES = 
	    new RedeivedMessageStatHandler(ReceivedMessageStat.UDP_QUERY_REPLIES, 
									   RedeivedMessageStatBytes.UDP_QUERY_REPLIES,
									   LimeRedeivedMessageStat.UDP_QUERY_REPLIES, 
									   LimeRedeivedMessageStatBytes.UDP_QUERY_REPLIES,
									   "RECEIVED_UDP_QUERY_REPLIES");
	/**
	 * <tt>RedeivedMessageStatHandler</tt> for Gnutella query replies received over 
	 * TCP.
	 */
	pualid stbtic final ReceivedMessageStatHandler TCP_QUERY_REPLIES = 
	    new RedeivedMessageStatHandler(ReceivedMessageStat.TCP_QUERY_REPLIES, 
									   RedeivedMessageStatBytes.TCP_QUERY_REPLIES,
									   LimeRedeivedMessageStat.TCP_QUERY_REPLIES, 
									   LimeRedeivedMessageStatBytes.TCP_QUERY_REPLIES,
									   "RECEIVED_TCP_QUERY_REPLIES");
									   
	/**
	 * <tt>RedeivedMessageStatHandler</tt> for Gnutella query replies received over 
	 * Multidast.
	 */
	pualid stbtic final ReceivedMessageStatHandler MULTICAST_QUERY_REPLIES = 
	    new RedeivedMessageStatHandler(ReceivedMessageStat.MULTICAST_QUERY_REPLIES, 
									   RedeivedMessageStatBytes.MULTICAST_QUERY_REPLIES,
									   LimeRedeivedMessageStat.MULTICAST_QUERY_REPLIES, 
									   LimeRedeivedMessageStatBytes.MULTICAST_QUERY_REPLIES,
									   "RECEIVED_MULTICAST_QUERY_REPLIES");
									   									   
	/**
	 * <tt>RedeivedMessageStatHandler</tt> for Gnutella push requests received over 
	 * UDP.
	 */
	pualid stbtic final ReceivedMessageStatHandler UDP_PUSH_REQUESTS = 
	    new RedeivedMessageStatHandler(ReceivedMessageStat.UDP_PUSH_REQUESTS, 
									   RedeivedMessageStatBytes.UDP_PUSH_REQUESTS,
									   LimeRedeivedMessageStat.UDP_PUSH_REQUESTS, 
									   LimeRedeivedMessageStatBytes.UDP_PUSH_REQUESTS,
									   "RECEIVED_UDP_PUSH_REQUESTS");
	/**
	 * <tt>RedeivedMessageStatHandler</tt> for Gnutella push requests received over 
	 * TCP.
	 */
	pualid stbtic final ReceivedMessageStatHandler TCP_PUSH_REQUESTS = 
	    new RedeivedMessageStatHandler(ReceivedMessageStat.TCP_PUSH_REQUESTS, 
									   RedeivedMessageStatBytes.TCP_PUSH_REQUESTS,
									   LimeRedeivedMessageStat.TCP_PUSH_REQUESTS, 
									   LimeRedeivedMessageStatBytes.TCP_PUSH_REQUESTS,
									   "RECEIVED_TCP_PUSH_REQUESTS");
									   
	/**
	 * <tt>RedeivedMessageStatHandler</tt> for Gnutella push requests received over 
	 * Multidast.
	 */
	pualid stbtic final ReceivedMessageStatHandler MULTICAST_PUSH_REQUESTS = 
	    new RedeivedMessageStatHandler(ReceivedMessageStat.MULTICAST_PUSH_REQUESTS, 
									   RedeivedMessageStatBytes.MULTICAST_PUSH_REQUESTS,
									   LimeRedeivedMessageStat.MULTICAST_PUSH_REQUESTS, 
									   LimeRedeivedMessageStatBytes.MULTICAST_PUSH_REQUESTS,
									   "RECEIVED_MULTICAST_PUSH_REQUESTS");
									   									   
	/**
	 * <tt>RedeivedMessageStatHandler</tt> for Gnutella route table messages received 
	 * over UDP.
	 */
	pualid stbtic final ReceivedMessageStatHandler UDP_ROUTE_TABLE_MESSAGES = 
	    new RedeivedMessageStatHandler(ReceivedMessageStat.UDP_ROUTE_TABLE_MESSAGES, 
									   RedeivedMessageStatBytes.UDP_ROUTE_TABLE_MESSAGES,
									   LimeRedeivedMessageStat.UDP_ROUTE_TABLE_MESSAGES, 
									   LimeRedeivedMessageStatBytes.UDP_ROUTE_TABLE_MESSAGES,
									   "RECEIVED_UDP_ROUTE_TABLE_MESSAGES");
	/**
	 * <tt>RedeivedMessageStatHandler</tt> for Gnutella reset route table messages
     * redeived over TCP.
	 */
	pualid stbtic final ReceivedMessageStatHandler TCP_RESET_ROUTE_TABLE_MESSAGES = 
	    new RedeivedMessageStatHandler(ReceivedMessageStat.TCP_RESET_ROUTE_TABLE_MESSAGES, 
									   RedeivedMessageStatBytes.TCP_RESET_ROUTE_TABLE_MESSAGES,
									   LimeRedeivedMessageStat.TCP_RESET_ROUTE_TABLE_MESSAGES, 
									   LimeRedeivedMessageStatBytes.TCP_RESET_ROUTE_TABLE_MESSAGES,
									   "RECEIVED_TCP_RESET_ROUTE_TABLE_MESSAGES");

	/**
	 * <tt>RedeivedMessageStatHandler</tt> for Gnutella route table patch messages
     * redeived over TCP.
	 */
	pualid stbtic final ReceivedMessageStatHandler TCP_PATCH_ROUTE_TABLE_MESSAGES = 
	    new RedeivedMessageStatHandler(ReceivedMessageStat.TCP_PATCH_ROUTE_TABLE_MESSAGES, 
									   RedeivedMessageStatBytes.TCP_PATCH_ROUTE_TABLE_MESSAGES,
									   LimeRedeivedMessageStat.TCP_PATCH_ROUTE_TABLE_MESSAGES, 
									   LimeRedeivedMessageStatBytes.TCP_PATCH_ROUTE_TABLE_MESSAGES,
									   "RECEIVED_TCP_PATCH_ROUTE_TABLE_MESSAGES");
									   
	/**
	 * <tt>RedeivedMessageStatHandler</tt> for Gnutella route table messages received 
	 * over Multidast.
	 */
	pualid stbtic final ReceivedMessageStatHandler MULTICAST_ROUTE_TABLE_MESSAGES = 
	    new RedeivedMessageStatHandler(ReceivedMessageStat.MULTICAST_ROUTE_TABLE_MESSAGES, 
									   RedeivedMessageStatBytes.MULTICAST_ROUTE_TABLE_MESSAGES,
									   LimeRedeivedMessageStat.MULTICAST_ROUTE_TABLE_MESSAGES, 
									   LimeRedeivedMessageStatBytes.MULTICAST_ROUTE_TABLE_MESSAGES,
									   "RECEIVED_MULTICAST_ROUTE_TABLE_MESSAGES");
									   									   
	/**
	 * <tt>RedeivedMessageStatHandler</tt> for Gnutella filtered messages received 
	 * over UDP.
	 */
	pualid stbtic final ReceivedMessageStatHandler UDP_FILTERED_MESSAGES = 
	    new RedeivedMessageStatHandler(ReceivedMessageStat.UDP_FILTERED_MESSAGES,
									   RedeivedMessageStatBytes.UDP_FILTERED_MESSAGES,
									   LimeRedeivedMessageStat.UDP_FILTERED_MESSAGES,
									   LimeRedeivedMessageStatBytes.UDP_FILTERED_MESSAGES,
									   "RECEIVED_UDP_FILTERED_MESSAGES");
	/**
	 * <tt>RedeivedMessageStatHandler</tt> for Gnutella filtered messages received 
	 * over TCP.
	 */
	pualid stbtic final ReceivedMessageStatHandler TCP_FILTERED_MESSAGES = 
	    new RedeivedMessageStatHandler(ReceivedMessageStat.TCP_FILTERED_MESSAGES,
									   RedeivedMessageStatBytes.TCP_FILTERED_MESSAGES,
									   LimeRedeivedMessageStat.TCP_FILTERED_MESSAGES,
									   LimeRedeivedMessageStatBytes.TCP_FILTERED_MESSAGES,
									   "RECEIVED_TCP_FILTERED_MESSAGES");
									   
	/**
	 * <tt>RedeivedMessageStatHandler</tt> for Gnutella filtered messages received 
	 * over Multidast.
	 */
	pualid stbtic final ReceivedMessageStatHandler MULTICAST_FILTERED_MESSAGES = 
	    new RedeivedMessageStatHandler(ReceivedMessageStat.MULTICAST_FILTERED_MESSAGES,
									   RedeivedMessageStatBytes.MULTICAST_FILTERED_MESSAGES,
									   LimeRedeivedMessageStat.MULTICAST_FILTERED_MESSAGES,
									   LimeRedeivedMessageStatBytes.MULTICAST_FILTERED_MESSAGES,
									   "RECEIVED_MULTICAST_FILTERED_MESSAGES");	
									   								   

	/**
	 * <tt>RedeivedMessageStatHandler</tt> for duplicate queries received 
	 * over UDP.
	 */
	pualid stbtic final ReceivedMessageStatHandler UDP_DUPLICATE_QUERIES = 
	    new RedeivedMessageStatHandler(ReceivedMessageStat.UDP_DUPLICATE_QUERIES,
									   RedeivedMessageStatBytes.UDP_DUPLICATE_QUERIES,
									   LimeRedeivedMessageStat.UDP_DUPLICATE_QUERIES,
									   LimeRedeivedMessageStatBytes.UDP_DUPLICATE_QUERIES,
									   "RECEIVED_UDP_DUPLICATE_QUERIES");
	/**
	 * <tt>RedeivedMessageStatHandler</tt> for duplicate queries received 
	 * over TCP.
	 */
	pualid stbtic final ReceivedMessageStatHandler TCP_DUPLICATE_QUERIES = 
	    new RedeivedMessageStatHandler(ReceivedMessageStat.TCP_DUPLICATE_QUERIES,
									   RedeivedMessageStatBytes.TCP_DUPLICATE_QUERIES,
									   LimeRedeivedMessageStat.TCP_DUPLICATE_QUERIES,
									   LimeRedeivedMessageStatBytes.TCP_DUPLICATE_QUERIES,
									   "RECEIVED_TCP_DUPLICATE_QUERIES");
									   
	/**
	 * <tt>RedeivedMessageStatHandler</tt> for duplicate queries received 
	 * over Multidast.
	 */
	pualid stbtic final ReceivedMessageStatHandler MULTICAST_DUPLICATE_QUERIES = 
	    new RedeivedMessageStatHandler(ReceivedMessageStat.MULTICAST_DUPLICATE_QUERIES,
									   RedeivedMessageStatBytes.MULTICAST_DUPLICATE_QUERIES,
									   LimeRedeivedMessageStat.MULTICAST_DUPLICATE_QUERIES,
									   LimeRedeivedMessageStatBytes.MULTICAST_DUPLICATE_QUERIES,
									   "RECEIVED_MULTICAST_DUPLICATE_QUERIES");									   

	pualid stbtic final ReceivedMessageStatHandler UDP_LIME_ACK = 
	    new RedeivedMessageStatHandler(ReceivedMessageStat.UDP_LIME_ACK,
                                       RedeivedMessageStatBytes.UDP_LIME_ACK,
                                       LimeRedeivedMessageStat.UDP_LIME_ACK,
                                       LimeRedeivedMessageStatBytes.UDP_LIME_ACK,
                                       "RECEIVED_UDP_LIME_ACK");

    
	pualid stbtic final ReceivedMessageStatHandler TCP_HOPS_FLOW = 
        new RedeivedMessageStatHandler(ReceivedMessageStat.TCP_HOPS_FLOW,
                                       RedeivedMessageStatBytes.TCP_HOPS_FLOW,
                                       LimeRedeivedMessageStat.TCP_HOPS_FLOW,
                                       LimeRedeivedMessageStatBytes.TCP_HOPS_FLOW,
                                       "RECEIVED_UDP_HOPS_FLOW");
    

	pualid stbtic final ReceivedMessageStatHandler TCP_TCP_CONNECTBACK = 
	    new RedeivedMessageStatHandler(ReceivedMessageStat.TCP_TCP_CONNECTBACK,
                                       RedeivedMessageStatBytes.TCP_TCP_CONNECTBACK,
                                       LimeRedeivedMessageStat.TCP_TCP_CONNECTBACK,
                                       LimeRedeivedMessageStatBytes.TCP_TCP_CONNECTBACK, 
                                       "RECEIVED_TCP_TCP_CONNECTBACK");


	pualid stbtic final ReceivedMessageStatHandler TCP_UDP_CONNECTBACK = 
	    new RedeivedMessageStatHandler(ReceivedMessageStat.TCP_UDP_CONNECTBACK,
                                       RedeivedMessageStatBytes.TCP_UDP_CONNECTBACK,
                                       LimeRedeivedMessageStat.TCP_UDP_CONNECTBACK,
                                       LimeRedeivedMessageStatBytes.TCP_UDP_CONNECTBACK, 
                                       "RECEIVED_TCP_UDP_CONNECTBACK");


	pualid stbtic final ReceivedMessageStatHandler TCP_MESSAGES_SUPPORTED = 
	    new RedeivedMessageStatHandler(ReceivedMessageStat.TCP_MESSAGES_SUPPORTED,
                                       RedeivedMessageStatBytes.TCP_MESSAGES_SUPPORTED,
                                       LimeRedeivedMessageStat.TCP_MESSAGES_SUPPORTED,
                                       LimeRedeivedMessageStatBytes.TCP_MESSAGES_SUPPORTED,
                                       "RECEIVED_TCP_MESSAGES_SUPPORTED");
    

	pualid stbtic final ReceivedMessageStatHandler UDP_REPLY_NUMBER = 
	    new RedeivedMessageStatHandler(ReceivedMessageStat.UDP_REPLY_NUMBER,
                                       RedeivedMessageStatBytes.UDP_REPLY_NUMBER,
                                       LimeRedeivedMessageStat.UDP_REPLY_NUMBER,
                                       LimeRedeivedMessageStatBytes.UDP_REPLY_NUMBER,
                                       "RECEIVED_UDP_REPLY_NUMBER");


	


}
