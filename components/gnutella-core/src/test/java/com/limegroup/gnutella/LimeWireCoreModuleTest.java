package com.limegroup.gnutella;

import junit.framework.TestCase;

import org.limewire.net.ConnectionDispatcher;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.limegroup.gnutella.gui.LimeWireGUIModule;

public class LimeWireCoreModuleTest extends TestCase {

    public void testConnectionDispatcher() {
        Injector injector = Guice.createInjector(new LimeWireCoreModule(), new LimeWireGUIModule());
        ConnectionDispatcher globalInstance = injector.getInstance(Key.get(ConnectionDispatcher.class, Names.named("global")));
        ConnectionDispatcher localInstance = injector.getInstance(Key.get(ConnectionDispatcher.class, Names.named("local")));
        
        assertNotSame(globalInstance, localInstance);
        
        Provider<ConnectionDispatcher> localProvider = injector.getProvider(Key.get(ConnectionDispatcher.class, Names.named("local")));
        assertSame(localInstance, localProvider.get());
        assertSame(localProvider.get(), localProvider.get());

        Provider<ConnectionDispatcher> globalProvider = injector.getProvider(Key.get(ConnectionDispatcher.class, Names.named("global")));
        assertSame(globalInstance, globalProvider.get());
        assertSame(globalProvider.get(), globalProvider.get());
    }
    
    public void testOverrideConstantBinding() {
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bindConstant().annotatedWith(Names.named("constant!")).to("Hello2");
            }
        }, new AbstractModule() {
            @Override
            protected void configure() {
                bindConstant().annotatedWith(Names.named("constant!")).to("Hello1");
            }
        });
        C c = injector.getInstance(C.class);
        assertEquals("Hello2", c.constant);
    }
    
    private static class C {
        @Inject @Named("constant!") String constant;
    }
    
}
