package org.limewire.mojito.concurrent;

import java.lang.Thread.UncaughtExceptionHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ExceptionUtils {

    private static final Log LOG = LogFactory.getLog(ExceptionUtils.class);
    
    private static final UncaughtExceptionHandler DEFAULT 
            = new UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            LOG.error(e);
        }
    };
    
    private ExceptionUtils() {}
    
    /**
     * 
     */
    public static void exceptionCaught(Throwable t) {
        Thread thread = Thread.currentThread();
        UncaughtExceptionHandler ueh = thread.getUncaughtExceptionHandler();
        if (ueh == null) {
            ueh = Thread.getDefaultUncaughtExceptionHandler();
            
            if (ueh == null) {
                ueh = DEFAULT;
            }
        }
        
        ueh.uncaughtException(thread, t);
    }
}
