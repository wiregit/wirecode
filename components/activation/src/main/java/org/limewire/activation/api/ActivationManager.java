package org.limewire.activation.api;

import java.util.List;

public interface ActivationManager {

    public List<ActivationItem> getActivationItems();
    
    public String getLicenseKey();
    
    public void activateKey(String key);
}
