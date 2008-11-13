package org.limewire.ui.swing.library;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.library.table.LibraryTableFactory;
import org.limewire.ui.swing.lists.CategoryFilter;
import org.limewire.ui.swing.util.CategoryIconManager;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class FriendLibraryPanel extends LibraryPanel {

    private CategoryIconManager categoryIconManager;
    private LibraryTableFactory tableFactory;
    private DownloadListManager downloadListManager;
    
    @AssistedInject
    public FriendLibraryPanel(@Assisted Friend friend,
                    @Assisted EventList<RemoteFileItem> eventList, 
                    CategoryIconManager categoryIconManager, 
                    LibraryTableFactory tableFactory,
                    DownloadListManager downloadListManager) {
        super(friend);
        
        this.categoryIconManager = categoryIconManager;
        this.tableFactory = tableFactory;

        loadHeader();
        loadSelectionPanel();
        createMyCategories(eventList, friend);   
    }

    @Override
    public void loadHeader() {
    }

    @Override
    public void loadSelectionPanel() {
    }
    
    private Map<Category, JComponent> createMyCategories(EventList<RemoteFileItem> eventList, Friend friend) {       
        Map<Category, JComponent> categories = new LinkedHashMap<Category, JComponent>();
        for(Category category : Category.getCategoriesInOrder()) {
            JComponent component = createMyCategoryAction(category, eventList, friend);
            if(component != null) {
                createButton(categoryIconManager.getIcon(category), category, component);                
            }
        }
        return categories;
    }
    
    private JComponent createMyCategoryAction(Category category, EventList<RemoteFileItem> eventList, Friend friend) {
        FilterList<RemoteFileItem> filtered = GlazedListsFactory.filterList(eventList, new CategoryFilter(category));
        
        //don't bother creating anything if there's nothing to display
        if(filtered.size() == 0) {
            filtered.dispose();
            return null;
        }
        EventList<RemoteFileItem> filterList = GlazedListsFactory.filterList(filtered, 
                new TextComponentMatcherEditor<RemoteFileItem>(getFilterTextField(), new LibraryTextFilterator<RemoteFileItem>()));

        LibraryTable table = tableFactory.createTable(category, filterList, friend);
        table.enableDownloading(downloadListManager);
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
}