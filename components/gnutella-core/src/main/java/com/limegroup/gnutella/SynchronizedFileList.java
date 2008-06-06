package com.limegroup.gnutella;

import java.io.File;
import java.util.List;

import org.limewire.collection.IntSet;

public class SynchronizedFileList implements FileList {

    final FileList fileList;
    final Object mutex;     // Object on which to synchronize
    
    public SynchronizedFileList(FileList fileList) { 
        if(fileList == null)
            throw new NullPointerException();
        this.fileList = fileList;
        mutex = this;
    }
    
    public void addFile(File file, FileDesc fileDesc) {
        synchronized (mutex) {
            fileList.addFile(file, fileDesc);
        }
    }

    public boolean contains(File file) {
        synchronized (mutex) {
            return fileList.contains(file);
        }
    }

    public boolean contains(FileDesc fileDesc) {
        synchronized (mutex) {
            return fileList.contains(fileDesc);
        }
    }

    public boolean contains(URN urn) {
        synchronized (mutex) {
            return fileList.contains(urn);
        }
    }

    public FileDesc get(int i) {
        synchronized (mutex) {
            return fileList.get(i);
        }
    }

    public List<FileDesc> getAllFileDescs() {
        synchronized (mutex) {
            return fileList.getAllFileDescs();
        }
    }

    public FileDesc getFileDesc(URN urn) {
        synchronized (mutex) {
            return fileList.getFileDesc(urn);
        }
    }

    public FileDesc getFileDesc(File file) {
        synchronized (mutex) {
            return fileList.getFileDesc(file);
        }
    }

    public IntSet getIndicesForUrn(URN urn) {
        synchronized (mutex) {
            return fileList.getIndicesForUrn(urn);
        }
    }

    public int getNumBytes() {
        synchronized (mutex) {
            return fileList.getNumBytes();
        }
    }

    public int getNumFiles() {
        synchronized (mutex) {
            return fileList.getNumFiles();
        }
    }

    public boolean isValidSharedIndex(int i) {
        synchronized (mutex) {
            return fileList.isValidSharedIndex(i);
        }
    }

    public void remove(File file) {
        synchronized (mutex) {
            fileList.remove(file);
        }
    }

    public void remove(FileDesc fileDesc) {
        synchronized (mutex) {
            fileList.remove(fileDesc);
        }
    }

    public void remove(URN urn) {
        synchronized (mutex) {
            fileList.remove(urn);
        }
    }

    public void resetVariables() {
        synchronized (mutex) {
            fileList.resetVariables();
        }
    }

    public int getListLength() {
        synchronized (mutex) {
            return fileList.getListLength();
        }
    }

    public void updateUrnIndex(FileDesc fileDesc) {
        synchronized (mutex) {
            fileList.updateUrnIndex(fileDesc);
        }
    }
    
    public void addIncompleteFile(File incompleteFile, IncompleteFileDesc incompleteFileDesc) {
        synchronized (mutex) {
            fileList.addIncompleteFile(incompleteFile, incompleteFileDesc);
        }
    }

    public int getNumForcedFiles() {
        synchronized (mutex) {
            return fileList.getNumForcedFiles();
        }
    }

    public int getNumIncompleteFiles() {
        synchronized (mutex) {
            return fileList.getNumIncompleteFiles();
        }
    }

    public List<FileDesc> getFilesInDirectory(File directory) {
        synchronized (mutex) {
            return fileList.getFilesInDirectory(directory);
        }
    }

}
