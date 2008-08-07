package org.limewire.core.impl.library;

import java.io.File;
import java.util.Collections;
import java.util.Map;

import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LibraryListListener;
import org.limewire.core.api.library.LibraryManager;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;


public class MockLibraryManager implements LibraryManager {

    private EventList<FileItem> allFileList;
    private EventList<FileItem> gnutellaList;
    private EventList<FileItem> buddyList;
    
    public MockLibraryManager() {
        allFileList = GlazedLists.threadSafeList(new BasicEventList<FileItem>());
        gnutellaList = GlazedLists.threadSafeList(new BasicEventList<FileItem>());
        buddyList = GlazedLists.threadSafeList(new BasicEventList<FileItem>());

        initializeMockGnutellaData();
        initializeMockBuddyData();
        
        allFileList.addAll(gnutellaList);
        allFileList.addAll(buddyList);
    }
    
    private void initializeMockGnutellaData(){
        MockFileItem item = new MockFileItem("Small Town Hero.mp3", 1000,12345,23456, 5,1);
        gnutellaList.add(item);
        
        item = new MockFileItem("LimeWireStore.html", 12345,12312,534512,3,2);
        gnutellaList.add(item);
        
        item = new MockFileItem("Pictures.png", 32423, 3415412, 123123,0,0);
        gnutellaList.add(item);
    }
    
    private void initializeMockBuddyData(){
        MockFileItem item = new MockFileItem("Small Town Hero.mp3", 1000,12345,23456,1,1);
        buddyList.add(item);
        
        item = new MockFileItem("LimeWireStore.html", 12345,12312,534512,5,0);
        buddyList.add(item);
        
        item = new MockFileItem("Pictures.png", 32423, 3415412, 123123,0,0);
        buddyList.add(item);
    }
    
    @Override
    public void addLibraryLisListener(LibraryListListener libraryListener) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public EventList<FileItem> getAllFiles() {
        return allFileList;
    }   
    
    @Override
    public EventList<FileItem> getAllBuddyList() {
        return buddyList;
    }

    @Override
    public EventList<FileItem> getGnutellaList() {
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

    @Override
    public void addGnutellaFile(File file) {
        MockFileItem item = new MockFileItem(file.getName(), 1000,12345,23456, 0,0);
        gnutellaList.add(item);
    }

    @Override
    public void removeGnutellaFile(File file) {
        // TODO Auto-generated method stub
        
    }
}
