package com.limegroup.gnutella;

import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.routing.*;
import com.sun.java.util.collections.*;
import java.net.*;

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
	boolean isHighDegreeConnection();

    /**
     * Returns whether or not this handler uses Ultrapeer query routing.
     *
     * @return <tt>true</tt> if this connection uses query routing
     *  between Ultrapeers, otherwise <tt>false</tt>
     */
    boolean isUltrapeerQueryRoutingConnection();


    /**
     * Returns whether or not this handler is considered a "good" Ultrapeer 
     * connection.  The definition of a good connection changes over time as new 
     * features are released.
     * 
     * @return <tt>true</tt> if this is considered a good Ultrapeer connection,
     *  otherwise <tt>false</tt>
     */
    boolean isGoodUltrapeer();

    /**
     * Returns whether or not this handler is considered a "good" leaf
     * connection.  The definition of a good connection changes over time as new 
     * features are released.
     * 
     * @return <tt>true</tt> if this is considered a good leaf connection,
     *  otherwise <tt>false</tt>
     */
    boolean isGoodLeaf();

    /**
     * Returns whether or not this node supports pong caching.  
     *
     * @return <tt>true</tt> if this node supports pong caching, otherwise
     *  <tt>false</tt>
     */
    boolean supportsPongCaching();

    /**
     * Determines whether new pings should be allowed from this reply handler.
     * Pings should only be accepted if we have not seen another ping from
     * this handler in a given number of milliseconds, avoiding messages
     * bursts.
     *
     * @return <tt>true</tt> if new pings are allowed, otherwise 
     *  <tt>false</tt>
     */
    boolean allowNewPings();

    /**
     * Updates the time after which we will allow new pings from this handler
     * Before this time is reached, new pings will be ignored.
     */
    void updatePingTime();

    /**
     * Accessor for the <tt>InetAddress</tt> instance for this host.
     *
     * @return the <tt>InetAddress</tt> instance for this host
     */
    InetAddress getInetAddress();

    /**
     * Determines whether or not this <tt>ReplyHandler</tt> is considered
     * stable.  For TCP connections, this will mean that the connection
     * has been alive for some minimal period of time, while UDP handlers
     * will never be considered stable.
     *
     * @return <tt>true</tt> if this <tt>ReplyHandler</tt>
     */
    boolean isStable();
}
