package org.limewire.ui.swing.sharing.fancy;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryProvider;

public class LimeWireUiSharingFancyModule extends AbstractModule {

    @Override
    protected void configure() {
        
        bind(SharingFancyPanelFactory.class).toProvider(
                FactoryProvider.newFactory(
                        SharingFancyPanelFactory.class, SharingFancyPanel.class));
        
    }
    
}
