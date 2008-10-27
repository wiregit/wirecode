package com.limegroup.gnutella.library;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.limegroup.gnutella.URN;


public class FileListStub extends GnutellaSharedFileListImpl {
    
    public final static URN NOT_HAVE;
    static {
        try {
            NOT_HAVE = URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZZZZZZZZZZ");
        } catch(IOException ignored){
            throw new RuntimeException(ignored);    
        }
    }

    private FileDescStub fdStub = new FileDescStub();
    private FileDescStub defaultStub = new FileDescStub("other.txt");


    private Map<URN,FileDesc> urns = new HashMap<URN,FileDesc>();
    
    private List<FileDesc> files;
    private List<FileDesc> descs;
    private Map<File, FileDesc> filesToFileDesc;

    
    public FileListStub(LibraryFileData data, ManagedFileList managedList) {
        super(data, managedList);
    }

    public void setDescs(List<FileDesc> newDescs) {
        this.descs = new ArrayList<FileDesc>();
        for(FileDesc fd : newDescs ) {
            descs.add(fd);
        }
    }
    
    public void setDescsAddIndividual(List<FileDesc> newDescs) {
        this.descs = new ArrayList<FileDesc>();
        for(FileDesc fd: newDescs) {
            descs.add(fd);
        }
    }
    
    @Override
    public boolean contains(FileDesc fileDesc) {
        if(fileDesc == null)
            return false;
        if(descs.contains(fileDesc)) {
            return true;
        } else if(fileDesc.getFile().equals(fdStub.getFile()) || 
                fileDesc.getFile().equals(defaultStub.getFile())) {
            return true;
        } else { 
            return false;
        }
    }
    
    @Override
    public FileDesc getFileDescForIndex(int i) {
        if(files != null) {
            if (i < files.size())
                return files.get(i);
        }
        
        return fdStub;
    }
    
    @Override
    public FileDesc getFileDesc(URN urn) {
        if(urn.toString().equals(FileDescStub.DEFAULT_URN))
            return fdStub;
        else if (urn.equals(NOT_HAVE))
            return null;
        else if (urns.containsKey(urn))
            return urns.get(urn);
        else
            return new FileDescStub("other.txt");
    }
    
    @Override
    public FileDesc getFileDesc(File f) {
        FileDesc fd = filesToFileDesc.get(f);
        if(files == null || fd == null) {
            return fdStub;
        } else {
            return fd;
        }
    }
    
    public void setUrns(Map<URN,FileDesc> urns) {
        this.urns = urns;
    }
    
    public void setFiles(Map<File,FileDesc> map) {
        filesToFileDesc = map;
    }
    
    @Override
    public int size() {
        if(filesToFileDesc != null) {
            return filesToFileDesc.size();
        } else {
            return super.size();
        }
    }
    
    public void setFileDesc(List<FileDesc> files) {
        this.files = files;
    }
}
