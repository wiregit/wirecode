package com.limegroup.gnutella;

import com.limegroup.gnutella.routing.*;
import com.sun.java.util.collections.*;

/**
 * An interface for those things that handle replies and thus are placed
 * as values in RouteTables.
 * This interface is implemented by ManagedConnection and by
 * MessageRouter.ForMeReplyHandler.
 */
interface ReplyHandler {

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
	 * Accessor for the reported number of intra-Ultrapeer connections
	 * this connection attempts to maintain.  If the node is not an
	 * Ultrapeer, this returns 0.  If it is an Ultrapeer but does not
	 * support this header, we assume that it tries to maintain 6 intra-
	 * Ultrapeer connections.
	 *
	 * @return the number of intra-Ultrapeer connections the connected node
	 *  attempts to maintain, as reported in the X-Degree handshake header
	 *  or guessed at otherwise
	 */
	int getNumIntraUltrapeerConnections();
}
