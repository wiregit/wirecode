package com.limegroup.gnutella;

/**
 * An implementation of PingReplyHandler updates the horizon statistics on the
 * receiving connection.
 *
 * @author Ron Vogl
 */
public class StandardPingReplyHandler
    implements PingReplyHandler
{
    private static StandardPingReplyHandler _instance;

    private StandardPingReplyHandler() {}

    public static StandardPingReplyHandler instance()
    {
        if(_instance == null)
            _instance = new StandardPingReplyHandler();
        return _instance;
    }

    public void handlePingReply(PingReply pingReply,
                                ManagedConnection receivingConnection,
                                MessageRouter router,
                                ActivityCallback callback)
    {
        receivingConnection.updateHorizonStats(pingReply);
    }
}
