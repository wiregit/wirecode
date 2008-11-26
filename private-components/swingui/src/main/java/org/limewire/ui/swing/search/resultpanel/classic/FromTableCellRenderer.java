package org.limewire.ui.swing.search.resultpanel.classic;

import java.awt.Component;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import net.miginfocom.swing.MigLayout;

import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.SearchResultFromWidget;

public class FromTableCellRenderer extends JPanel implements TableCellRenderer, TableCellEditor {

    private final List<CellEditorListener> listeners = new ArrayList<CellEditorListener>();
    private SearchResultFromWidget fromWidget;
    
    public FromTableCellRenderer(SearchResultFromWidget fromWidget) {
        this.fromWidget = fromWidget;
        
        setLayout(new MigLayout("insets 0"));
        
        add(fromWidget, "aligny 50%, gapleft 10");
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {        
        if (value != null) {
            fromWidget.setPeople(((VisualSearchResult)value).getSources());
        }
        return this;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            int row, int column) {
        if (value != null) {
            fromWidget.setPeople(((VisualSearchResult)value).getSources());
        }
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
}