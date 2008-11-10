package com.limegroup.gnutella.library;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.limewire.util.FileUtils;


class GnutellaFileListImpl extends AbstractFileList implements GnutellaFileList {
    
    /** Size of all the FileDescs in this list in bytes */
    private final AtomicLong numBytes;
    
    /** Where to store the info. */
    private final LibraryFileData data;
    
    /** A list of session-shared data -- it isn't saved. */
    private Map<File, File> sessionFiles = new ConcurrentHashMap<File, File>();
    
    /** A list of application shared files. */
    private final AtomicInteger applicationShared = new AtomicInteger();
    
    public GnutellaFileListImpl(LibraryFileData data, ManagedFileListImpl managedList) {
        super(managedList);
        this.data = data;
        this.numBytes = new AtomicLong();
    }
    
    @Override
    public boolean add(FileDesc fileDesc) {
        return super.add(fileDesc);
    }
    
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
            if(LibraryUtils.isApplicationSpecialShare(fileDesc.getFile())) {
                applicationShared.incrementAndGet();
            }
            fileDesc.setSharedWithGnutella(true);
            return true;
        } else {
            return false;
        }
    }
    
    @Override
    protected boolean removeFileDescImpl(FileDesc fileDesc) {
        if (super.removeFileDescImpl(fileDesc)) {
            numBytes.addAndGet(-fileDesc.getFileSize());
            if(LibraryUtils.isApplicationSpecialShare(fileDesc.getFile())) {
                applicationShared.decrementAndGet();
            }
            fileDesc.setSharedWithGnutella(false);
            return true;
        } else {
            return false;
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public Object inspect() {        
        Map<String,Object> inspections = (Map<String,Object>)super.inspect();
        inspections.put("size of files", numBytes.get());        
        return inspections;
    }
    
    /**
     * Returns true if this list is allowed to add this FileDesc
     * @param fileDesc - FileDesc to be added
     */
    @Override
    protected boolean isFileAddable(FileDesc fileDesc) {
        if(fileDesc instanceof IncompleteFileDesc) {
            return false;
        } else if (fileDesc.getLimeXMLDocuments().size() != 0
                && isStoreXML(fileDesc.getLimeXMLDocuments().get(0))) {
            return false;
        } else {
            return true;
        }
    }
    
    @Override
    protected boolean isPending(File file, FileDesc fd) {
        return LibraryUtils.isForcedShareDirectory(file.getParentFile())
            || data.isSharedWithGnutella(file) 
            || sessionFiles.containsKey(file);
    }
    
    @Override
    protected void saveChange(File file, boolean added) {
        if(!sessionFiles.containsKey(file)) {
            data.setSharedWithGnutella(file, added);
        }        
        
        // Make sure removed things are removed.
        if(!added) {
            sessionFiles.remove(file);
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
}
