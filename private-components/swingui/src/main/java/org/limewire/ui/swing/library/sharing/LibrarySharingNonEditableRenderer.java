package org.limewire.ui.swing.library.sharing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;

public class LibrarySharingNonEditableRenderer extends JLabel implements TableCellRenderer {

    private @Resource Font font;
    private @Resource Color fontColor;
    private @Resource Color backgroundColor;
    private @Resource Color backgroundScrollBarColor;
    
    @Inject
    public LibrarySharingNonEditableRenderer() {
        GuiUtils.assignResources(this);
        
        setBorder(BorderFactory.createEmptyBorder(10,14,10,5));
        setFont(font);
        setForeground(fontColor);
        setBackground(backgroundScrollBarColor);
        setOpaque(true);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        if(table.getVisibleRect().height < table.getRowCount() * table.getRowHeight())
            setBackground(backgroundScrollBarColor);
        else
            setBackground(backgroundColor);

        if(value instanceof String) {
            setText((String)value);
        } else {
            setText("");
        }
        
        return this;
    }
}
