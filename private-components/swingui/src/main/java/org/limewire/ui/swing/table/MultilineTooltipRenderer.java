package org.limewire.ui.swing.table;

import java.awt.Component;

import javax.swing.JTable;

import org.limewire.util.StringUtils;

public class MultilineTooltipRenderer extends DefaultLimeTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        String text = getText();
        StringBuilder builder = new StringBuilder("<html>");
        builder.append(StringUtils.explode(text.split("\n"), "<br/>"));
        builder.append("</html>");
        setToolTipText(builder.toString());
        
        return this;
    }
    
}
