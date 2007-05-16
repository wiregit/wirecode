package com.limegroup.gnutella;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;

import org.limewire.nio.NIODispatcher;
import org.limewire.util.PrivilegedAccessor;

public class LimeTestUtils {

    public static byte[] writeRandomData(File file, int size) throws IOException {
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

    public static void setActivityCallBack(ActivityCallback cb) throws Exception {
        if (RouterService.getCallback() == null) {
            new RouterService(cb);
        } else {
            PrivilegedAccessor.setValue(RouterService.class, "callback", cb);
        }
    }
    
}
