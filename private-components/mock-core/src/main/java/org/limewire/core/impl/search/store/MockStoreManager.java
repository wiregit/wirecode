package org.limewire.core.impl.search.store;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.store.StoreConnectionFactory;
import org.limewire.core.api.search.store.StoreListener;
import org.limewire.core.api.search.store.StoreManager;
import org.limewire.core.api.search.store.StoreResult;
import org.limewire.core.api.search.store.StoreSearchListener;
import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.core.api.search.store.TrackResult;
import org.limewire.core.api.search.store.StoreStyle.Type;

import com.google.inject.Inject;
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
    
    private final StoreConnectionFactory storeConnectionFactory;
    
    @Inject
    public MockStoreManager(StoreConnectionFactory storeConnectionFactory) {
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
    public String getConfirmURI() {
        return getClass().getResource("confirm.html").toString();
    }

    @Override
    public String getLoginURI() {
        return getClass().getResource("login.html").toString();
    }

    @Override
    public boolean isDownloadApproved(StoreResult storeResult) {
        return false;
    }

    @Override
    public boolean isDownloadApproved(TrackResult trackResult) {
        return false;
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
                MockStoreConnection storeConnection = (MockStoreConnection) storeConnectionFactory.create();
                
                // Determine mock style type for connection.
                Type styleType;
                if (query.indexOf("monkey") > -1) {
                    styleType = Type.STYLE_A;
                } else if (query.indexOf("bear") > -1) {
                    styleType = Type.STYLE_B;
                } else if (query.indexOf("cat") > -1) {
                    styleType = Type.STYLE_C;
                } else if (query.indexOf("dog") > -1) {
                    styleType = Type.STYLE_D;
                } else {
                    styleType = Type.STYLE_A;
                }
                storeConnection.setStyleType(styleType);
                
                // Execute query.
                String jsonStr = storeConnection.doQuery(query);
                
                try {
                    // Parse JSON to create store style and results collection.
                    JSONObject jsonObj = new JSONObject(jsonStr);
                    StoreStyle storeStyle = extractStoreStyle(jsonObj);
                    StoreResult[] storeResults = extractStoreResults(jsonObj);

                    // Fire event to update style.
                    storeSearchListener.styleUpdated(storeStyle);

                    // Fire event to handle results.
                    storeSearchListener.resultsFound(storeResults);
                    
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    
    /**
     * Returns the store style contained in the specified JSON object.
     */
    private StoreStyle extractStoreStyle(JSONObject jsonObj) throws JSONException {
        JSONObject styleObj = jsonObj.getJSONObject("storeStyle");
        return new MockStoreStyle(styleObj);
    }
    
    /**
     * Returns an array of store results contained in the specified JSON object.
     */
    private StoreResult[] extractStoreResults(JSONObject jsonObj) throws JSONException {
        // Retrieve JSON array of results.
        JSONArray resultsArr = jsonObj.getJSONArray("storeResults");
        
        // Create StoreResult array.
        StoreResult[] storeResults = new StoreResult[resultsArr.length()];
        for (int i = 0, len = resultsArr.length(); i < len; i++) {
            JSONObject resultObj = resultsArr.getJSONObject(i);
            storeResults[i] = new MockStoreResult(resultObj);
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
