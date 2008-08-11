package org.limewire.core.impl.library;

import java.util.Collections;
import java.util.Map;

import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FileList;
import org.limewire.core.api.library.LibraryListListener;
import org.limewire.core.api.library.LibraryManager;

import ca.odell.glazedlists.EventList;


public class MockLibraryManager implements LibraryManager {

    private FileListAdapter allFileList;
    private FileListAdapter gnutellaList;
    private FileListAdapter buddyList;
    
    public MockLibraryManager() {
        allFileList = new FileListAdapter();
        gnutellaList = new FileListAdapter();
        buddyList = new FileListAdapter();

        initializeMockGnutellaData();
        initializeMockBuddyData();
    }
    
    private void initializeMockGnutellaData(){
        MockFileItem item = new MockFileItem("Small Town Hero.mp3", 1000,12345,23456, 5,1);
        gnutellaList.addFileItem(item);
        allFileList.addFileItem(item);
        
        item = new MockFileItem("LimeWireStore.html", 12345,12312,534512,3,2);
        gnutellaList.addFileItem(item);
        allFileList.addFileItem(item);
        
        item = new MockFileItem("Pictures.png", 32423, 3415412, 123123,0,0);
        gnutellaList.addFileItem(item);
        allFileList.addFileItem(item);
    }
    
    private void initializeMockBuddyData(){
        MockFileItem item = new MockFileItem("Small Town Hero.mp3", 1000,12345,23456,1,1);
        buddyList.addFileItem(item);
        allFileList.addFileItem(item);
        
        item = new MockFileItem("LimeWireStore.html", 12345,12312,534512,5,0);
        buddyList.addFileItem(item);
        allFileList.addFileItem(item);
        
        item = new MockFileItem("Pictures.png", 32423, 3415412, 123123,0,0);
        buddyList.addFileItem(item);
        allFileList.addFileItem(item);
    }
    
    @Override
    public void addLibraryLisListener(LibraryListListener libraryListener) {
        // TODO Auto-generated method stub
        
    }
    

    @Override
    public FileList getLibraryList() {
        return allFileList;
    }
    
    @Override
    public FileList getAllBuddyList() {
        return buddyList;
    }
    
    @Override
    public FileList getGnutellaList() {
        return gnutellaList;
    }

    @Override
    public Map<String, EventList<FileItem>> getUniqueLists() {
        return Collections.emptyMap();
    }

    @Override
    public void removeLibraryListener(LibraryListListener libraryListener) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addBuddy(String name) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removeBuddy(String name) {
        // TODO Auto-generated method stub
        
    }
}
