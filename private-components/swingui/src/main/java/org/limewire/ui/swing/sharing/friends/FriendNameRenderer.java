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
    private Color seperatorColor;
    
    private Border emptyBorder;
    private Border compoundBorder;

    public FriendNameRenderer() {
        GuiUtils.assignResources(this);
        emptyBorder = BorderFactory.createEmptyBorder(0,10,0,10);
        compoundBorder = BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, seperatorColor),
                emptyBorder);
        setBorder(emptyBorder);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {

        FriendItem item = (FriendItem) value;
        // sharing something
        if(item.getShareListSize() > 0) {
            setBorder(emptyBorder);
            FontUtils.changeStyle(this, Font.BOLD);
            setOpaque(true);
            if(isSelected) 
                setBackground(selectionColor);
            else
                setBackground(backGroundOnlineColor);
            if(column == 0)
                setForeground(onlineNameColor);
            else
                setForeground(numberColor);
        } else { // not sharing something
            // set the border
            if(row > 0 && ((FriendItem)table.getModel().getValueAt(row - 1, 0)).getShareListSize() > 0)
                setBorder(compoundBorder);
            else
                setBorder(emptyBorder);
            setBackground(backGroundOnlineColor);
            setForeground(offlineNameColor);
            
            FontUtils.changeStyle(this, Font.ITALIC);
            if(isSelected) {
                setOpaque(true);
                setBackground(selectionColor);
            } else
                setOpaque(false);
        }
        
        //set the text
        if(column == 0) {
            setText(item.getFriend().getRenderName());
        } else {
            setHorizontalAlignment(RIGHT);
            if( item.getShareListSize() > 0) 
                setText(Integer.toString(item.getShareListSize()));
            else
                setText("");
        }

        return this;
    }

}
