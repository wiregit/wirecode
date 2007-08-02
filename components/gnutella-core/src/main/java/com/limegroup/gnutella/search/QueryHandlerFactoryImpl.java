package com.limegroup.gnutella.search;

import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;

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

    public QueryHandlerFactoryImpl(QueryRequestFactory queryRequestFactory) {
        this.queryRequestFactory = queryRequestFactory;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.search.QueryHandlerFactory#createHandler(com.limegroup.gnutella.messages.QueryRequest, com.limegroup.gnutella.ReplyHandler, com.limegroup.gnutella.search.ResultCounter)
     */
    public QueryHandler createHandler(QueryRequest query, ReplyHandler handler,
            ResultCounter counter) {
        return new QueryHandler(query, QueryHandler.ULTRAPEER_RESULTS, handler,
                counter, queryRequestFactory);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.search.QueryHandlerFactory#createHandlerForMe(com.limegroup.gnutella.messages.QueryRequest, com.limegroup.gnutella.search.ResultCounter)
     */
    public QueryHandler createHandlerForMe(QueryRequest query,
            ResultCounter counter) {
        // because UPs seem to get less results, give them more than usual
        return new QueryHandler(
                query,
                (int) (QueryHandler.ULTRAPEER_RESULTS * QueryHandler.UP_RESULT_BUMP),
                ProviderHacks.getForMeReplyHandler(), counter, queryRequestFactory);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.search.QueryHandlerFactory#createHandlerForOldLeaf(com.limegroup.gnutella.messages.QueryRequest, com.limegroup.gnutella.ReplyHandler, com.limegroup.gnutella.search.ResultCounter)
     */
    public QueryHandler createHandlerForOldLeaf(QueryRequest query,
            ReplyHandler handler, ResultCounter counter) {
        return new QueryHandler(query,
                QueryHandlerFactoryImpl.OLD_LEAF_RESULTS, handler, counter,
                queryRequestFactory);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.search.QueryHandlerFactory#createHandlerForNewLeaf(com.limegroup.gnutella.messages.QueryRequest, com.limegroup.gnutella.ReplyHandler, com.limegroup.gnutella.search.ResultCounter)
     */
    public QueryHandler createHandlerForNewLeaf(QueryRequest query,
            ReplyHandler handler, ResultCounter counter) {
        return new QueryHandler(query,
                QueryHandlerFactoryImpl.NEW_LEAF_RESULTS, handler, counter,
                queryRequestFactory);
    }

}
