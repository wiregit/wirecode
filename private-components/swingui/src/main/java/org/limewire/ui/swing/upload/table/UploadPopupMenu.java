package org.limewire.ui.swing.upload.table;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.table.TableCellEditor;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadState;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * Popup menu for the Uploads table.
 */
public class UploadPopupMenu extends JPopupMenu {

    private final UploadTable table;
    private final List<UploadItem> uploadItems;
    private final UploadActionHandler actionHandler;
    private final LibraryManager libraryManager;

    @Inject
    public UploadPopupMenu(
            @Assisted UploadTable table,
            @Assisted List<UploadItem> uploadItems,
            UploadActionHandler actionHandler,
            LibraryManager libraryManager) {
        this.table = table;
        this.uploadItems = uploadItems;
        this.actionHandler = actionHandler;
        this.libraryManager = libraryManager;
        
        createMenu();
    }
    
    /**
     * Builds the menu.
     */
    private void createMenu() {
        int itemCount = uploadItems.size();
        if (itemCount == 0) {
            throw new IllegalStateException(I18n.tr("No selected items"));
            
        } else if (itemCount == 1) {
            UploadItem item = uploadItems.get(0);
            switch (item.getUploadItemType()) {
            case GNUTELLA:
                createSingleGnutellaMenu(item);
                break;
                
            case BITTORRENT:
                createSingleTorrentMenu(item);
                break;
                
            default:
                throw new IllegalStateException(I18n.tr("Unknown upload type"));
            }
            
        } else {
            // Multiple items selected.
            boolean allDone = true;
            for (UploadItem item : uploadItems) {
                if (item.getState() != UploadState.DONE) {
                    allDone = false;
                    break;
                }
            }
            createMultipleItemMenu(allDone);
        }
        
    }
    
    /**
     * Builds the menu for a single Gnutella upload item.
     */
    private void createSingleGnutellaMenu(UploadItem uploadItem) {
        ActionListener listener = new SingleItemMenuListener();
        
        UploadState state = uploadItem.getState();
        
        if (isDone(state)) {
            JMenuItem removeMenuItem = new JMenuItem(I18n.tr("Clear from Tray"));
            removeMenuItem.setActionCommand(UploadActionHandler.REMOVE_COMMAND);
            removeMenuItem.addActionListener(listener);
            add(removeMenuItem);
        }
        
        if (!isBrowse(state)) {
            if (isDone(state)) addSeparator();

            JMenuItem launchMenuItem = new JMenuItem(I18n.tr("Launch File"));
            launchMenuItem.setActionCommand(isPlayable(uploadItem.getCategory()) ?
                    UploadActionHandler.PLAY_COMMAND : UploadActionHandler.LAUNCH_COMMAND);
            launchMenuItem.addActionListener(listener);
            add(launchMenuItem);

            JMenuItem locateOnDiskMenuItem = new JMenuItem(I18n.tr("Locate on Disk"));
            locateOnDiskMenuItem.setActionCommand(UploadActionHandler.LOCATE_ON_DISK_COMMAND);
            locateOnDiskMenuItem.addActionListener(listener);
            add(locateOnDiskMenuItem);

            JMenuItem showInLibraryMenuItem = new JMenuItem(I18n.tr("Locate in Library"));
            showInLibraryMenuItem.setActionCommand(UploadActionHandler.LIBRARY_COMMAND);
            showInLibraryMenuItem.addActionListener(listener);
            add(showInLibraryMenuItem).setEnabled(libraryManager.getLibraryManagedList().contains(uploadItem.getUrn()));

            addSeparator();

            if (isDone(state)) {
                JMenu addToListMenu = new JMenu(I18n.tr("Add to List"));
                add(addToListMenu);
                
                JMenu showInListMenu = new JMenu(I18n.tr("Show in List"));
                add(showInListMenu);
                
            } else {
                JMenu browseMenu = new JMenu(I18n.tr("Browse Files"));
                add(browseMenu);
                
                JMenu blockMenu = new JMenu(I18n.tr("Block User"));
                add(blockMenu);
            }
            
            addSeparator();
            
            JMenuItem fileInfoMenuItem = new JMenuItem(I18n.tr("View File Info..."));
            fileInfoMenuItem.setActionCommand(UploadActionHandler.PROPERTIES_COMMAND);
            fileInfoMenuItem.addActionListener(listener);
            add(fileInfoMenuItem);
        }
        
//        if(state == UploadState.BROWSE_HOST || state == UploadState.BROWSE_HOST_DONE || state == UploadState.DONE || state == UploadState.UNABLE_TO_UPLOAD){
//            add(removeMenuItem);
//        } else {
//            add(cancelMenuItem);
//        }
//        
//        if (state != UploadState.BROWSE_HOST && state != UploadState.BROWSE_HOST_DONE) {
//            addSeparator();
//
//            if (uploadItem.getCategory() == Category.VIDEO || uploadItem.getCategory() == Category.AUDIO) {
//                add(playMenuItem);
//            } else if (uploadItem.getCategory() != Category.PROGRAM && uploadItem.getCategory() != Category.OTHER) {
//                add(launchMenuItem);
//            }
//            add(locateOnDiskMenuItem);
//            add(showInLibraryMenuItem).setEnabled(libraryManager.getLibraryManagedList().contains(uploadItem.getUrn()));
//
//            addSeparator();
//
//            add(fileInfoMenuItem);
//        }
    }
    
    /**
     * Builds the menu for a single BitTorrent upload item.
     */
    private void createSingleTorrentMenu(UploadItem uploadItem) {
        ActionListener listener = new SingleItemMenuListener();

        UploadState state = uploadItem.getState();
        
        if (isDone(state)) {
            JMenuItem removeMenuItem = new JMenuItem(I18n.tr("Clear from Tray"));
            removeMenuItem.setActionCommand(UploadActionHandler.REMOVE_COMMAND);
            removeMenuItem.addActionListener(listener);
            add(removeMenuItem);
        }
        
        if (!isBrowse(state)) {
            JMenuItem locateOnDiskMenuItem = new JMenuItem(I18n.tr("Locate on Disk"));
            locateOnDiskMenuItem.setActionCommand(UploadActionHandler.LOCATE_ON_DISK_COMMAND);
            locateOnDiskMenuItem.addActionListener(listener);
            add(locateOnDiskMenuItem);

            addSeparator();

            if (isDone(state)) {
                JMenu addToListMenu = new JMenu(I18n.tr("Add to List"));
                add(addToListMenu);
                
            } else {
                JMenu browseMenu = new JMenu(I18n.tr("Browse Files"));
                add(browseMenu);
                
                JMenu blockMenu = new JMenu(I18n.tr("Block User"));
                add(blockMenu);
            }
            
            addSeparator();
            
            JMenuItem fileInfoMenuItem = new JMenuItem(I18n.tr("View File Info..."));
            fileInfoMenuItem.setActionCommand(UploadActionHandler.PROPERTIES_COMMAND);
            fileInfoMenuItem.addActionListener(listener);
            add(fileInfoMenuItem);
        }
    }
    
    /**
     * Builds the menu for multiple upload items.
     */
    private void createMultipleItemMenu(boolean allDone) {
        // TODO create listener for multiple items
        
        if (!allDone) {
            JMenuItem pauseMenuItem = new JMenuItem(I18n.tr("Pause"));
            add(pauseMenuItem);
            
            JMenuItem resumeMenuItem = new JMenuItem(I18n.tr("Resume"));
            add(resumeMenuItem);
            
            JMenuItem retryMenuItem = new JMenuItem(I18n.tr("Retry"));
            add(retryMenuItem);
        }
        
        JMenuItem removeMenuItem = new JMenuItem(I18n.tr("Clear from Tray"));
        removeMenuItem.setActionCommand(UploadActionHandler.REMOVE_COMMAND);
        //removeMenuItem.addActionListener(listener);
        add(removeMenuItem);
        
        if (!allDone) {
            addSeparator();

            JMenu browseMenu = new JMenu(I18n.tr("Browse Files"));
            add(browseMenu);

            JMenu blockMenu = new JMenu(I18n.tr("Block User"));
            add(blockMenu);

            addSeparator();
            
            // TODO add cancel
            JMenuItem cancelMenuItem = new JMenuItem(I18n.tr("Cancel"));
            cancelMenuItem.setActionCommand(UploadActionHandler.CANCEL_COMMAND);
            //cancelMenuItem.addActionListener(menuListener);
            add(cancelMenuItem);
            
        } else {
            JMenu addToListMenu = new JMenu(I18n.tr("Add to List"));
            add(addToListMenu);
        }
    }
    
    private boolean isBrowse(UploadState state) {
        return (state == UploadState.BROWSE_HOST) || (state == UploadState.BROWSE_HOST_DONE);
    }
    
    private boolean isDone(UploadState state) {
        return (state == UploadState.DONE) || (state == UploadState.BROWSE_HOST) ||
            (state == UploadState.BROWSE_HOST_DONE) || (state == UploadState.UNABLE_TO_UPLOAD);
    }
    
    private boolean isPlayable(Category category) {
        return (category == Category.AUDIO) || (category == Category.VIDEO);
    }
    
    /**
     * Action listener for menu items that apply to a single item.
     */
    private class SingleItemMenuListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            UploadItem uploadItem = uploadItems.get(0);
            if (uploadItem != null) {
                actionHandler.performAction(e.getActionCommand(), uploadItem);
            }
            
            // must cancel editing
            Component comp = table.getEditorComponent();
            if (comp instanceof TableCellEditor) {
                ((TableCellEditor)comp).cancelCellEditing();
            }
        }
    }
}
