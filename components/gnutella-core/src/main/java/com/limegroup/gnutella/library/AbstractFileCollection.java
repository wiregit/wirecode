package com.limegroup.gnutella.library;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.limewire.concurrent.ListeningFuture;
import org.limewire.concurrent.ListeningFutureDelegator;
import org.limewire.concurrent.SimpleFuture;
import org.limewire.inspection.Inspectable;
import org.limewire.listener.DisposableEventMulticaster;
import org.limewire.listener.EventListener;
import org.limewire.listener.SourcedEventMulticasterFactory;
import org.limewire.util.FileUtils;
import org.limewire.util.Objects;

import com.limegroup.gnutella.licenses.LicenseType;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * A List of FileDescs that are grouped together 
 */
abstract class AbstractFileCollection extends AbstractFileView implements FileCollection, Inspectable {

    /**  A list of listeners for this list */
    private final DisposableEventMulticaster<FileViewChangeEvent> listenerSupport;
    
    /** The listener on the ManagedList, to synchronize changes. */
    private final EventListener<FileViewChangeEvent> managedListListener;
    
    /** A rw lock. */
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    
    public AbstractFileCollection(LibraryImpl managedList,
            SourcedEventMulticasterFactory<FileViewChangeEvent, FileView> multicasterFactory) {
        super(managedList);
        this.listenerSupport = multicasterFactory.createDisposableMulticaster(this);
        this.managedListListener = new ManagedListSynchronizer();
    }
    
    /** Initializes this list.  Until the list is initialized, it is not valid. */
    protected void initialize() {
        library.addFileViewListener(managedListListener);
    }

    @Override
    public ListeningFuture<List<ListeningFuture<FileDesc>>> addFolder(final File folder) {
        library.addFolder(folder);
        
        return library.submit(new Callable<List<ListeningFuture<FileDesc>>>() {
            @Override
            public List<ListeningFuture<FileDesc>> call() throws Exception {
                File[] potentials = folder.listFiles(library.newManageableFilter());
                List<ListeningFuture<FileDesc>> futures = new ArrayList<ListeningFuture<FileDesc>>();
                for(File file : potentials) {
                    if(!contains(file)) {
                        futures.add(add(file));
                    }
                }
                return futures;
            }
        });
    }
    
    @Override
    public ListeningFuture<FileDesc> add(File file) {
        FileDesc fd = library.getFileDesc(file);
        if(fd == null) {
            saveChange(canonicalize(file), true); // Save early, will RM if it can't become FD.
            return wrapFuture(library.add(file));
        } else {
            add(fd);
            return futureFor(fd);
        }
    }
    
    @Override
    public ListeningFuture<FileDesc> add(File file, List<? extends LimeXMLDocument> documents) {
        FileDesc fd = library.getFileDesc(file);
        if(fd == null) {
            saveChange(canonicalize(file), true); // Save early, will RM if it can't become FD.
            return wrapFuture(library.add(file, documents));
        } else {
            add(fd);
            return futureFor(fd);
        }
    }
    
    @Override
    public boolean add(FileDesc fileDesc) {
        if(addFileDescImpl(fileDesc)) {
            saveChange(fileDesc.getFile(), true);
            fireAddEvent(fileDesc);
            return true;
        } else {
            // Must rm from save-state, because we optimistically
            // inserted when adding as a file.
            if(!contains(fileDesc)) {
                saveChange(fileDesc.getFile(), false);
            }
            return false;
        }
    }
        
    /**
     * Performs the actual add. No notification is sent when this returns.
     * @return true if the fileDesc was added, false otherwise
     */
    protected boolean addFileDescImpl(FileDesc fileDesc) {
        Objects.nonNull(fileDesc, "fileDesc");        
        rwLock.writeLock().lock();
        try {
            if(isFileAddable(fileDesc) && getIndexes().add(fileDesc.getIndex())) {
                return true;
            } else {
                return false;
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    
    @Override
    public boolean remove(File file) {
        FileDesc fd = library.getFileDesc(file);
        if(fd != null) {
            return remove(fd);
        } else {
            saveChange(canonicalize(file), false);
            return false;
        }        
    }
    
    @Override
    public boolean remove(FileDesc fileDesc) {
        saveChange(fileDesc.getFile(), false);
        if(removeFileDescImpl(fileDesc)) {
            fireRemoveEvent(fileDesc);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Performs the actual remove. No notification is sent when this returns. 
     * @return true if the fileDesc was removed, false otherwise
     */
    protected boolean removeFileDescImpl(FileDesc fileDesc) {
        Objects.nonNull(fileDesc, "fileDesc");        
        rwLock.writeLock().lock();
        try {
            if(getIndexes().remove(fileDesc.getIndex())) {
                return true;
            } else {
                return false;
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    
    @Override
    public Iterator<FileDesc> iterator() {
        return new FileViewIterator(AbstractFileCollection.this, getIndexes());
    }
    
    @Override
    public Iterable<FileDesc> pausableIterable() {
        return new Iterable<FileDesc>() {
            @Override
            public Iterator<FileDesc> iterator() {
                return new ThreadSafeFileViewIterator(AbstractFileCollection.this, library);
            }
        };
    }

    @Override
    public void clear() {
        boolean needsClearing;
        rwLock.writeLock().lock();
        try {
            needsClearing = getIndexes().size() > 0;
            getIndexes().clear();
        } finally {
            rwLock.writeLock().unlock();
        }
        
        if(needsClearing) {
            fireClearEvent();
        }
    }
    
    // Exists here so all subclasses can have access if needed.
    protected List<FileDesc> getFilesInDirectory(File directory) {
        // Remove case, trailing separators, etc.
        try {
            directory = FileUtils.getCanonicalFile(Objects.nonNull(directory, "directory"));
        } catch (IOException e) { // invalid directory ?
            return Collections.emptyList();
        }

        List<FileDesc> list = new ArrayList<FileDesc>();
        rwLock.readLock().lock();
        try {
            for(FileDesc fd : this) {
                if(directory.equals(fd.getFile().getParentFile())) {
                    list.add(fd);
                }
            }
        } finally {
            rwLock.readLock().unlock();
        }
        return list;
    }

    @Override
    public Object inspect() {
        rwLock.readLock().lock();
        try {
            Map<String,Object> inspections = new HashMap<String,Object>();
            inspections.put("num of files", Integer.valueOf(getIndexes().size()));
            return inspections;
        } finally {
            rwLock.readLock().unlock();
        }   
    }
        
    @Override
    public Lock getReadLock() {
        return rwLock.readLock();
    }

    @Override
    public void addFileViewListener(EventListener<FileViewChangeEvent> listener) {
        listenerSupport.addListener(listener);
    }

    @Override
    public void removeFileViewListener(EventListener<FileViewChangeEvent> listener) {
        listenerSupport.removeListener(listener);
    }
    
    /**
     * Fires an addFileDesc event to all the listeners
     * @param fileDesc that was added
     */
    protected void fireAddEvent(FileDesc fileDesc) {
        listenerSupport.broadcast(new FileViewChangeEvent(AbstractFileCollection.this, FileViewChangeEvent.Type.FILE_ADDED, fileDesc));
    }
    
    /**
     * Fires a removeFileDesc event to all the listeners
     * @param fileDesc that was removed
     */
    protected void fireRemoveEvent(FileDesc fileDesc) {
        listenerSupport.broadcast(new FileViewChangeEvent(AbstractFileCollection.this, FileViewChangeEvent.Type.FILE_REMOVED, fileDesc));
    }

    /**
     * Fires a changeEvent to all the listeners
     * @param oldFileDesc FileDesc that was there previously
     * @param newFileDesc FileDesc that replaced oldFileDesc
     */
    protected void fireChangeEvent(FileDesc oldFileDesc, FileDesc newFileDesc) {
        listenerSupport.broadcast(new FileViewChangeEvent(AbstractFileCollection.this, FileViewChangeEvent.Type.FILE_CHANGED, oldFileDesc, newFileDesc));
    }
    
    /** Fires a clear event to all listeners. */
    protected void fireClearEvent() {
        listenerSupport.broadcast(new FileViewChangeEvent(AbstractFileCollection.this, FileViewChangeEvent.Type.FILES_CLEARED));
    }
    
    /** Fires an event when the state of a shared collection changes*/
    protected void fireCollectionEvent(FileViewChangeEvent.Type type, boolean value) {
        listenerSupport.broadcast(new FileViewChangeEvent(type, value));
    }
    
    /**
     * Updates the list if a containing file has been renamed
     */
    protected void updateFileDescs(FileDesc oldFileDesc, FileDesc newFileDesc) {     
        boolean failed = false;
        boolean success = false;
        
        // Unfortunately cannot lock between these, since rm & add can be overridden
        // and the overridden methods cannot be expected to be OK with locks.
        if (removeFileDescImpl(oldFileDesc)) {
            if(addFileDescImpl(newFileDesc)) {
                success = true;
            } else {
                failed = true;
            }
        } // else nothing to remove -- neither success nor failure
        
        if(success) {
            fireChangeEvent(oldFileDesc, newFileDesc);
        } else if(failed) {
            // TODO: What do we want to do here?
            //       This will have the side effect of causing ripples
            //       if a rename/change fails for any reason.
            //saveChange(oldFileDesc.getFile(), false);
            fireRemoveEvent(oldFileDesc);
        }
    }

    /**
     * Returns true if this list is allowed to add this FileDesc
     * @param fileDesc - FileDesc to be added
     */
    protected abstract boolean isFileAddable(FileDesc fileDesc);

    /**
     * Returns true if the XML doc contains information regarding the LWS
     */
    protected boolean isStoreXML(LimeXMLDocument doc) {
       return doc != null && doc.getLicenseString() != null &&
               doc.getLicenseString().equals(LicenseType.LIMEWIRE_STORE_PURCHASE.name());
    }

    public void dispose() {
        library.removeFileViewListener(managedListListener);
        listenerSupport.dispose();
    }
    
    /**
     * Returns true if a newly loaded file from the Managed List should be added to this list.
     * If FileDesc is non-null, it's the FileDesc that will be loaded.
     */
    protected abstract boolean isPending(File file, FileDesc fileDesc);
    
    /** Hook for saving changes to data. */
    protected abstract void saveChange(File file, boolean added);
    
    protected int getMaxIndex() {
        rwLock.readLock().lock();
        try {
            return getIndexes().max();
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    protected int getMinIndex() {
        rwLock.readLock().lock();
        try {
            return getIndexes().min();
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    private File canonicalize(File file) {
        try {
            return FileUtils.getCanonicalFile(file);
        } catch(IOException iox) {
            return file;
        }
    }
    
    private FileDesc throwExecutionExceptionIfNotContains(FileDesc fd) throws ExecutionException {            
        if(contains(fd)) {
            return fd;
        } else {
            throw new ExecutionException(new FileViewChangeFailedException(
                    new FileViewChangeEvent(AbstractFileCollection.this, FileViewChangeEvent.Type.FILE_ADD_FAILED, fd.getFile()),
                    FileViewChangeFailedException.Reason.CANT_ADD_TO_LIST));
        }
    }
    
    private ListeningFuture<FileDesc> wrapFuture(final ListeningFuture<FileDesc> future) {
        return new ListeningFutureDelegator<FileDesc, FileDesc>(future) {
            @Override
            protected FileDesc convertSource(FileDesc source) throws ExecutionException {
                return throwExecutionExceptionIfNotContains(source);
            }
            
            @Override
            protected FileDesc convertException(ExecutionException ee) throws ExecutionException {
                // We can fail because we attempted to add a File that already existed --
                // if that's why we failed, then we return the file anyway (because it is added.)
                if(ee.getCause() instanceof FileViewChangeFailedException) {
                    FileViewChangeFailedException fe = (FileViewChangeFailedException)ee.getCause();
                    if(fe.getEvent().getType() == FileViewChangeEvent.Type.FILE_ADD_FAILED) {
                        if(contains(fe.getEvent().getFile())) {
                            return getFileDesc(fe.getEvent().getFile());
                        }
                    }
                }
                throw ee;
            }
        };
    }    
    
    private ListeningFuture<FileDesc> futureFor(final FileDesc fd) {
        try {
            return new SimpleFuture<FileDesc>(throwExecutionExceptionIfNotContains(fd));
        } catch(ExecutionException ee) {
            return new SimpleFuture<FileDesc>(ee);
        }
    }
    
    private class ManagedListSynchronizer implements EventListener<FileViewChangeEvent> {
        @Override
        public void handleEvent(FileViewChangeEvent event) {
            // Note: We only need to check for pending on adds,
            //       because that's the only kind that doesn't
            //       require it already exists.
            switch(event.getType()) {
            case FILE_ADDED:
                if(isPending(event.getFile(), event.getFileDesc())) {
                    add(event.getFileDesc());
                }
                break;
            case FILE_CHANGED:
                updateFileDescs(event.getOldValue(), event.getFileDesc());
                break;
            case FILE_REMOVED:
                remove(event.getFileDesc());
                break;
            case FILES_CLEARED:
                clear();
                break;
            case FILE_CHANGE_FAILED:
            case FILE_ADD_FAILED:
                // This can fail for double-adds, meaning the FD really does exist.
                // If that's why it failed, we pretend this is really an add.
                FileDesc fd = library.getFileDesc(event.getFile());
                if(fd == null) { // File doesn't exist, it was a real failure.
                    if(isPending(event.getFile(), null) && !contains(event.getFile())) {
                        saveChange(event.getFile(), false);
                    }           
                } else if(isPending(event.getFile(), fd)) {
                    add(fd);
                }
                break;
            }
        }
    }
}
