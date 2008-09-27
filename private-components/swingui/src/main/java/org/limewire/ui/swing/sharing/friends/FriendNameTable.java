package org.limewire.ui.swing.sharing.friends;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;

import javax.swing.ListSelectionModel;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXTable;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.sharing.menu.FriendSharingActionHandler;
import org.limewire.ui.swing.sharing.menu.FriendSharingPopupHandler;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.EventTableModel;

/**
 * Table for displaying a list of friends in the shared view
 */
public class FriendNameTable extends JXTable {

    @Resource
    Color tableBackgroundColor;
    
    private EventTableModel<FriendItem> tableModel;
    
    public FriendNameTable(EventList<FriendItem> eventList, TableFormat<FriendItem> tableFormat,
            LibraryManager libraryManager, ShareListManager shareListManager, Navigator navigator) {
        GuiUtils.assignResources(this);
        
        SortedList<FriendItem> friendList = new SortedList<FriendItem>(eventList, new FriendComparator());       
        tableModel = new EventTableModel<FriendItem>(friendList, tableFormat);
        
        setModel(tableModel);
        
        setBackground(tableBackgroundColor);
        setColumnControlVisible(false);
        setTableHeader(null);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setShowGrid(false, false);
        setColumnSelectionAllowed(false);
        
        getColumn(0).setCellRenderer(new FriendNameRenderer());
        getColumn(1).setCellRenderer(new FriendNameRenderer());
        
        getColumn(1).setWidth(30);
        getColumn(1).setPreferredWidth(30);
        
        final FriendSharingPopupHandler handler = new FriendSharingPopupHandler(this, new FriendSharingActionHandler(navigator, libraryManager), shareListManager);
        
        addMouseListener(new MouseAdapter() {
            
            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int col = columnAtPoint(e.getPoint());
                    int row = rowAtPoint(e.getPoint());
                    if(row != getSelectedRow()) {
                        setRowSelectionInterval(row, row);
                    }
                    if (row >= 0 && col >= 0) {
                        handler.maybeShowPopup(
                            e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });
    }
    
    public EventTableModel<FriendItem> getEventTableModel() {
        return tableModel;
    }
    
    private static class FriendComparator implements Comparator<FriendItem> {
        @Override
        public int compare(FriendItem o1, FriendItem o2) {
            if(o1.size() > 0 && o2.size() > 0) { 
                return o1.getFriend().getRenderName().compareTo(o2.getFriend().getRenderName());
            } else if(o1.size() > 0 && o2.size() <= 0) {
                return -1;
            } else if(o1.size() <= 0 && o2.size() > 0) {
                return 1;
            } else {
                return o1.getFriend().getRenderName().compareTo(o2.getFriend().getRenderName());
            }
        }
    }
}
