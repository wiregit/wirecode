package com.limegroup.gnutella;

import java.util.concurrent.ScheduledExecutorService;

import org.limewire.nio.ByteBufferCache;
import org.limewire.nio.NIODispatcher;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.name.Names;
import com.limegroup.gnutella.auth.ContentManager;

// DPINJ: A module of stuff that needs to be converted...
//        Basically, a bunch of providers that delegate elsewhere.
public class ModuleHacks extends AbstractModule {

    @Override
    protected void configure() {
        // DPINJ: Need to figure out what the hell to do with these.
        //----------------------------------------------        
        bind(ActivityCallback.class).toProvider(activityCallback);
        
        // DPINJ: Need to create & inject the provider w/ NIODispatcher somehow...
        //----------------------------------------------
        bind(ScheduledExecutorService.class).annotatedWith(Names.named("nioExecutor")).toProvider(nioScheduledExecutorService);
    }

    public static final Provider<ActivityCallback> activityCallback = new Provider<ActivityCallback>() {
        public ActivityCallback get() {
            ActivityCallback callback = RouterService.getCallback();
            // Guice can't deal with null values, and lots of tests leave this
            // as null sometimes...
            // This might cause some problems if stuff is constructed too early, but we'll find out in tests.
            return callback != null ? callback : new ActivityCallbackAdapter();
        }
    };
    
    public static final Provider<NIODispatcher> nioDispatcher = new Provider<NIODispatcher>() {
        public NIODispatcher get() {
            return NIODispatcher.instance();
        }
    };
    
    public static final Provider<Statistics> statistics = new Provider<Statistics>() {
        public Statistics get() {
            return Statistics.instance();
        }
    };
    
    public static final Provider<ContentManager> contentManager = new Provider<ContentManager>() {
        public ContentManager get() {
            return RouterService.getContentManager();
        }
    };
    
      
    public static final Provider<CreationTimeCache> creationTimeCache = new Provider<CreationTimeCache>() {
       public CreationTimeCache get() {
           return CreationTimeCache.instance();
       }
    };

    public static final Provider<ByteBufferCache> byteBufferCache = new Provider<ByteBufferCache>() {
        public ByteBufferCache get() {
            return nioDispatcher.get().getBufferCache();
        }
    };
    
    public static final Provider<ScheduledExecutorService> nioScheduledExecutorService = new Provider<ScheduledExecutorService>() {
        public ScheduledExecutorService get() {
            return nioDispatcher.get().getScheduledExecutorService();
        }
    };    
    
}
