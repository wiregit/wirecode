package org.limewire.ui.swing.search.resultpanel;

import java.awt.Component;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 * This class renders icons in JTables.
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class CalendarTableCellRenderer implements TableCellRenderer {

    private JLabel label = new JLabel();

    public Component getTableCellRendererComponent(
        JTable table, Object value,
        boolean isSelected, boolean hasFocus,
        int row, int column) {

        String text = null;
        if (value instanceof Calendar) {
            Calendar calendar = (Calendar) value;
            SimpleDateFormat sdf = new SimpleDateFormat("M/d/yyyy");
            text = sdf.format(calendar.getTime());
        } else {
            text = value == null ? "" : value.toString();
        }
        
        label.setText(text);

        // Change the font so it's not bold.
        Font font = label.getFont().deriveFont(Font.PLAIN);
        label.setFont(font);

        label.setOpaque(false);

        return label;
    }
}