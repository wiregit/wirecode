package org.limewire.ui.swing.table;

import java.awt.Component;
import java.awt.FlowLayout;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.swing.EventTableModel;

/**
 * This class is a table cell renderer that can toggle
 * between 100% and some other opacity.
 * 
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class StringTableCellRenderer implements TableCellRenderer {

    private static final int HGAP = 2;
    private static final int VGAP = 5;

    private JXLabel label = new JXLabel();
    private JXPanel panel;

    public StringTableCellRenderer() {
        panel = new JXPanel();
        panel.add(label);
    }

    public Component getTableCellRendererComponent(
        JTable table, Object value,
        boolean isSelected, boolean hasFocus,
        int row, int column) {

        if(value == null) {
            value = "";
        } else {
            EventTableModel tableModel = (EventTableModel) table.getModel();
            VisualSearchResult vsr = (VisualSearchResult) tableModel.getElementAt(row);
            panel.setAlpha(vsr.isSpam() ? 0.2f : 1.0f);
        }
        
        label.setText(value.toString());

        int align = value instanceof Number ?
            FlowLayout.RIGHT : FlowLayout.LEFT;
        panel.setLayout(new FlowLayout(align, HGAP, VGAP));

        return panel;
    }
}