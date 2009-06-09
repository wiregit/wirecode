package org.limewire.ui.swing.library.sharing;

import java.awt.Color;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXTable;
import org.limewire.core.api.friend.Friend;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.EventTableModel;

import com.google.inject.Inject;
import com.limegroup.gnutella.MockFriend;

public class LibrarySharingTable extends JXTable {

    @Resource private Color backgroundColor;
    
    private EventList<Friend> eventList;
    
    @Inject
    public LibrarySharingTable() {
        GuiUtils.assignResources(this);
        
        eventList = new BasicEventList<Friend>();
        
        setModel(new EventTableModel<Friend>(eventList, new SharingTableFormat()));
        
        eventList.add(new MockFriend("test"));
        eventList.add(new MockFriend("test 2"));
        eventList.add(new MockFriend("test 3"));
        
        initialize();
    }
    
    private void initialize() {
        setFillsViewportHeight(true);
        setBackground(backgroundColor);
        setShowGrid(false, false);
        setTableHeader(null);
        setRowHeight(24);
    }
    
    @Override
    public boolean isCellEditable(int row, int col) {
        if (row >= getRowCount() || col >= getColumnCount() || row < 0 || col < 0) {
            return false;
        }
        return true;
    }
    
    private class SharingTableFormat implements TableFormat<Friend> {

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public String getColumnName(int column) {
            return "";
        }

        @Override
        public Object getColumnValue(Friend baseObject, int column) {
            return baseObject;
        }
    }
}
