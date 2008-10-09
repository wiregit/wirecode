package org.limewire.ui.swing.table;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.util.FileUtils;

/**
 * Renders a table cell with a string and the system icon representing that
 * file type.
 */
public class IconLabelRenderer extends DefaultTableCellRenderer {

    IconManager iconManager;
    
    public IconLabelRenderer(IconManager iconManager) {
        this.iconManager = iconManager;
        
        setIconTextGap(5);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        
        FileItem item = (FileItem) value;
        
        if(table.getSelectedRow() == row) {
            this.setBackground(table.getSelectionBackground());
            this.setForeground(table.getSelectionForeground());
        }
        else {
            this.setBackground(table.getBackground());
            this.setForeground(table.getForeground());
        }
        
        if (item instanceof RemoteFileItem){
            setIcon(iconManager.getIconForExtension(FileUtils.getFileExtension(item.getFileName())));
        } else {
            setIcon(iconManager.getIconForFile(((LocalFileItem)item).getFile()));
        }
        setText(item.getName());
        
        return this;
    }
}
