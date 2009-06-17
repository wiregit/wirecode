package org.limewire.ui.swing.library.sharing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

class LibrarySharingEditableRendererEditor extends JPanel implements TableCellRenderer, TableCellEditor {

    private @Resource Font font;
    private @Resource Color fontColor;
    private @Resource Color backgroundColor;
    private @Resource Icon checkedCheckBox;
    private @Resource Icon uncheckedCheckBox;
    
    private final List<CellEditorListener> listeners = new ArrayList<CellEditorListener>();
    
    private final JCheckBox checkBox;
    
    private EditableSharingData data;
    
    public LibrarySharingEditableRendererEditor() {
        super(new MigLayout("filly, insets 0, gap 0"));

        GuiUtils.assignResources(this);
       
        checkBox = new JCheckBox();
        checkBox.setIcon(uncheckedCheckBox);
        checkBox.setSelectedIcon(checkedCheckBox);
        checkBox.setOpaque(false);
        checkBox.setIconTextGap(6);
        checkBox.setFont(font);
        checkBox.setFocusPainted(false);
        checkBox.setForeground(fontColor);
        checkBox.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if(data != null) {
                    data.setSelected(checkBox.isSelected());
                }
                stopCellEditing();
            }
        });
                
        setBackground(backgroundColor);
        add(checkBox, "aligny center, growx");
    }
    
    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            int row, int column) {

        if(value instanceof EditableSharingData) {
            data = (EditableSharingData) value;
            checkBox.setText(textFor(data));
            checkBox.setSelected(data.isSelected());
        } else {
            checkBox.setText("");
            checkBox.setSelected(false);
        }     
        return this;
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        if(value instanceof EditableSharingData) {
            EditableSharingData data = (EditableSharingData) value;
            checkBox.setText(textFor(data));
            checkBox.setSelected(data.isSelected());
        } else {
            checkBox.setText("");
            checkBox.setSelected(false);
        }     
        return this;
    }
    
    private String textFor(EditableSharingData data) {
        if(data.getFriend() != null) {
            return data.getFriend().getRenderName();
        } else {
            return I18n.tr("{0} friends from other accounts", data.getIds().size());
        }
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
        return true;
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