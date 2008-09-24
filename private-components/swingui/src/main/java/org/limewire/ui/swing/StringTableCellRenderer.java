package org.limewire.ui.swing;

import java.awt.Component;
import java.awt.FlowLayout;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.swingx.JXPanel;

/**
 * This class is a table cell renderer that can toggle
 * between 100% and some other opacity.
 * 
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class StringTableCellRenderer implements TableCellRenderer {

    private static final int HGAP = 2;
    private static final int VGAP = 5;

    private JLabel label = new JLabel();
    private JXPanel panel;

    public StringTableCellRenderer() {
        panel = new JXPanel() {
//            @Override
//            public void setBackground(Color bg) {
//                super.setBackground(bg);
//                label.setBackground(bg);
//                //System.out.println("StringTableCellRenderer: bg = " + bg);
 //           }
        };

        panel.add(label);
        //label.setOpaque(false);
        //panel.setOpaque(false);
    }

    public Component getTableCellRendererComponent(
        JTable table, Object value,
        boolean isSelected, boolean hasFocus,
        int row, int column) {

        if(value == null) {
            value = "";
        }
        
        label.setText(value.toString());

        int align = value instanceof Number ?
            FlowLayout.RIGHT : FlowLayout.LEFT;
        panel.setLayout(new FlowLayout(align, HGAP, VGAP));

        // TODO: RMV How can you determine the VisualSearchResult being rendered?
        //float opacity = vsr.isMarkedAsJunk() ? 0.2f : 1.0f;
        float opacity = 1.0f; // 0.2f;
        panel.setAlpha(opacity);

        return panel;
    }
}