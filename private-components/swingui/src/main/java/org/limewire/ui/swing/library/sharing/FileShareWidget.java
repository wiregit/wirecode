package org.limewire.ui.swing.library.sharing;

import java.awt.Component;
import java.io.File;

import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.ui.swing.components.ShapeDialog;
import org.limewire.ui.swing.friends.login.FriendActions;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.NotImplementedException;

import ca.odell.glazedlists.impl.ThreadSafeList;

public class FileShareWidget implements ShareWidget<File> {

    private LibrarySharePanel sharePanel;
//    private File file;
//    private SharedFileListManager shareListManager;
    private ThreadSafeList<SharingTarget> allFriends;
    private ShapeDialog shapeDialog;
    private FriendActions friendActions;

    public FileShareWidget(SharedFileListManager shareListManager, ThreadSafeList<SharingTarget> allFriends, ShapeDialog shapeDialog, FriendActions friendActions) {
//        this.shareListManager = shareListManager;
        this.allFriends = allFriends;
        this.shapeDialog = shapeDialog;
        this.friendActions = friendActions;
    }

    public void show(Component c) {
        if(sharePanel == null){
            initSharePanel();
        }
        throw new NotImplementedException();
//        sharePanel.show(c, new FileShareModel(shareListManager, file));
    }

    public void setShareable(File file) {
//        this.file = file;
    }

    @Override
    public void dispose() {
        sharePanel.dispose();
    }
    
    private void initSharePanel(){
        sharePanel = new LibrarySharePanel(allFriends, shapeDialog, friendActions);
        sharePanel.setTopLabel(I18n.tr("Currently sharing with"));
        sharePanel.setTitleLabel(I18n.tr("Share one file"));
    }

}
