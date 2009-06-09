package org.limewire.ui.swing.library.sharing;

import java.awt.Color;
import java.awt.Dimension;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXTable;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.EventTableModel;

import com.google.inject.Inject;

public class LibrarySharingTable extends JXTable {

    @Resource private Color backgroundColor;
    
    private EventList<String> eventList;
    
    private boolean isEditable = false;
    
    private static final int rowHeight = 24;
    
    @Inject
    public LibrarySharingTable() {
        GuiUtils.assignResources(this);
        
        eventList = new BasicEventList<String>();        
        eventList.add("test");
        eventList.add("test 2");
        eventList.add("test 3");
        setEventList(eventList);
        
        initialize();
    }
    
    public void setEventList(EventList<String> eventList) {
        setModel(new EventTableModel<String>(eventList, new SharingTableFormat()));
    }
    
    public void enableEditing(boolean isEditable) {
        this.isEditable = isEditable;
    }
    
    private void initialize() {
//        setPreferredScrollableViewportSize(new Dimension(100,50));
//        setFillsViewportHeight(true);
        setBackground(backgroundColor);
        setShowGrid(false, false);
        setTableHeader(null);
        setRowHeight(rowHeight);
    }
    
    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return new Dimension(super.getPreferredScrollableViewportSize().width, getModel().getRowCount() * getRowHeight());
    }
    
    @Override
    public boolean isCellEditable(int row, int col) {
        if (!isEditable && row >= getRowCount() || col >= getColumnCount() || row < 0 || col < 0) {
            return false;
        }
        return true;
    }
    
    private class SharingTableFormat implements TableFormat<String> {

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public String getColumnName(int column) {
            return "";
        }

        @Override
        public Object getColumnValue(String baseObject, int column) {
            return baseObject;
        }
    }
}
