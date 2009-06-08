package org.limewire.ui.swing.library.navigator;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;

public class LibraryNavTableRenderer extends JLabel implements TableCellRenderer, TableCellEditor {

    private final List<CellEditorListener> listeners = new ArrayList<CellEditorListener>();
    
    private Border border;
    private @Resource Color selectedColor;
    private @Resource Font font;
    private @Resource Color fontColor;
    
    @Inject
    public LibraryNavTableRenderer() {
        GuiUtils.assignResources(this);
        
        border = BorderFactory.createEmptyBorder(10,10,10,10);
        
        setBackground(selectedColor);
        setFont(font);
        setForeground(fontColor);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        setBorder(border);
        
        if(value instanceof LibraryNavItem) {
            LibraryNavItem item = (LibraryNavItem) value;
            setText(item.getDisplayedText());
        } else {
            setText("");
            setIcon(null);
        }

        setOpaque(isSelected);
           
        return this;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            int row, int column) {
        setBorder(border);
        setBackground(Color.RED);
        
        if(value instanceof LibraryNavItem) {
            LibraryNavItem item = (LibraryNavItem) value;
            setText(item.getDisplayedText());
        } else {
            setText("");
            setIcon(null);
        }

        setOpaque(isSelected);
           
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
