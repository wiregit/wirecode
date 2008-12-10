package org.limewire.ui.swing.library.sharing;

import java.awt.Component;
import java.util.Collection;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.library.sharing.model.FileShareModel;
import org.limewire.ui.swing.util.I18n;

public class SingleFileShareWidget implements ShareWidget<LocalFileItem> {

   private LibrarySharePanel sharePanel;
    
    public SingleFileShareWidget(ShareListManager shareListManager, Collection<Friend> allFriends){
        sharePanel = new LibrarySharePanel(allFriends);
        sharePanel.setShareModel(new FileShareModel(shareListManager));
    }
    
    public void show(Component c) {
        sharePanel.show(c);
    }
    
    public void setShareable(LocalFileItem file){
        ((FileShareModel)sharePanel.getShareModel()).setFileItem(file);
        sharePanel.setTopLabel(I18n.tr("Currently sharing with"));
    }

    @Override
    public void dispose() {
        sharePanel.dispose();
    }

}
