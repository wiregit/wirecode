package com.limegroup.gnutella;

import java.io.File;
import java.util.Iterator;
import java.util.List;

public class SynchronizedFileList implements FileListPackage {

    final FileListPackage fileList;
    final Object mutex;     // Object on which to synchronize
    
    public SynchronizedFileList(FileListPackage fileList) { 
        if(fileList == null)
            throw new NullPointerException();
        this.fileList = fileList;
        mutex = fileList;
    }
    
    public String getName() {
        return fileList.getName();
    }

    public void clear() {
        synchronized (mutex) {
            fileList.clear();
        }
    }
    
    public boolean add(FileDesc fileDesc) {
        synchronized (mutex) {
            return fileList.add(fileDesc);
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
    
    public void addFileListListener(FileListListener listener) {
        fileList.addFileListListener(listener);
    }

    public void removeFileListListener(FileListListener listener) {
        fileList.removeFileListListener(listener);
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

    public boolean hasIndividualFiles() {
        synchronized (mutex) {
            return fileList.hasIndividualFiles();
        }
    }

    public boolean isIndividualFile(File file) {
        synchronized (mutex) {
            return fileList.isIndividualFile(file);
        }
    }
}
