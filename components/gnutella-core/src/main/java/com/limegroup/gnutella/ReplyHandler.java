package com.limegroup.gnutella;

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
    void handlePingReply(PingReply pingReply,
                         ManagedConnection receivingConnection);

    /**
     * Handle the QueryReply, failing silently
     */
    void handleQueryReply(QueryReply queryReply,
                          ManagedConnection receivingConnection);

    /**
     * Handle the PushRequest, failing silently
     */
    void handlePushRequest(PushRequest pushRequest,
                           ManagedConnection receivingConnection);

    /**
     * Returns true if the reply handler is unable to handle
     * a reply because it's closed.
     */
    boolean isClosed();
}
