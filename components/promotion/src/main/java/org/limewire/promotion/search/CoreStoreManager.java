package org.limewire.promotion.search;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.concurrent.ScheduledListeningExecutorService;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.store.StoreConnection;
import org.limewire.core.api.search.store.StoreDownloadToken;
import org.limewire.core.api.search.store.StoreListener;
import org.limewire.core.api.search.store.StoreManager;
import org.limewire.core.api.search.store.StoreResult;
import org.limewire.core.api.search.store.StoreResults;
import org.limewire.core.api.search.store.StoreSearchListener;
import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.core.api.search.store.StoreStyle.Type;
import org.limewire.core.api.search.store.TrackResult;
import org.limewire.geocode.GeoLocation;
import org.limewire.inject.MutableProvider;
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
    
    private final Map<AttributeKey, Object> userAttributes = 
        Collections.synchronizedMap(new EnumMap<AttributeKey, Object>(AttributeKey.class));
    
    private final StoreConnection storeConnection;
    private final StyleManager styleManager;
    private final ScheduledListeningExecutorService executorService;
    private final MutableProvider<Properties> geoLocation;

    /**
     * Constructs a CoreStoreManager with the specified store connection 
     * factory.
     */
    @Inject
    public CoreStoreManager(StoreConnection storeConnection,
                            StyleManager styleManager,
                            @Named("backgroundExecutor") ScheduledListeningExecutorService executorService,
                            @GeoLocation MutableProvider<Properties> geoLocation) {
        this.storeConnection = storeConnection;
        this.styleManager = styleManager;
        this.executorService = executorService;
        this.geoLocation = geoLocation;
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
        return "https://www.store.limewire.com/store/app/pages/account/LogIn/noDest/1/";
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
                try {
                    String query = searchDetails.getSearchQuery();
                    String jsonStr = storeConnection.doQuery(query);
                
                    if (!StringUtils.isEmpty(jsonStr)) {                    
                        JSONObject jsonObj = new JSONObject(jsonStr);
//                        long time = valueToTimestamp(jsonObj.getString("styleTimestamp"));
                        
                        // Get store results array.
                        StoreResults storeResults = readStoreResults(jsonObj);
                        
                        //StoreStyle style = getStyle(storeResults);
                        StoreStyle style = new StoreStyleAdapter(new JSONObject(storeConnection.loadStyle(storeResults.getRenderStyle())));
                        // Fire event to update style.
                        storeSearchListener.styleUpdated(style);
                        // Fire event to handle results.
                        storeSearchListener.resultsFound(storeResults.getItems().toArray(new StoreResult[storeResults.getItems().size()]));
                    }
                        
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
    
    private StoreStyle getStyle(StoreResults storeResults) {        
        if(!StringUtils.isEmpty(storeResults.getRenderStyle())) {
            try {
                Type type = Type.valueOf(storeResults.getRenderStyle());
                StoreStyle style = styleManager.getStyle(type);
                return style != null ? style : styleManager.getDefaultStyle();
            } catch (IllegalArgumentException e) {
                return styleManager.getDefaultStyle();
            }
        } else {
            return styleManager.getDefaultStyle();    
        }        
    }
    
    /**
     * Returns the store style contained in the specified JSON object.
     */
    private StoreStyle readStoreStyle(JSONObject jsonObj) throws IOException, JSONException {
        if(jsonObj.has("storeStyle")) {
            StoreStyle style = new StoreStyleAdapter(jsonObj.getJSONObject("storeStyle"));
            if(style.getBackground() != null) {  
                styleManager.updateStyle(style);
                return style;
            } else {           
                return styleManager.getStyle(style.getType());
            }
        } else {
            return styleManager.getDefaultStyle();
        }
    }
    
    /**
     * Returns an array of store results contained in the specified JSON object.
     */
    private StoreResults readStoreResults(JSONObject jsonObj) throws IOException, JSONException {
        // Retrieve JSON array of results.
        JSONObject storeResults = jsonObj.getJSONObject("storeResults");
        
        return new StoreResultsAdapter(storeResults, storeConnection);
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
     * Notifies registered store listeners that the login state has changed
     * to the specified value.
     */
    private void fireLoginChanged(boolean loggedIn) {
        for (StoreListener listener : listenerList) {
            listener.loginChanged(loggedIn);
        }
    }
}
