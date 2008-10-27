package com.limegroup.gnutella.library;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.limewire.collection.IntSet;
import org.limewire.concurrent.ExecutorsHelper;
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
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnSet;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.auth.ContentResponseData;
import com.limegroup.gnutella.auth.ContentResponseObserver;
import com.limegroup.gnutella.downloader.VerifyingFile;
import com.limegroup.gnutella.xml.LimeXMLDocument;

class ManagedFileListImpl implements ManagedFileList, FileList {
    
    private static final Log LOG = LogFactory.getLog(ManagedFileListImpl.class);
    
    private final SourcedEventMulticaster<FileDescChangeEvent, FileDesc> fileDescMulticaster;
    private final EventMulticaster<ManagedListStatusEvent> managedListListenerSupport;
    private final EventMulticaster<FileListChangedEvent> fileListListenerSupport;
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final UrnCache urnCache;
    private final FileDescFactory fileDescFactory;
    private final ContentManager contentManager;    
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
    
    /**
     * A Set of URNs that we're currently requesting validation for.
     * This is NOT cleared on new revisions, because it'll always be
     * valid.
     */
    private Set<URN> requestingValidation = Collections.synchronizedSet(new HashSet<URN>());
    
    /** All the library data for this library -- loaded on-demand. */
    private final LibraryFileData fileData = new LibraryFileData();    

    @Inject
    ManagedFileListImpl(SourcedEventMulticaster<FileDescChangeEvent, FileDesc> fileDescMulticaster,
                        UrnCache urnCache,
                        FileDescFactory fileDescFactory,
                        ContentManager contentManager,
                        EventMulticaster<ManagedListStatusEvent> managedListSupportMulticaster) {
        this.urnCache = urnCache;
        this.fileDescFactory = fileDescFactory;
        this.contentManager = contentManager;
        this.fileDescMulticaster = fileDescMulticaster;
        this.managedListListenerSupport = managedListSupportMulticaster;
        this.fileListListenerSupport = new EventMulticasterImpl<FileListChangedEvent>();
        this.fileLoader = ExecutorsHelper.newProcessingQueue("ManagedList Loader");
        this.files = new ArrayList<FileDesc>();
        this.extensions = new HashSet<String>();
        this.managedDirectories = new HashSet<File>();
        this.urnMap = new HashMap<URN, IntSet>();
        this.fileToFileDescMap = new HashMap<File, FileDesc>();
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
    }
    
    void dispatch(FileListChangedEvent event) {
        fileListListenerSupport.broadcast(event);
    }
    
    void dispatch(ManagedListStatusEvent event) {
        managedListListenerSupport.broadcast(event);
    }

    public FileDesc getFileDesc(File file) {
        file = canonicalize(file);        
        rwLock.readLock().lock();
        try {
            return fileToFileDescMap.get(file);
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    public FileDesc getFileDesc(final URN urn) {
        if (!urn.isSHA1()) {
            throw new IllegalArgumentException();
        }
        
        rwLock.readLock().lock();
        try {
            IntSet indices = urnMap.get(urn);
            if (indices == null) {
                return null;
            }
            IntSet.IntSetIterator iter = indices.iterator();
          
            //Pick the first non-null non-Incomplete FileDesc.
            FileDesc ret = null;
            while ( iter.hasNext() 
                       && ( ret == null || ret instanceof IncompleteFileDesc) ) {
                int index = iter.next();
                ret = files.get(index);
            }
            return ret;
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
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
    public void addFolder(File folder) {
        throw new UnsupportedOperationException("TODO: Implement Managed Folders");
    }
    
    public void addIncompleteFile(File incompleteFile,
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
            } else {
                List<FileDesc> fds = new ArrayList<FileDesc>(urnsMatching.size());
                Iterator<FileDesc> fdIter = new FileListIterator(this, urnsMatching);
                while(fdIter.hasNext()) {
                    fds.add(fdIter.next());
                }
                return fds;
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public List<FileDesc> getFilesInDirectory(File directory) {
        throw new UnsupportedOperationException("cannot iterate on files in directory");
    }

    @Override
    public Lock getReadLock() {
        return rwLock.readLock();
    }
    
    @Override
    public Iterable<FileDesc> iterable() {
        return fileToFileDescMap.values();
    }
    
    @Override
    public Iterable<FileDesc> threadSafeIterable() {
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
    
    public void validate(final FileDesc fd) {
        if(requestingValidation.add(fd.getSHA1Urn())) {
            contentManager.request(fd.getSHA1Urn(), new ContentResponseObserver() {
               public void handleResponse(URN urn, ContentResponseData r) {
                   requestingValidation.remove(fd.getSHA1Urn());
                   if(r != null && !r.isOK()) {
                       remove(fd.getFile());
                   }
               }
            }, 5000);
        }
    }    

    ///////////////////////////////////////////////////////////////
    
    public void add(File file) {
        add(file, LimeXMLDocument.EMPTY_LIST);
    }    
    
    public void add(File file, List<? extends LimeXMLDocument> list) {
        add(file, list, revision.get(), null);
    }
    
    /**
     * Adds a file that is either associated with the store or is shared. Tries to create 
     * a FileDescriptor for the file and ew. Files are handled differently depending on their AddType. 
     * 
     * 
     * @param file - the file to be added
     * @param metadata - any LimeXMLDocs associated with this file
     * @param rev - current  version of LimeXMLDocs being used
     * @param oldFileDesc the old FileDesc this is replacing
     */
    private void add(File file, List<? extends LimeXMLDocument> metadata, int rev,
            FileDesc oldFileDesc) {
        LOG.debugf("Attempting to load store or shared file: {0}", file);

        // Make sure capitals are resolved properly, etc.
        try {
            file = FileUtils.getCanonicalFile(file);
        } catch (IOException e) {
            dispatch(new FileListChangedEvent(this, FileListChangedEvent.Type.ADD_FAILED, file));
            return;
        }
        
        boolean explicitAdd = false;
        rwLock.readLock().lock();
        try {
            explicitAdd = !managedDirectories.contains(file.getParentFile())
                       || !hasManageableExtension(file);
            
            // Exit if already added.
            if(fileToFileDescMap.containsKey(file)) {
                return;
            }
        } finally {
            rwLock.readLock().unlock();
        }
        
        //make sure a FileDesc can be created from this file
        if (!LibraryUtils.isFilePhysicallyManagable(file)) {
            dispatch(new FileListChangedEvent(this, FileListChangedEvent.Type.ADD_FAILED, file));
            return;
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
            dispatch(new FileListChangedEvent(this, FileListChangedEvent.Type.ADD_FAILED, file));
        } else {
            urnCache.calculateAndCacheUrns(file, getNewUrnCallback(file, metadata, rev, oldFileDesc));
        }
    }
    
    /**
     * Constructs a new UrnCallback that will possibly load the file with the given URNs.
     */
    private UrnCallback getNewUrnCallback(final File file, final List<? extends LimeXMLDocument> metadata, final int rev, 
                                final FileDesc oldFileDesc) {
        return new UrnCallback() {
            public void urnsCalculated(File f, Set<? extends URN> urns) {
                finishLoadingFileDesc(f, urns, metadata, rev, oldFileDesc);
            }

            
            public boolean isOwner(Object o) {
                return o == ManagedFileListImpl.this;
            }
        };
    }
    
    private void finishLoadingFileDesc(File file, Set<? extends URN> urns, List<? extends LimeXMLDocument> metadata, int rev, FileDesc oldFileDesc) {
            FileDesc fd = null;
            rwLock.writeLock().lock();
            try {
                if(rev != revision.get()) {
                    LOG.warn("Revisions changed, dropping share.");
                } else {
                    numPendingFiles--;
                    
                    // Only load the file if we were able to calculate URNs 
                    // assume the fd is being shared
                    if(!urns.isEmpty()) {
                        fd = createFileDesc(file, urns, files.size());
                    }
                }
            } finally {
                rwLock.writeLock().unlock();
            }
            
            if(fd == null) {
                dispatch(new FileListChangedEvent(this, FileListChangedEvent.Type.ADD_FAILED, file));
                return;
            }
                
            // try loading the XML for this fileDesc
            fileDescMulticaster.broadcast(new FileDescChangeEvent(fd, FileDescChangeEvent.Type.LOAD, metadata));
            
            boolean failed = false;
            rwLock.writeLock().lock();
            try {
                if(contains(file)) {
                    failed = true;
                } else {
                    files.add(fd);
                    fileToFileDescMap.put(file, fd);
                    updateUrnIndex(fd);
                }
            } finally {
                rwLock.writeLock().unlock();
            }
            
            if(failed) {
                dispatch(new FileListChangedEvent(this, FileListChangedEvent.Type.ADD_FAILED, file));
            } else if(oldFileDesc == null) {
                dispatch(new FileListChangedEvent(this, FileListChangedEvent.Type.ADDED, fd));
            } else {
                dispatch(new FileListChangedEvent(this, FileListChangedEvent.Type.CHANGED, oldFileDesc, fd));
            }
            
            boolean finished = false;
            rwLock.writeLock().lock();
            try {
                if(numPendingFiles == 0) {
                    pendingFinished = rev;
                    finished = true;
                }
            } finally {
                rwLock.writeLock().unlock();
            
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
        FileDesc fileDesc = fileDescFactory.createFileDesc(file, urns, index);
        ContentResponseData r = contentManager.getResponse(fileDesc.getSHA1Urn());
        // if we had a response & it wasn't good, don't add this FD.
        if(r != null && !r.isOK())
            return null;
        else
            return fileDesc;
    }

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
    
    public void fileRenamed(File oldName, final File newName) {
        LOG.debugf("Attempting to rename: {0} to: {1}", oldName, newName);      
        
        oldName = canonicalize(oldName);
        FileDesc fd = removeInternal(oldName);        
        if (fd != null) {                
            // Prepopulate the cache with new URNs.
            urnCache.addUrns(newName, fd.getUrns());
            List<LimeXMLDocument> xmlDocs = new ArrayList<LimeXMLDocument>(fd.getLimeXMLDocuments());
            add(newName, xmlDocs, revision.get(), fd);
        }
    }
    
    public void fileChanged(File file, List<? extends LimeXMLDocument> xmlDocs) {
        LOG.debugf("File Changed: {0}", file);

        file = canonicalize(file);
        FileDesc fd = removeInternal(file);        
        if (fd != null) {
            add(file, xmlDocs, revision.get(), fd);
        }
    }
    
    void loadSettings() {
        final int currentRevision = revision.incrementAndGet();
        LOG.debugf("Starting new library revision: {0}", currentRevision);
        
        fileLoader.execute(new Runnable() {
            public void run() {
                dispatch(new ManagedListStatusEvent(ManagedFileListImpl.this, ManagedListStatusEvent.Type.LOAD_STARTED));
                loadSettingsInternal(currentRevision);
            }
        });
    }

    /** 
     * Loads all shared files, putting them in a queue for being added.
     *
     * If the current revision ever changed from the expected revision, this returns
     * immediately.
     */
    private void loadSettingsInternal(int revision) {
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
        
        loadManagedFiles(revision);

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
    }
    
    /** Kicks off necessary stuff for loading being done. */
    private void loadFinished(int rev) {
        LOG.debugf("Finished loading revision: {0}", rev);
        dispatch(new ManagedListStatusEvent(this, ManagedListStatusEvent.Type.LOAD_FINISHING));
        save();
        dispatch(new ManagedListStatusEvent(this, ManagedListStatusEvent.Type.LOAD_COMPLETE));
    }
    
    public boolean isLoadFinished() {
        return loadingFinished == revision.get();
    }

    private void loadManagedFiles(int rev) {
        updateManagedDirectories(LibraryUtils.PROGRAM_SHARE, null, rev);
        updateManagedDirectories(LibraryUtils.PREFERENCE_SHARE, null, rev);

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
            updateManagedDirectories(directory, null, rev);        
        }
        
        for(File file : getLibraryData().getManagedFiles()) {
            if(rev != revision.get()) {
                break;
            }
            add(file);
        }
    }
    
    /**
     * Recursively adds this directory and all subdirectories to the managed
     * directories as well as queueing their files for managing.  Does nothing
     * if <tt>directory</tt> doesn't exist, isn't a directory, or has already
     * been added.  This method is thread-safe.  It acquires locks on a
     * per-directory basis.  If the current revision ever changes from the
     * expected revision, this returns immediately.
     * 
     * @requires directory is part of DIRECTORIES_TO_SHARE or one of its
     *           children, and parent is directory's shared parent or null if
     *           directory's parent is not shared.
     */
     private void updateManagedDirectories(File rootShare, File directory, File parent, int rev, int depth) {
         directory = canonicalize(directory);
         if(!directory.exists()) {
             return;
         }
     
         // Exit quickly (without doing the dir lookup) if revisions changed.
         if(rev != revision.get()) {
             return;
         }
    
         // STEP 1:
         // Add directory
         
         rwLock.readLock().lock();
         try {
             // if it was already added, ignore.
             if (managedDirectories.contains(directory)) {
                 return;
             } else {
                 managedDirectories.add(directory);
             }
         } finally {
             rwLock.readLock().unlock();
         }
     
         // STEP 2:
         // Scan subdirectory for the amount of shared files.
         File[] fileList = directory.listFiles(new ManageableFileFilter());
         if (fileList == null) {
             return;
         }
         
         for(int i = 0; i < fileList.length && rev == revision.get(); i++) {
             add(fileList[i]);
         }
             
         // Exit quickly (without doing the dir lookup) if revisions changed.
         if(rev != revision.get()) {
             return;
         }
    
         // STEP 3:
         // Recursively add subdirectories.
         // This has the effect of ensuring that the number of pending files
         // is closer to correct number.
         // TODO: when we add non-recursive support, add it here.
         if (LibraryUtils.isForcedShareDirectory(directory)) { 
             return;
         }
         
         // Do not share subdirectories of the forcibly shared dir.
         File[] dirList = directory.listFiles(new ManagedDirectoryFilter());
         if(dirList != null) {
             for(int i = 0; i < dirList.length && rev == revision.get(); i++) {
                 updateManagedDirectories(rootShare, dirList[i], directory, rev, depth+1);
             }
        }
    }

    void save() {
        dispatch(new ManagedListStatusEvent(this, ManagedListStatusEvent.Type.SAVE));
        getLibraryData().save();
    }

    private boolean isFolderManageable(File folder, boolean excludeExcludedDirectories) {
        if (!folder.isDirectory() || !folder.canRead()) {
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

    private void updateManagedDirectories(File directory, File parent, int revision) {
        updateManagedDirectories(directory, directory, parent, revision, 1);
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
                while (index <= files.size()) {
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
        @Override
        public boolean accept(File file) {
            return file.isFile()
                && LibraryUtils.isFilePhysicallyManagable(file)
                && hasManageableExtension(file)
                && !getLibraryData().isFileExcluded(file);
        }
    }
    
    /** The filter object to use to determine directories. */
    private class ManagedDirectoryFilter implements FileFilter {
        public boolean accept(File f) {
            return f.isDirectory()
                && isFolderManageable(f, true);
        }        
    };
}
