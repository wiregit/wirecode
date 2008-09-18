package org.limewire.ui.swing.images;

import com.google.inject.AbstractModule;

public class LimeWireUIImagesModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ThumbnailManager.class).to(ThumbnailManagerImpl.class);
    }
}
