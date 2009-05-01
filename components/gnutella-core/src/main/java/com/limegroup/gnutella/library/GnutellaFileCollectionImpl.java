package com.limegroup.gnutella.library;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.limewire.inspection.InspectionHistogram;
import org.limewire.listener.SourcedEventMulticasterFactory;
import org.limewire.util.FileUtils;
import org.limewire.util.MediaType;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.tigertree.HashTreeCache;
import com.limegroup.gnutella.xml.LimeXMLDocument;

@Singleton
class GnutellaFileCollectionImpl extends SharedFileCollectionImpl implements GnutellaFileCollection, GnutellaFileView {
    
    /** Size of all the FileDescs in this list in bytes */
    private final AtomicLong numBytes;
    
    /** A list of session-shared data -- it isn't saved. */
    private Map<File, File> sessionFiles = new ConcurrentHashMap<File, File>();
    
    /** A list of application shared files. */
    private final AtomicInteger applicationShared = new AtomicInteger();
    
    /** Number of files removed from this list **/
    private final AtomicInteger numRemoved = new AtomicInteger();
    
    private final InspectionHistogram<String> addedFilesByType = new InspectionHistogram<String>();
    
    @Inject
    public GnutellaFileCollectionImpl(LibraryFileData data, LibraryImpl managedList, 
                                  @AllFileCollections SourcedEventMulticasterFactory<FileViewChangeEvent, FileView> multicasterFactory,
                                  HashTreeCache treeCache) {
        super(data, managedList, multicasterFactory, LibraryFileData.GNUTELLA_COLLECTION_ID, treeCache);
        this.numBytes = new AtomicLong();
    }
    
    @Override
    public boolean add(FileDesc fileDesc) {
        return super.add(fileDesc);
    }

    /**
     * Stubbed out because gnutella list is populated by file added
     * events dispatched upon lw startup.  There are no pending
     * managed files.
     */
    @Override
    protected void addPendingManagedFiles() {}
    
    @Override
    public void clear() {
        applicationShared.set(0);
        sessionFiles.clear();
        super.clear();
    }

    @Override
    public long getNumBytes() {
        return numBytes.get();
    }    
    
    @Override
    public Future<FileDesc> addForSession(File file) {
        sessionFiles.put(file, file);
        return super.add(file);
    }
    
    @Override
    protected boolean addFileDescImpl(FileDesc fileDesc) {
        if(super.addFileDescImpl(fileDesc)) {
            numBytes.addAndGet(fileDesc.getFileSize());
            // inspection code
            LimeXMLDocument doc = fileDesc.getXMLDocument();
            if (doc != null) {
                addedFilesByType.count(doc.getSchemaDescription());
            }
            // inspection code end
            if(LibraryUtils.isApplicationSpecialShare(fileDesc.getFile())) {
                applicationShared.incrementAndGet();
            }
            fileDesc.setInGnutellaCollection(true);
            return true;
        } else {
            return false;
        }
    }
        
    @Override
    protected boolean removeFileDescImpl(FileDesc fileDesc) {
        if (super.removeFileDescImpl(fileDesc)) {
            numBytes.addAndGet(-fileDesc.getFileSize());
            numRemoved.incrementAndGet();
            if(LibraryUtils.isApplicationSpecialShare(fileDesc.getFile())) {
                applicationShared.decrementAndGet();
            }
            fileDesc.setInGnutellaCollection(false);
            return true;
        } else {
            return false;
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public Object inspect() {        
        Map<Object,Object> inspections = (Map<Object,Object>)super.inspect();
        inspections.put("size of files", numBytes.get());
        inspections.put("removed", numRemoved.get());
        inspections.put("file types", addedFilesByType.inspect());
        return inspections;
    }
    
    /**
     * This method delegates to the FriendFileListImpl isFileAddable method. However it does add the additional check from 
     * files of type document, that gnutellaDocumentSharing must be allowed for the file ot be allowed.
     */
    @Override
    protected boolean isFileAddable(FileDesc fileDesc) {
        String extension = FileUtils.getFileExtension(fileDesc.getFileName());
        MediaType mediaType = MediaType.getMediaTypeForExtension(extension);
        if( MediaType.getDocumentMediaType().equals(mediaType) && !data.isGnutellaDocumentSharingAllowed()) {
            return false;
        }
        return super.isFileAddable(fileDesc);
    }
    
    @Override
    protected boolean isPending(File file, FileDesc fd) {
        return LibraryUtils.isForcedShareDirectory(file.getParentFile())
            || sessionFiles.containsKey(file)
            || super.isPending(file, fd);
    }
    
    @Override
    protected void saveChange(File file, boolean added) {
        // Make sure removed things are removed.
        if(!added) {
            sessionFiles.remove(file);
        }
        
        if(!sessionFiles.containsKey(file)) {
            super.saveChange(file, added);
        }
    }
    
    @Override
    public boolean hasApplicationSharedFiles() {
        return applicationShared.get() > 0;
    }
    
    @Override
    public boolean isFileApplicationShare(String filename) {
        File file = new File(LibraryUtils.APPLICATION_SPECIAL_SHARE, filename);
        try {
            file = FileUtils.getCanonicalFile(file);
        } catch (IOException bad) {
            return false;
        }
        return contains(file);
    }
    
    // Raise access
    @Override
    public List<FileDesc> getFilesInDirectory(File directory) {
        // TODO Auto-generated method stub
        return super.getFilesInDirectory(directory);
    }

    @Override
    public void removeDocuments() {
            for( FileDesc fileDesc : pausableIterable()) {
                String extension = FileUtils.getFileExtension(fileDesc.getFileName());
                MediaType mediaType = MediaType.getMediaTypeForExtension(extension);
                if(MediaType.getDocumentMediaType().equals(mediaType)) {
                    remove(fileDesc);
                }
            }
    }
}
