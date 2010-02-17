package org.limewire.activation.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import org.limewire.activation.api.ActivationID;
import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationModuleEvent;
import org.limewire.activation.api.ActivationItem.Status;
import org.limewire.listener.EventListener;
import org.limewire.listener.AsynchronousEventMulticaster;
import org.limewire.listener.AsynchronousMulticasterImpl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
class ActivationModelImpl implements ActivationModel {

    private final Map<ActivationID, ActivationItem> itemMap = new HashMap<ActivationID, ActivationItem>(4);
    private final AsynchronousEventMulticaster<ActivationModuleEvent> listeners;
    
    @Inject
    public ActivationModelImpl(@Named("fastExecutor") Executor executor) {
        listeners = new AsynchronousMulticasterImpl<ActivationModuleEvent>(executor);
    }
    
    @Override
    public List<ActivationItem> getActivationItems() {
        synchronized (this) {
            return new ArrayList<ActivationItem>(itemMap.values());            
        }
    }
    
    @Override
    public int size() {
        synchronized(this) {
            return itemMap.size();
        }
    }
    
    @Override
    public void setActivationItems(List<ActivationItem> items) {
        setActivationItems(items, false);
    }
    
    private boolean setActivationItems(List<ActivationItem> items, boolean loadedFromDisk) {
        List<ActivationItem> oldItems;
        synchronized (this) {
            oldItems = new ArrayList<ActivationItem>(itemMap.values());
            itemMap.clear();
            for(ActivationItem item : items) {
                itemMap.put(item.getModuleID(), item);
            }
        }

        // copy the list of newly added Items.
        List<ActivationItem> eventItems = new ArrayList<ActivationItem>(items);
        
        // compare all the old items in the list to the new items added
        for(ActivationItem item: oldItems) {
            // if the old item existed in the list
            if(itemMap.containsKey(item.getModuleID())) {
                ActivationItem currentItem = itemMap.get(item.getModuleID());
                // if the status remains the same, don't notify anyone
                if(currentItem.getStatus() == item.getStatus()) {
                    eventItems.remove(currentItem);
                }
            } else {
                // if the old item doesn't exist, fire event as expired
                listeners.broadcast(new ActivationModuleEvent(item.getModuleID(), Status.EXPIRED));
            }
        }

        //fire event changes on the changed status of newly added items
        for(ActivationItem item : eventItems) {
            if(item.getModuleID() != ActivationID.UNKNOWN_MODULE)
                listeners.broadcast(new ActivationModuleEvent(item.getModuleID(), item.getStatus()));
        }
        
        return true;
    }
    
    @Override
    public boolean isActive(ActivationID id) {
        ActivationItem item;
        synchronized (this) {
            item = itemMap.get(id);            
        }
        if(item == null)
            return false;
        else
            return item.getStatus() == Status.ACTIVE;
    }
    
    @Override
    public void addListener(EventListener<ActivationModuleEvent> listener) {
        listeners.addListener(listener);
    }
    
    @Override
    public boolean removeListener(EventListener<ActivationModuleEvent> listener) {
        return listeners.removeListener(listener);
    }
}
