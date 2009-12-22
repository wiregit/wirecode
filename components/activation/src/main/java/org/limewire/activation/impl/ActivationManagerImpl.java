package org.limewire.activation.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.limewire.activation.api.ActivationError;
import org.limewire.activation.api.ActivationEvent;
import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationManager;
import org.limewire.activation.api.ActivationState;
import org.limewire.inject.EagerSingleton;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;
import org.limewire.setting.ActivationSettings;

//TODO: this needs to be a service, within the service start call 
// we attempt to authenticate the key if the key exists. otherwise
// it waits for user action.
@EagerSingleton
public class ActivationManagerImpl implements ActivationManager {

    private final EventListenerList<ActivationEvent> listeners = new EventListenerList<ActivationEvent>();

    private volatile ActivationState currentState = ActivationState.UNINITIALIZED;
    private volatile ActivationError activationError = ActivationError.NO_ERROR;
    
    @Override
    public void addListener(EventListener<ActivationEvent> listener) {
        listeners.addListener(listener);
    }

    @Override
    public boolean removeListener(EventListener<ActivationEvent> listener) {
        return listeners.removeListener(listener);
    }
    
    @Override
    public void activateKey(final String key) {
        //TODO: this sould hit the real server
        Thread t = new Thread(new Runnable(){
            public void run() {
                currentState = ActivationState.ACTIVATING;
                activationError = ActivationError.NO_ERROR;
                listeners.broadcast(new ActivationEvent(ActivationState.ACTIVATING));
                try {
                    Thread.sleep(4000);
                } catch(InterruptedException e) {
                    
                }
                if(key.equals("12345")) {
                    currentState = ActivationState.ACTIVATED;
                    activationError = ActivationError.NO_ERROR;
                    listeners.broadcast(new ActivationEvent(ActivationState.ACTIVATED));
                    ActivationSettings.ACTIVATION_KEY.set(key);
                } else {
                    currentState = ActivationState.NOT_ACTIVATED;
                    activationError = ActivationError.INVALID_KEY;
                    listeners.broadcast(new ActivationEvent(ActivationState.NOT_ACTIVATED, ActivationError.INVALID_KEY));
//                    ActivationSettings.ACTIVATION_KEY.set("");
                }
            }
        });
        t.start();
    }
    
    @Override
    public ActivationState getActivationState() {
        return currentState;
    }
    
    @Override
    public ActivationError getActivationError() {
        return activationError;
    }

    @Override
    public List<ActivationItem> getActivationItems() {
        if(currentState == ActivationState.ACTIVATED) {
            List<ActivationItem> list = new ArrayList<ActivationItem>();
            list.add(new ActivationItemTest("Test Active", false, true));
            list.add(new ActivationItemTest("Test Inactive", false, false));
            list.add(new ActivationItemTest("Test Expired", true, true));   
            return list;
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public String getLicenseKey() {
        return ActivationSettings.ACTIVATION_KEY.getValueAsString();
    }

    @Override
    public boolean isValidKey(String key) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isPro() {
        return false;
    }
    
    //NOTE: for testing only
    private class ActivationItemTest implements ActivationItem {

        private String name;
        private boolean isExpired;
        private boolean isActivate;
        
        public ActivationItemTest(String name, boolean isExpired, boolean isActive) {
            this.name = name;
            this.isExpired = isExpired;
            this.isActivate = isActive;
        }
        
        @Override
        public long getDateExpired() {
            return 100010101;
        }

        @Override
        public long getDatePurchased() {
            return 100010101;
        }

        @Override
        public String getLicenseName() {
            return name;
        }

        @Override
        public String getURL() {
            // TODO Auto-generated method stub
            return "http://www.google.com";
        }

        @Override
        public boolean isActiveVersion() {
            return isActivate;
        }

        @Override
        public boolean isExpired() {
            return isExpired;
        }

        @Override
        public boolean isSubscription() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public String getFirstSupportedVersion() {
            return "5.5";
        }
        
    }
}
