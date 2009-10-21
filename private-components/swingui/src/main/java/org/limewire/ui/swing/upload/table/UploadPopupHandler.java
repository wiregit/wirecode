package org.limewire.ui.swing.upload.table;

import java.awt.Component;
import java.awt.Point;
import java.util.List;

import javax.swing.JPopupMenu;

import org.limewire.core.api.upload.UploadItem;
import org.limewire.ui.swing.table.TablePopupHandler;

/**
 * Popup menu handler for the Uploads table.
 */
public class UploadPopupHandler implements TablePopupHandler {

    private final UploadTable table;
    private final UploadPopupMenuFactory popupMenuFactory;

    private JPopupMenu popupMenu;
    
    /**
     * Constructs a popup handler for the specified table.
     */
    public UploadPopupHandler(UploadTable table,
            UploadPopupMenuFactory popupMenuFactory) {
        this.table = table;
        this.popupMenuFactory = popupMenuFactory;
    }
    
    @Override
    public boolean isPopupShowing(int row) {
        return (popupMenu != null) && popupMenu.isVisible();
    }

    @Override
    public void maybeShowPopup(Component component, int x, int y) {
        // Get upload item for popup row.
        int popupRow = table.rowAtPoint(new Point(x, y));
        UploadItem item = table.getUploadItem(popupRow);
        
        // Get selected items.
        List<UploadItem> uploadItems = table.getSelectedItems();
        
        // Adjust selection if popup row is not selected.
        if ((uploadItems.size() == 0) || !uploadItems.contains(item)) {
            table.setRowSelectionInterval(popupRow, popupRow);
            uploadItems = table.getSelectedItems();
        }
        
        // Create popup menu and display.
        popupMenu = popupMenuFactory.create(table, uploadItems);
        popupMenu.show(component, x, y);
    }
}
