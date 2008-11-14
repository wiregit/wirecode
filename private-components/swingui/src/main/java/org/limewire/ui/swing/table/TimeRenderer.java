package org.limewire.ui.swing.table;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.SwingConstants;

import org.limewire.ui.swing.library.table.DefaultLibraryRenderer;
import org.limewire.util.CommonUtils;

public class TimeRenderer extends DefaultLibraryRenderer {
    
    public TimeRenderer(){
        setHorizontalAlignment(SwingConstants.RIGHT);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        
        if (value != null) {
            setText(CommonUtils.seconds2time(Long.valueOf((String)value))); 
        }
        return this;
    }
}
