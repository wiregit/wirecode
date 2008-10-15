package org.limewire.ui.swing.library;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.library.table.LibraryTableFactory;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.EventTableModel;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

class FriendLibraryPanel extends JPanel implements Disposable {
   
    public final LibraryTable table;
    
    @AssistedInject
    public FriendLibraryPanel(@Assisted Friend friend,
                              @Assisted Category category,
                              @Assisted EventList<RemoteFileItem> eventList,
                              DownloadListManager downloadListManager,
                              LibraryTableFactory tableFactory) {
        setLayout(new BorderLayout());

        LibraryHeaderPanel header = new LibraryHeaderPanel(category, friend);
        
        EventList<RemoteFileItem> filterList = GlazedListsFactory.filterList(eventList, 
                new TextComponentMatcherEditor<RemoteFileItem>(header.getFilterTextField(), new LibraryTextFilterator<RemoteFileItem>()));
        
        table = tableFactory.createTable(category, filterList, friend); 
        table.enableDownloading(downloadListManager);
        JScrollPane scrollPane = new JScrollPane(table);
        
        add(scrollPane, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);
    }
    
    public void dispose() {
        table.dispose();
        ((EventTableModel)table.getModel()).dispose();
    }
}
