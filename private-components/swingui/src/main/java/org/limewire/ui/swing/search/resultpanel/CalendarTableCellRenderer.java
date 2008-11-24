package org.limewire.ui.swing.search.resultpanel;

import java.awt.Component;
import java.awt.FlowLayout;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.swing.EventTableModel;

/**
 * This class renders a date in human readable format.
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
            EventTableModel tableModel = (EventTableModel) table.getModel();
            VisualSearchResult vsr = (VisualSearchResult) tableModel.getElementAt(row);
            panel.setAlpha(vsr.isSpam() ? 0.2f : 1.0f);
        } else {
            text = value == null ? "" : value.toString();
        }
        
        label.setText(text);

        return panel;
    }
}