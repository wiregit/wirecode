package org.limewire.ui.swing.table;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.util.IconManager;


public class IconLabelRenderer extends DefaultTableCellRenderer {

    IconManager iconManager;
    
    public IconLabelRenderer(IconManager iconManager) {
        this.iconManager = iconManager;
        
        setIconTextGap(5);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        
        LocalFileItem item = (LocalFileItem) value;
        
        if(table.getSelectedRow() == row) {
            this.setBackground(table.getSelectionBackground());
            this.setForeground(table.getSelectionForeground());
        }
        else {
            this.setBackground(table.getBackground());
            this.setForeground(table.getForeground());
        }
        
        setIcon(iconManager.getIconForFile(item.getFile()));
        setText(item.getName());
        
        return this;
    }
}
