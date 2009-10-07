package org.limewire.promotion.search;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.concurrent.ScheduledListeningExecutorService;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.store.StoreConnection;
import org.limewire.core.api.search.store.StoreDownloadToken;
import org.limewire.core.api.search.store.StoreListener;
import org.limewire.core.api.search.store.StoreManager;
import org.limewire.core.api.search.store.StoreResult;
import org.limewire.core.api.search.store.StoreSearchListener;
import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.core.api.search.store.TrackResult;
import org.limewire.core.api.search.store.StoreStyle.Type;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * Implementation of StoreManager for the live core.
 */
@Singleton
public class CoreStoreManager implements StoreManager {
    private static final Log LOG = LogFactory.getLog(CoreStoreManager.class);
    
    private final List<StoreListener> listenerList = 
        new CopyOnWriteArrayList<StoreListener>();
    
    private final Map<Type, StoreStyle> styleMap = 
        Collections.synchronizedMap(new EnumMap<Type, StoreStyle>(Type.class));
    
    private final Map<AttributeKey, Object> userAttributes = 
        Collections.synchronizedMap(new EnumMap<AttributeKey, Object>(AttributeKey.class));
    
    private final StoreConnection storeConnection;
    private final ScheduledListeningExecutorService executorService;

    /**
     * Constructs a CoreStoreManager with the specified store connection 
     * factory.
     */
    @Inject
    public CoreStoreManager(StoreConnection storeConnection,
                            @Named("backgroundExecutor") ScheduledListeningExecutorService executorService) {
        this.storeConnection = storeConnection;
        this.executorService = executorService;
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
        executorService.submit(new Runnable() {
            public void run() {
                
                // Execute query.
                String query = searchDetails.getSearchQuery();
                String jsonStr = storeConnection.doQuery(query);
                
                if (!StringUtils.isEmpty(jsonStr)) {
                    try {
                        // Create JSON object from query result.
                        JSONObject jsonObj = new JSONObject(jsonStr);
                        
                        // Get style type and timestamp.
                        Type type = valueToType(jsonObj.getString("styleType"));
                        long time = valueToTimestamp(jsonObj.getString("styleTimestamp"));
                        
                        // Get store results array.
                        StoreResult[] storeResults = readStoreResults(jsonObj);

                        // Get cached style and compare timestamp.
                        StoreStyle storeStyle = styleMap.get(type);
                        if (storeStyle != null) {
                            if (storeStyle.getTimestamp() < time) {
                                storeStyle = null;
                            }
                        }
                        
                        // Load new style if necessary.
                        if (storeStyle == null) {
                            JSONObject styleJson = new JSONObject(storeConnection.loadStyle(type.toString()));
                            storeStyle = readStoreStyle(styleJson);
                            styleMap.put(type, storeStyle);
                        }
                        
                        // Fire event to update style.
                        storeSearchListener.styleUpdated(storeStyle);

                        // Fire event to handle results.
                        storeSearchListener.resultsFound(storeResults);
                        
                    } catch (Exception ex) {
                        LOG.warnf(ex, ex.getMessage());
                    }
                }
            }
        });
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
     * Converts the specified input value to a timestamp.
     */
    private long valueToTimestamp(String value) {
        try {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return dateFormat.parse(value).getTime();
            
        } catch (ParseException ex) {
            ex.printStackTrace();
            return 0;
        }
    }
    
    /**
     * Returns the Type that matches the input value.
     */
    private Type valueToType(String value) {
        for (Type type : Type.values()) {
            if (type.toString().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
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
