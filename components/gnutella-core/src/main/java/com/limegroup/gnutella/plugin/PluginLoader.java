package com.limegroup.gnutella.plugin;

import java.io.File;
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.java.plugin.JpfException;
import org.java.plugin.ObjectFactory;
import org.java.plugin.PluginManager;
import org.java.plugin.PluginManager.PluginLocation;
import org.java.plugin.registry.PluginDescriptor;
import org.java.plugin.registry.PluginRegistry;
import org.java.plugin.standard.StandardPluginLocation;
import org.limewire.concurrent.AtomicLazyReference;
import org.limewire.service.ErrorService;
import org.limewire.util.CommonUtils;

import com.limegroup.gnutella.settings.PluginSettings;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * Provides a simple interface to load LimeWire Plugins. For advanced
 * Plugin operations you may use the actual PluginManager.
 * 
 * The lookup order of the Plugins is as follows:
 * 
 * ../lib/plugins
 * ./plugins
 * ~/plugins
 * [path to settings]/plugins
 * [custom path]
 * 
 * Plugins that have the same ID are loaded in first come, first 
 * served basis.
 */
public class PluginLoader {
    
    private static final Log LOG = LogFactory.getLog(PluginLoader.class);
    
    /**
     * Name of the Plugins directory
     */
    private static final String PLUGINS_DIR = "plugins";
    
    /**
     * An AtomicLazyReference of the PluginLoader
     */
    private static final AtomicLazyReference<PluginLoader> PLUGIN_LOADER_REFERENCE
        = new AtomicLazyReference<PluginLoader>() {
            @Override
            protected PluginLoader createObject() {
                return new PluginLoader();
            }
        };
    
    /**
     * Returns the singleton instance of the PluginLoader
     */
    public static PluginLoader instance() {
        return PLUGIN_LOADER_REFERENCE.get();
    }
    
    /**
     * A handle of the PluginManager
     */
    private final PluginManager manager;
    
    /**
     * Flag for whether or not the PluginLoader is running
     */
    private boolean running = false;
    
    private PluginLoader() {
        manager = ObjectFactory.newInstance().createManager();
    }
    
    /**
     * Returns the PluginManager
     */
    public PluginManager getPluginManager() {
        return manager;
    }
    
    /**
     * Returns true if the PluginLoader is running
     */
    public synchronized boolean isRunning() {
        return running;
    }
    
    /**
     * Starts the PluginLoader and publishes all Plugins
     * but doesn't start them.
     */
    public synchronized void launch() {
        if (!isRunning()) {
            try {
                PluginLocation[] locations = getPluginLocations();
                manager.publishPlugins(locations);
                running = true;
            } catch (JpfException err) {
                LOG.error("JpfException", err);
                ErrorService.error(err);
            }
        }
    }
    
    /**
     * Stops the PluginLoader and all Plugins
     */
    public synchronized void shutdown() {
        if (isRunning()) {
            try {
                stopAll();
                manager.shutdown();
            } finally {
                running = false;
            }
        }
    }
    
    /**
     * Starts all Plugins
     */
    public synchronized void startAll() {
        PluginRegistry registry = manager.getRegistry();
        Collection<PluginDescriptor> descriptors = registry.getPluginDescriptors();
        for (PluginDescriptor descriptor : descriptors) {
            if (!manager.isPluginActivated(descriptor)) {
                try {
                    if (LOG.isTraceEnabled()) { LOG.trace("BEGIN ACTIVATE: " + descriptor); }
                    manager.activatePlugin(descriptor.getId());
                    if (LOG.isTraceEnabled()) { LOG.trace("END ACTIVATE: " + descriptor); }
                } catch (Throwable err) {
                    LOG.error("Throwable", err);
                    ErrorService.error(err);
                }
            }
        }
    }

    /**
     * Stops all Plugins
     */
    public synchronized void stopAll() {
        PluginRegistry registry = manager.getRegistry();
        Collection<PluginDescriptor> descriptors = registry.getPluginDescriptors();
        for (PluginDescriptor descriptor : descriptors) {
            if (manager.isPluginActivated(descriptor)) {
                manager.deactivatePlugin(descriptor.getId());
            }
        }
    }
    
    /**
     * Returns all available Plugins that are found in the various paths
     */
    private static PluginLocation[] getPluginLocations() {
        List<PluginLocation> locations = new ArrayList<PluginLocation>();
        
        // "../lib/plugins" -- DEVELOPMENT ONLY
        if (LimeWireUtils.isCVS()) {
            File libPluginsDir = new File("../lib/", PLUGINS_DIR);
            locations.addAll(Arrays.asList(getPluginLocations(libPluginsDir)));
        }
        
        // "./plugins"
        File mainPluginsDir = new File(PLUGINS_DIR);
        locations.addAll(Arrays.asList(getPluginLocations(mainPluginsDir)));
        
        // "~/plugins"
        File userPluginsDir = new File(CommonUtils.getUserHomeDir(), PLUGINS_DIR);
        locations.addAll(Arrays.asList(getPluginLocations(userPluginsDir)));
        
        // "<settings>/plugins"
        File settingsPluginsDir = new File(CommonUtils.getUserSettingsDir(), PLUGINS_DIR);
        locations.addAll(Arrays.asList(getPluginLocations(settingsPluginsDir)));
        
        // "<custom path>"
        if (!PluginSettings.CUSTOM_PLUGINS_PATH.isDefault()) {
            String customPluginsPath = PluginSettings.CUSTOM_PLUGINS_PATH.getValue();
            locations.addAll(Arrays.asList(getPluginLocations(new File(customPluginsPath))));
        }
        
        return (PluginLocation[])locations.toArray(new PluginLocation[0]);
    }
    
    /**
     * Returns all plugins that are found in the given Path
     */
    private static PluginLocation[] getPluginLocations(File path) {
        if (path == null || !path.exists()) {
            return new PluginLocation[0];
        }
        
        File[] files = null;
        if (path.isDirectory() && path.getName().equals(PLUGINS_DIR)) {
            final String[] extensions = PluginSettings.PLUGIN_EXTENSIONS.getValue();
            
            files = path.listFiles(new FileFilter() {
                public boolean accept(File pathname) {
                    if (pathname.isFile()) {
                        String name = pathname.getName().toLowerCase(Locale.US);
                        for (String ext : extensions) {
                            if (name.endsWith(ext)) {
                                return true;
                            }
                        }
                    }
                    return pathname.isDirectory();
                }
            });
        } else if (path.isFile()) {
            files = new File[] { path };
        }
        
        if (files == null) {
            return new PluginLocation[0];
        }
        
        List<PluginLocation> locations = new ArrayList<PluginLocation>();
        for (File file : files) {
            try {
                PluginLocation loc = StandardPluginLocation.create(file);
                if (loc != null) {
                    locations.add(loc);
                }
                
                if (LOG.isInfoEnabled()) {
                    LOG.info("Plugin: " + file + " -> " + loc);
                }
                
            } catch (MalformedURLException err) {
                LOG.error("MalformedURLException", err);
            }
        }
        
        return (PluginLocation[])locations.toArray(new PluginLocation[0]);
    }
}
