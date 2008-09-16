package org.limewire.ui.swing.sharing.table;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;

import org.limewire.ui.swing.sharing.fancy.SharingFancyTablePanel.TableMouseListener;
import org.limewire.ui.swing.table.MultiButtonTableCellRendererEditor;

public class SharingFancyMultiButtonTableCellRendererEditor extends MultiButtonTableCellRendererEditor {
    
    private final TableMouseListener tableListener;
    
    public SharingFancyMultiButtonTableCellRendererEditor(TableMouseListener tableListener) {
        super();
        setOpaque(true);
        
        this.tableListener = tableListener;
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {

        if(row == tableListener.getMouseOverRow()) {
            setBackground(Color.BLUE);
            setForeground(Color.WHITE);
            for(int i = 0; i < getComponentCount(); i++) {
                getComponent(i).setVisible(true);
            }
        } else {
            setBackground(Color.WHITE);
            setForeground(Color.BLACK);
            for(int i = 0; i < getComponentCount(); i++) {
                getComponent(i).setVisible(false);
            }
        }
        return this;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            int row, int column) {

        if(row == tableListener.getMouseOverRow()) {
            setBackground(Color.BLUE);
            setForeground(Color.WHITE);
            for(int i = 0; i < getComponentCount(); i++) {
                getComponent(i).setVisible(true);
            }
        } else {
            setBackground(Color.WHITE);
            setForeground(Color.BLACK);
            for(int i = 0; i < getComponentCount(); i++) {
                getComponent(i).setVisible(false);
            }
        }
        return this;
    }
}
