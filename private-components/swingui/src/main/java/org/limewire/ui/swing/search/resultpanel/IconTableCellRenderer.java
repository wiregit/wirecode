package org.limewire.ui.swing.search.resultpanel;

import java.awt.Component;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 * This class renders icons in JTables.
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class IconTableCellRenderer implements TableCellRenderer {

    public Component getTableCellRendererComponent(
        JTable table, Object value,
        boolean isSelected, boolean hasFocus,
        int row, int column) {
        Icon icon = (Icon) value;
        return new JLabel(icon);
    }
}