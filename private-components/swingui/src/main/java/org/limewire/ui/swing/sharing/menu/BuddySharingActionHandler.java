package org.limewire.ui.swing.sharing.menu;

import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.ui.swing.sharing.friends.BuddyItem;
import org.limewire.ui.swing.sharing.table.CategoryFilter;

import ca.odell.glazedlists.FilterList;

public class BuddySharingActionHandler {
    
    private final LibraryManager libraryManager;
    
    public static final String VIEW_LIBRARY = "VIEW_LIBRARY";
    public static final String SHARE_ALL_VIDEO = "SHARE_ALL_VIDEO";
    public static final String SHARE_ALL_AUDIO = "SHARE_ALL_AUDIO";
    public static final String SHARE_ALL_IMAGE = "SHARE_ALL_IMAGE";
    public static final String UNSHARE_ALL = "UNSHARE_ALL";
    
    public BuddySharingActionHandler(LibraryManager libraryManager) {
        this.libraryManager = libraryManager;
    }
    
    public void performAction(final String actionCommand, final FileList fileList, final BuddyItem item) {
        //TODO: background executor
        Thread t = new Thread(new Runnable(){
            public void run() {
                if(actionCommand == VIEW_LIBRARY) {
                    //TODO: set navigator here
                } else if(actionCommand == SHARE_ALL_VIDEO) {
                    FilterList<FileItem> video = new FilterList<FileItem>( libraryManager.getLibraryList().getModel(), new CategoryFilter(FileItem.Category.VIDEO));
                    for(FileItem fileItem : video) {
                        fileList.addFile(fileItem.getFile());
                    }
                } else if(actionCommand == SHARE_ALL_AUDIO) {
                    FilterList<FileItem> audio = new FilterList<FileItem>( libraryManager.getLibraryList().getModel(), new CategoryFilter(FileItem.Category.AUDIO));
                    for(FileItem fileItem : audio) {
                        fileList.addFile(fileItem.getFile());
                    }
                } else if(actionCommand == SHARE_ALL_IMAGE) {
                    FilterList<FileItem> image = new FilterList<FileItem>( libraryManager.getLibraryList().getModel(), new CategoryFilter(FileItem.Category.IMAGE));
                    for(FileItem fileItem : image) {
                        fileList.addFile(fileItem.getFile());
                    }
                } else if(actionCommand == UNSHARE_ALL) {
                    fileList.clear();
                }
            }
        });
        t.start();
    }
}
