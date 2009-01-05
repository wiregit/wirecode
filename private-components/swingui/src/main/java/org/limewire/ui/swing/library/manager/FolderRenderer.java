package org.limewire.ui.swing.library.manager;

import java.awt.Component;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.limewire.ui.swing.util.IconManager;

class FolderRenderer extends DefaultTreeCellRenderer {
    
    private final IconManager iconManager;
    
    public FolderRenderer(IconManager iconManager) {
        this.iconManager = iconManager;
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
            boolean expanded, boolean leaf, int row, boolean hasFocus) {
        if(value instanceof LibraryManagerItem) {
            LibraryManagerItem item = (LibraryManagerItem)value;
            getTreeCellRendererComponent(tree, item.displayName(), false, expanded, leaf, row, false);
            setIcon(iconManager.getIconForFile(item.getFile()));
            return this;
        } else {
            return super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        }
    }
}
