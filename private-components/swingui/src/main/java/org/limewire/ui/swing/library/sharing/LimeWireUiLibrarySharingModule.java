package org.limewire.ui.swing.library.sharing;

import org.limewire.inject.LazyBinder;

import com.google.inject.AbstractModule;

public class LimeWireUiLibrarySharingModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(ShareWidgetFactory.class).toProvider(LazyBinder.newLazyProvider(
                ShareWidgetFactory.class, ShareWidgetFactoryImpl.class));
    }
}
