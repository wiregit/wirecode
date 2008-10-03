package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Component;
import java.awt.Font;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 * This class renders icons in JTables.
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class IconTableCellRenderer implements TableCellRenderer {

    private JLabel label = new JLabel();

    public Component getTableCellRendererComponent(
        JTable table, Object value,
        boolean isSelected, boolean hasFocus,
        int row, int column) {

        if (value instanceof Icon) {
            label.setIcon((Icon) value);
        } else {
            label.setText(value == null ? tr("none") : value.toString());

            // Change the font so it's not bold.
            Font font = label.getFont().deriveFont(Font.PLAIN);
            label.setFont(font);
        }
        
        return label;
    }
}