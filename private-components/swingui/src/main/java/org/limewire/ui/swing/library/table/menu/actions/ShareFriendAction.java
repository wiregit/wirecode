package org.limewire.ui.swing.library.table.menu.actions;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.Action;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.SelectAllable;
import org.limewire.ui.swing.library.sharing.ShareWidget;
import org.limewire.ui.swing.library.sharing.ShareWidgetFactory;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

/**
 * Creates a menu action for sharing files with a friend. This spawns a
 * sharing dialog for choosing multiple friends. A boolean can be set 
 * for sharing all the files in the selectable table.
 */
public class ShareFriendAction extends AbstractAction {

    private final ShareWidgetFactory shareWidgetFactory;
    private final SelectAllable<LocalFileItem> librarySelectable;
    private final boolean isShareAll;
    
    public ShareFriendAction(ShareWidgetFactory shareWidgetFactory, SelectAllable<LocalFileItem> librarySelectable, boolean isShareAll) {
        if(isShareAll) {
            putValue(Action.NAME, I18n.tr("Share all with Friend"));
        } else {
            putValue(Action.NAME, I18n.tr("Share with Friend"));
        }
        
        this.shareWidgetFactory = shareWidgetFactory;
        this.librarySelectable = librarySelectable;
        this.isShareAll = isShareAll;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if(isShareAll) {
            librarySelectable.selectAll();
        }
        List<LocalFileItem> selectedItems = librarySelectable.getSelectedItems();
        
        ShareWidget<LocalFileItem[]> shareWidget = shareWidgetFactory.createMultiFileShareWidget();
        shareWidget.setShareable(selectedItems.toArray(new LocalFileItem[selectedItems.size()]));
        shareWidget.show(GuiUtils.getMainFrame());
    }
}
