package org.limewire.ui.swing.util;

import com.google.inject.AbstractModule;

public class LimeWireUiUtilModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(FileIconController.class).to(BasicFileIconController.class);
    }
}