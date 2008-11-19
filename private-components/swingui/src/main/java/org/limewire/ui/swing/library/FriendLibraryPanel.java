package org.limewire.ui.swing.library;

import java.awt.event.ActionEvent;
import java.util.LinkedHashMap;
import java.util.Map;

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
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class FriendLibraryPanel extends LibraryPanel {

    private CategoryIconManager categoryIconManager;
    private LibraryTableFactory tableFactory;
    private DownloadListManager downloadListManager;
    private LibraryManager libraryManager;
    private FriendLibraryMediator mediator;
    
    @AssistedInject
    public FriendLibraryPanel(@Assisted Friend friend,
                    @Assisted EventList<RemoteFileItem> eventList, 
                    @Assisted FriendLibraryMediator mediator,
                    CategoryIconManager categoryIconManager, 
                    LibraryTableFactory tableFactory,
                    DownloadListManager downloadListManager,
                    LibraryManager libraryManager,
                    LimeHeaderBarFactory headerBarFactory){
        
        super(friend, true, headerBarFactory);
        
        this.categoryIconManager = categoryIconManager;
        this.tableFactory = tableFactory;
        this.downloadListManager = downloadListManager;
        this.libraryManager = libraryManager;
        this.mediator = mediator;

        loadHeader();
        loadSelectionPanel();
        createMyCategories(eventList, friend);   
        
        selectFirst();
    }

    @Override
    public void loadHeader() {
        //don't show share button for browse hosts
        if(!friend.isAnonymous())
            headerPanel.enableButton(new ViewSharedLibraryAction());
    }
    
    @Override
    public void loadSelectionPanel() {
    }
    
    private Map<Category, JComponent> createMyCategories(EventList<RemoteFileItem> eventList, Friend friend) {       
        Map<Category, JComponent> categories = new LinkedHashMap<Category, JComponent>();
        for(Category category : Category.getCategoriesInOrder()) {
            FilterList<RemoteFileItem> filtered = GlazedListsFactory.filterList(eventList, new CategoryFilter(category));
            JComponent component = createMyCategoryAction(category, filtered, friend);
            if(component != null) {
                createButton(categoryIconManager.getIcon(category), category, component, filtered);                
            }
        }
        return categories;
    }
    
    private JComponent createMyCategoryAction(Category category, EventList<RemoteFileItem> filtered, Friend friend) {
        EventList<RemoteFileItem> filterList = GlazedListsFactory.filterList(filtered, 
                new TextComponentMatcherEditor<RemoteFileItem>(getFilterTextField(), new LibraryTextFilterator<RemoteFileItem>()));

        LibraryTable table = tableFactory.createTable(category, filterList, friend);
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
            putValue(Action.NAME, I18n.tr("Share with {0}", friend.getRenderName()));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Show files you're sharing with {0}", friend.getRenderName()));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            mediator.showSharingCard();
        }
    }
}