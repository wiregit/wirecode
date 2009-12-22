package org.limewire.core.impl.search.store;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.store.StoreAuthState;
import org.limewire.core.api.search.store.StoreDownloadToken;
import org.limewire.core.api.search.store.StoreDownloadToken.Status;
import org.limewire.core.api.search.store.StoreManager;
import org.limewire.core.api.search.store.ReleaseResult;
import org.limewire.core.api.search.store.StoreResults;
import org.limewire.core.api.search.store.StoreSearchListener;
import org.limewire.core.api.search.store.TrackResult;
import org.limewire.listener.EventBean;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Implementation of StoreManager for the mock core.
 */
@Singleton
public class MockStoreManager implements StoreManager {
    
    private EventBean<StoreAuthState> storeAuth;

    @Inject
    void register(EventBean<StoreAuthState> storeAuth){
        this.storeAuth = storeAuth;
    }

    @Override
    public URI getLoginURI() throws URISyntaxException {
        return getClass().getResource("login.html").toURI();
    }

    @Override
    public StoreDownloadToken validateDownload(ReleaseResult releaseResult)  {
        if (isLoggedIn()) {
            return new MockStoreDownloadToken(Status.CONFIRM_REQ, getConfirmURI());
        } else {
            try {
                return new MockStoreDownloadToken(Status.LOGIN_REQ, getLoginURI().toASCIIString());
            } catch (URISyntaxException e) {
                return new MockStoreDownloadToken(Status.FAILED, "");
            }
        }
    }

    @Override
    public StoreDownloadToken validateDownload(TrackResult trackResult)  {
        if (isLoggedIn()) {
            return new MockStoreDownloadToken(Status.CONFIRM_REQ, getConfirmURI());
        } else {
            try {
                return new MockStoreDownloadToken(Status.LOGIN_REQ, getLoginURI().toASCIIString());
            } catch (URISyntaxException e) {
                return new MockStoreDownloadToken(Status.FAILED, "");
            }
        }
    }

    @Override
    public boolean isLoggedIn() {
        return storeAuth.getLastEvent().isLoggedIn();
    }
    
    @Override
    public void logout() {
        // TODO 
    }

    @Override
    public void startSearch(final SearchDetails searchDetails, 
            final StoreSearchListener storeSearchListener) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {}
                
                // Get query text.
                String query = searchDetails.getSearchQuery();
                
                // Create store connection.
                MockStoreConnection storeConnection = new MockStoreConnection();
                
                // Execute query.
                StoreResults storeResults = storeConnection.doQuery(query);
                
                // Fire event to update style.
                try {
                    storeSearchListener.styleUpdated(storeConnection.loadStyle(storeResults.getRenderStyle(), storeSearchListener));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Fire event to handle results.
                storeSearchListener.resultsFound(storeResults.getResults().toArray(new ReleaseResult[storeResults.getResults().size()]));

            }
        }).start();
    }
    
    /**
     * Returns the URI text for the confirm page.
     */
    private String getConfirmURI() {
        return getClass().getResource("confirm.html").toString();
    }
}
