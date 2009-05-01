package com.limegroup.gnutella.library;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;

import org.limewire.collection.IntSet;
import org.limewire.collection.IntSet.IntSetIterator;
import org.limewire.listener.EventListener;

class SharedCollectionBackedFileViewImpl extends AbstractFileView {
    
    private final List<FileView> backingViews = new ArrayList<FileView>();
    private final Lock readLock;
    private final String id;

    public SharedCollectionBackedFileViewImpl(String id, Lock readLock, LibraryImpl library) {
        super(library);
        this.id = id;
        this.readLock = readLock;
    }

    @Override
    public void addFileViewListener(EventListener<FileViewChangeEvent> listener) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Lock getReadLock() {
        return readLock;
    }

    @Override
    public Iterator<FileDesc> iterator() {
        readLock.lock();
        try {
            return new FileViewIterator(library, new IntSet(getIndexes()));
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Iterable<FileDesc> pausableIterable() {
        return new Iterable<FileDesc>() {
            @Override
            public Iterator<FileDesc> iterator() {
                return SharedCollectionBackedFileViewImpl.this.iterator();
            }
        };
    }

    @Override
    public void removeFileViewListener(EventListener<FileViewChangeEvent> listener) {
        // TODO Auto-generated method stub
        
    }
    
    List<FileDesc> removeBackingCollection(SharedFileCollection collection) {
        if(backingViews.remove(collection)) {
            return validateItems();
        } else {
            return Collections.emptyList();
        }
    }

    List<FileDesc> addNewBackingCollection(SharedFileCollection collection) {
        if(!backingViews.contains(collection)) {
            backingViews.add(collection);
        }
        List<FileDesc> added = new ArrayList<FileDesc>(collection.size());
        collection.getReadLock().lock();
        try {
            IntSetIterator iter = collection.getIndexes().iterator();
            while(iter.hasNext()) {
                int i = iter.next();
                if(getIndexes().add(i)) {
                    added.add(library.getFileDescForIndex(i));
                }
            }
        } finally {
            collection.getReadLock().unlock();
        }
        return added;
    }

    List<FileDesc> fileCollectionCleared(FileView fileView) {        
        return validateItems();
    }

    boolean fileRemovedFromCollection(FileDesc fileDesc, FileView fileView) {
        for(FileView view : backingViews) {
            if(view.contains(fileDesc)) {
                return false;
            }
        }
        getIndexes().remove(fileDesc.getIndex());
        return true;
    }

    boolean fileAddedFromCollection(FileDesc fileDesc, FileView fileView) {
        return getIndexes().add(fileDesc.getIndex());
    }
    
    private List<FileDesc> validateItems() {
        // Calculate the current FDs in the set.
        IntSet newItems = new IntSet();
        for(FileView view : backingViews) {
            view.getReadLock().lock();
            try {
                newItems.addAll(view.getIndexes());                
            } finally {
                view.getReadLock().unlock();
            }
        }
        
        // Calculate the FDs that were removed.
        List<FileDesc> removedFds;
        IntSet indexes = getIndexes();
        indexes.removeAll(newItems);
        if(indexes.size() == 0) {
            removedFds = Collections.emptyList();
        } else {
            removedFds = new ArrayList<FileDesc>(indexes.size());
            IntSetIterator iter = indexes.iterator();
            while(iter.hasNext()) {
                FileDesc fd = library.getFileDescForIndex(iter.next());
                if(fd != null) {
                    removedFds.add(fd);
                }                
            }
        }
        
        // Set the current FDs & return the removed ones.
        indexes.clear();
        indexes.addAll(newItems);
        return removedFds;
    }

}
