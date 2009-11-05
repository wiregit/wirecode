package org.limewire.ui.swing.table;

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;

import com.google.inject.Singleton;

/**
 * A DefaultTableCellRenderer that adds padding to the left and right border
 * and removes the focus rectangle around the cell.
 */       
@Singleton
public class DefaultLimeTableCellRenderer extends DefaultTableCellRenderer {

    private static final Border border = BorderFactory.createEmptyBorder(0,5,0,5);
    
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    
        setBorder(border);
        
        return this;
    }
    
    @Override
    protected void setValue(Object value) {
        // set value to empty string to avoid super class from calling toString()
        // on value which could be expensive
        super.setValue("");
    }
}
