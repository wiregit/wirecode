package com.limegroup.gnutella.plugin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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
import org.limewire.io.IOUtils;
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
    
    private static final String PACKAGE_LIST = "package-list";
    
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
        
        InputStream in = null;
        try {
            in = CommonUtils.getResourceStream(getLocation());
            if (in != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                
                StringBuilder buffer = new StringBuilder();
                String line = null;
                while((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.length() == 0) {
                        continue;
                    }
                    
                    if (line.startsWith("#")) {
                        continue;
                    }
                    
                    buffer.append(line).append(",");
                }
                
                configMap.put(Constants.FRAMEWORK_SYSTEMPACKAGES, buffer.toString());
            }
        } catch (IOException err) {
            LOG.error("IOException", err);
        } finally {
            IOUtils.close(in);
        }
        
        return configMap;
    }
    
    private static String getLocation() {
        Package pkg = PluginManager.class.getPackage();
        String location = pkg.getName().replace('.', '/') + "/" + PACKAGE_LIST;
        //System.out.println(location);
        return location;
    }
    
    private static File getCacheProfileDir() {
        return new File(CommonUtils.getUserSettingsDir(), "osgi-cache");
    }
    
    public void test() {
        try {
            BundleContext bundleContext = getBundleContext();
            
            //String location = "file:///Users/roger/plugins/SamplePlugin.jar";
            //String location = "file:///Users/roger/plugins/SamplePluginTwo_1.0.0.jar";
            //Bundle bundle = bundleContext.installBundle(location);
            
            FileInputStream fis = new FileInputStream(new File("/Users/roger/plugins/SamplePlugin.jar"));
            Bundle bundle = bundleContext.installBundle("FooBar", fis);
            bundle.start();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (BundleException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) throws Exception {
        /*String s = generatePackageList();
        BufferedWriter out = new BufferedWriter(new FileWriter(PACKAGE_LIST));
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
    
    public static String generatePackageList() {
        StringBuilder buffer = new StringBuilder();
        
        buffer.append("# ").append(new Date()).append("\n");
        
        buffer.append("\n# Default OSGi Packages\n");
        buffer.append("org.osgi.framework; version=1.3.0").append("\n")
            .append("org.osgi.service.packageadmin; version=1.2.0").append("\n")
            .append("org.osgi.service.startlevel; version=1.0.0").append("\n")
            .append("org.osgi.service.url; version=1.0.0").append("\n");
        
        buffer.append("\n# All public non java.* Packages\n");
        BufferedReader in = null;
        try {
            // The package-list comes with the J2SE JavaDoc!
            File pkgList = new File("/Developer/Documentation/j2sdk-1.6/api/package-list");
            in = new BufferedReader(new FileReader(pkgList));
            String line = null;
            while((line = in.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0) {
                    continue;
                }
                
                // Ignore all java.* Packages because they're
                // automatically exported by OSGi!
                if (line.startsWith("java.")) {
                    continue;
                }
                
                buffer.append(line).append("\n");
            }
        } catch (IOException err) {
            throw new RuntimeException(err);
        } finally {
            IOUtils.close(in);
        }
        
        buffer.append("\n# All Lime Wire Packages\n");
        File dir = new File("/Users/roger/Documents/workspace/mainline/bin");
        packages(buffer, dir, dir);
        
        if (buffer.length() > 0) {
            buffer.setLength(buffer.length()-1);
        }
        
        return buffer.toString();
    }
    
    private static void packages(StringBuilder buffer, File root, File dir) {
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
            buffer.append(pkg).append("\n");
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
            packages(buffer, root, trav);
        }
    }
}
