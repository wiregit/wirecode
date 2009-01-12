package org.limewire.ui.swing.library.sharing;

import java.awt.Component;
import java.io.File;

import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.components.ShapeDialog;
import org.limewire.ui.swing.library.sharing.model.FileShareModel;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;

public class FileShareWidget implements ShareWidget<File> {

   private LibrarySharePanel sharePanel;
    
    public FileShareWidget(ShareListManager shareListManager, EventList<SharingTarget> allFriends, ShapeDialog shapeDialog){
        sharePanel = new LibrarySharePanel(allFriends, shapeDialog);
        sharePanel.setShareModel(new FileShareModel(shareListManager));
        sharePanel.setTopLabel(I18n.tr("Currently sharing with"));
        sharePanel.setTitleLabel(I18n.tr("Share one file"));
    }
    
    public void show(Component c) {
        sharePanel.show(c);
    }
    
    public void setShareable(File file){
        ((FileShareModel)sharePanel.getShareModel()).setFile(file);
    }

    @Override
    public void dispose() {
        sharePanel.dispose();
    }

}
