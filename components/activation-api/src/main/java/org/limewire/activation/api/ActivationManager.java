package org.limewire.activation.api;

import java.util.List;

import org.limewire.listener.EventListener;

public interface ActivationManager {

    public static final int PRO_MODULE_ID = 0;
    public static final int AVG_MODULE_ID = 1;
    
    public List<ActivationItem> getActivationItems();
    
    public String getLicenseKey();
    
    /**
     * This activates a key that has been newly entered by the user.
     */
    public void activateKey(String key);
    
    /**
     * This uses the key entered previously by the user to check whether
     * his modules have expired.
     */
    public void revalidateKey(String key);

    /**
     * Returns the current state of the ActivationManager.
     */
    public ActivationState getActivationState();
    
    /**
     * Returns any error that may occured when transitioning to the 
     * current ActivationState.
     */
    public ActivationError getActivationError();
    
    /**
     * Performs analysis on the Key to perform local validation checks
     * before taking the time to hit the server. Returns true if the 
     * key is valid, false otherwise.
     */
    public boolean isValidKey(String key);
        
    public boolean isActive(ActivationID id);
    
    public void addModuleListener(EventListener<ActivationModuleEvent> listener);
    
    public boolean removeModuleListener(EventListener<ActivationModuleEvent> listener);
    
    public void addListener(EventListener<ActivationEvent> listener);
    
    public boolean removeListener(EventListener<ActivationEvent> listener);
}
