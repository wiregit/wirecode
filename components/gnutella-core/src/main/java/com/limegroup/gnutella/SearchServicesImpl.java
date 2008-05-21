package com.limegroup.gnutella;

import java.util.Set;

import org.limewire.concurrent.ThreadExecutor;
import org.limewire.io.Connectable;
import org.limewire.io.IpPort;
import org.limewire.util.DebugRunnable;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.filters.MutableGUIDFilter;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.search.QueryDispatcher;
import com.limegroup.gnutella.search.SearchResultHandler;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.MessageSettings;
import com.limegroup.gnutella.statistics.OutOfBandStatistics;
import com.limegroup.gnutella.statistics.QueryStats;

@Singleton
public class SearchServicesImpl implements SearchServices {
    
    private final Provider<ResponseVerifier> responseVerifier;
    private final Provider<QueryUnicaster> queryUnicaster;
    private final Provider<SearchResultHandler> searchResultHandler;
    private final Provider<MessageRouter> messageRouter;
    private final Provider<ConnectionServices> connectionServices;
    private final Provider<QueryDispatcher> queryDispatcher;
    private final Provider<MutableGUIDFilter> mutableGUIDFilter;
    private final Provider<QueryStats> queryStats; 
    private final Provider<NetworkManager> networkManager;
    private final Provider<QueryRequestFactory> queryRequestFactory;
    private final BrowseHostHandlerManager browseHostHandlerManager;
    private final OutOfBandStatistics outOfBandStatistics;
    
    @Inject
    public SearchServicesImpl(Provider<ResponseVerifier> responseVerifier,
            Provider<QueryUnicaster> queryUnicaster,
            Provider<SearchResultHandler> searchResultHandler,
            Provider<MessageRouter> messageRouter,
            Provider<ConnectionServices> connectionServices,
            Provider<QueryDispatcher> queryDispatcher,
            Provider<MutableGUIDFilter> mutableGUIDFilter,
            Provider<QueryStats> queryStats,
            Provider<NetworkManager> networkManager,
            Provider<QueryRequestFactory> queryRequestFactory,
            BrowseHostHandlerManager browseHostHandlerManager,
            OutOfBandStatistics outOfBandStatistics) {
        this.responseVerifier = responseVerifier;
        this.queryUnicaster = queryUnicaster;
        this.searchResultHandler = searchResultHandler;
        this.messageRouter = messageRouter;
        this.connectionServices = connectionServices;
        this.queryDispatcher = queryDispatcher;
        this.mutableGUIDFilter = mutableGUIDFilter;
        this.queryStats = queryStats;
        this.networkManager = networkManager;
        this.queryRequestFactory = queryRequestFactory;
        this.browseHostHandlerManager = browseHostHandlerManager;
        this.outOfBandStatistics = outOfBandStatistics;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SearchServices#doAsynchronousBrowseHost(org.limewire.io.Connectable, com.limegroup.gnutella.GUID, com.limegroup.gnutella.GUID, java.util.Set, boolean)
     */
    public BrowseHostHandler doAsynchronousBrowseHost(
      final Connectable host, GUID guid, GUID serventID, 
      final Set<? extends IpPort> proxies, final boolean canDoFWTransfer) {
        final BrowseHostHandler handler = browseHostHandlerManager.createBrowseHostHandler(guid, serventID);
        ThreadExecutor.startThread(new DebugRunnable(new Runnable() {
            public void run() {
                handler.browseHost(host, proxies, canDoFWTransfer);
            }
        }), "BrowseHoster" );
        
        return handler;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SearchServices#isMandragoreWorm(byte[], com.limegroup.gnutella.Response)
     */
    public boolean isMandragoreWorm(byte[] guid, Response response) {
        return responseVerifier.get().isMandragoreWorm(guid, response);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SearchServices#matchesQuery(byte[], com.limegroup.gnutella.Response)
     */
    public boolean matchesQuery(byte [] guid, Response response) {
        return responseVerifier.get().matchesQuery(guid, response);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SearchServices#matchesType(byte[], com.limegroup.gnutella.Response)
     */
    public boolean matchesType(byte[] guid, Response response) {
        return responseVerifier.get().matchesType(guid, response);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SearchServices#stopQuery(com.limegroup.gnutella.GUID)
     */
    public void stopQuery(GUID guid) {
        queryUnicaster.get().purgeQuery(guid);
        searchResultHandler.get().removeQuery(guid);
        messageRouter.get().queryKilled(guid);
        if(connectionServices.get().isSupernode())
            queryDispatcher.get().addToRemove(guid);
        mutableGUIDFilter.get().removeGUID(guid.bytes());
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SearchServices#getLastQueryTime()
     */
    public long getLastQueryTime() {
    	return queryStats.get().getLastQueryTime();
    }

    /**
     * Just aggregates some common code in query() and queryWhatIsNew().
     * 
     * @param qr the search request 
     * @param type
     * @return The new stats object for this query.
     */
    private void recordAndSendQuery(final QueryRequest qr, 
                                           final MediaType type) {
        queryStats.get().recordQuery();
        responseVerifier.get().record(qr, type);
        searchResultHandler.get().addQuery(qr); // so we can leaf guide....
        messageRouter.get().sendDynamicQuery(qr);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SearchServices#queryWhatIsNew(byte[], com.limegroup.gnutella.MediaType)
     */
    public void queryWhatIsNew(final byte[] guid, final MediaType type) {
            QueryRequest qr = null;
            if (GUID.addressesMatch(guid, networkManager.get().getAddress(), networkManager.get().getPort())) {
                // if the guid is encoded with my address, mark it as needing out
                // of band support.  note that there is a VERY small chance that
                // the guid will be address encoded but not meant for out of band
                // delivery of results.  bad things may happen in this case but 
                // it seems tremendously unlikely, even over the course of a 
                // VERY long lived client
                qr = queryRequestFactory.get().createWhatIsNewOOBQuery(guid, (byte)2, type);
                outOfBandStatistics.addSentQuery();
            } else {
                qr = queryRequestFactory.get().createWhatIsNewQuery(guid, (byte)2, type);
            }
    
            if(FilterSettings.FILTER_WHATS_NEW_ADULT.getValue())
                mutableGUIDFilter.get().addGUID(guid);
    
            recordAndSendQuery(qr, type);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SearchServices#query(byte[], java.lang.String, java.lang.String, com.limegroup.gnutella.MediaType)
     */
    public void query(final byte[] guid, 
    						 final String query, 
    						 final String richQuery, 
    						 final MediaType type) {
            QueryRequest qr = null;
            if (networkManager.get().isIpPortValid() && (new GUID(guid)).addressesMatch(networkManager.get().getAddress(), 
                    networkManager.get().getPort())) {
                // if the guid is encoded with my address, mark it as needing out
                // of band support.  note that there is a VERY small chance that
                // the guid will be address encoded but not meant for out of band
                // delivery of results.  bad things may happen in this case but 
                // it seems tremendously unlikely, even over the course of a 
                // VERY long lived client
                qr = queryRequestFactory.get().createOutOfBandQuery(guid, query, richQuery, type);
                outOfBandStatistics.addSentQuery();
            } else {
                qr = queryRequestFactory.get().createQuery(guid, query, richQuery, type);
            }
            
            
            recordAndSendQuery(qr, type);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SearchServices#query(byte[], java.lang.String)
     */
    public void query(byte[] guid, String query) {
        query(guid, query, null);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SearchServices#query(byte[], java.lang.String, com.limegroup.gnutella.MediaType)
     */
    public void query(byte[] guid, String query, MediaType type) {
    	query(guid, query, "", type);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.SearchServices#newQueryGUID()
     */
    public byte[] newQueryGUID() {
        byte []ret;
        if (networkManager.get().isOOBCapable() && outOfBandStatistics.isOOBEffectiveForMe())
            ret = GUID.makeAddressEncodedGuid(networkManager.get().getAddress(), networkManager.get().getPort());
        else
            ret = GUID.makeGuid();
        if (MessageSettings.STAMP_QUERIES.getValue())
            GUID.timeStampGuid(ret);
        return ret;
    }
}
