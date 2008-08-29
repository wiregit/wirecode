package org.limewire.ui.swing.search.resultpanel;

import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 * This class renders numbers in JTables so they are right-aligned.
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class NumberTableCellRenderer implements TableCellRenderer {

    private JLabel label = new JLabel();

    public Component getTableCellRendererComponent(
        JTable table, Object value,
        boolean isSelected, boolean hasFocus,
        int row, int column) {

        System.out.println("NumberTableCellRenderer.getTableCellRendererComponent entered");

        String text = value.toString();
        int availableWidth =
            table.getColumnModel().getColumn(column).getWidth();
        availableWidth -= table.getIntercellSpacing().getWidth();
        Insets borderInsets = label.getBorder().getBorderInsets((Component) label);
        availableWidth -= borderInsets.left + borderInsets.right;
        FontMetrics fm = label.getFontMetrics(label.getFont());
        
        if (fm.stringWidth(text) > availableWidth) {
            String dots = "...";
            int textWidth = fm.stringWidth(dots);
            int nChars = text.length() - 1;
            for (; nChars > 0; nChars--) {
                textWidth += fm.charWidth(text.charAt(nChars));
                if (textWidth > availableWidth) break;
            }
            
            label.setText(dots + text.substring(nChars + 1));
        } else {
            label.setText(text);
        }

        // Change the font so it's not bold.
        Font font = label.getFont().deriveFont(Font.PLAIN);
        label.setFont(font);

        return label;
    }
}