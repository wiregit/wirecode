package org.limewire.ui.swing.sharing.friends;

import java.awt.Color;
import java.util.Comparator;

import javax.swing.ListSelectionModel;

import org.jdesktop.swingx.JXTable;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.EventTableModel;

/**
 * Table for displaying a list of buddies in the shared view
 */
public class BuddyNameTable extends JXTable {

    public BuddyNameTable(EventList<BuddyItem> eventList, TableFormat<BuddyItem> tableFormat) {
        super(new EventTableModel<BuddyItem>(new SortedList<BuddyItem>(eventList, new BuddyComparator()), tableFormat));
        
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
    }
    
    private static class BuddyComparator implements Comparator<BuddyItem> {

        @Override
        public int compare(BuddyItem o1, BuddyItem o2) { System.out.println("comparing " + o1.getName() +":"+ o1.size() + "  " + o2.getName() + ":" + o2.size());
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
