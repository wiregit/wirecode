package org.limewire.ui.swing.library;

import java.io.File;

import javax.swing.SwingUtilities;

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
        showLibrary();
        getComponent().selectLocalFileList(libraryManager.get().getLibraryManagedList());
        getComponent().selectAndScrollTo(file);
        
    }
    
    private void showLibrary(){
        NavItem item = navigatorProvider.get().getNavItem(NavCategory.LIBRARY, NAME);
        item.select();
    }
    
    /**
     * Selects the specified SharedFileList in the library nav and starts editing on its name.
     * @param sharedFileList can not be the public shared list
     */
    public void selectAndRenameSharedList(final SharedFileList sharedFileList) {
        assert(!sharedFileList.isPublic());
        showLibrary();
        //allow library to show before selecting and editing or the list won't display properly
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                getComponent().editSharedListName(sharedFileList);
            }
        });
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

    /**
     * Returns true if the library has been initialized. 
     */
    public boolean isInitialized() {
        return libraryPanel != null;
    }
    
    /**
     * Clears any active filters on the library.
     */
    public void clearFilters() {
        getComponent().clearFilters();
    }
}
