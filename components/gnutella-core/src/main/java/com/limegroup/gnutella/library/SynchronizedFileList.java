package com.limegroup.gnutella.library;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.limewire.listener.EventListener;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.xml.LimeXMLDocument;


public class SynchronizedFileList implements FileListPackage {

    final FileListPackage fileList;
    final Object mutex;     // Object on which to synchronize
    
    public SynchronizedFileList(FileListPackage fileList) { 
        if(fileList == null)
            throw new NullPointerException();
        this.fileList = fileList;
        mutex = fileList;
    }
    
    public void clear() {
        synchronized (mutex) {
            fileList.clear();
        }
    }
    
    @Override
    public void addFolder(File folder) {
        fileList.addFolder(folder); // TODO: lock?
    }
    
    @Override
    public FileDesc getFileDesc(File f) {
        synchronized(mutex) {
            return fileList.getFileDesc(f);
        }
    }
    
    @Override
    public FileDesc getFileDesc(URN urn) {
        synchronized(mutex) {
            return fileList.getFileDesc(urn);
        }
    }

    public boolean add(FileDesc fileDesc) {
        synchronized (mutex) {
            return fileList.add(fileDesc);
        }
    }
    
    @Override
    public void add(File file) {
        synchronized (mutex) {
            fileList.add(file);
        }
    }
    
    @Override
    public void add(File file, List<LimeXMLDocument> documents) {
        synchronized (mutex) {
            fileList.add(file, documents);
        }        
    }
    
    @Override
    public void addForSession(File file) {
        synchronized(mutex) {
            fileList.addForSession(file);
        }
    }

    @Override
    public boolean contains(File file) {
        synchronized (mutex) {
            return fileList.contains(file);
        }
    }

    @Override
    public boolean remove(File file) {
        synchronized (mutex) {
            return fileList.remove(file);
        }
    }

    public boolean contains(FileDesc fileDesc) {
        synchronized (mutex) {
            return fileList.contains(fileDesc);
        }
    }

    public Iterator<FileDesc> iterator() {
        return fileList.iterator();
    }

    public List<FileDesc> getAllFileDescs() {
        synchronized (mutex) {
            return fileList.getAllFileDescs();
        }
    }

    public int getNumBytes() {
        synchronized (mutex) {
            return fileList.getNumBytes();
        }
    }

    public void addPendingFileAlways(File file) {
        synchronized (mutex) {
            fileList.addPendingFileAlways(file);            
        }
    }

    public void addPendingFileForSession(File file) {
        synchronized (mutex) {
            fileList.addPendingFileForSession(file);            
        }
    }

    public void addPendingFile(File file) {
        synchronized (mutex) {
            fileList.addPendingFile(file);            
        }
    }

    public boolean remove(FileDesc fileDesc) {
        synchronized (mutex) {
            return fileList.remove(fileDesc);
        }
    }

    public int size() {
        synchronized (mutex) {
            return fileList.size();
        }
    }

    public boolean isFileAddable(File file) {
        synchronized (mutex) {
            return fileList.isFileAddable(file);
        }
    }
    
    public List<FileDesc> getFilesInDirectory(File directory) {
        synchronized (mutex) {
            return fileList.getFilesInDirectory(directory);
        }
    }

    public int getNumForcedFiles() {
        synchronized (mutex) {
            return fileList.getNumForcedFiles();
        }
    }
    
    public Object getLock() {
        return mutex;
    }

    public void cleanupListeners() {
        synchronized (mutex) {
            fileList.cleanupListeners();
        }
    }

    public void addFileListListener(EventListener<FileListChangedEvent> listener) {
        synchronized (mutex) {
            fileList.addFileListListener(listener);            
        }
    }

    public void removeFileListListener(EventListener<FileListChangedEvent> listener) {
        synchronized (mutex) {
            fileList.removeFileListListener(listener);
        }
    }
    
    @Override
    public boolean isAddNewAudioAlways() {
        synchronized (mutex) {
            return fileList.isAddNewAudioAlways();
        }
    }

    @Override
    public boolean isAddNewImageAlways() {
        synchronized (mutex) {
            return fileList.isAddNewImageAlways();
        }
    }

    @Override
    public boolean isAddNewVideoAlways() {
        synchronized (mutex) {
            return fileList.isAddNewVideoAlways();
        }
    }

    @Override
    public void setAddNewAudioAlways(boolean value) {
        synchronized (mutex) {
            fileList.setAddNewAudioAlways(value);
        }
    }

    @Override
    public void setAddNewImageAlways(boolean value) {
        synchronized (mutex) {
            fileList.setAddNewImageAlways(value);
        }
    }

    @Override
    public void setAddNewVideoAlways(boolean value) {
        synchronized (mutex) {
            fileList.setAddNewVideoAlways(value);
        }
    }
    
    /////////// backwards compatibility /////////////////////////

    public File[] getIndividualFiles() {
        synchronized (mutex) {
            return fileList.getIndividualFiles();
        }
    }

    public int getNumIndividualFiles() {
        synchronized (mutex) {
            return fileList.getNumIndividualFiles();
        }
    }

    public boolean isIndividualFile(File file) {
        synchronized (mutex) {
            return fileList.isIndividualFile(file);
        }
    }
}
