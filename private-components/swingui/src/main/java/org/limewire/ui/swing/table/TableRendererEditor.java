package org.limewire.ui.swing.table;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.swingx.JXPanel;

public abstract class TableRendererEditor extends JXPanel implements TableCellRenderer, TableCellEditor {

    private final List<CellEditorListener> listeners = new ArrayList<CellEditorListener>();

    @Override
    public final void addCellEditorListener(CellEditorListener lis) {
        synchronized (listeners) {
            if (!listeners.contains(lis))
                listeners.add(lis);
        }
    }

    @Override
    public final void cancelCellEditing() {
        synchronized (listeners) {
            for (int i = 0, N = listeners.size(); i < N; i++) {
                listeners.get(i).editingCanceled(new ChangeEvent(this));
            }
        }
    }

    @Override
    public final Object getCellEditorValue() {
        return null;
    }

    @Override
    public boolean isCellEditable(EventObject e) {
        return true;
    }

    @Override
    public final void removeCellEditorListener(CellEditorListener lis) {
        synchronized (listeners) {
            if (listeners.contains(lis))
                listeners.remove(lis);
        }
    }

    @Override
    public final boolean shouldSelectCell(EventObject e) {
        return false;
    }

    @Override
    public final boolean stopCellEditing() {
        synchronized (listeners) {
            for (int i = 0, N = listeners.size(); i < N; i++) {
                listeners.get(i).editingStopped(new ChangeEvent(this));
            }
        }
        return true;
    }

}
