package org.limewire.ui.swing.sharing.menu;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.sharing.actions.GoToLibraryAction;
import org.limewire.ui.swing.sharing.actions.SharingAddAction;
import org.limewire.ui.swing.sharing.friends.FriendItem;
import org.limewire.ui.swing.util.BackgroundExecutorService;

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
                    new GoToLibraryAction(navigator, item.getFriend()).actionPerformed(null);
                } else if(actionCommand == SHARE_ALL_VIDEO) {
                    new SharingAddAction(fileList, libraryManager.getLibraryManagedList(), Category.VIDEO).actionPerformed(null);
                } else if(actionCommand == SHARE_ALL_AUDIO) {
                    new SharingAddAction(fileList, libraryManager.getLibraryManagedList(), Category.AUDIO).actionPerformed(null);
                } else if(actionCommand == SHARE_ALL_IMAGE) {
                    new SharingAddAction(fileList, libraryManager.getLibraryManagedList(), Category.IMAGE).actionPerformed(null);
                } else if(actionCommand == UNSHARE_ALL) {
                    throw new UnsupportedOperationException("TODO: Implement Me");
                }
            }
        });
    }
}
