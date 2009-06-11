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
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.ui.swing.library.navigator.LibraryNavItem.NavType;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;

public class LibraryNavTableRenderer extends JLabel implements TableCellRenderer {
    
    private Border border;
    private @Resource Color selectedColor;
    private @Resource Font font;
    private @Resource Color fontColor;
    private @Resource Icon libraryIcon;
    private @Resource Icon publicIcon;
    private @Resource Icon listIcon;
    private @Resource Icon listSharedIcon;
    
    private final SharedFileListManager sharedFileListManager;
    
    @Inject
    public LibraryNavTableRenderer(SharedFileListManager sharedFileListManager) {
        this.sharedFileListManager = sharedFileListManager;
        
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
            String id = item.getTabID();
            if(id != null) {
                if(sharedFileListManager.getSharedFileList(id).getFriendIds().size() > 0)
                    setIcon(listSharedIcon);
                else
                    setIcon(listIcon);
            } else {
                setIcon(listIcon);
            }
        }
    }
}
