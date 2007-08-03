package com.limegroup.gnutella;

import java.util.concurrent.Executor;

import org.limewire.concurrent.ExecutorsHelper;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import com.limegroup.gnutella.dht.DHTControllerFactory;
import com.limegroup.gnutella.dht.DHTControllerFactoryImpl;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.DHTManagerImpl;

public class LimeWireCore {
    
    private final Injector injector;
    
    public LimeWireCore() {
        this.injector = Guice.createInjector(new LimeWireCoreModule());
    }
    
    public Injector getInjector() {
        return injector;
    }
    
    private static class LimeWireCoreModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(NetworkManager.class).to(NetworkManagerImpl.class);
            bind(DHTManager.class).to(DHTManagerImpl.class);
            bind(DHTControllerFactory.class).to(DHTControllerFactoryImpl.class);

            // DPINJ: Necessary evil for now...
            requestStaticInjection(ProviderHacks.class);
            
            // DPINJ: Need to add interface to these classes
            //bind(UDPService.class)
            //bind(Acceptor.class);
            //bind(ConnectionManager.class);
            
            // DPINJ: Need to figure out what the hell to do with these.
            bind(ActivityCallback.class).toProvider(ProviderHacks.activityCallback);
            
            // DPINJ: Are these right?
            bind(Executor.class).annotatedWith(Names.named("dhtExecutor")).toInstance(ExecutorsHelper.newProcessingQueue("DHT-Executor"));
        }    
    }

}
