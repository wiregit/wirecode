package org.limewire.ui.swing.library.manager;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.util.I18n;

/**
 * Table cell renderer that displays the "remove" hyperlink button.
 */
class RemoveButtonRenderer extends HyperlinkButton implements TableCellRenderer {

    /**
     * Constructs a RemoveButtonRenderer.
     */
    public RemoveButtonRenderer() {
        super(new AbstractAction(I18n.tr("remove")) {
            @Override
            public void actionPerformed(ActionEvent e) {
            }
        });
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(2, 4, 0, 4));
    }
    
    /**
     * Changes to foreground using this method will be ignored
     */
    @Override
    public void setForeground(Color c) {
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        return this;
    }

}
