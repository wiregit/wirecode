package com.limegroup.gnutella;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import com.limegroup.gnutella.xml.LimeXMLDocument;

public class SynchronizedFileList implements FileList {

    final FileList fileList;
    final Object mutex;     // Object on which to synchronize
    
    public SynchronizedFileList(FileList fileList) { 
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
    
    public void addFileDesc(FileDesc fileDesc) {
        synchronized (mutex) {
            fileList.addFileDesc(fileDesc);
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
    
    public void addFileAlways(File file) {
        fileList.addFileAlways(file);
    }

    public void addFileAlways(File file, List<? extends LimeXMLDocument> list) {
        fileList.addFileAlways(file,list);
    }

    public void addFileForSession(File file) {
        fileList.addFileForSession(file);
    }
    
    public void addFile(File file) {
        fileList.addFile(file);
    }

    public void addFile(File file, List<? extends LimeXMLDocument> list) {
        fileList.addFile(file, list);
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
    
    public Object getLock() {
        return mutex;
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

    public int getNumForcedFiles() {
        synchronized (mutex) {
            return fileList.getNumForcedFiles();
        }
    }
}
