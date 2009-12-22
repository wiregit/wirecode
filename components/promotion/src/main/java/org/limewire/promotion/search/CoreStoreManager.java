package org.limewire.promotion.search;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.concurrent.ScheduledListeningExecutorService;
import org.limewire.core.api.URNFactory;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.store.ReleaseResult;
import org.limewire.core.api.search.store.StoreAuthState;
import org.limewire.core.api.search.store.StoreConnection;
import org.limewire.core.api.search.store.StoreDownloadToken;
import org.limewire.core.api.search.store.StoreManager;
import org.limewire.core.api.search.store.StoreResults;
import org.limewire.core.api.search.store.StoreSearchListener;
import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.core.api.search.store.TrackResult;
import org.limewire.listener.CachingEventMulticaster;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * Implementation of StoreManager for the live core.
 */
@Singleton
class CoreStoreManager implements StoreManager {
    private static final Log LOG = LogFactory.getLog(CoreStoreManager.class);
    
    private final StoreConnection storeConnection;
    private final ScheduledListeningExecutorService executorService;
    private final CachingEventMulticaster<StoreAuthState> storeAuthState;
    private final Provider<String> storeLoginPopupURL;
    private final URNFactory urnFactory;

    /**
     * Constructs a CoreStoreManager with the specified store connection 
     * factory.
     */
    @Inject
    public CoreStoreManager(StoreConnection storeConnection,
                            @Named("backgroundExecutor") ScheduledListeningExecutorService executorService,
                            CachingEventMulticaster<StoreAuthState> storeAuthState,
                            @StoreLoginPopupURL Provider<String> storeLoginPopupURL,
                            URNFactory urnFactory) {
        this.storeConnection = storeConnection;
        this.executorService = executorService;
        this.storeAuthState = storeAuthState;
        this.storeLoginPopupURL = storeLoginPopupURL;
        this.urnFactory = urnFactory;
    }

    @Override
    public URI getLoginURI() throws URISyntaxException {
        return new URI(storeLoginPopupURL.get());
    }

    @Override
    public StoreDownloadToken validateDownload(ReleaseResult releaseResult) {
        // TODO implement
        return null;
    }

    @Override
    public StoreDownloadToken validateDownload(TrackResult trackResult) {
        // TODO implement
        return null;
    }
    
    @Override
    public boolean isLoggedIn() {
        return storeAuthState.getLastEvent().isLoggedIn();
    }

    @Override
    public void logout() {
        storeConnection.logout();
        storeAuthState.broadcast(new StoreAuthState(false));
    }

    @Override
    public void startSearch(final SearchDetails searchDetails, 
            final StoreSearchListener storeSearchListener) {
        // Start background process to retrieve store results.
        executorService.execute(new Runnable() {
            public void run() {
                try {
                    String query = searchDetails.getSearchQuery();
                    JSONObject storeSearchResponse = storeConnection.doQuery(query);
                
                    if (!storeSearchResponse.isNull("storeResults")) {             
//                        long time = valueToTimestamp(jsonObj.getString("styleTimestamp"));
                        
                        // Get store results array.
                        StoreResults storeResults = readStoreResults(storeSearchResponse);

                        StoreStyle style = loadStyle(storeResults);
                        // Fire event to update style.
                        storeSearchListener.styleUpdated(style);
                        // Fire event to handle results.
                        storeSearchListener.resultsFound(storeResults.getResults().toArray(new ReleaseResult[storeResults.getResults().size()]));
                    }
                        
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private StoreStyle loadStyle(StoreResults storeResults) throws IOException, JSONException {
        JSONObject style = storeConnection.loadStyle(storeResults.getRenderStyle());
        return new StoreStyleAdapter(style, storeConnection);
    }
    
    /**
     * Returns an array of store results contained in the specified JSON object.
     */
    private StoreResults readStoreResults(JSONObject jsonObj) throws IOException, JSONException {
        // Retrieve JSON array of results.
        JSONObject storeResults = jsonObj.getJSONObject("storeResults");
        
        return new StoreResultsAdapter(storeResults, storeConnection, executorService, urnFactory);
    }
}
