package com.limegroup.gnutella;

import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.routing.*;
import com.sun.java.util.collections.*;

/**
 * An interface for those things that handle replies and thus are placed
 * as values in RouteTables.
 * This interface is implemented by ManagedConnection and by
 * MessageRouter.ForMeReplyHandler.
 */
public interface ReplyHandler {

    /**
     * Handle the PingReply, failing silently
     */
    void handlePingReply(PingReply pingReply, ReplyHandler handler);

    /**
     * Handle the QueryReply, failing silently
     */
    void handleQueryReply(QueryReply queryReply, ReplyHandler handler);

    /**
     * Handle the PushRequest, failing silently
     */
    void handlePushRequest(PushRequest pushRequest, ReplyHandler handler);

	int getNumMessagesReceived();

	void countDroppedMessage();

	Set getDomains();

	boolean isPersonalSpam(Message m);

	boolean isOutgoing();

	/**
	 * Returns whether or not this handler is killable by the handler
	 * watchdog.  In particular, this is used for old Clip2 indexing queries,
	 * which should not be killed.
	 *
	 * @return <tt>true</tt> if the handler is 'killable', i.e. a clip2
	 *  indexing query, otherwise <tt>false</tt>
	 */
	boolean isKillable();

	/**
	 * Returns whether or not this <tt>ReplyHandler</tt> sends replies
	 * from an Ultrapeer to a leaf.  This returns <tt>true</tt> only
	 * if this node is an Ultrapeer, and the node receiving these 
	 * replies is a leaf of that Ultrapeer.
	 *
	 * @return <tt>true</tt> if this node is an Ultrapeer, and the node
	 *  it is sending replies to is a leaf, otherwise returns 
	 *  <tt>false</tt>
	 */
	boolean isSupernodeClientConnection();

    /**
     * Returns true if the reply handler is still able to handle
     * a reply.
     */
    boolean isOpen();

	/**
	 * Returns whether or not this reply handler is a leaf -- whether 
	 * or not the host on the other end of this connection is a leaf 
	 * of this (necessarily) Ultrapeer.
	 *
	 * @return <tt>true</tt> if the host on the other end of this 
	 *  connection is a leaf, making this an Ultrapeer, <tt>false</tt> 
	 *  otherwise
	 */
	boolean isLeafConnection();

	/**
	 * Returns whether or not this connection is a high-degree connection,
	 * meaning that it maintains a high number of intra-Ultrapeer connections.
	 *
	 * @return <tt>true</tt> if this is a 'high-degree' connection, 
	 * otherwise <tt>false</tt>
	 */
	public boolean isHighDegreeConnection();

    /**
     * Returns whether or not this handler uses Ultrapeer query routing.
     *
     * @return <tt>true</tt> if this connection uses query routing
     *  between Ultrapeers, otherwise <tt>false</tt>
     */
    public boolean isUltrapeerQueryRoutingConnection();

    /**
     * Returns whether or not this handler is considered a "good" connection.
     * The definition of a good connection changes over time as new features
     * are released.
     * 
     * @return <tt>true</tt> if this is considered a good connections,
     *  otherwise <tt>false</tt>
     */
    public boolean isGoodConnection();
}
