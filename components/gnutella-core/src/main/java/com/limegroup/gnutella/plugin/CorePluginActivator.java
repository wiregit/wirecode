package com.limegroup.gnutella.plugin;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class CorePluginActivator implements BundleActivator {

    private final CorePluginContext pluginContext;
    
    private BundleContext bundleContext;
    
    private ServiceRegistration registration;
    
    public CorePluginActivator(CorePluginContext pluginContext) {
        this.pluginContext = pluginContext;
    }
    
    public void start(BundleContext bundleContext) throws Exception {
        this.bundleContext = bundleContext;
        registration = bundleContext.registerService(
                CorePluginContext.class.getName(), pluginContext, null);
    }

    public void stop(BundleContext bundleContext) throws Exception {
        this.bundleContext = bundleContext;
        
        registration.unregister();
        bundleContext = null;
        registration = null;
    }
    
    public BundleContext getBundleContext() {
        return bundleContext;
    }
}
