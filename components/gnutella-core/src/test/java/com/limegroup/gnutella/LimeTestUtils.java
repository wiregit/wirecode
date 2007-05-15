package com.limegroup.gnutella;

import org.limewire.nio.NIODispatcher;
import org.limewire.util.PrivilegedAccessor;

public class LimeTestUtils {

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
