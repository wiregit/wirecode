package com.limegroup.gnutella.library;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import org.limewire.collection.IntSet;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;
import org.limewire.util.ByteUtils;
import org.limewire.util.FileUtils;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.licenses.LicenseType;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * A List of FileDescs that are grouped together 
 */
abstract class FileListImpl implements FileListPackage, EventListener<FileManagerEvent> {
    
    private final Executor eventThread;

    /** 
     * A list of listeners for this list
     */
    private final EventMulticaster<FileListChangedEvent> listenerSupport;
    
    protected final IntSet fileDescIndexes;
    
    /**
     * List of files whose FileDescs are in the List and are not located in a 
     * completed folder.
     */
    protected final Set<File> individualFiles;
    
    /**
     * A list of files that have been added to FileManager but have yet to have 
     * their FileDescs calculated. 
     */
    protected final List<File> pendingFiles;
    
    /**
     * Size of all the FileDescs in this list in bytes
     */
    protected long numBytes;
    
    protected final FileManagerImpl fileManager;
    
    public FileListImpl(Executor eventThread, FileManagerImpl fileManager, Set<File> filesToShare) {
        this.eventThread = eventThread;
        this.fileManager = fileManager;
        this.individualFiles = filesToShare;
        this.fileDescIndexes = new IntSet();
        pendingFiles = new ArrayList<File>();
    
        fileManager.addFileEventListener(this);
        
        listenerSupport = new EventMulticasterImpl<FileListChangedEvent>();
        
        clear();
    }
    
    @Override
    public FileDesc getFileDescForIndex(int index) {
        FileDesc fd = fileManager.getManagedFileList().getFileDescForIndex(index);
        if(contains(fd)) {
            return fd;
        } else {
            return null;
        }
    }
    
    @Override
    public FileDesc getFileDesc(File f) {
        return checkContains(fileManager.getManagedFileList().getFileDesc(f));
    }
    
    @Override
    public FileDesc getFileDesc(URN urn) {
        return checkContains(fileManager.getManagedFileList().getFileDesc(urn));
    }
    
    private FileDesc checkContains(FileDesc fd) {
        if(contains(fd)) {
            return fd;
        } else {
            return null;
        }
    }
    
    @Override
    public List<FileDesc> getFileDescsMatching(URN urn) {
        List<FileDesc> fds = null;
        List<FileDesc> matching = fileManager.getManagedFileList().getFileDescsMatching(urn);
        for(FileDesc fd : matching) {
            if(contains(fd)) {
                if(fds == null) {
                    fds = new ArrayList<FileDesc>(matching.size());
                }
                fds.add(fd);
            }
        }
        
        if(fds == null) {
            return Collections.emptyList();
        } else {
            return fds;
        }
    }

    public void addPendingFileAlways(File file) {
        addPendingFile(file);
    }

    public void addPendingFileForSession(File file) {
        addPendingFile(file);
    }

    public void addPendingFile(File file) {
        if(!pendingFiles.contains(file))
            pendingFiles.add(file);
    }
    
    @Override
    public void addFolder(File folder) {
        // TODO: Add the folder to managelist as a managed folder,
        //       then iterate through the contents and share every resulting
        //       FD.
        
    }
    
    public boolean add(FileDesc fileDesc) {
        if(addFileDesc(fileDesc)) {
            fileManager.setDirtySaveLater();
            fireAddEvent(fileDesc);
            return true;
        } else
            return false;
    }
    
    /**
     * Only called by events from FileManager. Will only add this
     * FileDesc if this list was explicitly waiting for this file
     */
    protected void addPendingFileDesc(FileDesc fileDesc) {
        if(pendingFiles.contains(fileDesc.getFile()))
            add(fileDesc);
    }
        
    /**
     * Performs the actual add. No notification is sent when this returns.
     * @return true if the fileDesc was added, false otherwise
     */
    protected boolean addFileDesc(FileDesc fileDesc) {
        if(fileDesc == null)
            throw new IllegalArgumentException("FileDesc cannot be null");
        
        // always remove pending file, whether it is allowed to get added or not
        pendingFiles.remove(fileDesc.getFile());
        
        if(isFileAddable(fileDesc) && fileDescIndexes.add(fileDesc.getIndex())) {
            numBytes += fileDesc.getFileSize();
            addAsIndividualFile(fileDesc);
            return true;
        } else {
            return false;
        }
    }
    
    public boolean remove(File file) {
        FileDesc fd = fileManager.getManagedFileList().getFileDesc(file);
        if(fd != null) {
            return remove(fd);
        } else {
            return false;
        }
        
    }
    
    public boolean remove(FileDesc fileDesc) {
        if(removeFileDesc(fileDesc)) {
            fileManager.setDirtySaveLater();
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
    protected boolean removeFileDesc(FileDesc fileDesc) {
        if(fileDesc == null)
            throw new IllegalArgumentException("FileDesc cannot be null");
        
        // if we were waiting on this FileDesc but still hadn't recieved it for
        // some reason, remove it from pending files anyways
        pendingFiles.remove(fileDesc.getFile());
    
        if(fileDescIndexes.remove(fileDesc.getIndex())) {
            numBytes -= fileDesc.getFileSize();
            removeAsIndividualFile(fileDesc);
            return true;
        } else {
            return false;
        }
    }
    
    @Override
    public boolean contains(File file) {
        return contains(fileManager.getManagedFileList().getFileDesc(file));
    }
    
    public boolean contains(FileDesc fileDesc) {
        if(fileDesc == null) {
            return false;
        }
        
        return fileDescIndexes.contains(fileDesc.getIndex()) || pendingFiles.contains(fileDesc.getFile());
    }
    
    public Iterable<FileDesc> iterable() {
        return new Iterable<FileDesc>() {
            @Override
            public Iterator<FileDesc> iterator() {
                return new FileListIterator(FileListImpl.this, fileDescIndexes);
            }
        };
    }
    
    @Override
    public Iterable<FileDesc> threadSafeIterable() {
        return new Iterable<FileDesc>() {
            @Override
            public Iterator<FileDesc> iterator() {
                return new ThreadSafeFileListIterator(FileListImpl.this);
            }
        };
    }
    
    public int getNumBytes() {
        return ByteUtils.long2int(numBytes);
    }

    public int size() {
        return fileDescIndexes.size();
    }

    public void clear() {
        fileDescIndexes.clear();
        pendingFiles.clear();
        numBytes = 0;
    }
    
    public boolean isFileAddable(File file) {
        return true;
    }
    
    public List<FileDesc> getFilesInDirectory(File directory) {
        if (directory == null)
            throw new NullPointerException("null directory");
        
        // Remove case, trailing separators, etc.
        try {
            directory = FileUtils.getCanonicalFile(directory);
        } catch (IOException e) { // invalid directory ?
            return Collections.emptyList();
        }

        List<FileDesc> list = new ArrayList<FileDesc>();

        for(FileDesc fd : threadSafeIterable()) {
            if(directory.equals(fd.getFile().getParentFile()))
                list.add(fd);
        }
        
        return list;
    }

    public Object inspect() {
        Map<String,Object> inspections = new HashMap<String,Object>();
        inspections.put("size of files", Long.valueOf(numBytes));
        inspections.put("num of files", Integer.valueOf(fileDescIndexes.size()));
        return inspections;
    }
    
    public int getNumForcedFiles() {
        return 0;
        }
        
    public Object getLock() {
        return this;
    }

    public void addFileListListener(EventListener<FileListChangedEvent> listener) {
        listenerSupport.addListener(listener);
    }

    public void removeFileListListener(EventListener<FileListChangedEvent> listener) {
        listenerSupport.removeListener(listener);
    }
    
    /**
     * Fires an addFileDesc event to all the listeners
     * @param fileDesc that was added
     */
    protected void fireAddEvent(final FileDesc fileDesc) {
        eventThread.execute(new Runnable() {
            @Override
            public void run() {
                listenerSupport.handleEvent(new FileListChangedEvent(FileListImpl.this, FileListChangedEvent.Type.ADDED, fileDesc));
            }
        });
    }
    
    /**
     * Fires a removeFileDesc event to all the listeners
     * @param fileDesc that was removed
     */
    protected void fireRemoveEvent(final FileDesc fileDesc) {
        eventThread.execute(new Runnable() {
            @Override
            public void run() {
                listenerSupport.handleEvent(new FileListChangedEvent(FileListImpl.this, FileListChangedEvent.Type.REMOVED, fileDesc));
            }
        });
    }

    /**
     * Fires a changeEvent to all the listeners
     * @param oldFileDesc FileDesc that was there previously
     * @param newFileDesc FileDesc that replaced oldFileDesc
     */
    protected void fireChangeEvent(final FileDesc oldFileDesc, final FileDesc newFileDesc) {
        eventThread.execute(new Runnable() {
            @Override
            public void run() {
                listenerSupport.handleEvent(new FileListChangedEvent(FileListImpl.this, FileListChangedEvent.Type.CHANGED, oldFileDesc, newFileDesc));
            }
        });
    }
    
    /**
     * Updates the list if a containing file has been renamed
     */
    protected void updateFileDescs(FileDesc oldFileDesc, FileDesc newFileDesc) {     
        if (removeFileDesc(oldFileDesc)) {
            fileManager.setDirtySaveLater();
            if(addFileDesc(newFileDesc)) {
                fireChangeEvent(oldFileDesc, newFileDesc);
            } else {
                fireRemoveEvent(oldFileDesc);
            }
        }
    }

    /**
     * Removes this FileDesc as an individual file
     */
    private boolean removeAsIndividualFile(FileDesc fileDesc) {
        if(individualFiles != null)
            return individualFiles.remove(fileDesc.getFile());
        else
            return false;
    }
    
    /**
     * Adds this FileDesc as an individual file
     */
    protected void addAsIndividualFile(FileDesc fileDesc) {
    }

    /**
     * Returns true if this list is allowed to add this FileDesc
     * @param fileDesc - FileDesc to be added
     */
    protected boolean isFileAddable(FileDesc fileDesc) {
        return true;
    }

    /**
     * Returns true if the XML doc contains information regarding the LWS
     */
    protected boolean isStoreXML(LimeXMLDocument doc) {
       return doc != null && doc.getLicenseString() != null &&
               doc.getLicenseString().equals(LicenseType.LIMEWIRE_STORE_PURCHASE.name());
    }
    
    /**
     * Listens for changes from FileManager and updates this list if 
     * a containing file is modified
     */
    public void handleEvent(FileManagerEvent evt) {
        switch(evt.getType()) {
            case ADD_FILE:
                synchronized (this) {
                    addPendingFileDesc(evt.getNewFileDesc());
                }
                break;
            case FILE_ALREADY_ADDED:
                synchronized (this) {
                    addPendingFileDesc(evt.getNewFileDesc());
                }
                break;
            case ADD_FAILED_FILE:
                synchronized (this) {
                    if(pendingFiles.contains(evt.getNewFile())){
                        pendingFiles.remove(evt.getNewFile());
                    }
                }
                break;
            case REMOVE_FILE:
                synchronized (this) {
                    remove(evt.getNewFileDesc());                    
                }
                break;
            case RENAME_FILE:
                synchronized (this) {
                    updateFileDescs(evt.getOldFileDesc(), evt.getNewFileDesc());                 
                }
                break;
            case CHANGE_FILE:
                synchronized (this) {
                    updateFileDescs(evt.getOldFileDesc(), evt.getNewFileDesc());                   
                }
                break;
        }
    }

    public void cleanupListeners() {
        fileManager.removeFileEventListener(this);
    }
    
    @Override
    public void setAddNewImageAlways(boolean value) {}
    @Override
    public boolean isAddNewImageAlways(){return false;}
    @Override
    public void setAddNewAudioAlways(boolean value){}
    @Override
    public boolean isAddNewAudioAlways(){return false;}
    @Override
    public void setAddNewVideoAlways(boolean value){}
    @Override
    public boolean isAddNewVideoAlways(){return false;}
    
    ///// BELOW for backwards compatibility with LW 4.x. Notion of an individual file ////
    /////   does not exist in 5.x  
    ///// Do Not Add anything below this line
    
    public File[] getIndividualFiles() {
        ArrayList<File> files = new ArrayList<File>(individualFiles.size());
        for(File f : individualFiles) {
            if (f.exists())
                files.add(f);
        }
          
        if (files.isEmpty())
            return new File[0];
        else
            return files.toArray(new File[files.size()]);
    }

    public int getNumIndividualFiles() {
        return individualFiles.size();
    }

    public boolean hasIndividualFiles() {
        return !individualFiles.isEmpty();
    }

    public boolean isIndividualFile(File file) {
        return individualFiles.contains(file);
    }
    
    protected int getMaxIndex() {
        return fileDescIndexes.max();
    }
    
    protected int getMinIndex() {
        return fileDescIndexes.min();
    }
}
