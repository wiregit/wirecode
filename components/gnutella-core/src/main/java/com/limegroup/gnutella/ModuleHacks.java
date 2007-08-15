package com.limegroup.gnutella;

import java.util.concurrent.ScheduledExecutorService;

import org.limewire.concurrent.SimpleTimer;
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
        bind(NIODispatcher.class).toProvider(NIODispatcherProvider.class);
        bind(ByteBufferCache.class).toProvider(ByteBufferCacheProvider.class);
        bind(SimppManager.class).toProvider(SimppManagerProvider.class);
        bind(ScheduledExecutorService.class).annotatedWith(Names.named("nioExecutor")).toProvider(NIOScheduledExecutorServiceProvider.class);
        bind(ScheduledExecutorService.class).annotatedWith(Names.named("backgroundExecutor")).toProvider(BackgroundTimerProvider.class);
    }
    
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
    
    @Singleton
    private static class BackgroundTimerProvider implements Provider<ScheduledExecutorService> {
        public ScheduledExecutorService get() {
            return SimpleTimer.sharedTimer();
        }
    }
    
}
