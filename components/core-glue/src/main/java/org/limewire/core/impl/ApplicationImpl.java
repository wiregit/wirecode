package org.limewire.core.impl;

import org.limewire.core.api.Application;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.LifecycleManager;
import com.limegroup.gnutella.util.LimeWireUtils;

@Singleton
class ApplicationImpl implements Application {
    
    private final ApplicationServices applicationServices;
    private final LifecycleManager lifecycleManager;
    
    @Inject
    public ApplicationImpl(ApplicationServices applicationServices, LifecycleManager lifecycleManager) {
        this.applicationServices = applicationServices;
        this.lifecycleManager = lifecycleManager;
    }
    
    @Override
    public String getUniqueUrl(String baseUrl) {
        return LimeWireUtils.addLWInfoToUrl(baseUrl, applicationServices.getMyGUID());
    }
    
    @Override
    public void startCore() {
        lifecycleManager.start();
    }
    
    @Override
    public void stopCore() {
        lifecycleManager.shutdown();
    }
    

}
