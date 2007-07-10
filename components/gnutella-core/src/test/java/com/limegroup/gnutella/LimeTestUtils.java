package com.limegroup.gnutella;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.limewire.nio.NIODispatcher;
import org.limewire.util.AssertComparisons;
import org.limewire.util.PrivilegedAccessor;

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
        if (RouterService.getCallback() == null) {
            new RouterService(cb);
        } else {
            PrivilegedAccessor.setValue(RouterService.class, "callback", cb);
        }
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
}
