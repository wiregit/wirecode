package org.limewire.activation.api;

import java.util.List;

import org.limewire.listener.EventListener;

public interface ActivationManager {

    public List<ActivationItem> getActivationItems();
    
    public String getLicenseKey();
    
    public void activateKey(String key);
    
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
    
    /**
     * Returns true if 
     * @return
     */
    public boolean isPro();
    
    public void addListener(EventListener<ActivationEvent> listener);
    
    public boolean removeListener(EventListener<ActivationEvent> listener);
}
