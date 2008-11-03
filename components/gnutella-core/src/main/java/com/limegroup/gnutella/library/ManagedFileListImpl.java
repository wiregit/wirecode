package com.limegroup.gnutella.library;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.limewire.collection.CollectionUtils;
import org.limewire.collection.IntSet;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.concurrent.SimpleFuture;
import org.limewire.inspection.InspectableForSize;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;
import org.limewire.listener.SourcedEventMulticaster;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.FileUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnSet;
import com.limegroup.gnutella.auth.UrnValidator;
import com.limegroup.gnutella.auth.ValidationEvent;
import com.limegroup.gnutella.downloader.VerifyingFile;
import com.limegroup.gnutella.xml.LimeXMLDocument;

@Singleton
class ManagedFileListImpl implements ManagedFileList, FileList {
    
    private static final Log LOG = LogFactory.getLog(ManagedFileListImpl.class);
    
    private final SourcedEventMulticaster<FileDescChangeEvent, FileDesc> fileDescMulticaster;
    private final EventMulticaster<ManagedListStatusEvent> managedListListenerSupport;
    private final EventMulticaster<FileListChangedEvent> fileListListenerSupport;
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final UrnCache urnCache;
    private final FileDescFactory fileDescFactory; 
    private final ExecutorService fileLoader;
    
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
     * A map of appropriately case-normalized URN strings to the
     * indices in files.  Used to make query-by-hash faster.
     * 
     * INVARIANT: for all keys k in urnMap, for all i in urnMap.get(k),
     * files[i].containsUrn(k).  Likewise for all i, for all k in
     * files[i].getUrns(), rnMap.get(k) contains i.
     */
    private final Map<URN, IntSet> urnMap;
    
    /** 
     * The total number of files that are pending managed.
     *  (ie: awaiting hashing or being added)
     */
    @InspectablePrimitive("number of pending files")
    private int numPendingFiles;
    
    /**
     * The set of file extensions to manage, sorted by StringComparator. 
     * INVARIANT: all extensions are lower case.
     */
    private final Set<String> extensions;
    
    /**
     * A Set of managed directories that are completely shared.  Files in these
     * directories are managed by default and will be managed unless the File is
     * listed in the set of files not to share.
     */
    @InspectableForSize("number completely managed directories")
    private final Set<File> managedDirectories;
    
    /**
     * The revision of the library.  Every time 'loadSettings' is called, the revision
     * is incremented.
     */
    @InspectablePrimitive("filemanager revision")
    private final AtomicInteger revision = new AtomicInteger();
    
    /** The revision that finished loading all pending files. */
    @InspectablePrimitive("revision that finished loading")
    private volatile int pendingFinished = -1;
    
    /** The revision that finished updating shared directories. */
    private volatile int updatingFinished = -1;
    
    /** The last revision that finished both pending & updating. */
    private volatile int loadingFinished = -1;
    
    /** All the library data for this library -- loaded on-demand. */
    private final LibraryFileData fileData = new LibraryFileData();  
    
    /** The validator to ask if URNs are OK. */
    private final UrnValidator urnValidator;

    @Inject
    ManagedFileListImpl(SourcedEventMulticaster<FileDescChangeEvent, FileDesc> fileDescMulticaster,
                        UrnCache urnCache,
                        FileDescFactory fileDescFactory,
                        EventMulticaster<ManagedListStatusEvent> managedListSupportMulticaster,
                        UrnValidator urnValidator) {
        this.urnCache = urnCache;
        this.fileDescFactory = fileDescFactory;
        this.fileDescMulticaster = fileDescMulticaster;
        this.managedListListenerSupport = managedListSupportMulticaster;
        this.fileListListenerSupport = new EventMulticasterImpl<FileListChangedEvent>();
        this.fileLoader = ExecutorsHelper.newProcessingQueue("ManagedList Loader");
        this.files = new ArrayList<FileDesc>();
        this.extensions = new ConcurrentSkipListSet<String>();
        this.managedDirectories = new HashSet<File>();
        this.urnMap = new HashMap<URN, IntSet>();
        this.fileToFileDescMap = new HashMap<File, FileDesc>();
        this.urnValidator = urnValidator;
    }
    
    LibraryFileData getLibraryData() {
        if(!fileData.isLoaded()) {
            fileData.load();
        }
        return fileData;
    }
    
    void initialize() {
        fileDescMulticaster.addListener(new EventListener<FileDescChangeEvent>() {
            @Override
            public void handleEvent(FileDescChangeEvent event) {
                switch(event.getType()) {
                case URNS_CHANGED:
                    rwLock.writeLock().lock();
                    try {
                        updateUrnIndex(event.getSource());
                    } finally {
                        rwLock.writeLock().unlock();
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
                }
            }
        });
    }
    
    private FileListChangedEvent dispatchFailure(File file, FileDesc oldFileDesc) {
        FileListChangedEvent event;
        if(oldFileDesc != null) {
            event = new FileListChangedEvent(this, FileListChangedEvent.Type.CHANGE_FAILED, oldFileDesc.getFile(), oldFileDesc, file);
            // First dispatch a CHANGE_FAILED for the new event
            dispatch(event);
            // Then dispatch a REMOVE for the old FD.
            dispatch(new FileListChangedEvent(this, FileListChangedEvent.Type.REMOVED, oldFileDesc));
        } else {
            event = new FileListChangedEvent(this, FileListChangedEvent.Type.ADD_FAILED, file);
            // Just dispatch an ADD_FAIL for the file.
            dispatch(event);
        }
        
        return event;
    }
    
    void dispatch(FileListChangedEvent event) {
        fileListListenerSupport.broadcast(event);
    }
    
    void dispatch(ManagedListStatusEvent event) {
        managedListListenerSupport.broadcast(event);
    }

    @Override
    public FileDesc getFileDesc(File file) {
        file = canonicalize(file);        
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
    
    private File canonicalize(File file) {
        try {
            return FileUtils.getCanonicalFile(file);
        } catch(IOException iox) {
            return file;
        }
    }
    
    @Override
    public boolean contains(File file) {
        rwLock.readLock().lock();
        try {
            return fileToFileDescMap.containsKey(canonicalize(file));
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
    
    @Override
    public Future<List<Future<FileDesc>>> setManagedFolders(
            Collection<File> recursiveFoldersToManage,
            Collection<File> foldersToExclude) {
        getLibraryData().setDirectoriesToManageRecursively(recursiveFoldersToManage);
        getLibraryData().setDirectoriesToExcludeFromManaging(foldersToExclude);
        
        final Set<File> oldManagedDirs = new HashSet<File>();
        rwLock.readLock().lock();
        try {
            oldManagedDirs.addAll(managedDirectories);
        } finally {
            rwLock.readLock().unlock();
        }
        
        final int startRevision = revision.get();
        
        return fileLoader.submit(new Callable<List<Future<FileDesc>>>() {
            @Override
            public List<Future<FileDesc>> call() throws Exception {
                if(startRevision != revision.get()) {
                    return Collections.emptyList();
                }
                
                Set<File> newManagedDirs = calculateManagedDirs();
                
                Set<File> removedDirs = new HashSet<File>(oldManagedDirs);
                removedDirs.removeAll(newManagedDirs);
                
                Set<File> addedDirs = new HashSet<File>(newManagedDirs);
                addedDirs.removeAll(oldManagedDirs);
                
                List<FileDesc> removed = new ArrayList<FileDesc>();
                rwLock.writeLock().lock();
                try {
                    if(startRevision != revision.get()) {
                        return Collections.emptyList();
                    }
                    
                    if(!removedDirs.isEmpty()) {
                        managedDirectories.removeAll(removedDirs);
                        for(FileDesc fd : files) {
                            if(fd != null && removedDirs.contains(fd.getFile().getParentFile()) && hasManageableExtension(fd.getFile())) {
                                removed.add(removeInternal(fd.getFile()));
                            }
                        }
                    }
                    
                    if(!addedDirs.isEmpty()) {
                        managedDirectories.addAll(addedDirs);                        
                    }
                } finally {
                    rwLock.writeLock().unlock();
                }
                
                for(FileDesc fd : removed) {
                    dispatch(new FileListChangedEvent(ManagedFileListImpl.this, FileListChangedEvent.Type.REMOVED, fd));
                }

                List<Future<FileDesc>> added = new ArrayList<Future<FileDesc>>();
                for(File dir : addedDirs) {
                    updateManagedDirectories(extensions, dir, startRevision, false, false, added);
                }
                return added;
            }
        });
    }
    
    /** Calculates all dirs (including subdirs) that should be managed. */
    private Set<File> calculateManagedDirs() {
        List<File> dirs = getLibraryData().getDirectoriesToManageRecursively();
        Set<File> allManagedDirs = new HashSet<File>();
        for(File dir : dirs) {
            calculateManagedDirsImpl(dir, allManagedDirs);
        }        
        return allManagedDirs;
    }
    
    /** Recursively calculates all dirs (including subdirs) that should be managed. */
    private void calculateManagedDirsImpl(File dir, Set<File> files) {
        dir = canonicalize(dir);
        
        if(files.contains(dir)) {
            return;
        }
        
        files.add(dir);
        
        File[] dirList = dir.listFiles(new ManagedDirectoryFilter());
        for(File subdir : dirList) {
            calculateManagedDirsImpl(subdir, files);
        }
    }
    
    @Override
    public Future<List<Future<FileDesc>>> addFolder(File f) {
        final File folder = canonicalize(f);        
        rwLock.readLock().lock();
        try {
            if(managedDirectories.contains(folder)) {
                List<Future<FileDesc>> list = Collections.emptyList();
                return new SimpleFuture<List<Future<FileDesc>>>(list);
            }
        } finally {
            rwLock.readLock().unlock();
        }
        
        getLibraryData().addDirectoryToManageRecursively(folder);
        return fileLoader.submit(new Callable<List<Future<FileDesc>>>() {
            @Override
            public List<Future<FileDesc>> call() {
                List<Future<FileDesc>> futures = new ArrayList<Future<FileDesc>>();
                updateManagedDirectories(extensions, folder, revision.get(), true, true, futures);
                return futures;
            }
        });
    }
    
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
            dispatch(new FileListChangedEvent(ManagedFileListImpl.this, FileListChangedEvent.Type.ADDED, fd));
        }
    }

    @Override
    public void addFileListListener(EventListener<FileListChangedEvent> listener) {
        fileListListenerSupport.addListener(listener);
    }
    
    @Override
    public void removeFileListListener(EventListener<FileListChangedEvent> listener) {
        fileListListenerSupport.removeListener(listener);
    }
    
    @Override
    public void addManagedListStatusListener(EventListener<ManagedListStatusEvent> listener) {
        managedListListenerSupport.addListener(listener);
    }
    
    @Override
    public void removeManagedListStatusListener(EventListener<ManagedListStatusEvent> listener) {
        managedListListenerSupport.removeListener(listener);
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("cannot clear managed list");
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
                return CollectionUtils.listOf(new FileListIterator(this, urnsMatching));
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
                    return new ThreadSafeManagedListIterator();
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
    public Collection<String> getManagedExtensions() {
        return fileData.getManagedExtensions();
    }
    
    @Override
    public Future<List<Future<FileDesc>>> setManagedExtensions(Collection<String> newManagedExtensions) {
        // Go through and collect a list of removed FDs.
        List<FileDesc> removed = new ArrayList<FileDesc>();
        final Collection<String> newExtensions = new HashSet<String>(newManagedExtensions);
        final int rev;
        
        rwLock.writeLock().lock();
        try {
            rev = revision.get();
            newExtensions.removeAll(extensions);
            Collection<String> removedExtensions = new HashSet<String>(extensions);
            removedExtensions.removeAll(newManagedExtensions);
            
            if(!removedExtensions.isEmpty()) {
                for(FileDesc fd : files) {
                    if(fd != null) {
                        File parent = fd.getFile().getParentFile();
                        String ext = FileUtils.getFileExtension(fd.getFile()).toLowerCase(Locale.US);
                        
                        if(managedDirectories.contains(parent) && removedExtensions.contains(ext)) {
                            removed.add(removeInternal(fd.getFile()));
                        }
                    }
                }
            }

            getLibraryData().setManagedExtensions(newManagedExtensions);
            extensions.clear();
            extensions.addAll(getLibraryData().getManagedExtensions());
        } finally {
            rwLock.writeLock().unlock();
        }
        
        // Dispatch all removed FDs.
        for(FileDesc fd : removed) {
            dispatch(new FileListChangedEvent(this, FileListChangedEvent.Type.REMOVED, fd));
        }

        // Go through all managed directories & add any files for new extensions.
        Future<List<Future<FileDesc>>> future;
        if(!newExtensions.isEmpty()) {
            future = fileLoader.submit(new Callable<List<Future<FileDesc>>>() {
                @Override
                public List<Future<FileDesc>> call() {
                    List<Future<FileDesc>> futures = new ArrayList<Future<FileDesc>>();
                    Set<File> directoryCopy;
                    rwLock.readLock().lock();
                    try {
                        directoryCopy = new HashSet<File>(managedDirectories);
                    } finally {
                        rwLock.readLock().unlock();
                    }
                    
                    for(File directory : directoryCopy) {
                        updateManagedDirectories(newExtensions, directory, rev, false, false, futures);
                    }
                    return futures;
                }
            });
        } else {
            List<Future<FileDesc>> futures = Collections.emptyList();
            future = new SimpleFuture<List<Future<FileDesc>>>(futures);
        }            
        
        return future;
    }

    @Override
    public List<File> getDirectoriesToManageRecursively() {
        return fileData.getDirectoriesToManageRecursively();
    }
    
    @Override
    public Future<FileDesc> add(File file) {
        return add(file, LimeXMLDocument.EMPTY_LIST);
    }    
    
    @Override
    public Future<FileDesc> add(File file, List<? extends LimeXMLDocument> list) {
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
    private Future<FileDesc> add(File file, List<? extends LimeXMLDocument> metadata, int rev,
            FileDesc oldFileDesc) {
        LOG.debugf("Attempting to load file: {0}", file);
        
        // Make sure capitals are resolved properly, etc.
        try {
            file = FileUtils.getCanonicalFile(file);
        } catch (IOException e) {
            LOG.debugf("Not adding {0} because canonicalize failed", file);
            FileListChangedEvent event = dispatchFailure(file, oldFileDesc);
            return new SimpleFuture<FileDesc>(new FileListChangeFailedException(event, "Can't canonicalize file"));
        }
        
        boolean explicitAdd = false;
        rwLock.readLock().lock();
        try {
            explicitAdd = !managedDirectories.contains(file.getParentFile())
                       || !hasManageableExtension(file);
            
            // Exit if already added.
            if(fileToFileDescMap.containsKey(file)) {
                LOG.debugf("Not loading because file already loaded {0}", file);
                return new SimpleFuture<FileDesc>(fileToFileDescMap.get(file));
            }
        } finally {
            rwLock.readLock().unlock();
        }
        
        //make sure a FileDesc can be created from this file
        if (!LibraryUtils.isFilePhysicallyManagable(file)) {
            LOG.debugf("Not adding {0} because file isn't physically manageable", file);
            FileListChangedEvent event = dispatchFailure(file, oldFileDesc);
            return new SimpleFuture<FileDesc>(new FileListChangeFailedException(event, "File isn't physically manageable"));
        }

        getLibraryData().addManagedFile(file, explicitAdd);

        boolean failed = false;
        rwLock.writeLock().lock();
        try {
            if (rev != revision.get()) {
                failed = true;
            } else {
                numPendingFiles++;
                // make sure _pendingFinished does not hold _revision
                // while we're still adding files
                pendingFinished = -1;
            }
        } finally {
            rwLock.writeLock().unlock();
        }
        
        if(failed) {
            LOG.debugf("Not adding {0} because revisions changed while loading", file);
            FileListChangedEvent event = dispatchFailure(file, oldFileDesc);
            return new SimpleFuture<FileDesc>(new FileListChangeFailedException(event, "Revisions changed while loading"));
        } else {
            PendingFuture task = new PendingFuture();
            urnCache.calculateAndCacheUrns(file, getNewUrnCallback(file, metadata, rev, oldFileDesc, task));
            return task;
        }
    }
    
    /**
     * Constructs a new UrnCallback that will possibly load the file with the given URNs.
     * @param task 
     */
    private UrnCallback getNewUrnCallback(final File file, final List<? extends LimeXMLDocument> metadata, final int rev, 
                                final FileDesc oldFileDesc, final PendingFuture task) {
        return new UrnCallback() {
            @Override
            public void urnsCalculated(File f, Set<? extends URN> urns) {
                finishLoadingFileDesc(f, urns, metadata, rev, oldFileDesc, task);
            }

            @Override
            public boolean isOwner(Object o) {
                return o == ManagedFileListImpl.this;
            }
        };
    }
    
    private void finishLoadingFileDesc(File file, Set<? extends URN> urns,
            List<? extends LimeXMLDocument> metadata, int rev, FileDesc oldFileDesc,
            PendingFuture task) {
        FileDesc fd = null;
        boolean revchange = false;
        boolean failed = false;
        rwLock.writeLock().lock();
        try {
            if(rev != revision.get()) {
                revchange = true;
                LOG.warn("Revisions changed, dropping share.");
            } else {
                numPendingFiles--;
                
                // Only load the file if we were able to calculate URNs 
                // assume the fd is being shared
                if(!urns.isEmpty()) {
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
            FileListChangedEvent event = dispatchFailure(file, oldFileDesc);
            task.setException(new FileListChangeFailedException(event, "Revisions changed while loading FD."));          
        } else if(fd == null) {
            FileListChangedEvent event = dispatchFailure(file, oldFileDesc);
            task.setException(new FileListChangeFailedException(event, "Couldn't create FD"));
        } else if(failed) {
            LOG.debugf("Couldn't load FD because FD with file {0} exists already.  FD: {1}", file, fd);
            dispatchFailure(file, oldFileDesc);
            task.set(fd); // pseudo-success because we already have a FD by that name
        } else { // SUCCESS!
            // try loading the XML for this fileDesc
            fileDescMulticaster.broadcast(new FileDescChangeEvent(fd, FileDescChangeEvent.Type.LOAD, metadata));
            
            // It is very important that the events get dispatched
            // prior to setting the value on the task, so that other FileLists
            // listening to these events can receive & process the event
            // prior to the future.get() returning.
            if(oldFileDesc == null) {
                LOG.debugf("Added file: {0}", file);
                dispatch(new FileListChangedEvent(this, FileListChangedEvent.Type.ADDED, fd));
            } else {
                LOG.debugf("Changed to new file: {0}", file);
                dispatch(new FileListChangedEvent(this, FileListChangedEvent.Type.CHANGED, oldFileDesc, fd));
            }
            
            task.set(fd);
        }
            
        // In all cases except revision change, try to finish this revision.
        if(!revchange) {
            boolean finished = false;
            rwLock.writeLock().lock();
            try {
                if(numPendingFiles == 0) {
                    pendingFinished = rev;
                    finished = true;
                }
            } finally {
                rwLock.writeLock().unlock();
            }
            
            if (finished) {
                tryToFinish();
            }
        }
    }
    
    /** Notification that something finished loading. */
    private void tryToFinish() {
        int rev;
        rwLock.writeLock().lock();
        try {
            if(pendingFinished != updatingFinished || // Pending's revision must == update
               pendingFinished != revision.get() ||       // The revision must be the current library's
               loadingFinished >= revision.get())         // And we can't have already finished.
                return;
            loadingFinished = revision.get();
            rev = loadingFinished;
        } finally {
            rwLock.writeLock().unlock();
        }
        
        loadFinished(rev);
    }
  
    /**
     * Creates a file descriptor for a given file and a set of urns
     * @param file - file to create descriptor for
     * @param urns - urns to use
     * @param index - index to use
     * @return
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

        file = canonicalize(file);
        FileDesc fd = removeInternal(file);        
        if(fd != null) {
            dispatch(new FileListChangedEvent(this, FileListChangedEvent.Type.REMOVED, fd));
        }
        return fd != null;
    }
    
    private FileDesc removeInternal(File file) {
        FileDesc fd;
        boolean exclude;
        rwLock.writeLock().lock();
        try {
            exclude = managedDirectories.contains(file.getParentFile()) && hasManageableExtension(file);
            fd = fileToFileDescMap.get(file);
            if(fd != null) {
                removeFileDesc(file, fd);
            }
        } finally {
            rwLock.writeLock().unlock();
        }
        if(fd != null) {
            getLibraryData().removeManagedFile(file, exclude);
        }
        return fd;
    }
    
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
    public Future<FileDesc> fileRenamed(File oldName, final File newName) {
        LOG.debugf("Attempting to rename: {0} to: {1}", oldName, newName);      
        
        oldName = canonicalize(oldName);
        FileDesc fd = removeInternal(oldName);        
        if (fd != null) {
            // TODO: It's dangerous to prepopulate, because we might actually
            //       be called with wrong data, giving us wrong URNs.
            // Prepopulate the cache with new URNs.
            urnCache.addUrns(newName, fd.getUrns());
            List<LimeXMLDocument> xmlDocs = new ArrayList<LimeXMLDocument>(fd.getLimeXMLDocuments());
            return add(newName, xmlDocs, revision.get(), fd);
        } else {
            return new SimpleFuture<FileDesc>(new FileListChangeFailedException(new FileListChangedEvent(this, FileListChangedEvent.Type.CHANGE_FAILED, oldName, null, newName), "Old file wasn't managed"));
        }
    }
    
    @Override
    public Future<FileDesc> fileChanged(File file, List<? extends LimeXMLDocument> xmlDocs) {
        LOG.debugf("File Changed: {0}", file);

        file = canonicalize(file);
        FileDesc fd = removeInternal(file);
        if (fd != null) {
            urnCache.removeUrns(file); // Explicitly remove URNs to force recalculating.
            return add(file, xmlDocs, revision.get(), fd);
        } else {
            return new SimpleFuture<FileDesc>(new FileListChangeFailedException(new FileListChangedEvent(this, FileListChangedEvent.Type.CHANGE_FAILED, file, null, file), "Old file wasn't managed"));
        }
    }
    
    /**
     * Loads all files from prior sessions.
     * This returns immediately with a Future that contains
     * the list of all Future FileDescs that will be added.
     */  
    Future<List<Future<FileDesc>>> loadManagedFiles() {
        final int currentRevision = revision.incrementAndGet();
        LOG.debugf("Starting new library revision: {0}", currentRevision);
        
        Future<List<Future<FileDesc>>> future = fileLoader.submit(new Callable<List<Future<FileDesc>>>() {
            @Override
            public List<Future<FileDesc>> call() {
                dispatch(new ManagedListStatusEvent(ManagedFileListImpl.this, ManagedListStatusEvent.Type.LOAD_STARTED));
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
    private List<Future<FileDesc>> loadSettingsInternal(int revision) {
        LOG.debugf("Loading Library Revision: {0}", revision);
        
        rwLock.writeLock().lock();
        try {
            files.clear();
            urnMap.clear();
            fileToFileDescMap.clear();
            managedDirectories.clear();
            numPendingFiles = 0;
            extensions.clear();
            extensions.addAll(getLibraryData().getManagedExtensions());
        } finally {
            rwLock.writeLock().unlock();
        }
        
        dispatch(new FileListChangedEvent(ManagedFileListImpl.this, FileListChangedEvent.Type.CLEAR));
        
        List<Future<FileDesc>> futures = loadManagedFiles(revision);

        LOG.debugf("Finished queueing files for revision: {0}", revision);
            
        rwLock.writeLock().lock();
        try {
            updatingFinished = revision;
            if(numPendingFiles == 0) // if we didn't even try adding any files, pending is finished also.
                pendingFinished = revision;
        } finally {
            rwLock.writeLock().unlock();
        }
        tryToFinish();
        return futures;
    }
    
    /** Kicks off necessary stuff for loading being done. */
    private void loadFinished(int rev) {
        LOG.debugf("Finished loading revision: {0}", rev);
        dispatch(new ManagedListStatusEvent(this, ManagedListStatusEvent.Type.LOAD_FINISHING));
        save();
        dispatch(new ManagedListStatusEvent(this, ManagedListStatusEvent.Type.LOAD_COMPLETE));
    }
    
    @Override
    public boolean isLoadFinished() {
        return loadingFinished == revision.get();
    }

    private List<Future<FileDesc>> loadManagedFiles(int rev) {
        List<Future<FileDesc>> futures = new ArrayList<Future<FileDesc>>();
        
        // TODO: We want to always share this stuff, not just approved extensions.
        updateManagedDirectories(extensions, LibraryUtils.PROGRAM_SHARE, rev, false, true, futures);
        updateManagedDirectories(extensions, LibraryUtils.PREFERENCE_SHARE, rev, false, true, futures);

        List<File> directories = getLibraryData().getDirectoriesToManageRecursively();
        // Sorting is not terribly necessary, but we'll do it anyway...
        Collections.sort(directories, new Comparator<File>() {
            public int compare(File a, File b) {
                return a.toString().length()-b.toString().length();
            }
        });
        
        for(File directory : directories) {
            if(rev != revision.get()) {
                break;
            }
            updateManagedDirectories(extensions, directory, rev, true, true, futures);        
        }
        
        for(File file : getLibraryData().getManagedFiles()) {
            if(rev != revision.get()) {
                break;
            }
            futures.add(add(file, LimeXMLDocument.EMPTY_LIST, rev, null));
        }
        
        return futures;
    }
    
    /**
     * Recursively adds this directory and all subdirectories to the managed
     * directories as well as queueing their files for managing.  Does nothing
     * if <tt>directory</tt> doesn't exist, isn't a directory, or has already
     * been added.  This method is thread-safe.  It acquires locks on a
     * per-directory basis.  If the current revision ever changes from the
     * expected revision, this returns immediately.
     */
    private void updateManagedDirectories(Collection<String> managedExts, File directory, int rev,
            boolean recurse, boolean validateDir,
            List<Future<FileDesc>> futures) {
        LOG.debugf("Adding [{0}] to managed directories", directory);
         
         directory = canonicalize(directory);
         if(!isFolderManageable(directory, true)) {
             LOG.debugf("Exiting because dir isn't manageable {0}", directory);
             return;
         }
     
         // Exit quickly (without doing the dir lookup) if revisions changed.
         if(rev != revision.get()) {
             LOG.debugf("Exiting because revisions changed.  Expected {0}, was {1}", rev, revision.get());
             return;
         }
    
         // STEP 1:
         // Add directory
         if(validateDir) {
             rwLock.readLock().lock();
             try {
                 // if it was already added, ignore.
                 if (managedDirectories.contains(directory)) {
                     LOG.debugf("Exiting because dir already managed {0}", directory);
                     return;
                 } else {
                     managedDirectories.add(directory);
                 }
             } finally {
                 rwLock.readLock().unlock();
             }
         }
     
         // STEP 2:
         // Scan subdirectory for the amount of shared files.
         File[] fileList = directory.listFiles(new ManageableFileFilter(managedExts));
         if (fileList == null) {
             LOG.debugf("Exiting because no files in directory {0}", directory);
             return;
         }
         
         for(int i = 0; i < fileList.length && rev == revision.get(); i++) {
             futures.add(add(fileList[i], LimeXMLDocument.EMPTY_LIST, rev, null));
         }
             
         // Exit quickly (without doing the dir lookup) if revisions changed.
         if(rev != revision.get()) {
             LOG.debugf("Exiting because revisions changed.  Expected {0}, was {1}", rev, revision.get());
             return;
         }

         // Explicitly unset recurse for forced share dirs.
         if(LibraryUtils.isForcedShareDirectory(directory)) {
             recurse = false;
         }
    
         // STEP 3:
         // Recursively add subdirectories.
         if(recurse) {
             File[] dirList = directory.listFiles(new ManagedDirectoryFilter());
             if(dirList != null) {
                 for(int i = 0; i < dirList.length && rev == revision.get(); i++) {
                     updateManagedDirectories(managedExts, dirList[i], rev, recurse, validateDir, futures);
                 }
            }
         } else {
             LOG.debugf("Not recursing beyond dir {0}", directory);
         }
    }

    void save() {
        dispatch(new ManagedListStatusEvent(this, ManagedListStatusEvent.Type.SAVE));
        getLibraryData().save();
    }
    
    int revision() {
        return revision.get();
    }

    private boolean isFolderManageable(File folder, boolean excludeExcludedDirectories) {
        if (!folder.isDirectory() || !folder.canRead() || !folder.exists()) {
            return false;
        }

        if (getLibraryData().isIncompleteDirectory(folder)) {
            return false;
        }

        if (LibraryUtils.isApplicationSpecialShareDirectory(folder)) {
            return false;
        }

        if (excludeExcludedDirectories && getLibraryData().isFolderExcluded(folder)) {
            return false;
        }

        if (LibraryUtils.isFolderBanned(folder)) {
            return false;
        }

        return true;
    }
    
    private class ThreadSafeManagedListIterator implements Iterator<FileDesc> {        
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
        String extension = FileUtils.getFileExtension(file);
        return extensions.contains(extension.toLowerCase(Locale.US));
    }
    
    private class ManageableFileFilter implements FileFilter {
        private final Set<String> extensions;
        
        public ManageableFileFilter(Collection<String> extensions) {
            this.extensions = new HashSet<String>(extensions);
        }
        
        @Override
        public boolean accept(File file) {
            return file.isFile()
                && LibraryUtils.isFilePhysicallyManagable(file)
                && extensions.contains(FileUtils.getFileExtension(file).toLowerCase(Locale.US))
                && !getLibraryData().isFileExcluded(file);
        }
    }
    
    /** The filter object to use to determine directories. */
    private class ManagedDirectoryFilter implements FileFilter {
        @Override
        public boolean accept(File f) {
            return f.isDirectory()
                && isFolderManageable(f, true);
        }        
    }

    private final static Callable<FileDesc> EMPTY_CALLABLE = new Callable<FileDesc>() {
        @Override
        public FileDesc call() { return null; }
    };
    
    /** A future that delegates on another future, occasionally. */
    private class PendingFuture extends FutureTask<FileDesc> {     
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
}
