package org.limewire.ui.swing.library.sharing;

import java.awt.Component;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.components.ShapeDialog;
import org.limewire.ui.swing.library.sharing.model.MultiFileUnshareModel;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;

public class MultiFileUnshareWidget implements ShareWidget<LocalFileItem[]>{
    private LibrarySharePanel unsharePanel;
    
    public MultiFileUnshareWidget(ShareListManager shareListManager, EventList<SharingTarget> allFriends, ShapeDialog shapeDialog){
        unsharePanel = new LibrarySharePanel(allFriends, shapeDialog);
        unsharePanel.setShareModel(new MultiFileUnshareModel(shareListManager));
        unsharePanel.setComboBoxVisible(false);
    }
    
    public void show(Component c) {
        unsharePanel.show(null);
    }
    

    @Override
    public void dispose() {
        unsharePanel.dispose();
    }

    @Override
    public void setShareable(LocalFileItem[] files) {
        ((MultiFileUnshareModel)unsharePanel.getShareModel()).setFileItem(files);
        unsharePanel.setTitleLabel(I18n.tr("Unshare {0} files", files.length));    
        unsharePanel.setTopLabel("with the following people");    
    }
}
