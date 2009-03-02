package org.limewire.ui.swing.library.table.menu.actions;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;

import org.jdesktop.application.Resource;
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
    @Resource
    private Icon friendIcon;
    
    private final ShareWidgetFactory shareWidgetFactory;
    private final SelectAllable<LocalFileItem> librarySelectable;
    private final boolean isShareAll;
    
    public ShareFriendAction(ShareWidgetFactory shareWidgetFactory, SelectAllable<LocalFileItem> librarySelectable, boolean isShareAll) {
        GuiUtils.assignResources(this);
        
        if(isShareAll) {
            putValue(Action.NAME, I18n.tr("Share folder with Friend"));
        } else {
            putValue(Action.NAME, I18n.tr("Share with Friend"));
        }
        putValue(Action.SMALL_ICON, friendIcon);
        
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
