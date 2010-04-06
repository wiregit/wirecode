package org.limewire.mojito.concurrent2;

import java.lang.Thread.UncaughtExceptionHandler;

public class ExceptionUtils {

    private ExceptionUtils() {}
    
    /**
     * 
     */
    public static void exceptionCaught(Throwable t) {
        Thread thread = Thread.currentThread();
        UncaughtExceptionHandler ueh = thread.getUncaughtExceptionHandler();
        if (ueh == null) {
            ueh = Thread.getDefaultUncaughtExceptionHandler();
        }
        
        ueh.uncaughtException(thread, t);
    }
}
