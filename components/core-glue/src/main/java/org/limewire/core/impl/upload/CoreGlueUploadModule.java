package org.limewire.core.impl.upload;

import org.limewire.core.api.upload.UploadListManager;

import com.google.inject.AbstractModule;

public class CoreGlueUploadModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(UploadListManager.class).to(CoreUploadListManager.class);
    }
}
