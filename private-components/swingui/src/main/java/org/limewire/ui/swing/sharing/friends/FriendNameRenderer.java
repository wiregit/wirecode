package org.limewire.ui.swing.sharing.friends;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * Renders friend names in the sharing panel. Friends that you have shared files
 * with are displayed at the top of the table in alphabetical order. Buddies
 * that you are not sharing anything with are displayed at the bottom of the 
 * table with no file count.
 */
public class FriendNameRenderer extends JLabel implements TableCellRenderer {

    @Resource
    private Color selectionColor;
    @Resource
    private Color backGroundOnlineColor;
    @Resource
    private Color onlineNameColor;
    @Resource
    private Color numberColor;
    @Resource
    private Color offlineNameColor;
    @Resource
    private int fontSize;
    
    private Border emptyRightBorder;
    private Border emptyLeftBorder;

    public FriendNameRenderer() {
        GuiUtils.assignResources(this);
        emptyRightBorder = BorderFactory.createEmptyBorder(10,0,10,6);
        emptyLeftBorder = BorderFactory.createEmptyBorder(10, 6, 10, 0);
        
        FontUtils.setSize(this, fontSize);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {

        FriendItem item = (FriendItem) value;
        // sharing something
        if(item.getShareListSize() > 0) {
            FontUtils.changeStyle(this, Font.BOLD);
            setOpaque(true);

            setSharingBackgroundColor(isSelected);
            setSharingForeground(column);
        } else { // not sharing something
            setBackground(backGroundOnlineColor);
            setForeground(offlineNameColor);
            
            FontUtils.changeStyle(this, Font.ITALIC);
            setNotSharingBackgroundColor(isSelected);
        }
        
        //set the text
        if(column == 0) {
            setText(item.getFriend().getRenderName());
            setBorder(emptyLeftBorder);
        } else {
            setBorder(emptyRightBorder);
            setHorizontalAlignment(RIGHT);
            if( item.getShareListSize() > 0) 
                setText(Integer.toString(item.getShareListSize()));
            else
                setText("");
        }

        return this;
    }
    
    /**
     * Sets the background color for a friend sharing with.
     */
    private void setSharingBackgroundColor(boolean isSelected) {
        if(isSelected) 
            setBackground(selectionColor);
        else
            setBackground(backGroundOnlineColor);
    }
    
    /**
     * Sets the foreground color for a sharing friend
     */
    private void setSharingForeground(int column) {
        if(column == 0)
            setForeground(onlineNameColor);
        else
            setForeground(numberColor);
    }
    
    /**
     * Sets the background color a friend not being shared with
     */
    private void setNotSharingBackgroundColor(boolean isSelected) {
        if(isSelected) {
            setOpaque(true);
            setBackground(selectionColor);
        } else
            setOpaque(false);
    }
}
