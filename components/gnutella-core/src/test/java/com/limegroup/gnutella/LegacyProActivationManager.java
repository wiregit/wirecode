package com.limegroup.gnutella;

import java.util.List;
import java.util.EnumSet;

import org.limewire.activation.api.ActivationManager;
import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationState;
import org.limewire.activation.api.ActivationError;
import org.limewire.activation.api.ActivationID;
import org.limewire.activation.api.ActivationModuleEvent;
import org.limewire.activation.api.ActivationEvent;
import org.limewire.activation.api.ModuleCodeEvent;
import org.limewire.listener.EventListener;

/**
 * impl of legacy "pro"
 */
public class LegacyProActivationManager implements ActivationManager {
    
    private EnumSet<ActivationID> activeFeatures = EnumSet.noneOf(ActivationID.class);
    
    public void setLegacyPro(boolean isPro) {
        if (isPro) {
            // add in optimized search results and faster downloads as supported features
            activeFeatures = EnumSet.of(ActivationID.OPTIMIZED_SEARCH_RESULT_MODULE, 
                                        ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE,
                                        ActivationID.TECH_SUPPORT_MODULE);
        } else {
            activeFeatures = EnumSet.noneOf(ActivationID.class);    
        }
        
    }
    
    @Override
    public boolean isActive(ActivationID id) {
        return activeFeatures.contains(id);
    }

    @Override
    public boolean isProActive() {
        return !activeFeatures.isEmpty();
    }
    
    // default skeleton impls of interface.
    @Override public List<ActivationItem> getActivationItems() { return null; }
    @Override public String getLicenseKey() { return null; }
    @Override public String getModuleCode() { return null; }
    @Override public void activateKey(String key) { }
    @Override public void refreshKey(String key) { }
    @Override public ActivationState getActivationState() { return null; }
    @Override public ActivationError getActivationError() { return null; }
    @Override public void addModuleListener(EventListener<ActivationModuleEvent> listener) { }
    @Override public boolean removeModuleListener(EventListener<ActivationModuleEvent> listener) { return false; }
    @Override public void addListener(EventListener<ActivationEvent> listener) { }
    @Override public boolean removeListener(EventListener<ActivationEvent> listener) { return false; }
    @Override public void addMCodeListener(EventListener<ModuleCodeEvent> listener) {}
    @Override public boolean isMCodeUpToDate() { return false; }
    @Override public boolean removeMCodeListener(EventListener<ModuleCodeEvent> listener) { return false; }
}
