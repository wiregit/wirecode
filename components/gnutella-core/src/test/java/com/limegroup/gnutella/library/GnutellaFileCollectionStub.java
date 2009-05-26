package com.limegroup.gnutella.library;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

import org.limewire.core.api.Category;

import com.limegroup.gnutella.URN;


public class GnutellaFileCollectionStub extends AbstractFileCollectionStub implements GnutellaFileCollection, GnutellaFileView {
    
    public final static URN DEFAULT_URN;
    static {
        try {
            DEFAULT_URN = URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZZZZZZZZZZ");
        } catch(IOException ignored){
            throw new RuntimeException(ignored);    
        }
    }

    public final static FileDescStub FD_STUB_ONE = new FileDescStub();
    public final static FileDescStub FD_STUB_TWO = new FileDescStub("other.txt");
    
    @Override
    public void addFriend(String id) {
    }
    
    @Override
    public List<String> getFriendList() {
        return Collections.emptyList();
    }
    
    @Override
    public boolean removeFriend(String id) {
        return false;
    }
    
    @Override
    public void setFriendList(List<String> ids) {
    }
    
    @Override
    public Future<FileDesc> addForSession(File file) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public long getNumBytes() {
        long bytes = 0;
        for(FileDesc fd : fileDescList) {
            bytes += fd.getFileSize();
        }
        return bytes;
    }
    
    @Override
    public void clearCategory(Category category) {
        
    }
    
    @Override
    public boolean hasApplicationSharedFiles() {
        return false;
    }
    
    @Override
    public boolean isFileApplicationShare(String filename) {
        return false;
    }
    
    @Override
    public List<FileDesc> getFilesInDirectory(File directory) {
        return super.getFilesInDirectory(directory);
    }

    @Override
    public boolean isAddNewAudioAlways() {
        return false;
    }

    @Override
    public boolean isAddNewImageAlways() {
        return false;
    }

    @Override
    public boolean isAddNewVideoAlways() {
        return false;
    }

    @Override
    public void setAddNewAudioAlways(boolean value) {
    }

    @Override
    public void setAddNewImageAlways(boolean value) {
    }

    @Override
    public void setAddNewVideoAlways(boolean value) {
    }

    @Override
    public void removeDocuments() {
        
    }

    @Override
    public void addSnapshotCategory(Category category) {
    }

    @Override
    public int getId() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }
}
