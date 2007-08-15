package com.limegroup.gnutella.search;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ForMeReplyHandler;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;

@Singleton
public class QueryHandlerFactoryImpl implements QueryHandlerFactory {

    /**
     * The number of results to try to get if the query came from an old
     * leaf -- they are connected to 2 other Ultrapeers that may or may
     * not use this algorithm.
     */
    private static final int OLD_LEAF_RESULTS = 20;

    /**
     * The number of results to try to get for new leaves -- they only 
     * maintain 2 connections and don't generate as much overall traffic,
     * so give them a little more.
     */
    private static final int NEW_LEAF_RESULTS = 38;

    private final QueryRequestFactory queryRequestFactory;

    private final ForMeReplyHandler forMeReplyHandler;

    private final Provider<ConnectionManager> connectionManager;

    private final Provider<MessageRouter> messageRouter;

    @Inject
    public QueryHandlerFactoryImpl(QueryRequestFactory queryRequestFactory,
            ForMeReplyHandler forMeReplyHandler,
            Provider<ConnectionManager> connectionManager,
            Provider<MessageRouter> messageRouter) {
        this.queryRequestFactory = queryRequestFactory;
        this.forMeReplyHandler = forMeReplyHandler;
        this.connectionManager = connectionManager;
        this.messageRouter = messageRouter;
    }

    public QueryHandler createHandler(QueryRequest query, ReplyHandler handler,
            ResultCounter counter) {
        return new QueryHandler(query, QueryHandler.ULTRAPEER_RESULTS, handler,
                counter, queryRequestFactory, connectionManager.get(), messageRouter.get());
    }

    public QueryHandler createHandlerForMe(QueryRequest query,
            ResultCounter counter) {
        // because UPs seem to get less results, give them more than usual
        return new QueryHandler(
                query,
                (int) (QueryHandler.ULTRAPEER_RESULTS * QueryHandler.UP_RESULT_BUMP),
                forMeReplyHandler, counter, queryRequestFactory, connectionManager.get(),
                messageRouter.get());
    }

    public QueryHandler createHandlerForOldLeaf(QueryRequest query,
            ReplyHandler handler, ResultCounter counter) {
        return new QueryHandler(query,
                QueryHandlerFactoryImpl.OLD_LEAF_RESULTS, handler, counter,
                queryRequestFactory, connectionManager.get(), messageRouter.get());
    }

    public QueryHandler createHandlerForNewLeaf(QueryRequest query,
            ReplyHandler handler, ResultCounter counter) {
        return new QueryHandler(query,
                QueryHandlerFactoryImpl.NEW_LEAF_RESULTS, handler, counter,
                queryRequestFactory, connectionManager.get(), messageRouter.get());
    }

}
