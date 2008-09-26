package org.limewire.ui.swing.sharing.menu;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.sharing.friends.FriendItem;
import org.limewire.ui.swing.sharing.table.CategoryFilter;
import org.limewire.ui.swing.util.BackgroundExecutorService;

import ca.odell.glazedlists.FilterList;

public class FriendSharingActionHandler {
    
    private final Navigator navigator;
    private final LibraryManager libraryManager;
    
    public static final String VIEW_LIBRARY = "VIEW_LIBRARY";
    public static final String SHARE_ALL_VIDEO = "SHARE_ALL_VIDEO";
    public static final String SHARE_ALL_AUDIO = "SHARE_ALL_AUDIO";
    public static final String SHARE_ALL_IMAGE = "SHARE_ALL_IMAGE";
    public static final String UNSHARE_ALL = "UNSHARE_ALL";
    
    public FriendSharingActionHandler(Navigator navigator, LibraryManager libraryManager) {
        this.navigator = navigator;
        this.libraryManager = libraryManager;
    }
    
    public void performAction(final String actionCommand, final LocalFileList fileList, final FriendItem item) {
        BackgroundExecutorService.schedule(new Runnable(){
            public void run() {
                if(actionCommand == VIEW_LIBRARY) {
                    NavItem navItem = navigator.getNavItem(NavCategory.LIBRARY, item.getFriend().getId());
                    navItem.select();
                } else if(actionCommand == SHARE_ALL_VIDEO) {
                    FilterList<LocalFileItem> video = new FilterList<LocalFileItem>( libraryManager.getLibraryManagedList().getModel(), new CategoryFilter(Category.VIDEO));
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
                    FilterList<LocalFileItem> audio = new FilterList<LocalFileItem>( libraryManager.getLibraryManagedList().getModel(), new CategoryFilter(Category.AUDIO));
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
                    FilterList<LocalFileItem> image = new FilterList<LocalFileItem>( libraryManager.getLibraryManagedList().getModel(), new CategoryFilter(Category.IMAGE));                 
                    
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
    }
}
