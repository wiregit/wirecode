package com.limegroup.gnutella;

import com.limegroup.gnutella.routing.*;
import com.sun.java.util.collections.*;

/**
 * An interface for those things that handle replies and thus are placed
 * as values in RouteTables.
 * This interface is implemented by ManagedConnection and by
 * MessageRouter.ForMeReplyHandler.
 */
interface ReplyHandler
{
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
     * Returns true if the reply handler is still able to handle
     * a reply.
     */
    boolean isOpen();
}
