package org.limewire.activation.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.limewire.activation.api.ActivationID;
import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationModuleEvent;
import org.limewire.activation.api.ActivationItem.Status;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;

import com.google.inject.Singleton;

@Singleton
public class ActivationModelImpl implements ActivationModel {

    private final Map<ActivationID, ActivationItem> itemMap = new HashMap<ActivationID, ActivationItem>(4);
    private final EventListenerList<ActivationModuleEvent> moduleListeners = new EventListenerList<ActivationModuleEvent>();
    
    @Override
    public List<ActivationItem> getActivationItems() {
        synchronized (this) {
            return new ArrayList<ActivationItem>(itemMap.values());            
        }
    }
    
    @Override
    public void setActivationItems(List<ActivationItem> items) {
        List<ActivationItem> oldItems;
        synchronized (this) {
            oldItems = new ArrayList<ActivationItem>(itemMap.values());
            itemMap.clear();
            for(ActivationItem item : items) {
                itemMap.put(item.getModuleID(), item);
            }
        }
        save();
        // we need to disable anything that may have been previously active
        for(ActivationItem item : oldItems) {
            if(item.getModuleID() != ActivationID.UNKNOWN_MODULE)
                moduleListeners.broadcast(new ActivationModuleEvent(item.getModuleID(), Status.EXPIRED));
        }
        // activate any new items that were added
        for(ActivationItem item : items) {
            if(item.getModuleID() != ActivationID.UNKNOWN_MODULE)
                moduleListeners.broadcast(new ActivationModuleEvent(item.getModuleID(), item.getStatus()));
        }
    }
    
    @Override
    public void load() {
        
    }
    
    @Override
    public void save() {
        
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
            return item.getStatus() == Status.ACTIVE;
    }
    
    @Override
    public void addModuleListener(EventListener<ActivationModuleEvent> listener) {
        moduleListeners.addListener(listener);
    }
    
    @Override
    public boolean removeModuleListener(EventListener<ActivationModuleEvent> listener) {
        return moduleListeners.removeListener(listener);
    }
}
