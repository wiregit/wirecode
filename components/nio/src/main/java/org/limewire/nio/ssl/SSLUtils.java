package org.limewire.nio.ssl;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLContext;

import org.limewire.concurrent.AtomicLazyReference;
import org.limewire.concurrent.ExecutorsHelper;

class SSLUtils {
    
    private SSLUtils() {}
        
    private static final Executor TLS_PROCESSOR = ExecutorsHelper.newProcessingQueue("TLSProcessor");
    private static final AtomicLazyReference<SSLContext> TLS_CONTEXT = new AtomicLazyReference<SSLContext>() {
        @Override
        protected SSLContext createObject() {
                try {
                    SSLContext context = SSLContext.getInstance("TLS");
                    context.init(null, null, null);
                    return context;
                } catch (NoSuchAlgorithmException e) {
                    throw new IllegalStateException(e);
                } catch (KeyManagementException e) {
                    throw new IllegalStateException(e);
                }
        }        
    };
    
    public static Executor getExecutor() {
        return TLS_PROCESSOR;
    }
    
    public static SSLContext getTLSContext() {
        return TLS_CONTEXT.get();
    }
}
