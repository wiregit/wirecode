package org.limewire.activation.api;

import java.util.List;

import org.limewire.listener.EventListener;

/**
 * Handles activating and deactivating LimeWire Modules. This class
 * can be queried to return the given state of the activation process
 * or the state of a given module.
 */
public interface ActivationManager {
    
    /**
     * Returns a list of ActivationItems. Returns an EmptyCollection
     * if no ActivationItems currently exist.
     */
    public List<ActivationItem> getActivationItems();
    
    /**
     * Returns the LicenseKey associated with this LW. If no LicenseKey
     * exists, returns empty String.
     */
    public String getLicenseKey();
    
    /**
     * Returns the m_Code associated with this LicenseKey. If no LicenseKey
     * exists this will return empty String.
     */
    public String getMCode();
    
    /**
     * This activates a key that has been newly entered by the user.
     */
    public void activateKey(String key);
    
    /**
     * This refreshes the module list for a key that has already been entered by the user.
     * If there's a communication error, this will preserve the list of activation items that
     * were loaded from disk.
     */
    public void refreshKey(String key);
    
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
     * Returns true if this ActivationID currently exists and is in a Active
     * state, false otherwise.
     */
    public boolean isActive(ActivationID id);
    
    /**
     * This returns true any of the following are active: 
     *          ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE isActive ||
     *          ActivationID.OPTIMIZED_SEARCH_RESULT_MODULE isActive ||
     *          ActivationID.TECH_SUPPORT_MODULE isActive,
     *      false otherwise.
     */
    public boolean isProActive();
    
    public void addModuleListener(EventListener<ActivationModuleEvent> listener);
    
    public boolean removeModuleListener(EventListener<ActivationModuleEvent> listener);
    
    public void addListener(EventListener<ActivationEvent> listener);
    
    public boolean removeListener(EventListener<ActivationEvent> listener);
}
