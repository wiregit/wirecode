package org.limewire.ui.swing.library;

import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JComponent;

import org.jdesktop.application.Resource;
import org.limewire.core.api.Category;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.components.LimeHeaderBarFactory;
import org.limewire.ui.swing.dnd.GhostDragGlassPane;
import org.limewire.ui.swing.library.table.LibraryTableFactory;
import org.limewire.ui.swing.util.ButtonDecorator;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

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
                    @Assisted FriendLibraryMediator mediator,
                    CategoryIconManager categoryIconManager, 
                    LibraryTableFactory tableFactory,
                    DownloadListManager downloadListManager,
                    LibraryManager libraryManager,
                    LimeHeaderBarFactory headerBarFactory,
                    ButtonDecorator buttonDecorator,
                    GhostDragGlassPane ghostPane) {
        super(friend, friendFileList, eventList, categoryIconManager, tableFactory, downloadListManager, libraryManager, headerBarFactory, ghostPane);

        GuiUtils.assignResources(this);
        
        this.friend = friend;

        if (selectionPanelBackgroundOverride != null) { 
            getSelectionPanel().setBackground(selectionPanelBackgroundOverride);
        }
        
        //don't show share button for browse hosts
        if(!friend.isAnonymous()) {
            addButtonToHeader(new ViewSharedLibraryAction(mediator), buttonDecorator);
        }
        
        createMyCategories(eventList);
        selectFirst();
        getHeaderPanel().setText(I18n.tr("Download from {0}", getFullPanelName()));
    }
    
    protected JComponent createMyCategoryAction(Category category, EventList<RemoteFileItem> filtered) {
        addFriendInfoBar(category, filtered);
        return super.createMyCategoryAction(category, filtered);
    }
    
    protected String getFullPanelName() {
        return friend.getRenderName();
    }
    
    protected String getShortPanelName() {
        return friend.getFirstName();
    }
    
    @Override
    protected <T extends FileItem> void addCategorySizeListener(Category category,
            Action action, FilterList<T> filteredAllFileList, FilterList<T> filteredList) {
        // Comment this out & return null if you don't want sizes added to library panels.
        ButtonSizeListener<T> listener = new ButtonSizeListener<T>(category, action, filteredList);
        filteredList.addListEventListener(listener);
        addDisposable(listener);
    }
    
    /**
	 * Hide any category that has no files in it.
	 */
    private class ButtonSizeListener<T> implements Disposable, ListEventListener<T>, SettingListener {
        private final Category category;
        private final Action action;
        private final FilterList<T> list;
        
        private ButtonSizeListener(Category category, Action action, FilterList<T> list) {
            this.category = category;
            this.action = action;
            this.list = list;
            action.putValue(Action.NAME, I18n.tr(category.toString()));
            setText();
            if(category == Category.PROGRAM) {
                LibrarySettings.ALLOW_PROGRAMS.addSettingListener(this);
            }
        }

        private void setText() {
            if(category == Category.PROGRAM) { // hide program category is not enabled
                action.setEnabled(LibrarySettings.ALLOW_PROGRAMS.getValue());
            }
            //disable any category if size is 0
            action.setEnabled(list.size() > 0);
        }
        
        @Override
        public void dispose() {
            list.removeListEventListener(this);
            if(category == Category.PROGRAM) {
                LibrarySettings.ALLOW_PROGRAMS.removeSettingListener(this);
            }
        }

        @Override
        public void listChanged(ListEvent<T> listChanges) {
            setText();
        }

        @Override
        public void settingChanged(SettingEvent evt) {
            setText();
        }
    }     
    
    private class ViewSharedLibraryAction extends AbstractAction {
        private final FriendLibraryMediator friendLibraryMediator;

        public ViewSharedLibraryAction(FriendLibraryMediator friendLibraryMediator) {
            this.friendLibraryMediator = friendLibraryMediator;
            putValue(Action.NAME, I18n.tr("Share with {0}", getShortPanelName()));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Share your files with {0}", getShortPanelName()));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            friendLibraryMediator.showSharingCard();
        }
    }
}