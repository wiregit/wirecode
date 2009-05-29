package org.limewire.ui.swing.library.sharing;

import java.awt.Component;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.ui.swing.components.ShapeDialog;
import org.limewire.ui.swing.friends.login.FriendActions;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.NotImplementedException;

import ca.odell.glazedlists.impl.ThreadSafeList;

public class MultiFileShareWidget implements ShareWidget<LocalFileItem[]>{
    private LibrarySharePanel sharePanel;
//    private LocalFileItem[] files;
//    private SharedFileListManager shareListManager;
    
    public MultiFileShareWidget(SharedFileListManager shareListManager, 
            ThreadSafeList<SharingTarget> allFriends, ShapeDialog shapeDialog, FriendActions friendActions) {
//        this.shareListManager = shareListManager;
        sharePanel = new LibrarySharePanel(allFriends, shapeDialog, friendActions);
    }
    
    public MultiFileShareWidget(SharedFileListManager shareListManager, 
            ThreadSafeList<SharingTarget> allFriends, ShapeDialog shapeDialog, FriendActions friendActions, 
            boolean canShowP2P) {
//        this.shareListManager = shareListManager;
        sharePanel = new LibrarySharePanel(allFriends, shapeDialog, friendActions, canShowP2P);
    }
    
    @Override
    public void show(Component c) {
        throw new NotImplementedException();
//        sharePanel.show(null, new MultiFileShareModel(shareListManager, files));
    }
    
    @Override
    public void setShareable(LocalFileItem[] files){
//        this.files = files;
        // {0} number of files, {1}: type of file (Image, Document...)
        sharePanel.setTitleLabel(I18n.trn("Share {0} {1} file", "Share {0} {1} files", files.length, files.length, files[0].getCategory().getSingularName()));
        // {0}: number of files 
        sharePanel.setTopLabel(I18n.trn("Shared {0} file with:", "Shared {0} files with:", files.length));
    }

    @Override
    public void dispose() {
        sharePanel.dispose();
    }

}
