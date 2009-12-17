package org.limewire.activation.api;

import java.util.List;

public interface ActivationManager {

    public List<ActivationItem> getActivationItems();
    
    public String getLicenseKey();
    
    public void activateKey(String key);
    
    /**
     * Performs analysis on the Key to perform local validation checks
     * before taking the time to hit the server. Returns true if the 
     * key is valid, false otherwise.
     */
    public boolean isValidKey(String key);
}
