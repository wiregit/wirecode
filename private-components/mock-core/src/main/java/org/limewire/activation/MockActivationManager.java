package org.limewire.activation;

import java.util.List;

import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationManager;

public class MockActivationManager implements ActivationManager {

    @Override
    public void activateKey(String key) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public List<ActivationItem> getActivationItems() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getLicenseKey() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isValidKey(String key) {
        // TODO Auto-generated method stub
        return false;
    }

}
