package org.limewire.ui.swing.library;

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.library.table.LibraryTableModel;

import ca.odell.glazedlists.EventList;

class FriendLibraryPanelImpl extends JPanel implements FriendLibraryPanel {
   
    public final LibraryTable table;
    
    public FriendLibraryPanelImpl() {
        setLayout(new BorderLayout());

        table = new LibraryTable(); 
        JScrollPane scrollPane = new JScrollPane(table);
        
        add(scrollPane, BorderLayout.CENTER);
    }
    
    @Override
    public JComponent getComponent() {
        return this;
    }
    
    @Override
    public void setCategory(Category category, EventList<RemoteFileItem> remoteFileList) {
        table.setModel(new LibraryTableModel<RemoteFileItem>(remoteFileList));
    }
}
