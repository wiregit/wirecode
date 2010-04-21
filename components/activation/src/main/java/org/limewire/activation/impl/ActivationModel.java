package org.limewire.activation.impl;

import java.util.List;

import org.limewire.activation.api.ActivationID;
import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationModuleEvent;
import org.limewire.listener.EventListener;

/**
 * Container for current Modules that are known about and loaded
 * within LimeWire. Given Module information may be looked up 
 * using the ActivationID. Events are fired when changes to the
 * model occur. 
 */
interface ActivationModel {

    /**
     * Returns a copy of the List of ActivationItems loaded by LimeWire. A loaded
     * ActivationItem does not ensure the item is Activated. If no
     * items are loaded an empty list is returned.
     */
    public List<ActivationItem> getActivationItems();
    
    /**
     * Replaces the current list of ActivationItems with this new list
     * of ActivationItems. This will result an events being broadcast
     * of the changes.
     */
    public void setActivationItems(List<ActivationItem> items);
    
    /**
     * Returns the number of current Modules loaded in ActivationModel.
     */
    public int size();
    
    /**
     * Returns true if a Module is loaded that has this ActivationID and
     * this Module is Active, false otherwise.
     */
    public boolean isActive(ActivationID id);
    
    /**
     * Adds a listener that will receive ActivationModuleEvents anytime the
     * current ActivationModel changes state.
     */
    public void addListener(EventListener<ActivationModuleEvent> listener);
    
    /**
     * Removes a listener for ActivationModuleEvents.
     */
    public boolean removeListener(EventListener<ActivationModuleEvent> listener);
}
