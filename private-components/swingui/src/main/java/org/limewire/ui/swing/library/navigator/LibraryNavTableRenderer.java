package org.limewire.ui.swing.library.navigator;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.ui.swing.library.navigator.LibraryNavItem.NavType;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;

public class LibraryNavTableRenderer extends JLabel implements TableCellRenderer {
    
    private Border border;
    private @Resource Color selectedColor;
    private @Resource Font font;
    private @Resource Color fontColor;
    private @Resource int iconGap;
    private @Resource Icon libraryIcon;
    private @Resource Icon publicIcon;
    private @Resource Icon listIcon;
    private @Resource Icon listSharedIcon;
    
    @Inject
    public LibraryNavTableRenderer() {        
        GuiUtils.assignResources(this);
        
        border = BorderFactory.createEmptyBorder(5,6,5,6);
        
        setBackground(selectedColor);
        setFont(font);
        setIconTextGap(iconGap);
        setForeground(fontColor);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        setBorder(border);
        
        if(value instanceof LibraryNavItem) {
            LibraryNavItem item = (LibraryNavItem) value;
            setText(item.getDisplayedText());
            setIconType(item);
        } else {
            setText("");
            setIcon(null);
        }

        setOpaque(isSelected);
           
        return this;
    }
    
    private void setIconType(LibraryNavItem item) {
        if(item.getType() == NavType.LIBRARY)
            setIcon(libraryIcon);
        else if(item.getType() == NavType.PUBLIC_SHARED)
            setIcon(publicIcon);
        else {
            if(item.getLocalFileList() instanceof SharedFileList) {
                if(((SharedFileList)item.getLocalFileList()).getFriendIds().size() > 0)
                    setIcon(listSharedIcon);
                else
                    setIcon(listIcon);
            } else
                setIcon(listIcon);
        }
    }
}
