package com.limegroup.gnutella;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.SocketTimeoutException;
import java.util.Random;

import org.limewire.nio.NIODispatcher;
import org.limewire.util.PrivilegedAccessor;

public class LimeTestUtils {

    public static byte[] writeRandomData(File file, int size)
            throws IOException {
        byte[] data = new byte[size];
        Random r = new Random();
        r.nextBytes(data);
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        try {
            raf.write(data);
        } finally {
            raf.close();
        }
        return data;
    }

    public static void waitForNIO() throws InterruptedException {
        NIODispatcher.instance().invokeAndWait(new Runnable() {
            public void run() {
            }
        });
        NIODispatcher.instance().invokeAndWait(new Runnable() {
            public void run() {
            }
        });
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
        FileOutputStream fos = new FileOutputStream(dest);
        int read = fis.read();
        while(read != -1) {
            fos.write(read);
            read = fis.read();
        }
        fis.close();
        fos.close();
    }

}
