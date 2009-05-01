package com.limegroup.gnutella.library;

import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SourcedEventMulticaster;
import org.limewire.listener.SourcedEventMulticasterFactory;
import org.limewire.listener.SourcedEventMulticasterImpl;
import org.limewire.listener.SourcedListenerSupport;
import org.limewire.listener.EventListenerList.EventListenerListContext;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryProvider;


public class LimeWireLibraryModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(LocalFileDetailsFactory.class).to(LocalFileDetailsFactoryImpl.class);
        bind(SharedFilesKeywordIndex.class).to(SharedFilesKeywordIndexImpl.class);
        bind(CreationTimeCache.class);        
        
        bind(FileManager.class).to(FileManagerImpl.class);
        bind(Library.class).to(LibraryImpl.class);
        bind(GnutellaFileCollection.class).to(GnutellaFileCollectionImpl.class);
        bind(IncompleteFileCollection.class).to(IncompleteFileCollectionImpl.class);        
        bind(SharedFileCollectionImplFactory.class).toProvider(
                FactoryProvider.newFactory(SharedFileCollectionImplFactory.class, SharedFileCollectionImpl.class));
        
        EventListenerListContext context = new EventListenerListContext();
        
        SourcedEventMulticaster<FileViewChangeEvent, FileView> allFileCollectionMulticaster =
            new SourcedEventMulticasterImpl<FileViewChangeEvent, FileView>(context);
        bind(new TypeLiteral<SourcedEventMulticasterFactory<FileViewChangeEvent, FileView>>(){}).annotatedWith(AllFileCollections.class).toInstance(allFileCollectionMulticaster);
        bind(new TypeLiteral<ListenerSupport<FileViewChangeEvent>>(){}).annotatedWith(AllFileCollections.class).toInstance(allFileCollectionMulticaster);
        
        EventMulticaster<SharedFileCollectionChangeEvent> sharedAllFileCollectionMulticaster =
            new EventMulticasterImpl<SharedFileCollectionChangeEvent>(context);
        bind(new TypeLiteral<EventBroadcaster<SharedFileCollectionChangeEvent>>(){}).annotatedWith(AllFileCollections.class).toInstance(sharedAllFileCollectionMulticaster);
        bind(new TypeLiteral<ListenerSupport<SharedFileCollectionChangeEvent>>(){}).annotatedWith(AllFileCollections.class).toInstance(sharedAllFileCollectionMulticaster);
        
        EventMulticaster<ManagedListStatusEvent> managedListMulticaster =
            new EventMulticasterImpl<ManagedListStatusEvent>();
        bind(new TypeLiteral<EventBroadcaster<ManagedListStatusEvent>>(){}).toInstance(managedListMulticaster);
        bind(new TypeLiteral<ListenerSupport<ManagedListStatusEvent>>(){}).toInstance(managedListMulticaster);
        bind(new TypeLiteral<EventMulticaster<ManagedListStatusEvent>>(){}).toInstance(managedListMulticaster);
        
        SourcedEventMulticaster<FileDescChangeEvent, FileDesc> fileDescMulticaster =
            new SourcedEventMulticasterImpl<FileDescChangeEvent, FileDesc>();
        bind(new TypeLiteral<EventBroadcaster<FileDescChangeEvent>>(){}).toInstance(fileDescMulticaster);
        bind(new TypeLiteral<SourcedListenerSupport<FileDescChangeEvent, FileDesc>>(){}).toInstance(fileDescMulticaster);
        bind(new TypeLiteral<ListenerSupport<FileDescChangeEvent>>(){}).toInstance(fileDescMulticaster);
        bind(new TypeLiteral<SourcedEventMulticaster<FileDescChangeEvent, FileDesc>>(){}).toInstance(fileDescMulticaster);

        bind(FileDescFactory.class).to(FileDescFactoryImpl.class);
        
    }
    
    @Provides LibraryFileData libraryFileData(LibraryImpl library) {
        return library.getLibraryData();
    }

}
