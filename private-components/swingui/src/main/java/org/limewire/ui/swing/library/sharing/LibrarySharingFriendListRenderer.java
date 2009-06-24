package org.limewire.ui.swing.library.sharing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;

import org.jdesktop.application.Resource;
import org.limewire.friend.api.Friend;
import org.limewire.ui.swing.util.GuiUtils;

class LibrarySharingFriendListRenderer extends DefaultTableCellRenderer {

    private @Resource Font font;
    private @Resource Color fontColor;
    private @Resource Color backgroundColor;
    private @Resource Color backgroundScrollBarColor;
    
    private final Border border = BorderFactory.createEmptyBorder(10,14,10,5);
    
    private final JScrollPane scrollPane;
    
    public LibrarySharingFriendListRenderer(JScrollPane scrollPane) {
        GuiUtils.assignResources(this);
        this.scrollPane = scrollPane;
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        if(value instanceof Friend) {
            value = ((Friend)value).getRenderName();
        } else if(value instanceof String) {
            value = "<html>" + value + "</html>";
        }
        
        if(value == null) {
            value = "";
        }        
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        
        setBorder(border);
        setFont(font);
        setForeground(fontColor);
        setBackground(scrollPane.getVerticalScrollBar().isVisible() ? backgroundScrollBarColor : backgroundColor);
//        setPreferredSize(null);
//        setPreferredSize(new Dimension(table.getWidth(), getPreferredSize().height));
        return this;
    }
}