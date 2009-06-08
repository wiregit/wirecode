package com.limegroup.gnutella.library;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.limewire.collection.CollectionUtils;
import org.limewire.collection.IntSet;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.concurrent.FutureEvent;
import org.limewire.concurrent.ListeningExecutorService;
import org.limewire.concurrent.ListeningFuture;
import org.limewire.concurrent.ListeningFutureTask;
import org.limewire.concurrent.SimpleFuture;
import org.limewire.core.api.Category;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectableContainer;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.inspection.InspectionPoint;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;
import org.limewire.listener.SourcedEventMulticaster;
import org.limewire.listener.SwingSafePropertyChangeSupport;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.ExceptionUtils;
import org.limewire.util.FileUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnSet;
import com.limegroup.gnutella.auth.UrnValidator;
import com.limegroup.gnutella.auth.ValidationEvent;
import com.limegroup.gnutella.downloader.VerifyingFile;
import com.limegroup.gnutella.malware.DangerousFileChecker;
import com.limegroup.gnutella.xml.LimeXMLDocument;

@Singleton
class LibraryImpl implements Library, FileCollection {
    
    private static final Log LOG = LogFactory.getLog(LibraryImpl.class);
    
    private final SourcedEventMulticaster<FileDescChangeEvent, FileDesc> fileDescMulticaster;
    private final EventMulticaster<LibraryStatusEvent> managedListListenerSupport;
    private final EventMulticaster<FileViewChangeEvent> fileListListenerSupport;
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final UrnCache urnCache;
    private final FileDescFactory fileDescFactory; 
    private final ListeningExecutorService fileLoader;
    private final PropertyChangeSupport changeSupport;
    private final DangerousFileChecker dangerousFileChecker;
    
    /** 
     * The list of complete and incomplete files.  An entry is null if it
     *  is no longer managed.
     * INVARIANT: for all i, files[i]==null, or files[i].index==i and either
     *  files[i]._path is in a managed directory with a managed extension or
     *  files[i]._path is the incomplete directory if files[i] is an IncompleteFileDesc.
     */
    private final List<FileDesc> files;
    
    /**
     * An index that maps a <tt>File</tt> on disk to the 
     *  <tt>FileDesc</tt> holding it.
     *
     * INVARIANT: For all keys k in _fileToFileDescMap, 
     *  files[_fileToFileDescMap.get(k).getIndex()].getFile().equals(k)
     *
     * Keys must be canonical <tt>File</tt> instances.
     */
    private final Map<File, FileDesc> fileToFileDescMap;
    
    /**
     * The map of pending calculations for each File.
     */
    private final Map<File, Future> fileToFutures;
 
    /**
     * A map of appropriately case-normalized URN strings to the
     * indices in files.  Used to make query-by-hash faster.
     * 
     * INVARIANT: for all keys k in urnMap, for all i in urnMap.get(k),
     * files[i].containsUrn(k).  Likewise for all i, for all k in
     * files[i].getUrns(), rnMap.get(k) contains i.
     */
    private final Map<URN, IntSet> urnMap;
    
    /**
     * The set of file extensions to manage, sorted by StringComparator. 
     * INVARIANT: all extensions are lower case.
     */
    private final Set<String> extensions;
    
    /**
     * The revision of the library.  Every time 'loadSettings' is called, the revision
     * is incremented.
     */
    @InspectablePrimitive("filemanager revision")
    private final AtomicInteger revision = new AtomicInteger();
    
    /** All the library data for this library -- loaded on-demand. */
    private final LibraryFileData fileData = new LibraryFileData();  
    
    /** The validator to ask if URNs are OK. */
    private final UrnValidator urnValidator;
    
    /** The revision this finished loading. */
    private volatile int loadingFinished = -1;
    
    /** The number of files that are pending calculation. */
    private final AtomicInteger pendingFiles = new AtomicInteger(0);
    
    @SuppressWarnings("unused")
    @InspectableContainer
    private class LazyInspectableContainer {
        @InspectionPoint("managed files")
        private final Inspectable inspectable = new Inspectable() {
            @Override
            public Object inspect() {
                Map<String, Object> data = new HashMap<String, Object>();
                data.put("size", size());
                return data;
            }
        };
    }

    @Inject
    LibraryImpl(SourcedEventMulticaster<FileDescChangeEvent, FileDesc> fileDescMulticaster,
                        UrnCache urnCache,
                        FileDescFactory fileDescFactory,
                        EventMulticaster<LibraryStatusEvent> managedListSupportMulticaster,
                        UrnValidator urnValidator,
                        DangerousFileChecker dangerousFileChecker) {
        this.urnCache = urnCache;
        this.fileDescFactory = fileDescFactory;
        this.fileDescMulticaster = fileDescMulticaster;
        this.managedListListenerSupport = managedListSupportMulticaster;
        this.fileListListenerSupport = new EventMulticasterImpl<FileViewChangeEvent>();
        this.fileLoader = ExecutorsHelper.newProcessingQueue("ManagedList Loader");
        this.files = new ArrayList<FileDesc>();
        this.extensions = new ConcurrentSkipListSet<String>();
        this.urnMap = new HashMap<URN, IntSet>();
        this.fileToFileDescMap = new HashMap<File, FileDesc>();
        this.fileToFutures = new HashMap<File, Future>();
        this.urnValidator = urnValidator;
        this.changeSupport = new SwingSafePropertyChangeSupport(this);
        this.dangerousFileChecker = dangerousFileChecker;
    }
    
    @Override
    public List<FileDesc> getFilesInDirectory(File directory) {
        throw new UnsupportedOperationException("unsupported");
    }
    
    @Override
    public long getNumBytes() {
        throw new UnsupportedOperationException("unsupported");
    }
    
    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }
    
    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }
    
    /** Gets the library data, loading it if necessary. */
    LibraryFileData getLibraryData() {
        if(!fileData.isLoaded()) {
            fileData.load();
        }
        return fileData;
    }
    
    /**
     * Runs the callable in the managed filelist thread & returns a Future used
     * to get its result.
     */
    <V> ListeningFuture<V> submit(Callable<V> callable) {
        return fileLoader.submit(callable);
    }
    
    /** Initializes all listeners. */
    void initialize() {
        fileDescMulticaster.addListener(new EventListener<FileDescChangeEvent>() {
            @Override
            public void handleEvent(FileDescChangeEvent event) {
                switch(event.getType()) {
                case URNS_CHANGED:
                    if(event.getUrn() == null || event.getUrn().isSHA1()) {
                        rwLock.writeLock().lock();
                        try {
                            updateUrnIndex(event.getSource());
                        } finally {
                            rwLock.writeLock().unlock();
                        }
                    }
                    break;
                }
            }
        });
        
        urnValidator.addListener(new EventListener<ValidationEvent>() {
            @Override
            public void handleEvent(ValidationEvent event) {
                switch(event.getType()) {
                case INVALID:
                    List<FileDesc> fds = getFileDescsMatching(event.getUrn());
                    for(FileDesc fd : fds) {
                        remove(fd);
                    }
					break;
                }
            }
        });
    }
    
    /**
     * Dispatches a failure, sending a CHANGE_FAILED & REMOVE event if
     * oldFileDesc is non-null, an ADD_FAILED otherwise.
     */
    private FileViewChangeEvent dispatchFailure(File file, FileDesc oldFileDesc) {
        FileViewChangeEvent event;
        if(oldFileDesc != null) {
            event = new FileViewChangeEvent(this, FileViewChangeEvent.Type.FILE_CHANGE_FAILED, oldFileDesc.getFile(), oldFileDesc, file);
            // First dispatch a CHANGE_FAILED for the new event
            dispatch(event);
            // Then dispatch a REMOVE for the old FD.
            dispatch(new FileViewChangeEvent(this, FileViewChangeEvent.Type.FILE_REMOVED, oldFileDesc));
        } else {
            event = new FileViewChangeEvent(this, FileViewChangeEvent.Type.FILE_ADD_FAILED, file);
            // Just dispatch an ADD_FAIL for the file.
            dispatch(event);
        }
        
        return event;
    }
    
    void dispatch(FileViewChangeEvent event) {
        fileListListenerSupport.broadcast(event);
    }
    
    void dispatch(LibraryStatusEvent event) {
        managedListListenerSupport.broadcast(event);
    }

    @Override
    public FileDesc getFileDesc(File file) {
        file = FileUtils.canonicalize(file);        
        rwLock.readLock().lock();
        try {
            return fileToFileDescMap.get(file);
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    @Override
    public FileDesc getFileDescForIndex(int index) {
        rwLock.readLock().lock();
        try {
            if(index < 0 || index >= files.size()) {
                return null;
            }
            return files.get(index);
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    @Override
    public boolean contains(File file) {
        rwLock.readLock().lock();
        try {
            return fileToFileDescMap.containsKey(FileUtils.canonicalize(file));
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    
    @Override
    public boolean contains(FileDesc fileDesc) {
        rwLock.readLock().lock();
        try {
            return files.get(fileDesc.getIndex()) == fileDesc;
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    /** Adds this incomplete file to the list of managed files */
    void addIncompleteFile(File incompleteFile,
                           Set<? extends URN> urns,
                           String name,
                           long size,
                           VerifyingFile vf) {
        // Note -- Purposely not using canonicalize so we can fail on invalid ones.
        try {
            incompleteFile = FileUtils.getCanonicalFile(incompleteFile);
        } catch(IOException ioe) {
            //invalid file?... don't add incomplete file.
            return;
        }
        
        FileDesc fd = null;
        rwLock.writeLock().lock();
        try {
            if(!fileToFileDescMap.containsKey(incompleteFile)) {
                // no indices were found for any URN associated with this
                // IncompleteFileDesc... add it.
                fd = fileDescFactory.createIncompleteFileDesc(incompleteFile, urns, files.size(), name, size, vf);
                files.add(fd);
                fileToFileDescMap.put(incompleteFile, fd);
                updateUrnIndex(fd);
            }
        } finally {
            rwLock.writeLock().unlock();
        }
        
        if(fd != null) {
            dispatch(new FileViewChangeEvent(LibraryImpl.this, FileViewChangeEvent.Type.FILE_ADDED, fd));
        }
    }

    @Override
    public void addListener(EventListener<FileViewChangeEvent> listener) {
        fileListListenerSupport.addListener(listener);
    }
    
    @Override
    public boolean removeListener(EventListener<FileViewChangeEvent> listener) {
        return fileListListenerSupport.removeListener(listener);
    }
    
    @Override
    public void addManagedListStatusListener(EventListener<LibraryStatusEvent> listener) {
        managedListListenerSupport.addListener(listener);
    }
    
    @Override
    public void removeManagedListStatusListener(EventListener<LibraryStatusEvent> listener) {
        managedListListenerSupport.removeListener(listener);
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("cannot clear managed list");
    }
    
    @Override
    public FileDesc getFileDesc(URN urn) {
        List<FileDesc> matching = getFileDescsMatching(urn);
        if(matching.isEmpty()) {
            return null;
        } else {
            return matching.get(0);
        }
    }
    
    @Override
    public List<FileDesc> getFileDescsMatching(URN urn) {
        rwLock.readLock().lock();
        try {
            IntSet urnsMatching = urnMap.get(urn);
            if(urnsMatching == null || urnsMatching.size() == 0) {
                return Collections.emptyList();
            } else if(urnsMatching.size() == 1) { // Optimal case
                return Collections.singletonList(files.get(urnsMatching.iterator().next()));
            } else {
                return CollectionUtils.listOf(new FileViewIterator(this, urnsMatching));
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public Lock getReadLock() {
        return rwLock.readLock();
    }
    
    @Override
    public Iterator<FileDesc> iterator() {
        return CollectionUtils.readOnlyIterator(fileToFileDescMap.values().iterator());
    }
    
    @Override
    public Iterable<FileDesc> pausableIterable() {
        return new Iterable<FileDesc>() {
            @Override
            public Iterator<FileDesc> iterator() {
                rwLock.readLock().lock();
                try {
                    return new ThreadSafeLibraryIterator();
                } finally {
                    rwLock.readLock().unlock();
                }
            }
        };
    }

    @Override
    public boolean remove(FileDesc fileDesc) {
        return remove(fileDesc.getFile());
    }

    @Override
    public int size() {
        rwLock.readLock().lock();
        try {
            return fileToFileDescMap.size();
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    @Override
    public Collection<String> getDefaultManagedExtensions() {
        return fileData.getDefaultManagedExtensions();
    }
    
    @Override
    public Map<Category, Collection<String>> getExtensionsPerCategory() {
        return fileData.getExtensionsPerCategory();
    }
    
    @Override
    public Collection<Category> getManagedCategories() {
        return fileData.getManagedCategories();
    }
    
    @Override
    public boolean add(FileDesc fileDesc) {
        // an FD should never exist unless it is already in the library
        assert contains(fileDesc);
        return true;
    }
    
    @Override
    public ListeningFuture<FileDesc> add(File file) {
        return add(file, LimeXMLDocument.EMPTY_LIST);
    }    
    
    @Override
    public ListeningFuture<FileDesc> add(File file, List<? extends LimeXMLDocument> list) {
        return add(file, list, revision.get(), null);
    }
    
    /**
     * Adds a managed file.  Returns a future that can be used to get the FD or failure
     * event from adding the file.  Failures are throws as ExecutionExceptions from the Future.
     * 
     * @param file - the file to be added
     * @param metadata - any LimeXMLDocs associated with this file
     * @param rev - current  version of LimeXMLDocs being used
     * @param oldFileDesc the old FileDesc this is replacing
     */
    private ListeningFuture<FileDesc> add(File file, 
            final List<? extends LimeXMLDocument> metadata,
            final int rev,
            final FileDesc oldFileDesc) {
        LOG.debugf("Attempting to load file: {0}", file);
        
        // Make sure capitals are resolved properly, etc.
        try {
            file = FileUtils.getCanonicalFile(file);
        } catch (IOException e) {
            LOG.debugf("Not adding {0} because canonicalize failed", file);
            FileViewChangeEvent event = dispatchFailure(file, oldFileDesc);
            return new SimpleFuture<FileDesc>(new FileViewChangeFailedException(event, FileViewChangeFailedException.Reason.CANT_CANONICALIZE));
        }
        
        rwLock.readLock().lock();
        try {
            // Exit if already added.
            if(fileToFileDescMap.containsKey(file)) {
                LOG.debugf("Not loading because file already loaded {0}", file);
                FileViewChangeEvent event = dispatchFailure(file, oldFileDesc);
                return new SimpleFuture<FileDesc>(new FileViewChangeFailedException(event, FileViewChangeFailedException.Reason.ALREADY_MANAGED));
            }
        } finally {
            rwLock.readLock().unlock();
        }
        
        //make sure a FileDesc can be created from this file
        if (!LibraryUtils.isFilePhysicallyManagable(file)) {
            LOG.debugf("Not adding {0} because file isn't physically manageable", file);
            FileViewChangeEvent event = dispatchFailure(file, oldFileDesc);
            return new SimpleFuture<FileDesc>(new FileViewChangeFailedException(event, FileViewChangeFailedException.Reason.NOT_MANAGEABLE));
        }
        
        if (!LibraryUtils.isFileAllowedToBeManaged(file)) {
            LOG.debugf("Not adding {0} because programs are not allowed to be manageable", file);
            FileViewChangeEvent event = dispatchFailure(file, oldFileDesc);
            return new SimpleFuture<FileDesc>(new FileViewChangeFailedException(event, FileViewChangeFailedException.Reason.PROGRAMS_NOT_MANAGEABLE));
        }
        
        final File interned = new File(file.getPath().intern());
        getLibraryData().addManagedFile(interned);

        boolean failed = false;
        rwLock.writeLock().lock();
        try {
            if (rev != revision.get()) {
                failed = true;
            }
        } finally {
            rwLock.writeLock().unlock();
        }
        
        if(failed) {
            LOG.debugf("Not adding {0} because revisions changed while loading", interned);
            FileViewChangeEvent event = dispatchFailure(interned, oldFileDesc);
            return new SimpleFuture<FileDesc>(new FileViewChangeFailedException(event, FileViewChangeFailedException.Reason.REVISIONS_CHANGED));
        } else {
            final PendingFuture task = new PendingFuture();
            ListeningFuture<Set<URN>> urnFuture = urnCache.calculateAndCacheUrns(interned);
            if(!urnFuture.isDone()) {
                rwLock.writeLock().lock();
                try {
                    fileToFutures.put(interned, urnFuture);
                } finally {
                    rwLock.writeLock().unlock();
                }
            }
            
            urnFuture.addFutureListener(new EventListener<FutureEvent<Set<URN>>>() {
                @Override
                public void handleEvent(final FutureEvent<Set<URN>> event) {
                    // Report the exception, if one happened, so we know about it.
                    if(event.getException() != null) {
                        ExceptionUtils.reportOrReturn(event.getException());
                    }
                    rwLock.writeLock().lock();
                    try {
                        if(rev != revision.get()) {
                            FileViewChangeEvent fileEvent = dispatchFailure(interned, oldFileDesc);
                            task.setException(new FileViewChangeFailedException(fileEvent, FileViewChangeFailedException.Reason.REVISIONS_CHANGED));
                        } else {
                            fileToFutures.remove(interned);
                            fileToFutures.put(interned, fileLoader.submit(new Runnable() {
                                @Override
                                public void run() {
                                    finishLoadingFileDesc(interned, event, metadata, rev, oldFileDesc, task);
                                }
                            }));
                        }
                    } finally {
                        rwLock.writeLock().unlock();
                    }
                }
            });
            return task;
        }
    }
    
    /** Finishes the process of loading the FD, now that URNs are known. */
    private void finishLoadingFileDesc(File file, FutureEvent<Set<URN>> urnEvent,
            List<? extends LimeXMLDocument> metadata, int rev, FileDesc oldFileDesc,
            PendingFuture task) {
        FileDesc fd = null;
        boolean revchange = false;
        boolean failed = false;
        
        // Don't add dangerous files to the library (this call may block)
        boolean dangerous = dangerousFileChecker.isDangerous(file);
        
        Set<URN> urns = urnEvent.getResult();
        rwLock.writeLock().lock();
        try {
            if(rev != revision.get()) {
                revchange = true;
                LOG.warn("Revisions changed, dropping share.");
            } else {
                fileToFutures.remove(file);
                
                // Only load the file if we were able to calculate URNs 
                // assume the fd is being shared
                if(urns != null && !urns.isEmpty() && !dangerous) {
                    fd = createFileDesc(file, urns, files.size());
                    if(fd != null) {
                        if(contains(file)) {
                            failed = true;
                            fd = getFileDesc(file);
                        } else {
                            files.add(fd);
                            fileToFileDescMap.put(file, fd);
                            updateUrnIndex(fd);
                        }
                    }
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }
        
        if(revchange) {
            FileViewChangeEvent event = dispatchFailure(file, oldFileDesc);
            task.setException(new FileViewChangeFailedException(event, FileViewChangeFailedException.Reason.REVISIONS_CHANGED));
        } else if(urnEvent.getType() != FutureEvent.Type.SUCCESS) {
            FileViewChangeEvent event = dispatchFailure(file, oldFileDesc);
            task.setException(new FileViewChangeFailedException(event, FileViewChangeFailedException.Reason.ERROR_LOADING_URNS, urnEvent.getException()));
        } else if(dangerous) {
            LOG.debugf("Not adding {0} because the file is dangerous", file);
            FileViewChangeEvent event = dispatchFailure(file, oldFileDesc);
            task.setException(new FileViewChangeFailedException(event, FileViewChangeFailedException.Reason.DANGEROUS_FILE));
        } else if(fd == null) {
            FileViewChangeEvent event = dispatchFailure(file, oldFileDesc);
            task.setException(new FileViewChangeFailedException(event, FileViewChangeFailedException.Reason.CANT_CREATE_FD));
        } else if(failed) {
            LOG.debugf("Couldn't load FD because FD with file {0} exists already.  FD: {1}", file, fd);
            FileViewChangeEvent event = dispatchFailure(file, oldFileDesc);
            task.setException(new FileViewChangeFailedException(event, FileViewChangeFailedException.Reason.ALREADY_MANAGED));
        } else { // SUCCESS!
            // try loading the XML for this fileDesc
            fileDescMulticaster.broadcast(new FileDescChangeEvent(fd, FileDescChangeEvent.Type.LOAD, metadata));
            
            // It is very important that the events get dispatched
            // prior to setting the value on the task, so that other FileLists
            // listening to these events can receive & process the event
            // prior to the future.get() returning.
            if(oldFileDesc == null) {
                LOG.debugf("Added file: {0}", file);
                dispatch(new FileViewChangeEvent(this, FileViewChangeEvent.Type.FILE_ADDED, fd));
            } else {
                LOG.debugf("Changed to new file: {0}", file);
                dispatch(new FileViewChangeEvent(this, FileViewChangeEvent.Type.FILE_CHANGED, oldFileDesc, fd));
            }
            
            task.set(fd);
        }
    }
  
    /**
     * Creates an FD for the file.  Returns null if the FD cannot be created
     * (because the URN validator says it's not valid, for example).
     */
    private FileDesc createFileDesc(File file, Set<? extends URN> urns, int index){
        if(urnValidator.isInvalid(UrnSet.getSha1(urns))) {
            return null;
        } else {
            return fileDescFactory.createFileDesc(file, urns, index);
        }
    }

    @Override
    public boolean remove(File file) {
        LOG.debugf("Removing file: {0}", file);                

        file = FileUtils.canonicalize(file);
        FileDesc fd = removeInternal(file);        
        if(fd != null) {
            dispatch(new FileViewChangeEvent(this, FileViewChangeEvent.Type.FILE_REMOVED, fd));
        }
        return fd != null;
    }
    
    /**
     * Removes the FD for this file, returning the FD that was removed.
     * This does NOT dispatch a remove event.  It will update the libraryData
     * to signify the file should not be shared, though.
     * 
     * The file should be canonicalized already.
     */
    private FileDesc removeInternal(File file) {
        FileDesc fd;
        rwLock.writeLock().lock();
        try {
            fd = fileToFileDescMap.get(file);
            if(fd != null) {
                removeFileDesc(file, fd);
            }
        } finally {
            rwLock.writeLock().unlock();
        }
        if(fd != null) {
            getLibraryData().removeManagedFile(file);
        }
        return fd;
    }
    
    /** Removes the given FD. */
    private void removeFileDesc(File file, FileDesc fd) {
        removeUrnIndex(fd);
        FileDesc rm = files.set(fd.getIndex(), null);
        assert rm == fd;
        rm = fileToFileDescMap.remove(file);
        assert rm == fd;
        fileDescMulticaster.removeListeners(fd);
    }

    /** Generic method for adding a fileDesc's URNS to a map */
    private void updateUrnIndex(FileDesc fileDesc) {
        URN sha1 = UrnSet.getSha1(fileDesc.getUrns());
        IntSet indices = urnMap.get(sha1);
        if (indices == null) {
            indices = new IntSet();
            urnMap.put(sha1, indices);
        }
        indices.add(fileDesc.getIndex());
    }
    
    /** 
     * Removes stored indices for a URN associated with a given FileDesc
     */
    private void removeUrnIndex(FileDesc fileDesc) {
        URN sha1 = UrnSet.getSha1(fileDesc.getUrns());
        // Lookup each of desc's URN's ind _urnMap.
        IntSet indices = urnMap.get(sha1);
        if (indices == null) {
            assert fileDesc instanceof IncompleteFileDesc;
            return;
        }

        // Delete index from set. Remove set if empty.
        indices.remove(fileDesc.getIndex());
        if(indices.size() == 0) {
            urnMap.remove(sha1);
        }
    }
    
    @Override
    public ListeningFuture<FileDesc> fileRenamed(File oldName, final File newName) {
        LOG.debugf("Attempting to rename: {0} to: {1}", oldName, newName);      
        
        oldName = FileUtils.canonicalize(oldName);
        FileDesc fd = removeInternal(oldName);        
        if (fd != null) {
            // TODO: It's dangerous to prepopulate, because we might actually
            //       be called with wrong data, giving us wrong URNs.
            // Prepopulate the cache with new URNs.
            urnCache.addUrns(newName, fd.getUrns());
            List<LimeXMLDocument> xmlDocs = new ArrayList<LimeXMLDocument>(fd.getLimeXMLDocuments());
            return add(newName, xmlDocs, revision.get(), fd);
        } else {
            return new SimpleFuture<FileDesc>(new FileViewChangeFailedException(new FileViewChangeEvent(this, FileViewChangeEvent.Type.FILE_CHANGE_FAILED, oldName, null, newName), FileViewChangeFailedException.Reason.OLD_WASNT_MANAGED));
        }
    }
    
    @Override
    public ListeningFuture<FileDesc> fileChanged(File file, List<? extends LimeXMLDocument> xmlDocs) {
        LOG.debugf("File Changed: {0}", file);

        file = FileUtils.canonicalize(file);
        FileDesc fd = removeInternal(file);
        if (fd != null) {
            urnCache.removeUrns(file); // Explicitly remove URNs to force recalculating.
            return add(file, xmlDocs, revision.get(), fd);
        } else {
            return new SimpleFuture<FileDesc>(new FileViewChangeFailedException(new FileViewChangeEvent(this, FileViewChangeEvent.Type.FILE_CHANGE_FAILED, file, null, file), FileViewChangeFailedException.Reason.OLD_WASNT_MANAGED));
        }
    }
    
    /**
     * Loads all files from prior sessions.
     * This returns immediately with a Future that contains
     * the list of all Future FileDescs that will be added.
     */  
    ListeningFuture<List<ListeningFuture<FileDesc>>> loadManagedFiles() {
        final int currentRevision = revision.incrementAndGet();
        LOG.debugf("Starting new library revision: {0}", currentRevision);
        
        ListeningFuture<List<ListeningFuture<FileDesc>>> future = submit(new Callable<List<ListeningFuture<FileDesc>>>() {
            @Override
            public List<ListeningFuture<FileDesc>> call() {
                return loadSettingsInternal(currentRevision);
            }
        });
        return future;
    }

    /** 
     * Loads all shared files, putting them in a queue for being added.
     *
     * If the current revision ever changed from the expected revision, this returns
     * immediately.
     */
    private List<ListeningFuture<FileDesc>> loadSettingsInternal(int rev) {
        LOG.debugf("Loading Library Revision: {0}", rev);
        
        List<Future> fileFutures;
        rwLock.writeLock().lock();
        try {
            fileFutures = new ArrayList<Future>(fileToFutures.values());
            fileToFutures.clear();
            files.clear();
            urnMap.clear();
            fileToFileDescMap.clear();
            extensions.clear();
            extensions.addAll(getLibraryData().getExtensionsInManagedCategories());
        } finally {
            rwLock.writeLock().unlock();
        }
        
        for(Future future : fileFutures) {
            future.cancel(true);
        }
        
        dispatch(new FileViewChangeEvent(LibraryImpl.this, FileViewChangeEvent.Type.FILES_CLEARED));
        
        fireLoading();
        final List<ListeningFuture<FileDesc>> futures = loadManagedFiles(rev);
        addLoadingListener(futures, rev);
        LOG.debugf("Finished queueing files for revision: {0}", rev);
        return futures;
    }
    
    void fireLoading() {
        changeSupport.firePropertyChange("hasPending", false, true);
    }
    
    private void addLoadingListener(final List<ListeningFuture<FileDesc>> futures, final int rev) {
        if(futures.isEmpty() && pendingFiles.get() == 0) {
            loadFinished(rev);
        } else if(!futures.isEmpty()) {
            pendingFiles.addAndGet(futures.size());
            
            EventListener<FutureEvent<FileDesc>> listener = new EventListener<FutureEvent<FileDesc>>() {
                @Override
                public void handleEvent(FutureEvent<FileDesc> event) {
                    if(pendingFiles.addAndGet(-1) == 0) {
                        loadFinished(rev); 
                    }
                }
            };
            
            for(ListeningFuture<FileDesc> future : futures) {
                future.addFutureListener(listener);
            }
        }
    }
    
    /** Kicks off necessary stuff for loading being done. */
    private void loadFinished(int rev) {
        changeSupport.firePropertyChange("hasPending", true, false);
        if(loadingFinished != rev) {
            loadingFinished = rev;
            LOG.debugf("Finished loading revision: {0}", rev);
            dispatch(new LibraryStatusEvent(this, LibraryStatusEvent.Type.LOAD_FINISHING));
            save();
            dispatch(new LibraryStatusEvent(this, LibraryStatusEvent.Type.LOAD_COMPLETE));
        }
    }
    
    @Override
    public boolean isLoadFinished() {
        return loadingFinished == revision.get();
    }

    private List<ListeningFuture<FileDesc>> loadManagedFiles(int rev) {
        List<ListeningFuture<FileDesc>> futures = new ArrayList<ListeningFuture<FileDesc>>();
        
        // TODO: We want to always share this stuff, not just approved extensions.
//        addManagedDirectory(extensions, LibraryUtils.PROGRAM_SHARE, rev, false, true, true, futures);
//        addManagedDirectory(extensions, LibraryUtils.PREFERENCE_SHARE, rev, false, true, true, futures);
        
        // A listener that will remove individually managed files if they can't load.
        EventListener<FutureEvent<FileDesc>> indivListeners = new EventListener<FutureEvent<FileDesc>>() {
            @Override
            public void handleEvent(FutureEvent<FileDesc> event) {
                switch(event.getType()) {
                case EXCEPTION:
                    if(event.getException().getCause() instanceof FileViewChangeFailedException) {
                        FileViewChangeFailedException ex = (FileViewChangeFailedException)event.getException().getCause();
                        switch(ex.getReason()) {
                        case CANT_CANONICALIZE:
                        case CANT_CREATE_FD:
                        case NOT_MANAGEABLE:
                        case PROGRAMS_NOT_MANAGEABLE:
                            getLibraryData().removeManagedFile(ex.getEvent().getFile());
                            break;
                        }
                    }
                    break;
                }
            }
        };
        
        for(File file : getLibraryData().getManagedFiles()) {
            if(rev != revision.get()) {
                break;
            }

            ListeningFuture<FileDesc> future = add(file, LimeXMLDocument.EMPTY_LIST, rev, null);
            future.addFutureListener(indivListeners);
            futures.add(future);
        }
        
        return futures;
    }

    /** Dispatches a SAVE event & tells library data to save. */
    void save() {
        dispatch(new LibraryStatusEvent(this, LibraryStatusEvent.Type.SAVE));
        urnCache.persistCache();
        getLibraryData().save();
    }
    
    /** Returns the current revision.  Revisions are incmemented when loadManagedFiles is called. */
    int revision() {
        return revision.get();
    }

    /**
     * Returns true if this folder is can have files from it added.
     */
    private boolean isFolderManageable(File folder) {
        folder = FileUtils.canonicalize(folder);
        
        if (!folder.isDirectory() || !folder.canRead() || !folder.exists() && folder.getParent() != null) {
            return false;
        }

        if (getLibraryData().isIncompleteDirectory(folder)) {
            return false;
        }

        if (LibraryUtils.isApplicationSpecialShareDirectory(folder)) {
            return false;
        }

        if (LibraryUtils.isFolderBanned(folder)) {
            return false;
        }

        return true;
    }
    
    /** An iterator that works over changes to the list. */
    private class ThreadSafeLibraryIterator implements Iterator<FileDesc> {        
        /** Points to the index that is to be examined next. */
        private int startRevision = revision.get();
        private int index = 0;
        private FileDesc preview;
        
        private boolean preview() {
            assert preview == null;            
            if (revision.get() != startRevision) {
                return false;
            }

            rwLock.readLock().lock();
            try {
                while (index < files.size()) {
                    preview = files.get(index);
                    index++;
                    if (preview != null) {
                        return true;
                    }
                }            
                return false;
            } finally {
                rwLock.readLock().unlock();
            }
            
        }
        
        @Override
        public boolean hasNext() {
            if (revision.get() != startRevision) {
                return false;
            }

            if (preview != null) {
                if (!contains(preview)) {
                    // file was removed in the meantime
                    preview = null;
                }
            }
            return preview != null || preview();
        }
        
        @Override
        public FileDesc next() {
            if (hasNext()) {
                FileDesc item = preview;
                preview = null;
                return item;
            }
            throw new NoSuchElementException();     
        }
        
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
    
    /** Returns true if file has a manageable extension.  Case is ignored. */
    boolean hasManageableExtension(File file) {
        return extensions.contains(FileUtils.getFileExtension(file).toLowerCase(Locale.US));
    }
    
    /** Returns a filter used to get manageable files. */
    FileFilter newManageableFilter() {
        return new ManageableFileFilter(extensions, true);
    }
    
    @Override
    public boolean isDirectoryAllowed(File folder) {
        return folder.isDirectory() && isFolderManageable(folder);
    }
    
    @Override
    public boolean isProgramManagingAllowed() {
        return getLibraryData().isProgramManagingAllowed();
    }
    
    /** A filter used to see if a file is manageable. */
    private class ManageableFileFilter implements FileFilter {
        private final Set<String> extensions;
        private final boolean includeContainedFiles;
        
        /** Constructs the filter with the given set of allowed extensions. */
        public ManageableFileFilter(Collection<String> extensions, boolean includeContainedFiles) {
            this.extensions = new HashSet<String>(extensions);
            this.includeContainedFiles = includeContainedFiles;
        }
        
        @Override
        public boolean accept(File file) {
            return file.isFile()
                && (includeContainedFiles || !contains(file))
                && LibraryUtils.isFileManagable(file)
                && extensions.contains(FileUtils.getFileExtension(file).toLowerCase(Locale.US));
        }
    }

    /** A simple empty callable for use in PendingFuture. */
    private final static Callable<FileDesc> EMPTY_CALLABLE = new Callable<FileDesc>() {
        @Override
        public FileDesc call() { return null; }
    };
    
    /** A future that delegates on another future, occasionally. */
    private static class PendingFuture extends ListeningFutureTask<FileDesc> {
        public PendingFuture() {
            super(EMPTY_CALLABLE);
        }
        
        @Override
        public void run() {
            // Do nothing -- there is nothing to run.
        }

        @Override
        // Raise access so we can set the FD. */
        public void set(FileDesc v) {
            super.set(v);
        }

        @Override
        // Raise access so we can set the error. */
        public void setException(Throwable t) {
            super.setException(t);
        }
    }

    @Override
    public void setCategoriesToIncludeWhenAddingFolders(Collection<Category> managedCategories) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setManagedExtensions(Collection<String> extensions) {
        // TODO Auto-generated method stub
    }

    @Override
    public ListeningFuture<List<ListeningFuture<FileDesc>>> addFolder(File folder) {
        // TODO Auto-generated method stub
        return null;
    }
}
