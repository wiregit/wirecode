package com.limegroup.gnutella;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.limewire.io.IOUtils;
import org.limewire.nio.NIODispatcher;
import org.limewire.util.AssertComparisons;
import org.limewire.util.Base32;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.limegroup.gnutella.connection.BlockingConnectionFactory;
import com.limegroup.gnutella.connection.BlockingConnectionFactoryImpl;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.stubs.NetworkManagerStub;

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
    
    public static void setSharedDirectories(File[] dirs) {
        Set<File> set = new HashSet<File>(Arrays.asList(dirs));
        SharingSettings.DIRECTORIES_TO_SHARE.setValue(set);
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
        list.add(new BlockingConnectionFactoryModule());
        list.add(new LimeWireCoreModule(callbackClass));
        Injector injector = Guice.createInjector(list);        
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

    public static class NetworkManagerStubModule extends AbstractModule {
        
        private final NetworkManagerStub networkManagerStub;

        public NetworkManagerStubModule(NetworkManagerStub networkManagerStub) {
            this.networkManagerStub = networkManagerStub;
        }
        
        @Override
        protected void configure() {
            bind(NetworkManager.class).toInstance(networkManagerStub);
        }
    }
    
    public static class BlockingConnectionFactoryModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(BlockingConnectionFactory.class).to(BlockingConnectionFactoryImpl.class);
        }
    }

    /**
     * Establishes an incoming connection.  It is necessary to send a proper
     * connect back string.  We ignore exceptions because various components 
     * may have been stubbed out in the specific test case.
     * @param port where to establish the connection to.
     */
    public static void establishIncoming(int port) {
        Socket s = null;
        try {
            s = new Socket();
            s.connect(new InetSocketAddress("127.0.0.1",port));
            s.getOutputStream().write("CONNECT ".getBytes());
            s.getOutputStream().flush();
            s.close();
        } catch (IOException ignore) {}
        finally {
            IOUtils.close(s);
        }
    }
    
    /**
     * @return a <tt>Matcher</tt> to use with JMock that compares byte []
     * using Arrays.equals
     */
    public static Matcher<byte []> createByteMatcher(byte [] toMatch) {
        return new ByteMatcher(toMatch.clone());
    }
    
    private static class ByteMatcher extends BaseMatcher<byte []> {
        
        private final byte[] toMatch;
        
        ByteMatcher(byte [] toMatch) {
            this.toMatch = toMatch.clone();
        }
        
        public boolean matches(Object item) {
            if (! (item instanceof byte []))
                return false;
            byte [] b = (byte [])item;
            return Arrays.equals(toMatch,b);
        }

        public void describeTo(Description description) {
            description.appendText("byte [] matcher for "+Base32.encode(toMatch));
        }
        
    }
}
