package org.limewire.ui.swing.sharing.friends;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;

import javax.swing.ListSelectionModel;

import org.jdesktop.swingx.JXTable;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.ui.swing.sharing.menu.BuddySharingActionHandler;
import org.limewire.ui.swing.sharing.menu.BuddySharingPopupHandler;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.EventTableModel;

/**
 * Table for displaying a list of buddies in the shared view
 */
public class BuddyNameTable extends JXTable {

    private EventTableModel<BuddyItem> tableModel;
    
    public BuddyNameTable(EventList<BuddyItem> eventList, TableFormat<BuddyItem> tableFormat, LibraryManager libraryManager) {
//        super(new EventTableModel<BuddyItem>(new SortedList<BuddyItem>(eventList, new BuddyComparator()), tableFormat));
        
        SortedList<BuddyItem> buddyList = new SortedList<BuddyItem>(eventList, new BuddyComparator());       
        tableModel = new EventTableModel<BuddyItem>(buddyList, tableFormat);
        
        setModel(tableModel);
//        EventSelectionModel model = new EventSelectionModel<BuddyItem>(buddyList);
//        setSelectionModel(model);
//        buddyList.setMode(SortedList.STRICT_SORT_ORDER);
        
        setBackground(Color.GRAY);
        setColumnControlVisible(false);
        setTableHeader(null);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setShowGrid(false, false);
        setColumnSelectionAllowed(false);
        
        getColumn(0).setCellRenderer(new BuddyNameRenderer());
        getColumn(1).setCellRenderer(new BuddyNameRenderer());
        
        getColumn(1).setWidth(30);
        getColumn(1).setPreferredWidth(30);
        
        final BuddySharingPopupHandler handler = new BuddySharingPopupHandler(this, new BuddySharingActionHandler(libraryManager), libraryManager);
        
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
    
    public EventTableModel<BuddyItem> getEventTableModel() {
        return tableModel;
    }
    
    private static class BuddyComparator implements Comparator<BuddyItem> {

        @Override
        public int compare(BuddyItem o1, BuddyItem o2) {
            if(o1.size() > 0 && o2.size() > 0) { 
                return o1.getName().compareTo(o2.getName());
            } else if(o1.size() > 0 && o2.size() <= 0) {
                return -1;
            } else if(o1.size() <= 0 && o2.size() > 0) {
                return 1;
            } else {
                return o1.getName().compareTo(o2.getName());
            }
        }
    }

}
