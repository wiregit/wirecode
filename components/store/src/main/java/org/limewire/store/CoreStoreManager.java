package org.limewire.store;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.store.StoreConnection;
import org.limewire.core.api.search.store.StoreConnectionFactory;
import org.limewire.core.api.search.store.StoreDownloadToken;
import org.limewire.core.api.search.store.StoreListener;
import org.limewire.core.api.search.store.StoreManager;
import org.limewire.core.api.search.store.StoreResult;
import org.limewire.core.api.search.store.StoreSearchListener;
import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.core.api.search.store.TrackResult;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Implementation of StoreManager for the live core.
 */
@Singleton
public class CoreStoreManager implements StoreManager {
    private static final Log LOG = LogFactory.getLog(CoreStoreManager.class);
    
    private final List<StoreListener> listenerList = 
        new CopyOnWriteArrayList<StoreListener>();
    
    private final Map<AttributeKey, Object> userAttributes = 
        Collections.synchronizedMap(new EnumMap<AttributeKey, Object>(AttributeKey.class));
    
    private final StoreConnectionFactory storeConnectionFactory;

    /**
     * Constructs a CoreStoreManager with the specified store connection 
     * factory.
     */
    @Inject
    public CoreStoreManager(StoreConnectionFactory storeConnectionFactory) {
        this.storeConnectionFactory = storeConnectionFactory;
    }
    
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
        // TODO implement
        return null;
    }

    @Override
    public StoreDownloadToken validateDownload(StoreResult storeResult) {
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
        return (userAttributes.get(AttributeKey.COOKIES) != null);
    }

    @Override
    public Object getUserAttribute(AttributeKey key) {
        return userAttributes.get(key);
    }

    @Override
    public void setUserAttribute(AttributeKey key, Object attribute) {
        boolean wasLoggedIn = isLoggedIn();
        
        // Set attribute value.
        userAttributes.put(key, attribute);
        
        // Notify listeners if login state changed.
        if (isLoggedIn() != wasLoggedIn) {
            fireLoginChanged(isLoggedIn());
        }
    }

    @Override
    public void logout() {
        boolean wasLoggedIn = isLoggedIn();
        
        // Remove cookies.
        userAttributes.remove(AttributeKey.COOKIES);
        
        // Notify listeners if login state changed.
        if (isLoggedIn() != wasLoggedIn) {
            fireLoginChanged(isLoggedIn());
        }
    }

    @Override
    public void startSearch(final SearchDetails searchDetails, 
            final StoreSearchListener storeSearchListener) {
        // Start background process to retrieve store results.
        new Thread(new Runnable() {
            public void run() {
                // Create store connection.
                StoreConnection storeConnection = storeConnectionFactory.create();
                
                // Execute query.
                String query = searchDetails.getSearchQuery();
                String jsonStr = storeConnection.doQuery(query);
                
                if (!StringUtils.isEmpty(jsonStr)) {
                    try {
                        // Parse JSON to create store style and results collection.
                        JSONObject jsonObj = new JSONObject(jsonStr);
                        StoreStyle storeStyle = readStoreStyle(jsonObj);
                        StoreResult[] storeResults = readStoreResults(jsonObj);

                        // Fire event to update style.
                        storeSearchListener.styleUpdated(storeStyle);

                        // Fire event to handle results.
                        storeSearchListener.resultsFound(storeResults);
                        
                    } catch (Exception ex) {
                        LOG.warnf(ex, ex.getMessage());
                    }
                }
            }
        }).start();
    }
    
    /**
     * Returns the store style contained in the specified JSON object.
     */
    private StoreStyle readStoreStyle(JSONObject jsonObj) throws IOException, JSONException {
        JSONObject styleObj = jsonObj.getJSONObject("storeStyle");
        return new StoreStyleAdapter(styleObj);
    }
    
    /**
     * Returns an array of store results contained in the specified JSON object.
     */
    private StoreResult[] readStoreResults(JSONObject jsonObj) throws IOException, JSONException {
        // Retrieve JSON array of results.
        JSONArray resultsArr = jsonObj.getJSONArray("storeResults");
        
        // Create StoreResult array.
        StoreResult[] storeResults = new StoreResult[resultsArr.length()];
        for (int i = 0, len = resultsArr.length(); i < len; i++) {
            JSONObject resultObj = resultsArr.getJSONObject(i);
            storeResults[i] = new StoreResultAdapter(resultObj);
        }
        
        return storeResults;
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
