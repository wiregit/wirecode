package org.limewire.activation.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.limewire.activation.api.ActivationID;
import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationModuleEvent;
import org.limewire.activation.api.ActivationItem.Status;
import org.limewire.activation.serial.ActivationMemento;
import org.limewire.activation.serial.ActivationSerializer;
import org.limewire.concurrent.ListeningFuture;
import org.limewire.concurrent.SimpleFuture;
import org.limewire.io.InvalidDataException;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ActivationModelImpl implements ActivationModel {

    private final Map<ActivationID, ActivationItem> itemMap = new HashMap<ActivationID, ActivationItem>(4);
    private final EventListenerList<ActivationModuleEvent> listeners = new EventListenerList<ActivationModuleEvent>();
    
    private AtomicBoolean hasContactedServer = new AtomicBoolean(false);
    
    private final ActivationSerializer serializer;
    private final ActivationItemFactory activationItemFactory;
    
    @Inject
    public ActivationModelImpl(ActivationSerializer serializer, ActivationItemFactory activationItemFactory) {
        this.serializer = serializer;
        this.activationItemFactory = activationItemFactory;
    }
    
    @Override
    public List<ActivationItem> getActivationItems() {
        synchronized (this) {
            return new ArrayList<ActivationItem>(itemMap.values());            
        }
    }
    
    @Override
    public int size() {
        return itemMap.size();
    }
    
    @Override
    public void setActivationItems(List<ActivationItem> items) {
        setActivationItems(items, false);
    }
    
    private boolean setActivationItems(List<ActivationItem> items, boolean loadedFromDisk) {
        List<ActivationItem> oldItems;
        synchronized (this) {
            // if the server has already been contacted, don't override the with old data
            // from disk.
            if(loadedFromDisk && hasContactedServer.get())
                return false;
            if(!loadedFromDisk)
                hasContactedServer.set(true);

            oldItems = new ArrayList<ActivationItem>(itemMap.values());
            itemMap.clear();
            for(ActivationItem item : items) {
                itemMap.put(item.getModuleID(), item);
            }
        }
        if(!loadedFromDisk)
            save();
        // we need to disable anything that may have been previously active
        for(ActivationItem item : oldItems) {
            if(item.getModuleID() != ActivationID.UNKNOWN_MODULE)
                listeners.broadcast(new ActivationModuleEvent(item.getModuleID(), Status.EXPIRED));
        }
        // activate any new items that were added
        for(ActivationItem item : items) {
            if(item.getModuleID() != ActivationID.UNKNOWN_MODULE)
                listeners.broadcast(new ActivationModuleEvent(item.getModuleID(), item.getStatus()));
        }
        
        return true;
    }
    
    @Override
    public ListeningFuture<Boolean> load() {
        if(hasContactedServer.get())
            return new SimpleFuture<Boolean>(false);
        List<ActivationMemento> mementos;
        
        try {
            mementos = serializer.readFromDisk();
        } catch(IOException e) {
            mementos = Collections.emptyList();
        }
        
        boolean added = false;
        if(mementos.size() > 0) {
            List<ActivationItem> activationItems = new ArrayList<ActivationItem>(mementos.size());
            for(ActivationMemento memento : mementos) {
                try {
                    // create ActivationItem
                    // add it to activationItems list
                    ActivationItem item = activationItemFactory.createActivationItem(memento);
                    activationItems.add(item);
                } catch (InvalidDataException e) {
                    
                }
            }
            // add this list to the 
            added = setActivationItems(activationItems, true);
        }
        return new SimpleFuture<Boolean>(added);
    }
    
    @Override
    public void save() {
        if(!hasContactedServer.get())
            return;
        
        List<ActivationMemento> mementos;
        synchronized (this) {
            mementos = new ArrayList<ActivationMemento>(itemMap.size());
            for(ActivationItem item : itemMap.values()) {
                if(item instanceof ActivationItemImpl) {
                    ActivationItemImpl itemImpl = (ActivationItemImpl) item;
                    if(itemImpl.isMementoSupported()) {
                        mementos.add(itemImpl.toActivationMemento());
                    }
                }
            }
        }
        
        serializer.writeToDisk(mementos);
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
