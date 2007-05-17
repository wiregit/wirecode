package com.limegroup.gnutella.plugin;

import java.io.File;
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.java.plugin.JpfException;
import org.java.plugin.ObjectFactory;
import org.java.plugin.PluginLifecycleException;
import org.java.plugin.PluginManager;
import org.java.plugin.PluginManager.PluginLocation;
import org.java.plugin.registry.PluginDescriptor;
import org.java.plugin.registry.PluginRegistry;
import org.java.plugin.standard.StandardPluginLocation;
import org.limewire.concurrent.AtomicLazyReference;
import org.limewire.util.CommonUtils;

public class LimePluginManager {
    
    private static final Log LOG = LogFactory.getLog(LimePluginManager.class);
    
    private static final String PLUGINS = "plugins";
    
    private static final AtomicLazyReference<LimePluginManager> PLUGIN_MANAGER_REFERENCE
        = new AtomicLazyReference<LimePluginManager>() {
            @Override
            protected LimePluginManager createObject() {
                return new LimePluginManager();
            }
        };
    
    public static LimePluginManager instance() {
        return PLUGIN_MANAGER_REFERENCE.get();
    }
    
    private final PluginManager manager;
    
    private boolean running = false;
    
    private LimePluginManager() {
        manager = ObjectFactory.newInstance().createManager();
    }
    
    public PluginManager getPluginManager() {
        return manager;
    }
    
    public synchronized boolean isRunning() {
        return running;
    }
    
    /**
     * Starts the Plugin Manager
     */
    public synchronized void start() {
        if (!isRunning()) {
            try {
                PluginLocation[] locations = getPluginLocations();
                manager.publishPlugins(locations);
                startAll();
                running = true;
            } catch (JpfException err) {
                LOG.error("JpfException", err);
                err.printStackTrace();
            }
        }
    }
    
    /**
     * Stops the Plugin Manager
     */
    public synchronized void stop() {
        if (isRunning()) {
            try {
                stopAll();
                manager.shutdown();
            } finally {
                running = false;
            }
        }
    }
    
    public synchronized void startAll() {
        PluginRegistry registry = manager.getRegistry();
        Collection<PluginDescriptor> descriptors = registry.getPluginDescriptors();
        for (PluginDescriptor descriptor : descriptors) {
            if (!manager.isPluginActivated(descriptor)) {
                try {
                    manager.activatePlugin(descriptor.getId());
                } catch (PluginLifecycleException err) {
                    LOG.error("PluginLifecycleException", err);
                    err.printStackTrace();
                }
            }
        }
    }

    public synchronized void stopAll() {
        PluginRegistry registry = manager.getRegistry();
        Collection<PluginDescriptor> descriptors = registry.getPluginDescriptors();
        for (PluginDescriptor descriptor : descriptors) {
            if (manager.isPluginActivated(descriptor)) {
                manager.deactivatePlugin(descriptor.getId());
            }
        }
    }
    
    private static PluginLocation[] getPluginLocations() {
        File root = new File(CommonUtils.getUserSettingsDir(), PLUGINS);
        if (!root.exists() || !root.isDirectory()) {
            return new PluginLocation[0];
        }
        
        File[] files = root.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                if (pathname.isFile()) {
                    String name = pathname.getName().toLowerCase(Locale.US);
                    return name.endsWith(".jar") || name.endsWith(".zip");
                }
                return pathname.isDirectory();
            }
        });
        
        List<PluginLocation> locations = new ArrayList<PluginLocation>();
        for (File file : files) {
            try {
                PluginLocation loc = StandardPluginLocation.create(file);
                if (loc != null) {
                    locations.add(loc);
                }
                System.out.println(file + " -> " + loc);
            } catch (MalformedURLException err) {
                LOG.error("MalformedURLException", err);
                err.printStackTrace();
            }
        }
        
        return (PluginLocation[])locations.toArray(new PluginLocation[0]);
    }
}
