package org.limewire.core.impl.library;

import java.util.HashMap;
import java.util.Map;

import org.limewire.core.api.library.BuddyShareListListener;
import org.limewire.core.api.library.FileItem.Category;
import org.limewire.core.api.library.FileList;
import org.limewire.core.api.library.LibraryListListener;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.RemoteFileList;


public class MockLibraryManager implements LibraryManager {

    private FileListAdapter allFileList;
    private FileListAdapter gnutellaList;
    private FileListAdapter buddyList;
    
    private Map<String, LocalFileList> buddyMap;
    
    public MockLibraryManager() {
        allFileList = new FileListAdapter("My Library");
        gnutellaList = new FileListAdapter("Gnutella List");
        buddyList = new FileListAdapter("Buddy List");
        
        buddyMap = new HashMap<String, LocalFileList>();

        initializeMockGnutellaData();
        initializeMockBuddyData();
        initializeBuddys();
    }
    
    private void initializeMockGnutellaData(){
        MockLocalFileItem item = new MockLocalFileItem("Small Town Hero.mp3", 1000,12345,23456, 5,1, Category.AUDIO);
        gnutellaList.addFileItem(item);
        allFileList.addFileItem(item);
        
        item = new MockLocalFileItem("LimeWireStore.html", 12345,12312,534512,3,2, Category.AUDIO);
        gnutellaList.addFileItem(item);
        allFileList.addFileItem(item);
        
        item = new MockLocalFileItem("Pictures.png", 32423, 3415412, 123123,0,0, Category.DOCUMENT);
        gnutellaList.addFileItem(item);
        allFileList.addFileItem(item);
        
        Thread t = new Thread(new Runnable(){
            public void run(){
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    return;
                }
                MockLocalFileItem item = new MockLocalFileItem("Lazy load.png", 32423, 3415412, 123123,0,0, Category.DOCUMENT);
                gnutellaList.addFileItem(item);
                allFileList.addFileItem(item);
            }
        });
        t.start();
    }
    
    private void initializeMockBuddyData(){
        MockLocalFileItem item = new MockLocalFileItem("Small Town Hero.mp3", 1000,12345,23456,1,1, Category.IMAGE);
        buddyList.addFileItem(item);
        allFileList.addFileItem(item);
        
        item = new MockLocalFileItem("LimeWireStore.html", 12345,12312,534512,5,0, Category.OTHER);
        buddyList.addFileItem(item);
        allFileList.addFileItem(item);
        
        item = new MockLocalFileItem("Pictures.png", 32423, 3415412, 123123,0,0, Category.OTHER);
        buddyList.addFileItem(item);
        allFileList.addFileItem(item);
    }
    
    private void initializeBuddys() {
        buddyMap.put("Sean", new FileListAdapter("Sean"));
        initializeBuddy4(buddyMap.get("Sean"));
        buddyMap.put("Bob", new FileListAdapter("Bob"));
        buddyMap.put("Johanna", new FileListAdapter("Johanna"));
        buddyMap.put("Mark", new FileListAdapter("Mark"));
        initializeBuddy(buddyMap.get("Mark"));
        buddyMap.put("Rob", new FileListAdapter("Rob"));
        initializeBuddy2(buddyMap.get("Rob"));
    }
    
    private void initializeBuddy(FileList fileList) {
        FileListAdapter files = (FileListAdapter) fileList;
        files.addFileItem(allFileList.getModel().get(0));
    }
    
    private void initializeBuddy2(FileList fileList) {
        FileListAdapter files = (FileListAdapter) fileList;
        files.addFileItem(allFileList.getModel().get(0));
        files.addFileItem(allFileList.getModel().get(2));
    }
    
    private void initializeBuddy4(FileList fileList) {
        FileListAdapter files = (FileListAdapter) fileList;
        files.addFileItem(allFileList.getModel().get(0));
        files.addFileItem(allFileList.getModel().get(1));
        files.addFileItem(allFileList.getModel().get(2));
        files.addFileItem(allFileList.getModel().get(3));
    }
    
    
    @Override
    public void addLibraryLisListener(LibraryListListener libraryListener) {
        // TODO Auto-generated method stub
        
    }
    

    @Override
    public LocalFileList getLibraryList() {
        return allFileList;
    }
    
//    @Override
//    public FileList getAllBuddyList() {
//        return buddyList;
//    }
    
    @Override
    public LocalFileList getGnutellaList() {
        return gnutellaList;
    }

    @Override
    public Map<String, LocalFileList> getAllBuddyLists() {
        return buddyMap;
    }
    
    @Override
    public LocalFileList getBuddy(String name) {
        return new FileListAdapter(name);
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
    public boolean containsBuddy(String name) {
        return false;
    }

    @Override
    public void addBuddyShareListListener(BuddyShareListListener listener) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removeBuddyShareListListener(BuddyShareListListener listener) {
        // TODO Auto-generated method stub
        
    }
    
    ///////////////////////////////////////////////
    //  Accessors for Buddy Libraries
    ///////////////////////////////////////////////

    @Override
    public void addBuddyLibrary(String name) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean containsBuddyLibrary(String name) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Map<String, RemoteFileList> getAllBuddyLibraries() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RemoteFileList getBuddyLibrary(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void removeBuddyLibrary(String name) {
        // TODO Auto-generated method stub
        
    }
}
