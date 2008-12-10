package org.limewire.ui.swing.library.sharing;

import java.awt.Component;
import java.util.Collection;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.library.sharing.model.MultiFileUnshareModel;
import org.limewire.ui.swing.util.I18n;

public class MultiFileUnshareWidget implements ShareWidget<LocalFileItem[]>{
    private LibrarySharePanel unsharePanel;
    
    public MultiFileUnshareWidget(ShareListManager shareListManager, Collection<Friend> allFriends){
        unsharePanel = new LibrarySharePanel(allFriends);
        unsharePanel.setShareModel(new MultiFileUnshareModel(shareListManager));
        unsharePanel.setComboBoxVisible(false);
    }
    
    public void show(Component c) {
        unsharePanel.show(c);
    }
    

    @Override
    public void dispose() {
        unsharePanel.dispose();
    }

    @Override
    public void setShareable(LocalFileItem[] files) {
        ((MultiFileUnshareModel)unsharePanel.getShareModel()).setFileItem(files);
        unsharePanel.setTitleLabel(I18n.tr("Unshare {0} {1} files", files.length, files[0].getCategory().toString()));    
    }
}
