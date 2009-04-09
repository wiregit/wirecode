package org.limewire.ui.swing.components;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryProvider;

public class LimeWireUiComponentsModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(FancyTabListFactory.class).toProvider(
                FactoryProvider.newFactory(FancyTabListFactory.class, FancyTabList.class));   
        bind(FlexibleTabListFactory.class).toProvider(
                FactoryProvider.newFactory(FlexibleTabListFactory.class, FlexibleTabList.class));   
        
        bind(ShapeDialog.class);
        
    }


}

