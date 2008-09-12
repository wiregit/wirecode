package org.limewire.ui.swing.sharing.menu;

import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavigableTree;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.sharing.friends.BuddyItem;
import org.limewire.ui.swing.sharing.table.CategoryFilter;

import ca.odell.glazedlists.FilterList;

public class BuddySharingActionHandler {
    
    private final NavigableTree navTree;
    private final LibraryManager libraryManager;
    
    public static final String VIEW_LIBRARY = "VIEW_LIBRARY";
    public static final String SHARE_ALL_VIDEO = "SHARE_ALL_VIDEO";
    public static final String SHARE_ALL_AUDIO = "SHARE_ALL_AUDIO";
    public static final String SHARE_ALL_IMAGE = "SHARE_ALL_IMAGE";
    public static final String UNSHARE_ALL = "UNSHARE_ALL";
    
    public BuddySharingActionHandler(NavigableTree tree, LibraryManager libraryManager) {
        this.navTree = tree;
        this.libraryManager = libraryManager;
    }
    
    public void performAction(final String actionCommand, final LocalFileList fileList, final BuddyItem item) {
        //TODO: background executor
        Thread t = new Thread(new Runnable(){
            public void run() {
                if(actionCommand == VIEW_LIBRARY) {
                    NavItem navItem = navTree.getNavigableItemByName(Navigator.NavCategory.LIBRARY, item.getName());
                    navItem.select();
                } else if(actionCommand == SHARE_ALL_VIDEO) {
                    FilterList<LocalFileItem> video = new FilterList<LocalFileItem>( libraryManager.getLibraryList().getModel(), new CategoryFilter(FileItem.Category.VIDEO));
                    try {
                        video.getReadWriteLock().readLock().lock();
                        for(LocalFileItem fileItem : video) {
                            fileList.addFile(fileItem.getFile());
                        }
                    } finally {
                        video.getReadWriteLock().readLock().unlock();
                    }
                    video.dispose();
                } else if(actionCommand == SHARE_ALL_AUDIO) {
                    FilterList<LocalFileItem> audio = new FilterList<LocalFileItem>( libraryManager.getLibraryList().getModel(), new CategoryFilter(FileItem.Category.AUDIO));
                    try {
                        audio.getReadWriteLock().readLock().lock();
                        for(LocalFileItem fileItem : audio) {
                            fileList.addFile(fileItem.getFile());
                        }
                    } finally {
                        audio.getReadWriteLock().readLock().unlock();
                    }
                    audio.dispose();
                } else if(actionCommand == SHARE_ALL_IMAGE) {
                    FilterList<LocalFileItem> image = new FilterList<LocalFileItem>( libraryManager.getLibraryList().getModel(), new CategoryFilter(FileItem.Category.IMAGE));                 
                    
                    try {
                        image.getReadWriteLock().readLock().lock();
                        for(LocalFileItem fileItem : image) {
                            fileList.addFile(fileItem.getFile());
                        }
                    } finally {
                        image.getReadWriteLock().readLock().unlock();
                    }
                    image.dispose();
                } else if(actionCommand == UNSHARE_ALL) {
//                    fileList.clear();
                    //TODO: implement this correctly;
                }
            }
        });
        t.start();
    }
}
