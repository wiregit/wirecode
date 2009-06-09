package org.limewire.ui.swing.library.sharing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.application.Resource;
import org.limewire.core.api.friend.Friend;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;

public class LibrarySharingNonEditableRenderer extends JLabel implements TableCellRenderer {

    private @Resource Font font;
    private @Resource Color fontColor;
    
    @Inject
    public LibrarySharingNonEditableRenderer() {
        GuiUtils.assignResources(this);
        
        setFont(font);
        setForeground(fontColor);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        
        if(value instanceof Friend) {
            Friend friend = (Friend) value;
            setText(friend.getRenderName());
        } else {
            setText("");
        }
        
        return this;
    }
}
