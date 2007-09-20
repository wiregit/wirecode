package com.limegroup.gnutella;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.limewire.nio.NIODispatcher;
import org.limewire.util.AssertComparisons;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;

public class LimeTestUtils {

    public static void waitForNIO() throws InterruptedException {
        Future<?> future = NIODispatcher.instance().getScheduledExecutorService().submit(new Runnable() {
            public void run() {
            }
        });
        try {
            future.get();
        } catch(ExecutionException ee) {
            throw new IllegalStateException(ee);
        }
        
        // the runnable is run at the beginning of the processing cycle so 
        // we need a second runnable to make sure the cycle has been completed
        future = NIODispatcher.instance().getScheduledExecutorService().submit(new Runnable() {
            public void run() {
            }
        });
        try {
            future.get();
        } catch(ExecutionException ee) {
            throw new IllegalStateException(ee);
        }
        
    }

    public static void setActivityCallBack(ActivityCallback cb)
            throws Exception {
        throw new RuntimeException("fix me");
    }

    public static void readBytes(InputStream in, long count) throws IOException {
        for (long i = 0; i < count; i++) {
            try {
                if (in.read() == -1) {
                    throw new AssertionError("Unexpected end of stream after "
                            + i + " bytes");
                }
            } catch (SocketTimeoutException e) {
                throw new AssertionError("Timeout while reading " + count
                        + " bytes (read " + i + " bytes)");
            }
        }
    }

    /**
     * Simple copy.  Horrible performance for large files.
     * Good performance for alphabets.
     */
    public static void copyFile(File source, File dest) throws Exception {
        FileInputStream fis = new FileInputStream(source);
        try {
            FileOutputStream fos = new FileOutputStream(dest);
            try {
                int read = fis.read();
                while(read != -1) {
                    fos.write(read);
                    read = fis.read();
                }
            } finally {
                fos.close();
            }
        } finally {
            fis.close();
        }
    }

    
    /**
     * Creates subdirs in tmp dir and ensures that they are deleted on JVM
     * exit. 
     */
    public static File[] createTmpDirs(String... dirs) {
		File tmpDir = new File(System.getProperty("java.io.tmpdir"));
		AssertComparisons.assertTrue(tmpDir.isDirectory());
		return createDirs(tmpDir, dirs);
	}
	
    /**
     * Creates <code>dirs</code> as subdirs of <code>parent</code> and ensures
     * that they are deleted on JVM exit. 
     */
	public static File[] createDirs(File parent, String... dirs) {
		List<File> list = new ArrayList<File>(dirs.length);
		for (String name : dirs) {
			File dir = new File(parent, name);
			AssertComparisons.assertTrue(dir.mkdirs() || dir.exists());
			// Make sure it's clean!
			deleteFiles(dir.listFiles());
			list.add(dir);
			while (!dir.equals(parent)) {
				dir.deleteOnExit();
				dir = dir.getParentFile();
			}
		}
		return list.toArray(new File[list.size()]);
	}
	
	/** Deletes all files listed. */
	public static void deleteFiles(File...files) {
	    for(int i = 0; i < files.length; i++)
	        files[i].delete();
	}

    /**
     * Clears the hostcatcher.
     */
    public static void clearHostCatcher() {
        ProviderHacks.getHostCatcher().clear();
    }

    /**
     * Creates the Guice injector with the limewire default modules and the 
     * test module that can override bindings in the former modules.
     * 
     * @param module the test modules that can override bindings
     * @param callbackClass the class that is used as a callback
     * @return the injector
     */
    public static Injector createInjector(Class<? extends ActivityCallback> callbackClass, Module...modules) {
        List<Module> list = new ArrayList<Module>();
        list.addAll(Arrays.asList(modules));
        list.add(new LimeWireCoreModule(callbackClass));
        list.add(new ModuleHacks());
        Injector injector = Guice.createInjector(list);
        
        ProviderHacks.setLimeWireCore(injector.getInstance(LimeWireCore.class));        
        
        return injector;
    }

    /**
     * Wraps {@link #createInjector(Module, Class) createInjector(Module, ActivityCallbackStub.class)}.
     */
    public static Injector createInjector(Module... modules) {
        return createInjector(ActivityCallbackStub.class, modules);
    }

    /**
     * Creates the Guice injector with the limewire default modules and the 
     * test module that can override bindings in the former modules.
     * 
     * Also starts the {@link LifecycleManager}.
     * 
     * @param module the test modules that can override bindings
     * @param callbackClass the class that is used as a callback
     * @return the injector
     */
    public static Injector createInjectorAndStart(Class<? extends ActivityCallback> callbackClass, Module...modules) {
        Injector injector = createInjector(callbackClass, modules);
        LifecycleManager lifecycleManager = injector.getInstance(LifecycleManager.class);
        lifecycleManager.start();
        return injector;
    }
    
    /**
     * Wraps {@link #createInjectorAndStart(Module, Class) createInjectorAndStart(Module, ActivityCallbackStub.class)}.
     */
    public static Injector createInjectorAndStart(Module...modules) {
        return createInjectorAndStart(ActivityCallbackStub.class, modules);
    }

}
