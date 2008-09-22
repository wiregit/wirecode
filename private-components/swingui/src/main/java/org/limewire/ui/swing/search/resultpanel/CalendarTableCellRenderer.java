package org.limewire.ui.swing.search.resultpanel;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.swingx.JXPanel;

/**
 * This class renders icons in JTables.
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class CalendarTableCellRenderer implements TableCellRenderer {

    private static final int HGAP = 2;
    private static final int VGAP = 5;
    
    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("M/d/yyyy");

    private JLabel label = new JLabel();
    private JXPanel panel = new JXPanel();

    public CalendarTableCellRenderer() {
        panel.add(label);
        label.setOpaque(false);
        //panel.setOpaque(false);

        int align = FlowLayout.LEFT;
        panel.setLayout(new FlowLayout(align, HGAP, VGAP));
    }

    public Component getTableCellRendererComponent(
        JTable table, Object value,
        boolean isSelected, boolean hasFocus,
        int row, int column) {

        String text = null;
        if (value instanceof Long) {
            text = DATE_FORMAT.format(new Date((Long)value));
        } else {
            text = value == null ? "" : value.toString();
        }
        
        label.setText(text);

        // Change the font so it's not bold.
        Font font = label.getFont().deriveFont(Font.PLAIN);
        label.setFont(font);

        // TODO: RMV How can you determine the VisualSearchResult being rendered?
        //float opacity = vsr.isMarkedAsJunk() ? 0.2f : 1.0f;
        float opacity = 0.2f;
        panel.setAlpha(opacity);

        return panel;
    }
}