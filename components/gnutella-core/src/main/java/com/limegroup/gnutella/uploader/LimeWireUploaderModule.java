package com.limegroup.gnutella.uploader;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

public class LimeWireUploaderModule extends AbstractModule implements Module {

    @Override
    protected void configure() {
        bind(FileRequestHandlerFactory.class).to(FileRequestHandlerFactoryImpl.class);
    }

}
