package com.limegroup.gnutella.plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.felix.framework.Felix;
import org.apache.felix.framework.cache.BundleCache;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.framework.util.MutablePropertyResolverImpl;
import org.apache.felix.framework.util.StringMap;
import org.limewire.util.CommonUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class PluginManager {
    
    private static final Log LOG = LogFactory.getLog(PluginManager.class);
    
    private final Felix felix;
    
    private final CorePluginContext pluginContext;
    
    private final CorePluginActivator pluginActivator;
    
    public PluginManager() {
        felix = new Felix();
        pluginContext = new CorePluginContext();
        pluginActivator = new CorePluginActivator(pluginContext);
        
        Map configMap = getConfigMap();
        
        List<BundleActivator> activators = new ArrayList<BundleActivator>();
        activators.add(pluginActivator);
        
        felix.start(new MutablePropertyResolverImpl(configMap), 
                activators);
    }
    
    @SuppressWarnings("unchecked")
    private static Map getConfigMap() {
        Map configMap = new StringMap(false);
        configMap.put(FelixConstants.EMBEDDED_EXECUTION_PROP, "true");
        
        configMap.put(BundleCache.CACHE_BUFSIZE_PROP, "4096");
        configMap.put(BundleCache.CACHE_PROFILE_DIR_PROP, getCacheProfileDir());
        
        //configMap.put(Constants.FRAMEWORK_BOOTDELEGATION, "javax.swing");
        
        configMap.put(Constants.FRAMEWORK_SYSTEMPACKAGES,
                "org.osgi.framework; version=1.3.0," +
                "org.osgi.service.packageadmin; version=1.2.0," +
                "org.osgi.service.startlevel; version=1.0.0," +
                "org.osgi.service.url; version=1.0.0," +
                "host.service.lookup; version=1.0.0," /*+
                "javax.swing;"*/);
        
        return configMap;
    }
    
    private static String getCacheProfileDir() {
        File dir = new File(CommonUtils.getUserSettingsDir(), "osgi-cache");
        return dir.getAbsolutePath();
    }
    
    public void launch() {
        BundleContext bundleContext = pluginActivator.getBundleContext();
        
        try {
            String location = "file:///Users/roger/plugins/SamplePlugin.jar";
            //String location = "file:///Users/roger/plugins/SamplePluginTwo_1.0.0.jar";
            Bundle bundle = bundleContext.installBundle(location);
            bundle.start();
        } catch (BundleException err) {
            err.printStackTrace();
        }
    }
    
    public void uninstallAll() {
        if (felix != null) {
            BundleContext bundleContext = pluginActivator.getBundleContext();
            for (Bundle bundle : bundleContext.getBundles()) {
                try {
                    bundle.uninstall();
                } catch (BundleException err) {
                    LOG.error("BundleException", err);
                }
            }
            felix.shutdown();
        }
    }
    
    public void stopAll() {
        if (felix != null) {
            BundleContext bundleContext = pluginActivator.getBundleContext();
            for (Bundle bundle : bundleContext.getBundles()) {
                try {
                    bundle.stop();
                } catch (BundleException err) {
                    LOG.error("BundleException", err);
                }
            }
            felix.shutdown();
        }
    }
    
    public void shutdown() {
        if (felix != null) {
            stopAll();
            uninstallAll();
            felix.shutdown();
        }
    }
    
    public static void main(String[] args) {
        PluginManager manager = null;
        try {
            manager = new PluginManager();
            manager.launch();
        } finally {
            //manager.shutdown();
        }
    }
}
