package org.limewire.activation.impl;

import java.util.List;

import org.limewire.activation.api.ActivationID;
import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationModuleEvent;
import org.limewire.listener.EventListener;

public interface ActivationModel {

    public List<ActivationItem> getActivationItems();
    
    public void setActivationItems(List<ActivationItem> items);
    
    public void load();
    
    public void save();
    
    public boolean isActive(ActivationID id);
    
    public void addModuleListener(EventListener<ActivationModuleEvent> listener);
    
    public boolean removeModuleListener(EventListener<ActivationModuleEvent> listener);
}
