package org.limewire.activation;

import java.util.List;

import org.limewire.activation.api.ActivationError;
import org.limewire.activation.api.ActivationEvent;
import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationManager;
import org.limewire.activation.api.ActivationModuleEvent;
import org.limewire.activation.api.ActivationState;
import org.limewire.listener.EventListener;

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

    @Override
    public void addListener(EventListener<ActivationEvent> listener) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public ActivationError getActivationError() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ActivationState getActivationState() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean removeListener(EventListener<ActivationEvent> listener) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void addModuleListener(EventListener<ActivationModuleEvent> listener) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isActive(int id) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean removeModuleListener(EventListener<ActivationModuleEvent> listener) {
        // TODO Auto-generated method stub
        return false;
    }
}
