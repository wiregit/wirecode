package com.limegroup.gnutella.library;

import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SourcedEventMulticaster;
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
        
        bind(FileViewManager.class).to(FileViewManagerImpl.class);
        bind(FileCollectionManager.class).to(FileCollectionManagerImpl.class);
        
        bind(FileManager.class).to(FileManagerImpl.class);
        bind(Library.class).to(LibraryImpl.class);
        bind(IncompleteFileCollection.class).to(IncompleteFileCollectionImpl.class);
        bind(FileView.class).annotatedWith(IncompleteFiles.class).to(IncompleteFileCollection.class);
        bind(SharedFileCollectionImplFactory.class).toProvider(
                FactoryProvider.newFactory(SharedFileCollectionImplFactory.class, SharedFileCollectionImpl.class));
        
        EventListenerListContext context = new EventListenerListContext();
        
        SourcedEventMulticaster<FileViewChangeEvent, FileView> allFileCollectionMulticaster =
            new SourcedEventMulticasterImpl<FileViewChangeEvent, FileView>(context);
        bind(new TypeLiteral<SourcedEventMulticaster<FileViewChangeEvent, FileView>>(){}).toInstance(allFileCollectionMulticaster);
        bind(new TypeLiteral<ListenerSupport<FileViewChangeEvent>>(){}).toInstance(allFileCollectionMulticaster);
        
        EventMulticaster<SharedFileCollectionChangeEvent> sharedAllFileCollectionMulticaster =
            new EventMulticasterImpl<SharedFileCollectionChangeEvent>(context);
        bind(new TypeLiteral<EventBroadcaster<SharedFileCollectionChangeEvent>>(){}).toInstance(sharedAllFileCollectionMulticaster);
        bind(new TypeLiteral<ListenerSupport<SharedFileCollectionChangeEvent>>(){}).toInstance(sharedAllFileCollectionMulticaster);
        
        EventMulticaster<LibraryStatusEvent> managedListMulticaster =
            new EventMulticasterImpl<LibraryStatusEvent>();
        bind(new TypeLiteral<EventBroadcaster<LibraryStatusEvent>>(){}).toInstance(managedListMulticaster);
        bind(new TypeLiteral<ListenerSupport<LibraryStatusEvent>>(){}).toInstance(managedListMulticaster);
        bind(new TypeLiteral<EventMulticaster<LibraryStatusEvent>>(){}).toInstance(managedListMulticaster);
        
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
        
    @Provides @GnutellaFiles FileCollection gnetFileCollection(FileCollectionManagerImpl manager) {
        return manager.getGnutellaCollection();
    }

    @Provides @GnutellaFiles FileView gnetFileView(FileViewManagerImpl manager) {
        return manager.getGnutellaFileView();
    }
    
    @Provides @SharedFiles FileView allSharedView(FileViewManagerImpl manager) {
        return manager.getAllSharedFilesView();
    }
    
    

}
