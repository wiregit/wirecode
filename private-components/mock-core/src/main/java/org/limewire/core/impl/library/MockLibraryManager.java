package org.limewire.core.impl.library;

import java.util.HashMap;
import java.util.Map;

import org.limewire.core.api.library.FileList;
import org.limewire.core.api.library.LibraryListListener;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.FileItem.Category;


public class MockLibraryManager implements LibraryManager {

    private FileListAdapter allFileList;
    private FileListAdapter gnutellaList;
    private FileListAdapter buddyList;
    
    private Map<String, FileList> buddyMap;
    
    public MockLibraryManager() {
        allFileList = new FileListAdapter("My Library");
        gnutellaList = new FileListAdapter("Gnutella List");
        buddyList = new FileListAdapter("Buddy List");
        
        buddyMap = new HashMap<String, FileList>();

        initializeMockGnutellaData();
        initializeMockBuddyData();
        initializeBuddys();
    }
    
    private void initializeMockGnutellaData(){
        MockFileItem item = new MockFileItem("Small Town Hero.mp3", 1000,12345,23456, 5,1, Category.AUDIO);
        gnutellaList.addFileItem(item);
        allFileList.addFileItem(item);
        
        item = new MockFileItem("LimeWireStore.html", 12345,12312,534512,3,2, Category.AUDIO);
        gnutellaList.addFileItem(item);
        allFileList.addFileItem(item);
        
        item = new MockFileItem("Pictures.png", 32423, 3415412, 123123,0,0, Category.DOCUMENT);
        gnutellaList.addFileItem(item);
        allFileList.addFileItem(item);
        
        Thread t = new Thread(new Runnable(){
            public void run(){
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    return;
                }
                MockFileItem item = new MockFileItem("Lazy load.png", 32423, 3415412, 123123,0,0, Category.DOCUMENT);
                gnutellaList.addFileItem(item);
                allFileList.addFileItem(item);
            }
        });
        t.start();
    }
    
    private void initializeMockBuddyData(){
        MockFileItem item = new MockFileItem("Small Town Hero.mp3", 1000,12345,23456,1,1, Category.IMAGE);
        buddyList.addFileItem(item);
        allFileList.addFileItem(item);
        
        item = new MockFileItem("LimeWireStore.html", 12345,12312,534512,5,0, Category.OTHER);
        buddyList.addFileItem(item);
        allFileList.addFileItem(item);
        
        item = new MockFileItem("Pictures.png", 32423, 3415412, 123123,0,0, Category.OTHER);
        buddyList.addFileItem(item);
        allFileList.addFileItem(item);
    }
    
    private void initializeBuddys() {
        buddyMap.put("Sean", new FileListAdapter("Sean"));
        initializeBuddy(buddyMap.get("Sean"));
        buddyMap.put("Bob", new FileListAdapter("Bob"));
        buddyMap.put("Johanna", new FileListAdapter("Johanna"));
        buddyMap.put("Mark", new FileListAdapter("Mark"));
        initializeBuddy(buddyMap.get("Mark"));
        buddyMap.put("Rob", new FileListAdapter("Rob"));
        initializeBuddy(buddyMap.get("Rob"));
    }
    
    private void initializeBuddy(FileList fileList) {
        FileListAdapter files = (FileListAdapter) fileList;
        files.addFileItem(allFileList.getModel().get(0));
        files.addFileItem(allFileList.getModel().get(2));
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
    public Map<String, FileList> getUniqueLists() {
        return buddyMap;
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
