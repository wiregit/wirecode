package com.limegroup.gnutella;

import java.util.concurrent.ScheduledExecutorService;

import org.limewire.nio.ByteBufferCache;
import org.limewire.nio.NIODispatcher;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.name.Names;
import com.limegroup.gnutella.simpp.SimppManager;

// DPINJ: A module of stuff that needs to be converted...
//        Basically, a bunch of providers that delegate elsewhere.
public class ModuleHacks extends AbstractModule {

    @Override
    protected void configure() {
        // DPINJ: Need to figure out what the hell to do with these.
        //----------------------------------------------        
        bind(ActivityCallback.class).toProvider(activityCallback);
        bind(NIODispatcher.class).toProvider(nioDispatcher);
        bind(ByteBufferCache.class).toProvider(byteBufferCache);
        bind(SimppManager.class).toProvider(simppManager);
        
        // DPINJ: Need to create & inject the provider w/ NIODispatcher somehow...
        //----------------------------------------------
        bind(ScheduledExecutorService.class).annotatedWith(Names.named("nioExecutor")).toProvider(nioScheduledExecutorService);
    }

    private static final Provider<ActivityCallback> activityCallback = new Provider<ActivityCallback>() {
        public ActivityCallback get() {
            ActivityCallback callback = RouterService.getCallback();
            // Guice can't deal with null values, and lots of tests leave this
            // as null sometimes...
            // This might cause some problems if stuff is constructed too early, but we'll find out in tests.
            return callback != null ? callback : new ActivityCallbackAdapter();
        }
    };
    
    private static final Provider<NIODispatcher> nioDispatcher = new Provider<NIODispatcher>() {
        public NIODispatcher get() {
            return NIODispatcher.instance();
        }
    };      

    private static final Provider<ByteBufferCache> byteBufferCache = new Provider<ByteBufferCache>() {
        public ByteBufferCache get() {
            return nioDispatcher.get().getBufferCache();
        }
    };
    
    private static final Provider<ScheduledExecutorService> nioScheduledExecutorService = new Provider<ScheduledExecutorService>() {
        public ScheduledExecutorService get() {
            return nioDispatcher.get().getScheduledExecutorService();
        }
    };
    
    private static final Provider<SimppManager> simppManager = new Provider<SimppManager>() {
        public SimppManager get() {
            return SimppManager.instance();
        }
    };
    
}
