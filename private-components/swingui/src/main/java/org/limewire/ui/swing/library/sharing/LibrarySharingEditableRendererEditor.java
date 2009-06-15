package org.limewire.ui.swing.library.sharing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;

public class LibrarySharingEditableRendererEditor extends JPanel implements TableCellRenderer, TableCellEditor {

    private @Resource Font font;
    private @Resource Color fontColor;
    private @Resource Color backgroundColor;
    
    private final List<CellEditorListener> listeners = new ArrayList<CellEditorListener>();
    
    private final JCheckBox checkBox;
    private final JLabel nameLabel;
    
    private EditableSharingData data;
    
    @Inject
    public LibrarySharingEditableRendererEditor() {
        super(new MigLayout("filly, insets 0, gap 0"));

        GuiUtils.assignResources(this);
       
        checkBox = new JCheckBox();
        checkBox.setOpaque(false);
        checkBox.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if(data != null)
                    data.setIsSelected(checkBox.isSelected());
                stopCellEditing();
            }
        });
        nameLabel = new JLabel();
        nameLabel.setFont(font);
        nameLabel.setForeground(fontColor);
                
        setBackground(backgroundColor);
        add(checkBox, "aligny center, gapright 6");
        add(nameLabel, "growx, alignx left, aligny center, wrap");
    }
    
    public boolean isSelected() {
        return checkBox.isSelected();
    }
    
    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            int row, int column) {

        if(value instanceof EditableSharingData) {
            data = (EditableSharingData) value;
            nameLabel.setText(data.getName());
            checkBox.setSelected(data.isSelected());
        } else {
            nameLabel.setText("");
            checkBox.setSelected(false);
        }
        
        return this;
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        if(value instanceof EditableSharingData) {
            EditableSharingData data = (EditableSharingData) value;
            nameLabel.setText(data.getName());
            checkBox.setSelected(data.isSelected());
        } else {
            nameLabel.setText("");
            checkBox.setSelected(false);
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