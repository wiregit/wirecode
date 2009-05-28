package com.limegroup.gnutella.library;

import java.io.File;
import java.util.List;

import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.SourcedEventMulticaster;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.library.SharedFileCollectionChangeEvent.Type;
import com.limegroup.gnutella.tigertree.HashTreeCache;


/**
 * A collection of FileDescs containing files that may be shared with one or more people.
 */
class SharedFileCollectionImpl extends AbstractFileCollection implements SharedFileCollection {
    
    private final int collectionId;    
    private final LibraryFileData data;
    private final HashTreeCache treeCache;    
    private final EventBroadcaster<SharedFileCollectionChangeEvent> sharedBroadcaster;

    @Inject
    public SharedFileCollectionImpl(LibraryFileData data, LibraryImpl managedList, 
                                    SourcedEventMulticaster<FileViewChangeEvent, FileView> multicaster,
                                    EventBroadcaster<SharedFileCollectionChangeEvent> sharedCollectionBroadcaster,
                                    @Assisted int id, HashTreeCache treeCache) {
        super(managedList, multicaster);
        this.collectionId = id;
        this.data = data;
        this.treeCache = treeCache;
        this.sharedBroadcaster = sharedCollectionBroadcaster;
    }
    
    @Override
    public int getId() {
        return collectionId;
    }
    
    public String getName() {
        return data.getNameForCollection(collectionId);
    }
    
    @Override
    public void addFriend(String id) {
        if(data.addFriendToCollection(collectionId, id)) {
            sharedBroadcaster.broadcast(new SharedFileCollectionChangeEvent(Type.FRIEND_ADDED, this, id));
        }
    }
    
    @Override
    public boolean removeFriend(String id) {
        if(data.removeFriendFromCollection(collectionId, id)) {
            sharedBroadcaster.broadcast(new SharedFileCollectionChangeEvent(Type.FRIEND_REMOVED, this, id));
            return true;
        } else {
            return false;
        }
    }
    
    @Override
    public List<String> getFriendList() {
        return data.getFriendsForCollection(collectionId);
    }
    
    @Override
    public void setFriendList(List<String> ids) {
        data.setFriendsForCollection(collectionId, ids);
        sharedBroadcaster.broadcast(new SharedFileCollectionChangeEvent(Type.FRIEND_IDS_CHANGED, this, ids));
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this) + ", name: " + getName();
    }
    
    @Override
    protected boolean addFileDescImpl(FileDesc fileDesc) {
        if(super.addFileDescImpl(fileDesc)) {
            // if no root, calculate one and propagate it.
            if(fileDesc.getTTROOTUrn() == null) {
                // Schedule all additions for having a hash tree root.
                URN root = treeCache.getOrScheduleHashTreeRoot(fileDesc);
                if(root != null) {
                	for(FileDesc fd : library.getFileDescsMatching(fileDesc.getSHA1Urn())) {
                	    fd.setTTRoot(root);
                	}
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void initialize() {
        super.initialize();
        addPendingManagedFiles();
    }
    
    /**
     * This method initializes the friend file list.  It adds the files
     * that are shared with the friend represented by this list.  This
     * is necessary because friend file lists are populated/unpopulated when
     * needed, not upon startup.
     */
    protected void addPendingManagedFiles() {
        // add files from the MASTER list which are for the current friend
        // normally we would not want to lock the master list while adding
        // items internally... but it's OK here because we're guaranteed
        // that nothing is listening to this list, since this will happen
        // immediately after construction.
        library.getReadLock().lock();
        try {
            for (FileDesc fd : library) {
                if(isPending(fd.getFile(), fd)) {
                    add(fd);
                }
            }
        } finally {
            library.getReadLock().unlock();
        }
    }

    /**
     * Unloading the list makes the sharing
     * characteristics of the files in the list invisible externally (files are still in list,
     * but do not have the appearance of being shared)
     */
    public void unload() {
        // for each file in the friend list, decrement its' file share count
        getReadLock().lock();
        try {
            for (FileDesc fd : this) {
                fd.decrementSharedCollectionCount();
            }
        } finally {
            getReadLock().unlock();
        }
        clear();
        dispose();
    }
    
    /**
     * Returns false if it's an {@link IncompleteFileDesc} or it's a store
     * file.
     */
    @Override
    protected boolean isFileAddable(FileDesc fileDesc) {
        if (fileDesc instanceof IncompleteFileDesc) {
            return false;
        } else if( fileDesc.getLimeXMLDocuments().size() != 0 && 
                isStoreXML(fileDesc.getLimeXMLDocuments().get(0))) {
            return false;
        } 
        return true;
    }
    
    @Override
    protected boolean isPending(File file, FileDesc fd) {
        return data.isFileInCollection(file, collectionId);
    }
    
    @Override
    protected void saveChange(File file, boolean added) {
        data.setFileInCollection(file, collectionId, added);      
    }
    
    @Override
    protected void fireAddEvent(FileDesc fileDesc) {
        fileDesc.incrementSharedCollectionCount();
        super.fireAddEvent(fileDesc);
    }

    @Override
    protected void fireRemoveEvent(FileDesc fileDesc) {
        fileDesc.decrementSharedCollectionCount();
        super.fireRemoveEvent(fileDesc);
    }

    @Override
    protected void fireChangeEvent(FileDesc oldFileDesc, FileDesc newFileDesc) {
        oldFileDesc.decrementSharedCollectionCount();
        newFileDesc.incrementSharedCollectionCount();
        super.fireChangeEvent(oldFileDesc, newFileDesc);
    }
}
