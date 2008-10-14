package org.limewire.ui.swing.sharing.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;

public class CustomTableCellHeaderRenderer extends DefaultTableCellRenderer {

    @Resource
    private Color backgroundColor;
    @Resource
    private Color foregroundColor;
    
    public CustomTableCellHeaderRenderer() {
        GuiUtils.assignResources(this);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        setBackground(backgroundColor);
        setForeground(foregroundColor);
        setPreferredSize(new Dimension(20, getPreferredSize().width));
        
        return this;
    }

}
