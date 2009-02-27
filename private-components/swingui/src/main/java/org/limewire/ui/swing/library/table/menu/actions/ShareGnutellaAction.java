package org.limewire.ui.swing.library.table.menu.actions;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.Action;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.SelectAllable;
import org.limewire.ui.swing.library.sharing.SharingTarget;
import org.limewire.ui.swing.library.sharing.model.MultiFileShareModel;
import org.limewire.ui.swing.util.I18n;

/**
 * Creates a menu action for sharing files with gnutella. This runs the
 * process in the background and does not spawn any other dialog. A 
 * boolean can be set for sharing all the files in the selectable table.
 */
public class ShareGnutellaAction extends AbstractAction {

    private final ShareListManager shareListManager;
    private final SelectAllable<LocalFileItem> librarySelectable;
    private final boolean isShareAll;
    
    public ShareGnutellaAction(ShareListManager shareListManager, SelectAllable<LocalFileItem> librarySelectable, boolean isShareAll) {
        if(isShareAll) {
            putValue(Action.NAME, I18n.tr("Share all with the P2P Network"));
        } else {
            putValue(Action.NAME, I18n.tr("Share with the P2P Network"));
        }
        
        this.shareListManager = shareListManager;
        this.librarySelectable = librarySelectable;
        this.isShareAll = isShareAll;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if(isShareAll) {
            librarySelectable.selectAll();
        }
        List<LocalFileItem> selectedItems = librarySelectable.getSelectedItems();
        
        MultiFileShareModel model = new MultiFileShareModel(shareListManager, selectedItems.toArray(new LocalFileItem[selectedItems.size()]));
        model.shareFriend(SharingTarget.GNUTELLA_SHARE);
    }
}
