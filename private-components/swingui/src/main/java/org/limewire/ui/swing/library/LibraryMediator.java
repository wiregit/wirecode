package org.limewire.ui.swing.library;

import java.io.File;

import org.limewire.core.api.Category;
import org.limewire.core.api.URN;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.nav.NavMediator;
import org.limewire.util.NotImplementedException;

import com.google.inject.Inject;
import com.google.inject.Provider;

@LazySingleton
public class LibraryMediator implements NavMediator<LibraryPanel> {

//    private final MyLibraryFactory myLibraryFactory;
//    private final PluggableList<LocalFileItem> baseLibraryList;
//    private final LibraryListSourceChanger currentFriendFilterChanger;
//    private MyLibraryPanel panel;
    private Provider<LibraryPanel> libraryProvider;
    private LibraryPanel libraryPanel;
    
    @Inject
    public LibraryMediator(Provider<LibraryPanel> libraryProvider, LibraryManager libraryManager) {
        this.libraryProvider = libraryProvider;
        
//        baseLibraryList = new PluggableList<LocalFileItem>(libraryManager.getLibraryListEventPublisher(), libraryManager.getReadWriteLock());
//        currentFriendFilterChanger = new LibraryListSourceChanger(baseLibraryList, libraryManager);
    }
    
//    public Friend getCurrentFriend() {
//        return currentFriendFilterChanger.getCurrentFriend();
//    }
    
    public void showSharingState(Friend friend) {
        throw new NotImplementedException("not implemented");
//        getComponent().showSharingState(friend);
    }
    
    public void showAllFiles() {
        throw new NotImplementedException("not implemented");
//        getComponent().showAllFiles();
    }
    
    public void selectItem(URN urn, Object catalog) {
        throw new NotImplementedException("not implemented");
//        getComponent().selectItem(urn, catalog);
    }
    
    public void selectItem(File file, Category category) {
        throw new NotImplementedException("not implemented");
//        getComponent().selectItem(file, new Catalog(category));
    }
    
    public File getPreviousItem(File file, Object catalog) {
        throw new NotImplementedException("not implemented");
//        return null;//getComponent().getPreviousItem(file, catalog);
    }
    
    public File getNextItem(File file, Object catalog) {
        throw new NotImplementedException("not implemented");
//        return null;//getComponent().getNextItem(file, catalog);
    }
    
    @Override
    public LibraryPanel getComponent() {
        if(libraryPanel == null) {
            libraryPanel = libraryProvider.get();
//            libraryPanel.registerListeners();
        }
//        if(panel == null)
//            panel = myLibraryFactory.createMyLibraryPanel(baseLibraryList, currentFriendFilterChanger);
        
        return libraryPanel;
//        return panel;
    }
//    
//    public void addFriendListener(ListSourceChanger.ListChangedListener listener) {
////        currentFriendFilterChanger.addListener(listener);
//        throw new NotImplementedException("not implemented");
//    }

    public void selectInLibrary(File file, Category category) {
        // TODO Auto-generated method stub
        throw new NotImplementedException("not implemented");
        
    }

    public void selectInLibrary(URN urn, Category category) {
        // TODO Auto-generated method stub
        throw new NotImplementedException("not implemented");
    }

    public void selectLibrary() {
        // TODO Auto-generated method stub
        throw new NotImplementedException("not implemented");
    }

    public void setActiveCatalog(Object catalog) {
        // TODO Auto-generated method stub
        throw new NotImplementedException("not implemented");
    }

    public File getPreviousInLibrary(File file) {
        // TODO Auto-generated method stub
//        return null;
        throw new NotImplementedException("not implemented");
    }

    public File getNextInLibrary(File file) {
        // TODO Auto-generated method stub
//        return null;
        throw new NotImplementedException("not implemented");
    }
}
