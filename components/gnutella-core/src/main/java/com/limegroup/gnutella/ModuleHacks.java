package com.limegroup.gnutella;

import java.util.concurrent.ScheduledExecutorService;

import org.limewire.nio.ByteBufferCache;
import org.limewire.nio.NIODispatcher;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.limegroup.gnutella.simpp.SimppManager;

// DPINJ: A module of stuff that needs to be converted...
//        Basically, a bunch of providers that delegate elsewhere.
public class ModuleHacks extends AbstractModule {

    @Override
    protected void configure() {
        bind(ActivityCallback.class).toProvider(ActivityCallbackProvider.class);
        bind(NIODispatcher.class).toProvider(NIODispatcherProvider.class);
        bind(ByteBufferCache.class).toProvider(ByteBufferCacheProvider.class);
        bind(SimppManager.class).toProvider(SimppManagerProvider.class);
        bind(ScheduledExecutorService.class).annotatedWith(Names.named("nioExecutor")).toProvider(NIOScheduledExecutorServiceProvider.class);
    }

    @Singleton
    private static class ActivityCallbackProvider implements Provider<ActivityCallback> {
        public ActivityCallback get() {
            ActivityCallback callback = RouterService.getCallback();
            // Guice can't deal with null values, and lots of tests leave this
            // as null sometimes...
            // This might cause some problems if stuff is constructed too early, but we'll find out in tests.
            return callback != null ? callback : new ActivityCallbackAdapter();
        }
    };
    
    @Singleton
    private static class NIODispatcherProvider implements Provider<NIODispatcher> {
        public NIODispatcher get() {
            return NIODispatcher.instance();
        }
    };      

    @Singleton
    private static class ByteBufferCacheProvider implements Provider<ByteBufferCache> {
        private final Provider<NIODispatcher> nioDispatcher;
        
        @Inject
        public ByteBufferCacheProvider(Provider<NIODispatcher> nioDispatcher) {
            this.nioDispatcher = nioDispatcher;
        }
        
        public ByteBufferCache get() {
            return nioDispatcher.get().getBufferCache();
        }
    };
    
    @Singleton
    private static class NIOScheduledExecutorServiceProvider implements Provider<ScheduledExecutorService> {
        private final Provider<NIODispatcher> nioDispatcher;
        
        @Inject
        public NIOScheduledExecutorServiceProvider(Provider<NIODispatcher> nioDispatcher) {
            this.nioDispatcher = nioDispatcher;
        }
        
        public ScheduledExecutorService get() {
            return nioDispatcher.get().getScheduledExecutorService();
        }
    };
    
    @Singleton
    private static class SimppManagerProvider implements Provider<SimppManager> {
        public SimppManager get() {
            return SimppManager.instance();
        }
    };
    
}
