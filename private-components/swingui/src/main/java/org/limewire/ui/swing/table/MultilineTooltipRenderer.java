package org.limewire.ui.swing.table;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JTable;

import org.limewire.util.StringUtils;

public class MultilineTooltipRenderer extends DefaultLimeTableCellRenderer {

    private static final Dimension INFINITE_SIZE = new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        String text = String.valueOf(value);
        
        String[] lines = text.split("\n");
        
        StringBuilder builder = new StringBuilder("<html>");
        builder.append(StringUtils.explode(lines, "<br/>"));
        builder.append("</html>");
        setToolTipText(builder.toString());
        
        // Only should show the first line in the table...
        if (lines.length > 0) {
            setText(lines[0]);
        }
        
        return this;
    }
    
    /**
     * Overriden so these cells always exceed their table's space restrictions and thus
     *  always show tooltips and subsequent lines. 
     */
    @Override
    public Dimension getPreferredSize() {
        return INFINITE_SIZE;
    }
    
}
