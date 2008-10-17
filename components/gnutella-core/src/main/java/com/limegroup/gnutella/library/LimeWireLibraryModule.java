package com.limegroup.gnutella.library;

import org.limewire.listener.EventListener;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;
import org.limewire.listener.ListenerSupport;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;


public class LimeWireLibraryModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(LocalFileDetailsFactory.class).to(LocalFileDetailsFactoryImpl.class);
        bind(SharedFilesKeywordIndex.class).to(SharedFilesKeywordIndexImpl.class);
        bind(CreationTimeCache.class);        
        
        bind(FileManager.class).to(FileManagerImpl.class);
        EventMulticaster<FileManagerEvent> fileManagerMulticaster = new EventMulticasterImpl<FileManagerEvent>();
        bind(new TypeLiteral<EventListener<FileManagerEvent>>(){}).toInstance(fileManagerMulticaster);
        bind(new TypeLiteral<ListenerSupport<FileManagerEvent>>(){}).toInstance(fileManagerMulticaster);
    }

}
