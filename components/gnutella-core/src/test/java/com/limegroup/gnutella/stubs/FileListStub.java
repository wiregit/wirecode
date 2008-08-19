package com.limegroup.gnutella.stubs;

import java.io.File;
import java.util.List;
import java.util.Set;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.SharedFileListImpl;

public class FileListStub extends SharedFileListImpl {

    private FileDescStub fdStub = new FileDescStub();
    private FileDescStub defaultStub = new FileDescStub("other.txt");
    
    public FileListStub(String name, FileManager fileManager, Set<File> individual, Set<File> files) {
        super(name, fileManager, individual, files);
    }

    public void setDescs(List<FileDesc> descs) {
        for(FileDesc fd : descs ) {
            fileDescs.add(fd);
            numBytes += fd.getFile().length();
        }
    }
    
    public void setDescsAddIndividual(List<FileDesc> files) {
        for(FileDesc fd: files) {
            fileDescs.add(fd);
            numBytes += fd.getFile().length();
            individualFiles.add(fd.getFile());
        }
    }
    
    @Override
    public boolean contains(FileDesc fileDesc) {
        if(fileDesc == null)
            return false;
        
        if(fileDescs.contains(fileDesc))
            return true;
        else if(fileDesc.getFile().equals(fdStub.getFile()) ||
                fileDesc.getFile().equals(defaultStub.getFile()))
            return true;
        else 
            return false;
    }
}
