package com.limegroup.gnutella.library;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;


public class FileListStub extends GnutellaSharedFileListImpl {

    private List<File> whitelist;
    private FileDescStub fdStub = new FileDescStub();
    private FileDescStub defaultStub = new FileDescStub("other.txt");
    
    public FileListStub(FileManager fileManager, Set<File> individual, Set<File> files) {
        super(fileManager, individual, files);
    }
    
    public void setWhitelist(File... files) {        
        this.whitelist = Arrays.asList(files);
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
    public boolean isFileAddable(File file) {
        if(whitelist != null) {
            return whitelist.contains(file);
        } else {
            return super.isFileAddable(file);
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
