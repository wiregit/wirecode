package com.limegroup.gnutella.library;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;


public class FileListStub extends GnutellaSharedFileListImpl {

    private List<File> whitelist;
    private FileDescStub fdStub = new FileDescStub();
    private FileDescStub defaultStub = new FileDescStub("other.txt");
    
    private List<FileDesc> descs;
    
    public FileListStub(FileManagerImpl fileManager, Set<File> individual, Set<File> files) {
        super(new Executor() {
            public void execute(Runnable command) {
                command.run();
            }
        }, fileManager, individual, files);
    }
    
    public void setWhitelist(File... files) {        
        this.whitelist = Arrays.asList(files);
    }

    public void setDescs(List<FileDesc> newDescs) {
        this.descs = new ArrayList<FileDesc>();
        for(FileDesc fd : newDescs ) {
            descs.add(fd);
            numBytes += fd.getFile().length();
        }
    }
    
    public void setDescsAddIndividual(List<FileDesc> newDescs) {
        this.descs = new ArrayList<FileDesc>();
        for(FileDesc fd: newDescs) {
            descs.add(fd);
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
        if(descs.contains(fileDesc)) {
            return true;
        } else if(fileDesc.getFile().equals(fdStub.getFile()) || 
                fileDesc.getFile().equals(defaultStub.getFile())) {
            return true;
        } else { 
            return false;
        }
    }
}
