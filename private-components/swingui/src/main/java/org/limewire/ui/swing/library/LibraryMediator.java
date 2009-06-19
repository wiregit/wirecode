package org.limewire.ui.swing.library;

import java.io.File;

import org.limewire.core.api.Category;
import org.limewire.core.api.URN;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavMediator;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.util.NotImplementedException;

import com.google.inject.Inject;
import com.google.inject.Provider;

@LazySingleton
public class LibraryMediator implements NavMediator<LibraryPanel> {

    public static final String NAME = "Library";
    
//    private final MyLibraryFactory myLibraryFactory;
//    private final PluggableList<LocalFileItem> baseLibraryList;
//    private final LibraryListSourceChanger currentFriendFilterChanger;
//    private MyLibraryPanel panel;
    private Provider<LibraryPanel> libraryProvider;
    private LibraryPanel libraryPanel;
    private final Provider<Navigator> navigatorProvider;
    
    @Inject
    public LibraryMediator(Provider<LibraryPanel> libraryProvider, LibraryManager libraryManager, 
            Provider<Navigator> navigatorProvider) {
        this.libraryProvider = libraryProvider;
        this.navigatorProvider = navigatorProvider;
        
//        baseLibraryList = new PluggableList<LocalFileItem>(libraryManager.getLibraryListEventPublisher(), libraryManager.getReadWriteLock());
//        currentFriendFilterChanger = new LibraryListSourceChanger(baseLibraryList, libraryManager);
    }
    
    @Override
    public LibraryPanel getComponent() {
        if(libraryPanel == null) {
            libraryPanel = libraryProvider.get();
        }
        
        return libraryPanel;
    }

    public void selectInLibrary(File file, Category category) {
        // TODO Auto-generated method stub
        throw new NotImplementedException("not implemented");
        
    }

    public void selectInLibrary(URN urn, Category category) {
        // TODO Auto-generated method stub
        throw new NotImplementedException("not implemented");
    }
    
    public void showSharedFileList(SharedFileList list) {
        NavItem item = navigatorProvider.get().getNavItem(NavCategory.LIBRARY, NAME);
        item.select();
        getComponent().selectSharedFileList(list);
    }
}
