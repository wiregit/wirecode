package org.limewire.ui.swing.util;

import com.google.inject.AbstractModule;

public class LimeWireUiUtilModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(NonBlockFileIconController.class).to(BasicFileIconController.class);
    }
}