package org.limewire.activation;

import java.util.Collections;
import java.util.List;

import org.limewire.activation.api.ActivationError;
import org.limewire.activation.api.ActivationEvent;
import org.limewire.activation.api.ActivationID;
import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationManager;
import org.limewire.activation.api.ActivationModuleEvent;
import org.limewire.activation.api.ActivationState;
import org.limewire.activation.api.ModuleCodeEvent;
import org.limewire.listener.EventListener;

public class MockActivationManager implements ActivationManager {

    @Override
    public void activateKey(String key) {
        
    }

    @Override
    public List<ActivationItem> getActivationItems() {
        return Collections.emptyList();
    }

    @Override
    public String getLicenseKey() {
        return "";
    }
    
    @Override
    public String getModuleCode() {
        return "";
    }

    @Override
    public void addListener(EventListener<ActivationEvent> listener) {        
    }

    @Override
    public ActivationError getActivationError() {
        return ActivationError.NO_ERROR;
    }

    @Override
    public ActivationState getActivationState() {
        return ActivationState.NOT_AUTHORIZED;
    }

    @Override
    public boolean removeListener(EventListener<ActivationEvent> listener) {
        return false;
    }

    @Override
    public void addModuleListener(EventListener<ActivationModuleEvent> listener) {
    }

    @Override
    public boolean isActive(ActivationID id) {
        return false;
    }

    @Override
    public boolean removeModuleListener(EventListener<ActivationModuleEvent> listener) {
        return false;
    }

    @Override
    public boolean isProActive() {
        return false;
    }

    @Override
    public void refreshKey(String key) {
        
    }

    @Override
    public boolean isMCodeUpToDate() {
        return false;
    }
    
    @Override
    public void addMCodeListener(EventListener<ModuleCodeEvent> listener) {}
    
    @Override
    public boolean removeMCodeListener(EventListener<ModuleCodeEvent> listener) { return false; }

}
