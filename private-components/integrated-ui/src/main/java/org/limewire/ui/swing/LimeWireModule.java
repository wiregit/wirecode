package org.limewire.ui.swing;

import org.limewire.core.impl.CoreGlueModule;

import com.google.inject.AbstractModule;
import com.limegroup.gnutella.LimeWireCoreModule;

public class LimeWireModule extends AbstractModule {
    
    @Override
    protected void configure() {
        install(new LimeWireCoreModule());
        install(new CoreGlueModule());
    }
    

}
