package com.limegroup.gnutella.library;

import java.util.concurrent.ScheduledExecutorService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class FileManagerStub extends FileManagerImpl {

    private FileListStub fileListStub;    
    
    @Inject
    public FileManagerStub(ManagedFileListImpl managedList, @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor) {
        super(managedList, backgroundExecutor);
    
        fileListStub = new FileListStub(new LibraryFileData(), this.getManagedFileList());
        
    }
    
    @Override
    public GnutellaFileList getGnutellaSharedFileList() {
        return fileListStub;
    }
}

