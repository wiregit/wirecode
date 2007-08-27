package com.limegroup.gnutella;

import junit.framework.TestCase;

import org.limewire.net.ConnectionDispatcher;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.limegroup.gnutella.gui.LimeWireGUIModule;

public class LimeWireCoreModuleTest extends TestCase {

    public void testConnectionDispatcher() {
        Injector injector = Guice.createInjector(new LimeWireCoreModule(), new LimeWireGUIModule(), new ModuleHacks());
        ConnectionDispatcher globalInstance = injector.getInstance(ConnectionDispatcher.class);
        ConnectionDispatcher localInstance = injector.getInstance(Key.get(ConnectionDispatcher.class, Names.named("local")));
        assertNotSame(globalInstance, localInstance);
    }
    
}
