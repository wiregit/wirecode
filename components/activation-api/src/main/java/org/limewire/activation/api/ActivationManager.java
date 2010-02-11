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
     * Returns the Module Code associated with this LicenseKey. If no LicenseKey
     * exists this will return empty String.
     */
    public String getModuleCode();
    
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
     * Returns whether the MCode (the string representing the user's list of paid features)
     * is up to date. This returns true if 
     * (a) the user doesn't have a license and therefore doesn't have any paid features
     * (b) the user does have a license and the activation manager has gotten the latest mcode
     *     from the activation server or there was a communication error with the activation server
     *     but the cached mcode is available.
     */
    public boolean isMCodeUpToDate();

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

    public void addMCodeListener(EventListener<ModuleCodeEvent> listener);
    
    public boolean removeMCodeListener(EventListener<ModuleCodeEvent> listener);
}
