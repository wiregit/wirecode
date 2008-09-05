package org.limewire.ui.swing.search.resultpanel;

import java.awt.Component;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 * This class renders any Component in a JTable.
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class ComponentTableCellRenderer implements TableCellRenderer {

    public Component getTableCellRendererComponent(
        JTable table, Object value,
        boolean isSelected, boolean hasFocus,
        int row, int column) {

        return (Component) value;
    }
}