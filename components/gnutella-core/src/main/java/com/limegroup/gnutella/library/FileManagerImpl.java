package com.limegroup.gnutella.library;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.limewire.core.api.friend.Friend;
import org.limewire.inspection.InspectionPoint;
import org.limewire.lifecycle.Service;
import org.limewire.listener.EventBroadcaster;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * The list of all known files. This creates and maintains a list of 
 * directories and FileDescs. It also creates a set of FileLists which 
 * may contain subsets of all FileDescs. Files can be added to just the 
 * FileManager or loaded into both the FileManager and a specified FileList
 * once the FileDesc has been created. <p>
 *
 * This class is thread-safe.
 */
@Singleton 
class FileManagerImpl implements FileManager, Service {
    
    private final LibraryImpl managedFileList;
    
    @InspectionPoint("gnutella shared file list")
    private final SharedFileCollectionImpl defaultSharedCollection;
    
    @InspectionPoint("incomplete file list")
    private final IncompleteFileCollectionImpl incompleteCollection;
    
    private final SharedFileCollectionImplFactory sharedFileCollectionImplFactory;
    
    private final Map<Integer, SharedFileCollectionImpl> sharedCollections =
        new HashMap<Integer,SharedFileCollectionImpl>();
    
    private Saver saver;
    
    /**
     * Whether the FileManager has been shutdown.
     */
    private volatile boolean shutdown;
    
    /** The background executor. */
    private final ScheduledExecutorService backgroundExecutor;
    
    private final EventBroadcaster<SharedFileCollectionChangeEvent> sharedBroadcaster;

	/**
	 * Creates a new <tt>FileManager</tt> instance.
	 */
    @Inject
    public FileManagerImpl(LibraryImpl managedFileList,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
            IncompleteFileCollectionImpl incompleteFileCollectionImpl,
            SharedFileCollectionImplFactory sharedFileCollectionImplFactory,
            EventBroadcaster<SharedFileCollectionChangeEvent> sharedBroadcaster) {        
        this.backgroundExecutor = backgroundExecutor;
        this.managedFileList = managedFileList;
        this.incompleteCollection = incompleteFileCollectionImpl;
        this.incompleteCollection.initialize();
        this.sharedFileCollectionImplFactory = sharedFileCollectionImplFactory;
        this.sharedBroadcaster = sharedBroadcaster;
        this.defaultSharedCollection = sharedFileCollectionImplFactory.createSharedFileCollectionImpl(LibraryFileData.DEFAULT_SHARED_COLLECTION_ID);
        this.defaultSharedCollection.initialize();
    }

    @Override
    public String getServiceName() {
        return org.limewire.i18n.I18nMarker.marktr("Shared Files");
    }

    @Override
    public void initialize() {
        managedFileList.initialize();
    }
    
    @Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this);
    }

    @Override
    public void start() {
        LibraryConverter converter = new LibraryConverter();
        if(converter.isOutOfDate()) {
            managedFileList.fireLoading();
            converter.convert(managedFileList.getLibraryData());
        }
        
        loadStoredCollections();        
        defaultSharedCollection.addFriend(Friend.P2P_FRIEND_ID);
        
        managedFileList.loadManagedFiles();
        
        synchronized (this) {
            if (saver == null) {
                this.saver = new Saver();
                backgroundExecutor.scheduleWithFixedDelay(saver, 1, 1, TimeUnit.MINUTES);
            }
        }
    }
    
    private void loadStoredCollections() {
        for(Integer id : managedFileList.getLibraryData().getStoredCollectionIds()) {
            if(!sharedCollections.containsKey(id)) {
                SharedFileCollectionImpl collection =  sharedFileCollectionImplFactory.createSharedFileCollectionImpl(id);
                collection.initialize();
                synchronized(this) {
                    sharedCollections.put(id, collection);
                }
                sharedBroadcaster.broadcast(new SharedFileCollectionChangeEvent(SharedFileCollectionChangeEvent.Type.COLLECTION_ADDED, collection));
            }
        }
    }

    @Override
    public void stop() {
        managedFileList.save();
        shutdown = true;
    }    
    
    @Override
    public Library getLibrary() {
        return managedFileList;
    }

    @Override
    public FileCollection getGnutellaCollection() {
        return defaultSharedCollection;
    }

    @Override
    public synchronized SharedFileCollection getCollectionById(int collectionId) {
        if(collectionId == LibraryFileData.DEFAULT_SHARED_COLLECTION_ID) {
            return defaultSharedCollection;
        } else {
            return sharedCollections.get(collectionId);
        }
    }

    @Override
    public void removeCollectionById(int collectionId) {
        // Cannot remove the default collection.
        if(collectionId != LibraryFileData.DEFAULT_SHARED_COLLECTION_ID) {        
            // if it was a valid key, remove saved references to it
            SharedFileCollectionImpl removeFileList;
            synchronized(this) {
                removeFileList = sharedCollections.get(collectionId);
                if(removeFileList != null) {
                    removeFileList.dispose();
                    sharedCollections.remove(collectionId);
                    // TODO: remove from library data somehow?
                }
            }
            
            if(removeFileList != null) {
                sharedBroadcaster.broadcast(new SharedFileCollectionChangeEvent(SharedFileCollectionChangeEvent.Type.COLLECTION_REMOVED, removeFileList));
            }
        }
    }
    
    private synchronized SharedFileCollectionImpl createNewCollectionImpl(String name) {
        int newId = managedFileList.getLibraryData().createNewCollection(name);
        SharedFileCollectionImpl collection =  sharedFileCollectionImplFactory.createSharedFileCollectionImpl(newId);
        collection.initialize();
        sharedCollections.put(newId, collection);
        return collection;
    }
    
    @Override
    public SharedFileCollection createNewCollection(String name) {
        SharedFileCollectionImpl collection = createNewCollectionImpl(name);
        sharedBroadcaster.broadcast(new SharedFileCollectionChangeEvent(SharedFileCollectionChangeEvent.Type.COLLECTION_ADDED, collection));
        return collection;
    }
    
    @Override
    public SharedFileCollection getOrCreateCollectionByName(String name) {
        SharedFileCollectionImpl collection;
        synchronized(this) {
            for(SharedFileCollectionImpl shared : sharedCollections.values()) {
                if(shared.getName().equals(name)) {
                    return shared;
                }
            }
            
            collection = createNewCollectionImpl(name);
        }
        
        sharedBroadcaster.broadcast(new SharedFileCollectionChangeEvent(SharedFileCollectionChangeEvent.Type.COLLECTION_ADDED, collection));
        return collection;
    }
    
    @Override
    public void removeCollectionByName(String name) {
        Integer idToRemove = null;
        synchronized(this) {
            for(Iterator<SharedFileCollectionImpl> entries = sharedCollections.values().iterator(); entries.hasNext(); ) {
                SharedFileCollectionImpl collection = entries.next();
                if(collection.getName().equals(name)) {
                    idToRemove = collection.getId();
                    break;
                }
            }
        }
        
        if(idToRemove != null) {
            removeCollectionById(idToRemove);
        }
    }

    public void unloadCollectionByName(String friendName) {
        SharedFileCollectionImpl removeFileList = null;
        synchronized (this) {
            removeFileList = sharedCollections.get(friendName);
            if (removeFileList != null) {
                sharedCollections.remove(friendName);
            }
        }
        
        if(removeFileList != null) {
            removeFileList.unload();
        }

    }

    @Override
    public IncompleteFileCollection getIncompleteFileCollection() {
        return incompleteCollection;
    }
    
    private class Saver implements Runnable {
        public void run() {
            if (!shutdown && managedFileList.isLoadFinished()) {
                managedFileList.save();
            }
        }
    }
}