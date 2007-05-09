package com.limegroup.gnutella.plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.felix.framework.Felix;
import org.apache.felix.framework.cache.BundleCache;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.framework.util.MutablePropertyResolverImpl;
import org.apache.felix.framework.util.StringMap;
import org.limewire.concurrent.AtomicLazyReference;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

import com.limegroup.gnutella.settings.PluginSettings;

public class PluginManager {
    
    private static final Log LOG = LogFactory.getLog(PluginManager.class);
    
    private static final AtomicLazyReference<PluginManager> PLUGIN_MANAGER_REFERENCE
        = new AtomicLazyReference<PluginManager>() {
            @Override
            protected PluginManager createObject() {
                return new PluginManager();
            }
        };
    
    public static PluginManager instance() {
        return PLUGIN_MANAGER_REFERENCE.get();
    }
    
    private final Felix felix;
    
    private final CorePluginContext pluginContext;
    
    private final CorePluginActivator pluginActivator;
    
    private PluginManager() {
        felix = new Felix();
        pluginContext = new CorePluginContext();
        pluginActivator = new CorePluginActivator(pluginContext);
    }
    
    /**
     * Returns true if the Plugin Manager is running
     */
    public synchronized boolean isRunning() {
        return felix.getStatus() == Felix.RUNNING_STATUS;
    }
    
    /**
     * Starts the Plugin Manager
     */
    public synchronized void start(BundleActivator... activators) {
        if (!isRunning()) {
            Map configMap = getConfigMap();
            
            List<BundleActivator> list = new ArrayList<BundleActivator>();
            list.add(pluginActivator);
            list.addAll(Arrays.asList(activators));
            
            felix.start(new MutablePropertyResolverImpl(configMap), list);
        }
    }
    
    /**
     * Stops the Plugin Manager
     */
    public synchronized void stop() {
        if (isRunning()) {
            stopAll();
            uninstallAll();
            felix.shutdown();
        }
    }
    
    /**
     * Returns the BundleContext
     * 
     * @throws IllegalStateException if PluginManager is not running
     */
    public synchronized BundleContext getBundleContext() 
            throws IllegalStateException {
        if (!isRunning()) {
            throw new IllegalStateException("Plugin Interface is not running");
        }
        return pluginActivator.getBundleContext();
    }
    
    /**
     * Starts all installed Bundles
     */
    public synchronized void startAll() {
        BundleContext bundleContext = getBundleContext();
        for (Bundle bundle : bundleContext.getBundles()) {
            try {
                bundle.start();
            } catch (BundleException err) {
                LOG.error("BundleException", err);
            }
        }
    }
    
    /**
     * Stops all installed Bundles
     */
    public synchronized void stopAll() {
        BundleContext bundleContext = getBundleContext();
        for (Bundle bundle : bundleContext.getBundles()) {
            try {
                bundle.stop();
            } catch (BundleException err) {
                LOG.error("BundleException", err);
            }
        }
    }
    
    /**
     * Uninstalls all installed Bundles
     */
    public synchronized void uninstallAll() {
        BundleContext bundleContext = getBundleContext();
        for (Bundle bundle : bundleContext.getBundles()) {
            try {
                bundle.uninstall();
            } catch (BundleException err) {
                LOG.error("BundleException", err);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private static Map getConfigMap() {
        Map configMap = new StringMap(false);
        configMap.put(FelixConstants.EMBEDDED_EXECUTION_PROP, "true");
        
        configMap.put(BundleCache.CACHE_BUFSIZE_PROP, "4096");
        
        File cacheDir = getCacheProfileDir();
        if (PluginSettings.CACHE_PROFILE_DIR.getValue()
                && cacheDir.exists() && cacheDir.isDirectory()) {
            FileUtils.deleteRecursive(cacheDir);
        }
        configMap.put(BundleCache.CACHE_PROFILE_DIR_PROP, cacheDir.getAbsolutePath());
        
        configMap.put(Constants.FRAMEWORK_BOOTDELEGATION, getFrameworkBootDelegation());
        
        configMap.put(Constants.FRAMEWORK_SYSTEMPACKAGES,
                "org.osgi.framework; version=1.3.0," +
                "org.osgi.service.packageadmin; version=1.2.0," +
                "org.osgi.service.startlevel; version=1.0.0," +
                "org.osgi.service.url; version=1.0.0");
        
        return configMap;
    }
    
    private static String getFrameworkBootDelegation() {
        // TODO Use Boot-Delegation which supports wild-cards
        // or 'FRAMEWORK_SYSTEMPACKAGES' where you've to specify
        // every package you'd like to make accessable for the
        // Plugins. Both have pros and cons...
        String[] delegate = {
            "javax.swing.*",
            "org.ietf.*",
            "org.omg.*",
            "org.w3c.*",
            "org.xml.*",
            "sun.*",
            "com.limegroup.gnutella.*",
            "com.limegroup.bittorrent.*",
            "org.limewire.collection.*",
            "org.limewire.common.*",
            "org.limewire.io.*",
            "org.limewire.mojito.*",
            "org.limewire.nio.*",
            "org.limewire.resources.*",
            "org.limewire.rudp.*",
            "org.limewire.security.*",
            "org.limewire.setting.*",
            "org.limewire.statistic.*",
        };
        
        StringBuilder buffer = new StringBuilder();
        for (String str : delegate) {
            buffer.append(str).append(",");
        }
        return buffer.toString();
    }
    
    private static File getCacheProfileDir() {
        return new File(CommonUtils.getUserSettingsDir(), "osgi-cache");
    }
    
    public void test() {
        try {
            BundleContext bundleContext = getBundleContext();
            
            String location = "file:///Users/roger/plugins/SamplePlugin.jar";
            //String location = "file:///Users/roger/plugins/SamplePluginTwo_1.0.0.jar";
            Bundle bundle = bundleContext.installBundle(location);
            bundle.start();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (BundleException e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) throws BundleException {
        PluginManager manager = null;
        try {
            manager = new PluginManager();
            manager.start();
            
            manager.test();
            
        } finally {
            //manager.shutdown();
        }
    }
}
