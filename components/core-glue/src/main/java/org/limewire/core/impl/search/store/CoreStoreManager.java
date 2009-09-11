package org.limewire.core.impl.search.store;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.store.StoreListener;
import org.limewire.core.api.search.store.StoreManager;
import org.limewire.core.api.search.store.StoreResult;
import org.limewire.core.api.search.store.StoreSearchListener;
import org.limewire.core.api.search.store.TrackResult;

import com.google.inject.Singleton;

/**
 * Implementation of StoreManager for the live core.
 */
@Singleton
public class CoreStoreManager implements StoreManager {

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
    public String getConfirmURI() {
        // TODO implement
        return null;
    }

    @Override
    public String getLoginURI() {
        // TODO implement
        return null;
    }

    @Override
    public boolean isDownloadApproved(StoreResult storeResult) {
        // TODO implement
        return false;
    }

    @Override
    public boolean isDownloadApproved(TrackResult trackResult) {
        // TODO implement
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
    public void startSearch(SearchDetails searchDetails, StoreSearchListener storeSearchListener) {
        // TODO implement
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
