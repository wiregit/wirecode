package com.limegroup.gnutella;

/**
 * An implementation of QueryReplyHandler that applies the personal filter and
 * then informs the ActivityCallback of a QueryReply
 *
 * @author Ron Vogl
 */
public class StandardQueryReplyHandler
    implements QueryReplyHandler
{
    private static StandardQueryReplyHandler _instance;

    private StandardQueryReplyHandler() {}

    public static StandardQueryReplyHandler instance()
    {
        if(_instance == null)
            _instance = new StandardQueryReplyHandler();
        return _instance;
    }

    public void handleQueryReply(QueryReply queryReply,
                                 ManagedConnection receivingConnection,
                                 MessageRouter router,
                                 ActivityCallback callback)
    {
        if (!receivingConnection.isPersonalSpam(queryReply))
            callback.handleQueryReply(queryReply);
    }
}
