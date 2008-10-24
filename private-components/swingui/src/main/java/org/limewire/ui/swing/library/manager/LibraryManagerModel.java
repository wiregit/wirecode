package org.limewire.ui.swing.library.manager;

import java.util.List;

import org.jdesktop.swingx.treetable.AbstractTreeTableModel;
import org.limewire.ui.swing.util.I18n;

public class LibraryManagerModel extends AbstractTreeTableModel {

    public static final int FOLDER = 0;
    public static final int DONT_SCAN_INDEX = 1;
    public static final int SCAN_INDEX = 2;
    
    LibraryManagerItem item;
    
    public LibraryManagerModel(LibraryManagerItem item) {
        super(item);
        
        this.item = item;
    }
    
    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public Object getValueAt(Object node, int column) {
        if(node instanceof LibraryManagerItem) {
            LibraryManagerItem item = (LibraryManagerItem) node;
            switch (column) {
                case DONT_SCAN_INDEX: return item;
                case SCAN_INDEX: return item;
                case FOLDER: return item.displayName();
            }
        }
        return null;
    }
    
    public String getColumnName(int column) {
        switch(column) {
            case DONT_SCAN_INDEX: return I18n.tr("Don't Scan");
            case SCAN_INDEX: return I18n.tr("Scan");
            case FOLDER: return I18n.tr("Folder");
        }
        throw new IllegalArgumentException("Unknown column:" + column);
    }

    @Override
    public Object getChild(Object parent, int index) {
        if(parent instanceof LibraryManagerItem) {
            List<LibraryManagerItem> items = ((LibraryManagerItem)parent).getChildren();
            if(index < 0 || index >= items.size())
                return null;
            return items.get(index);
        }
        return null;
    }

    @Override
    public int getChildCount(Object parent) {
        if(parent instanceof LibraryManagerItem) {
            LibraryManagerItem item = (LibraryManagerItem) parent;
            return item.getChildren().size();
        }
        return 0;
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if(parent instanceof LibraryManagerItem && child instanceof LibraryManagerItem) {
            LibraryManagerItem childItem = (LibraryManagerItem) child;
            LibraryManagerItem parentItem = (LibraryManagerItem) parent;
            
            for(int i = 0; i < parentItem.getChildren().size(); i++) {
                LibraryManagerItem item = parentItem.getChildren().get(i);
                if(item.equals(childItem))
                    return i;
            }
        }
        
        return -1;
    }
}
