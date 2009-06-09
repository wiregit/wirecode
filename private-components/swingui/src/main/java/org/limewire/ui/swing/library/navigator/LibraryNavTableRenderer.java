package org.limewire.ui.swing.library.navigator;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;

public class LibraryNavTableRenderer extends JLabel implements TableCellRenderer {
    
    private Border border;
    private @Resource Color selectedColor;
    private @Resource Font font;
    private @Resource Color fontColor;
    
    @Inject
    public LibraryNavTableRenderer() {
        GuiUtils.assignResources(this);
        
        border = BorderFactory.createEmptyBorder(10,10,10,10);
        
        setBackground(selectedColor);
        setFont(font);
        setForeground(fontColor);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        setBorder(border);
        
        if(value instanceof LibraryNavItem) {
            LibraryNavItem item = (LibraryNavItem) value;
            setText(item.getDisplayedText());
        } else {
            setText("");
            setIcon(null);
        }

        setOpaque(isSelected);
           
        return this;
    }
}
