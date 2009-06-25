package org.limewire.ui.swing.library;

import java.io.File;

import org.limewire.core.api.URN;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavMediator;
import org.limewire.ui.swing.nav.Navigator;

import com.google.inject.Inject;
import com.google.inject.Provider;

@LazySingleton
public class LibraryMediator implements NavMediator<LibraryPanel> {

    public static final String NAME = "Library";
    
    private Provider<LibraryPanel> libraryProvider;
    private LibraryPanel libraryPanel;
    private final Provider<Navigator> navigatorProvider;
    private final Provider<LibraryManager> libraryManager;
    
    @Inject
    public LibraryMediator(Provider<LibraryPanel> libraryProvider, Provider<Navigator> navigatorProvider, 
            Provider<LibraryManager> libraryManager) {
        this.libraryProvider = libraryProvider;
        this.navigatorProvider = navigatorProvider;
        this.libraryManager = libraryManager;
    }
    
    @Override
    public LibraryPanel getComponent() {
        if(libraryPanel == null) {
            libraryPanel = libraryProvider.get();
        }
        
        return libraryPanel;
    }

    public void selectInLibrary(File file) {
        NavItem item = navigatorProvider.get().getNavItem(NavCategory.LIBRARY, NAME);
        item.select();
        getComponent().selectLocalFileList(libraryManager.get().getLibraryManagedList());
        getComponent().selectAndScrollTo(file);
        
    }

    public void selectInLibrary(URN urn) {
        NavItem item = navigatorProvider.get().getNavItem(NavCategory.LIBRARY, NAME);
        item.select();
        getComponent().selectLocalFileList(libraryManager.get().getLibraryManagedList());
        getComponent().selectAndScrollTo(urn);
    }
    
    public void showSharedFileList(SharedFileList list) {
        NavItem item = navigatorProvider.get().getNavItem(NavCategory.LIBRARY, NAME);
        item.select();
        getComponent().selectLocalFileList(list);
    }
}
