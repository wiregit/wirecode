package org.limewire.ui.swing.table;

import java.awt.Component;

import javax.swing.JTable;

import org.limewire.ui.swing.library.table.DefaultLibraryRenderer;
import org.limewire.ui.swing.util.GuiUtils;

public class QualityRenderer extends DefaultLibraryRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        
        if (value != null && value instanceof Number) {
            Number num = (Number)value;
            setText(GuiUtils.toQualityString(num.longValue())); 
        }
        return this;
    }
}
