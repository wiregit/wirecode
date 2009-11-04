package org.limewire.core.impl.search.store;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.store.StoreDownloadToken;
import org.limewire.core.api.search.store.StoreDownloadToken.Status;
import org.limewire.core.api.search.store.StoreListener;
import org.limewire.core.api.search.store.StoreManager;
import org.limewire.core.api.search.store.StoreResult;
import org.limewire.core.api.search.store.StoreResults;
import org.limewire.core.api.search.store.StoreSearchListener;
import org.limewire.core.api.search.store.TrackResult;

import com.google.inject.Singleton;

/**
 * Implementation of StoreManager for the mock core.
 */
@Singleton
public class MockStoreManager implements StoreManager {

    private final List<StoreListener> listenerList = 
        new CopyOnWriteArrayList<StoreListener>();
    
    private final Map<AttributeKey, Object> userAttributes = 
        Collections.synchronizedMap(new EnumMap<AttributeKey, Object>(AttributeKey.class));
    
    @Override
    public void addStoreListener(StoreListener listener) {
        listenerList.add(listener);
    }

    @Override
    public void removeStoreListener(StoreListener listener) {
        listenerList.remove(listener);
    }

    @Override
    public String getLoginURI() {
        return getClass().getResource("login.html").toString();
    }

    @Override
    public StoreDownloadToken validateDownload(StoreResult storeResult) {
        if (isLoggedIn()) {
            return new MockStoreDownloadToken(Status.CONFIRM_REQ, getConfirmURI());
        } else {
            return new MockStoreDownloadToken(Status.LOGIN_REQ, getLoginURI());
        }
    }

    @Override
    public StoreDownloadToken validateDownload(TrackResult trackResult) {
        if (isLoggedIn()) {
            return new MockStoreDownloadToken(Status.CONFIRM_REQ, getConfirmURI());
        } else {
            return new MockStoreDownloadToken(Status.LOGIN_REQ, getLoginURI());
        }
    }

    @Override
    public boolean isLoggedIn() {
        return (userAttributes.get(AttributeKey.COOKIES) != null);
    }
    
    @Override
    public Object getUserAttribute(AttributeKey key) {
        return userAttributes.get(key);
    }
    
    @Override
    public void setUserAttribute(AttributeKey key, Object attribute) {
        boolean wasLoggedIn = isLoggedIn();
        
        userAttributes.put(key, attribute);
        
        if (isLoggedIn() != wasLoggedIn) {
            fireLoginChanged(isLoggedIn());
        }
    }
    
    @Override
    public void logout() {
        boolean wasLoggedIn = isLoggedIn();
        
        userAttributes.remove(AttributeKey.COOKIES);
        
        if (isLoggedIn() != wasLoggedIn) {
            fireLoginChanged(isLoggedIn());
        }
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
                storeSearchListener.resultsFound(storeResults.getItems().toArray(new StoreResult[storeResults.getItems().size()]));

            }
        }).start();
    }
    
    /**
     * Returns the URI text for the confirm page.
     */
    private String getConfirmURI() {
        return getClass().getResource("confirm.html").toString();
    }
    
    /**
     * Notifies registered store listeners that the login state has changed
     * to the specified value.
     */
    private void fireLoginChanged(boolean loggedIn) {
        for (StoreListener listener : listenerList) {
            listener.loginChanged(loggedIn);
        }
    }
}
