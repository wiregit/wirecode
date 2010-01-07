package org.limewire.activation.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.limewire.activation.api.ActivationError;
import org.limewire.activation.api.ActivationEvent;
import org.limewire.activation.api.ActivationID;
import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationManager;
import org.limewire.activation.api.ActivationModuleEvent;
import org.limewire.activation.api.ActivationState;
import org.limewire.inject.EagerSingleton;
import org.limewire.lifecycle.Service;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;
import org.limewire.setting.ActivationSettings;

import com.google.inject.Inject;

//TODO: this needs to be a service, within the service start call 
// we attempt to authenticate the key if the key exists. otherwise
// it waits for user action.
@EagerSingleton
public class ActivationManagerImpl implements ActivationManager, Service {

    private final EventListenerList<ActivationEvent> listeners = new EventListenerList<ActivationEvent>();
    private final EventListenerList<ActivationModuleEvent> moduleListeners = new EventListenerList<ActivationModuleEvent>();
    private final Map<ActivationID, ActivationItem> itemMap = new HashMap<ActivationID, ActivationItem>(4);

    private volatile ActivationState currentState = ActivationState.UNINITIALIZED;
    private volatile ActivationError activationError = ActivationError.NO_ERROR;

    
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
                    
                    // this is temporary
                    List<ActivationItem> list = new ArrayList<ActivationItem>();
                    list.add(new ActivationItemTest(0, "Test Active", false, true));
                    list.add(new ActivationItemTest(1, "Test Inactive", false, false));
                    list.add(new ActivationItemTest(2, "Test Expired", true, true));  
                    setActivationItems(list);
                    
                    listeners.broadcast(new ActivationEvent(ActivationState.ACTIVATED));
                    ActivationSettings.ACTIVATION_KEY.set(key);
                } else if (key.equals("54321")) {
                    currentState = ActivationState.NOT_ACTIVATED;
                    activationError = ActivationError.BLOCKED_KEY;
                    listeners.broadcast(new ActivationEvent(ActivationState.NOT_ACTIVATED, ActivationError.BLOCKED_KEY));
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
        synchronized (this) {
            return new ArrayList<ActivationItem>(itemMap.values());            
        }
//        if(currentState == ActivationState.ACTIVATED) {
//            List<ActivationItem> list = new ArrayList<ActivationItem>();
//            list.add(new ActivationItemTest("Test Active", false, true));
//            list.add(new ActivationItemTest("Test Inactive", false, false));
//            list.add(new ActivationItemTest("Test Expired", true, true));   
//            return list;
//        } else {
//            return Collections.emptyList();
//        }
    }
    
    public void setActivationItems(List<ActivationItem> items) {
        synchronized (this) {
            itemMap.clear();
            for(ActivationItem item : items) {
                itemMap.put(item.getModuleID(), item);
            }
        }
        save();
        for(ActivationItem item : items) {
            if(item.getModuleID() != ActivationID.UNKNOWN_MODULE)
                moduleListeners.broadcast(new ActivationModuleEvent(item.getModuleID(), item.isActive()));
        }
    }
    
    public void load() {
        
    }
    
    public void save() {
        
    }

    @Override
    public String getLicenseKey() {
        return ActivationSettings.ACTIVATION_KEY.getValueAsString();
    }

    @Override
    public boolean isValidKey(String key) {
        return true;
    }

    @Override
    public boolean isActive(ActivationID id) {
        ActivationItem item;
        synchronized (itemMap) {
            item = itemMap.get(id);            
        }
        if(item == null)
            return false;
        else
            return item.isActive();
    }
    

    @Override
    public String getServiceName() {
        return "Activation-Manager Service";
    }
    
    @Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this);
    }

    @Override
    public void initialize() {
        load();
    }

    @Override
    public void start() {
        //TODO: if a PKey exists, start the server and try contacting the authentication server
    }

    @Override
    public void stop() {
    }

    @Override
    public void addModuleListener(EventListener<ActivationModuleEvent> listener) {
        moduleListeners.addListener(listener);
    }
    
    @Override
    public boolean removeModuleListener(EventListener<ActivationModuleEvent> listener) {
        return moduleListeners.removeListener(listener);
    }
        
    @Override
    public void addListener(EventListener<ActivationEvent> listener) {
        listeners.addListener(listener);
    }

    @Override
    public boolean removeListener(EventListener<ActivationEvent> listener) {
        return listeners.removeListener(listener);
    }
    
    //NOTE: for testing only
    private class ActivationItemTest implements ActivationItem {

        private ActivationID moduelID;
        private String name;
        private boolean isExpired;
        private boolean isActivate;
        
        public ActivationItemTest(int id, String name, boolean isExpired, boolean isActive) {
            this.moduelID = ActivationID.getActivationID(id);
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
        public boolean isUseable() {
            return isActivate;
        }

        @Override
        public boolean isActive() {
            return !isExpired;
        }

//        @Override
//        public boolean isSubscription() {
//            // TODO Auto-generated method stub
//            return false;
//        }
//
//        @Override
//        public String getFirstSupportedVersion() {
//            return "5.5";
//        }

        @Override
        public ActivationID getModuleID() {
            return moduelID;
        }
        
    }
}
