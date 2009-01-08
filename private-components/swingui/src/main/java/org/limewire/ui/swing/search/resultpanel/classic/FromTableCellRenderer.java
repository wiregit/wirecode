package org.limewire.ui.swing.search.resultpanel.classic;

import java.awt.Component;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.SearchResultFromWidget;

public class FromTableCellRenderer extends JXPanel implements TableCellRenderer, TableCellEditor {

    private static final int HGAP = 4;
    private static final int VGAP = 5;
    
    private final List<CellEditorListener> listeners = new ArrayList<CellEditorListener>();
    private final SearchResultFromWidget fromWidget;
    
    public FromTableCellRenderer(SearchResultFromWidget fromWidget) {
        super(new FlowLayout(FlowLayout.LEFT, HGAP, VGAP));
        
        this.fromWidget = fromWidget;
        
        add(fromWidget);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {        
        if (value != null) {
            fromWidget.setPeople(((VisualSearchResult)value).getSources());
        }
        fromWidget.setForeground(this.getForeground());
        
        return this;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            int row, int column) {
        if(!isSelected)
            setBackground(table.getBackground());
        else
            setBackground(table.getSelectionBackground());
        
        if (value != null) {
            fromWidget.setPeople(((VisualSearchResult)value).getSources());
        }
        fromWidget.setForeground(this.getForeground());
        
        return this;
    }

    @Override
    public void addCellEditorListener(CellEditorListener lis) {
        synchronized (listeners) {
            if (!listeners.contains(lis))
                listeners.add(lis);
        }
    }

    @Override
    public void cancelCellEditing() {
        synchronized (listeners) {
            for (int i = 0, N = listeners.size(); i < N; i++) {
                listeners.get(i).editingCanceled(new ChangeEvent(this));
            }
        }
    }

    @Override
    public Object getCellEditorValue() {
        return null;
    }

    @Override
    public boolean isCellEditable(EventObject anEvent) {
        return true;
    }

    @Override
    public void removeCellEditorListener(CellEditorListener lis) {
        synchronized (listeners) {
            if (listeners.contains(lis))
                listeners.remove(lis);
        }
    }

    @Override
    public boolean shouldSelectCell(EventObject anEvent) {
        return false;
    }

    @Override
    public boolean stopCellEditing() {
        synchronized (listeners) {
            for (int i = 0, N = listeners.size(); i < N; i++) {
                listeners.get(i).editingStopped(new ChangeEvent(this));
            }
        }
        return true;
    }
    
    
    @Override
    public String getToolTipText(){
        return fromWidget.getToolTipText();
    }
}