package com.limegroup.gnutella;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.limewire.inspection.Inspectable;
import org.limewire.util.ByteUtils;
import org.limewire.util.FileUtils;

import com.limegroup.gnutella.licenses.LicenseType;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * A List of FileDescs that are grouped together 
 */
public class FileListImpl implements FileList, FileEventListener, Inspectable {

    /**
     * A list of listeners for this list
     */
    private final CopyOnWriteArrayList<FileListListener> listeners;
    
    /** 
     * List of all the FileDescs in this FileList. This is a continous non-null
     * list.
     */
    protected final List<FileDesc> fileDescs;
    
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
     * Name of this FileList
     */
    private final String name;
    
    /**
     * Size of all the FileDescs in this list in bytes
     */
    protected long numBytes;
    
    protected final FileManager fileManager;
    
    public FileListImpl(String name, FileManager fileManager, Set<File> individualFiles) {
        this.name = name;
        this.fileManager = fileManager;
        this.individualFiles = individualFiles;
        this.fileDescs = new ArrayList<FileDesc>();
        this.pendingFiles = new ArrayList<File>(); 

        fileManager.addFileEventListener(this);
        
        listeners = new CopyOnWriteArrayList<FileListListener>();
        
        clear();
    }
    
    public String getName() {
        return name;
    }

    public void addFileAlways(File file) {
        addFile(file, LimeXMLDocument.EMPTY_LIST);
    }
    
    public void addFileAlways(File file, List<? extends LimeXMLDocument> list) {
        addFile(file, list);
    }

    public void addFileForSession(File file) {
        addFile(file);
    }
    
    public void addFile(File file) {
        addFile(file, LimeXMLDocument.EMPTY_LIST);
    }
    
    public void addFile(File file, List<? extends LimeXMLDocument> list) {
        synchronized(this) {
            if(!pendingFiles.contains(file))
                pendingFiles.add(file);
        }
        fileManager.addFile(file, list);
    }

    public void add(FileDesc fileDesc) {
        if(addFileDesc(fileDesc))
            fireAddEvent(fileDesc);
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
     * Performs the actual add. No notication is sent when this returns.
     * @return true if the fileDesc was added, false otherwise
     */
    protected boolean addFileDesc(FileDesc fileDesc) {
        if(fileDesc == null)
            throw new IllegalArgumentException("FileDesc cannot be null");
        
        // always remove pending file, whether it is allowed to get added or not
        if(pendingFiles.contains(fileDesc.getFile()))
            pendingFiles.remove(fileDesc.getFile());
        
        if(!fileDescs.contains(fileDesc) && isFileAddable(fileDesc)) {
            fileDescs.add(fileDesc);
            numBytes += fileDesc.getFileSize();
            addAsIndividualFile(fileDesc);
            return true;
        } else
            return false;
    }
    
    public boolean remove(FileDesc fileDesc) {
        if(removeFileDesc(fileDesc)) {
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
        if(pendingFiles.contains(fileDesc.getFile())) {
            pendingFiles.remove(fileDesc.getFile());
        }
        if(fileDescs.contains(fileDesc)) {
            fileDescs.remove(fileDesc);
            numBytes -= fileDesc.getFileSize();
            
            removeAsIndividualFile(fileDesc);
            return true;
        } else {
            return false;
        }
    }
    
    public boolean contains(FileDesc fileDesc) {
        if(fileDesc == null)
            return false;
        
        return fileDescs.contains(fileDesc) || pendingFiles.contains(fileDesc.getFile());
    }
        
    public Iterator<FileDesc> iterator() {
        return fileDescs.iterator();
    }
    
    public List<FileDesc> getAllFileDescs() { 
        return new ArrayList<FileDesc>(fileDescs);
    }
    
    public int getNumBytes() {
        return ByteUtils.long2int(numBytes);
    }
    
    public int size() {
        return fileDescs.size();
    }
    
    public void clear() {
        fileDescs.clear();
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

        for(FileDesc fd : fileDescs) {
            if( fd == null)
                continue;
            if(directory.equals(fd.getFile().getParentFile()))
                list.add(fd);
        }
        
        return list;
    }

    public Object inspect() {
        Map<String,Object> inspections = new HashMap<String,Object>();
        inspections.put("size of files", Long.valueOf(numBytes));
        inspections.put("num of files", Integer.valueOf(fileDescs.size()));
        return inspections;
    }
    
    public int getNumForcedFiles() {
        return 0;
    }
    
    public Object getLock() {
        return this;
    }

    public void addFileListListener(FileListListener listener) {
        if(listener == null)
            throw new IllegalArgumentException("FileListListener cannot be null");
        listeners.addIfAbsent(listener);
    }

    public void removeFileListListener(FileListListener listener) {
        if(listener == null)
            throw new IllegalArgumentException("FileListListener cannot be null");
        listeners.remove(listener);
    }
    
    protected void fireAddEvent(FileDesc fileDesc) {
        for(FileListListener listener : listeners) {
            listener.addEvent(fileDesc);
        }
    }
    
    protected void fireRemoveEvent(FileDesc fileDesc) {
        for(FileListListener listener : listeners) {
            listener.removeEvent(fileDesc);
        }
    }
    
    protected void fireChangeEvent(FileDesc oldFileDesc, FileDesc newFileDesc) {
        for(FileListListener listener : listeners) {
            listener.changeEvent(oldFileDesc, newFileDesc);
        }
    }
    
    /**
     * Updates the list if a containing file has been renamed
     */
    protected void updateFileDescs(FileDesc oldFileDesc, FileDesc newFileDesc) {     
        if (removeFileDesc(oldFileDesc)) {
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
    public void handleFileEvent(FileManagerEvent evt) {
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
}