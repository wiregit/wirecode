package org.limewire.ui.swing.sharing.friends;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;

import org.limewire.ui.swing.util.FontUtils;

/**
 * Renders buddy names in the sharing panel. Buddys that you have shared files
 * with are displayed at the top of the table in alphabetical order. Buddies
 * that you are not sharing anything with are displayed at the bottom of the 
 * table with no file count.
 */
public class BuddyNameRenderer extends JLabel implements TableCellRenderer {

    private Color selectionColor = Color.gray.brighter().brighter();
    private Color onlineColor = Color.LIGHT_GRAY;
    
    private Border emptyBorder;
    private Border compoundBorder;

    public BuddyNameRenderer() {
        emptyBorder = BorderFactory.createEmptyBorder(0,10,0,10);
        compoundBorder = BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, Color.BLACK),
                emptyBorder);
        setBorder(emptyBorder);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {

        BuddyItem item = (BuddyItem) value;
        // sharing something
        if(item.size() > 0) {
            setBorder(emptyBorder);
            FontUtils.changeStyle(this, Font.BOLD);
            setOpaque(true);
            if(isSelected) 
                setBackground(selectionColor);
            else
                setBackground(onlineColor);
        } else { // not sharing something
            // set the border
            if(row > 0 && ((BuddyItem)table.getModel().getValueAt(row - 1, 0)).size() > 0)
                setBorder(compoundBorder);
            else
                setBorder(emptyBorder);
            
            FontUtils.changeStyle(this, Font.ITALIC);
            if(isSelected) {
                setOpaque(true);
                setBackground(selectionColor);
            } else
                setOpaque(false);
        }
        
        //set the text
        if(column == 0) {
            setText(item.getName());
        } else {
            setHorizontalAlignment(RIGHT);
            if( item.size() > 0) 
                setText(Integer.toString(item.size()));
            else
                setText("");
        }

        return this;
    }

}
