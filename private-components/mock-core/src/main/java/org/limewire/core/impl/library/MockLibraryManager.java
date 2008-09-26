package org.limewire.core.impl.library;

import java.util.HashMap;
import java.util.Map;

import org.limewire.core.api.Category;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FileList;
import org.limewire.core.api.library.LibraryListListener;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.RemoteFileList;


public class MockLibraryManager implements LibraryManager {

    private FileListAdapter allFileList;
    private FileListAdapter gnutellaList;
    private FileListAdapter friendList;
    
    private Map<String, LocalFileList> friendMap;
    
    public MockLibraryManager() {
        allFileList = new FileListAdapter();
        gnutellaList = new FileListAdapter();
        friendList = new FileListAdapter();
        
        friendMap = new HashMap<String, LocalFileList>();

        initializeMockGnutellaData();
        initializeMockFriendData();
        initializeFriends();
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
    
    private void initializeMockFriendData(){
        MockLocalFileItem item = new MockLocalFileItem("Small Town Hero.mp3", 1000,12345,23456,1,1, Category.IMAGE);
        friendList.addFileItem(item);
        allFileList.addFileItem(item);
        
        item = new MockLocalFileItem("LimeWireStore.html", 12345,12312,534512,5,0, Category.OTHER);
        friendList.addFileItem(item);
        allFileList.addFileItem(item);
        
        item = new MockLocalFileItem("Pictures.png", 32423, 3415412, 123123,0,0, Category.OTHER);
        friendList.addFileItem(item);
        allFileList.addFileItem(item);
    }
    
    private void initializeFriends() {
        friendMap.put("Sean", new FileListAdapter());
        initializeFriend4(friendMap.get("Sean"));
        friendMap.put("Bob", new FileListAdapter());
        friendMap.put("Johanna", new FileListAdapter());
        friendMap.put("Mark", new FileListAdapter());
        initializeFriend(friendMap.get("Mark"));
        friendMap.put("Rob", new FileListAdapter());
        initializeFriend2(friendMap.get("Rob"));
    }
    
    private void initializeFriend(FileList fileList) {
        FileListAdapter files = (FileListAdapter) fileList;
        files.addFileItem(allFileList.getModel().get(0));
    }
    
    private void initializeFriend2(FileList fileList) {
        FileListAdapter files = (FileListAdapter) fileList;
        files.addFileItem(allFileList.getModel().get(0));
        files.addFileItem(allFileList.getModel().get(2));
    }
    
    private void initializeFriend4(FileList fileList) {
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
    
    
    @Override
    public LocalFileList getGnutellaList() {
        return gnutellaList;
    }

    @Override
    public Map<String, LocalFileList> getAllFriendLists() {
        return friendMap;
    }
    
    @Override
    public LocalFileList getFriend(String name) {
        return new FileListAdapter();
    }

    @Override
    public void removeLibraryListener(LibraryListListener libraryListener) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addFriend(String name) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removeFriend(String name) {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public boolean containsFriend(String name) {
        return false;
    }

    
    ///////////////////////////////////////////////
    //  Accessors for Friend Libraries
    ///////////////////////////////////////////////

    @Override
    public RemoteFileList getOrCreateFriendLibrary(Friend id) {
        return null;
    }

    @Override
    public void removeFriendLibrary(Friend id) {
    }
}
