package org.limewire.ui.swing.library;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.SwingUtilities;

import org.limewire.core.api.URN;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.core.api.library.LibraryManager;
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
        final File firstFile = findFile(file);
        showLibrary();
        clearFilters();
        getComponent().selectLocalFileList(libraryManager.get().getLibraryManagedList());
        //allow library to show before selecting the file, needed in case library
        //hasn't been loaded yet.
        SwingUtilities.invokeLater(new Runnable(){
            public void run() {
                getComponent().selectAndScrollTo(firstFile);     
            }
        });       
    }
    
    /**
     * Returns the given file if it is a file, otherwise if it is a directory, 
     * it will find the first file in its subdirectories, if no file
     * can be found in the subdirectories, the original file is returned.
     */
    private File findFile(File file) {
        File firstFile = file;
        if(firstFile.isDirectory()) {
            List<File> accumulator = new ArrayList<File>(Arrays.asList(firstFile));
            while(accumulator.size() > 0) {
                File folderOrFile = accumulator.remove(0);
                if(folderOrFile.isDirectory()) {
                    File[] files = folderOrFile.listFiles();
                    if(files != null) {
                        //must null check files because it can return null if there was an error accessing 
                        //the files the interface is wrong about returning an empty list for all circumstances
                        accumulator.addAll(Arrays.asList(files));
                    }
                } else {
                    return folderOrFile;
                }
            }
        }
        return firstFile;
    }
    
    private void showLibrary(){
        NavItem item = navigatorProvider.get().getNavItem(NavCategory.LIBRARY, NAME);
        item.select();
    }
    
    public void selectInLibrary(final URN urn) {
        NavItem item = navigatorProvider.get().getNavItem(NavCategory.LIBRARY, NAME);
        item.select();
        getComponent().selectLocalFileList(libraryManager.get().getLibraryManagedList());
        //allow library to show before selecting the file, needed in case library
        //hasn't been loaded yet.
        SwingUtilities.invokeLater(new Runnable(){
            public void run() {
                getComponent().selectAndScrollTo(urn);                
            }
        });
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

    public void locateInLibrary(DownloadItem item) {
        File file;
        if(item.getState() == DownloadState.DONE ||
                item.getState() == DownloadState.SCAN_FAILED) {
            file = item.getLaunchableFile();
        } else {
            file = item.getDownloadingFile();
        }
        URN urn = item.getUrn();
        
        if(file != null) {
            selectInLibrary(file);
        } else if (urn != null){
            selectInLibrary(urn);
        }
    }
}
