package org.limewire.ui.swing.library;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.LimeHeaderBarFactory;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.library.table.LibraryTableFactory;
import org.limewire.ui.swing.lists.CategoryFilter;
import org.limewire.ui.swing.util.ButtonDecorator;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class FriendLibraryPanel extends LibraryPanel {

    private final CategoryIconManager categoryIconManager;
    private final LibraryTableFactory tableFactory;
    private final DownloadListManager downloadListManager;
    private final LibraryManager libraryManager;
    private final FriendLibraryMediator mediator;
    private final Friend friend;
    
    @AssistedInject
    public FriendLibraryPanel(@Assisted Friend friend,
                    @Assisted EventList<RemoteFileItem> eventList, 
                    @Assisted FriendLibraryMediator mediator,
                    CategoryIconManager categoryIconManager, 
                    LibraryTableFactory tableFactory,
                    DownloadListManager downloadListManager,
                    LibraryManager libraryManager,
                    LimeHeaderBarFactory headerBarFactory,
                    ButtonDecorator buttonDecorator) {
        
        super(headerBarFactory);
        
        this.friend = friend;
        this.categoryIconManager = categoryIconManager;
        this.tableFactory = tableFactory;
        this.downloadListManager = downloadListManager;
        this.libraryManager = libraryManager;
        this.mediator = mediator;        

        //don't show share button for browse hosts
        if(!friend.isAnonymous()) {
            addButtonToHeader(new ViewSharedLibraryAction(), buttonDecorator);
        }
        createMyCategories(eventList, friend);   
        
        selectFirst();
        
        getHeaderPanel().setText(I18n.tr("Download from {0}", getFullPanelName()));
    }
    
    protected String getFullPanelName() {
        return friend.getRenderName();
    }
    
    protected String getShortPanelName() {
        return friend.getFirstName();
    }    
    
    private void createMyCategories(EventList<RemoteFileItem> eventList, Friend friend) {
        for(Category category : Category.getCategoriesInOrder()) {
            FilterList<RemoteFileItem> filtered = GlazedListsFactory.filterList(eventList, new CategoryFilter(category));
            JComponent component = createMyCategoryAction(category, filtered, friend);
            if(component != null) {
                addCategory(categoryIconManager.getIcon(category), category, component, filtered, null);                
            }
            addDisposable(filtered);
        }
    }
    
    private JComponent createMyCategoryAction(Category category, EventList<RemoteFileItem> filtered, Friend friend) {
        FilterList<RemoteFileItem> filterList = GlazedListsFactory.filterList(filtered, 
                new TextComponentMatcherEditor<RemoteFileItem>(getFilterTextField(), new LibraryTextFilterator<RemoteFileItem>()));
        addDisposable(filterList);
        
        LibraryTable table = tableFactory.createFriendTable(category, filterList, friend);
        table.enableDownloading(downloadListManager, libraryManager.getLibraryManagedList());
        addDisposable(table);
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setColumnHeaderView(table.getTableHeader());
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        if (table.isColumnControlVisible()) {
            scrollPane.setCorner(JScrollPane.UPPER_TRAILING_CORNER, table.getColumnControl());
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        }
        
        return scrollPane;
    }
    
    private class ViewSharedLibraryAction extends AbstractAction {

        public ViewSharedLibraryAction() {
            putValue(Action.NAME, I18n.tr("Share with {0}", getShortPanelName()));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Show files you're sharing with {0}", getShortPanelName()));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            mediator.showSharingCard();
        }
    }
}