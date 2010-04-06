package org.limewire.mojito.concurrent2;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 
 */
public class EventUtils {

    private static final AtomicReference<Thread> THREAD_REF 
        = new AtomicReference<Thread>();
    
    private static final ThreadFactory THREAD_FACTORY 
            = new DefaultThreadFactory("EventUtilsThread") {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = super.newThread(r);
            THREAD_REF.set(thread);
            return thread;
        }
    };
    
    private static final ThreadPoolExecutor EXECUTOR 
        = ExecutorUtils.newSingleThreadExecutor(THREAD_FACTORY);
    
    private EventUtils() {}
    
    /**
     * 
     */
    public static void fireEvent(Runnable event) {
        if (event == null) {
            throw new NullArgumentException("event");
        }
        
        EXECUTOR.execute(event);
    }
    
    /**
     * 
     */
    public static boolean isEventThread() {
        return Thread.currentThread() == THREAD_REF.get();
    }
}
