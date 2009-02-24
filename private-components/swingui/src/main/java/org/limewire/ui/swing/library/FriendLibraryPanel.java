package org.limewire.ui.swing.library;

import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.Action;

import org.jdesktop.application.Resource;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.decorators.ButtonDecorator;
import org.limewire.ui.swing.components.decorators.HeaderBarDecorator;
import org.limewire.ui.swing.components.decorators.TextFieldDecorator;
import org.limewire.ui.swing.dnd.GhostDragGlassPane;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.library.table.LibraryTableFactory;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class FriendLibraryPanel extends AbstractFriendLibraryPanel {

    @Resource(key="LibraryPanel.selectionPanelBackgroundOverride") 
    private Color selectionPanelBackgroundOverride = null;    
    
    private final Friend friend;
    
    @AssistedInject
    public FriendLibraryPanel(@Assisted Friend friend,
                    @Assisted FriendFileList friendFileList,
                    @Assisted EventList<RemoteFileItem> eventList, 
                    CategoryIconManager categoryIconManager, 
                    LibraryTableFactory tableFactory,
                    DownloadListManager downloadListManager,
                    LibraryManager libraryManager,
                    HeaderBarDecorator headerBarFactory,
                    ButtonDecorator buttonDecorator,
                    GhostDragGlassPane ghostPane,
                    LibraryNavigator libraryNavigator,
                    TextFieldDecorator textFieldDecorator) {
        
        super(friend, friendFileList, categoryIconManager, tableFactory, downloadListManager, 
                libraryManager, headerBarFactory, ghostPane, libraryNavigator,
                textFieldDecorator);

        GuiUtils.assignResources(this);
        
        this.friend = friend;

        if (selectionPanelBackgroundOverride != null) { 
            getSelectionPanel().setBackground(selectionPanelBackgroundOverride);
        }
        
        //don't show share button for browse hosts
        if(!friend.isAnonymous()) {
            addButtonToHeader(new ViewSharedLibraryAction(libraryNavigator), buttonDecorator);
        }
        
        createMyCategories(eventList);
        selectFirstVisible();
        getHeaderPanel().setText(I18n.tr("Download from {0}", getFullPanelName()));
    }
    
    protected String getFullPanelName() {
        return friend.getRenderName();
    }
    
    protected String getShortPanelName() {
        return friend.getFirstName();
    } 
    
    /**
	 * Opens My Library and applies a filter on this friend.
	 */
    private class ViewSharedLibraryAction extends AbstractAction {
        private final LibraryNavigator libraryNavigator;
        
        public ViewSharedLibraryAction(LibraryNavigator libraryNavigator){
            this.libraryNavigator = libraryNavigator;
            putValue(Action.NAME, I18n.tr("Share"));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Share your files with {0}", friend.getRenderName()));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            libraryNavigator.selectFriendShareList(friend);
        }
    }
}