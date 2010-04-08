package org.limewire.ui.swing.search.resultpanel.classic;

import java.awt.Component;

import javax.swing.JTable;

import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.table.DefaultLimeTableCellRenderer;

public class RelevanceRenderer extends DefaultLimeTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
                row, column);

        if(value instanceof VisualSearchResult) {
            VisualSearchResult vsr = (VisualSearchResult)value;
            if(vsr.isSpam()) {
                setText("");
            } else {
                int relevance = ((int) Math.sqrt(vsr.getRelevance())) - 1;
                StringBuilder sb = new StringBuilder();
                for(int i = 0; i < relevance && i < 10; i++) {
                    sb.append('\u2665');
                }
                setText(sb.toString());
            }
        } else {
            setText("");
        }
        return this;
    }
}