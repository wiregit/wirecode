package org.limewire.ui.swing.library;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.limewire.core.api.Category;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.library.table.AudioTableFormat;
import org.limewire.ui.swing.library.table.LibraryTable;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.EventTableModel;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

class FriendLibraryPanel extends JPanel implements Disposable {
   
    public final LibraryTable table;
    
    @AssistedInject
    public FriendLibraryPanel(@Assisted Friend friend,
                              @Assisted Category category,
                              @Assisted EventList<RemoteFileItem> eventList) {
        setLayout(new BorderLayout());
        //FIXME - need the right table format!

        table = new LibraryTable<RemoteFileItem>(eventList, new AudioTableFormat<RemoteFileItem>()); 
        JScrollPane scrollPane = new JScrollPane(table);
        
        add(scrollPane, BorderLayout.CENTER);
    }
    
    public void dispose() {
        ((EventTableModel)table.getModel()).dispose();
    }
    
}
