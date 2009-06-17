package org.limewire.ui.swing.library.sharing;

import java.awt.Dimension;

import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;

import org.jdesktop.swingx.JXTable;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.EventTableModel;

import com.google.inject.Inject;

class LibrarySharingTable<T> extends JXTable {
    
    private EventList<T> eventList;
    
    private boolean isEditable = false;
    
    private static final int rowHeight = 24;
    
    @Inject
    public LibrarySharingTable() {
       
        eventList = new BasicEventList<T>();        
        setEventList(eventList);
        
        initialize();
    }
    
    public void setEventList(EventList<T> eventList) {
        setModel(new EventTableModel<T>(eventList, new SharingTableFormat()));
    }
    
    public void enableEditing(boolean isEditable) {
        this.isEditable = isEditable;
    }
    
    private void initialize() {
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
        if (!isEditable || (row >= getRowCount() || col >= getColumnCount() || row < 0 || col < 0)) {
            return false;
        }
        return true;
    }
    
    //Don't set the cell value when editing is cancelled
    @Override
    public void editingStopped(ChangeEvent e) {
        TableCellEditor editor = getCellEditor();
        if (editor != null) {    
            //TODO: this replaces the need to keep a reference of the object in the editor. Not
            // sure if this is cleaner than the way it works now
//            if(editor instanceof LibrarySharingEditableRendererEditor) {
//                LibrarySharingEditableRendererEditor sharingEditor = (LibrarySharingEditableRendererEditor) editor;
//                Object value = getModel().getValueAt(getSelectedRow(), 0);
//                if(value instanceof EditableSharingData) {
//                    EditableSharingData data = (EditableSharingData) value;
//                    data.setIsSelected(sharingEditor.isSelected());
//                }
//            }
            removeEditor();
        }
    }
    
    private class SharingTableFormat implements TableFormat<T> {

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public String getColumnName(int column) {
            return "";
        }

        @Override
        public Object getColumnValue(T baseObject, int column) {
            return baseObject;
        }
    }
}
