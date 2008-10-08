package org.limewire.ui.swing.library.table;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import org.limewire.util.CommonUtils;

public class TimeRenderer extends DefaultTableCellRenderer {
    
    public TimeRenderer(){
        setHorizontalAlignment(SwingConstants.RIGHT);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        if (value != null && value instanceof Long){
            value = CommonUtils.seconds2time(((Long)value).longValue());
        }
        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }
}
