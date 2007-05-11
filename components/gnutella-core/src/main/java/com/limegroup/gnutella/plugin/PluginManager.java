package com.limegroup.gnutella.plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.felix.framework.Felix;
import org.apache.felix.framework.cache.BundleCache;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.framework.util.MutablePropertyResolverImpl;
import org.apache.felix.framework.util.StringMap;
import org.limewire.concurrent.AtomicLazyReference;
import org.limewire.io.IOUtils;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

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
    
    private final Felix platform;
    
    private final CorePluginContext pluginContext;
    
    private final CorePluginActivator pluginActivator;
    
    private PluginManager() {
        platform = new Felix();
        pluginContext = new CorePluginContext();
        pluginActivator = new CorePluginActivator(pluginContext);
    }
    
    /**
     * Returns true if the Plugin Manager is running
     */
    public synchronized boolean isRunning() {
        return platform.getStatus() == Felix.RUNNING_STATUS;
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
            
            platform.start(new MutablePropertyResolverImpl(configMap), list);
        }
    }
    
    /**
     * Stops the Plugin Manager
     */
    public synchronized void stop() {
        if (isRunning()) {
            stopAll();
            uninstallAll();
            platform.shutdown();
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
        
        //configMap.put(Constants.FRAMEWORK_BOOTDELEGATION, getFrameworkBootDelegation());
        
        /*configMap.put(Constants.FRAMEWORK_SYSTEMPACKAGES,
                "org.osgi.framework; version=1.3.0," +
                "org.osgi.service.packageadmin; version=1.2.0," +
                "org.osgi.service.startlevel; version=1.0.0," +
                "org.osgi.service.url; version=1.0.0");*/
        
        InputStream in = null;
        try {
            in = CommonUtils.getResourceStream("com/limegroup/gnutella/plugin/framework-systempackages.properties");
            if (in != null) {
                Properties props = new Properties();
                props.load(in);
                for (Object key : props.keySet()) {
                    configMap.put(((String)key).trim(), 
                            props.getProperty((String)key).trim());
                }
            }
        } catch (IOException err) {
            err.printStackTrace();
        } finally {
            IOUtils.close(in);
        }
        
        return configMap;
    }
    
    /*private static String getFrameworkBootDelegation() {
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
            "org.limewire.*"
        };
        
        StringBuilder buffer = new StringBuilder();
        for (String str : delegate) {
            buffer.append(str).append(",");
        }
        return buffer.toString();
    }*/
    
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
    
    public static void main(String[] args) throws Exception {
        /*String s = generateProperties();
        BufferedWriter out = new BufferedWriter(new FileWriter("framework-systempackages.properties"));
        out.write(s);
        out.close();*/
        
        PluginManager manager = null;
        try {
            manager = new PluginManager();
            manager.start();
            
            manager.test();
            
        } finally {
            //manager.shutdown();
        }
    }
    
    /*public static String generateProperties() {
        //final String delim = " \\\n";
        final String delim = " ";
        
        StringBuilder buffer = new StringBuilder();
        //buffer.append(Constants.FRAMEWORK_SYSTEMPACKAGES).append("=").append(delim);
        buffer.append("org.osgi.framework; version=1.3.0,").append(delim)
            .append("org.osgi.service.packageadmin; version=1.2.0,").append(delim)
            .append("org.osgi.service.startlevel; version=1.0.0,").append(delim)
            .append("org.osgi.service.url; version=1.0.0,").append(delim);
        
        BufferedReader in = null;
        try {
            // The package-list comes with the J2SE JavaDoc!
            File pkgList = new File("/Developer/Documentation/j2sdk-1.6/api/package-list");
            in = new BufferedReader(new FileReader(pkgList));
            String line = null;
            while((line = in.readLine()) != null) {
                buffer.append(line).append(",").append(delim);
            }
        } catch (IOException err) {
            throw new RuntimeException(err);
        } finally {
            IOUtils.close(in);
        }
        
        File dir = new File("/Users/roger/Documents/workspace/mainline/bin");
        packages(buffer, dir, dir, delim);
        
        if (buffer.length() > 0) {
            buffer.setLength(buffer.length()-1);
        }
        
        return buffer.toString();
    }
    
    private static void packages(StringBuilder buffer, File root, File dir, String delim) {
        File[] files = dir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                if (pathname.isFile()) {
                    return !pathname.getName().startsWith(".");
                }
                return false;
            }
        });
        
        if (files.length > 0 && !root.equals(dir)) {
            String pkg = dir.getAbsolutePath().substring(root.getAbsolutePath().length()+1);
            pkg = pkg.replace('/', '.');
            buffer.append(pkg).append(",").append(delim);
        }
        
        File[] dirs = dir.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                if (pathname.isDirectory()) {
                    String path = pathname.getAbsolutePath();
                    String name = pathname.getName();
                    return !name.contains("test") 
                            && (path.contains("com") || path.contains("org"));
                }
                return false;
            }
        });
        
        for (File trav : dirs) {
            packages(buffer, root, trav, delim);
        }
    }*/
}
